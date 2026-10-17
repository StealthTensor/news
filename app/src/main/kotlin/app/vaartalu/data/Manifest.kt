package app.vaartalu.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface StreamSource {
    data class Hls(val url: String) : StreamSource
    data class YouTubeLive(val liveUrl: String) : StreamSource
}

data class Channel(
    val id: String,
    val name: String,
    /** Tab this channel appears under in the UI, e.g. "News" or "Devotional". */
    val group: String,
    val logo: String?,
    /** Opaque ARGB behind the logo; null = white. Needed for logos drawn in white (10TV). */
    val plateArgb: Int?,
    val sources: List<StreamSource>,
)

data class Manifest(val version: Int, val channels: List<Channel>)

/** Tab names in the order they first appear in the manifest. */
val List<Channel>.groups: List<String> get() = map { it.group }.distinct()

@Serializable
private data class ManifestDto(val version: Int, val channels: List<ChannelDto>)

@Serializable
private data class ChannelDto(
    val id: String,
    val name: String,
    val group: String? = null,
    val logo: String? = null,
    val plate: String? = null,
    val sources: List<SourceDto> = emptyList(),
)

@Serializable
private data class SourceDto(val type: String, val url: String? = null, val ref: String? = null)

/**
 * Parses and validates the channel manifest. The remote copy is a trust boundary,
 * so anything malformed (non-https URLs, unknown source types, empty channels) is
 * dropped rather than passed to the player.
 */
object ManifestParser {
    private const val DEFAULT_GROUP = "News"
    private const val MAX_GROUP_CHARS = 20
    private val json = Json { ignoreUnknownKeys = true }
    private val httpsUrl = Regex("^https://\\S+$")
    private val hexColor = Regex("^#[0-9A-Fa-f]{6}$")
    private val OPAQUE = 0xFF000000.toInt()
    private val youTubeChannelId =Regex("^UC[0-9A-Za-z_-]{22}$")
    private val youTubeHandle = Regex("^@[0-9A-Za-z._-]{3,30}$")

    fun parse(text: String): Result<Manifest> = runCatching {
        val dto = json.decodeFromString<ManifestDto>(text)
        val channels = dto.channels.mapNotNull(::toChannel).distinctBy { it.id }
        require(channels.isNotEmpty()) { "manifest has no valid channels" }
        Manifest(dto.version, channels)
    }

    private fun toChannel(dto: ChannelDto): Channel? {
        val sources = dto.sources.mapNotNull(::toSource)
        if (dto.id.isBlank() || dto.name.isBlank() || sources.isEmpty()) return null
        return Channel(
            id = dto.id,
            name = dto.name,
            group = dto.group?.trim()?.take(MAX_GROUP_CHARS)?.takeIf { it.isNotEmpty() } ?: DEFAULT_GROUP,
            logo = dto.logo?.takeIf { httpsUrl.matches(it) },
            plateArgb = dto.plate?.takeIf { hexColor.matches(it) }?.let { OPAQUE or it.substring(1).toInt(16) },
            sources = sources,
        )
    }

    private fun toSource(dto: SourceDto): StreamSource? = when (dto.type) {
        "hls" -> dto.url?.takeIf { httpsUrl.matches(it) }?.let(StreamSource::Hls)
        "youtube" -> dto.ref?.let(::youTubeLiveUrl)?.let(StreamSource::YouTubeLive)
        else -> null
    }

    private fun youTubeLiveUrl(ref: String): String? = when {
        youTubeChannelId.matches(ref) -> "https://www.youtube.com/channel/$ref/live"
        youTubeHandle.matches(ref) -> "https://www.youtube.com/$ref/live"
        else -> null
    }
}
