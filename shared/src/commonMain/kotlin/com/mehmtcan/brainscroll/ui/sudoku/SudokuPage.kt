package com.mehmtcan.brainscroll.ui.sudoku

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.LineHeightStyle
import kotlinx.coroutines.delay
import kotlin.math.abs
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_sudoku_cell_empty
import brainscroll.shared.generated.resources.a11y_sudoku_cell_given
import brainscroll.shared.generated.resources.a11y_sudoku_cell_notes
import brainscroll.shared.generated.resources.a11y_sudoku_cell_value
import brainscroll.shared.generated.resources.a11y_sudoku_cell_wrong
import brainscroll.shared.generated.resources.a11y_sudoku_digit
import brainscroll.shared.generated.resources.a11y_sudoku_erase
import brainscroll.shared.generated.resources.a11y_sudoku_mistakes
import brainscroll.shared.generated.resources.a11y_sudoku_note
import brainscroll.shared.generated.resources.a11y_sudoku_notes_off
import brainscroll.shared.generated.resources.a11y_sudoku_notes_on
import brainscroll.shared.generated.resources.game_sudoku
import brainscroll.shared.generated.resources.sudoku_mistakes
import brainscroll.shared.generated.resources.sudoku_notes
import brainscroll.shared.generated.resources.sudoku_result_won
import brainscroll.shared.generated.resources.sudoku_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuLayout
import com.mehmtcan.brainscroll.game.sudoku.SudokuResult
import com.mehmtcan.brainscroll.game.sudoku.SudokuSnapshot
import com.mehmtcan.brainscroll.game.sudoku.SudokuState
import com.mehmtcan.brainscroll.game.sudoku.SudokuStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 2.dp

/** The cells are nearly square, like the squares of a printed sudoku. */
private val CellShape = RoundedCornerShape(3.dp)

/** The thick line round the whole board, and between the regions of the jigsaw board: the same line, so the two boards look alike. */
private val OuterBorder = 2.5.dp

/** Inside the border of the whole board. */
private val FramePadding = 3.dp

/**
 * The color of the thick line round the board and between the jigsaw regions: the text color a little dimmed (as a solid color,
 * so where lines cross nothing gets brighter), so it is the strongest line of the board without being the loudest thing in the app.
 */
internal fun boardLine(colors: com.mehmtcan.brainscroll.ui.theme.BrainScrollColors): Color = lerp(colors.bgPage, colors.textPrimary, 0.78f)

/** How round the lines between the jigsaw regions turn at a corner. */
private val RegionCorner = 5.dp

private val MaxCell = 44.dp
private val KeyHeight = 56.dp
private val KeyGap = 4.dp

/**
 * What a cell shows, for tests: 0 empty, 1..9 a digit, and `+100` for a wrong digit, `+200` for notes only (then the
 * value is 200 plus the notes mask).
 */
internal val SudokuCellValue = SemanticsPropertyKey<Int>("SudokuCellValue")
private var SemanticsPropertyReceiver.cellValue by SudokuCellValue

/** The region of every cell of a jigsaw board (0..8), for tests. */
internal val SudokuRegions = SemanticsPropertyKey<List<Int>>("SudokuRegions")
private var SemanticsPropertyReceiver.regionsValue by SudokuRegions

