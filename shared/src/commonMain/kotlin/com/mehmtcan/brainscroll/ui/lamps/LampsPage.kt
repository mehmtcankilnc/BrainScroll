package com.mehmtcan.brainscroll.ui.lamps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import brainscroll.shared.generated.resources.a11y_lamps_board
import brainscroll.shared.generated.resources.a11y_lamps_cell
import brainscroll.shared.generated.resources.a11y_lamps_clash
import brainscroll.shared.generated.resources.a11y_lamps_dark
import brainscroll.shared.generated.resources.a11y_lamps_dot
import brainscroll.shared.generated.resources.a11y_lamps_empty
import brainscroll.shared.generated.resources.a11y_lamps_lamp
import brainscroll.shared.generated.resources.a11y_lamps_lit
import brainscroll.shared.generated.resources.a11y_lamps_restart
import brainscroll.shared.generated.resources.a11y_lamps_wall
import brainscroll.shared.generated.resources.a11y_lamps_wall_number
import brainscroll.shared.generated.resources.lamps_hint
import brainscroll.shared.generated.resources.lamps_lit
import brainscroll.shared.generated.resources.lamps_result
import brainscroll.shared.generated.resources.lamps_won
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.lamps.LampMark
import com.mehmtcan.brainscroll.game.lamps.LampPuzzle
import com.mehmtcan.brainscroll.game.lamps.LampResult
import com.mehmtcan.brainscroll.game.lamps.LampRules
import com.mehmtcan.brainscroll.game.lamps.LampSnapshot
import com.mehmtcan.brainscroll.game.lamps.LampStatus
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
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
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
private val BottomAreaHeight = 152.dp
private val CellGap = 4.dp
private val MaxCell = 56.dp

/** What a cell holds, for tests: 0 empty, 1 lamp, 2 dot (a wall is 0). */
internal val LampMarkKey = SemanticsPropertyKey<Int>("LampMark")
private var SemanticsPropertyReceiver.lampMark by LampMarkKey

/** Whether a cell is lit, for tests. */
internal val LampLitKey = SemanticsPropertyKey<Boolean>("LampLit")
private var SemanticsPropertyReceiver.lampLit by LampLitKey

/** Whether a cell is a wall, for tests. */
internal val LampWallKey = SemanticsPropertyKey<Boolean>("LampWall")
private var SemanticsPropertyReceiver.lampWall by LampWallKey

/** What the board is, for tests: the side and the cells as the puzzle holds them (white, wall number or blank wall). */
internal val LampSizeKey = SemanticsPropertyKey<Int>("LampSize")
private var SemanticsPropertyReceiver.lampSize by LampSizeKey
internal val LampCellsKey = SemanticsPropertyKey<List<Int>>("LampCells")
private var SemanticsPropertyReceiver.lampCells by LampCellsKey

/**
 * One full-screen page of the feed: how many white cells are lit, the board (tap a white cell to put a lamp, again to put a dot that says
 * "no lamp here", again to empty it), and the restart button. The result takes the place of the hint and the button when the board is
 * solved. The cells a lamp lights are tinted; lamps that see each other and walls with too many lamps are red; a wall that has its number
 * is green.
 */
