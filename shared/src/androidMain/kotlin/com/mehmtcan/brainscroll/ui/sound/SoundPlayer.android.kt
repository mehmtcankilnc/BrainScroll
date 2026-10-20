package com.mehmtcan.brainscroll.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Short effects on a SoundPool: low delay, several at once, and no audio focus taken from the player's music. */
@Composable
actual fun rememberPlatformSoundPlayer(): SoundPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember(context) { AndroidSoundPlayer(context) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

private class AndroidSoundPlayer(private val context: Context) : SoundPlayer {
    private val pool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
        )
        .build()
    private val ids = ConcurrentHashMap<String, Int>()
    private val ready = ConcurrentHashMap.newKeySet<Int>()
    private val playing = ConcurrentHashMap<String, Int>()

    init {
        pool.setOnLoadCompleteListener { _, soundId, status -> if (status == 0) ready.add(soundId) }
    }

    override fun load(sound: Sfx, wav: ByteArray) {
        if (ids.containsKey(sound.file)) return
        // SoundPool reads from a file: the bytes go to the cache folder once.
        val file = File(context.cacheDir, "sfx_${sound.file}.wav")
        if (!file.exists() || file.length() != wav.size.toLong()) file.writeBytes(wav)
        ids[sound.file] = pool.load(file.absolutePath, 1)
    }

    override fun play(sound: Sfx, volume: Float) {
        val id = ids[sound.file] ?: return
        if (id !in ready) return
        val v = volume.coerceIn(0f, 1f)
        // The same sound never plays over its own tail: it starts again from the beginning, so fast repeats stay clean.
        playing[sound.file]?.let { pool.stop(it) }
        val stream = pool.play(id, v, v, 1, 0, 1f)
        if (stream != 0) playing[sound.file] = stream
    }

    override fun release() {
        pool.release()
    }
}
