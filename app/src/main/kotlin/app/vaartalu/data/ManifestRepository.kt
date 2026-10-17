package app.vaartalu.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val TAG = "ManifestRepository"
private const val MAX_MANIFEST_CHARS = 256 * 1024

/**
 * Channel list with three layers: bundled asset (always valid), disk cache of the last
 * good remote copy, and the remote copy itself. Highest `version` wins; a remote copy
 * that fails validation or is older than what we already have is ignored.
 */
class ManifestRepository(
    private val context: Context,
    private val http: OkHttpClient,
    private val remoteUrl: String,
) {
    private val cacheFile = File(context.filesDir, "channels.json")
    private val _manifest = MutableStateFlow(loadLocal())
    val manifest: StateFlow<Manifest> = _manifest.asStateFlow()

    suspend fun refresh() {
        if (remoteUrl.isBlank()) return
        val text = withContext(Dispatchers.IO) { fetch() } ?: return
        val remote = ManifestParser.parse(text)
            .onFailure { Log.w(TAG, "remote manifest rejected", it) }
            .getOrNull() ?: return
        if (remote.version < _manifest.value.version) return
        withContext(Dispatchers.IO) { writeCache(text) }
        _manifest.value = remote
    }

    private fun fetch(): String? = try {
        http.newCall(Request.Builder().url(remoteUrl).build()).execute().use { res ->
            if (!res.isSuccessful) {
                Log.w(TAG, "remote manifest HTTP ${res.code}")
                null
            } else {
                res.bodyString().takeIf { it.length <= MAX_MANIFEST_CHARS }
                    ?: run { Log.w(TAG, "remote manifest too large"); null }
            }
        }
    } catch (e: IOException) {
        Log.w(TAG, "remote manifest fetch failed", e)
        null
    }

    private fun writeCache(text: String) {
        val tmp = File(cacheFile.parentFile, "channels.json.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(cacheFile)) Log.w(TAG, "could not replace manifest cache")
    }

    private fun loadLocal(): Manifest {
        val bundled = ManifestParser
            .parse(context.assets.open("channels.json").bufferedReader().use { it.readText() })
            .getOrThrow()
        val cached = cacheFile.takeIf { it.exists() }?.let { file ->
            ManifestParser.parse(file.readText())
                .onFailure { Log.w(TAG, "cached manifest unreadable", it) }
                .getOrNull()
        }
        return if (cached != null && cached.version >= bundled.version) cached else bundled
    }
}
