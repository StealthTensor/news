package app.vaartalu.player

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.vaartalu.VaartaluApp
import app.vaartalu.data.Channel
import app.vaartalu.data.StreamSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PlayerViewModel"
private const val PREFS = "vaartalu"
private const val PREF_LAST_CHANNEL = "lastChannel"

/** A source that stays in BUFFERING this long is treated as dead. */
private const val STALL_TIMEOUT_MS = 20_000L

/** After every source of a channel failed, start over this often until one works. */
private const val RETRY_DELAY_MS = 15_000L

enum class PlaybackStatus { Loading, Playing, Failed }

data class HomeState(
    val channels: List<Channel> = emptyList(),
    val selectedId: String? = null,
    val status: PlaybackStatus = PlaybackStatus.Loading,
) {
    val selected: Channel? get() = channels.firstOrNull { it.id == selectedId }
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = (app as VaartaluApp).graph
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val player: ExoPlayer = ExoPlayer.Builder(app)
        .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(graph.http)))
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var plan = SourcePlan(emptyList())
    private var attempt: Job? = null
    private var watchdog: Job? = null
    private var retry: Job? = null
    private var inBackground = false

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    watchdog?.cancel()
                    retry?.cancel()
                    setStatus(PlaybackStatus.Playing)
                }
                Player.STATE_BUFFERING -> {
                    setStatus(PlaybackStatus.Loading)
                    armWatchdog()
                }
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition()
                player.prepare()
                return
            }
            Log.w(TAG, "playback error ${error.errorCodeName}", error)
            tryNextSource()
        }
    }

    init {
        player.addListener(listener)
        viewModelScope.launch {
            graph.manifest.manifest.collect { onManifest(it.channels) }
        }
    }

    fun select(channel: Channel) {
        prefs.edit().putString(PREF_LAST_CHANNEL, channel.id).apply()
        _state.update { it.copy(selectedId = channel.id) }
        start(channel)
    }

    /** Restart the current channel from its first source (fresh live edge). */
    fun reload() {
        _state.value.selected?.let(::start)
    }

    fun onForeground() {
        viewModelScope.launch { graph.manifest.refresh() }
        if (inBackground) {
            inBackground = false
            reload()
        }
    }

    fun onBackground() {
        inBackground = true
        cancelJobs()
        player.stop()
        player.clearMediaItems()
    }

    private fun onManifest(channels: List<Channel>) {
        val current = _state.value.selectedId
        val keep = channels.firstOrNull { it.id == current }
        _state.update { it.copy(channels = channels) }
        if (keep != null) return
        val last = prefs.getString(PREF_LAST_CHANNEL, null)
        val first = channels.firstOrNull { it.id == last } ?: channels.firstOrNull() ?: return
        select(first)
    }

    private fun start(channel: Channel) {
        cancelJobs()
        player.stop()
        player.clearMediaItems()
        plan = SourcePlan(channel.sources)
        setStatus(PlaybackStatus.Loading)
        tryNextSource()
    }

    private fun tryNextSource() {
        watchdog?.cancel()
        val source = plan.next() ?: return fail()
        attempt?.cancel()
        attempt = viewModelScope.launch {
            val url = try {
                resolve(source)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "could not resolve $source", e)
                tryNextSource()
                return@launch
            }
            play(url)
        }
    }

    private suspend fun resolve(source: StreamSource): String = when (source) {
        is StreamSource.Hls -> source.url
        is StreamSource.YouTubeLive -> graph.youTube.hlsUrl(source)
    }

    private fun play(url: String) {
        player.setMediaItem(
            MediaItem.Builder().setUri(url).setMimeType(MimeTypes.APPLICATION_M3U8).build(),
        )
        player.prepare()
        player.playWhenReady = true
        armWatchdog()
    }

    private fun armWatchdog() {
        watchdog?.cancel()
        watchdog = viewModelScope.launch {
            delay(STALL_TIMEOUT_MS)
            Log.w(TAG, "source stalled, trying next")
            tryNextSource()
        }
    }

    private fun fail() {
        player.stop()
        setStatus(PlaybackStatus.Failed)
        retry?.cancel()
        retry = viewModelScope.launch {
            delay(RETRY_DELAY_MS)
            reload()
        }
    }

    private fun cancelJobs() {
        attempt?.cancel()
        watchdog?.cancel()
        retry?.cancel()
    }

    private fun setStatus(status: PlaybackStatus) {
        _state.update { it.copy(status = status) }
    }

    override fun onCleared() {
        cancelJobs()
        player.removeListener(listener)
        player.release()
    }
}