/** One full-screen page of the feed: the title, the mistake counter, the board, and the number pad or the result. */
@Composable
fun SudokuPage(
    page: SudokuSnapshot,
    onPlace: (cell: Int, digit: Int) -> SudokuResult,
    onNote: (cell: Int, digit: Int) -> SudokuResult,
    onErase: (cell: Int) -> SudokuResult,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state

    val status = rememberSettledStatus(
        when (state.status) {
            SudokuStatus.Playing -> WordleStatus.Playing
            SudokuStatus.Won -> WordleStatus.Won
        },
        // The last digit swells and flashes: the result waits for it.
        750,
    )
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    var selected by remember(page.id) { mutableStateOf<Int?>(null) }
    var notesMode by remember(page.id) { mutableStateOf(false) }

    // A row, column, box or the whole board that was just completed gets a wave. A page that is merely shown again
    // (restored, scrolled back to) starts from what it already has, so it stays quiet.
    var wave by remember(page.id) { mutableStateOf(Wave(0, emptyMap())) }
    val seen = remember(page.id) { Seen() }
    val layout = remember(state.regions) { SudokuLayout.of(state.regions) }
    LaunchedEffect(state.values, state.wrong) {
        val done = completedUnits(state, layout)
        val before = seen.done
        if (before != null && done.size > before.size) {
            val origin = state.values.indices.firstOrNull { state.values[it] != 0 && state.values[it] != seen.values[it] } ?: 0
            wave = Wave(wave.id + 1, waveDelays(done - before, origin, layout))
        }
        seen.done = done
        seen.values = state.values
    }

    fun digit(digit: Int) {
        val cell = selected ?: return
        if (notesMode) {
            if (onNote(cell, digit) == SudokuResult.Changed) haptics.perform(HapticKind.Light)
        } else {
            when (onPlace(cell, digit)) {
                SudokuResult.Correct -> haptics.perform(HapticKind.Medium)
                SudokuResult.Mistake -> haptics.perform(HapticKind.Warning)
                else -> Unit
            }
        }
    }

    fun erase() {
        val cell = selected ?: return
        if (onErase(cell) == SudokuResult.Changed) haptics.perform(HapticKind.Light)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Chip(
                text = stringResource(Res.string.sudoku_mistakes, state.mistakes),
                textColor = if (state.mistakes > 0) colors.error else colors.textSecondary,
                description = stringResource(Res.string.a11y_sudoku_mistakes, state.mistakes),
            )
        }
        Spacer(Modifier.height(Spacing.md))

        Board(
            state = state,
            layout = layout,
            selected = selected,
            wave = wave,
            onSelect = { selected = it },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status != WordleStatus.Playing) {
                SudokuResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                NumberPad(state = state, notesMode = notesMode, onDigit = ::digit, onErase = ::erase, onToggleNotes = { notesMode = !notesMode })
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/**
 * The 81 cells as equal squares, as large as the room allows, in rows (so a screen reader and the tests meet them row by
 * row). Behind them the nine 3x3 boxes are drawn as lighter panels with a clear gap between them, and the cells are dark
 * squares set into them, so the boxes can be told apart at a glance.
 */
@Composable
private fun Board(state: SudokuState, layout: SudokuLayout, selected: Int?, wave: Wave, onSelect: (Int) -> Unit, modifier: Modifier) {
    val colors = BrainScrollTheme.colors
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val size = SudokuGenerator.SIZE
        val frames = FramePadding * 2 + OuterBorder * 2
        val gaps = CellGap * 8 + frames
        val cellSize: Dp = minOf(MaxCell, (maxWidth - gaps) / size, (maxHeight - gaps) / size)

        val selectedValue = selected?.let { state.values[it] } ?: 0
        val selectedPeers = selected?.let { layout.peers[it].toSet() } ?: emptySet()

        BoardGrid(state, layout, selected, selectedValue, selectedPeers, wave, cellSize, onSelect)
    }
}

/**
 * The board of both sudokus: equal gaps between the cells and a thick line along every border between two regions (the 3 by 3
 * boxes of the classic board, the shapes of the jigsaw board), and the same line round the whole board. There is no color on the
 * regions: the colors of the theme already mean something (right, picked, entered).
 */
@Composable
private fun BoardGrid(
    state: SudokuState,
    layout: SudokuLayout,
    selected: Int?,
    selectedValue: Int,
    selectedPeers: Set<Int>,
    wave: Wave,
    cellSize: Dp,
    onSelect: (Int) -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val size = SudokuGenerator.SIZE
    val lines = lerp(colors.bgPage, colors.borderStrong, 0.5f)
    val frame = RoundedCornerShape(6.dp)
    val border = boardLine(colors)
    Box(
        modifier = Modifier
            .testTag("sudokuBoard")
            .semantics { regionsValue = state.regions }
            .background(lines, frame)
            .border(OuterBorder, boardLine(colors), frame)
            .padding(FramePadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
            for (row in 0 until size) {
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    for (column in 0 until size) {
                        val index = row * size + column
                        GuideMark(isGuided(index), CellShape) {
                            Cell(
                                row = row + 1,
                                column = column + 1,
                                value = state.values[index],
                                notes = state.notes[index],
                                given = state.isGiven(index),
                                wrong = index in state.wrong,
                                isSelected = index == selected,
                                isPeer = index in selectedPeers,
                                sameDigit = selectedValue != 0 && state.values[index] == selectedValue,
                                size = cellSize,
                                waveId = wave.id,
                                waveDelay = wave.delays[index],
                                onClick = { onSelect(index) },
                            )
                        }
                    }
                }
            }
        }
        // The thick lines through the gaps between cells of different regions, drawn on top of the cells. They take no touches.
        Canvas(Modifier.matchParentSize().testTag("sudokuBorders")) {
            val gap = CellGap.toPx()
            drawRegionBorders(
                regionOf = layout.regionOf,
                size = size,
                step = (cellSize + CellGap).toPx(),
                origin = Offset(-gap / 2, -gap / 2),
                thickness = OuterBorder.toPx(),
                radius = RegionCorner.toPx(),
                color = border,
            )
        }
    }
}

