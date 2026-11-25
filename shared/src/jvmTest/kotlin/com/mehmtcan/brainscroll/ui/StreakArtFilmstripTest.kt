package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import com.mehmtcan.brainscroll.ui.pro.StreakArt
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.DarkColors
import kotlin.test.Test

/** The streak offer's story frame by frame: `shared/build/screenshots/93x_streak_t*.png`. */
@OptIn(ExperimentalTestApi::class)
class StreakArtFilmstripTest {
    @Test
    fun theStoryFrameByFrame() = runSkikoComposeUiTest(size = Size(520f, 520f), density = Density(2f)) {
        setContent {
            BrainScrollTheme {
                Box(Modifier.fillMaxSize().background(DarkColors.bgPage), contentAlignment = Alignment.Center) { StreakArt(days = 14) }
            }
        }
        // Time only moves where the test moves it, so each frame is the moment asked for.
        mainClock.autoAdvance = false
        var elapsed = 0L
        for ((name, at) in listOf("930_streak_t0" to 300L, "931_streak_t1" to 1_000L, "932_streak_t2" to 1_450L, "933_streak_t3" to 1_900L, "934_streak_t4" to 2_500L, "935_streak_t5" to 4_200L)) {
            mainClock.advanceTimeBy(at - elapsed)
            elapsed = at
            snap(name)
        }
    }
}
