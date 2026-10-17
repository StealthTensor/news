package app.vaartalu.ui

/**
 * Fullscreen overlay state. A stray touch must never cover the video or change anything,
 * so the first tap only reveals two buttons; the channel strip needs a second, deliberate tap.
 */
enum class FullscreenChrome(val autoHideMs: Long?) {
    Hidden(null),
    Buttons(4_000),
    Channels(10_000),
    ;

    fun onScreenTap(): FullscreenChrome = if (this == Hidden) Buttons else Hidden

    fun onChannelsPressed(): FullscreenChrome = Channels

    fun onChannelPicked(): FullscreenChrome = Hidden
}
