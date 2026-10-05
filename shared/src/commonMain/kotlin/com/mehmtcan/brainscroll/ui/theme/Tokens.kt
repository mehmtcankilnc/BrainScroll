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
)

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
    val pill = RoundedCornerShape(percent = 50)
}

val LocalBrainScrollColors = staticCompositionLocalOf { DarkColors }
val LocalBrainScrollTypography = staticCompositionLocalOf { BrainScrollTypography() }
