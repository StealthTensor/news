package app.vaartalu.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

data class LiveListing(val videoId: String, val title: String)

/** Parsing of YouTube pages and links; pure so it can be unit tested. */
object LiveVideoPage {
    private val canonical = Regex(
        "<link rel=\"canonical\" href=\"https://www\\.youtube\\.com/watch\\?v=([0-9A-Za-z_-]{11})\"",
    )
    private val watchUrlId = Regex("[?&]v=([0-9A-Za-z_-]{11})")

    /** Video id if the `/live` page is a live stream, null if the channel is not live right now. */
    fun videoId(html: String): String? =
        if ("\"isLive\":true" in html) canonical.find(html)?.groupValues?.get(1) else null

    fun videoIdOfWatchUrl(url: String): String? = watchUrlId.find(url)?.groupValues?.get(1)
}

class YouTubeLiveResolver(private val http: OkHttpClient) {
    suspend fun hlsUrl(source: StreamSource.YouTubeLive): String = withContext(Dispatchers.IO) {
        // A stale hint must not leave him with no picture: fall back to whatever YouTube calls live.
        val videoId = source.titleHint?.let { findByTitle(source) }?.videoId ?: liveVideoIdFromPage(source.liveUrl)
        val hls = StreamInfo.getInfo("https://www.youtube.com/watch?v=$videoId").hlsUrl
        check(hls.isNotBlank()) { "no HLS manifest for $videoId" }
        hls
    }

    /** Whether any YouTube source of [channel] is streaming right now; throws if YouTube cannot be asked. */
    suspend fun isLive(channel: Channel): Boolean = withContext(Dispatchers.IO) {
        channel.sources.filterIsInstance<StreamSource.YouTubeLive>().any { source ->
            if (source.titleHint != null) findByTitle(source) != null else liveStreams(source.channelUrl).isNotEmpty()
        }
    }

    /** The live stream whose title contains the source's hint, or null (blocking network call). */
    fun findByTitle(source: StreamSource.YouTubeLive): LiveListing? {
        val hint = source.titleHint ?: return null
        return liveStreams(source.channelUrl).firstOrNull { it.title.contains(hint, ignoreCase = true) }
    }

    /** Everything the channel is streaming live right now (blocking network call). */
    fun liveStreams(channelUrl: String): List<LiveListing> {
        val tab = ChannelInfo.getInfo(channelUrl).tabs.firstOrNull { ChannelTabs.LIVESTREAMS in it.contentFilters }
            ?: return emptyList()
        return ChannelTabInfo.getInfo(ServiceList.YouTube, tab).relatedItems
            .filterIsInstance<StreamInfoItem>()
            .filter { it.streamType == StreamType.LIVE_STREAM }
            .mapNotNull { item -> LiveVideoPage.videoIdOfWatchUrl(item.url)?.let { LiveListing(it, item.name) } }
    }

    private fun liveVideoIdFromPage(liveUrl: String): String {
        val request = Request.Builder()
            .url(liveUrl)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept-Language", "en-US")
            .build()
        val html = http.newCall(request).execute().use { res ->
            check(res.isSuccessful) { "HTTP ${res.code} for $liveUrl" }
            res.bodyString()
        }
        return checkNotNull(LiveVideoPage.videoId(html)) { "channel not live: $liveUrl" }
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
