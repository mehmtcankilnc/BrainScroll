package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.zerosones.BinPuzzle
import com.mehmtcan.brainscroll.game.zerosones.BinSolver
import com.mehmtcan.brainscroll.ui.zerosones.BinBrokenKey
import com.mehmtcan.brainscroll.ui.zerosones.BinGivenKey
import com.mehmtcan.brainscroll.ui.zerosones.BinSizeKey
import com.mehmtcan.brainscroll.ui.zerosones.BinValueKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The zeros and ones page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class ZerosOnesUiTest {

    private val games = listOf(GameKind.Word, GameKind.ZerosOnes)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("binCell").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show, and its cells come first.
    private fun ComposeUiTest.cells() = onAllNodesWithTag("binCell").fetchSemanticsNodes()

    private fun ComposeUiTest.side() = onAllNodesWithTag("binBoard").fetchSemanticsNodes().first().config[BinSizeKey]

    /** The board as it was given: the cells that are given hold their digit, the rest are empty. */
    private fun ComposeUiTest.puzzle(): BinPuzzle {
        val n = side()
        return BinPuzzle(n, cells().take(n * n).map { if (it.config[BinGivenKey]) it.config[BinValueKey] else -1 })
    }

    private fun ComposeUiTest.values() = cells().take(side() * side()).map { it.config[BinValueKey] }

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("binCell")[cell].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Tahta dengelendi!", "The board is balanced!")

    @Test
    fun theBoardShowsGivenDigitsAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("430_zerosones_start")
        assertTrue(hasAnyText("Sıfır-Bir", "Zeros & Ones"))
        val puzzle = puzzle()
        assertTrue(puzzle.givenCount > 0)
        assertTrue(values().any { it == -1 })
        assertTrue(!won())
    }

    @Test
    fun aTapPutsZeroThenOneThenEmptiesACell() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val cell = (0 until puzzle.size * puzzle.size).first { !puzzle.isGiven(it) }
        tap(cell)
        assertEquals(0, values()[cell])
        tap(cell)
        assertEquals(1, values()[cell])
        tap(cell)
        assertEquals(-1, values()[cell])
    }

    @Test
    fun aGivenCellDoesNotChange() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val cell = (0 until puzzle.size * puzzle.size).first { puzzle.isGiven(it) }
        val before = values()[cell]
        tap(cell)
        assertEquals(before, values()[cell])
    }

    @Test
    fun aDigitThatBreaksARuleIsMarked() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val n = puzzle.size
        val answer = BinSolver.uniqueSolution(puzzle)!!
        // Three open cells side by side in a row, if the board has them: three of the same digit break a rule.
        val start = (0 until n * n).firstOrNull { c -> c % n <= n - 3 && (0..2).all { !puzzle.isGiven(c + it) } } ?: return@runComposeUiTest
        for (k in 0..2) tap(start + k)
        assertTrue((0..2).all { cells()[start + it].config[BinBrokenKey] }, "the three cells are red")
        snap("431_zerosones_broken")
        assertTrue(answer.size == n * n)
    }

    @Test
    fun fillingEveryCellOfTheAnswerWins() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val answer = BinSolver.uniqueSolution(puzzle)!!
        for (cell in 0 until puzzle.size * puzzle.size) {
            if (puzzle.isGiven(cell)) continue
            tap(cell)
            if (answer[cell] == 1) tap(cell)
        }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("432_zerosones_won")
        assertTrue(won())
    }

    @Test
    fun theRestartButtonEmptiesEveryDigitThePlayerPut() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val cell = (0 until puzzle.size * puzzle.size).first { !puzzle.isGiven(it) }
        tap(cell)
        onAllNodesWithTag("binRestart")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertEquals(-1, values()[cell])
    }
}
