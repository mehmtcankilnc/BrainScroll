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
    fun theGuideOnLightsOut() = runComposeUiTest {
        start(openFeed = false, games = listOf(GameKind.Lights))
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("lightCell").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        mainClock.advanceTimeBy(500)
        snap("213_guide_lights")
        assertTrue(onAllNodesWithTag("guideMark").fetchSemanticsNodes().size == 1, "one light is ringed")
    }

    @Test
    fun aLongGuideSentenceOnSudoku() = runComposeUiTest {
        start(openFeed = false, games = listOf(GameKind.Sudoku))
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("sudokuCell").fetchSemanticsNodes().size >= 81 }
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        mainClock.advanceTimeBy(500)
        snap("214_guide_sudoku")
    }

    @Test
    fun aLongGuideSentenceWithBigText() = runComposeUiTest {
        start(openFeed = false, games = listOf(GameKind.Sudoku), fontScale = 1.3f)
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("sudokuCell").fetchSemanticsNodes().size >= 81 }
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        mainClock.advanceTimeBy(500)
        snap("215_guide_sudoku_big_text")
    }

    @Test
    fun theGuideOnACellWithAllNineNotes() = runComposeUiTest {
        start(openFeed = false, games = listOf(GameKind.Sudoku))
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("sudokuCell").fetchSemanticsNodes().size >= 81 }
        // Which cell does the guide point at? Then take the guide away, write all nine notes in that cell, ask again.
        onAllNodesWithTag("guideButton")[0].performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("guideMark").fetchSemanticsNodes().isNotEmpty() }
        val target = onAllNodesWithTag("guideMark")[0].fetchSemanticsNode().boundsInRoot.center
        val index = onAllNodesWithTag("sudokuCell").fetchSemanticsNodes().indexOfFirst {
            kotlin.math.abs(it.boundsInRoot.center.x - target.x) < 2f && kotlin.math.abs(it.boundsInRoot.center.y - target.y) < 2f
        }
        assertTrue(index >= 0, "the cell of the guide was found")
        onAllNodesWithTag("guideClose")[0].performClick()
        waitForIdle()
        onAllNodesWithTag("sudokuNotes")[0].performClick()
        onAllNodesWithTag("sudokuCell")[index].performClick()
        for (digit in 1..9) onAllNodesWithTag("sudokuKey$digit")[0].performClick()
        onAllNodesWithTag("sudokuNotes")[0].performClick()
        onAllNodesWithTag("guideButton")[0].performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        mainClock.advanceTimeBy(300)
        snap("216_guide_cell_with_notes")
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
