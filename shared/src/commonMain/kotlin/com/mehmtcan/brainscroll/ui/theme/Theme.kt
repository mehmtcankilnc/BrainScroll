package com.mehmtcan.brainscroll.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.nunito_bold
import brainscroll.shared.generated.resources.nunito_medium
import brainscroll.shared.generated.resources.nunito_regular
import org.jetbrains.compose.resources.Font

/** Bundled Nunito (OFL). `Font(...)` is composable because loading depends on the platform. */
@Composable
private fun nunitoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.nunito_regular, FontWeight.Normal),
    Font(Res.font.nunito_medium, FontWeight.Medium),
    Font(Res.font.nunito_bold, FontWeight.Bold),
)

/**
 * Wraps the app and makes colors and typography available to every composable below it
 * without passing them as parameters (CompositionLocal).
 */
@Composable
fun BrainScrollTheme(content: @Composable () -> Unit) {
    val fontFamily = nunitoFontFamily()
    val typography = remember(fontFamily) { BrainScrollTypography(fontFamily) }

    CompositionLocalProvider(
        LocalBrainScrollColors provides DarkColors,
        LocalBrainScrollTypography provides typography,
        content = content,
    )
}

/** Access point: `BrainScrollTheme.colors.correctFill`, `BrainScrollTheme.typography.title`. */
object BrainScrollTheme {
    val colors: BrainScrollColors
        @Composable @ReadOnlyComposable get() = LocalBrainScrollColors.current

    val typography: BrainScrollTypography
        @Composable @ReadOnlyComposable get() = LocalBrainScrollTypography.current
}
