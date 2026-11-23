package com.mehmtcan.brainscroll.ui.zerosones

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import brainscroll.shared.generated.resources.a11y_zerosones_board
import brainscroll.shared.generated.resources.a11y_zerosones_broken
import brainscroll.shared.generated.resources.a11y_zerosones_cell
import brainscroll.shared.generated.resources.a11y_zerosones_empty
import brainscroll.shared.generated.resources.a11y_zerosones_given
import brainscroll.shared.generated.resources.a11y_zerosones_restart
import brainscroll.shared.generated.resources.result_stat_time
import brainscroll.shared.generated.resources.zerosones_filled
import brainscroll.shared.generated.resources.zerosones_hint
import brainscroll.shared.generated.resources.zerosones_result
import brainscroll.shared.generated.resources.zerosones_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.game.zerosones.BinResult
import com.mehmtcan.brainscroll.game.zerosones.BinSnapshot
import com.mehmtcan.brainscroll.game.zerosones.BinStatus
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

/** What a cell holds, for tests: -1 empty, 0 or 1. */
internal val BinValueKey = SemanticsPropertyKey<Int>("BinValue")
private var SemanticsPropertyReceiver.binValue by BinValueKey

/** Whether a cell is one of the given ones, for tests. */
internal val BinGivenKey = SemanticsPropertyKey<Boolean>("BinGiven")
private var SemanticsPropertyReceiver.binGiven by BinGivenKey

/** Whether a cell breaks a rule (it is red), for tests. */
internal val BinBrokenKey = SemanticsPropertyKey<Boolean>("BinBroken")
private var SemanticsPropertyReceiver.binBroken by BinBrokenKey

/** The side of the board, for tests. */
internal val BinSizeKey = SemanticsPropertyKey<Int>("BinSize")
private var SemanticsPropertyReceiver.binSize by BinSizeKey

/**
 * One full-screen page of the feed: how many cells are filled, the board (tap a cell to put 0, again to put 1, again to empty it),
 * and the restart button. The result takes the place of the hint and the button when the board is solved. A cell that breaks a rule
 * (three of a digit side by side, too many of a digit in its line, two lines that are the same) is red, so the rules can be seen at work.
 */
@Composable
fun ZerosOnesPage(
    page: BinSnapshot,
    onTap: (Int) -> BinResult,
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
    val won = state.status == BinStatus.Won

    // The result and the buzz wait a moment so the last digit can be seen.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    val broken = state.conflicts

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(text = stringResource(Res.string.zerosones_filled, state.filled, n * n), textColor = if (won) colors.correctFill else colors.textSecondary)
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cellSize: Dp = minOf(MaxCell, (maxWidth - CellGap * (n - 1)) / n, (maxHeight - CellGap * (n - 1)) / n)
            val description = stringResource(Res.string.a11y_zerosones_board, n, n, state.filled, n * n)
            Column(
                modifier = Modifier.testTag("binBoard").semantics {
                    contentDescription = description
                    binSize = n
                },
                verticalArrangement = Arrangement.spacedBy(CellGap),
            ) {
                for (row in 0 until n) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                        for (column in 0 until n) {
                            val cell = row * n + column
                            GuideMark(isGuided(cell) && !won, Radius.tile, evidence = isGuideEvidence(cell) && !won) {
                                BinCell(
                                    row = row + 1,
                                    column = column + 1,
                                    value = state.cells[cell],
                                    given = puzzle.isGiven(cell),
                                    broken = cell in broken,
                                    playing = !won,
                                    size = cellSize,
                                    onClick = {
                                        when (onTap(cell)) {
                                            BinResult.Zero, BinResult.One -> haptics.perform(HapticKind.Medium)
                                            BinResult.Cleared -> haptics.perform(HapticKind.Light)
                                            BinResult.Solved, BinResult.Ignored -> Unit
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
                ZerosOnesResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.zerosones_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_zerosones_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = (0 until n * n).any { !puzzle.isGiven(it) && state.cells[it] >= 0 },
                        tag = "binRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/**
 * One cell. A given cell is a raised tile with a solid border; a digit the player put (0 or 1) is a tile filled in the colour of the
 * game, so the two digits are told apart by the digit itself and not by the colour. A cell that breaks a rule is red.
 */
@Composable
private fun BinCell(
    row: Int,
    column: Int,
    value: Int,
    given: Boolean,
    broken: Boolean,
    playing: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.ZerosOnes.world
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.tile
    val duration = if (LocalReduceMotion.current) 0 else Motion.BASE
    // Both digits the player put are filled tiles; the digit itself tells them apart.
    val ones = value >= 0 && !given
    val fill by animateColorAsState(
        when {
            ones -> world
            given -> colors.bgRaised
            else -> colors.bgSurface
        },
        tween(duration, easing = Motion.Standard),
    )
    val digitColor = when {
        broken -> colors.error
        ones -> colors.bgPage
        else -> colors.textPrimary
    }
    val border = when {
        broken -> colors.error
        value == 0 && !given -> world
        given -> colors.borderStrong
        ones -> world
        else -> colors.borderStrong.copy(alpha = 0.5f)
    }
    val stateText = if (value < 0) stringResource(Res.string.a11y_zerosones_empty) else stringResource(Res.string.a11y_zerosones_cell, value)
    val givenText = stringResource(Res.string.a11y_zerosones_given)
    val brokenText = stringResource(Res.string.a11y_zerosones_broken)
    val description = "$row, $column"
    val density = LocalDensity.current
    val digitSize = with(density) { (size.toPx() * 0.5f).toSp() }

    Box(
        modifier = Modifier
            .size(size)
            .testTag("binCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = listOfNotNull(stateText, givenText.takeIf { given }, brokenText.takeIf { broken }).joinToString(", ")
                binValue = value
                binGiven = given
                binBroken = broken
                role = Role.Button
            }
            .background(fill, shape)
            .border((1.5f + extraBorder).dp, border, shape)
            .then(
                if (playing && !given) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (value >= 0) {
            BasicText(
                text = value.toString(),
                style = TextStyle(fontSize = digitSize, fontWeight = FontWeight.Bold, color = digitColor, textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
private fun ZerosOnesResultCard(page: BinSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("binResultCard").resultSurface(colors.bgSurface, GameKind.ZerosOnes.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.zerosones_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.zerosones_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.ZerosOnes.world),
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

/** A small picture for the thumbnails: a solved 4 by 4 board of 0 and 1, the given cells raised and the others filled in. */
@Composable
internal fun ZerosOnesThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.ZerosOnes.world
    val density = LocalDensity.current
    val digits = listOf(
        0, 1, 1, 0,
        1, 1, 0, 0,
        0, 0, 1, 1,
        1, 0, 0, 1,
    )
    val given = setOf(0, 1, 4, 5, 8, 9, 15)
    Column(Modifier.size(side * 0.9f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until 4) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (column in 0 until 4) {
                    val cell = row * 4 + column
                    val isGiven = cell in given
                    val digit = digits[cell]
                    val ones = !isGiven
                    Box(
                        Modifier.weight(1f).fillMaxSize()
                            .background(if (ones) world else if (isGiven) colors.bgRaised else colors.bgSurface, Radius.tile)
                            .border(1.dp, if (isGiven) colors.borderStrong else world, Radius.tile),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = digit.toString(),
                            style = TextStyle(
                                fontSize = with(density) { (side.toPx() * 0.9f / 4f * 0.5f).toSp() },
                                fontWeight = FontWeight.Bold,
                                color = if (ones) colors.bgPage else colors.textPrimary,
                            ),
                        )
                    }
                }
            }
        }
    }
}
