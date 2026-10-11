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

/** The hint and the guide under a puzzle of the feed, through the real screen (minesweeper stands in for every game). */
@OptIn(ExperimentalTestApi::class)
class GuideUiTest {

    private fun androidx.compose.ui.test.ComposeUiTest.openMinesFeed() {
        start(openFeed = false, games = listOf(GameKind.Mines))
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("mineCell").fetchSemanticsNodes().size >= 64 }
    }

    @Test
    fun theButtonsAreUnderAPuzzleThatIsNotOver() = runComposeUiTest {
        openMinesFeed()
        snap("210_help_row")
        assertTrue(onAllNodesWithTag("hintButton").fetchSemanticsNodes().isNotEmpty())
        assertTrue(onAllNodesWithTag("guideButton").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun theHintIsAGeneralTipAndChangesNothing() = runComposeUiTest {
        openMinesFeed()
        onNodeWithTag("hintButton").performClick()
        waitForIdle()
        snap("211_hint_sheet")
        assertTrue(onAllNodesWithTag("hintSheet").fetchSemanticsNodes().isNotEmpty())
        assertTrue(hasAnyText("İpucu: Mayın tarlası", "Hint: Minesweeper"))
        assertTrue(onAllNodesWithTag("guideMark").fetchSemanticsNodes().isEmpty(), "a hint does not point at the board")
    }

    @Test
    fun theGuidePointsAtTheNextMoveAndSaysTheStreakIsLeftAlone() = runComposeUiTest {
        openMinesFeed()
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        snap("212_guide_step")

        assertTrue(hasAnyText("Başlamak için ortadan bir hücre aç", "Open a cell in the middle to start"))
        assertTrue(hasAnyText("kazanma serini değiştirmez", "will not change your win streak"))
        assertTrue(onAllNodesWithTag("guideMark").fetchSemanticsNodes().size == 1, "one cell is ringed")

        // Playing on makes the step old.
        onAllNodesWithTag("mineCell")[27].performClick()
        waitForIdle()
        assertTrue(onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isEmpty(), "after a move the old step is gone")
        assertFalse(onAllNodesWithTag("guideMark").fetchSemanticsNodes().isNotEmpty())
    }
}
