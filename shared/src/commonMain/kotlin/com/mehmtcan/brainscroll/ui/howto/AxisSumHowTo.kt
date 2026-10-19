package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_axissum_1
import brainscroll.shared.generated.resources.howto_card_axissum_2
import brainscroll.shared.generated.resources.howto_card_axissum_3
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the axis sum card: a tap erases a number (and brings it back), the sums on the edge turn green, solve the board. */
internal val AxisSumHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_axissum_1, { t -> AxisSumDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_axissum_2, { t -> AxisSumDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_axissum_3, { t -> AxisSumDemo(step = 2, t = t) }),
    ),
)

private const val N = 3
private val Digits = listOf(4, 7, 2, 6, 1, 8, 3, 9, 5)

// The one way to erase: the 7 and the 8. The sums the rest makes.
private val RowTargets = listOf(6, 7, 17)
private val ColumnTargets = listOf(13, 10, 7)

/** A tap of the hand on a number at time [at]: it is erased, and brought back at [backAt] when that is not negative. */
private class DemoErase(val cell: Int, val at: Float, val backAt: Float = -1f)

@Composable
internal fun AxisSumDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    val taps: List<DemoErase> = when (step) {
        0 -> listOf(DemoErase(3, 0.25f, backAt = 0.65f))
        1 -> listOf(DemoErase(1, 0.3f))
        else -> listOf(DemoErase(1, 0.15f), DemoErase(5, 0.5f))
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.1f
        val gap = size.width * 0.03f
        val cell = minOf((size.width - 2 * pad - gap * N) / (N + 1), (size.height - 2 * pad - gap * N) / (N + 1))
        val total = cell * (N + 1) + gap * N
        val left = (size.width - total) / 2f
        val top = (size.height - total) / 2f
        fun origin(row: Int, column: Int) = Offset(left + column * (cell + gap), top + row * (cell + gap))
        fun center(row: Int, column: Int) = origin(row, column) + Offset(cell / 2f, cell / 2f)
        val corner = CornerRadius(cell * 0.16f)

        // How present each number is now (1 there, 0 erased): a tap moves it to the other state, quickly.
        val present = FloatArray(N * N) { 1f }
        for (e in taps) {
            var p = 1f
            if (t >= e.at) p = 1f - demoEase(demoSegment(t, e.at, e.at + 0.06f))
            if (e.backAt >= 0f && t >= e.backAt) p = demoEase(demoSegment(t, e.backAt, e.backAt + 0.06f))
            present[e.cell] = p
        }
        val rowSums = IntArray(N) { r -> (0 until N).sumOf { c -> if (present[r * N + c] < 0.5f) 0 else Digits[r * N + c] } }
        val columnSums = IntArray(N) { c -> (0 until N).sumOf { r -> if (present[r * N + c] < 0.5f) 0 else Digits[r * N + c] } }
        val solved = (0 until N).all { rowSums[it] == RowTargets[it] && columnSums[it] == ColumnTargets[it] }

        // The sums on the edge: green when right, red when too much was erased.
        for (c in 0 until N) {
            val color = when {
                columnSums[c] < ColumnTargets[c] -> colors.error
                columnSums[c] == ColumnTargets[c] -> colors.correctFill
                else -> colors.textPrimary
            }
            drawRoundRect(colors.bgRaised.copy(alpha = fade), origin(0, c + 1), Size(cell, cell), corner)
            drawCenteredText(measurer, ColumnTargets[c].toString(), center(0, c + 1), cell * 0.42f, color.copy(alpha = fade))
        }
        for (r in 0 until N) {
            val color = when {
                rowSums[r] < RowTargets[r] -> colors.error
                rowSums[r] == RowTargets[r] -> colors.correctFill
                else -> colors.textPrimary
            }
            drawRoundRect(colors.bgRaised.copy(alpha = fade), origin(r + 1, 0), Size(cell, cell), corner)
            drawCenteredText(measurer, RowTargets[r].toString(), center(r + 1, 0), cell * 0.42f, color.copy(alpha = fade))
        }
        for (r in 0 until N) for (c in 0 until N) {
            val i = r * N + c
            val p = present[i]
            val fill = lerp(colors.bgSurface, if (solved && p >= 0.5f) colors.correctFill else colors.bgSurface, 1f)
            drawRoundRect(fill.copy(alpha = (0.15f + 0.85f * p) * fade), origin(r + 1, c + 1), Size(cell, cell), corner)
            drawRoundRect(colors.borderStrong.copy(alpha = 0.35f * fade), origin(r + 1, c + 1), Size(cell, cell), corner, style = Stroke(width = 1.5.dp.toPx()))
            val ink = if (solved && p >= 0.5f) colors.correctOn else colors.textPrimary
            drawCenteredText(measurer, Digits[i].toString(), center(r + 1, c + 1), cell * 0.46f, ink.copy(alpha = (0.18f + 0.82f * p) * fade))
        }

        val stops = taps.flatMap { e ->
            val point = center(e.cell / N + 1, e.cell % N + 1)
            if (e.backAt >= 0f) listOf(DemoTap(e.at, point), DemoTap(e.backAt, point)) else listOf(DemoTap(e.at, point))
        }
        val (hand, press) = demoHand(t, stops, Offset(size.width, size.height))
        drawHandCursor(handPainter, hand, cell * 0.95f, press, alpha = fade)
    }
}
