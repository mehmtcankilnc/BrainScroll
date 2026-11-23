package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.lonenumber.LonePuzzle
import com.mehmtcan.brainscroll.game.lonenumber.LoneSolver
import com.mehmtcan.brainscroll.ui.lonenumber.LoneMarkKey
import com.mehmtcan.brainscroll.ui.lonenumber.LoneNumberKey
import com.mehmtcan.brainscroll.ui.lonenumber.LoneRepeatedKey
import com.mehmtcan.brainscroll.ui.lonenumber.LoneSizeKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The lone number page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class LoneNumberUiTest {

    private val games = listOf(GameKind.Word, GameKind.LoneNumber)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("loneCell").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show, and its cells come first.
    private fun ComposeUiTest.cells() = onAllNodesWithTag("loneCell").fetchSemanticsNodes()

    private fun ComposeUiTest.side() = onAllNodesWithTag("loneBoard").fetchSemanticsNodes().first().config[LoneSizeKey]

    private fun ComposeUiTest.puzzle(): LonePuzzle {
        val n = side()
        return LonePuzzle(n, cells().take(n * n).map { it.config[LoneNumberKey] })
    }

    private fun ComposeUiTest.marks() = cells().take(side() * side()).map { it.config[LoneMarkKey] }

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("loneCell")[cell].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Her sayı yalnız kaldı!", "Every number stands alone!")

    @Test
    fun theBoardShowsNumbersAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("420_lonenumber_start")
        assertTrue(hasAnyText("Yalnız Sayı", "Lone Number"))
        val puzzle = puzzle()
        assertTrue(puzzle.numbers.all { it in 1..puzzle.size })
        assertTrue(marks().all { it == 0 })
        assertTrue(!won())
    }

    @Test
    fun aTapShadesThenCirclesThenEmptiesACell() = runComposeUiTest {
        openBoard()
        val cell = LoneSolver.uniqueSolution(puzzle())!!.first()
        tap(cell)
        assertEquals(1, marks()[cell], "shaded")
        tap(cell)
        assertEquals(2, marks()[cell], "circled")
        tap(cell)
        assertEquals(0, marks()[cell], "empty again")
    }

    @Test
    fun aCellNextToAShadedOneCannotBeShaded() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val cell = LoneSolver.uniqueSolution(puzzle)!!.first()
        tap(cell)
        val next = puzzle.neighbours(cell).first()
        tap(next)
        assertEquals(0, marks()[next], "refused")
        snap("421_lonenumber_refused")
    }

    @Test
    fun aRepeatedNumberIsMarkedUntilItIsShaded() = runComposeUiTest {
        openBoard()
        val solution = LoneSolver.uniqueSolution(puzzle())!!
        val n = side()
        val before = cells().take(n * n).count { it.config[LoneRepeatedKey] }
        assertTrue(before > 0, "a board always has repeats at the start")
        solution.forEach { tap(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(won())
    }

    @Test
    fun shadingEveryCellOfTheAnswerWins() = runComposeUiTest {
        openBoard()
        LoneSolver.uniqueSolution(puzzle())!!.forEach { tap(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("422_lonenumber_won")
        assertTrue(won())
    }

    @Test
    fun theRestartButtonTakesEveryMarkOff() = runComposeUiTest {
        openBoard()
        val cell = LoneSolver.uniqueSolution(puzzle())!!.first()
        tap(cell)
        onAllNodesWithTag("loneRestart")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertTrue(marks().all { it == 0 })
    }
}
