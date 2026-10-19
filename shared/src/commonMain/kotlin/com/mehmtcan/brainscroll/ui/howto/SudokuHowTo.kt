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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_jigsaw_1
import brainscroll.shared.generated.resources.howto_card_sudoku_1
import brainscroll.shared.generated.resources.howto_card_sudoku_2
import brainscroll.shared.generated.resources.howto_card_sudoku_3
import com.mehmtcan.brainscroll.game.sudoku.JigsawGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuPuzzle
import com.mehmtcan.brainscroll.ui.sudoku.boardLine
import com.mehmtcan.brainscroll.ui.sudoku.drawErase
import com.mehmtcan.brainscroll.ui.sudoku.drawPencil
import com.mehmtcan.brainscroll.ui.sudoku.drawRegionBorders
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The steps of the sudoku card: the rule (a row, a column and a box light up), tap a cell and a number, and the pencil for notes. */
internal val SudokuHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_sudoku_1, { t -> SudokuDemo(jigsaw = false, step = 0, t = t) }, cycleMillis = 4800),
        HowToStep(Res.string.howto_card_sudoku_2, { t -> SudokuDemo(jigsaw = false, step = 1, t = t) }),
        HowToStep(Res.string.howto_card_sudoku_3, { t -> SudokuDemo(jigsaw = false, step = 2, t = t) }, cycleMillis = 4800),
    ),
)

/** The jigsaw card: the same, but its first step is about the shapes of the regions. */
internal val JigsawHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_jigsaw_1, { t -> SudokuDemo(jigsaw = true, step = 0, t = t) }, cycleMillis = 4800),
        HowToStep(Res.string.howto_card_sudoku_2, { t -> SudokuDemo(jigsaw = true, step = 1, t = t) }),
        HowToStep(Res.string.howto_card_sudoku_3, { t -> SudokuDemo(jigsaw = true, step = 2, t = t) }, cycleMillis = 4800),
    ),
)

// Real boards, made once: the pictures show what the game really draws.
private val classicPuzzle: SudokuPuzzle by lazy { SudokuGenerator.generate(seed = 7, clues = 44) }
private val jigsawPuzzle: SudokuPuzzle by lazy { JigsawGenerator.generate(seed = 7, clues = 44) }

private const val N = 9

