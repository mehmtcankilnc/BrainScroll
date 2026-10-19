package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The word puzzle at each level on the screen: 7, 6 or 5 rows, and the hard mode notice. */
@OptIn(ExperimentalTestApi::class)
class WordLevelsUiTest {

    private fun ComposeUiTest.openWord(level: String, fontScale: Float = 1f, screen: Pair<Int, Int> = 390 to 740) {
        start(games = listOf(GameKind.Word), openFeed = false, levelledGames = setOf(GameKind.Word), fontScale = fontScale, screen = screen)
        clickAny("Akışın", "Your feed")
        waitForIdle()
        onNodeWithTag("editorNext").performClick()
        waitForIdle()
        onNodeWithTag("level_$level").performClick()
        waitForIdle()
        onNodeWithTag("startFeed").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("inputRow").fetchSemanticsNodes().isNotEmpty() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    /** The rows of the first page on the screen: the rows of the grid, the one being typed included. */
    private fun ComposeUiTest.rows(): Int =
        (onAllNodesWithTag("wordleRow").fetchSemanticsNodes() + onAllNodesWithTag("inputRow").fetchSemanticsNodes())
            .count { it.boundsInRoot.width > 0f && it.boundsInRoot.top >= 0f && it.boundsInRoot.top < 740f }

    @Test
    fun theGridHasSevenSixOrFiveRowsByLevel() {
        for ((level, rows) in listOf("Easy" to 7, "Medium" to 6, "Hard" to 5)) runComposeUiTest {
            openWord(level)
            snap("350_word_$level")
            assertEquals(rows, rows(), level)
        }
    }

    @Test
    fun theEasyGridFitsAShortScreenWithBigText() = runComposeUiTest {
        openWord("Easy", fontScale = 1.5f, screen = 360 to 600)
        snap("351_word_easy_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (node in onAllNodesWithTag("wordleRow").fetchSemanticsNodes() + onAllNodesWithTag("inputRow").fetchSemanticsNodes()) {
            val b = node.boundsInRoot
            if (b.width <= 0f) continue
            assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "a row is inside the screen: $b against $root")
        }
    }
}
