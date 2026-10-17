package app.vaartalu.data

import app.vaartalu.player.SourcePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourcePlanTest {
    private val hls = StreamSource.Hls("https://a.example/x.m3u8")
    private val yt = StreamSource.YouTubeLive("https://www.youtube.com/@tv5news/live")

    @Test
    fun `hands out sources in order then runs dry`() {
        val plan = SourcePlan(listOf(hls, yt))

        assertEquals(hls, plan.next())
        assertEquals(yt, plan.next())
        assertNull(plan.next())
        assertNull(plan.next())
    }

    @Test
    fun `empty plan is immediately exhausted`() {
        assertNull(SourcePlan(emptyList()).next())
    }
}
