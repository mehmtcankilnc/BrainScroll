package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.lamps.LampPuzzle
import com.mehmtcan.brainscroll.game.lamps.LampSolver
import com.mehmtcan.brainscroll.ui.lamps.LampCellsKey
import com.mehmtcan.brainscroll.ui.lamps.LampLitKey
import com.mehmtcan.brainscroll.ui.lamps.LampMarkKey
import com.mehmtcan.brainscroll.ui.lamps.LampSizeKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The lamps page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class LampsUiTest {

    private val games = listOf(GameKind.Word, GameKind.Lamps)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("lampCell").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show, and its cells come first.
    private fun ComposeUiTest.cells() = onAllNodesWithTag("lampCell").fetchSemanticsNodes()

    private fun ComposeUiTest.board() = onAllNodesWithTag("lampBoard").fetchSemanticsNodes().first().config

    private fun ComposeUiTest.puzzle() = LampPuzzle(board()[LampSizeKey], board()[LampCellsKey])

    private fun ComposeUiTest.marks() = cells().take(board()[LampSizeKey] * board()[LampSizeKey]).map { it.config[LampMarkKey] }

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("lampCell")[cell].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Bütün kareler aydınlandı!", "Every cell is lit!")

    @Test
    fun theBoardShowsWallsAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("440_lamps_start")
        assertTrue(hasAnyText("Ampuller", "Light Up"))
        val puzzle = puzzle()
        assertTrue(puzzle.wallCount > 0)
        assertTrue(marks().all { it == 0 })
        assertTrue(!won())
    }

    @Test
    fun aTapPutsALampThenADotThenEmptiesACell() = runComposeUiTest {
        openBoard()
        val cell = puzzle().whiteCells.first()
        tap(cell)
        assertEquals(1, marks()[cell], "lamp")
        tap(cell)
        assertEquals(2, marks()[cell], "dot")
        tap(cell)
        assertEquals(0, marks()[cell], "empty again")
    }

    @Test
    fun aLampLightsTheCellsItSees() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val cell = puzzle.whiteCells.first { puzzle.seenFrom(it).isNotEmpty() }
        assertTrue(!cells()[cell].config[LampLitKey])
        tap(cell)
        assertTrue(cells()[cell].config[LampLitKey], "its own cell is lit")
        for (seen in puzzle.seenFrom(cell)) assertTrue(cells()[seen].config[LampLitKey], "cell $seen is lit")
        snap("441_lamps_one_lamp")
    }

    @Test
    fun aTapOnAWallDoesNothing() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val wall = (0 until puzzle.size * puzzle.size).first { puzzle.isWall(it) }
        tap(wall)
        assertTrue(marks().all { it == 0 })
    }

    @Test
    fun puttingEveryLampOfTheAnswerWins() = runComposeUiTest {
        openBoard()
        val answer = LampSolver.uniqueSolution(puzzle())!!
        answer.indices.filter { answer[it] }.forEach { tap(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("442_lamps_won")
        assertTrue(won())
    }

    @Test
    fun theRestartButtonTakesEveryMarkOff() = runComposeUiTest {
        openBoard()
        val cell = puzzle().whiteCells.first()
        tap(cell)
        onAllNodesWithTag("lampRestart")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertTrue(marks().all { it == 0 })
    }
}