@Composable
fun LampsPage(
    page: LampSnapshot,
    onTap: (Int) -> LampResult,
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
    val n = puzzle.size
    val won = state.status == LampStatus.Won

    // The result and the buzz wait a moment so the last lamp can be seen where it was put.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    val lamps = state.lamps
    val lit = state.lit
    val clashing = state.clashing
    val overfull = state.overfull
    val satisfied = state.satisfied

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(text = stringResource(Res.string.lamps_lit, state.litCount, puzzle.whiteCells.size), textColor = if (won) colors.correctFill else colors.textSecondary)
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cellSize: Dp = minOf(MaxCell, (maxWidth - CellGap * (n - 1)) / n, (maxHeight - CellGap * (n - 1)) / n)
            val description = stringResource(Res.string.a11y_lamps_board, n, n, state.litCount, puzzle.whiteCells.size)
            Column(
                modifier = Modifier.testTag("lampBoard").semantics {
                    contentDescription = description
                    lampSize = n
                    lampCells = puzzle.cells
                },
                verticalArrangement = Arrangement.spacedBy(CellGap),
            ) {
                for (row in 0 until n) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                        for (column in 0 until n) {
                            val cell = row * n + column
                            GuideMark(isGuided(cell) && !won, Radius.tile, evidence = isGuideEvidence(cell) && !won) {
                                LampCell(
                                    puzzle = puzzle,
                                    row = row + 1,
                                    column = column + 1,
                                    cell = cell,
                                    mark = state.marks[cell],
                                    lit = lit[cell],
                                    clash = cell in clashing,
                                    overfull = cell in overfull,
                                    satisfied = cell in satisfied,
                                    lampsAround = if (puzzle.isWall(cell)) LampRules.lampsAround(puzzle, lamps, cell) else 0,
                                    playing = !won,
                                    size = cellSize,
                                    onClick = {
                                        when (onTap(cell)) {
                                            LampResult.Lamp -> haptics.perform(HapticKind.Medium)
                                            LampResult.Dot, LampResult.Cleared -> haptics.perform(HapticKind.Light)
                                            LampResult.Solved, LampResult.Ignored -> Unit
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                LampsResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.lamps_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_lamps_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.marks.any { it != LampMark.Empty },
                        tag = "lampRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/**
 * One cell. A wall is a solid tile (with its number if it has one: red when it has too many lamps, green when it has its number). A white
 * cell is tinted when a lamp lights it; a lamp is a bright disc (red when it sees another lamp), a dot is a small grey disc. So the three
 * marks are told apart by their shape and not only by their colour.
 */
@Composable
private fun LampCell(
    puzzle: LampPuzzle,
    row: Int,
    column: Int,
    cell: Int,
    mark: LampMark,
    lit: Boolean,
    clash: Boolean,
    overfull: Boolean,
    satisfied: Boolean,
    lampsAround: Int,
    playing: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.Lamps.world
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.tile
    val duration = if (LocalReduceMotion.current) 0 else Motion.BASE
    val wall = puzzle.isWall(cell)
    val fill by animateColorAsState(
        when {
            wall -> colors.borderStrong
            lit -> world.copy(alpha = 0.3f)
            else -> colors.bgSurface
        },
        tween(duration, easing = Motion.Standard),
    )
    val border = when {
        wall -> colors.borderStrong
        clash -> colors.error
        lit -> world.copy(alpha = 0.55f)
        else -> colors.borderStrong.copy(alpha = 0.5f)
    }
    val position = stringResource(Res.string.a11y_lamps_cell, row, column)
    val number = puzzle.numberAt(cell)
    val wallText = if (number >= 0) stringResource(Res.string.a11y_lamps_wall_number, number, lampsAround) else stringResource(Res.string.a11y_lamps_wall)
    val markText = stringResource(
        when (mark) {
            LampMark.Empty -> Res.string.a11y_lamps_empty
            LampMark.Lamp -> Res.string.a11y_lamps_lamp
            LampMark.Dot -> Res.string.a11y_lamps_dot
        },
    )
    val litText = stringResource(if (lit) Res.string.a11y_lamps_lit else Res.string.a11y_lamps_dark)
    val clashText = stringResource(Res.string.a11y_lamps_clash)
    val density = LocalDensity.current
    val numberSize = with(density) { (size.toPx() * 0.5f).toSp() }

    Box(
        modifier = Modifier
            .size(size)
            .testTag("lampCell")
            .semantics(mergeDescendants = true) {
                contentDescription = if (wall) "$position, $wallText" else position
                if (!wall) stateDescription = listOfNotNull(markText, litText, clashText.takeIf { clash }).joinToString(", ")
                lampMark = mark.ordinal
                lampLit = lit
                lampWall = wall
                if (!wall) role = Role.Button
            }
            .background(fill, shape)
            .border((1.5f + extraBorder).dp, border, shape)
            .then(
                if (playing && !wall) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (wall) {
            if (number >= 0) {
                BasicText(
                    text = number.toString(),
                    style = TextStyle(
                        fontSize = numberSize,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            overfull -> colors.error
                            satisfied -> colors.correctFill
                            else -> colors.textPrimary
                        },
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        } else if (mark == LampMark.Lamp) {
            val bulb = if (clash) colors.error else world
            Box(
                Modifier.size(size * 0.56f)
                    .drawBehind { drawCircle(bulb.copy(alpha = 0.35f), radius = this.size.minDimension * 0.9f) }
                    .background(bulb, CircleShape),
            )
        } else if (mark == LampMark.Dot) {
            Box(Modifier.size(size * 0.22f).background(colors.textSecondary, CircleShape))
        }
    }
}

@Composable
private fun LampsResultCard(page: LampSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("lampResultCard").resultSurface(colors.bgSurface, GameKind.Lamps.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.lamps_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.lamps_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.Lamps.world),
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

/** A small picture for the thumbnails: a solved 4 by 4 board, three walls (one with a 2), four lamps and everything lit. */
@Composable
internal fun LampsThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.Lamps.world
    val density = LocalDensity.current
    val walls = setOf(0, 5, 15)
    val lamps = setOf(1, 4, 11, 14)
    Column(Modifier.size(side * 0.9f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until 4) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (column in 0 until 4) {
                    val cell = row * 4 + column
                    val isWall = cell in walls
                    Box(
                        Modifier.weight(1f).fillMaxSize()
                            .background(if (isWall) colors.borderStrong else world.copy(alpha = 0.3f), Radius.tile)
                            .border(1.dp, if (isWall) colors.borderStrong else world.copy(alpha = 0.55f), Radius.tile),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (cell == 15) {
                            BasicText(
                                text = "2",
                                style = TextStyle(fontSize = with(density) { (side.toPx() * 0.9f / 4f * 0.5f).toSp() }, fontWeight = FontWeight.Bold, color = colors.correctFill),
                            )
                        }
                        if (cell in lamps) {
                            Box(
                                Modifier.size(side * 0.9f / 4f * 0.5f)
                                    .drawBehind { drawCircle(world.copy(alpha = 0.35f), radius = this.size.minDimension * 0.9f) }
                                    .background(world, CircleShape),
                            )
                        }
                    }
                }
            }
        }
    }
}
