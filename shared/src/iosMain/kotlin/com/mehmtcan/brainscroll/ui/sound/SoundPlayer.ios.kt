package com.mehmtcan.brainscroll.ui.sound

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.Foundation.NSData
import platform.Foundation.create
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** A few AVAudioPlayers for each file, used in turn so a sound can start again while its last one is still going. */
@Composable
actual fun rememberPlatformSoundPlayer(): SoundPlayer = remember { IosSoundPlayer() }

private const val VOICES = 3

@OptIn(ExperimentalForeignApi::class)
private class IosSoundPlayer : SoundPlayer {
    private val voices = HashMap<String, List<AVAudioPlayer>>()
    private val next = HashMap<String, Int>()

    init {
        // Ambient: silent when the ringer switch is off, and the player's music goes on playing.
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryAmbient, error = null)
    }

    override fun load(sound: Sfx, wav: ByteArray) {
        val data = wav.usePinned { NSData.create(bytes = it.addressOf(0), length = wav.size.toULong()) }
        // The maps belong to the main thread (play runs there): the files are read elsewhere, but filed here.
        dispatch_async(dispatch_get_main_queue()) {
            if (!voices.containsKey(sound.file)) {
                val players = (0 until VOICES).mapNotNull { AVAudioPlayer(data = data, error = null)?.also { p -> p.prepareToPlay() } }
                if (players.isNotEmpty()) voices[sound.file] = players
            }
        }
    }

    override fun play(sound: Sfx, volume: Float) {
        val players = voices[sound.file] ?: return
        val i = next[sound.file] ?: 0
        next[sound.file] = (i + 1) % players.size
        val player = players[i]
        player.volume = volume.coerceIn(0f, 1f)
        player.currentTime = 0.0
        player.play()
    }

    override fun release() {
        voices.values.flatten().forEach { it.stop() }
        voices.clear()
    }
}
