package com.mehmtcan.brainscroll.ui.sound

import androidx.compose.runtime.Composable

private object NoSoundPlayer : SoundPlayer {
    override fun load(sound: Sfx, wav: ByteArray) = Unit
    override fun play(sound: Sfx, volume: Float) = Unit
    override fun release() = Unit
}

/** Desktop plays nothing, like its haptics. */
@Composable
actual fun rememberPlatformSoundPlayer(): SoundPlayer = NoSoundPlayer
