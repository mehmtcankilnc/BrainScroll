package com.mehmtcan.brainscroll.ui.crosssum

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_crosssum_board
import brainscroll.shared.generated.resources.a11y_crosssum_broken
import brainscroll.shared.generated.resources.a11y_crosssum_cell
import brainscroll.shared.generated.resources.a11y_crosssum_clue_across
import brainscroll.shared.generated.resources.a11y_crosssum_clue_down
import brainscroll.shared.generated.resources.a11y_crosssum_digit
import brainscroll.shared.generated.resources.a11y_crosssum_empty
import brainscroll.shared.generated.resources.a11y_crosssum_given
import brainscroll.shared.generated.resources.a11y_crosssum_key
import brainscroll.shared.generated.resources.a11y_crosssum_restart
import brainscroll.shared.generated.resources.crosssum_filled
import brainscroll.shared.generated.resources.crosssum_hint
import brainscroll.shared.generated.resources.crosssum_result
import brainscroll.shared.generated.resources.crosssum_won
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.crosssum.CrossPuzzle
import com.mehmtcan.brainscroll.game.crosssum.CrossResult
import com.mehmtcan.brainscroll.game.crosssum.CrossSnapshot
import com.mehmtcan.brainscroll.game.crosssum.CrossStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.isGuideEvidence
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource

/** At least this high, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 124.dp
private val MaxCell = 52.dp
private val CellGap = 3.dp
private val KeyHeight = 40.dp
private val KeyGap = 6.dp

/** The digit of a cell (0 for empty), for tests. */
internal val CrossValueKey = SemanticsPropertyKey<Int>("CrossValue")
private var SemanticsPropertyReceiver.crossValue by CrossValueKey

/** True for a cell that was given, for tests. */
internal val CrossGivenKey = SemanticsPropertyKey<Boolean>("CrossGiven")
private var SemanticsPropertyReceiver.crossGiven by CrossGivenKey

/** True for a cell that breaks a rule now, for tests. */
internal val CrossBrokenKey = SemanticsPropertyKey<Boolean>("CrossBroken")
private var SemanticsPropertyReceiver.crossBroken by CrossBrokenKey

/** The digit of a key, for tests. */
internal val CrossKeyValueKey = SemanticsPropertyKey<Int>("CrossKeyValue")
private var SemanticsPropertyReceiver.crossKeyValue by CrossKeyValueKey

/** The saved board, for tests (a screen reader does not read it). */
internal val CrossPuzzleKey = SemanticsPropertyKey<String>("CrossPuzzle")
private var SemanticsPropertyReceiver.crossPuzzle by CrossPuzzleKey

/**
 * One full-screen page of the feed: how many cells are filled, the board (black cells with the sums of the blocks, white cells for the
 * digits), the keys 1 to 9 and the restart button. The player taps a white cell to select it and then a key to put that digit in it;
 * the same key again empties the cell. The result takes the place of the hint and the keys when the board is solved. A block that
 * breaks a rule (a digit twice, more than its sum, a full block that does not add up) shows its cells in red.
 */
