package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.daily_game_done
import brainscroll.shared.generated.resources.swipe_hint
import org.jetbrains.compose.resources.stringResource

/** True on the screens of the daily puzzle: there is no next puzzle to swipe to, the day's puzzle is the only one. */
val LocalDailyPage = compositionLocalOf { false }

/** The line under a finished puzzle: where to go next in the feed, or that the daily puzzle is done. */
@Composable
fun swipeHintText(): String =
    if (LocalDailyPage.current) stringResource(Res.string.daily_game_done) else "↑  " + stringResource(Res.string.swipe_hint)
