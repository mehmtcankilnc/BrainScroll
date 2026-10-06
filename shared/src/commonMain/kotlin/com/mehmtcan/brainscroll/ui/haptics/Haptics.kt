package com.mehmtcan.brainscroll.ui.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mehmtcan.brainscroll.game.wordle.WordleStatus

/** The four kinds of feedback in docs/design.md section 6. */
enum class HapticKind {
    /** A key, a selection, a card flip. */
    Light,

    /** A right answer, a match. */
    Medium,

    /** A wrong answer, an invalid move. */
    Warning,

    /** A finished puzzle, a longer streak. */
    Success,
}

/** Makes the phone buzz. Each platform answers in its own way; desktop does nothing. */
fun interface Haptics {
    fun perform(kind: HapticKind)
}

object NoHaptics : Haptics {
    override fun perform(kind: HapticKind) = Unit
}

/** What the screens use. The default does nothing, so previews and tests need no setup. */
val LocalHaptics = compositionLocalOf<Haptics> { NoHaptics }

/** The platform's haptics: the system vibration on Android, the Taptic Engine on iOS, nothing on desktop. */
@Composable
expect fun rememberPlatformHaptics(): Haptics

/**
 * Buzzes when a puzzle that was being played ends: a win is a success, a loss a warning. A puzzle that is
 * already finished when it appears (restored from the last session, or scrolled back to) stays quiet.
 */
@Composable
fun OutcomeHaptics(status: WordleStatus) {
    val haptics = LocalHaptics.current
    var wasPlaying by remember { mutableStateOf(status == WordleStatus.Playing) }
    LaunchedEffect(status) {
        when {
            status == WordleStatus.Playing -> wasPlaying = true
            wasPlaying -> {
                wasPlaying = false
                haptics.perform(if (status == WordleStatus.Won) HapticKind.Success else HapticKind.Warning)
            }
        }
    }
}
