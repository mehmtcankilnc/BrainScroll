package com.mehmtcan.brainscroll.ui.sudoku

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import brainscroll.shared.generated.resources.sudoku_lost
import brainscroll.shared.generated.resources.sudoku_mistakes
import brainscroll.shared.generated.resources.sudoku_notes
import brainscroll.shared.generated.resources.sudoku_result_lost
import brainscroll.shared.generated.resources.sudoku_result_won
import brainscroll.shared.generated.resources.sudoku_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuResult
import com.mehmtcan.brainscroll.game.sudoku.SudokuSnapshot
import com.mehmtcan.brainscroll.game.sudoku.SudokuState
import com.mehmtcan.brainscroll.game.sudoku.SudokuStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 2.dp

/** The extra room between two boxes, on top of the two cell gaps. */
private val BoxExtra = 4.dp
private val MaxCell = 44.dp
private val KeyHeight = 52.dp

/**
 * What a cell shows, for tests: 0 empty, 1..9 a digit, and `+100` for a wrong digit, `+200` for notes only (then the
 * value is 200 plus the notes mask).
 */
internal val SudokuCellValue = SemanticsPropertyKey<Int>("SudokuCellValue")
private var SemanticsPropertyReceiver.cellValue by SudokuCellValue

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

    val status = when (state.status) {
        SudokuStatus.Playing -> WordleStatus.Playing
        SudokuStatus.Won -> WordleStatus.Won
        SudokuStatus.Lost -> WordleStatus.Lost
    }
    OutcomeHaptics(status)
    val justFinished = rememberJustFinished(status)

    var selected by remember(page.id) { mutableStateOf<Int?>(null) }
    var notesMode by remember(page.id) { mutableStateOf(false) }

    // A row, column, box or the whole board that was just completed gets a wave. A page that is merely shown again
    // (restored, scrolled back to) starts from what it already has, so it stays quiet.
    var wave by remember(page.id) { mutableStateOf(Wave(0, emptyMap())) }
    val seen = remember(page.id) { Seen() }
    LaunchedEffect(state.values, state.wrong) {
        val done = completedUnits(state)
        val before = seen.done
        if (before != null && done.size > before.size) {
            val origin = state.values.indices.firstOrNull { state.values[it] != 0 && state.values[it] != seen.values[it] } ?: 0
            wave = Wave(wave.id + 1, waveDelays(done - before, origin))
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
                text = stringResource(Res.string.sudoku_mistakes, state.mistakes, state.maxMistakes),
                textColor = if (state.mistakes >= state.maxMistakes - 1 && state.mistakes > 0) colors.error else colors.textSecondary,
                description = stringResource(Res.string.a11y_sudoku_mistakes, state.mistakes, state.maxMistakes),
            )
            if (!page.isFinished) {
                Box(Modifier.testTag("sudokuNotes")) {
                    Chip(
                        text = stringResource(Res.string.sudoku_notes),
                        textColor = if (notesMode) colors.textPrimary else colors.textSecondary,
                        onClick = { notesMode = !notesMode },
                        raised = notesMode,
                        selected = notesMode,
                        description = stringResource(if (notesMode) Res.string.a11y_sudoku_notes_on else Res.string.a11y_sudoku_notes_off),
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))

        Board(
            state = state,
            selected = selected,
            wave = wave,
            onSelect = { selected = it },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (page.isFinished) {
                SudokuResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                NumberPad(state = state, notesMode = notesMode, onDigit = ::digit, onErase = ::erase)
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** The 81 cells as equal squares, as large as the room allows, with a wider gap between the nine boxes. */
@Composable
private fun Board(state: SudokuState, selected: Int?, wave: Wave, onSelect: (Int) -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val size = SudokuGenerator.SIZE
        val gaps = CellGap * 6 + (CellGap * 2 + BoxExtra) * 2
        val cellSize: Dp = minOf(MaxCell, (maxWidth - gaps) / size, (maxHeight - gaps) / size)

        val selectedValue = selected?.let { state.values[it] } ?: 0
        val selectedPeers = selected?.let { SudokuGenerator.peers(it).toSet() } ?: emptySet()

        Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
            for (row in 0 until size) {
                if (row == 3 || row == 6) Spacer(Modifier.height(BoxExtra))
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    for (column in 0 until size) {
                        if (column == 3 || column == 6) Spacer(Modifier.width(BoxExtra))
                        val index = row * size + column
                        GuideMark(isGuided(index), Radius.tile) {
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
    val shape = Radius.tile

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

    val background = when {
        wrong -> colors.error.copy(alpha = 0.28f)
        isSelected || sameDigit || isPeer -> colors.bgRaised
        else -> colors.bgSurface
    }
    val border = when {
        isSelected -> (2f + extraBorder).dp to colors.pendingFill
        sameDigit -> (1.5f + extraBorder).dp to colors.pendingFill.copy(alpha = 0.55f)
        else -> (1f + extraBorder).dp to colors.borderStrong.copy(alpha = 0.3f)
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
            .background(background, shape)
            .background(colors.correctFill.copy(alpha = flash.value * 0.55f), shape)
            .border(border.first, border.second, shape)
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
private fun completedUnits(state: SudokuState): Set<Int> {
    val done = mutableSetOf<Int>()
    SudokuGenerator.UNITS.forEachIndexed { unit, cells ->
        if (cells.all { state.values[it] != 0 && it !in state.wrong }) done += unit
    }
    if (state.values.indices.all { state.values[it] != 0 && it !in state.wrong }) done += BOARD
    return done
}

/** Each cell of the [units] and how long to wait before it swells: further from the [origin] cell, later. */
private fun waveDelays(units: Set<Int>, origin: Int): Map<Int, Int> {
    val cells = units.flatMap { if (it == BOARD) (0 until SudokuGenerator.CELLS).toList() else SudokuGenerator.UNITS[it].toList() }.toSet()
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

/** Nine digit keys and an eraser, in two rows. A digit that is already complete on the board is dimmed. */
@Composable
private fun NumberPad(state: SudokuState, notesMode: Boolean, onDigit: (Int) -> Unit, onErase: () -> Unit) {
    val done = (1..9).associateWith { digit ->
        state.values.indices.count { state.values[it] == digit && it !in state.wrong } >= 9
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            for (digit in 1..5) DigitKey(digit, notesMode, !done.getValue(digit), { onDigit(digit) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            for (digit in 6..9) DigitKey(digit, notesMode, !done.getValue(digit), { onDigit(digit) }, Modifier.weight(1f))
            EraseKey(onErase, Modifier.weight(1f))
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

@Composable
private fun EraseKey(onClick: () -> Unit, modifier: Modifier) {
    val color = BrainScrollTheme.colors.textPrimary
    Key(modifier, stringResource(Res.string.a11y_sudoku_erase), "sudokuErase", true, onClick) {
        Canvas(Modifier.size(26.dp)) { drawErase(color) }
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
    content: @Composable () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    Box(
        modifier = modifier
            .height(KeyHeight)
            .testTag(tag)
            .alpha(if (enabled) 1f else 0.35f)
            .background(colors.bgSurface, Radius.control)
            .border(1.5.dp, colors.borderStrong.copy(alpha = 0.6f), Radius.control)
            .semantics {
                contentDescription = description
                role = Role.Button
                if (!enabled) disabled()
            }
            .then(
                if (enabled) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
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
    val won = page.state.status == SudokuStatus.Won
    val time = formatDuration(page.durationMs)

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(if (won) Res.string.sudoku_won else Res.string.sudoku_lost),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = if (won) colors.correctFill else colors.textPrimary, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(if (won) Res.string.sudoku_result_won else Res.string.sudoku_result_lost, time),
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
