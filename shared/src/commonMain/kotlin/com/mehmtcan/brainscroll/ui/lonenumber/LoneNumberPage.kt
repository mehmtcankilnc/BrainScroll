package com.mehmtcan.brainscroll.ui.lonenumber

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import brainscroll.shared.generated.resources.a11y_lonenumber_board
import brainscroll.shared.generated.resources.a11y_lonenumber_cell
import brainscroll.shared.generated.resources.a11y_lonenumber_circled
import brainscroll.shared.generated.resources.a11y_lonenumber_empty
import brainscroll.shared.generated.resources.a11y_lonenumber_repeated
import brainscroll.shared.generated.resources.a11y_lonenumber_restart
import brainscroll.shared.generated.resources.a11y_lonenumber_shaded
import brainscroll.shared.generated.resources.lonenumber_hint
import brainscroll.shared.generated.resources.lonenumber_result
import brainscroll.shared.generated.resources.lonenumber_shaded
import brainscroll.shared.generated.resources.lonenumber_won
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.lonenumber.LoneMark
import com.mehmtcan.brainscroll.game.lonenumber.LoneResult
import com.mehmtcan.brainscroll.game.lonenumber.LoneSnapshot
import com.mehmtcan.brainscroll.game.lonenumber.LoneStatus
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
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** At least this high, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 6.dp
private val MaxCell = 60.dp

/** What a cell shows, for tests: its mark (0 empty, 1 shaded, 2 circled). */
internal val LoneMarkKey = SemanticsPropertyKey<Int>("LoneMark")
private var SemanticsPropertyReceiver.loneMark by LoneMarkKey

/** The side of the board, for tests. */
internal val LoneSizeKey = SemanticsPropertyKey<Int>("LoneSize")
private var SemanticsPropertyReceiver.loneSize by LoneSizeKey

/** The number a cell holds, for tests. */
internal val LoneNumberKey = SemanticsPropertyKey<Int>("LoneNumber")
private var SemanticsPropertyReceiver.loneNumber by LoneNumberKey

/** Whether a cell shows its number as repeated (the red one), for tests. */
internal val LoneRepeatedKey = SemanticsPropertyKey<Boolean>("LoneRepeated")
private var SemanticsPropertyReceiver.loneRepeated by LoneRepeatedKey

/**
 * One full-screen page of the feed: how many cells are shaded, the board of numbers (tap a cell to shade it, again to circle it, again
 * to empty it), and the restart button. The result takes the place of the hint and the button when the board is solved. A number that
 * still stands twice among the cells left is red, so the rule can be seen at work.
 */
