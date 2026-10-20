package com.mehmtcan.brainscroll.ui.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import brainscroll.shared.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val TAP_DEDUPE_MILLIS = 60L

/** One sound file of the pack in `files/sfx`, by name without the extension. */
@kotlin.jvm.JvmInline
value class Sfx(val file: String) {
    companion object {
        val BallSlideWhoosh = Sfx("ballslide_whoosh")
        val BallSlideCell = Sfx("ballslide_cell")
        val BallSlideWinChord = Sfx("win_ballslide_chord")
        val NonogramWinChord = Sfx("win_nonogram_chord")

        /** The sounds every other game shares, played with its haptics (see [SoundHaptics]). */
        val UiTap = Sfx("ui_tap")
        val UiWarning = Sfx("ui_warning")
        val UiWinChord = Sfx("ui_win_chord")
        val UiGood = Sfx("ui_good")

        /** Sounds of one game's own action. */
        val TubesPour = Sfx("tubes_pour")
        val MinesBoom = Sfx("mines_boom")
        val HanoiThunk = Sfx("hanoi_thunk")

        /** Notes of the pentatonic ladder: 10 for the fills of Nonogram, 8 rattles for finished lines. */
        fun nonogramFill(note: Int) = Sfx("nonogram_fill_" + two(note))
        fun nonogramLine(note: Int) = Sfx("nonogram_line_" + two(note))

        /** Every file of the pack, so the platform can load them when the app starts. */
        val All: List<Sfx> = listOf(BallSlideWhoosh, BallSlideCell, BallSlideWinChord, NonogramWinChord, UiTap, UiGood, UiWarning, UiWinChord, TubesPour, MinesBoom, HanoiThunk) +
            (0 until 10).map(::nonogramFill) + (0 until 8).map(::nonogramLine)

        private fun two(n: Int) = n.toString().padStart(2, '0')
    }
}

/** Plays a short sound. [volume] is 0..1 on top of the file's own level. */
fun interface SoundEffects {
    fun play(sound: Sfx, volume: Float)
}

object NoSoundEffects : SoundEffects {
    override fun play(sound: Sfx, volume: Float) = Unit
}

/** What the screens use. The default is silent, so previews and tests need no setup. */
val LocalSoundEffects = compositionLocalOf<SoundEffects> { NoSoundEffects }

/** What a platform has to do: keep the decoded files and play one by name, many at once. */
interface SoundPlayer {
    fun load(sound: Sfx, wav: ByteArray)
    fun play(sound: Sfx, volume: Float)
    fun release()
}

/** The platform's player: SoundPool on Android, AVAudioPlayer on iOS, nothing on desktop. */
@Composable
expect fun rememberPlatformSoundPlayer(): SoundPlayer

/**
 * The effects the app uses: the platform player, loaded with the pack once, and silent while [enabled] says so (the switch in the
 * settings).
 */
@Composable
fun rememberSoundEffects(enabled: Boolean): SoundEffects {
    val player = rememberPlatformSoundPlayer()
    LaunchedEffect(player) {
        withContext(Dispatchers.Default) {
            for (sound in Sfx.All) {
                runCatching { player.load(sound, Res.readBytes("files/sfx/${sound.file}.wav")) }
            }
        }
    }
    val on by rememberUpdatedState(enabled)
    return remember(player) {
        // The same click is never played twice within a blink: a button that clicks by itself and also buzzes through the haptics
        // (which play the click too) is heard once.
        var lastTap: TimeMark? = null
        SoundEffects { sound, volume ->
            if (on) {
                val skip = sound == Sfx.UiTap && lastTap?.let { it.elapsedNow() < TAP_DEDUPE_MILLIS.milliseconds } == true
                if (!skip) {
                    if (sound == Sfx.UiTap) lastTap = TimeSource.Monotonic.markNow()
                    player.play(sound, volume)
                }
            }
        }
    }
}
