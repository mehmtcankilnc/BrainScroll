package com.mehmtcan.brainscroll.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Semantic color tokens (docs/design.md section 2.2). Components read these, never raw hex. */
@Immutable
data class BrainScrollColors(
    val bgPage: Color,
    val bgSurface: Color,
    val bgRaised: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val correctFill: Color,
    val correctOn: Color,
    val pendingFill: Color,
    val pendingOn: Color,
    val absentFill: Color,
    val absentOn: Color,
    val error: Color,
    val streakDay: Color,
    val streakAnswer: Color,
    /** The color of the app's own controls (buttons, switches, the tab bar). Not the "correct" of a puzzle: that stays green. */
    val accent: Color,
    val accentOn: Color,
)

val DarkColors = BrainScrollColors(
    bgPage = Palette.Neutral900,
    bgSurface = Palette.Neutral800,
    bgRaised = Palette.Neutral700,
    borderSubtle = Palette.Neutral500.copy(alpha = 0.55f),
    borderStrong = Palette.Neutral300,
    textPrimary = Palette.Neutral50,
    textSecondary = Palette.Neutral300,
    textDisabled = Palette.Neutral400,
    correctFill = Palette.Green500,
    correctOn = Palette.Green900,
    pendingFill = Palette.Yellow500,
    pendingOn = Palette.Yellow900,
    absentFill = Palette.Neutral800,
    absentOn = Palette.Neutral400,
    error = Palette.Red500,
    streakDay = Palette.Orange500,
    streakAnswer = Palette.Blue500,
    accent = Palette.Green500,
    accentOn = Palette.Green900,
)

/** The accent colors Pro offers for the app's controls (docs/pro-plan.md section 8.5). The meaning colors of the games never change. */
enum class Accent(val id: String) {
    Green("green"),
    Blue("blue"),
    Orange("orange"),
    Yellow("yellow");

    companion object {
        fun fromId(id: String?): Accent = entries.firstOrNull { it.id == id } ?: Green
    }
}

internal val Accent.fill: Color
    get() = when (this) {
        Accent.Green -> Palette.Green500
        Accent.Blue -> Palette.Blue500
        Accent.Orange -> Palette.Orange500
        Accent.Yellow -> Palette.Yellow500
    }

internal val Accent.on: Color
    get() = when (this) {
        Accent.Green -> Palette.Green900
        Accent.Blue -> Palette.Blue900
        Accent.Orange -> Palette.Orange900
        Accent.Yellow -> Palette.Yellow900
    }

fun BrainScrollColors.withAccent(accent: Accent) = copy(accent = accent.fill, accentOn = accent.on)

/** Pure black behind everything, for OLED screens. Cards keep their gray, so they still stand out. */
fun BrainScrollColors.amoled() = copy(bgPage = Color.Black)

/** Text styles (docs/design.md section 3). Color is left unset, callers choose it. */
@Immutable
data class BrainScrollTypography(
    val fontFamily: FontFamily = FontFamily.Default,
) {
    val display = TextStyle(fontFamily = fontFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    val title = TextStyle(fontFamily = fontFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    val heading = TextStyle(fontFamily = fontFamily, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    val body = TextStyle(fontFamily = fontFamily, fontSize = 16.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontFamily = fontFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val caption = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal)
    val tile = TextStyle(fontFamily = fontFamily, fontSize = 22.sp, fontWeight = FontWeight.Medium)
}

/** Spacing scale, 4 dp unit (docs/design.md section 4). */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Corner radii (docs/design.md section 4). */
object Radius {
    val tile = RoundedCornerShape(8.dp)
    val control = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(16.dp)

    /** A panel sliding up from the bottom edge: round on top, flat where it meets the screen. */
    val sheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/**
 * The same palette with brighter secondary text and stronger outlines, for players who turned on the system's
 * contrast setting (docs/design.md section 9). Fill colors keep their meaning, so nothing is learned twice.
 */
fun BrainScrollColors.highContrast() = copy(
    textSecondary = Palette.Neutral100,
    textDisabled = Palette.Neutral200,
    absentOn = Palette.Neutral200,
    borderSubtle = Palette.Neutral200,
    borderStrong = Palette.Neutral50,
)

/** True when the high-contrast setting is on: outlines get thicker too. */
val LocalHighContrast = staticCompositionLocalOf { false }

val LocalBrainScrollColors = staticCompositionLocalOf { DarkColors }
val LocalBrainScrollTypography = staticCompositionLocalOf { BrainScrollTypography() }
