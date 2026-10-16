package com.mehmtcan.brainscroll.ui.daily

import com.mehmtcan.brainscroll.ui.motion.LocalWinSignal
import com.mehmtcan.brainscroll.ui.motion.WinSignal
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_day_streak
import brainscroll.shared.generated.resources.back
import brainscroll.shared.generated.resources.daily_game_finished
import brainscroll.shared.generated.resources.daily_game_lost
import brainscroll.shared.generated.resources.daily_game_result
import brainscroll.shared.generated.resources.daily_game_won
import brainscroll.shared.generated.resources.daily_next
import brainscroll.shared.generated.resources.daily_title
import brainscroll.shared.generated.resources.ranks_loading
import com.mehmtcan.brainscroll.daily.formatCountdown
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.crossword.CrosswordSchedule
import com.mehmtcan.brainscroll.game.crossword.CrosswordShare
import com.mehmtcan.brainscroll.game.crossword.CrosswordSnapshot
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.components.ChevronButton
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.feed.GamePage
import com.mehmtcan.brainscroll.ui.feed.LocalDailyPage
import com.mehmtcan.brainscroll.ui.home.displayName
import com.mehmtcan.brainscroll.ui.crossword.CrosswordDoneInfo
import com.mehmtcan.brainscroll.ui.crossword.LocalCrosswordShare
import com.mehmtcan.brainscroll.ui.howto.HowToIconButton
import com.mehmtcan.brainscroll.ui.howto.HowToPlaySheet
import com.mehmtcan.brainscroll.ui.motion.CelebrateWin
import com.mehmtcan.brainscroll.ui.motion.popOnIncrease
import com.mehmtcan.brainscroll.ui.streak.StreakInfoSheet
import com.mehmtcan.brainscroll.ui.streak.StreakKind
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The daily puzzle on the days when it is not the word puzzle: the same screen of the game as in the feed, with a bar of
 * its own (back, the name of the game and "?", the day streak) and the confetti for a win. A puzzle that was finished
 * before the app was opened this time shows its result and the time to the next one instead of the board.
 */
@Composable
fun DailyGameScreen(viewModel: DailyGameViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    // Opening the screen looks for a new day.
    LaunchedEffect(viewModel) { viewModel.refresh() }
    var howTo by remember { mutableStateOf(false) }
    // The page says when its win is on show; the confetti waits for it.
    val winSignal = remember { WinSignal() }
    var streakInfo by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            DailyGameBar(
                title = ui.game.displayName(),
                streak = ui.dayStreak.current,
                onBack = onBack,
                onHowToPlay = { howTo = true },
                onStreak = { streakInfo = true },
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val page = ui.page
                // What a player can paste into a chat once today's board is solved.
                val name = ui.game.displayName()
                val shareText = (page as? CrosswordSnapshot)?.takeIf { it.isFinished }?.let {
                    CrosswordShare.text(name, CrosswordSchedule.puzzleNumber(ui.day), it.stars, formatDuration(it.durationMs), ui.dayStreak.current)
                }
                CompositionLocalProvider(LocalDailyPage provides true, LocalCrosswordShare provides shareText, LocalWinSignal provides winSignal) {
                    val result = ui.result
                    when {
                        page != null -> GamePage(page, 0, viewModel, ui.favoriteIds)
                        result != null -> FinishedSummary(result)
                        else -> Loading()
                    }
                }
            }
        }
        ui.page?.let { CelebrateWin(it, winSignal) }
        if (howTo) HowToPlaySheet(ui.game, onClose = { howTo = false })
        if (streakInfo) {
            StreakInfoSheet(
                StreakKind.Daily,
                current = ui.dayStreak.current,
                best = ui.dayStreak.best,
                freezes = ui.dayStreak.freezesAvailable,
                onClose = { streakInfo = false },
            )
        }
    }
}

@Composable
private fun DailyGameBar(title: String, streak: Int, onBack: () -> Unit, onHowToPlay: () -> Unit, onStreak: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = Spacing.sm, end = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChevronButton(stringResource(Res.string.back), colors.textPrimary, onBack, touch = 48.dp)
        Column(Modifier.padding(start = Spacing.xs)) {
            BasicText(
                text = title,
                style = type.heading.copy(color = colors.textPrimary),
                maxLines = 1,
                modifier = Modifier.semantics { heading() }.testTag("dailyGameTitle"),
            )
            BasicText(stringResource(Res.string.daily_title), style = type.caption.copy(color = colors.textSecondary), maxLines = 1)
        }
        HowToIconButton(onHowToPlay)
        Box(Modifier.weight(1f))
        Box(Modifier.popOnIncrease(streak).testTag("dayStreak")) {
            Chip(
                text = streak.toString(),
                textColor = colors.streakDay,
                description = stringResource(Res.string.a11y_day_streak, streak),
                onClick = onStreak,
            ) { FlameIcon(colors.streakDay) }
        }
    }
}

/** The board is being made: a quiet word, no spinner. */
@Composable
private fun Loading() {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Box(Modifier.fillMaxSize().testTag("dailyGameLoading"), contentAlignment = Alignment.Center) {
        BasicText(stringResource(Res.string.ranks_loading), style = type.body.copy(color = colors.textSecondary))
    }
}

/** What is known of a puzzle finished earlier: how it went, how long it took, and when the next one comes. */
@OptIn(ExperimentalTime::class)
@Composable
private fun FinishedSummary(result: FinishedRound) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var nowMs by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = Clock.System.now().toEpochMilliseconds()
            delay(1000)
        }
    }
    val won = result.outcome == Outcome.WON
    Box(Modifier.fillMaxSize().padding(Spacing.lg), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.xl).testTag("dailyGameSummary"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BasicText(
                stringResource(Res.string.daily_game_finished),
                style = type.title.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
            )
            BasicText(
                stringResource(if (won) Res.string.daily_game_won else Res.string.daily_game_lost),
                style = type.heading.copy(color = if (won) colors.correctFill else colors.textSecondary),
            )
            if (result.game == GameKind.Crossword) {
                CrosswordDoneInfo(result)
            } else {
                BasicText(
                    stringResource(Res.string.daily_game_result, result.moveCount, formatDuration(result.durationMs ?: 0L)),
                    style = type.body.copy(color = colors.textPrimary),
                )
                BasicText(
                    stringResource(Res.string.daily_next, formatCountdown(IstanbulDay.millisUntilNextDay(nowMs))),
                    style = type.caption.copy(color = colors.textSecondary),
                )
            }
        }
    }
}
