package app.vaartalu.data

import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

/**
 * Hits the real network, so it only runs with `LIVE_SMOKE=1 ./gradlew :app:testDebugUnitTest`.
 * Run it from an Indian IP when a channel looks dead, then fix the manifest, not the app.
 * Per-source results go to the test's stdout in the XML report.
 */
class LiveSourcesSmokeTest {
    companion object {
        private val http: OkHttpClient = newHttpClient()

        @BeforeClass
        @JvmStatic
        fun setUp() {
            assumeTrue("set LIVE_SMOKE=1 to run", System.getenv("LIVE_SMOKE") == "1")
            NewPipe.init(NewPipeDownloader(http), Localization("en", "IN"), ContentCountry("IN"))
        }
    }

    private val youTube = YouTubeLiveResolver(http)

    @Test
    fun `every channel has at least one working source`() {
        val manifest = ManifestParser.parse(File("src/main/assets/channels.json").readText()).getOrThrow()
        val dead = mutableListOf<String>()
        val staleHints = mutableListOf<String>()

        for (channel in manifest.channels) {
            if (channel.liveOnly) {
                // Hidden in the app while offline, so offline is fine; when live it must play.
                val live = runCatching { runBlocking { youTube.isLive(channel) } }
                println("${channel.id}: liveOnly, live now -> ${live.getOrElse { "probe failed: ${it.message}" }}")
                if (live.getOrNull() == true) {
                    val outcomes = channel.sources.map { source -> source to check(source) }
                    outcomes.forEach { (source, outcome) -> println("${channel.id}: ${outcome ?: "OK"} <- $source") }
                    if (outcomes.none { (_, outcome) -> outcome == null }) dead += channel.id
                }
                continue
            }
            channel.sources.filterIsInstance<StreamSource.YouTubeLive>().forEach { source ->
                val lives = runCatching { youTube.liveStreams(source.channelUrl).map { it.title.take(70) } }
                println("${channel.id}: live on YouTube now (hint=${source.titleHint}) -> ${lives.getOrElse { "listing failed: ${it.message}" }}")
            }
            channel.sources.filterIsInstance<StreamSource.YouTubeLive>().filter { it.titleHint != null }.forEach { source ->
                val match = runCatching { youTube.findByTitle(source) }.getOrNull()
                println("${channel.id}: hint \"${source.titleHint}\" -> ${match?.let { "matched \"${it.title.take(70)}\"" } ?: "NO MATCH"}")
                if (match == null) staleHints += "${channel.id} (${source.titleHint})"
            }
            val results = channel.sources.map { source -> source to check(source) }
            results.forEach { (source, outcome) -> println("${channel.id}: ${outcome ?: "OK"} <- $source") }
            if (results.none { (_, outcome) -> outcome == null }) dead += channel.id
        }

        assertTrue("channels with no working source: $dead", dead.isEmpty())
        assertTrue("YouTube title hints matching no live stream (fallback would pick /live): $staleHints", staleHints.isEmpty())
    }

    /** Null when the source plays, otherwise why it does not. */
    private fun check(source: StreamSource): String? = try {
        val playlistUrl = when (source) {
            is StreamSource.Hls -> source.url
            is StreamSource.YouTubeLive -> runBlocking { youTube.hlsUrl(source) }
        }
        val request = Request.Builder().url(playlistUrl).build()
        http.newCall(request).execute().use { res ->
            when {
                !res.isSuccessful -> "FAIL HTTP ${res.code}"
                !res.bodyString().startsWith("#EXTM3U") -> "FAIL not an HLS playlist"
                else -> null
            }
        }
    } catch (e: Exception) {
        "FAIL ${e.javaClass.simpleName}: ${e.message?.take(120)}"
    }
}
