package com.mehmtcan.brainscroll.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mehmtcan.brainscroll.game.PageSnapshot
import kotlinx.coroutines.delay

private const val CONFETTI_MILLIS = 2_300L

/**
 * Throws confetti once when [current] is won while it is on screen: not for a puzzle that is shown again already won
 * (restored, scrolled back to). Put it over the page; it draws nothing otherwise and never takes touches.
 */
@Composable
fun CelebrateWin(current: PageSnapshot) {
    val seenPlaying = remember { mutableSetOf<String>() }
    val celebrated = remember { mutableSetOf<String>() }
    var confettiFor by remember { mutableStateOf<String?>(null) }
    if (!current.isFinished) seenPlaying += current.id
    LaunchedEffect(current.id, current.isWon) {
        if (current.isWon && current.id in seenPlaying && celebrated.add(current.id)) {
            confettiFor = current.id
            delay(CONFETTI_MILLIS)
            confettiFor = null
        }
    }
    confettiFor?.let { id -> key(id) { Confetti() } }
}