@Composable
fun CrossSumPage(
    page: CrossSnapshot,
    onPut: (Int, Int) -> CrossResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val puzzle = state.puzzle
    val won = state.status == CrossStatus.Won

    // The result and the buzz wait a moment so the last digit can be seen.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    val broken = state.conflicts
    // The cell the keys write to (-1: none). A new board starts with none, and so does a board that is won.
    var selected by remember(page.id) { mutableIntStateOf(-1) }
    if (won && selected >= 0) selected = -1

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(
            text = stringResource(Res.string.crosssum_filled, state.filled, puzzle.whiteCells.size),
            textColor = if (won) colors.correctFill else colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val across = puzzle.columns + 1
            val down = puzzle.rows + 1
            val cellSize: Dp = minOf(MaxCell, (maxWidth - CellGap * (across - 1)) / across, (maxHeight - CellGap * (down - 1)) / down)
            val description = stringResource(Res.string.a11y_crosssum_board, puzzle.rows, puzzle.columns, state.filled, puzzle.whiteCells.size)
            val saved = puzzle.encode()
            Column(
                modifier = Modifier.testTag("crossBoard").semantics {
                    contentDescription = description
                    crossPuzzle = saved
                },
                verticalArrangement = Arrangement.spacedBy(CellGap),
            ) {
                for (frameRow in 0..puzzle.rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                        for (frameColumn in 0..puzzle.columns) {
                            val cell = if (frameRow > 0 && frameColumn > 0) (frameRow - 1) * puzzle.columns + (frameColumn - 1) else -1
                            if (cell >= 0 && puzzle.white[cell]) {
                                GuideMark(isGuided(cell) && !won, Radius.tile, evidence = isGuideEvidence(cell) && !won) {
                                    CrossCell(
                                        row = frameRow,
                                        column = frameColumn,
                                        value = state.values[cell],
                                        given = puzzle.isGiven(cell),
                                        broken = cell in broken,
                                        playing = !won,
                                        selected = cell == selected,
                                        size = cellSize,
                                        onClick = {
                                            selected = if (selected == cell) -1 else cell
                                            haptics.perform(HapticKind.Light)
                                        },
                                    )
                                }
                            } else {
                                val across = puzzle.runs.firstOrNull { it.across && it.clueRow == frameRow && it.clueColumn == frameColumn }?.sum
                                val down = puzzle.runs.firstOrNull { !it.across && it.clueRow == frameRow && it.clueColumn == frameColumn }?.sum
                                ClueCell(across, down, cellSize)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                CrossSumResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    BasicText(
                        text = stringResource(Res.string.crosssum_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    val active = selected >= 0
                    fun press(digit: Int) {
                        // The same digit again empties the cell.
                        when (onPut(selected, if (state.values[selected] == digit) 0 else digit)) {
                            CrossResult.Placed -> haptics.perform(HapticKind.Medium)
                            CrossResult.Cleared -> haptics.perform(HapticKind.Light)
                            CrossResult.Solved, CrossResult.Ignored -> Unit
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                        for (digit in 1..5) DigitKey(digit, active, { press(digit) }, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KeyGap), verticalAlignment = Alignment.CenterVertically) {
                        for (digit in 6..9) DigitKey(digit, active, { press(digit) }, Modifier.weight(1f))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            RoundIconButton(
                                description = stringResource(Res.string.a11y_crosssum_restart),
                                onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                                enabled = puzzle.openCells.any { state.values[it] > 0 },
                                tag = "crossRestart",
                            ) { drawRestart(ink) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** One key under the board. It does nothing while no cell is selected. */
@Composable
private fun DigitKey(digit: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.CrossSum.world
    val density = LocalDensity.current
    val description = stringResource(Res.string.a11y_crosssum_key, digit)
    Box(
        modifier = modifier
            .height(KeyHeight)
            .testTag("crossKey")
            .alpha(if (active) 1f else 0.45f)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                crossKeyValue = digit
                role = Role.Button
            }
            .background(colors.bgSurface, Radius.tile)
            .border(1.5.dp, if (active) world else colors.borderStrong.copy(alpha = 0.5f), Radius.tile)
            .clickable(enabled = active, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = digit.toString(),
            style = TextStyle(fontSize = with(density) { (KeyHeight.toPx() * 0.45f).toSp() }, fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.Center),
        )
    }
}

/**
 * A cell of the frame or a black cell: dark, with a line from the top left to the bottom right when it has a sum. The sum of the block
 * along the row is at the top right, the one of the block down the column at the bottom left.
 */
@Composable
private fun ClueCell(across: Int?, down: Int?, size: Dp) {
    val colors = BrainScrollTheme.colors
    val shape = Radius.tile
    val density = LocalDensity.current
    val small = with(density) { (size.toPx() * 0.3f).toSp() }
    val acrossText = across?.let { stringResource(Res.string.a11y_crosssum_clue_across, it) }
    val downText = down?.let { stringResource(Res.string.a11y_crosssum_clue_down, it) }
    Box(
        modifier = Modifier
            .size(size)
            .background(colors.bgPage, shape)
            .border(1.dp, colors.borderStrong.copy(alpha = 0.28f), shape)
            .semantics(mergeDescendants = true) {
                if (across != null || down != null) {
                    contentDescription = listOfNotNull(acrossText, downText).joinToString(", ")
                }
            },
    ) {
        if (across != null || down != null) {
            val line = colors.borderStrong.copy(alpha = 0.6f)
            Canvas(Modifier.size(size)) {
                drawLine(line, Offset(0f, 0f), Offset(this.size.width, this.size.height), 1.dp.toPx())
            }
            if (across != null) {
                BasicText(
                    text = across.toString(),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 3.dp),
                    style = TextStyle(fontSize = small, fontWeight = FontWeight.Bold, color = colors.textSecondary),
                )
            }
            if (down != null) {
                BasicText(
                    text = down.toString(),
                    modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 2.dp, start = 3.dp),
                    style = TextStyle(fontSize = small, fontWeight = FontWeight.Bold, color = colors.textSecondary),
                )
            }
        }
    }
}

/**
 * A white cell. A given cell has a lighter tile and cannot be changed; a cell the player fills has the plain tile. The selected cell has
 * a ring of the colour of the game; a cell that breaks a rule has its digit and its ring in red.
 */
@Composable
private fun CrossCell(
    row: Int,
    column: Int,
    value: Int,
    given: Boolean,
    broken: Boolean,
    playing: Boolean,
    selected: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.CrossSum.world
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.tile
    val density = LocalDensity.current
    val digitSize = with(density) { (size.toPx() * 0.5f).toSp() }
    val border = when {
        broken -> colors.error
        selected -> world
        else -> colors.borderStrong.copy(alpha = if (given) 0.28f else 0.5f)
    }
    val description = stringResource(Res.string.a11y_crosssum_cell, row, column)
    val digitText = if (value > 0) stringResource(Res.string.a11y_crosssum_digit, value) else stringResource(Res.string.a11y_crosssum_empty)
    val givenText = if (given) stringResource(Res.string.a11y_crosssum_given) else ""
    val brokenText = if (broken) stringResource(Res.string.a11y_crosssum_broken) else ""
    Box(
        modifier = Modifier
            .size(size)
            .testTag("crossCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = listOf(digitText, givenText, brokenText).filter { it.isNotEmpty() }.joinToString(", ")
                crossValue = value
                crossGiven = given
                crossBroken = broken
                role = Role.Button
            }
            .background(if (given) colors.bgRaised else colors.bgSurface, shape)
            .border((if (selected) 2.5f else 1.5f + extraBorder).dp, border, shape)
            .then(
                if (playing && !given) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (value > 0) {
            BasicText(
                text = value.toString(),
                style = TextStyle(
                    fontSize = digitSize,
                    fontWeight = FontWeight.Bold,
                    color = if (broken) colors.error else colors.textPrimary,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

@Composable
private fun CrossSumResultCard(page: CrossSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("crossResultCard").resultSurface(colors.bgSurface, GameKind.CrossSum.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.crosssum_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.crosssum_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.CrossSum.world),
                ),
            )
            BasicText(
                text = swipeHintText(),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** A small picture for the thumbnails: a 4 by 4 frame with the black cells that carry sums and a few digits. */
@Composable
internal fun CrossSumThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.CrossSum.world
    val density = LocalDensity.current
    val digits = mapOf(5 to 3, 6 to 1, 9 to 2, 10 to 4)
    val fontSize = with(density) { (side.toPx() * 0.9f / 4f * 0.5f).toSp() }
    Column(Modifier.size(side * 0.9f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until 4) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (column in 0 until 4) {
                    val cell = row * 4 + column
                    val clue = row == 0 || column == 0
                    Box(
                        Modifier.weight(1f).fillMaxSize()
                            .background(if (clue) colors.bgPage else colors.bgSurface, Radius.tile)
                            .border(1.dp, colors.borderStrong.copy(alpha = if (clue) 0.28f else 0.5f), Radius.tile),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (clue && cell != 0) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawLine(world.copy(alpha = 0.7f), Offset(0f, 0f), Offset(size.width, size.height), 1.5.dp.toPx())
                            }
                        }
                        digits[cell]?.let {
                            BasicText(text = it.toString(), style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold, color = colors.textPrimary))
                        }
                    }
                }
            }
        }
    }
}