@Composable
private fun Cell(
    row: Int,
    column: Int,
    value: Int,
    notes: Int,
    given: Boolean,
    wrong: Boolean,
    isSelected: Boolean,
    isPeer: Boolean,
    sameDigit: Boolean,
    size: Dp,
    waveId: Int,
    waveDelay: Int?,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = CellShape

    val notesText = (1..9).filter { notes and (1 shl (it - 1)) != 0 }.joinToString(", ")
    val description = when {
        wrong -> stringResource(Res.string.a11y_sudoku_cell_wrong, row, column, value)
        given -> stringResource(Res.string.a11y_sudoku_cell_given, row, column, value)
        value != 0 -> stringResource(Res.string.a11y_sudoku_cell_value, row, column, value)
        notes != 0 -> stringResource(Res.string.a11y_sudoku_cell_notes, row, column, notesText)
        else -> stringResource(Res.string.a11y_sudoku_cell_empty, row, column)
    }
    val shown = when {
        wrong -> 100 + value
        value != 0 -> value
        notes != 0 -> 200 + notes
        else -> 0
    }

    // What the player is on keeps the colors it always had: the cell, the same digit and the row, column and box (the
    // peers) are a lighter gray, the cell itself and the same digit with a yellow outline.
    val background = when {
        wrong -> colors.error.copy(alpha = 0.28f)
        isSelected || sameDigit || isPeer -> lerp(colors.bgRaised, colors.borderStrong, 0.12f)
        else -> colors.bgSurface
    }
    val border = when {
        isSelected -> (2.5f + extraBorder).dp to colors.pendingFill
        // The same digit elsewhere: the same bright yellow as the cell itself, a little thinner (a dimmed yellow looked brown).
        sameDigit -> (1.5f + extraBorder).dp to colors.pendingFill
        else -> 0.dp to colors.borderStrong
    }
    val density = LocalDensity.current

    // The wave: the cell swells and flashes green when its turn comes, then settles back.
    val reduce = LocalReduceMotion.current
    val swell = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(waveId) {
        if (waveDelay == null || reduce) return@LaunchedEffect
        delay(waveDelay.toLong())
        flash.snapTo(1f)
        swell.animateTo(1.18f, tween(Motion.FAST, easing = Motion.Standard))
        flash.animateTo(0f, tween(Motion.SLOW, easing = Motion.Standard))
        swell.animateTo(1f, tween(Motion.POP, easing = Motion.Overshoot))
    }

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = swell.value
                scaleY = swell.value
            }
            .testTag("sudokuCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                cellValue = shown
                role = Role.Button
            }
            .background(colors.bgSurface, shape)
            .background(background, shape)
            .background(colors.correctFill.copy(alpha = flash.value * 0.55f), shape)
            .then(if (border.first.value > 0f) Modifier.border(border.first, border.second, shape) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // The text size follows the cell, not the text size setting: a digit has to fit its cell either way.
        when {
            value != 0 -> BasicText(
                text = value.toString(),
                style = BrainScrollTheme.typography.heading.copy(
                    color = when {
                        wrong -> colors.error
                        given -> colors.textPrimary
                        else -> colors.streakAnswer
                    },
                    fontSize = with(density) { (size * 0.52f).toSp() },
                    fontWeight = if (given) FontWeight.Bold else FontWeight.Medium,
                ),
            )
            notes != 0 -> Notes(notes, size, colors.textSecondary)
        }
    }
}

