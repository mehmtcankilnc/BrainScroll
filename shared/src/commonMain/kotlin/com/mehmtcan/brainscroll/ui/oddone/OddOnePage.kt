package com.mehmtcan.brainscroll.ui.oddone

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import brainscroll.shared.generated.resources.a11y_oddone_board
import brainscroll.shared.generated.resources.a11y_oddone_number
import brainscroll.shared.generated.resources.a11y_oddone_right
import brainscroll.shared.generated.resources.a11y_oddone_wrong
import brainscroll.shared.generated.resources.oddone_hint
import brainscroll.shared.generated.resources.oddone_lost
import brainscroll.shared.generated.resources.oddone_result
import brainscroll.shared.generated.resources.oddone_round
import brainscroll.shared.generated.resources.oddone_rule
import brainscroll.shared.generated.resources.oddone_won
import brainscroll.shared.generated.resources.result_stat_rounds
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.oddone.OddResult
import com.mehmtcan.brainscroll.game.oddone.OddReveal
import com.mehmtcan.brainscroll.game.oddone.OddSnapshot
import com.mehmtcan.brainscroll.game.oddone.OddStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import com.mehmtcan.brainscroll.ui.components.ResultStats
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
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** At least this high, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val Gap = 8.dp
private val MaxCell = 64.dp

/** The number a button holds, for tests. */
internal val OddNumberKey = SemanticsPropertyKey<Int>("OddNumber")
private var SemanticsPropertyReceiver.oddNumber by OddNumberKey

/** Whether a button holds the number that breaks the rule. Only tests read it: a screen reader is told after the pick, not before. */
internal val OddBreaksKey = SemanticsPropertyKey<Boolean>("OddBreaks")
private var SemanticsPropertyReceiver.oddBreaks by OddBreaksKey

/** How long the rule is shown after a right pick, before the next question (a little longer when motion is reduced: nothing else moves). */
private const val REVEAL_MILLIS = 1_400L

/**
 * One full-screen page of the feed: which round it is, five numbers in a row (tap the one that does not follow the rule the other four
 * do), and the hint. A pick turns the right number green (and a wrong pick red) and shows the name of the rule for a moment; a right
 * pick then brings the next question, a wrong one ends the run. The result takes the place of the hint when the run is over.
 */
