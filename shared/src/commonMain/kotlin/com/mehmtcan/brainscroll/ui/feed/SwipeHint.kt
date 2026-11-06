package com.mehmtcan.brainscroll.ui.feed

import brainscroll.shared.generated.resources.replay_done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.daily_game_done
import brainscroll.shared.generated.resources.swipe_hint
import org.jetbrains.compose.resources.stringResource

/** True on the screens of the daily puzzle: there is no next puzzle to swipe to, the day's puzzle is the only one. */
val LocalDailyPage = compositionLocalOf { false }

/** True on the screen that plays a favorite again (One Line, which is not in the feed): the way on is back to the favorites. */
val LocalReplayPage = compositionLocalOf { false }

/** The line under a finished puzzle: where to go next in the feed, that the daily puzzle is done, or how to leave a replay. */
@Composable
fun swipeHintText(): String = when {
    LocalReplayPage.current -> stringResource(Res.string.replay_done)
    LocalDailyPage.current -> stringResource(Res.string.daily_game_done)
    else -> "↑  " + stringResource(Res.string.swipe_hint)
}
