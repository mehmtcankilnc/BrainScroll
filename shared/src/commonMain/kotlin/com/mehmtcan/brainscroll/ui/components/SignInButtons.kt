package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

// The colors of the two buttons are the providers' own, not ours: players know these buttons and look for them.
// Apple's guideline for a dark screen is a white button with a black logo and text, and for it to be at least as
// prominent as any other way to sign in. Google's dark-theme button is a near-black pill with a thin grey edge, white
// text and the four-color "G".
private val AppleFill = Color(0xFFFFFFFF)
private val AppleInk = Color(0xFF000000)
private val GoogleFill = Color(0xFF131314)
private val GoogleEdge = Color(0xFF8E918F)
private val GoogleInk = Color(0xFFE3E3E3)

/** "Sign in with Apple", in Apple's look for a dark screen: a filled white button. Put it first. */
@Composable
fun AppleSignInButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ProviderButton(
        text = text,
        onClick = onClick,
        fill = AppleFill,
        edge = null,
        ink = AppleInk,
        mark = { AppleMark(AppleInk) },
        modifier = modifier,
    )
}

/** "Sign in with Google", in Google's look for a dark screen: dark with a grey edge, a step quieter than the Apple button. */
@Composable
fun GoogleSignInButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ProviderButton(
        text = text,
        onClick = onClick,
        fill = GoogleFill,
        edge = GoogleEdge,
        ink = GoogleInk,
        mark = { GoogleMark() },
        modifier = modifier,
    )
}

@Composable
private fun ProviderButton(
    text: String,
    onClick: () -> Unit,
    fill: Color,
    edge: Color?,
    ink: Color,
    mark: @Composable () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(fill, Radius.control)
            .then(if (edge != null) Modifier.border(1.dp, edge, Radius.control) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally),
    ) {
        mark()
        BasicText(text, style = BrainScrollTheme.typography.label.copy(color = ink, fontWeight = FontWeight.SemiBold))
    }
}

// The marks are drawn from their outlines (the apple on a 384 x 512 grid, the "G" on a 48 x 48 one).
private const val APPLE_PATH =
    "M318.7 268.7c-.2-36.7 16.4-64.4 50-84.8-18.8-26.9-47.2-41.7-84.7-44.6-35.5-2.8-74.3 20.7-88.5 20.7-15 0-49.4-19.7-76.4-19.7" +
        "C63.3 141.2 4 184.8 4 273.5q0 39.3 14.4 81.2c12.8 36.7 59 126.7 107.2 125.2 25.2-.6 43-17.9 75.8-17.9 31.8 0 48.3 17.9 76.4 17.9" +
        " 48.6-.7 90.4-82.5 102.6-119.3-65.2-30.7-61.7-90-61.7-91.9zm-56.6-164.2c27.3-32.4 24.8-61.9 24-72.5-24.1 1.4-52 16.4-67.9 34.9" +
        "-17.5 19.8-27.8 44.3-25.6 71.9 26.1 2 49.9-11.4 69.5-34.3z"

private const val G_BLUE =
    "M45.12 24.5c0-1.56-.14-3.06-.4-4.5H24v8.51h11.84c-.51 2.75-2.06 5.08-4.39 6.64v5.52h7.11c4.16-3.83 6.56-9.47 6.56-16.17z"
private const val G_GREEN =
    "M24 46c5.94 0 10.92-1.97 14.56-5.33l-7.11-5.52c-1.97 1.32-4.49 2.1-7.45 2.1-5.73 0-10.58-3.87-12.31-9.07H4.34v5.7C7.96 41.07 15.4 46 24 46z"
private const val G_YELLOW =
    "M11.69 28.18C11.25 26.86 11 25.45 11 24s.25-2.86.69-4.18v-5.7H4.34C2.85 17.09 2 20.45 2 24c0 3.55.85 6.91 2.34 9.88l7.35-5.7z"
private const val G_RED =
    "M24 10.75c3.23 0 6.13 1.11 8.41 3.29l6.31-6.31C34.91 4.18 29.93 2 24 2 15.4 2 7.96 6.93 4.34 14.12l7.35 5.7c1.73-5.2 6.58-9.07 12.31-9.07z"

@Composable
private fun AppleMark(color: Color) {
    val path = remember { PathParser().parsePathString(APPLE_PATH).toPath() }
    Canvas(Modifier.size(width = 18.dp, height = 22.dp)) {
        scale(size.width / 384f, size.height / 512f, pivot = Offset.Zero) { drawPath(path, color) }
    }
}

@Composable
private fun GoogleMark() {
    val parts = remember {
        listOf(
            PathParser().parsePathString(G_BLUE).toPath() to Color(0xFF4285F4),
            PathParser().parsePathString(G_GREEN).toPath() to Color(0xFF34A853),
            PathParser().parsePathString(G_YELLOW).toPath() to Color(0xFFFBBC05),
            PathParser().parsePathString(G_RED).toPath() to Color(0xFFEA4335),
        )
    }
    Canvas(Modifier.size(20.dp)) {
        scale(size.width / 48f, size.height / 48f, pivot = Offset.Zero) { parts.forEach { (path, color) -> drawPath(path, color) } }
    }
}
