package com.mehmtcan.brainscroll.ui.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.Haptics

/**
 * The haptics of the app with the common sounds on top: wherever a game buzzes, it also plays the matching sound (a tap, a note for
 * a right answer, a low one for a wrong answer, the win chord). So every game sounds right without a line of its own; a game with
 * sounds of its own plays the haptics through [withoutSound].
 */
class SoundHaptics(val inner: Haptics, private val sounds: SoundEffects) : Haptics {
    override fun perform(kind: HapticKind) {
        inner.perform(kind)
        when (kind) {
            HapticKind.Light -> sounds.play(Sfx.UiTap, UI_TAP_VOLUME)
            HapticKind.Medium -> sounds.play(Sfx.UiGood, UI_GOOD_VOLUME)
            HapticKind.Warning -> sounds.play(Sfx.UiWarning, UI_WARNING_VOLUME)
            HapticKind.Success -> sounds.play(Sfx.UiWinChord, UI_WIN_CHORD_VOLUME)
        }
    }
}

/** The same haptics without the common sounds, for the games that play their own. */
fun Haptics.withoutSound(): Haptics = (this as? SoundHaptics)?.inner ?: this

@Composable
fun rememberSoundHaptics(inner: Haptics, sounds: SoundEffects): Haptics = remember(inner, sounds) { SoundHaptics(inner, sounds) }
