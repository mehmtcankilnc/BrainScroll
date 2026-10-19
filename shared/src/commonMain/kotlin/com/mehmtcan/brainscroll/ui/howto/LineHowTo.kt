package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_line_1
import brainscroll.shared.generated.resources.howto_card_line_2
import brainscroll.shared.generated.resources.howto_card_line_3
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the one line card: draw from 1 through every square, drag or tap to go on, drag back to shorten. */
internal val LineHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_line_1, { t -> LineDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_line_2, { t -> LineDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_line_3, { t -> LineDemo(step = 2, t = t) }),
    ),
)

private const val N = 4

// The way through every square: along the first row, back along the second, and so on.
private val Way = listOf(
    0, 1, 2, 3,
    7, 6, 5, 4,
    8, 9, 10, 11,
    15, 14, 13, 12,
)

// The numbered squares, in the order the line must pass them: the 1 is the start, the last number is the last square.
private val Numbers = listOf(0, 7, 8, 12)

@Composable
internal fun LineDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    // How many squares the line has at time t, and where the hand is, per step.
    fun squares(t: Float): Float = when (step) {
        // A drag from the 1 along the first two rows.
        0 -> 1f + 7f * demoEase(demoSegment(t, 0.3f, 0.78f))
        // Taps on the squares next to the end: one square for each tap.
        1 -> 8f + (if (t >= 0.28f) 1f else 0f) + (if (t >= 0.5f) 1f else 0f) + (if (t >= 0.72f) 1f else 0f)
        // The line is long, and a drag back over it takes squares away.
        else -> 11f - 4f * demoEase(demoSegment(t, 0.4f, 0.8f))
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val side = minOf(size.width, size.height) * 0.84f
        val cell = side / N
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        fun center(index: Int) = Offset(left + (index % N + 0.5f) * cell, top + (index / N + 0.5f) * cell)
        val gap = cell * 0.04f
        val count = squares(t)
        val whole = count.toInt()
        val onLine = Way.take(whole).toSet()

        for (index in 0 until N * N) {
            val o = Offset(left + (index % N) * cell + gap, top + (index / N) * cell + gap)
            val box = Size(cell - 2 * gap, cell - 2 * gap)
            drawRoundRect((if (index in onLine) colors.correctFill.copy(alpha = 0.16f) else colors.bgSurface).copy(alpha = if (index in onLine) 0.16f * fade else fade), o, box, CornerRadius(cell * 0.16f))
            drawRoundRect(colors.borderStrong.copy(alpha = 0.3f * fade), o, box, CornerRadius(cell * 0.16f), style = Stroke(1.dp.toPx()))
        }
        // The line: through the centers of the squares, to the head that is between two squares while it moves.
        val points = Way.take(whole).map { center(it) }.toMutableList()
        if (whole < Way.size && count > whole) {
            val from = center(Way[whole - 1])
            val to = center(Way[whole])
            val f = count - whole
            points += Offset(from.x + (to.x - from.x) * f, from.y + (to.y - from.y) * f)
        }
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(path, colors.correctFill.copy(alpha = 0.6f * fade), style = Stroke(cell * 0.34f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (points.isNotEmpty()) drawCircle(colors.correctFill.copy(alpha = fade), cell * 0.2f, points.last())

        // The numbers: the ones the line has passed are green.
        Numbers.forEachIndexed { i, index ->
            val passed = Way.indexOf(index) < whole
            val c = center(index)
            drawCircle((if (passed) colors.correctFill else colors.textPrimary).copy(alpha = fade), cell * 0.3f, c)
            drawCenteredText(measurer, (i + 1).toString(), c, cell * 0.34f, (if (passed) colors.correctOn else colors.bgPage).copy(alpha = fade))
        }

        // The hand: it carries the head of the line in a drag; it taps the squares next to the end; it drags back.
        val offscreen = Offset(size.width, size.height)
        val hand: Offset
        var press = 0f
        when (step) {
            0 -> {
                val head = points.lastOrNull() ?: center(0)
                val arrive = demoEase(demoSegment(t, 0.05f, 0.28f))
                hand = if (t < 0.3f) Offset(offscreen.x + (center(0).x - offscreen.x) * arrive, offscreen.y + (center(0).y - offscreen.y) * arrive) else head
                press = demoSegment(t, 0.27f, 0.34f)
            }
            1 -> {
                val taps = listOf(0.28f to Way[8], 0.5f to Way[9], 0.72f to Way[10]).map { (at, i) -> DemoTap(at, center(i)) }
                val (h2, p) = demoHand(t, taps, offscreen)
                hand = h2
                press = p
            }
            else -> {
                val head = points.lastOrNull() ?: center(Way[10])
                val arrive = demoEase(demoSegment(t, 0.08f, 0.38f))
                hand = if (t < 0.4f) Offset(offscreen.x + (center(Way[10]).x - offscreen.x) * arrive, offscreen.y + (center(Way[10]).y - offscreen.y) * arrive) else head
                press = demoSegment(t, 0.36f, 0.43f)
            }
        }
        drawHandCursor(handPainter, hand, cell * 0.95f, press, alpha = fade)
    }
}
