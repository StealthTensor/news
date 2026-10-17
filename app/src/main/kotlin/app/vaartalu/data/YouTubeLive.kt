package app.vaartalu.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamInfo

/** Finds the current live video of a channel from its `/live` page. */
object LiveVideoPage {
    private val canonical = Regex(
        "<link rel=\"canonical\" href=\"https://www\\.youtube\\.com/watch\\?v=([0-9A-Za-z_-]{11})\"",
    )

    /** Video id if the page is a live stream, null if the channel is not live right now. */
    fun videoId(html: String): String? =
        if ("\"isLive\":true" in html) canonical.find(html)?.groupValues?.get(1) else null
}

class YouTubeLiveResolver(private val http: OkHttpClient) {
    suspend fun hlsUrl(liveUrl: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(liveUrl)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept-Language", "en-US")
            .build()
        val html = http.newCall(request).execute().use { res ->
            check(res.isSuccessful) { "HTTP ${res.code} for $liveUrl" }
            res.bodyString()
        }
        val videoId = checkNotNull(LiveVideoPage.videoId(html)) { "channel not live: $liveUrl" }
        val hls = StreamInfo.getInfo("https://www.youtube.com/watch?v=$videoId").hlsUrl
        check(hls.isNotBlank()) { "no HLS manifest for $videoId" }
        hls
    }
}

/** OkHttp-backed [Downloader] that NewPipeExtractor requires. */
class NewPipeDownloader(private val http: OkHttpClient) : Downloader() {
    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
        val builder = Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), request.dataToSend()?.toRequestBody())
            .header("User-Agent", BROWSER_USER_AGENT)
        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }
        http.newCall(builder.build()).execute().use { res ->
            if (res.code == 429) throw ReCaptchaException("reCaptcha challenge requested", request.url())
            return Response(
                res.code,
                res.message,
                res.headers.toMultimap(),
                res.bodyString(),
                res.request.url.toString(),
            )
        }
    }
}
