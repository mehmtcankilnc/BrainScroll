package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.greaterless.GlPuzzle
import com.mehmtcan.brainscroll.game.greaterless.GlSolver
import com.mehmtcan.brainscroll.ui.greaterless.GlBrokenKey
import com.mehmtcan.brainscroll.ui.greaterless.GlGivenKey
import com.mehmtcan.brainscroll.ui.greaterless.GlKeyValueKey
import com.mehmtcan.brainscroll.ui.greaterless.GlPuzzleKey
import com.mehmtcan.brainscroll.ui.greaterless.GlValueKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The greater and less page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class GreaterLessUiTest {

    private val games = listOf(GameKind.Word, GameKind.GreaterLess)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("glCell").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show, and its cells come first.
    private fun ComposeUiTest.cells() = onAllNodesWithTag("glCell").fetchSemanticsNodes()

    private fun ComposeUiTest.puzzle(): GlPuzzle =
        assertNotNull(GlPuzzle.decode(onAllNodesWithTag("glBoard").fetchSemanticsNodes().first().config[GlPuzzleKey]))

    private fun ComposeUiTest.values() = cells().take(puzzle().size * puzzle().size).map { it.config[GlValueKey] }

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("glCell")[cell].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    /** Selects the cell, then taps the key of [value] under the board. */
    private fun ComposeUiTest.put(cell: Int, value: Int) {
        tap(cell)
        onAllNodesWithTag("glKey")[value - 1].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Bütün işaretler tuttu!", "Every sign holds!")

    @Test
    fun theBoardShowsGivenNumbersAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("450_greaterless_start")
        assertTrue(hasAnyText("Büyük-Küçük", "Greater Less"))
        val puzzle = puzzle()
        assertTrue(puzzle.givenCount > 0)
        assertTrue(puzzle.signCount > 0)
        assertTrue(values().any { it == 0 })
        assertTrue(!won())
        // What the board shows is what was made.
        for (cell in 0 until puzzle.size * puzzle.size) assertEquals(puzzle.given[cell], values()[cell].takeIf { puzzle.isGiven(cell) } ?: 0)
    }

    @Test
    fun theKeysHoldEveryNumberOfTheBoard() = runComposeUiTest {
        openBoard()
        val n = puzzle().size
        val keys = onAllNodesWithTag("glKey").fetchSemanticsNodes().take(n).map { it.config[GlKeyValueKey] }
        assertEquals((1..n).toList(), keys)
    }

    @Test
    fun aKeyPutsItsNumberInTheSelectedCellAndTheSameKeyEmptiesIt() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val cell = (0 until puzzle.size * puzzle.size).first { !puzzle.isGiven(it) }
        put(cell, 2)
        assertEquals(2, values()[cell])
        onAllNodesWithTag("glKey")[1].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertEquals(0, values()[cell], "the same number again empties it")
        onAllNodesWithTag("glKey")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertEquals(1, values()[cell], "the cell stays selected for the next number")
    }

    @Test
    fun aKeyDoesNothingWhileNoCellIsSelected() = runComposeUiTest {
        openBoard()
        val before = values()
        onAllNodesWithTag("glKey")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertEquals(before, values())
    }

    @Test
    fun aGivenCellDoesNotChange() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val cell = (0 until puzzle.size * puzzle.size).first { puzzle.isGiven(it) }
        val before = values()[cell]
        tap(cell)
        assertEquals(before, values()[cell])
        assertTrue(cells()[cell].config[GlGivenKey])
    }

    @Test
    fun aNumberThatBreaksARuleIsMarked() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val open = (0 until puzzle.size * puzzle.size).filter { !puzzle.isGiven(it) }
        // Two open cells of a row, if the board has them, with the same number: both are red.
        val pair = open.firstNotNullOfOrNull { a -> open.firstOrNull { b -> b > a && b / puzzle.size == a / puzzle.size }?.let { a to it } } ?: return@runComposeUiTest
        put(pair.first, 1)
        put(pair.second, 1)
        assertTrue(cells()[pair.first].config[GlBrokenKey] && cells()[pair.second].config[GlBrokenKey], "both cells are red")
        snap("451_greaterless_broken")
    }

    @Test
    fun fillingEveryCellOfTheAnswerWins() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val answer = GlSolver.uniqueSolution(puzzle)!!
        for (cell in 0 until puzzle.size * puzzle.size) {
            if (puzzle.isGiven(cell)) continue
            put(cell, answer[cell])
        }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("452_greaterless_won")
        assertTrue(won())
    }

    @Test
    fun theRestartButtonEmptiesEveryNumberThePlayerPut() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val cell = (0 until puzzle.size * puzzle.size).first { !puzzle.isGiven(it) }
        put(cell, 1)
        onAllNodesWithTag("glRestart")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertEquals(0, values()[cell])
    }
}
