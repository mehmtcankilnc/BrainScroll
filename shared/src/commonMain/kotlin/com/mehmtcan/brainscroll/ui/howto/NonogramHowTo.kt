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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_nonogram_start
import brainscroll.shared.generated.resources.howto_nonogram_tech
import brainscroll.shared.generated.resources.howto_card_nonogram_1
import brainscroll.shared.generated.resources.howto_card_nonogram_2
import brainscroll.shared.generated.resources.howto_card_nonogram_3
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the nonogram card: fill (tap or drag), mark a cell empty with the cross, and the numbers fade when a line matches. */
internal val NonogramHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_nonogram_1, { t -> NonogramDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_nonogram_2, { t -> NonogramDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_nonogram_3, { t -> NonogramDemo(step = 2, t = t) }, cycleMillis = 6400),
        sceneStep(Res.string.howto_nonogram_tech, NonogramFullScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_nonogram_start, NonogramStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private const val SIZE = 4

// The picture: X is filled. Its numbers: rows 2 / 1 1 / 4 / 2, columns 2 / 1 2 / 1 2 / 2.
private val Picture = listOf(".XX.", "X..X", "XXXX", ".XX.")

private fun filled(row: Int, column: Int) = Picture[row][column] == 'X'

private fun runsOf(line: List<Boolean>): List<Int> {
    val out = ArrayList<Int>()
    var run = 0
    for (v in line) if (v) run++ else if (run > 0) { out += run; run = 0 }
    if (run > 0) out += run
    return out.ifEmpty { listOf(0) }
}

private val RowClues = (0 until SIZE).map { r -> runsOf((0 until SIZE).map { filled(r, it) }) }
private val ColumnClues = (0 until SIZE).map { c -> runsOf((0 until SIZE).map { filled(it, c) }) }

/** What the hand does to a cell: it is filled from [at] (a cross if [cross]). */
private class DemoCell(val row: Int, val column: Int, val at: Float, val cross: Boolean = false)

@Composable
internal fun NonogramDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    val handCells: List<DemoCell>
    val taps: List<Triple<Float, Int, Int>> // time, row, column; row -1 is the cross button
    val crossMode: (Float) -> Boolean
    when (step) {
        0 -> {
            // A drag along the third row fills its four cells one after the other.
            handCells = (0 until 4).map { DemoCell(2, it, 0.3f + it * 0.1f) }
            taps = emptyList()
            crossMode = { false }
        }
        1 -> {
            // Tap the cross button, then the cells that are surely empty: a cross in each. The third row is already filled.
            handCells = listOf(DemoCell(0, 0, 0.48f, cross = true), DemoCell(3, 0, 0.68f, cross = true))
            taps = listOf(Triple(0.26f, -1, 0), Triple(0.48f, 0, 0), Triple(0.68f, 3, 0))
            crossMode = { it >= 0.26f }
        }
        else -> {
            // The rest of the picture, cell by cell; the numbers of a line fade as it matches.
            val rest = listOf(0 to 1, 0 to 2, 1 to 0, 1 to 3, 3 to 1, 3 to 2)
            handCells = rest.mapIndexed { i, (r, c) -> DemoCell(r, c, 0.1f + i * 0.13f) }
            taps = rest.mapIndexed { i, (r, c) -> Triple(0.1f + i * 0.13f, r, c) }
            crossMode = { false }
        }
    }
    // The third row is filled from the start in steps 2 and 3.
    val startFilled = if (step >= 1) (0 until 4).map { 2 to it } else emptyList()

    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.06f
        // Room for the numbers on the left (1.2 cells) and on top (1.1 cells), and for the button below (1.25 cells).
        val cell = minOf(size.width / (SIZE + 2.4f), (size.height - 2 * pad) / (SIZE + 1.1f + 1.3f))
        // The board is in the middle of the picture (its numbers hang to the left), and the button is under its middle.
        val gridLeft = (size.width - SIZE * cell) / 2f
        val gridTop = pad + cell * 1.1f
        fun origin(row: Int, column: Int) = Offset(gridLeft + column * cell, gridTop + row * cell)
        fun center(row: Int, column: Int) = origin(row, column) + Offset(cell / 2f, cell / 2f)
        val corner = CornerRadius(cell * 0.18f)
        val gap = cell * 0.04f

        // What each cell holds now: 1 filled, 2 cross, 0 empty, with how far its pop has gone.
        val mark = Array(SIZE) { IntArray(SIZE) }
        val pop = Array(SIZE) { FloatArray(SIZE) { 1f } }
        startFilled.forEach { (r, c) -> mark[r][c] = 1 }
        handCells.forEach { h ->
            val p = demoPop(t, h.at, 0.06f)
            if (p > 0f) {
                mark[h.row][h.column] = if (h.cross) 2 else 1
                pop[h.row][h.column] = p
            }
        }

        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            val o = origin(r, c)
            val fill = if (mark[r][c] == 1) colors.correctFill else colors.bgSurface
            drawRoundRect(fill.copy(alpha = fade), Offset(o.x + gap, o.y + gap), Size(cell - 2 * gap, cell - 2 * gap), corner)
            drawRoundRect(colors.borderStrong.copy(alpha = 0.3f * fade), Offset(o.x + gap, o.y + gap), Size(cell - 2 * gap, cell - 2 * gap), corner, style = Stroke(width = 1.dp.toPx()))
            if (mark[r][c] == 2) {
                val cc = center(r, c)
                val w = cell * 0.22f
                val crossColor = colors.textSecondary.copy(alpha = 0.7f * fade)
                scale(pop[r][c], pop[r][c], pivot = cc) {
                    drawLine(crossColor, Offset(cc.x - w, cc.y - w), Offset(cc.x + w, cc.y + w), strokeWidth = cell * 0.08f, cap = StrokeCap.Round)
                    drawLine(crossColor, Offset(cc.x + w, cc.y - w), Offset(cc.x - w, cc.y + w), strokeWidth = cell * 0.08f, cap = StrokeCap.Round)
                }
            }
        }

        // The numbers: a row or column that matches the picture fades.
        fun rowDone(r: Int) = runsOf((0 until SIZE).map { mark[r][it] == 1 }) == RowClues[r] && (0 until SIZE).all { (mark[r][it] == 1) == filled(r, it) }
        fun columnDone(c: Int) = runsOf((0 until SIZE).map { mark[it][c] == 1 }) == ColumnClues[c] && (0 until SIZE).all { (mark[it][c] == 1) == filled(it, c) }
        for (r in 0 until SIZE) {
            val color = if (rowDone(r)) colors.textSecondary.copy(alpha = 0.4f) else colors.textPrimary
            val text = RowClues[r].joinToString(" ")
            drawCenteredText(measurer, text, Offset(gridLeft - cell * 0.1f - text.length * cell * 0.1f, center(r, 0).y), cell * 0.42f, color.copy(alpha = color.alpha * fade))
        }
        for (c in 0 until SIZE) {
            val color = if (columnDone(c)) colors.textSecondary.copy(alpha = 0.4f) else colors.textPrimary
            val numbers = ColumnClues[c]
            numbers.forEachIndexed { i, n ->
                val y = gridTop - cell * 0.1f - cell * 0.45f * (numbers.size - i - 0.5f)
                drawCenteredText(measurer, n.toString(), Offset(center(0, c).x, y), cell * 0.42f, color.copy(alpha = color.alpha * fade))
            }
        }

        // The cross button under the board: yellow outline while the cross mode is on.
        val buttonCenter = Offset(size.width / 2f, gridTop + SIZE * cell + cell * 0.75f)
        val on = crossMode(t)
        val radius = cell * 0.5f
        drawCircle((if (on) colors.bgRaised else colors.bgSurface).copy(alpha = fade), radius, buttonCenter)
        drawCircle((if (on) colors.pendingFill else colors.borderStrong.copy(alpha = 0.6f)).copy(alpha = fade), radius, buttonCenter, style = Stroke(width = (if (on) 2.5.dp else 1.5.dp).toPx()))
        val iconColor = (if (on) colors.pendingFill else colors.textPrimary).copy(alpha = fade)
        val r = radius * 0.4f
        drawLine(iconColor, Offset(buttonCenter.x - r, buttonCenter.y - r), Offset(buttonCenter.x + r, buttonCenter.y + r), strokeWidth = radius * 0.2f, cap = StrokeCap.Round)
        drawLine(iconColor, Offset(buttonCenter.x + r, buttonCenter.y - r), Offset(buttonCenter.x - r, buttonCenter.y + r), strokeWidth = radius * 0.2f, cap = StrokeCap.Round)

        // The hand: in step 1 a drag along the row, in the others a tap on the button or a cell.
        val offscreen = Offset(size.width, size.height)
        val hand: Offset
        val press: Float
        if (step == 0) {
            val from = center(2, 0)
            val to = center(2, 3)
            val arrive = demoEase(demoSegment(t, 0.05f, 0.28f))
            val drag = demoEase(demoSegment(t, 0.3f, 0.64f))
            hand = if (t < 0.3f) Offset(offscreen.x + (from.x - offscreen.x) * arrive, offscreen.y + (from.y - offscreen.y) * arrive) else Offset(from.x + (to.x - from.x) * drag, from.y)
            press = demoSegment(t, 0.27f, 0.34f)
        } else {
            val stops = taps.map { (at, r2, c2) -> DemoTap(at, if (r2 < 0) buttonCenter else center(r2, c2)) }
            val (h, p) = demoHand(t, stops, offscreen)
            hand = h
            press = p
        }
        drawHandCursor(handPainter, hand, cell * 0.95f, press, alpha = fade)
    }
}