/** One wave: every time [id] changes, each cell in [delays] plays its swell after that many milliseconds. */
private class Wave(val id: Int, val delays: Map<Int, Int>)

/** What the last look at the board saw, to tell what was completed since. */
private class Seen {
    var done: Set<Int>? = null
    var values: List<Int> = emptyList()
}

/** The number of the whole board among the unit numbers (0..26 are the rows, columns and boxes). */
private const val BOARD = 27

/** The rows, columns and boxes whose nine cells all hold a right digit, and [BOARD] if every cell does. */
private fun completedUnits(state: SudokuState, layout: SudokuLayout): Set<Int> {
    val done = mutableSetOf<Int>()
    layout.units.forEachIndexed { unit, cells ->
        if (cells.all { state.values[it] != 0 && it !in state.wrong }) done += unit
    }
    if (state.values.indices.all { state.values[it] != 0 && it !in state.wrong }) done += BOARD
    return done
}

/** Each cell of the [units] and how long to wait before it swells: further from the [origin] cell, later. */
private fun waveDelays(units: Set<Int>, origin: Int, layout: SudokuLayout): Map<Int, Int> {
    val cells = units.flatMap { if (it == BOARD) (0 until SudokuGenerator.CELLS).toList() else layout.units[it].toList() }.toSet()
    val size = SudokuGenerator.SIZE
    return cells.associateWith { abs(it / size - origin / size) + abs(it % size - origin % size) }.mapValues { it.value * 45 }
}

