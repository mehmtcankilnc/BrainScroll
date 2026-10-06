package com.mehmtcan.brainscroll.ui.memory

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.ui.theme.BrainScrollColors
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws one of the eight card shapes in [color], filling the size it is given. The shapes are drawn in code, so they
 * look the same on every platform. Each is a different shape (docs/design.md: content is told apart by shape, not by
 * color); the colors only make them easier to enjoy.
 */
@Composable
fun CardShape(symbol: CardSymbol, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawSymbol(symbol, color) }
}

/** The color a shape has while its card is face up. A matched card draws its shape in one dark color instead. */
@Composable
fun symbolColor(symbol: CardSymbol): Color = symbolColor(symbol, BrainScrollTheme.colors)

fun symbolColor(symbol: CardSymbol, colors: BrainScrollColors): Color = when (symbol) {
    CardSymbol.Circle -> colors.streakAnswer
    CardSymbol.Triangle -> colors.pendingFill
    CardSymbol.Star -> colors.streakDay
    CardSymbol.Heart -> colors.error
    CardSymbol.Diamond -> colors.correctFill
    CardSymbol.Square -> colors.textPrimary
    CardSymbol.Plus -> colors.textSecondary
    CardSymbol.Moon -> colors.borderStrong
}

private fun DrawScope.drawSymbol(symbol: CardSymbol, color: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val r = min(size.width, size.height) / 2f
    when (symbol) {
        CardSymbol.Circle -> drawCircle(color, radius = r * 0.82f, center = center)

        CardSymbol.Triangle -> drawPath(
            Path().apply {
                moveTo(center.x, center.y - r * 0.88f)
                lineTo(center.x + r * 0.95f, center.y + r * 0.72f)
                lineTo(center.x - r * 0.95f, center.y + r * 0.72f)
                close()
            },
            color,
        )

        CardSymbol.Star -> drawPath(starPath(center, outer = r * 0.95f, inner = r * 0.42f), color)

        CardSymbol.Heart -> drawPath(
            Path().apply {
                moveTo(center.x, center.y + r * 0.85f)
                cubicTo(center.x - r * 1.05f, center.y + r * 0.15f, center.x - r * 0.9f, center.y - r * 0.85f, center.x, center.y - r * 0.35f)
                cubicTo(center.x + r * 0.9f, center.y - r * 0.85f, center.x + r * 1.05f, center.y + r * 0.15f, center.x, center.y + r * 0.85f)
                close()
            },
            color,
        )

        CardSymbol.Diamond -> drawPath(
            Path().apply {
                moveTo(center.x, center.y - r * 0.95f)
                lineTo(center.x + r * 0.7f, center.y)
                lineTo(center.x, center.y + r * 0.95f)
                lineTo(center.x - r * 0.7f, center.y)
                close()
            },
            color,
        )

        CardSymbol.Square -> {
            val side = r * 1.4f
            drawRoundRect(color, topLeft = Offset(center.x - side / 2, center.y - side / 2), size = Size(side, side), cornerRadius = CornerRadius(r * 0.18f))
        }

        CardSymbol.Plus -> {
            val long = r * 1.7f
            val thick = r * 0.55f
            drawRoundRect(color, Offset(center.x - long / 2, center.y - thick / 2), Size(long, thick), CornerRadius(thick / 3))
            drawRoundRect(color, Offset(center.x - thick / 2, center.y - long / 2), Size(thick, long), CornerRadius(thick / 3))
        }

        CardSymbol.Moon -> {
            // A big circle with a smaller one cut out of it, offset to the upper right: a crescent.
            val full = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, r * 0.85f)) }
            val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(center.x + r * 0.42f, center.y - r * 0.18f), r * 0.72f)) }
            drawPath(Path.combine(PathOperation.Difference, full, cut), color)
        }
    }
}

private fun starPath(center: Offset, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 5
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
