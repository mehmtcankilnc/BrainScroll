package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_queens_start
import brainscroll.shared.generated.resources.howto_queens_tech
import brainscroll.shared.generated.resources.howto_card_queens_1
import brainscroll.shared.generated.resources.howto_card_queens_2
import brainscroll.shared.generated.resources.howto_card_queens_3
import com.mehmtcan.brainscroll.ui.queens.drawCrown
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.math.abs

/** The three steps of the queens card: a tap puts a queen, queens that see each other turn red, and a queen in every row and column. */
internal val QueensHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_queens_1, { t -> QueensDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_queens_2, { t -> QueensDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_queens_3, { t -> QueensDemo(step = 2, t = t) }, cycleMillis = 6400),
        sceneStep(Res.string.howto_queens_tech, QueensReachScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_queens_start, QueensStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private const val SIZE = 4

/** A queen the hand puts in a cell at [at], and takes away again at [removeAt] when that is not negative. */
private class DemoQueen(val row: Int, val column: Int, val at: Float, val removeAt: Float = -1f)

@Composable
internal fun QueensDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    val queens: List<DemoQueen>
    val taps: List<Triple<Float, Int, Int>>
    when (step) {
        0 -> {
            queens = listOf(DemoQueen(0, 1, 0.25f), DemoQueen(2, 0, 0.58f))
            taps = listOf(Triple(0.25f, 0, 1), Triple(0.58f, 2, 0))
        }
        1 -> {
            // The second queen is on a diagonal of the first: both turn red. Tapping it again takes it away.
            queens = listOf(DemoQueen(0, 1, -1f), DemoQueen(1, 2, 0.22f, removeAt = 0.62f))
            taps = listOf(Triple(0.22f, 1, 2), Triple(0.62f, 1, 2))
        }
        else -> {
            val times = listOf(0.1f, 0.28f, 0.46f, 0.64f)
            val solution = listOf(0 to 1, 1 to 3, 2 to 0, 3 to 2)
            queens = solution.mapIndexed { i, (r, c) -> DemoQueen(r, c, times[i]) }
            taps = solution.mapIndexed { i, (r, c) -> Triple(times[i], r, c) }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.07f
        val cell = (size.width - 2 * pad) / SIZE
        fun origin(row: Int, column: Int) = Offset(pad + column * cell, pad + row * cell)
        fun center(row: Int, column: Int) = origin(row, column) + Offset(cell / 2f, cell / 2f)
        val gap = cell * 0.04f
        val corner = CornerRadius(cell * 0.18f)

        fun scaleOf(q: DemoQueen): Float {
            if (q.at < 0f) return 1f
            val pop = demoPop(t, q.at)
            return if (q.removeAt in 0f..t) 1f - demoEase(demoSegment(t, q.removeAt, q.removeAt + 0.06f)) else pop
        }
        val present = queens.filter { scaleOf(it) > 0.01f }
        // A queen is attacked when another shares its row, its column or a diagonal.
        fun attacked(q: DemoQueen) = present.any { o -> o !== q && (o.row == q.row || o.column == q.column || abs(o.row - q.row) == abs(o.column - q.column)) }

        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            val o = origin(r, c)
            val dark = (r + c) % 2 == 1
            val fill = if (dark) colors.borderStrong.copy(alpha = 0.22f) else colors.bgSurface
            val topLeft = Offset(o.x + gap, o.y + gap)
            val tileSize = Size(cell - 2 * gap, cell - 2 * gap)
            drawRoundRect(colors.bgSurface.copy(alpha = fade), topLeft, tileSize, corner)
            drawRoundRect(fill.copy(alpha = fill.alpha * fade), topLeft, tileSize, corner)
            drawRoundRect(colors.borderStrong.copy(alpha = 0.35f * fade), topLeft, tileSize, corner, style = Stroke(width = 1.5.dp.toPx()))
        }
        for (q in queens) {
            val s = scaleOf(q)
            if (s <= 0.01f) continue
            val bad = attacked(q)
            val c = center(q.row, q.column)
            val side = cell * 0.7f
            scale(s, s, pivot = c) {
                if (bad) drawRoundRect(colors.error.copy(alpha = 0.22f * fade), Offset(c.x - cell / 2f + gap, c.y - cell / 2f + gap), Size(cell - 2 * gap, cell - 2 * gap), corner)
                inset(c.x - side / 2f, c.y - side / 2f, size.width - (c.x + side / 2f), size.height - (c.y + side / 2f)) {
                    drawCrown((if (bad) colors.error else colors.pendingFill).copy(alpha = fade))
                    if (bad) {
                        val w = this.size.width
                        val h = this.size.height
                        val cross = colors.bgPage.copy(alpha = fade)
                        drawLine(cross, Offset(w * 0.32f, h * 0.4f), Offset(w * 0.68f, h * 0.76f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                        drawLine(cross, Offset(w * 0.68f, h * 0.4f), Offset(w * 0.32f, h * 0.76f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
                    }
                }
            }
        }

        val stops = taps.map { (at, r, c) -> DemoTap(at, center(r, c)) }
        val (hand, press) = demoHand(t, stops, Offset(size.width, size.height))
        drawHandCursor(handPainter, hand, cell * 0.95f, press, alpha = fade)
    }
}
