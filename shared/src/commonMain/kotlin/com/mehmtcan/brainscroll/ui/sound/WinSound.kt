package com.mehmtcan.brainscroll.ui.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import kotlinx.coroutines.delay

/** One sound of a win, [atMillis] after the win. */
class WinStep(val atMillis: Long, val sound: Sfx, val volume: Float)

/**
 * Plays the [steps] when a puzzle that was being played is won (the [lostSteps] when it is lost). A puzzle that is already won when it appears (restored from the
 * last session, or scrolled back to) stays quiet: the same rule as [com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics].
 */
@Composable
fun WinSound(status: WordleStatus, steps: List<WinStep>, lostSteps: List<WinStep> = emptyList()) {
    val sounds = LocalSoundEffects.current
    var wasPlaying by remember { mutableStateOf(status == WordleStatus.Playing) }
    LaunchedEffect(status) {
        when {
            status == WordleStatus.Playing -> wasPlaying = true
            wasPlaying -> {
                wasPlaying = false
                var now = 0L
                for (step in (if (status == WordleStatus.Won) steps else lostSteps).sortedBy { it.atMillis }) {
                    delay(step.atMillis - now)
                    now = step.atMillis
                    sounds.play(step.sound, step.volume)
                }
            }
        }
    }
}
