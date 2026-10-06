package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.motion.Confetti
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The confetti on its own over a black background, so its pixels can be counted. */
@OptIn(ExperimentalTestApi::class)
class ConfettiUiTest {

    private fun ComposeUiTest.coloredPixels(): Int {
        val map = onRoot().captureToImage().toPixelMap()
        var count = 0
        for (x in 0 until map.width) for (y in 0 until map.height) if (map[x, y] != Color.Black) count++
        return count
    }

    private fun ComposeUiTest.show(reduceMotion: Boolean) {
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                BrainScrollTheme {
                    Box(Modifier.requiredSize(390.dp, 740.dp).background(Color.Black)) { Confetti() }
                }
            }
        }
    }

    @Test
    fun fallsThroughTheScreenThenIsGone() = runComposeUiTest {
        show(reduceMotion = false)
        mainClock.advanceTimeBy(50)
        val atStart = coloredPixels()
        mainClock.advanceTimeBy(750)
        val midway = coloredPixels()
        snap("31_confetti_alone")
        mainClock.advanceTimeBy(3_000)
        val atEnd = coloredPixels()

        assertTrue(atStart < 100, "the pieces start above the screen: $atStart pixels")
        assertTrue(midway > 500, "pieces are falling through the screen: $midway pixels")
        assertEquals(0, atEnd, "and it is over after about two seconds")
    }

    @Test
    fun drawsNothingWhenMotionIsReduced() = runComposeUiTest {
        show(reduceMotion = true)
        mainClock.advanceTimeBy(800)
        assertEquals(0, coloredPixels())
    }
}
