package app.vaartalu.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Dark on purpose: the video is the hero, logos sit on white plates and pop, and an
 * OLED screen stays dim in a dark room. Amber is the only accent and means "this one".
 */
object Palette {
    val Ink = Color(0xFF0D0F12)
    val Panel = Color(0xFF1A1E24)
    val Text = Color(0xFFF5F6F8)
    val TextDim = Color(0xFFA8B0BB)
    val Amber = Color(0xFFFFB020)
    val OnAmber = Color(0xFF1A1100)
    val Live = Color(0xFFFF4D4F)
}

private val scheme = darkColorScheme(
    primary = Palette.Amber,
    onPrimary = Palette.OnAmber,
    background = Palette.Ink,
    onBackground = Palette.Text,
    surface = Palette.Panel,
    onSurface = Palette.Text,
)

@Composable
fun VaartaluTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