@Composable
internal fun SudokuDemo(jigsaw: Boolean, step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    val puzzle = if (jigsaw) jigsawPuzzle else classicPuzzle
    val regionOf = puzzle.layout.regionOf
    val blanks = (0 until N * N).filter { puzzle.givens[it] == 0 }
    // The cell of the digit (the blank nearest the middle) and the one of the notes (the blank nearest the top left).
    val digitCell = blanks.minByOrNull { kotlin.math.abs(it / N - 4) + kotlin.math.abs(it % N - 4) } ?: 40
    val noteCell = blanks.filter { it != digitCell }.minByOrNull { it / N + it % N } ?: 0
    val noteDigits = listOf(puzzle.solution[noteCell], puzzle.solution[noteCell] % 9 + 1)

    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val h = size.height
        val boardSide = h * 0.68f
        val cell = boardSide / N
        val left = (size.width - boardSide) / 2f
        val top = h * 0.04f
        fun center(cellIndex: Int) = Offset(left + (cellIndex % N + 0.5f) * cell, top + (cellIndex / N + 0.5f) * cell)
        val keysTop = top + boardSide + h * 0.03f
        val keyHeight = h * 0.085f
        val keyGap = cell * 0.08f
        val keyWidth = (boardSide - keyGap * 8) / 9f
        fun keyCenter(digit: Int) = Offset(left + (digit - 1) * (keyWidth + keyGap) + keyWidth / 2f, keysTop + keyHeight / 2f)
        val roundRadius = h * 0.045f
        val pencilCenter = Offset(size.width / 2f - roundRadius * 1.6f, keysTop + keyHeight + h * 0.02f + roundRadius)
        val eraserCenter = Offset(size.width / 2f + roundRadius * 1.6f, pencilCenter.y)

        // What the hand does, and what the board shows because of it.
        var selected = -1
        val entered = HashMap<Int, Pair<Int, Float>>() // cell -> digit and its pop
        val notes = HashMap<Int, MutableList<Pair<Int, Float>>>()
        var notesOn = false
        var litCells: Set<Int> = emptySet()
        var pressedDigit = 0
        val stops = ArrayList<DemoTap>()
        when (step) {
            0 -> {
                val row = (0 until N).map { 4 * N + it }.toSet()
                val column = (0 until N).map { it * N + 4 }.toSet()
                val region = (0 until N * N).filter { regionOf[it] == regionOf[40] }.toSet()
                val sets = if (jigsaw) listOf(region, row, column) else listOf(row, column, region)
                litCells = when {
                    t < 0.04f -> emptySet()
                    t < 0.34f -> sets[0]
                    t < 0.36f -> emptySet()
                    t < 0.66f -> sets[1]
                    t < 0.68f -> emptySet()
                    t < 0.98f -> sets[2]
                    else -> emptySet()
                }
            }
            1 -> {
                stops += DemoTap(0.2f, center(digitCell))
                stops += DemoTap(0.5f, keyCenter(puzzle.solution[digitCell]))
                if (t >= 0.2f) selected = digitCell
                if (t >= 0.5f) {
                    entered[digitCell] = puzzle.solution[digitCell] to demoPop(t, 0.5f)
                    pressedDigit = if (t < 0.58f) puzzle.solution[digitCell] else 0
                } else if (t >= 0.2f) {
                    pressedDigit = 0
                }
            }
            else -> {
                stops += DemoTap(0.12f, pencilCenter)
                stops += DemoTap(0.28f, center(noteCell))
                stops += DemoTap(0.44f, keyCenter(noteDigits[0]))
                stops += DemoTap(0.6f, keyCenter(noteDigits[1]))
                notesOn = t >= 0.12f
                if (t >= 0.28f) selected = noteCell
                val list = ArrayList<Pair<Int, Float>>()
                if (t >= 0.44f) list += noteDigits[0] to demoPop(t, 0.44f)
                if (t >= 0.6f) list += noteDigits[1] to demoPop(t, 0.6f)
                if (list.isNotEmpty()) notes[noteCell] = list
                pressedDigit = when {
                    t in 0.44f..0.52f -> noteDigits[0]
                    t in 0.6f..0.68f -> noteDigits[1]
                    else -> 0
                }
            }
        }
        val peers = if (selected >= 0) puzzle.layout.peers[selected].toSet() else emptySet()

        // The board: a line color behind the cells, a tile for each cell, the thick lines between regions and round the board.
        val lines = lerp(colors.bgPage, colors.borderStrong, 0.5f)
        drawRoundRect(lines.copy(alpha = fade), Offset(left, top), Size(boardSide, boardSide), CornerRadius(cell * 0.2f))
        val gap = cell * 0.045f
        for (i in 0 until N * N) {
            val o = Offset(left + (i % N) * cell + gap, top + (i / N) * cell + gap)
            val s = Size(cell - 2 * gap, cell - 2 * gap)
            val shade = if (i == selected || i in peers) lerp(colors.bgRaised, colors.borderStrong, 0.12f) else colors.bgSurface
            drawRoundRect(shade.copy(alpha = fade), o, s, CornerRadius(cell * 0.12f))
            if (i in litCells) drawRoundRect(colors.pendingFill.copy(alpha = 0.32f * fade), o, s, CornerRadius(cell * 0.12f))
            if (i == selected) drawRoundRect(colors.pendingFill.copy(alpha = fade), o, s, CornerRadius(cell * 0.12f), style = Stroke(width = 2.5.dp.toPx()))
        }
        drawRegionBorders(regionOf, N, cell, Offset(left, top), 2.2.dp.toPx(), cell * 0.25f, boardLine(colors).copy(alpha = fade))
        drawRoundRect(boardLine(colors).copy(alpha = fade), Offset(left, top), Size(boardSide, boardSide), CornerRadius(cell * 0.2f), style = Stroke(width = 2.2.dp.toPx()))

        // The digits: the givens (and the whole solution in the first step), the one the player enters, the notes.
        for (i in 0 until N * N) {
            val c = center(i)
            val given = puzzle.givens[i]
            val digit = when {
                given != 0 -> given
                step == 0 -> puzzle.solution[i]
                else -> 0
            }
            if (digit != 0) drawCenteredText(measurer, digit.toString(), c, cell * 0.6f, colors.textPrimary.copy(alpha = fade))
            entered[i]?.let { (d, pop) -> scale(pop, pop, pivot = c) { drawCenteredText(measurer, d.toString(), c, cell * 0.6f, colors.streakAnswer.copy(alpha = fade)) } }
            notes[i]?.forEach { (d, pop) ->
                val spot = Offset(c.x + ((d - 1) % 3 - 1) * cell * 0.3f, c.y + ((d - 1) / 3 - 1) * cell * 0.3f)
                scale(pop, pop, pivot = spot) { drawCenteredText(measurer, d.toString(), spot, cell * 0.34f, colors.textSecondary.copy(alpha = fade)) }
            }
        }

        // The number pad: nine keys, and the pencil (yellow while the notes are on) and the eraser as round buttons.
        for (d in 1..9) {
            val kc = keyCenter(d)
            val o = Offset(kc.x - keyWidth / 2f, keysTop)
            val on = d == pressedDigit
            drawRoundRect((if (on) colors.bgRaised else colors.bgSurface).copy(alpha = fade), o, Size(keyWidth, keyHeight), CornerRadius(cell * 0.2f))
            drawRoundRect((if (on) colors.pendingFill else colors.borderStrong.copy(alpha = 0.6f)).copy(alpha = fade), o, Size(keyWidth, keyHeight), CornerRadius(cell * 0.2f), style = Stroke(width = 1.5.dp.toPx()))
            drawCenteredText(measurer, d.toString(), kc, keyHeight * 0.5f, (if (notesOn) colors.textSecondary else colors.textPrimary).copy(alpha = fade))
        }
        for ((c, isPencil) in listOf(pencilCenter to true, eraserCenter to false)) {
            val on = isPencil && notesOn
            drawCircle((if (on) colors.bgRaised else colors.bgSurface).copy(alpha = fade), roundRadius, c)
            drawCircle((if (on) colors.pendingFill else colors.borderStrong.copy(alpha = 0.6f)).copy(alpha = fade), roundRadius, c, style = Stroke(width = (if (on) 2.5.dp else 1.5.dp).toPx()))
            val ink = (if (on) colors.pendingFill else colors.textPrimary).copy(alpha = fade)
            // The same icons as the buttons of the page, drawn in the middle of the circle.
            val side = roundRadius * 1.05f
            inset(c.x - side / 2f, c.y - side / 2f, size.width - (c.x + side / 2f), size.height - (c.y + side / 2f)) {
                if (isPencil) drawPencil(ink) else drawErase(ink)
            }
        }

        if (stops.isNotEmpty()) {
            val (hand, press) = demoHand(t, stops, Offset(size.width, size.height))
            drawHandCursor(handPainter, hand, cell * 1.2f, press, alpha = fade)
        }
    }
}
