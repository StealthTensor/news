package app.vaartalu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveVideoPageTest {
    private val canonicalWatch =
        """<link rel="canonical" href="https://www.youtube.com/watch?v=szHkcssWIdM">"""
    private val canonicalChannel =
        """<link rel="canonical" href="https://www.youtube.com/channel/UCJd_bLxtJaunWxAbhNjoopw/live">"""

    @Test
    fun `live page yields the video id`() {
        val html = """<html>$canonicalWatch<script>{"isLive":true}</script></html>"""

        assertEquals("szHkcssWIdM", LiveVideoPage.videoId(html))
    }

    @Test
    fun `channel that is not live yields null`() {
        val html = """<html>$canonicalChannel</html>"""

        assertNull(LiveVideoPage.videoId(html))
    }

    @Test
    fun `finished stream with a watch canonical but no live flag yields null`() {
        val html = """<html>$canonicalWatch<script>{"isLive":false}</script></html>"""

        assertNull(LiveVideoPage.videoId(html))
    }
}
