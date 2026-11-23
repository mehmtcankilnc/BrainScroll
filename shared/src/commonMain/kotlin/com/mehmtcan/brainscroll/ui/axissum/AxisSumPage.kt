package com.mehmtcan.brainscroll.ui.axissum

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_axissum_cell
import brainscroll.shared.generated.resources.a11y_axissum_cell_erased
import brainscroll.shared.generated.resources.a11y_axissum_column_target
import brainscroll.shared.generated.resources.a11y_axissum_count
import brainscroll.shared.generated.resources.a11y_axissum_restart
import brainscroll.shared.generated.resources.a11y_axissum_row_target
import brainscroll.shared.generated.resources.axissum_erased
import brainscroll.shared.generated.resources.axissum_hint
import brainscroll.shared.generated.resources.axissum_result
import brainscroll.shared.generated.resources.axissum_taps
import brainscroll.shared.generated.resources.axissum_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.axissum.AxisSumResult
import com.mehmtcan.brainscroll.game.axissum.AxisSumSnapshot
import com.mehmtcan.brainscroll.game.axissum.AxisSumStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
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
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import brainscroll.shared.generated.resources.result_stat_time
import brainscroll.shared.generated.resources.result_stat_taps
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 6.dp
private val MaxCell = 64.dp

/** How long the board waits for the numbers to take their winning color before the result shows. */
private const val WIN_MILLIS = 320L

/** For tests: the number of a cell (negative when it is erased). */
internal val AxisSumCellValue = SemanticsPropertyKey<Int>("AxisSumCellValue")
private var SemanticsPropertyReceiver.cellValue by AxisSumCellValue

/**
 * One full-screen page of the feed: how many numbers are erased and how many taps were made, the board with the sums of the
 * rows on the left and of the columns on top, and the restart button or the result. A tap on a number erases it (or brings it
 * back); the board is won when every row and column makes its sum.
 */
@Composable
fun AxisSumPage(
    page: AxisSumSnapshot,
    onTap: (Int) -> AxisSumResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val won = state.status == AxisSumStatus.Won

    // The numbers turn green, then the buzz and the result card come.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, WIN_MILLIS)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.axissum_erased, state.erasedCount, state.eraseGoal),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_axissum_count, state.erasedCount, state.eraseGoal),
            )
            Chip(text = stringResource(Res.string.axissum_taps, state.taps), textColor = colors.textSecondary)
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val n = state.size
            // The board has one more row and column than the numbers: the sums of the columns on top, of the rows at the left.
            val cell: Dp = minOf(MaxCell, (maxWidth - CellGap * n) / (n + 1), (maxHeight - CellGap * n) / (n + 1))
            val density = LocalDensity.current
            val step = with(density) { (cell + CellGap).roundToPx() }
            val side = cell * (n + 1) + CellGap * n
            Box(Modifier.size(side).testTag("axisSumBoard")) {
                for (c in 0 until n) {
                    Box(Modifier.offset { IntOffset((c + 1) * step, 0) }) {
                        Target(
                            target = state.columnTargets[c],
                            sum = state.columnSums[c],
                            size = cell,
                            tag = "axisColumnTarget",
                            description = stringResource(Res.string.a11y_axissum_column_target, c + 1, state.columnTargets[c], state.columnSums[c]),
                        )
                    }
                }
                for (r in 0 until n) {
                    Box(Modifier.offset { IntOffset(0, (r + 1) * step) }) {
                        Target(
                            target = state.rowTargets[r],
                            sum = state.rowSums[r],
                            size = cell,
                            tag = "axisRowTarget",
                            description = stringResource(Res.string.a11y_axissum_row_target, r + 1, state.rowTargets[r], state.rowSums[r]),
                        )
                    }
                    for (c in 0 until n) {
                        val index = r * n + c
                        Box(Modifier.offset { IntOffset((c + 1) * step, (r + 1) * step) }) {
                            GuideMark(isGuided(index), Radius.card, evidence = isGuideEvidence(index)) {
                                NumberCell(
                                    row = r + 1,
                                    column = c + 1,
                                    digit = state.digits[index],
                                    erased = state.erased[index],
                                    solved = status == WordleStatus.Won,
                                    playing = !won,
                                    size = cell,
                                    onClick = {
                                        when (onTap(index)) {
                                            AxisSumResult.Erased, AxisSumResult.Restored -> haptics.perform(HapticKind.Light)
                                            else -> Unit
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
                AxisSumResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.axissum_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_axissum_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "axisSumRestart",
                    ) { drawRestart(iconColor) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/**
 * The sum a row or column must make, in a raised tile: green when the numbers that are left make it, red when too much was
 * erased (the sum is below it and can only be brought up by bringing a number back), plain while there is still more than it. The size follows the cell, not the text size setting: the number has to fit.
 */
@Composable
private fun Target(target: Int, sum: Int, size: Dp, tag: String, description: String) {
    val colors = BrainScrollTheme.colors
    val density = LocalDensity.current
    val color by animateColorAsState(
        when {
            sum < target -> colors.error
            sum == target -> colors.correctFill
            else -> colors.textPrimary
        },
        tween(if (LocalReduceMotion.current) 0 else Motion.BASE, easing = Motion.Standard),
    )
    Box(
        modifier = Modifier
            .size(size)
            .testTag(tag)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .background(colors.bgRaised, Radius.card),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = target.toString(),
            style = BrainScrollTheme.typography.heading.copy(color = color, fontSize = with(density) { (size * 0.42f).toSp() }, fontWeight = FontWeight.Bold),
        )
    }
}

/**
 * One number. Erased, its tile turns into an outline and the number fades and shrinks; brought back, it grows again. When the
 * board is solved the numbers that are left turn green.
 */
@Composable
private fun NumberCell(row: Int, column: Int, digit: Int, erased: Boolean, solved: Boolean, playing: Boolean, size: Dp, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val density = LocalDensity.current
    val reduce = LocalReduceMotion.current
    val present by animateFloatAsState(if (erased) 0f else 1f, tween(if (reduce) 0 else Motion.BASE, easing = Motion.Standard))
    val fill by animateColorAsState(
        if (solved && !erased) colors.correctFill else colors.bgSurface,
        tween(if (reduce) 0 else Motion.BASE, easing = Motion.Standard),
    )
    val description = stringResource(if (erased) Res.string.a11y_axissum_cell_erased else Res.string.a11y_axissum_cell, row, column, digit)

    Box(
        modifier = Modifier
            .size(size)
            .testTag("axisSumCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                cellValue = if (erased) -digit else digit
                role = Role.Button
            }
            .graphicsLayer {
                val s = 0.9f + 0.1f * present
                scaleX = s
                scaleY = s
            }
            .background(fill.copy(alpha = 0.15f + 0.85f * present), Radius.card)
            .border((1.5f + extraBorder).dp, colors.borderStrong.copy(alpha = 0.35f), Radius.card)
            .then(
                if (playing) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = digit.toString(),
            style = BrainScrollTheme.typography.display.copy(
                color = (if (solved && !erased) colors.correctOn else colors.textPrimary).copy(alpha = 0.18f + 0.82f * present),
                fontSize = with(density) { (size * 0.46f).toSp() },
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun AxisSumResultCard(page: AxisSumSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().resultSurface(colors.bgSurface, GameKind.AxisSum.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.axissum_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
    summary = stringResource(Res.string.axissum_result, page.state.taps, formatDuration(page.durationMs)),
    stats = listOf(
        ResultStatData(page.state.taps.toString(), stringResource(Res.string.result_stat_taps), GameKind.AxisSum.world),
        ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), colors.streakAnswer),
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
