package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.pro.PackArt
import com.mehmtcan.brainscroll.ui.pro.ProMascot
import com.mehmtcan.brainscroll.ui.pro.SoftGlow
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * Draws the pictures of the three store products (the same ones in Google Play and the App Store) from the real store art, at 1024 x 1024:
 * the mascot only, no text. Written to `shared/build/store-icons/`. Run it when the art changes.
 */
@OptIn(ExperimentalTestApi::class)
class StoreIconExportTest {

    private fun export(name: String, art: @androidx.compose.runtime.Composable () -> Unit) = runDesktopComposeUiTest(1100, 1100) {
        setContent {
            // 256 dp at 4 pixels per dp is exactly 1024 pixels.
            CompositionLocalProvider(LocalDensity provides Density(4f, 1f), LocalReduceMotion provides true) {
                BrainScrollTheme {
                    Box(
                        Modifier.requiredSize(256.dp).testTag("icon").background(BrainScrollTheme.colors.bgPage),
                        contentAlignment = Alignment.Center,
                    ) { art() }
                }
            }
        }
        waitForIdle()
        val image = onNodeWithTag("icon").captureToImage()
        assertEquals(1024, image.width)
        assertEquals(1024, image.height)
        val dir = File("build/store-icons").also { it.mkdirs() }
        File(dir, "$name.png").writeBytes(Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }

    @Test
    fun pro() = export("brainscroll_pro") {
        val colors = BrainScrollTheme.colors
        SoftGlow(colors.accent, 256.dp, strength = 0.22f)
        Box(Modifier.scale(1.75f)) { ProMascot() }
    }

    @Test
    fun hints20() = export("brainscroll_hints_20") {
        val colors = BrainScrollTheme.colors
        SoftGlow(colors.pendingFill, 256.dp, strength = 0.16f)
        Box(Modifier.scale(1.85f)) { PackArt(20) }
    }

    @Test
    fun hints75() = export("brainscroll_hints_75") {
        val colors = BrainScrollTheme.colors
        SoftGlow(colors.pendingFill, 256.dp, strength = 0.2f)
        Box(Modifier.scale(1.85f)) { PackArt(75) }
    }
}
