package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.crosssum.CrossPuzzle
import com.mehmtcan.brainscroll.ui.crosssum.CrossBrokenKey
import com.mehmtcan.brainscroll.ui.crosssum.CrossGivenKey
import com.mehmtcan.brainscroll.ui.crosssum.CrossKeyValueKey
import com.mehmtcan.brainscroll.ui.crosssum.CrossPuzzleKey
import com.mehmtcan.brainscroll.ui.crosssum.CrossValueKey
import com.mehmtcan.brainscroll.ui.howto.CrossSumScenes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The cross sums page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class CrossSumUiTest {

    private val games = listOf(GameKind.Word, GameKind.CrossSum)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("crossCell").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.puzzle(): CrossPuzzle =
        CrossPuzzle.decode(onAllNodesWithTag("crossBoard").fetchSemanticsNodes().first().config[CrossPuzzleKey])!!

    // The page next to the one on show may be there too: the cells of the first board come first, one for every white cell.
    private fun ComposeUiTest.cells() = onAllNodesWithTag("crossCell").fetchSemanticsNodes().take(puzzle().whiteCells.size)

    private fun ComposeUiTest.values() = cells().map { it.config[CrossValueKey] }

    /** Taps the white cell number [index] (in the order of the white cells) and then the key of [digit]. */
    private fun ComposeUiTest.put(index: Int, digit: Int) {
        onAllNodesWithTag("crossCell")[index].performClick()
        waitForIdle()
        pressKey(digit)
    }

    /** Taps the key of [digit] (the cell that is selected, if any, gets it). */
    private fun ComposeUiTest.pressKey(digit: Int) {
        val key = onAllNodesWithTag("crossKey").fetchSemanticsNodes().take(9).indexOfFirst { it.config[CrossKeyValueKey] == digit }
        onAllNodesWithTag("crossKey")[key].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Bütün toplamlar tuttu!", "Every sum adds up!")

    @Test
    fun theBoardShowsTheSumsTheGivenDigitsAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("450_crosssum_start")
        assertTrue(hasAnyText("Çapraz Toplam", "Cross Sums"))
        val puzzle = puzzle()
        assertEquals(puzzle.whiteCells.size, cells().size)
        assertEquals(puzzle.whiteCells.map { if (puzzle.isGiven(it)) puzzle.solution[it] else 0 }, values(), "the given digits are on the board and nothing else")
        assertEquals(puzzle.whiteCells.map { puzzle.isGiven(it) }, cells().map { it.config[CrossGivenKey] })
        assertTrue(!won())
    }

    @Test
    fun aKeyDoesNothingUntilACellIsSelectedThenPutsItsDigitAndTheSameKeyEmptiesTheCell() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val open = puzzle.whiteCells.indexOfFirst { !puzzle.isGiven(it) }
        val before = values()
        val key = onAllNodesWithTag("crossKey").fetchSemanticsNodes().take(9).indexOfFirst { it.config[CrossKeyValueKey] == 5 }
        onAllNodesWithTag("crossKey")[key].performClick()
        waitForIdle()
        assertEquals(before, values(), "no cell was selected")
        put(open, 5)
        assertEquals(5, values()[open])
        pressKey(5)
        assertEquals(0, values()[open], "the same key empties the cell")
    }

    @Test
    fun aGivenCellCannotBeChanged() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val given = puzzle.whiteCells.indexOfFirst { puzzle.isGiven(it) }
        val before = values()[given]
        put(given, 3)
        assertEquals(before, values()[given])
    }

    @Test
    fun aDigitTwiceInABlockIsMarkedRed() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val run = puzzle.runs.first { run -> run.cells.count { !puzzle.isGiven(it) } >= 2 }
        val open = run.cells.filter { !puzzle.isGiven(it) }.take(2).map { puzzle.whiteCells.indexOf(it) }
        // A digit that is not in the block yet, so the only rule broken is "twice".
        val digit = (1..9).first { d -> run.cells.none { puzzle.isGiven(it) && puzzle.solution[it] == d } }
        put(open[0], digit)
        put(open[1], digit)
        snap("451_crosssum_conflict")
        assertTrue(cells()[open[0]].config[CrossBrokenKey] && cells()[open[1]].config[CrossBrokenKey])
    }

    @Test
    fun puttingTheAnswerWins() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        puzzle.whiteCells.forEachIndexed { i, cell -> if (!puzzle.isGiven(cell)) put(i, puzzle.solution[cell]) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("452_crosssum_won")
        assertTrue(won())
    }

    @Test
    fun theRestartButtonTakesOutWhatWasPut() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        val open = puzzle.whiteCells.indexOfFirst { !puzzle.isGiven(it) }
        put(open, 4)
        onAllNodesWithTag("crossRestart")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertEquals(0, values()[open])
    }

    @Test
    fun theScenesOfTheCardAreRealBoardsWhoseSumsAreRight() {
        // The dark cells of the card say the row sums 17, 23, 7 and the column sums 18, 20, 9 of 9 7 1 / 8 9 6 / 1 4 2.
        for ((name, scene) in CrossSumScenes) {
            val sums = scene.cells.filter { it.text.contains('|') }.flatMap { it.text.split('|') }.filter { it.isNotEmpty() }.map { it.toInt() }
            assertEquals(listOf(18, 20, 9, 17, 23, 7), sums, name)
        }
    }
}
