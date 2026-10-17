package app.vaartalu.ui

import app.vaartalu.ui.FullscreenChrome.Buttons
import app.vaartalu.ui.FullscreenChrome.Channels
import app.vaartalu.ui.FullscreenChrome.Hidden
import org.junit.Assert.assertEquals
import org.junit.Test

class FullscreenChromeTest {
    @Test
    fun `a stray tap only reveals buttons and never the channel strip`() {
        assertEquals(Buttons, Hidden.onScreenTap())
    }

    @Test
    fun `tapping the screen again hides whatever is showing`() {
        assertEquals(Hidden, Buttons.onScreenTap())
        assertEquals(Hidden, Channels.onScreenTap())
    }

    @Test
    fun `channel strip opens only from the channels button`() {
        assertEquals(Channels, Buttons.onChannelsPressed())
    }

    @Test
    fun `picking a channel closes the overlay immediately from any state`() {
        FullscreenChrome.entries.forEach { assertEquals(Hidden, it.onChannelPicked()) }
    }

    @Test
    fun `only visible states auto-hide`() {
        assertEquals(null, Hidden.autoHideMs)
        assertEquals(true, Buttons.autoHideMs != null)
        assertEquals(true, Channels.autoHideMs != null)
    }
}