@Composable
fun LoneNumberPage(
    page: LoneSnapshot,
    onTap: (Int) -> LoneResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val n = state.puzzle.size
    val won = state.status == LoneStatus.Won

    // The result and the buzz wait a moment so the last shaded cell can be seen.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    val shaded = state.shaded
    val repeated = state.repeated

    // The cell that was refused (it shows red a moment).
    var refused by remember(page.id) { mutableIntStateOf(-1) }
    LaunchedEffect(refused) {
        if (refused >= 0) {
            delay(450)
            refused = -1
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(text = stringResource(Res.string.lonenumber_shaded, shaded.size), textColor = if (won) colors.correctFill else colors.textSecondary)
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cellSize: Dp = minOf(MaxCell, (maxWidth - CellGap * (n - 1)) / n, (maxHeight - CellGap * (n - 1)) / n)
            val description = stringResource(Res.string.a11y_lonenumber_board, n, n, shaded.size)
            Column(
                modifier = Modifier.testTag("loneBoard").semantics {
                    contentDescription = description
                    loneSize = n
                },
                verticalArrangement = Arrangement.spacedBy(CellGap),
            ) {
                for (row in 0 until n) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                        for (column in 0 until n) {
                            val cell = row * n + column
                            GuideMark(isGuided(cell) && !won, Radius.tile, evidence = isGuideEvidence(cell) && !won) {
                                LoneCell(
                                    row = row + 1,
                                    column = column + 1,
                                    number = state.puzzle.numbers[cell],
                                    mark = state.marks[cell],
                                    repeated = cell in repeated,
                                    refused = refused == cell,
                                    playing = !won,
                                    size = cellSize,
                                    onClick = {
                                        when (onTap(cell)) {
                                            LoneResult.Shaded -> haptics.perform(HapticKind.Medium)
                                            LoneResult.Circled, LoneResult.Cleared -> haptics.perform(HapticKind.Light)
                                            LoneResult.Rejected -> {
                                                refused = cell
                                                haptics.perform(HapticKind.Warning)
                                            }
                                            LoneResult.Solved, LoneResult.Ignored -> Unit
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
                LoneNumberResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.lonenumber_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_lonenumber_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.marks.any { it != LoneMark.Empty },
                        tag = "loneRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/**
 * One cell. Empty it is a plain tile with its number; shaded it is filled with the colour of the game and its number is dimmed;
 * circled it has a ring round its number. So the three are told apart by their shape and not only by their colour. A number that stands
 * twice among the cells left is red.
 */
@Composable
private fun LoneCell(
    row: Int,
    column: Int,
    number: Int,
    mark: LoneMark,
    repeated: Boolean,
    refused: Boolean,
    playing: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.LoneNumber.world
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.tile
    val duration = if (LocalReduceMotion.current) 0 else Motion.BASE
    val shadedNow = mark == LoneMark.Shaded
    val fill by animateColorAsState(if (shadedNow) world else colors.bgSurface, tween(duration, easing = Motion.Standard))
    val numberColor = when {
        shadedNow -> colors.bgPage.copy(alpha = 0.7f)
        repeated -> colors.error
        else -> colors.textPrimary
    }
    val border = when {
        refused -> colors.error
        shadedNow -> world
        else -> colors.borderStrong.copy(alpha = 0.5f)
    }
    val markText = stringResource(
        when (mark) {
            LoneMark.Empty -> Res.string.a11y_lonenumber_empty
            LoneMark.Shaded -> Res.string.a11y_lonenumber_shaded
            LoneMark.Circled -> Res.string.a11y_lonenumber_circled
        },
    )
    val repeatedText = stringResource(Res.string.a11y_lonenumber_repeated)
    val description = stringResource(Res.string.a11y_lonenumber_cell, row, column, number)
    val density = LocalDensity.current
    val numberSize = with(density) { (size.toPx() * 0.46f).toSp() }

    Box(
        modifier = Modifier
            .size(size)
            .testTag("loneCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = if (repeated) "$markText, $repeatedText" else markText
                loneMark = mark.ordinal
                loneNumber = number
                loneRepeated = repeated
                role = Role.Button
            }
            .background(fill, shape)
            .border((1.5f + extraBorder).dp, border, shape)
            .then(
                if (playing) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = number.toString(),
            style = TextStyle(fontSize = numberSize, fontWeight = FontWeight.Bold, color = numberColor, textAlign = TextAlign.Center),
        )
        if (mark == LoneMark.Circled) {
            val ring = world
            Canvas(Modifier.size(size)) {
                drawCircle(ring, radius = this.size.minDimension * 0.38f, center = Offset(this.size.width / 2f, this.size.height / 2f), style = Stroke(2.5.dp.toPx()))
            }
        }
    }
}

@Composable
private fun LoneNumberResultCard(page: LoneSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("loneResultCard").resultSurface(colors.bgSurface, GameKind.LoneNumber.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.lonenumber_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.lonenumber_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.LoneNumber.world),
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

/** A small picture for the thumbnails: a solved 4 by 4 board with three shaded cells, one circled, and the numbers. */
@Composable
internal fun LoneNumberThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.LoneNumber.world
    val density = LocalDensity.current
    val numbers = listOf(
        2, 1, 3, 3,
        3, 2, 1, 4,
        1, 4, 2, 1,
        4, 3, 4, 2,
    )
    val shadedCells = setOf(3, 11, 12)
    val circled = 5
    Column(Modifier.size(side * 0.9f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until 4) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (column in 0 until 4) {
                    val cell = row * 4 + column
                    val isShaded = cell in shadedCells
                    Box(
                        Modifier.weight(1f).fillMaxSize()
                            .background(if (isShaded) world else colors.bgSurface, Radius.tile)
                            .border(1.dp, if (isShaded) world else colors.borderStrong.copy(alpha = 0.5f), Radius.tile),
                        contentAlignment = Alignment.Center,
                    ) {
                        val ink: Color = if (isShaded) colors.bgPage.copy(alpha = 0.7f) else colors.textPrimary
                        BasicText(
                            text = numbers[cell].toString(),
                            style = TextStyle(fontSize = with(density) { (side.toPx() * 0.9f / 4f * 0.46f).toSp() }, fontWeight = FontWeight.Bold, color = ink),
                        )
                        if (cell == circled) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawCircle(world, radius = size.minDimension * 0.38f, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(2.dp.toPx()))
                            }
                        }
                    }
                }
            }
        }
    }
}
