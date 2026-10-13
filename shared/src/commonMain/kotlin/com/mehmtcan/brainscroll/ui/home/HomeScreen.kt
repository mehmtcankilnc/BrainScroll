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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_answer_streak
import brainscroll.shared.generated.resources.a11y_day_streak
import brainscroll.shared.generated.resources.app_name
import brainscroll.shared.generated.resources.home_daily_continue
import brainscroll.shared.generated.resources.home_daily_description
import brainscroll.shared.generated.resources.home_daily_solve
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
import brainscroll.shared.generated.resources.confirm_cancel
import brainscroll.shared.generated.resources.home_continue_feed
import brainscroll.shared.generated.resources.new_feed_confirm
import brainscroll.shared.generated.resources.new_feed_message
import brainscroll.shared.generated.resources.new_feed_title
import com.mehmtcan.brainscroll.ui.components.ConfirmDialog
import brainscroll.shared.generated.resources.home_feed_edit
import brainscroll.shared.generated.resources.home_feed_edit_description
import brainscroll.shared.generated.resources.home_feed_more
import brainscroll.shared.generated.resources.home_feed_title
import brainscroll.shared.generated.resources.home_freeze_badge
import brainscroll.shared.generated.resources.home_start_feed
import brainscroll.shared.generated.resources.home_waiting_today
import brainscroll.shared.generated.resources.ranks_guesses
import brainscroll.shared.generated.resources.ranks_loading
import brainscroll.shared.generated.resources.ranks_you
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.social.DailyRow
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.AppTextButton
import com.mehmtcan.brainscroll.ui.components.ForwardChevron
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.daily.DailyPhase
import com.mehmtcan.brainscroll.ui.daily.DailyViewModel
import com.mehmtcan.brainscroll.ui.daily.DailyGamePhase
import com.mehmtcan.brainscroll.ui.daily.DailyGameUi
import com.mehmtcan.brainscroll.ui.daily.DailyGameViewModel
import com.mehmtcan.brainscroll.ui.daily.FlameIcon
import brainscroll.shared.generated.resources.home_daily_description_board
import brainscroll.shared.generated.resources.home_fastest_game
import brainscroll.shared.generated.resources.ranks_moves
import com.mehmtcan.brainscroll.ui.feed.BoltIcon
import com.mehmtcan.brainscroll.ui.streak.StreakInfoSheet
import com.mehmtcan.brainscroll.ui.streak.StreakKind
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
    dailyGameViewModel: DailyGameViewModel,
    socialViewModel: SocialViewModel,
    onOpenDaily: () -> Unit,
    onOpenRanks: () -> Unit,
    onOpenFeedEditor: () -> Unit,
    onStartFeed: () -> Unit,
    onContinueFeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    val feedState by feedViewModel.state.collectAsStateWithLifecycle()
    val profile by feedViewModel.profile.collectAsStateWithLifecycle()
    val enabled by feedViewModel.enabledGames.collectAsStateWithLifecycle()
    val daily by dailyViewModel.ui.collectAsStateWithLifecycle()
    val dailyGame by dailyGameViewModel.ui.collectAsStateWithLifecycle()
    val preview by socialViewModel.preview.collectAsStateWithLifecycle()
    val social by socialViewModel.ui.collectAsStateWithLifecycle()

    val ready = feedState as? FeedUiState.Ready
    val wordLanguage = ready?.feed?.language ?: daily.language

    // Opened: look again quietly (a result or a new day may have come in while another page was open).
    LaunchedEffect(Unit) {
        feedViewModel.refreshProfile()
        dailyViewModel.onShown()
        dailyGameViewModel.refresh()
    }
    LaunchedEffect(wordLanguage) { socialViewModel.loadPreview(wordLanguage) }

    var streakInfo by remember { mutableStateOf<StreakKind?>(null) }
    var confirmNewFeed by remember { mutableStateOf(false) }
    // Starting a new feed forgets a puzzle that was left half done: ask first, but only when there is one to lose.
    val startNewFeed = { if (ready?.feed?.hasUnfinished == true) confirmNewFeed = true else onStartFeed() }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Spacer(Modifier.height(Spacing.sm))

        StreakStrip(
            dayStreak = profile.dayStreak.current,
            waitingToday = !profile.dailyDoneToday,
            freezeReady = profile.dayStreak.freezesAvailable > 0,
            onDayStreak = { streakInfo = StreakKind.Daily },
        )

        DailyCard(daily.phase, daily.language, dailyGame, onOpenDaily)

        FeedCard(
            enabled = feedViewModel.availableGames.filter { it in enabled },
            wordLanguage = wordLanguage,
            onEdit = onOpenFeedEditor,
            onStart = startNewFeed,
            // A feed that was left half done can be gone on with; it is not offered when there is none.
            onContinue = onContinueFeed.takeIf { ready?.feed?.hasUnfinished == true },
        )

        // --- Fastest today ---
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BasicText(
                    text = if (dailyGame.game == GameKind.Word) stringResource(Res.string.home_fastest_title) + " · " + wordLanguage.name
                    else stringResource(Res.string.home_fastest_game, dailyGame.game.displayName()),
                    style = type.heading.copy(color = colors.textPrimary),
                    modifier = Modifier.weight(1f),
                )
                Chip(text = stringResource(Res.string.home_fastest_all), textColor = colors.textPrimary, onClick = onOpenRanks)
            }
            FastestPreview(preview.rows.take(PREVIEW_ROWS), preview.loading, preview.failed, dailyGame.game)
            if (social.access == Access.Anonymous) {
                BasicText(
                    text = stringResource(Res.string.home_fastest_sign_in),
                    style = type.caption.copy(color = colors.textSecondary),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
    if (confirmNewFeed) {
        ConfirmDialog(
            title = stringResource(Res.string.new_feed_title),
            message = stringResource(Res.string.new_feed_message),
            confirmText = stringResource(Res.string.new_feed_confirm),
            cancelText = stringResource(Res.string.confirm_cancel),
            onConfirm = { confirmNewFeed = false; onStartFeed() },
            onCancel = { confirmNewFeed = false },
        )
    }
    when (streakInfo) {
        StreakKind.Daily -> StreakInfoSheet(
            StreakKind.Daily,
            current = profile.dayStreak.current,
            best = profile.dayStreak.best,
            freezes = profile.dayStreak.freezesAvailable,
            onClose = { streakInfo = null },
        )
        StreakKind.Win -> Unit
        null -> Unit
    }
    }
}

