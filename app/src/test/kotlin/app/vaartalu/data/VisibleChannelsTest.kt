package app.vaartalu.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleChannelsTest {
    private fun channel(id: String, group: String = "News", liveOnly: Boolean = false) = Channel(
        id = id,
        name = id,
        group = group,
        logo = null,
        plateArgb = null,
        sources = listOf(StreamSource.Hls("https://x.example/$id.m3u8")),
        liveOnly = liveOnly,
    )

    private val tv9 = channel("tv9")
    private val dd = channel("dd", group = "Sports")
    private val icc = channel("icc", group = "Sports", liveOnly = true)
    private val fifa = channel("fifa", group = "Sports", liveOnly = true)
    private val all = listOf(tv9, dd, icc, fifa)

    @Test
    fun `always-on channels show whether or not anything is live`() {
        assertEquals(listOf(tv9, dd), all.visibleChannels(liveNow = emptySet(), keepId = null))
    }

    @Test
    fun `live-only channel shows exactly while it is live and keeps manifest order`() {
        assertEquals(listOf(tv9, dd, fifa), all.visibleChannels(liveNow = setOf("fifa"), keepId = null))
        assertEquals(all, all.visibleChannels(liveNow = setOf("fifa", "icc"), keepId = null))
    }

    @Test
    fun `selected live-only channel stays visible after its stream ends`() {
        assertEquals(listOf(tv9, dd, icc), all.visibleChannels(liveNow = emptySet(), keepId = "icc"))
    }

    @Test
    fun `tab with only offline live-only channels disappears`() {
        val onlyEvents = listOf(tv9, icc, fifa)

        assertEquals(listOf("News"), onlyEvents.visibleChannels(emptySet(), null).groups)
        assertEquals(listOf("News", "Sports"), onlyEvents.visibleChannels(setOf("icc"), null).groups)
    }
}
