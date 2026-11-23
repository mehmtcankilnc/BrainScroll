package com.mehmtcan.brainscroll.ui.mirror

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_mirror_board
import brainscroll.shared.generated.resources.a11y_mirror_cell
import brainscroll.shared.generated.resources.a11y_mirror_empty
import brainscroll.shared.generated.resources.a11y_mirror_fill
import brainscroll.shared.generated.resources.a11y_mirror_given
import brainscroll.shared.generated.resources.a11y_mirror_painted
import brainscroll.shared.generated.resources.a11y_mirror_restart
import brainscroll.shared.generated.resources.mirror_hint
import brainscroll.shared.generated.resources.mirror_painted
import brainscroll.shared.generated.resources.mirror_result
import brainscroll.shared.generated.resources.mirror_won
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.mirror.MirrorAxis
import com.mehmtcan.brainscroll.game.mirror.MirrorResult
import com.mehmtcan.brainscroll.game.mirror.MirrorSnapshot
import com.mehmtcan.brainscroll.game.mirror.MirrorStatus
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
private val BottomAreaHeight = 152.dp
private val CellGap = 5.dp
private val MaxCell = 56.dp

/** Whether a cell is painted (1) or empty (0), for tests. */
internal val MirrorPaintedKey = SemanticsPropertyKey<Int>("MirrorPainted")
private var SemanticsPropertyReceiver.mirrorPainted by MirrorPaintedKey

/** True for a cell of the ready half (it cannot be tapped), for tests. */
internal val MirrorGivenKey = SemanticsPropertyKey<Boolean>("MirrorGiven")
private var SemanticsPropertyReceiver.mirrorGiven by MirrorGivenKey

/** The side of the board, for tests. */
internal val MirrorSizeKey = SemanticsPropertyKey<Int>("MirrorSize")
private var SemanticsPropertyReceiver.mirrorSize by MirrorSizeKey

/**
 * One full-screen page of the feed: how many cells are painted, the board with its axis (tap a cell of the empty half to paint it, again
 * to empty it), and the restart button. The result takes the place of the hint and the button when both sides are the same. The ready
 * half is a little lighter than the half to paint, so the two can be told apart without the colour of the game.
 */
