@file:OptIn(ExperimentalLayoutApi::class)

package com.mehmtcan.brainscroll.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_answer_streak
import brainscroll.shared.generated.resources.a11y_day_streak
import brainscroll.shared.generated.resources.game_memory
import brainscroll.shared.generated.resources.game_mines
import brainscroll.shared.generated.resources.game_tubes
import brainscroll.shared.generated.resources.game_word_puzzle
import brainscroll.shared.generated.resources.home_daily_continue
import brainscroll.shared.generated.resources.home_daily_done
import brainscroll.shared.generated.resources.home_daily_offline
import brainscroll.shared.generated.resources.home_daily_play
import brainscroll.shared.generated.resources.home_daily_result
import brainscroll.shared.generated.resources.home_daily_title
import brainscroll.shared.generated.resources.home_fastest_all
import brainscroll.shared.generated.resources.home_fastest_empty
import brainscroll.shared.generated.resources.home_fastest_failed
import brainscroll.shared.generated.resources.home_fastest_sign_in
import brainscroll.shared.generated.resources.home_fastest_title
import brainscroll.shared.generated.resources.home_freeze_badge
import brainscroll.shared.generated.resources.home_waiting_today
import brainscroll.shared.generated.resources.personalize_games
import brainscroll.shared.generated.resources.personalize_language
import brainscroll.shared.generated.resources.personalize_last_game
import brainscroll.shared.generated.resources.personalize_title
import brainscroll.shared.generated.resources.ranks_guesses
import brainscroll.shared.generated.resources.ranks_loading
import brainscroll.shared.generated.resources.ranks_you
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.social.DailyRow
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.daily.DailyPhase
import com.mehmtcan.brainscroll.ui.daily.DailyViewModel
import com.mehmtcan.brainscroll.ui.daily.FlameIcon
import com.mehmtcan.brainscroll.ui.feed.BoltIcon
import com.mehmtcan.brainscroll.ui.feed.FeedUiState
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.motion.popOnIncrease
import com.mehmtcan.brainscroll.ui.social.Access
import com.mehmtcan.brainscroll.ui.social.SocialViewModel
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Number of players the "fastest today" preview shows. */
private const val PREVIEW_ROWS = 3

/**
 * The first tab (docs/decisions.md, Sadeleştirme): a thin streak strip, today's puzzle, the feed settings and a short
 * "fastest today" list. Only the puzzle card has a surface; the rest sits on the page. It reads what the other
 * screens own and sends the player on to them.
 */
@Composable
fun HomeScreen(
    feedViewModel: FeedViewModel,
    dailyViewModel: DailyViewModel,
    socialViewModel: SocialViewModel,
    onOpenDaily: () -> Unit,
    onOpenRanks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    val feedState by feedViewModel.state.collectAsStateWithLifecycle()
    val profile by feedViewModel.profile.collectAsStateWithLifecycle()
    val enabled by feedViewModel.enabledGames.collectAsStateWithLifecycle()
    val daily by dailyViewModel.ui.collectAsStateWithLifecycle()
    val preview by socialViewModel.preview.collectAsStateWithLifecycle()
    val social by socialViewModel.ui.collectAsStateWithLifecycle()

    val ready = feedState as? FeedUiState.Ready
    val answerStreak = ready?.feed?.answerStreak ?: 0
    val wordLanguage = ready?.feed?.language ?: daily.language

    // Opened: look again quietly (a result or a new day may have come in while another page was open).
    LaunchedEffect(Unit) {
        feedViewModel.refreshProfile()
        dailyViewModel.onShown()
    }
    LaunchedEffect(wordLanguage) { socialViewModel.loadPreview(wordLanguage) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Spacer(Modifier.height(Spacing.sm))

        StreakStrip(
            dayStreak = profile.dayStreak.current,
            answerStreak = answerStreak,
            waitingToday = !profile.dailyDoneToday,
            freezeReady = profile.dayStreak.freezesAvailable > 0,
        )

        DailyCard(daily.phase, daily.language, dailyViewModel, onOpenDaily)

        // --- Feed settings, in place ---
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            BasicText(stringResource(Res.string.personalize_title), style = type.heading.copy(color = colors.textPrimary))
            ChipRow(stringResource(Res.string.personalize_games)) {
                feedViewModel.availableGames.forEach { game ->
                    val on = game in enabled
                    Chip(
                        text = when (game) {
                            GameKind.Word -> stringResource(Res.string.game_word_puzzle)
                            GameKind.Memory -> stringResource(Res.string.game_memory)
                            GameKind.Tubes -> stringResource(Res.string.game_tubes)
                            GameKind.Mines -> stringResource(Res.string.game_mines)
                        },
                        textColor = if (on) colors.textPrimary else colors.textSecondary,
                        raised = on,
                        selected = on,
                        // The last game that is on cannot be switched off: the feed would be empty.
                        onClick = if (on && enabled.size == 1) null else ({ feedViewModel.setGameEnabled(game, !on) }),
                    )
                }
            }
            if (enabled.size == 1) {
                BasicText(stringResource(Res.string.personalize_last_game), style = type.caption.copy(color = colors.textSecondary))
            }
            ChipRow(stringResource(Res.string.personalize_language)) {
                Language.entries.forEach { option ->
                    Chip(
                        text = option.name,
                        textColor = if (option == wordLanguage) colors.textPrimary else colors.textSecondary,
                        raised = option == wordLanguage,
                        selected = option == wordLanguage,
                        onClick = { feedViewModel.setWordLanguage(option) },
                    )
                }
            }
        }

        // --- Fastest today ---
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BasicText(
                    text = stringResource(Res.string.home_fastest_title) + " · " + wordLanguage.name,
                    style = type.heading.copy(color = colors.textPrimary),
                    modifier = Modifier.weight(1f),
                )
                Chip(text = stringResource(Res.string.home_fastest_all), textColor = colors.textPrimary, onClick = onOpenRanks)
            }
            FastestPreview(preview.rows.take(PREVIEW_ROWS), preview.loading, preview.failed)
            if (social.access == Access.Anonymous) {
                BasicText(
                    text = stringResource(Res.string.home_fastest_sign_in),
                    style = type.caption.copy(color = colors.textSecondary),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

/** A small label and the chips of one setting, wrapping when the text is large. */
@Composable
private fun ChipRow(label: String, chips: @Composable () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(label, style = type.caption.copy(color = colors.textSecondary), modifier = Modifier.padding(end = Spacing.xs))
        chips()
    }
}

/** Both streaks in one quiet line. A dot beside the day streak says today's puzzle is still open. */
@Composable
private fun StreakStrip(dayStreak: Int, answerStreak: Int, waitingToday: Boolean, freezeReady: Boolean) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val waiting = stringResource(Res.string.home_waiting_today)
    val dayDescription = stringResource(Res.string.a11y_day_streak, dayStreak) + if (waitingToday) ", $waiting" else ""
    val answerDescription = stringResource(Res.string.a11y_answer_streak, answerStreak)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = dayDescription
            },
        ) {
            FlameIcon(colors.streakDay)
            Box(Modifier.popOnIncrease(dayStreak)) {
                BasicText(dayStreak.toString(), style = type.heading.copy(color = colors.streakDay))
            }
            if (waitingToday) Box(Modifier.size(6.dp).background(colors.streakDay, CircleShape))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = answerDescription
            },
        ) {
            BoltIcon(colors.streakAnswer)
            Box(Modifier.popOnIncrease(answerStreak)) {
                BasicText(answerStreak.toString(), style = type.heading.copy(color = colors.streakAnswer))
            }
        }
        if (freezeReady) {
            BasicText(stringResource(Res.string.home_freeze_badge), style = type.caption.copy(color = colors.textSecondary))
        }
    }
}

