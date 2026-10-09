package com.mehmtcan.brainscroll.ui.mines

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_cell_closed
import brainscroll.shared.generated.resources.a11y_cell_empty
import brainscroll.shared.generated.resources.a11y_cell_exploded
import brainscroll.shared.generated.resources.a11y_cell_flag_action
import brainscroll.shared.generated.resources.a11y_cell_flagged
import brainscroll.shared.generated.resources.a11y_cell_mine
import brainscroll.shared.generated.resources.a11y_cell_number
import brainscroll.shared.generated.resources.a11y_mines_flag_mode_off
import brainscroll.shared.generated.resources.a11y_mines_flag_mode_on
import brainscroll.shared.generated.resources.a11y_mines_left
import brainscroll.shared.generated.resources.game_mines
import brainscroll.shared.generated.resources.mines_hint
import brainscroll.shared.generated.resources.mines_left
import brainscroll.shared.generated.resources.mines_lost
import brainscroll.shared.generated.resources.mines_result_lost
import brainscroll.shared.generated.resources.mines_result_won
import brainscroll.shared.generated.resources.mines_won
import brainscroll.shared.generated.resources.swipe_hint
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.mines.MinesResult
import com.mehmtcan.brainscroll.game.mines.MinesSnapshot
import com.mehmtcan.brainscroll.game.mines.MinesState
import com.mehmtcan.brainscroll.game.mines.MinesStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.howto.HowToPlayButton
import com.mehmtcan.brainscroll.ui.theme.BrainScrollColors
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 3.dp
private val MaxCell = 44.dp

/**
 * What a cell shows, for tests: -1 closed, -2 flagged, 0..8 open with that many mines around, -3 a mine (after a loss),
 * -4 the mine that was opened.
 */
internal val CellValue = SemanticsPropertyKey<Int>("CellValue")
private var SemanticsPropertyReceiver.cellValue by CellValue

