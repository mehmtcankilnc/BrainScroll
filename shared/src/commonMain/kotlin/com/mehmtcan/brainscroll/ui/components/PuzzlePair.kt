package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.textSoft

/**
 * A small drawing for the friends page when there is nobody yet: two puzzle pieces side by side, each with sleepy smiling eyes, joined by
 * a green knob, with three little sparks over the joint. It is drawn with lines in the theme's colors on a 96 by 64 grid.
 */
@Composable
fun PuzzlePair(modifier: Modifier = Modifier, width: Dp = 128.dp) {
    val colors = BrainScrollTheme.colors
    val line = colors.textSoft
    val green = colors.accent
    Canvas(modifier.size(width = width, height = width * (66f / 96f))) {
        val u = size.width / 96f
        val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // One piece with a knob on top: left is the x of its left edge.
        fun piece(left: Float): Path = Path().apply {
            val right = left + 38f
            moveTo(left * u, 30f * u)
            lineTo((left + 12f) * u, 30f * u)
            // The knob over the top edge: from 9 o'clock round over the top to 3 o'clock.
            arcTo(Rect((left + 12f) * u, 24f * u, (left + 26f) * u, 36f * u).let { Rect(it.left, it.top, it.right, it.bottom) }, 180f, 180f, false)
            lineTo(right * u, 30f * u)
            lineTo(right * u, 62f * u)
            lineTo(left * u, 62f * u)
            close()
        }
        for (left in listOf(6f, 52f)) {
            drawPath(piece(left), line, style = stroke)
            // Two closed, smiling eyes.
            for (eye in listOf(left + 11f, left + 25f)) {
                val path = Path().apply { arcTo(Rect(eye * u - 4f * u, 42f * u, eye * u + 4f * u, 50f * u), 20f, 140f, true) }
                drawPath(path, line, style = stroke)
            }
        }
        // The green knob that joins them.
        val joint = Path().apply { arcTo(Rect(38f * u, 40f * u, 52f * u, 54f * u), -90f, 180f, true) }
        drawPath(joint, green, style = stroke)
        // Three sparks over the joint.
        drawLine(green, Offset(48f * u, 6f * u), Offset(48f * u, 14f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(green, Offset(38f * u, 10f * u), Offset(42f * u, 16f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(green, Offset(58f * u, 10f * u), Offset(54f * u, 16f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}
