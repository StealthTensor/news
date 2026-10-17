package app.vaartalu.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestParserTest {
    private fun source(type: String, field: String, value: String) =
        """{"type":"$type","$field":"$value"}"""

    private fun manifest(vararg channels: String) =
        """{"version":1,"channels":[${channels.joinToString(",")}]}"""

    private fun channel(id: String, vararg sources: String, logo: String? = null) =
        """{"id":"$id","name":"Name","logo":${logo?.let { "\"$it\"" }},"sources":[${sources.joinToString(",")}]}"""

    @Test
    fun `bundled manifest parses with every channel playable`() {
        val text = File("src/main/assets/channels.json").readText()

        val manifest = ManifestParser.parse(text).getOrThrow()

        assertEquals(12, manifest.channels.size)
        assertEquals(listOf("News", "Devotional"), manifest.channels.groups)
        assertEquals(9, manifest.channels.count { it.group == "News" })
        assertTrue(manifest.channels.all { it.sources.isNotEmpty() })
        assertTrue(manifest.channels.all { it.logo?.startsWith("https://") != false })
    }

    @Test
    fun `non-https hls url is dropped and channel without sources is removed`() {
        val text = manifest(
            channel("a", source("hls", "url", "http://insecure.example/x.m3u8")),
            channel("b", source("hls", "url", "https://ok.example/x.m3u8")),
        )

        val ids = ManifestParser.parse(text).getOrThrow().channels.map { it.id }

        assertEquals(listOf("b"), ids)
    }

    @Test
    fun `unknown source types are ignored while known ones stay in order`() {
        val text = manifest(
            channel(
                "a",
                source("rtmp", "url", "https://x.example/stream"),
                source("hls", "url", "https://ok.example/x.m3u8"),
                source("youtube", "ref", "@tv5news"),
            ),
        )

        val sources = ManifestParser.parse(text).getOrThrow().channels.single().sources

        assertEquals(
            listOf(
                StreamSource.Hls("https://ok.example/x.m3u8"),
                StreamSource.YouTubeLive("https://www.youtube.com/@tv5news/live"),
            ),
            sources,
        )
    }

    @Test
    fun `youtube channel id becomes a channel live url and junk refs are rejected`() {
        val id = "UCPXTXMecYqnRKNdqdVOGSFg"
        val text = manifest(
            channel("a", source("youtube", "ref", id)),
            channel("b", source("youtube", "ref", "not a ref; rm -rf")),
        )

        val channels = ManifestParser.parse(text).getOrThrow().channels

        assertEquals(listOf("a"), channels.map { it.id })
        assertEquals(
            StreamSource.YouTubeLive("https://www.youtube.com/channel/$id/live"),
            channels.single().sources.single(),
        )
    }

    @Test
    fun `non-https logo is discarded but channel is kept`() {
        val text = manifest(
            channel("a", source("hls", "url", "https://ok.example/x.m3u8"), logo = "http://x.example/l.png"),
        )

        assertNull(ManifestParser.parse(text).getOrThrow().channels.single().logo)
    }

    @Test
    fun `plate color is parsed as opaque argb and bad values fall back to null`() {
        val hls = source("hls", "url", "https://ok.example/x.m3u8")
        fun withPlate(plate: String) =
            """{"id":"a","name":"Name","plate":"$plate","sources":[$hls]}"""

        fun plateOf(plate: String) =
            ManifestParser.parse(manifest(withPlate(plate))).getOrThrow().channels.single().plateArgb

        assertEquals(0xFF15171C.toInt(), plateOf("#15171C"))
        assertNull(plateOf("red"))
        assertNull(plateOf("#12345"))
        assertNull(plateOf("#GGGGGG"))
    }

    @Test
    fun `group defaults to News and blank or missing group falls back to it`() {
        val hls = source("hls", "url", "https://ok.example/x.m3u8")
        fun groupOf(field: String) =
            ManifestParser.parse(manifest("""{"id":"a","name":"N",$field"sources":[$hls]}"""))
                .getOrThrow().channels.single().group

        assertEquals("News", groupOf(""))
        assertEquals("News", groupOf(""""group":"   ","""))
        assertEquals("Devotional", groupOf(""""group":" Devotional ","""))
    }

    @Test
    fun `groups keep first-appearance order`() {
        val hls = source("hls", "url", "https://ok.example/x.m3u8")
        fun ch(id: String, group: String) = """{"id":"$id","name":"N","group":"$group","sources":[$hls]}"""

        val channels = ManifestParser.parse(manifest(ch("a", "B"), ch("b", "A"), ch("c", "B"))).getOrThrow().channels

        assertEquals(listOf("B", "A"), channels.groups)
    }

    @Test
    fun `duplicate channel ids keep the first`() {
        val hls = source("hls", "url", "https://ok.example/x.m3u8")

        val channels = ManifestParser.parse(manifest(channel("a", hls), channel("a", hls))).getOrThrow().channels

        assertEquals(1, channels.size)
    }

    @Test
    fun `manifest with no valid channels fails`() {
        assertTrue(ManifestParser.parse(manifest()).isFailure)
        assertTrue(ManifestParser.parse(manifest(channel("a"))).isFailure)
    }

    @Test
    fun `malformed json fails instead of throwing`() {
        assertTrue(ManifestParser.parse("{not json").isFailure)
    }
}
