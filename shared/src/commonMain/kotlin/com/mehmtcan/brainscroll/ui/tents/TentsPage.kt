package com.mehmtcan.brainscroll.ui.tents

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_tents_column_target
import brainscroll.shared.generated.resources.a11y_tents_conflict
import brainscroll.shared.generated.resources.a11y_tents_count
import brainscroll.shared.generated.resources.a11y_tents_empty
import brainscroll.shared.generated.resources.a11y_tents_grass
import brainscroll.shared.generated.resources.a11y_tents_restart
import brainscroll.shared.generated.resources.a11y_tents_row_target
import brainscroll.shared.generated.resources.a11y_tents_tent
import brainscroll.shared.generated.resources.a11y_tents_tree
import brainscroll.shared.generated.resources.tents_count
import brainscroll.shared.generated.resources.tents_hint
import brainscroll.shared.generated.resources.tents_result
import brainscroll.shared.generated.resources.tents_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.tents.TentsMark
import com.mehmtcan.brainscroll.game.tents.TentsResult
import com.mehmtcan.brainscroll.game.tents.TentsSnapshot
import com.mehmtcan.brainscroll.game.tents.TentsStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.popOnAppear
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 4.dp
private val MaxCell = 56.dp

/** What a cell holds, for tests: 0 empty, 1 tent, 2 grass, 3 tree, 4 tent that breaks a rule. */
internal val TentsCellValue = SemanticsPropertyKey<Int>("TentsCellValue")
private var SemanticsPropertyReceiver.cellValue by TentsCellValue

/** The number a row or column clue shows, for tests. */
internal val TentsClueValue = SemanticsPropertyKey<Int>("TentsClueValue")
private var SemanticsPropertyReceiver.clueValue by TentsClueValue

/**
 * One full-screen page of the feed: how many tents are placed, the board with the numbers of its rows and columns, and the
 * restart button or the result. A tap cycles a cell: tent, grass, empty.
 */
@Composable
fun TentsPage(
    page: TentsSnapshot,
    onTap: (Int) -> TentsResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val won = state.status == TentsStatus.Won

    // The result, the buzz and the card wait until the last tent has settled into its cell.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, Motion.POP.toLong() + 50)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Chip(
            text = stringResource(Res.string.tents_count, state.tentCount, state.treeCount),
            textColor = colors.textSecondary,
            description = stringResource(Res.string.a11y_tents_count, state.tentCount, state.treeCount),
        )
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().testTag("tentsBoard"), contentAlignment = Alignment.Center) {
            // One more column and one more row than the board, for the numbers.
            val lines = state.size + 1
            val cellSize: Dp = minOf(MaxCell, (maxWidth - CellGap * (lines - 1)) / lines, (maxHeight - CellGap * (lines - 1)) / lines)
            Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    for (column in 0 until state.size) {
                        Clue(
                            target = state.columnTargets[column],
                            count = state.columnCounts[column],
                            size = cellSize,
                            tag = "tentsColumnClue",
                            description = stringResource(Res.string.a11y_tents_column_target, column + 1, state.columnTargets[column], state.columnCounts[column]),
                        )
                    }
                    Spacer(Modifier.size(cellSize))
                }
                for (row in 0 until state.size) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                        for (column in 0 until state.size) {
                            val index = row * state.size + column
                            GuideMark(isGuided(index), Radius.card) {
                                Cell(
                                    row = row + 1,
                                    column = column + 1,
                                    tree = state.trees[index],
                                    mark = state.marks[index],
                                    conflict = state.conflicts[index],
                                    playing = !won,
                                    size = cellSize,
                                    onClick = {
                                        when (onTap(index)) {
                                            TentsResult.Ignored -> if (state.trees[index]) haptics.perform(HapticKind.Warning)
                                            else -> haptics.perform(HapticKind.Light)
                                        }
                                    },
                                )
                            }
                        }
                        Clue(
                            target = state.rowTargets[row],
                            count = state.rowCounts[row],
                            size = cellSize,
                            tag = "tentsRowClue",
                            description = stringResource(Res.string.a11y_tents_row_target, row + 1, state.rowTargets[row], state.rowCounts[row]),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                TentsResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.tents_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_tents_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "tentsRestart",
                    ) { drawRestart(iconColor) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/**
 * The number beside a row or above a column: how many tents it must hold. It turns red when the line holds too many, and
 * green when it holds exactly as many, so the player sees at a glance which lines are done.
 */
@Composable
private fun Clue(target: Int, count: Int, size: Dp, tag: String, description: String) {
    val colors = BrainScrollTheme.colors
    val density = LocalDensity.current
    val color = when {
        count > target -> colors.error
        count == target -> colors.correctFill
        else -> colors.textPrimary
    }
    Box(
        modifier = Modifier
            .size(size)
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                clueValue = target
            },
        contentAlignment = Alignment.Center,
    ) {
        // The size follows the cell, not the text size setting: the number has to fit.
        BasicText(
            text = target.toString(),
            style = BrainScrollTheme.typography.heading.copy(color = color, fontSize = with(density) { (size * 0.5f).toSp() }, fontWeight = FontWeight.Bold),
        )
    }
}

