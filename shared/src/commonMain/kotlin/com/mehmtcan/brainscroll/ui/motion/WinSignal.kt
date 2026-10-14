package com.mehmtcan.brainscroll.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import kotlinx.coroutines.delay

/**
 * Which pages have shown their win to the player: the page says so (through [com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics])
 * when it has finished showing the last move, and not when the game rules say it is won. The confetti waits for it, so it never
 * comes while the last disk, tile or card is still moving.
 */
@Stable
class WinSignal {
    private val shown = mutableStateListOf<String>()

    fun markShown(pageId: String) {
        if (pageId !in shown) shown += pageId
    }

    fun isShown(pageId: String): Boolean = pageId in shown
}

/** The signal of the screen the pages are on (the feed, the daily puzzle). Null in a page that stands on its own. */
val LocalWinSignal = compositionLocalOf<WinSignal?> { null }

/**
 * The status a page shows: the same as [status], except that when it changes from Playing to won or lost it stays Playing
 * for [settleMillis] first, so the last move can finish moving before the result, the buzz and the confetti arrive. A page
 * that appears already finished (restored, scrolled back to) shows it at once, and so does a page with reduced motion.
 * Going back to Playing is never held back. Use the time of the longest animation the last move can start.
 */
@Composable
fun rememberSettledStatus(status: WordleStatus, settleMillis: Long): WordleStatus {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(status) }
    LaunchedEffect(status, settleMillis, reduce) {
        if (status != WordleStatus.Playing && shown == WordleStatus.Playing && !reduce && settleMillis > 0) delay(settleMillis)
        shown = status
    }
    return if (status == WordleStatus.Playing) status else shown
}
