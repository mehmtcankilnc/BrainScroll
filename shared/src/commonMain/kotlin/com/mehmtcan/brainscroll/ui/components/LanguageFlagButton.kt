package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The round flag in the top right corner of a word puzzle: the language of the puzzles. One tap switches to the other
 * language. The flags are drawn, not emoji, so they look the same on every device.
 *
 * NOTE: with a third language a tap that switches between two is not enough. Then this
 * becomes a small menu (a modal list of flags and names) that opens right where this flag is, anchored to it with a
 * popup, not centered on the screen and not as a bottom sheet.
 */
@Composable
fun LanguageFlagButton(language: Language, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    Box(
        modifier = modifier
            .testTag("languageFlag")
            .size(48.dp)
            .semantics { contentDescription = description }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(38.dp).clip(CircleShape)) {
            when (language) {
                Language.TR -> drawTurkey()
                Language.EN -> drawBritain()
            }
        }
        Box(Modifier.size(38.dp).border(2.dp, colors.borderStrong.copy(alpha = 0.6f), CircleShape))
    }
}

private fun DrawScope.drawTurkey() {
    val red = Color(0xFFE30A17)
    drawRect(red)
    val r = size.minDimension
    // The crescent: a white disc with a smaller red disc cut into it, then the star.
    drawCircle(Color.White, radius = r * 0.26f, center = Offset(r * 0.43f, r * 0.5f))
    drawCircle(red, radius = r * 0.21f, center = Offset(r * 0.49f, r * 0.5f))
    val star = Path()
    val c = Offset(r * 0.66f, r * 0.5f)
    val outer = r * 0.1f
    val inner = outer * 0.4f
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = (-90.0 + i * 36.0) * PI / 180.0
        val x = c.x + radius * cos(angle).toFloat()
        val y = c.y + radius * sin(angle).toFloat()
        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
    }
    star.close()
    drawPath(star, Color.White)
}

private fun DrawScope.drawBritain() {
    val blue = Color(0xFF012169)
    val red = Color(0xFFC8102E)
    val w = size.width
    val h = size.height
    drawRect(blue)
    drawLine(Color.White, Offset(0f, 0f), Offset(w, h), strokeWidth = w * 0.2f)
    drawLine(Color.White, Offset(w, 0f), Offset(0f, h), strokeWidth = w * 0.2f)
    drawLine(red, Offset(0f, 0f), Offset(w, h), strokeWidth = w * 0.07f)
    drawLine(red, Offset(w, 0f), Offset(0f, h), strokeWidth = w * 0.07f)
    drawRect(Color.White, Offset(w * 0.4f, 0f), Size(w * 0.2f, h))
    drawRect(Color.White, Offset(0f, h * 0.4f), Size(w, h * 0.2f))
    drawRect(red, Offset(w * 0.45f, 0f), Size(w * 0.1f, h))
    drawRect(red, Offset(0f, h * 0.45f), Size(w, h * 0.1f))
}
