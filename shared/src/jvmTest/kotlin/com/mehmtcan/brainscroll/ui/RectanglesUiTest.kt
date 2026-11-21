package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.up
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.rectangles.Rect
import com.mehmtcan.brainscroll.game.rectangles.RectPuzzle
import com.mehmtcan.brainscroll.game.rectangles.RectSolver
import com.mehmtcan.brainscroll.ui.rectangles.RectNumbersKey
import com.mehmtcan.brainscroll.ui.rectangles.RectPlacedKey
import com.mehmtcan.brainscroll.ui.rectangles.RectSizeKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The rectangles page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class RectanglesUiTest {

    private val games = listOf(GameKind.Word, GameKind.Rectangles)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("rectanglesBoard").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show.
    private fun ComposeUiTest.board() = onAllNodesWithTag("rectanglesBoard").fetchSemanticsNodes().first().config

    private fun ComposeUiTest.puzzle() = RectPuzzle(board()[RectSizeKey], board()[RectNumbersKey])

    private fun ComposeUiTest.placed(): List<Rect> = board()[RectPlacedKey].chunked(4).map { Rect(it[0], it[1], it[2], it[3]) }

    private fun ComposeUiTest.solution() = RectSolver.uniqueSolution(puzzle())!!

    private fun ComposeUiTest.won() = hasAnyText("Bütün hücreler dolu!", "Every cell is covered!")

    /** The middle of a cell, the way a finger touches it. */
    private fun cellCenter(column: Int, row: Int, n: Int, width: Float) = Offset((column + 0.5f) * width / n, (row + 0.5f) * width / n)

    /** A drag from the middle of the first cell of [rect] to the middle of its last, the way a finger draws it. */
    private fun ComposeUiTest.draw(rect: Rect) {
        val n = puzzle().size
        onAllNodesWithTag("rectanglesBoard")[0].performTouchInput {
            down(cellCenter(rect.x, rect.y, n, width.toFloat()))
            moveTo(cellCenter(rect.x + rect.w - 1, rect.y + rect.h - 1, n, width.toFloat()))
            up()
        }
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.tap(column: Int, row: Int) {
        val n = puzzle().size
        onAllNodesWithTag("rectanglesBoard")[0].performTouchInput { click(cellCenter(column, row, n, width.toFloat())) }
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    @Test
    fun theBoardShowsNumbersAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("400_rectangles_start")
        assertTrue(hasAnyText("Dikdörtgenler", "Rectangles"))
        val puzzle = puzzle()
        assertEquals(puzzle.size * puzzle.size, puzzle.numbers.sum())
        assertEquals(emptyList(), placed())
        assertTrue(!won())
    }

    @Test
    fun draggingAcrossTheCellsOfARightRectangleLaysIt() = runComposeUiTest {
        openBoard()
        val rect = solution().first { it.area > 1 }
        draw(rect)
        assertEquals(listOf(rect), placed())
    }

    @Test
    fun aDragThatIsNotAllowedLaysNothing() = runComposeUiTest {
        openBoard()
        val n = puzzle().size
        draw(Rect(0, 0, n, n))
        assertEquals(emptyList(), placed(), "the whole board holds every number")
        snap("401_rectangles_rejected")
    }

    @Test
    fun aTapOnARectangleTakesItOff() = runComposeUiTest {
        openBoard()
        val rect = solution().first { it.area > 1 }
        draw(rect)
        tap(rect.x + rect.w - 1, rect.y + rect.h - 1)
        assertEquals(emptyList(), placed())
    }

    @Test
    fun aTapOnAFreeOneFillsItAtOnce() = runComposeUiTest {
        // Boards are random: look through the first pages' boards for one that has a 1 (the easy ones nearly always do).
        openBoard()
        val one = puzzle().numbers.indexOf(1)
        if (one < 0) return@runComposeUiTest
        val n = puzzle().size
        tap(one % n, one / n)
        assertEquals(listOf(Rect(one % n, one / n, 1, 1)), placed())
        tap(one % n, one / n)
        assertEquals(emptyList(), placed(), "a second tap takes it off again")
    }

    @Test
    fun aTapOnAFreeCellThatIsNotAOneDoesNothing() = runComposeUiTest {
        openBoard()
        val cell = puzzle().numbers.indexOfFirst { it > 1 }
        val n = puzzle().size
        tap(cell % n, cell / n)
        assertEquals(emptyList(), placed())
    }

    @Test
    fun drawingEveryRectangleOfTheAnswerWins() = runComposeUiTest {
        openBoard()
        solution().forEach { draw(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("402_rectangles_won")
        assertTrue(won())
        assertEquals(puzzle().clueCells.size, placed().size)
    }

    @Test
    fun theBoardCanBeDrawnWithTalkBackActionsToo() = runComposeUiTest {
        openBoard()
        val node = onAllNodesWithTag("rectanglesBoard")[0]
        fun actions() = node.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions)!!
        assertEquals(6, actions().size, "next and previous cell, next and previous number, set corner, take off")
        val n = puzzle().size
        val rect = solution().first { it.area > 1 }
        var at = 0
        fun goTo(cell: Int) {
            while (at != cell) {
                runOnIdle { actions()[0].action() }
                waitForIdle()
                at = (at + 1) % (n * n)
            }
        }
        goTo(rect.y * n + rect.x)
        runOnIdle { actions()[4].action() }
        waitForIdle()
        goTo((rect.y + rect.h - 1) * n + rect.x + rect.w - 1)
        runOnIdle { actions()[4].action() }
        waitForIdle()
        assertEquals(listOf(rect), placed())
        // And the rectangle can be taken off from the cell it covers.
        runOnIdle { actions()[5].action() }
        waitForIdle()
        assertEquals(emptyList(), placed())
    }

    @Test
    fun theBoardAndTheResultFitAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Rectangles), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("403_rectangles_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        fun inside(tag: String) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        inside("rectanglesBoard")
        solution().forEach { draw(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("404_rectangles_small_won")
        assertTrue(won())
        inside("rectanglesResultCard")
        val boardBottom = onAllNodesWithTag("rectanglesBoard").fetchSemanticsNodes().first().boundsInRoot.bottom
        val cardTop = onAllNodesWithTag("rectanglesResultCard").fetchSemanticsNodes().first().boundsInRoot.top
        assertTrue(boardBottom <= cardTop + 1f, "the board and the result card do not overlap: $boardBottom, $cardTop")
    }
}