@Composable
fun OddOnePage(
    page: OddSnapshot,
    onPick: (Int) -> OddResult,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val puzzle = state.puzzle
    val playing = state.status == OddStatus.Playing

    // The result and the buzz wait for the rule to be seen.
    val status = rememberSettledStatus(
        when (state.status) {
            OddStatus.Playing -> WordleStatus.Playing
            OddStatus.Won -> WordleStatus.Won
            OddStatus.Lost -> WordleStatus.Lost
        },
        REVEAL_MILLIS,
    )
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    // The pick that was just made stays on the screen with its rule for a moment (for a run that is over it stays).
    var reveal by remember(page.id) { mutableStateOf<OddReveal?>(null) }
    LaunchedEffect(state.last) {
        val last = state.last ?: return@LaunchedEffect
        reveal = last
        if (state.status == OddStatus.Playing) {
            delay(REVEAL_MILLIS)
            reveal = null
        }
    }

    val shown = reveal?.question ?: state.question
    val roundShown = (if (reveal?.correct == true) state.cleared - 1 else minOf(state.cleared, puzzle.rounds - 1)) + 1
    val ruleName = stringResource(oddRuleName(shown.rule))

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(
            text = stringResource(Res.string.oddone_round, roundShown, puzzle.rounds),
            textColor = if (state.status == OddStatus.Won) colors.correctFill else colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cell: Dp = minOf(MaxCell, (maxWidth - Gap * 4) / 5)
            val description = stringResource(Res.string.a11y_oddone_board)
            Row(
                modifier = Modifier.testTag("oddBoard").semantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(Gap),
            ) {
                shown.numbers.forEachIndexed { index, number ->
                    GuideMark(isGuided(index) && playing && reveal == null, Radius.tile, evidence = isGuideEvidence(index) && playing && reveal == null) {
                        OddNumber(
                            number = number,
                            breaks = index == shown.oddIndex,
                            right = reveal != null && index == shown.oddIndex,
                            wrong = reveal != null && !reveal!!.correct && index == reveal!!.picked,
                            enabled = playing && reveal == null,
                            size = cell,
                            onClick = {
                                when (onPick(index)) {
                                    OddResult.Correct, OddResult.Perfect -> haptics.perform(HapticKind.Medium)
                                    OddResult.Wrong -> haptics.perform(HapticKind.Warning)
                                    OddResult.Ignored -> Unit
                                }
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status != WordleStatus.Playing) {
                OddOneResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                BasicText(
                    text = if (reveal != null) stringResource(Res.string.oddone_rule, ruleName) else stringResource(Res.string.oddone_hint),
                    modifier = Modifier.semantics { if (reveal != null) liveRegion = LiveRegionMode.Polite },
                    style = type.body.copy(color = if (reveal == null) colors.textSecondary else if (reveal!!.correct) colors.correctFill else colors.error, textAlign = TextAlign.Center),
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** One number: a square tile with the number in it. After a pick the right one is green and a wrong pick is red, so the answer is told by its colour and its description. */
@Composable
private fun OddNumber(
    number: Int,
    breaks: Boolean,
    right: Boolean,
    wrong: Boolean,
    enabled: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.tile
    val duration = if (LocalReduceMotion.current) 0 else Motion.BASE
    val fill by animateColorAsState(
        when {
            right -> colors.correctFill.copy(alpha = 0.28f)
            wrong -> colors.error.copy(alpha = 0.28f)
            else -> colors.bgRaised
        },
        tween(duration, easing = Motion.Standard),
    )
    val border = when {
        right -> colors.correctFill
        wrong -> colors.error
        else -> colors.borderStrong.copy(alpha = 0.28f)
    }
    val description = stringResource(Res.string.a11y_oddone_number, number)
    val state = when {
        right -> stringResource(Res.string.a11y_oddone_right)
        wrong -> stringResource(Res.string.a11y_oddone_wrong)
        else -> ""
    }
    val density = LocalDensity.current
    val numberSize = with(density) { (size.toPx() * 0.5f).toSp() }
    Box(
        modifier = Modifier
            .size(size)
            .testTag("oddNumber")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (state.isNotEmpty()) stateDescription = state
                oddNumber = number
                oddBreaks = breaks
                role = Role.Button
            }
            .background(fill, shape)
            .border((1.5f + extraBorder).dp, border, shape)
            .then(
                if (enabled) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = number.toString(),
            style = TextStyle(
                fontSize = numberSize,
                fontWeight = FontWeight.Bold,
                color = when {
                    right -> colors.correctFill
                    wrong -> colors.error
                    else -> colors.textPrimary
                },
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun OddOneResultCard(page: OddSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val won = page.state.status == OddStatus.Won
    val rounds = page.state.cleared

    Box(modifier = modifier.fillMaxWidth().testTag("oddResultCard").resultSurface(colors.bgSurface, GameKind.OddOne.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(if (won) Res.string.oddone_won else Res.string.oddone_lost),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = if (won) colors.correctFill else colors.error, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.oddone_rule, stringResource(oddRuleName(page.state.question.rule))),
                style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.oddone_result, rounds),
                stats = listOf(
                    ResultStatData(rounds.toString(), stringResource(Res.string.result_stat_rounds), GameKind.OddOne.world),
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.OddOne.world),
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

/** A small picture for the thumbnails: five numbers, four alike and one that breaks the rule, in the colour of the game. */
@Composable
internal fun OddOneThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.OddOne.world
    val density = LocalDensity.current
    val numbers = listOf(2, 4, 6, 8, 9)
    val fontSize = with(density) { (side.toPx() * 0.9f / 3f * 0.5f).toSp() }
    Column(Modifier.size(side * 0.9f), verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) {
        for (rowNumbers in listOf(numbers.take(3), numbers.drop(3))) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)) {
                for (n in rowNumbers) {
                    val odd = n == 9
                    Box(
                        Modifier.size(side * 0.9f / 3f - 3.dp)
                            .background(if (odd) world.copy(alpha = 0.28f) else colors.bgRaised, Radius.tile)
                            .border(1.dp, if (odd) world else colors.borderStrong.copy(alpha = 0.28f), Radius.tile),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = n.toString(),
                            style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold, color = if (odd) world else colors.textPrimary),
                        )
                    }
                }
            }
        }
    }
}