/** The name of the app and the day streak. A dot beside the streak says today's puzzle is still open. */
@Composable
private fun StreakStrip(dayStreak: Int, waitingToday: Boolean, freezeReady: Boolean, onDayStreak: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val waiting = stringResource(Res.string.home_waiting_today)
    val dayDescription = stringResource(Res.string.a11y_day_streak, dayStreak) + if (waitingToday) ", $waiting" else ""
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // The name of the app on the left; the streaks stay in the top right corner.
        BasicText(
            text = stringResource(Res.string.app_name),
            style = type.title.copy(color = colors.textPrimary),
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier
                .testTag("dayStreak")
                .semantics(mergeDescendants = true) { contentDescription = dayDescription }
                .heightIn(min = 48.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onDayStreak),
        ) {
            FlameIcon(colors.streakDay)
            Box(Modifier.popOnIncrease(dayStreak)) {
                BasicText(dayStreak.toString(), style = type.heading.copy(color = colors.streakDay))
            }
            if (waitingToday) Box(Modifier.size(6.dp).background(colors.streakDay, CircleShape))
        }
        if (freezeReady) {
            BasicText(stringResource(Res.string.home_freeze_badge), style = type.caption.copy(color = colors.textSecondary))
        }
    }
}

/**
 * Today's puzzle: the picture and the name of the game of the day (every game comes in turn), a line about it and one
 * outlined button. The whole card opens the daily page, whatever the state is. The word puzzle needs a connection, so
 * the card says so when there is none; the other games are played on the device. The language of the word puzzle is the
 * language of the app, shown as a label.
 */
@Composable
private fun DailyCard(phase: DailyPhase, language: Language, gameUi: DailyGameUi, onOpen: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val game = gameUi.game
    Column(
        modifier = Modifier
            .testTag("dailyCard")
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onOpen)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BasicText(
                stringResource(Res.string.home_daily_title),
                style = type.title.copy(color = colors.textPrimary),
                modifier = Modifier.weight(1f),
            )
            if (game == GameKind.Word) Chip(text = language.nativeName(), textColor = colors.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            GameThumbnail(game, language, Modifier.size(DAILY_THUMBNAIL))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BasicText(game.displayName(), style = type.heading.copy(color = colors.textPrimary), modifier = Modifier.testTag("dailyCardGame"))
                if (game == GameKind.Word) {
                    when (phase) {
                        DailyPhase.Intro -> {
                            BasicText(stringResource(Res.string.home_daily_description), style = type.body.copy(color = colors.textSecondary))
                            AppTextButton(stringResource(Res.string.home_daily_solve), onOpen)
                        }
                        DailyPhase.Playing -> {
                            BasicText(stringResource(Res.string.home_daily_description), style = type.body.copy(color = colors.textSecondary))
                            AppTextButton(stringResource(Res.string.home_daily_continue), onOpen)
                        }
                        DailyPhase.Finished -> {
                            BasicText(stringResource(Res.string.home_daily_done), style = type.body.copy(color = colors.textSecondary))
                            AppTextButton(stringResource(Res.string.home_daily_result), onOpen)
                        }
                        DailyPhase.Offline, DailyPhase.Unavailable ->
                            BasicText(stringResource(Res.string.home_daily_offline), style = type.body.copy(color = colors.textSecondary))
                        DailyPhase.Loading ->
                            BasicText(stringResource(Res.string.ranks_loading), style = type.body.copy(color = colors.textSecondary))
                    }
                } else if (gameUi.phase == DailyGamePhase.Finished) {
                    BasicText(stringResource(Res.string.home_daily_done), style = type.body.copy(color = colors.textSecondary))
                    AppTextButton(stringResource(Res.string.home_daily_result), onOpen)
                } else {
                    BasicText(stringResource(Res.string.home_daily_description_board), style = type.body.copy(color = colors.textSecondary))
                    AppTextButton(stringResource(Res.string.home_daily_solve), onOpen)
                }
            }
        }
    }
}

