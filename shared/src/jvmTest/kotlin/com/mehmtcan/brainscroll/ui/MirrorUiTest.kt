package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.mirror.MirrorAxis
import com.mehmtcan.brainscroll.game.mirror.MirrorPuzzle
import com.mehmtcan.brainscroll.ui.mirror.MirrorGivenKey
import com.mehmtcan.brainscroll.ui.mirror.MirrorPaintedKey
import com.mehmtcan.brainscroll.ui.mirror.MirrorSizeKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The mirror page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class MirrorUiTest {

    private val games = listOf(GameKind.Word, GameKind.Mirror)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("mirrorCell").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show, and its cells come first.
    private fun ComposeUiTest.cells() = onAllNodesWithTag("mirrorCell").fetchSemanticsNodes()

    private fun ComposeUiTest.side() = onAllNodesWithTag("mirrorBoard").fetchSemanticsNodes().first().config[MirrorSizeKey]

    private fun ComposeUiTest.painted() = cells().take(side() * side()).map { it.config[MirrorPaintedKey] == 1 }

    private fun ComposeUiTest.given() = cells().take(side() * side()).map { it.config[MirrorGivenKey] }

    /** The board as it stands on the screen: the axis is the one whose given half is the one the cells say. */
    private fun ComposeUiTest.puzzle(): MirrorPuzzle {
        val n = side()
        val given = given()
        val axis = MirrorAxis.entries.first { a -> MirrorPuzzle.shape(n, a).let { p -> given.indices.all { p.isGiven(it) == given[it] } } }
        val marks = painted()
        return MirrorPuzzle(n, axis, marks.mapIndexed { i, on -> on && given[i] })
    }

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("mirrorCell")[cell].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Tam simetri!", "Perfect symmetry!")

    @Test
    fun theBoardShowsTheAxisAndTheReadyHalfAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("430_mirror_start")
        assertTrue(hasAnyText("Ayna", "Mirror"))
        assertTrue(onAllNodesWithTag("mirrorAxis").fetchSemanticsNodes().isNotEmpty(), "the axis is drawn")
        val puzzle = puzzle()
        assertEquals(puzzle.size * puzzle.size - puzzle.fillCells.size, given().count { it }, "the cells the page calls ready are the given half")
        assertTrue(painted().indices.none { !given()[it] && painted()[it] }, "nothing is painted in the half to fill")
        assertTrue(!won())
    }

    @Test
    fun aTapPaintsThenEmptiesACellOfTheHalfToFill() = runComposeUiTest {
        openBoard()
        val cell = puzzle().fillCells.first()
        tap(cell)
        assertTrue(painted()[cell])
        tap(cell)
        assertTrue(!painted()[cell])
    }

    @Test
    fun aCellOfTheReadyHalfCannotBeChanged() = runComposeUiTest {
        openBoard()
        val cell = given().indexOfFirst { it }
        val before = painted()[cell]
        tap(cell)
        assertEquals(before, painted()[cell])
    }

    @Test
    fun paintingTheMirrorImageWins() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        puzzle.fillCells.filter { puzzle.target(it) }.forEach { tap(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("431_mirror_won")
        assertTrue(won())
    }

    @Test
    fun theRestartButtonEmptiesTheHalfToFill() = runComposeUiTest {
        openBoard()
        val puzzle = puzzle()
        tap(puzzle.fillCells.first())
        onAllNodesWithTag("mirrorRestart")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertTrue(puzzle.fillCells.none { painted()[it] })
    }
}