/** One full-screen page of the feed: the title, the mine counter, the board, and the flag switch or the result. */
@Composable
fun MinesPage(
    page: MinesSnapshot,
    onOpen: (Int) -> MinesResult,
    onFlag: (Int) -> Boolean,
    onChord: (Int) -> MinesResult,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onHowToPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state

    val status = when (state.status) {
        MinesStatus.Playing -> WordleStatus.Playing
        MinesStatus.Won -> WordleStatus.Won
        MinesStatus.Lost -> WordleStatus.Lost
    }
    OutcomeHaptics(status)
    val justFinished = rememberJustFinished(status)

    // In flag mode a tap flags and a long press opens: the two swap places.
    var flagMode by remember(page.id) { mutableStateOf(false) }

    fun open(cell: Int) {
        // A mine is buzzed by the outcome haptics, so only the opens that go well are felt here.
        if (onOpen(cell) == MinesResult.Opened) haptics.perform(HapticKind.Light)
    }

    fun flag(cell: Int) {
        if (onFlag(cell)) haptics.perform(HapticKind.Medium)
    }

    fun chord(cell: Int) {
        if (onChord(cell) == MinesResult.Opened) haptics.perform(HapticKind.Light)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        BasicText(stringResource(Res.string.game_mines), style = type.heading.copy(color = colors.textPrimary))
        HowToPlayButton(onHowToPlay)
        Spacer(Modifier.height(Spacing.sm))
        Chip(
            text = stringResource(Res.string.mines_left, state.minesLeft),
            textColor = if (state.minesLeft < 0) colors.error else colors.textSecondary,
            description = stringResource(Res.string.a11y_mines_left, state.minesLeft),
        )
        Spacer(Modifier.height(Spacing.md))

        Board(
            state = state,
            flagMode = flagMode,
            onOpen = ::open,
            onFlag = ::flag,
            onChord = ::chord,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (page.isFinished) {
                MinesResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.mines_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val flagColor = colors.error
                    val poleColor = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(if (flagMode) Res.string.a11y_mines_flag_mode_on else Res.string.a11y_mines_flag_mode_off),
                        onClick = { flagMode = !flagMode },
                        selected = flagMode,
                        tag = "minesFlagMode",
                    ) { drawFlag(poleColor, flagColor) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** The cells as equal squares, as large as the room allows (the board is square, the page is tall). */
@Composable
private fun Board(
    state: MinesState,
    flagMode: Boolean,
    onOpen: (Int) -> Unit,
    onFlag: (Int) -> Unit,
    onChord: (Int) -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val byWidth = (maxWidth - CellGap * (state.cols - 1)) / state.cols
        val byHeight = (maxHeight - CellGap * (state.rows - 1)) / state.rows
        val cellSize: Dp = minOf(MaxCell, byWidth, byHeight)

        Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
            for (row in 0 until state.rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    for (column in 0 until state.cols) {
                        val index = row * state.cols + column
                        Cell(
                            row = row + 1,
                            column = column + 1,
                            number = state.numbers[index],
                            flagged = index in state.flagged,
                            mine = index in state.mineCells,
                            exploded = index == state.exploded,
                            playing = state.status == MinesStatus.Playing,
                            flagMode = flagMode,
                            size = cellSize,
                            onOpen = { onOpen(index) },
                            onFlag = { onFlag(index) },
                            onChord = { onChord(index) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * One cell. Closed: a tap does the main action (open, or flag in flag mode) and a long press the other one. Open with
 * a number: a double tap chords. For a screen reader the same things are the click (open, flag, or chord) and a
 * custom long click for the other action, so nothing needs a gesture.
 */
@Composable
private fun Cell(
    row: Int,
    column: Int,
    number: Int,
    flagged: Boolean,
    mine: Boolean,
    exploded: Boolean,
    playing: Boolean,
    flagMode: Boolean,
    size: Dp,
    onOpen: () -> Unit,
    onFlag: () -> Unit,
    onChord: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val open = number >= 0

    val value = when {
        exploded -> -4
        mine -> -3
        flagged -> -2
        open -> number
        else -> -1
    }
    val description = when {
        exploded -> stringResource(Res.string.a11y_cell_exploded, row, column)
        mine -> stringResource(Res.string.a11y_cell_mine, row, column)
        flagged -> stringResource(Res.string.a11y_cell_flagged, row, column)
        open && number == 0 -> stringResource(Res.string.a11y_cell_empty, row, column)
        open -> stringResource(Res.string.a11y_cell_number, row, column, number)
        else -> stringResource(Res.string.a11y_cell_closed, row, column)
    }
    val flagAction = stringResource(Res.string.a11y_cell_flag_action)

    // The main and the other action of a closed cell.
    val primary = if (flagMode) onFlag else onOpen
    val secondary = if (flagMode) onOpen else onFlag

    val shape = Radius.tile
    Box(
        modifier = Modifier
            .size(size)
            .testTag("mineCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                cellValue = value
                role = Role.Button
                if (playing) {
                    when {
                        open && number > 0 -> onClick { onChord(); true }
                        !open -> {
                            onClick { primary(); true }
                            onLongClick(label = flagAction) { secondary(); true }
                        }
                    }
                }
            }
            .background(if (open || mine) colors.bgSurface else colors.bgRaised, shape)
            .then(if (!open && !mine) Modifier.border((1f + extraBorder).dp, colors.borderStrong.copy(alpha = 0.6f), shape) else Modifier)
            .then(if (exploded) Modifier.background(colors.error.copy(alpha = 0.45f), shape) else Modifier)
            .then(
                if (!playing) Modifier
                else if (open) Modifier.pointerInput(number) { detectTapGestures(onDoubleTap = { onChord() }) }
                else Modifier.pointerInput(flagMode) { detectTapGestures(onTap = { primary() }, onLongPress = { secondary() }) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            mine -> Canvas(Modifier.size(size * 0.6f)) { drawMine(colors.textPrimary, colors.error) }
            flagged -> Canvas(Modifier.size(size * 0.62f)) { drawFlag(colors.textPrimary, colors.error) }
            open && number > 0 -> BasicText(
                text = number.toString(),
                style = BrainScrollTheme.typography.heading.copy(color = numberColor(number, colors)),
            )
        }
    }
}

private fun numberColor(number: Int, colors: BrainScrollColors): Color = when (number) {
    1 -> colors.streakAnswer
    2 -> colors.correctFill
    3 -> colors.error
    4 -> colors.pendingFill
    5 -> colors.streakDay
    else -> colors.textPrimary
}

/** A pole with a pennant. */
private fun DrawScope.drawFlag(pole: Color, flag: Color) {
    val s = size.minDimension
    drawLine(pole, Offset(s * 0.30f, s * 0.12f), Offset(s * 0.30f, s * 0.88f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
    drawLine(pole, Offset(s * 0.16f, s * 0.88f), Offset(s * 0.62f, s * 0.88f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
    val pennant = Path().apply {
        moveTo(s * 0.30f, s * 0.14f)
        lineTo(s * 0.86f, s * 0.34f)
        lineTo(s * 0.30f, s * 0.56f)
        close()
    }
    drawPath(pennant, flag)
}

/** A round bomb with spikes. */
private fun DrawScope.drawMine(body: Color, spark: Color) {
    val s = size.minDimension
    val center = Offset(s / 2, s / 2)
    for (i in 0 until 8) {
        val angle = i * PI.toFloat() / 4f
        drawLine(
            body,
            Offset(center.x + cos(angle) * s * 0.30f, center.y + sin(angle) * s * 0.30f),
            Offset(center.x + cos(angle) * s * 0.48f, center.y + sin(angle) * s * 0.48f),
            strokeWidth = s * 0.09f,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(body, radius = s * 0.30f, center = center)
    drawCircle(spark, radius = s * 0.08f, center = Offset(center.x - s * 0.08f, center.y - s * 0.08f))
}

@Composable
private fun MinesResultCard(page: MinesSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val won = page.state.status == MinesStatus.Won
    val time = formatDuration(page.durationMs)

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(if (won) Res.string.mines_won else Res.string.mines_lost),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = if (won) colors.correctFill else colors.textPrimary, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(if (won) Res.string.mines_result_won else Res.string.mines_result_lost, time),
                style = type.body.copy(color = colors.textPrimary),
            )
            BasicText(
                text = "↑  " + stringResource(Res.string.swipe_hint),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}