/** The pencil notes of a cell as a small 3x3 grid, digit 1 top left. */
@Composable
private fun Notes(notes: Int, size: Dp, color: Color) {
    val density = LocalDensity.current
    val part = size / 3
    // Weights, not fixed sizes: the three slots always add up to exactly the cell, whatever the pixel rounding.
    // The padding keeps the digits clear of the border, which is thickest on the selected cell.
    Column(Modifier.fillMaxSize().padding(3.dp)) {
        for (r in 0 until 3) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (c in 0 until 3) {
                    val digit = r * 3 + c + 1
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (notes and (1 shl (digit - 1)) != 0) {
                            BasicText(
                                text = digit.toString(),
                                style = BrainScrollTheme.typography.caption.copy(
                                    color = color,
                                    fontSize = with(density) { (part * 0.72f).toSp() },
                                    // No extra line space above or below, or the digit sits off the middle of its slot.
                                    lineHeight = with(density) { (part * 0.72f).toSp() },
                                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Two rows: the nine digits on top, tall enough to hit easily, and under them the pencil that switches to notes and the
 * eraser as the round icon buttons of the other games. A digit that is already complete on the board is dimmed.
 */
@Composable
private fun NumberPad(state: SudokuState, notesMode: Boolean, onDigit: (Int) -> Unit, onErase: () -> Unit, onToggleNotes: () -> Unit) {
    val done = (1..9).associateWith { digit ->
        state.values.indices.count { state.values[it] == digit && it !in state.wrong } >= 9
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
            for (digit in 1..9) DigitKey(digit, notesMode, !done.getValue(digit), { onDigit(digit) }, Modifier.weight(1f))
        }
        // The same round buttons as the other games (undo and restart of the tubes, the flag of the mines).
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            val pencil = if (notesMode) BrainScrollTheme.colors.pendingFill else BrainScrollTheme.colors.textPrimary
            val eraser = BrainScrollTheme.colors.textPrimary
            RoundIconButton(
                description = stringResource(if (notesMode) Res.string.a11y_sudoku_notes_on else Res.string.a11y_sudoku_notes_off),
                onClick = onToggleNotes,
                selected = notesMode,
                tag = "sudokuNotes",
            ) { drawPencil(pencil) }
            RoundIconButton(
                description = stringResource(Res.string.a11y_sudoku_erase),
                onClick = onErase,
                tag = "sudokuErase",
            ) { drawErase(eraser) }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun DigitKey(digit: Int, notesMode: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = BrainScrollTheme.colors
    val description = stringResource(if (notesMode) Res.string.a11y_sudoku_note else Res.string.a11y_sudoku_digit, digit)
    Key(modifier, description, "sudokuKey$digit", enabled, onClick) {
        BasicText(
            text = digit.toString(),
            style = BrainScrollTheme.typography.title.copy(color = if (notesMode) colors.textSecondary else colors.textPrimary),
        )
    }
}

/** One key of the pad: at least 48 dp tall, so it is easy to hit. */
@Composable
private fun Key(
    modifier: Modifier,
    description: String,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit,
    selected: Boolean = false,
    height: Dp = KeyHeight,
    content: @Composable () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    Box(
        modifier = modifier
            .height(height)
            .testTag(tag)
            .alpha(if (enabled) 1f else 0.35f)
            .background(if (selected) colors.bgRaised else colors.bgSurface, Radius.control)
            .border(1.5.dp, if (selected) colors.pendingFill else colors.borderStrong.copy(alpha = 0.6f), Radius.control)
            .semantics {
                contentDescription = description
                role = Role.Button
                this.selected = selected
                if (!enabled) disabled()
            }
            .then(
                if (enabled) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A pencil lying at an angle, tip down left. */
private fun DrawScope.drawPencil(color: Color) {
    val s = size.minDimension
    val stroke = Stroke(width = s * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val body = Path().apply {
        moveTo(s * 0.14f, s * 0.86f)
        lineTo(s * 0.20f, s * 0.60f)
        lineTo(s * 0.64f, s * 0.16f)
        lineTo(s * 0.84f, s * 0.36f)
        lineTo(s * 0.40f, s * 0.80f)
        close()
    }
    drawPath(body, color, style = stroke)
    drawLine(color, Offset(s * 0.54f, s * 0.26f), Offset(s * 0.74f, s * 0.46f), strokeWidth = s * 0.1f, cap = StrokeCap.Round)
}

/** A backspace key: a tag pointing left with a cross in it. */
private fun DrawScope.drawErase(color: Color) {
    val s = size.minDimension
    val stroke = Stroke(width = s * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val tag = Path().apply {
        moveTo(s * 0.04f, s * 0.5f)
        lineTo(s * 0.30f, s * 0.18f)
        lineTo(s * 0.96f, s * 0.18f)
        lineTo(s * 0.96f, s * 0.82f)
        lineTo(s * 0.30f, s * 0.82f)
        close()
    }
    drawPath(tag, color, style = stroke)
    drawLine(color, Offset(s * 0.48f, s * 0.38f), Offset(s * 0.76f, s * 0.62f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
    drawLine(color, Offset(s * 0.76f, s * 0.38f), Offset(s * 0.48f, s * 0.62f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
}

@Composable
private fun SudokuResultCard(page: SudokuSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    // A sudoku is only ever won.
    val time = formatDuration(page.durationMs)

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.sudoku_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.sudoku_result_won, time),
                style = type.body.copy(color = colors.textPrimary),
            )
            BasicText(
                text = swipeHintText(),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}
