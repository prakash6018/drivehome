package com.drivehome.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.drivehome.launcher.data.ThemeMode
import java.time.LocalTime

/**
 * Both palettes are dark: a bright screen in a car cabin causes glare and windscreen
 * reflections. "Night" goes true-black and lowers text/accent brightness further.
 * Surfaces are drawn translucent (see Glass) over the ambient backdrop.
 */
data class DrivePalette(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val onAccent: Color,
    val isNight: Boolean,
)

data class Accent(val name: String, val color: Color)

val Accents = listOf(
    Accent("Electric Blue", Color(0xFF3DA5FF)),
    Accent("Rosso Red", Color(0xFFFF3B3B)),
    Accent("Sport Amber", Color(0xFFFFB020)),
    Accent("Acid Lime", Color(0xFFB4FF39)),
)

/** Gauge redline, independent of the accent. */
val Redline = Color(0xFFFF3B3B)

fun dayPalette(accent: Color) = DrivePalette(
    background = Color(0xFF05070B),
    surface = Color(0xFF161B24),
    surfaceHigh = Color(0xFF232A35),
    outline = Color(0xFF2C3440),
    text = Color(0xFFF4F6FA),
    textDim = Color(0xFF9AA4B2),
    accent = accent,
    onAccent = Color(0xFF0A0D12),
    isNight = false,
)

fun nightPalette(accent: Color) = DrivePalette(
    background = Color.Black,
    surface = Color(0xFF0E1218),
    surfaceHigh = Color(0xFF181D25),
    outline = Color(0xFF1C222B),
    text = Color(0xFFC5CBD3),
    textDim = Color(0xFF7A838F),
    accent = lerp(accent, Color.Black, 0.25f),
    onAccent = Color.Black,
    isNight = true,
)

fun isNight(mode: ThemeMode, now: LocalTime): Boolean = when (mode) {
    ThemeMode.DAY -> false
    ThemeMode.NIGHT -> true
    ThemeMode.AUTO -> now >= LocalTime.of(18, 30) || now < LocalTime.of(6, 30)
}

val LocalPalette = staticCompositionLocalOf { dayPalette(Accents[0].color) }

@Composable
fun DriveHomeTheme(palette: DrivePalette, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        background = palette.background,
        onBackground = palette.text,
        surface = palette.surface,
        onSurface = palette.text,
        surfaceVariant = palette.surfaceHigh,
        onSurfaceVariant = palette.textDim,
        outline = palette.outline,
    )
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = DriveTypography, content = content)
    }
}
