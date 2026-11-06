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

    /**
     * True from the move that wins a puzzle until the finger that made it is lifted: the feed does not scroll in that time, so a swipe
     * that wins cannot carry the player to the next page. The feed screen lets go as soon as no finger is down.
     */
    var scrollLocked by mutableStateOf(false)
}

/**
 * A short swell of the whole page at the moment its win is on show (the board "breathes" with the burst), then it settles back with a
 * spring. Only for a page that was seen playing, never for one that is shown already won; none with reduced motion.
 * Returns the scale (1 at rest); apply it to the page with a graphics layer.
 */
@Composable
fun rememberWinPulse(pageId: String, finished: Boolean, signal: WinSignal?): Float {
    val reduce = LocalReduceMotion.current
    val seenPlaying = remember(pageId) { booleanArrayOf(false) }
    if (!finished) seenPlaying[0] = true
    val shown = signal?.isShown(pageId) ?: false
    val scale = remember(pageId) { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(pageId, shown) {
        if (shown && seenPlaying[0] && !reduce) {
            scale.animateTo(1.018f,androidx.compose.animation.core.tween(110))
            scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 300f))
        }
    }
    return scale.value
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
