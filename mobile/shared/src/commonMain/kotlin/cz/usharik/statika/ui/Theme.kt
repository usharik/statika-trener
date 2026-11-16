package cz.usharik.statika.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Barvy převzaté z webové verze (src/index.css), světlé i tmavé téma. */
@Immutable
data class StatikaColors(
    val dark: Boolean,
    val bg: Color,
    val paper: Color,
    val ink: Color,
    val inkSoft: Color,
    val line: Color,
    val ext: Color,
    val beam: Color,
    val load: Color,
    val loadFill: Color,
    val react: Color,
    val derived: Color,
    val accent: Color,
    val subtle: Color,
    val okBg: Color,
    val nudge: Color,
    val nudgeBg: Color,
    val warnBg: Color,
    val warnLine: Color,
    val hintBg: Color,
    val infoBg: Color,
    val infoIcon: Color,
    val chipReactBorder: Color,
    val chipLoadBorder: Color,
    val activeBorder: Color,
    val focusBg: Color,
    val star: Color,
    val highlight: Color,
)

val LightColors = StatikaColors(
    dark = false,
    bg = Color(0xFFF5F6F8), paper = Color(0xFFFFFFFF), ink = Color(0xFF1B2330), inkSoft = Color(0xFF6B7482),
    line = Color(0xFFE3E6EB), ext = Color(0xFFC9CED6), beam = Color(0xFFEEF1F5),
    load = Color(0xFFD1432B), loadFill = Color(0x0FD1432B), react = Color(0xFF2459D6), derived = Color(0xFF7C3AED),
    accent = Color(0xFF0F9D6E), subtle = Color(0xFFF6F7F9), okBg = Color(0xFFECF8F2),
    nudge = Color(0xFFF3BD55), nudgeBg = Color(0xFFFFF8EB), warnBg = Color(0xFFFFF1D6), warnLine = Color(0xFFF0B43C),
    hintBg = Color(0xFFEEF3FF), infoBg = Color(0xFFF3F4F6), infoIcon = Color(0xFF9AA3AF),
    chipReactBorder = Color(0xFFC9D6F5), chipLoadBorder = Color(0xFFF1CFC8), activeBorder = Color(0xFF9DB4EE),
    focusBg = Color(0xFFF6F9FF), star = Color(0xFFF5B301), highlight = Color(0xF2FFBA00),
)

val DarkColors = StatikaColors(
    dark = true,
    bg = Color(0xFF0F1216), paper = Color(0xFF181C22), ink = Color(0xFFE6E9EE), inkSoft = Color(0xFF98A2B0),
    line = Color(0xFF2B3139), ext = Color(0xFF3A424D), beam = Color(0xFF2A313A),
    load = Color(0xFFFF7B5F), loadFill = Color(0x17FF7B5F), react = Color(0xFF6F9FFF), derived = Color(0xFFB692FF),
    accent = Color(0xFF36C48F), subtle = Color(0xFF1F242B), okBg = Color(0xFF15271F),
    nudge = Color(0xFFE0A53A), nudgeBg = Color(0xFF2A2317), warnBg = Color(0xFF3A2E14), warnLine = Color(0xFFD19A2A),
    hintBg = Color(0xFF1A2438), infoBg = Color(0xFF22272E), infoIcon = Color(0xFF6B7482),
    chipReactBorder = Color(0xFF2F4675), chipLoadBorder = Color(0xFF5C3128), activeBorder = Color(0xFF3F5FA6),
    focusBg = Color(0xFF1B2330), star = Color(0xFFF5B301), highlight = Color(0xF2FFBA00),
)

val LocalColors = staticCompositionLocalOf { LightColors }

@Composable
fun StatikaTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.ink, onPrimary = Color(0xFF11151A), secondary = c.react, tertiary = c.accent,
            background = c.bg, onBackground = c.ink, surface = c.paper, onSurface = c.ink,
            surfaceVariant = c.subtle, onSurfaceVariant = c.inkSoft, outline = c.line, outlineVariant = c.line,
            surfaceContainerLow = c.paper, surfaceContainer = c.paper, surfaceContainerHigh = c.subtle,
        )
    } else {
        lightColorScheme(
            primary = c.ink, onPrimary = Color.White, secondary = c.react, tertiary = c.accent,
            background = c.bg, onBackground = c.ink, surface = c.paper, onSurface = c.ink,
            surfaceVariant = c.subtle, onSurfaceVariant = c.inkSoft, outline = c.line, outlineVariant = c.line,
            surfaceContainerLow = c.paper, surfaceContainer = c.paper, surfaceContainerHigh = c.subtle,
        )
    }
    CompositionLocalProvider(LocalColors provides c) {
        MaterialTheme(colorScheme = scheme) {
            // výchozí barva textu podle tématu – text bez výslovné barvy nesmí být v tmavém tématu černý
            CompositionLocalProvider(LocalContentColor provides c.ink, content = content)
        }
    }
}