/**
 * One cell of the board. A tree is drawn on it, a tent is a little tent (red with a cross when it breaks a rule, so it is told
 * apart by more than its color), grass is three blades. A tree cannot be tapped.
 */
@Composable
private fun Cell(row: Int, column: Int, tree: Boolean, mark: Int, conflict: Boolean, playing: Boolean, size: Dp, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.card
    val duration = if (LocalReduceMotion.current) 0 else Motion.BASE
    val tent = mark == TentsMark.TENT
    val fill by animateColorAsState(if (conflict) colors.error.copy(alpha = 0.22f) else colors.bgSurface, tween(duration, easing = Motion.Standard))
    val description = stringResource(
        when {
            tree -> Res.string.a11y_tents_tree
            conflict -> Res.string.a11y_tents_conflict
            tent -> Res.string.a11y_tents_tent
            mark == TentsMark.GRASS -> Res.string.a11y_tents_grass
            else -> Res.string.a11y_tents_empty
        },
        row, column,
    )

    Box(
        modifier = Modifier
            .size(size)
            .testTag("tentsCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                cellValue = when {
                    tree -> 3
                    conflict -> 4
                    else -> mark
                }
                role = Role.Button
            }
            .background(fill, shape)
            .border((1.5f + extraBorder).dp, if (conflict) colors.error else colors.borderStrong.copy(alpha = 0.35f), shape)
            .then(
                if (playing) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            tree -> Canvas(Modifier.size(size * 0.72f)) { drawTree(colors.correctFill, colors.borderStrong) }
            // Always there for the empty and the tent cells, so a tent that appears can pop into its cell.
            else -> Box(Modifier.size(size * 0.72f).popOnAppear(tent)) {
                if (tent) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawTent(if (conflict) colors.error else colors.pendingFill, if (conflict) colors.bgPage else colors.bgSurface)
                        if (conflict) drawCross(colors.bgPage)
                    }
                } else if (mark == TentsMark.GRASS) {
                    Canvas(Modifier.fillMaxSize()) { drawGrass(colors.correctFill.copy(alpha = 0.55f)) }
                }
            }
        }
    }
}

/** A tree: a round crown on a short trunk. */
internal fun DrawScope.drawTree(crown: Color, trunk: Color) {
    val w = size.width
    val h = size.height
    drawRoundRect(trunk, Offset(w * 0.43f, h * 0.55f), Size(w * 0.14f, h * 0.4f), androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
    drawCircle(crown, w * 0.3f, Offset(w * 0.5f, h * 0.34f))
    drawCircle(crown, w * 0.2f, Offset(w * 0.3f, h * 0.52f))
    drawCircle(crown, w * 0.2f, Offset(w * 0.7f, h * 0.52f))
}

/** A tent: a triangle with a dark doorway. */
internal fun DrawScope.drawTent(body: Color, door: Color) {
    val w = size.width
    val h = size.height
    val tent = Path().apply {
        moveTo(w * 0.5f, h * 0.1f)
        lineTo(w * 0.98f, h * 0.88f)
        lineTo(w * 0.02f, h * 0.88f)
        close()
    }
    drawPath(tent, body)
    val doorway = Path().apply {
        moveTo(w * 0.5f, h * 0.42f)
        lineTo(w * 0.68f, h * 0.88f)
        lineTo(w * 0.32f, h * 0.88f)
        close()
    }
    drawPath(doorway, door)
}

/** A cross over a tent, in the color of the cell behind it. */
private fun DrawScope.drawCross(color: Color) {
    val w = size.width
    val h = size.height
    drawLine(color, Offset(w * 0.3f, h * 0.5f), Offset(w * 0.7f, h * 0.9f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.7f, h * 0.5f), Offset(w * 0.3f, h * 0.9f), strokeWidth = w * 0.1f, cap = StrokeCap.Round)
}

/** Three blades of grass: the player's note that there is no tent here. */
private fun DrawScope.drawGrass(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.09f
    drawLine(color, Offset(w * 0.5f, h * 0.8f), Offset(w * 0.5f, h * 0.35f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.38f, h * 0.8f), Offset(w * 0.24f, h * 0.45f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(w * 0.62f, h * 0.8f), Offset(w * 0.76f, h * 0.45f), strokeWidth = stroke, cap = StrokeCap.Round)
}

@Composable
private fun TentsResultCard(page: TentsSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.tents_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.tents_result, formatDuration(page.durationMs)),
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
