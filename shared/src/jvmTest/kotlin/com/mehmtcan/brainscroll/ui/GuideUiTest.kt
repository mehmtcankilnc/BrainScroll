package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The guide of a puzzle of the feed, through the real screen (minesweeper stands in for every game). */
@OptIn(ExperimentalTestApi::class)
class GuideUiTest {

    private fun androidx.compose.ui.test.ComposeUiTest.openMinesFeed() {
        start(openFeed = false, games = listOf(GameKind.Mines))
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("mineCell").fetchSemanticsNodes().size >= 64 }
    }

    @Test
    fun theGuideIsALightBulbUnderTheStreakAndThereIsNoHintButton() = runComposeUiTest {
        openMinesFeed()
        snap("210_guide_icon")
        assertTrue(onAllNodesWithTag("guideButton").fetchSemanticsNodes().isNotEmpty())
        assertTrue(onAllNodesWithTag("hintButton").fetchSemanticsNodes().isEmpty(), "the general hint is gone")
        assertTrue(onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isEmpty(), "nothing is said until the player asks")
    }

    @Test
    fun theCrossClosesTheGuideSentence() = runComposeUiTest {
        openMinesFeed()
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("guideClose").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun theGuidePointsAtTheNextMoveAndSaysTheStreakIsLeftAlone() = runComposeUiTest {
        openMinesFeed()
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        snap("212_guide_step")

        assertTrue(hasAnyText("Başlamak için ortadan bir hücre aç", "Open a cell in the middle to start"))
        assertTrue(onAllNodesWithTag("guideMark").fetchSemanticsNodes().size == 1, "one cell is ringed")

        // Playing on makes the step old.
        onAllNodesWithTag("mineCell")[27].performClick()
        waitForIdle()
        assertTrue(onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isEmpty(), "after a move the old step is gone")
        assertFalse(onAllNodesWithTag("guideMark").fetchSemanticsNodes().isNotEmpty())
    }
}