@Composable
fun MirrorPage(
    page: MirrorSnapshot,
    onTap: (Int) -> MirrorResult,
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
    val won = state.status == MirrorStatus.Won

    // The result and the buzz wait a moment so the last painted cell can be seen.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(text = stringResource(Res.string.mirror_painted, state.painted), textColor = if (won) colors.correctFill else colors.textSecondary)
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cellSize: Dp = minOf(MaxCell, (maxWidth - CellGap * (n - 1)) / n, (maxHeight - CellGap * (n - 1)) / n)
            val description = stringResource(Res.string.a11y_mirror_board, n, n, state.painted)
            Box(
                modifier = Modifier.testTag("mirrorBoard").semantics {
                    contentDescription = description
                    mirrorSize = n
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                    for (row in 0 until n) {
                        Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                            for (column in 0 until n) {
                                val cell = row * n + column
                                GuideMark(isGuided(cell) && !won, Radius.tile, evidence = isGuideEvidence(cell) && !won) {
                                    MirrorCell(
                                        row = row + 1,
                                        column = column + 1,
                                        painted = state.marks[cell],
                                        given = puzzle.isGiven(cell),
                                        playing = !won,
                                        size = cellSize,
                                        onClick = {
                                            when (onTap(cell)) {
                                                MirrorResult.Painted -> haptics.perform(HapticKind.Medium)
                                                MirrorResult.Erased -> haptics.perform(HapticKind.Light)
                                                MirrorResult.Solved, MirrorResult.Ignored -> Unit
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                AxisLine(puzzle.axis, n, cellSize)
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                MirrorResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.mirror_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_mirror_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.painted > 0,
                        tag = "mirrorRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** The line the board is mirrored on, or the point it is turned about, drawn over the cells without catching touches (the colour of the game, as in the how to card). */
@Composable
private fun AxisLine(axis: MirrorAxis, n: Int, cellSize: Dp) {
    val color = GameKind.Mirror.world
    val total = cellSize * n + CellGap * (n - 1)
    Canvas(Modifier.size(total).testTag("mirrorAxis")) {
        val cell = cellSize.toPx()
        val gap = CellGap.toPx()
        val side = size.width
        val width = 3.dp.toPx()
        val half = (cell + gap) * (n / 2) - gap / 2f
        when (axis) {
            MirrorAxis.Vertical -> drawLine(color, Offset(half, 0f), Offset(half, side), width, StrokeCap.Round)
            MirrorAxis.Horizontal -> drawLine(color, Offset(0f, half), Offset(side, half), width, StrokeCap.Round)
            MirrorAxis.Diagonal -> drawLine(color, Offset(0f, 0f), Offset(side, side), width, StrokeCap.Round)
            MirrorAxis.Point -> {
                // The middle of the board: a small ring, because the board is turned half a way round about it.
                val c = Offset(side / 2f, side / 2f)
                drawCircle(color, radius = 9.dp.toPx(), center = c, style = Stroke(width))
                drawCircle(color, radius = 2.5.dp.toPx(), center = c)
            }
        }
    }
}

/**
 * One cell. A cell of the ready half has a lighter tile and cannot be tapped; a cell of the half to paint has the plain tile. A painted
 * cell has a square of the colour of the game inside, so painted and empty differ by shape and not only by colour.
 */
@Composable
private fun MirrorCell(
    row: Int,
    column: Int,
    painted: Boolean,
    given: Boolean,
    playing: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.Mirror.world
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.tile
    val tile = if (given) colors.bgRaised else colors.bgSurface
    val markText = stringResource(if (painted) Res.string.a11y_mirror_painted else Res.string.a11y_mirror_empty)
    val halfText = stringResource(if (given) Res.string.a11y_mirror_given else Res.string.a11y_mirror_fill)
    val description = stringResource(Res.string.a11y_mirror_cell, row, column)
    Box(
        modifier = Modifier
            .size(size)
            .testTag("mirrorCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = "$markText, $halfText"
                mirrorPainted = if (painted) 1 else 0
                mirrorGiven = given
                role = Role.Button
            }
            .background(tile, shape)
            .border((1.5f + extraBorder).dp, colors.borderStrong.copy(alpha = if (given) 0.35f else 0.6f), shape)
            .then(
                if (playing && !given) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (painted) {
            Canvas(Modifier.size(size)) {
                val inset = this.size.minDimension * 0.14f
                drawRoundRect(world, Offset(inset, inset), Size(this.size.width - 2 * inset, this.size.height - 2 * inset), CornerRadius(this.size.minDimension * 0.14f))
            }
        }
    }
}

@Composable
private fun MirrorResultCard(page: MirrorSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("mirrorResultCard").resultSurface(colors.bgSurface, GameKind.Mirror.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.mirror_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.mirror_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.Mirror.world),
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

/** A small picture for the thumbnails: a 4 by 4 board with a vertical axis, the left half painted and its mirror image painted on the right. */
@Composable
internal fun MirrorThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.Mirror.world
    val pattern = listOf(
        true, false, true, false,
        false, true, true, false,
        true, true, false, false,
        false, false, true, false,
    )
    Box(Modifier.size(side * 0.9f)) {
        Column(Modifier.size(side * 0.9f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (row in 0 until 4) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (column in 0 until 4) {
                        val left = column < 2
                        val painted = if (left) pattern[row * 4 + column] else pattern[row * 4 + (3 - column)]
                        Box(
                            Modifier.weight(1f).fillMaxSize()
                                .background(if (left) colors.bgRaised else colors.bgSurface, Radius.tile)
                                .border(1.dp, colors.borderStrong.copy(alpha = if (left) 0.35f else 0.6f), Radius.tile),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (painted) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val inset = size.minDimension * 0.14f
                                    drawRoundRect(world, Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset), CornerRadius(size.minDimension * 0.14f))
                                }
                            }
                        }
                    }
                }
            }
        }
        Canvas(Modifier.size(side * 0.9f)) {
            drawLine(world, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), 2.5.dp.toPx(), StrokeCap.Round)
        }
    }
}
