package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.home.GameThumbnail
import com.mehmtcan.brainscroll.ui.home.warmUpThumbnailSamples
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import java.io.File
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The game pictures of the feed editor are PNG files in the app (composeResources/drawable/thumb_*.png), made from the live pictures
 * ([GameThumbnail]) by this test. Drawing them on the phone every time was slow; a file is not.
 *
 * To make them again after a picture changed (or a game was added), run the tests with the environment variable GENERATE_THUMBS=1.
 * Without it the test only checks that every game has its file. The dark theme only: the app has no light theme.
 */
@OptIn(ExperimentalTestApi::class)
class ThumbnailPngTest {

    /** The file name (without .png) of a picture. The word puzzle has one per language. */
    private fun names(): List<Pair<String, Pair<GameKind, Language>>> =
        GameKind.entries.flatMap { kind ->
            if (kind == GameKind.Word) Language.entries.map { "thumb_word_${it.name.lowercase()}" to (kind to it) }
            else listOf("thumb_${kind.id}" to (kind to Language.TR))
        }

    private val dir = File("src/commonMain/composeResources/drawable")

    @Test
    fun everyGameHasItsPicture() {
        if (System.getenv("GENERATE_THUMBS") == "1") return
        for ((name, _) in names()) assertTrue(File(dir, "$name.png").isFile, "$name.png is missing: run the tests with GENERATE_THUMBS=1")
    }

    @Test
    fun generateWhenAsked() = runComposeUiTest {
        if (System.getenv("GENERATE_THUMBS") != "1") return@runComposeUiTest
        runBlocking { warmUpThumbnailSamples() }
        dir.mkdirs()
        var current by mutableStateOf(names().first().second)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f)) {
                BrainScrollTheme {
                    Box(Modifier.size(WIDTH_DP.dp, (WIDTH_DP / 1.25f).dp).testTag("thumb")) {
                        GameThumbnail(current.first, current.second, Modifier.size(WIDTH_DP.dp, (WIDTH_DP / 1.25f).dp))
                    }
                }
            }
        }
        for ((name, pick) in names()) {
            current = pick
            waitForIdle()
            val image = onNodeWithTag("thumb").captureToImage()
            val bytes = Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes
            File(dir, "$name.png").writeBytes(bytes)
        }
    }

    private companion object {
        /** The width the pictures are made at (3x the density: sharp on a phone, the editor shows them about 90 dp wide). */
        const val WIDTH_DP = 120f
    }
}
