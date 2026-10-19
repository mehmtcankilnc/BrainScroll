package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_tents_1
import brainscroll.shared.generated.resources.howto_card_tents_2
import brainscroll.shared.generated.resources.howto_card_tents_3
import com.mehmtcan.brainscroll.ui.tents.drawGrass
import com.mehmtcan.brainscroll.ui.tents.drawTent
import com.mehmtcan.brainscroll.ui.tents.drawTree
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the tents card: a tap puts a tent, a second tap puts grass, and the whole board with its numbers. */
internal val TentsHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_tents_1, { t -> TentsDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_tents_2, { t -> TentsDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_tents_3, { t -> TentsDemo(step = 2, t = t) }, cycleMillis = 6400),
    ),
)

private const val SIZE = 4

// (row, column) of the four trees, and of the four tents that solve the board (each next to its own tree, none touching).
private val Trees = listOf(0 to 0, 1 to 3, 3 to 1, 3 to 3)
private val Solution = listOf(0 to 1, 0 to 3, 2 to 3, 3 to 0)

/** A mark the hand puts in a cell: a tent from [tentAt], and if [grassAt] is not negative the tent turns into grass then. */
private class DemoMark(val cell: Pair<Int, Int>, val tentAt: Float, val grassAt: Float = -1f)

@Composable
internal fun TentsDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    val tile = colors.bgSurface
    val edge = colors.borderStrong.copy(alpha = 0.35f)
    // What the hand does: the marks it puts in the cells, and the taps that make them.
    val marks: List<DemoMark>
    val taps: List<Pair<Float, Pair<Int, Int>>>
    when (step) {
        0 -> {
            marks = listOf(DemoMark(0 to 1, 0.25f), DemoMark(3 to 0, 0.58f))
            taps = listOf(0.25f to (0 to 1), 0.58f to (3 to 0))
        }
        1 -> {
            marks = listOf(DemoMark(0 to 1, -1f), DemoMark(2 to 1, 0.22f, grassAt = 0.55f))
            taps = listOf(0.22f to (2 to 1), 0.55f to (2 to 1))
        }
        else -> {
            val times = listOf(0.1f, 0.28f, 0.46f, 0.64f)
            marks = Solution.mapIndexed { i, cell -> DemoMark(cell, times[i]) }
            taps = Solution.mapIndexed { i, cell -> times[i] to cell }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.07f
        val cell = (size.width - 2 * pad) / (SIZE + 1)
        // The clues take the first row (columns) and the last column (rows), as on the board; the cells are below and to the left.
        fun origin(row: Int, column: Int) = Offset(pad + column * cell, pad + (row + 1) * cell)
        fun center(row: Int, column: Int) = origin(row, column) + Offset(cell / 2f, cell / 2f)
        val gap = cell * 0.05f
        val corner = CornerRadius(cell * 0.18f)

        // The tents that are in the cells now (a tent that turned into grass is not one).
        fun tentScale(m: DemoMark): Float {
            if (m.tentAt < 0f) return 1f
            val pop = demoPop(t, m.tentAt)
            return if (m.grassAt in 0f..t) 0f else pop
        }
        val tentCells = marks.filter { tentScale(it) > 0f }.map { it.cell }
        val rowCounts = IntArray(SIZE) { r -> tentCells.count { it.first == r } }
        val columnCounts = IntArray(SIZE) { c -> tentCells.count { it.second == c } }
        val rowTargets = IntArray(SIZE) { r -> Solution.count { it.first == r } }
        val columnTargets = IntArray(SIZE) { c -> Solution.count { it.second == c } }

        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            val o = origin(r, c)
            drawRoundRect(tile.copy(alpha = fade), Offset(o.x + gap, o.y + gap), Size(cell - 2 * gap, cell - 2 * gap), corner)
            drawRoundRect(edge.copy(alpha = edge.alpha * fade), Offset(o.x + gap, o.y + gap), Size(cell - 2 * gap, cell - 2 * gap), corner, style = Stroke(width = 1.5.dp.toPx()))
        }

        fun clue(text: String, at: Offset, count: Int, target: Int) {
            val color = when {
                count > target -> colors.error
                count == target -> colors.correctFill
                else -> colors.textPrimary
            }
            val label = measurer.measure(text, TextStyle(color = color.copy(alpha = fade), fontSize = (cell * 0.5f / (density * fontScale)).sp, fontWeight = FontWeight.Bold))
            drawText(label, topLeft = Offset(at.x - label.size.width / 2f, at.y - label.size.height / 2f))
        }
        // Clues only for lines that have a tent in the solution of this step (the other lines say 0).
        for (c in 0 until SIZE) clue(columnTargets[c].toString(), Offset(pad + c * cell + cell / 2f, pad + cell / 2f), columnCounts[c], columnTargets[c])
        for (r in 0 until SIZE) clue(rowTargets[r].toString(), Offset(pad + SIZE * cell + cell / 2f, pad + (r + 1) * cell + cell / 2f), rowCounts[r], rowTargets[r])

        fun piece(row: Int, column: Int, scaleBy: Float, draw: DrawScope.() -> Unit) {
            val c = center(row, column)
            val side = cell * 0.72f
            scale(scaleBy, scaleBy, pivot = c) {
                inset(c.x - side / 2f, c.y - side / 2f, size.width - (c.x + side / 2f), size.height - (c.y + side / 2f)) { draw() }
            }
        }
        for ((r, c) in Trees) piece(r, c, 1f) { drawTree(colors.correctFill.copy(alpha = fade), colors.borderStrong.copy(alpha = fade)) }
        for (m in marks) {
            val s = tentScale(m)
            if (s > 0f) piece(m.cell.first, m.cell.second, s) { drawTent(colors.pendingFill.copy(alpha = fade), colors.bgSurface.copy(alpha = fade)) }
            if (m.grassAt in 0f..t) piece(m.cell.first, m.cell.second, demoPop(t, m.grassAt)) { drawGrass(colors.correctFill.copy(alpha = 0.55f * fade)) }
        }

        val stops = taps.map { (at, cellRC) -> DemoTap(at, center(cellRC.first, cellRC.second)) }
        val (hand, press) = demoHand(t, stops, Offset(size.width, size.height))
        drawHandCursor(handPainter, hand, cell * 0.95f, press, alpha = fade)
    }
}
