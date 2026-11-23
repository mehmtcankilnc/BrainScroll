package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.fences.FencePuzzle
import com.mehmtcan.brainscroll.game.fences.FenceSolver
import com.mehmtcan.brainscroll.ui.fences.FenceMarkKey
import com.mehmtcan.brainscroll.ui.fences.FencePuzzleKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The fences page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class FencesUiTest {

    private val games = listOf(GameKind.Word, GameKind.Fences)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("fenceEdge").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show, and its edges come first.
    private fun ComposeUiTest.puzzle(): FencePuzzle =
        assertNotNull(FencePuzzle.decode(onAllNodesWithTag("fenceBoard").fetchSemanticsNodes().first().config[FencePuzzleKey]))

    private fun ComposeUiTest.marks() = onAllNodesWithTag("fenceEdge").fetchSemanticsNodes().take(puzzle().geometry.edges).map { it.config[FenceMarkKey] }

    private fun ComposeUiTest.tap(edge: Int) {
        onAllNodesWithTag("fenceEdge")[edge].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Halka kapandı!", "The loop is closed!")

    @Test
    fun theBoardShowsNumbersAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("460_fences_start")
        assertTrue(hasAnyText("Çitler", "Fences"))
        val puzzle = puzzle()
        assertTrue(puzzle.clueCount > 0)
        assertEquals(puzzle.geometry.edges, marks().size)
        assertTrue(marks().all { it == 0 })
        assertTrue(!won())
    }

    @Test
    fun aTapDrawsALineThenACrossThenEmptiesTheEdge() = runComposeUiTest {
        openBoard()
        tap(5)
        assertEquals(1, marks()[5], "line")
        tap(5)
        assertEquals(2, marks()[5], "cross")
        tap(5)
        assertEquals(0, marks()[5], "empty again")
    }

    @Test
    fun everyEdgeHasItsOwnTarget() = runComposeUiTest {
        openBoard()
        // Tapping each edge of the first board marks exactly that edge.
        val edges = puzzle().geometry.edges
        for (e in listOf(0, edges / 2, edges - 1)) {
            tap(e)
            assertEquals(1, marks()[e])
            assertEquals(1, marks().count { it == 1 }, "only edge $e is marked")
            tap(e)
            tap(e)
        }
    }

    @Test
    fun drawingTheLoopOfTheAnswerWins() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val answer = FenceSolver.solveByLogic(puzzle, true)!!
        answer.indices.filter { answer[it] == FenceSolver.LINE }.forEach { tap(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("461_fences_won")
        assertTrue(won())
    }

    @Test
    fun theRestartButtonTakesEveryMarkOff() = runComposeUiTest {
        openBoard()
        tap(3)
        onAllNodesWithTag("fenceRestart")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertTrue(marks().all { it == 0 })
    }
}