/**
 * Today's puzzle. The whole card opens the daily page, whatever the state is: offline, the page shows the message and
 * the retry button, so this card only says how things stand.
 */
@Composable
private fun DailyCard(phase: DailyPhase, language: Language, dailyViewModel: DailyViewModel, onOpen: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(
        modifier = Modifier
            .testTag("dailyCard")
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onOpen)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BasicText(
                stringResource(Res.string.home_daily_title),
                style = type.title.copy(color = colors.textPrimary),
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Language.entries.forEach { option ->
                    Chip(
                        text = option.name,
                        textColor = if (option == language) colors.textPrimary else colors.textSecondary,
                        raised = option == language,
                        selected = option == language,
                        onClick = { dailyViewModel.selectLanguage(option) },
                    )
                }
            }
        }
        when (phase) {
            DailyPhase.Intro -> AppButton(stringResource(Res.string.home_daily_play), onOpen, primary = true)
            DailyPhase.Playing -> AppButton(stringResource(Res.string.home_daily_continue), onOpen, primary = true)
            DailyPhase.Finished -> {
                BasicText(stringResource(Res.string.home_daily_done), style = type.body.copy(color = colors.textSecondary))
                AppButton(stringResource(Res.string.home_daily_result), onOpen)
            }
            DailyPhase.Offline, DailyPhase.Unavailable ->
                BasicText(stringResource(Res.string.home_daily_offline), style = type.body.copy(color = colors.textSecondary))
            DailyPhase.Loading ->
                BasicText(stringResource(Res.string.ranks_loading), style = type.body.copy(color = colors.textSecondary))
        }
    }
}

/** The first few rows of today's speed table. No surface: the rows sit on the page, set apart by hairlines. */
@Composable
private fun FastestPreview(rows: List<DailyRow>, loading: Boolean, failed: Boolean) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    when {
        rows.isNotEmpty() -> Column {
            rows.forEachIndexed { index, row ->
                if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                ) {
                    BasicText(row.rank.toString(), style = type.label.copy(color = colors.textSecondary))
                    BasicText(
                        text = if (row.isMe) row.username + " (" + stringResource(Res.string.ranks_you) + ")" else row.username,
                        style = type.body.copy(color = colors.textPrimary),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    BasicText(stringResource(Res.string.ranks_guesses, row.guesses), style = type.label.copy(color = colors.textSecondary))
                    BasicText(formatDuration(row.durationMs), style = type.label.copy(color = colors.textPrimary))
                }
            }
        }
        loading -> BasicText(stringResource(Res.string.ranks_loading), style = type.body.copy(color = colors.textSecondary))
        failed -> BasicText(stringResource(Res.string.home_fastest_failed), style = type.body.copy(color = colors.textSecondary))
        else -> BasicText(stringResource(Res.string.home_fastest_empty), style = type.body.copy(color = colors.textSecondary))
    }
}
