package com.drivehome.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.drivehome.launcher.R

/** Instrument-cluster display face: numbers, headings, labels. Variable font, one file. */
val Orbitron = FontFamily(
    Font(R.font.orbitron, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.orbitron, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.orbitron, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

/** Condensed technical face for everything else; also covers Devanagari song titles. */
val Rajdhani = FontFamily(
    Font(R.font.rajdhani_medium, FontWeight.Normal),
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)

/** Small uppercase cluster label, e.g. "NOW PLAYING". */
fun labelStyle(size: TextUnit = 12.sp) = TextStyle(
    fontFamily = Orbitron,
    fontWeight = FontWeight.Bold,
    fontSize = size,
    letterSpacing = 2.sp,
)

val DriveTypography: Typography = Typography().let { t ->
    fun TextStyle.r() = copy(fontFamily = Rajdhani, fontWeight = FontWeight.Medium)
    t.copy(
        displayLarge = t.displayLarge.r(), displayMedium = t.displayMedium.r(), displaySmall = t.displaySmall.r(),
        headlineLarge = t.headlineLarge.r(), headlineMedium = t.headlineMedium.r(), headlineSmall = t.headlineSmall.r(),
        titleLarge = t.titleLarge.r(), titleMedium = t.titleMedium.r(), titleSmall = t.titleSmall.r(),
        bodyLarge = t.bodyLarge.r(), bodyMedium = t.bodyMedium.r(), bodySmall = t.bodySmall.r(),
        labelLarge = t.labelLarge.r(), labelMedium = t.labelMedium.r(), labelSmall = t.labelSmall.r(),
    )
}
