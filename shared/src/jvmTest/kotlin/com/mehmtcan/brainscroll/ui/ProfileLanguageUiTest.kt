package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The daily numbers of the profile for both languages together or for one of them. */
@OptIn(ExperimentalTestApi::class)
class ProfileLanguageUiTest {

    private fun ComposeUiTest.scrollProfileTo(vararg texts: String) {
        onNode(hasScrollAction()).performScrollToNode(texts.map { hasText(it, substring = true) }.reduce { a, b -> a or b })
        waitForIdle()
    }

    /** A Turkish puzzle that took 1:23 and an English one that took 0:40, both finished earlier today. */
    private fun twoDailyResults(): TestCloud {
        val cloud = TestCloud()
        val clock = cloud.daily.clock
        cloud.daily.prefill(Language.TR, listOf("KİTAP"), startedAt = clock - 83_000)
        cloud.daily.prefill(Language.EN, listOf("CRANE"), startedAt = clock - 40_000)
        return cloud
    }

    @Test
    fun theDailyNumbersCanBeShownForOneLanguage() = runComposeUiTest {
        val cloud = twoDailyResults()
        start(cloud)
        // Opening the daily tab in each language brings the finished result onto the device.
        openTab("Günlük", "Daily")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        // The language chip that is not selected switches the puzzle.
        onAllNodesWithText(if (hasAnyText("Günün bulmacası")) "EN" else "TR")[0].performClick()
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()

        openTab("Profil", "Profile")
        waitForIdle()
        scrollProfileTo("En iyi süre", "Best time")
        snap("70_profile_daily_both")
        assertTrue(hasAnyText("En iyi süre: 00:40", "Best time: 00:40"), "both together: the faster one")

        scrollProfileTo("TR")
        // The chip "TR" of the daily section (the feed's own TR chip is on another tab).
        onAllNodesWithText("TR")[0].performClick()
        waitForIdle()
        scrollProfileTo("En iyi süre", "Best time")
        snap("71_profile_daily_tr")
        assertTrue(hasAnyText("En iyi süre: 01:23", "Best time: 01:23"), "Turkish alone")
        assertFalse(hasAnyText("00:40"))
        assertTrue(hasAnyText("Gün serisi iki dilden", "The day streak counts either language"))

        onAllNodesWithText("EN")[0].performClick()
        waitForIdle()
        scrollProfileTo("En iyi süre", "Best time")
        assertTrue(hasAnyText("En iyi süre: 00:40", "Best time: 00:40"), "English alone")
        assertFalse(hasAnyText("01:23"))
    }
}
