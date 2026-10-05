package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/**
 * The favorite heart. Filled when the puzzle is a favorite, an outline otherwise, so the state does not
 * depend on color alone (docs/design.md section 9). The touch area is 48 dp even though the drawing is smaller.
 */
@Composable
fun HeartButton(
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (filled) BrainScrollTheme.colors.error else BrainScrollTheme.colors.textSecondary
    Box(
        modifier = modifier
            .testTag("favoriteHeart")
            .size(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            val heart = Path().apply {
                moveTo(0.5f * w, 0.9f * h)
                cubicTo(0.02f * w, 0.55f * h, 0.08f * w, 0.08f * h, 0.5f * w, 0.30f * h)
                cubicTo(0.92f * w, 0.08f * h, 0.98f * w, 0.55f * h, 0.5f * w, 0.9f * h)
                close()
            }
            drawPath(heart, color, style = if (filled) Fill else Stroke(width = 2.dp.toPx()))
        }
    }
}