private val DAILY_THUMBNAIL = 104.dp

/** How many games the feed card shows by name before the rest are counted ("+3 games"). */
private const val FEED_PREVIEW = 3

/**
 * The feed in short: the pictures of the first games that are on, a tile for the rest, the language of the word games
 * and the button that starts the feed. Everything that is not "go" opens the feed editor, where the games are chosen.
 */
@Composable
private fun FeedCard(
    enabled: List<GameKind>,
    wordLanguage: Language,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    /** Goes on with the feed that was left half done; null when there is none. */
    onContinue: (() -> Unit)?,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val shown = enabled.take(FEED_PREVIEW)
    val more = enabled.size - shown.size
    val tileText = when {
        more > 0 -> stringResource(Res.string.home_feed_more, more)
        else -> null
    }
    val editDescription = stringResource(Res.string.home_feed_edit_description)
    // The card is a surface with a layer behind its content that opens the editor: anywhere on the card but the start
    // button (and the thumbnails' own taps) goes to the editor. The content keeps its own accessibility nodes this way.
    Box(
        modifier = Modifier
            .testTag("feedCard")
            .fillMaxWidth()
            .clip(Radius.card)
            .background(colors.bgSurface),
    ) {
    Box(
        Modifier
            .testTag("editFeed")
            .matchParentSize()
            .semantics { contentDescription = editDescription }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onEdit),
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // The title and, at its right, the way to change what is in the feed.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BasicText(stringResource(Res.string.home_feed_title), style = type.title.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
            AppTextButton(stringResource(Res.string.home_feed_edit), onEdit, Modifier.testTag("editFeedLink"))
        }
        // Four equal slots: the pictures of the first games, then a tile for the rest. Empty slots keep the pictures their size.
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
            shown.forEach { game ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    GameThumbnail(game, wordLanguage, Modifier.fillMaxWidth().aspectRatio(1f).testTag("feedThumbnail"))
                    BasicText(text = game.displayName(), style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center), maxLines = 1, modifier = Modifier.fillMaxWidth())
                }
            }
            if (tileText != null) {
                // The text is part of the tile; the line under the pictures stays empty so the tiles are as tall as they are.
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Column(
                        modifier = Modifier
                            .testTag("feedMore")
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(Radius.control)
                            .background(colors.bgRaised),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        BasicText("+", style = type.display.copy(color = colors.textSecondary))
                        BasicText(
                            text = tileText,
                            style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                            maxLines = 1,
                        )
                    }
                    BasicText(text = " ", style = type.caption.copy(color = Color.Transparent), maxLines = 1)
                }
            }
            repeat(FEED_PREVIEW + 1 - shown.size - if (tileText != null) 1 else 0) { Spacer(Modifier.weight(1f)) }
        }
        if (onContinue != null) {
            // The feed that was left is the main button. A new one is started from the editor, which asks first.
            AppButton(stringResource(Res.string.home_continue_feed), onContinue, modifier = Modifier.testTag("continueFeed"), primary = true)
        } else {
            AppButton(stringResource(Res.string.home_start_feed), onStart, modifier = Modifier.testTag("startFeedHome"), primary = true)
        }
    }
    }
}

/** The first few rows of today's speed table. No surface: the rows sit on the page, set apart by hairlines. */
@Composable
private fun FastestPreview(rows: List<DailyRow>, loading: Boolean, failed: Boolean, game: GameKind) {
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
                    BasicText(
                        text = if (game == GameKind.Word) stringResource(Res.string.ranks_guesses, row.guesses) else stringResource(Res.string.ranks_moves, row.guesses),
                        style = type.label.copy(color = colors.textSecondary),
                    )
                    BasicText(formatDuration(row.durationMs), style = type.label.copy(color = colors.textPrimary))
                }
            }
        }
        loading -> BasicText(stringResource(Res.string.ranks_loading), style = type.body.copy(color = colors.textSecondary))
        failed -> BasicText(stringResource(Res.string.home_fastest_failed), style = type.body.copy(color = colors.textSecondary))
        else -> BasicText(stringResource(Res.string.home_fastest_empty), style = type.body.copy(color = colors.textSecondary))
    }
}
