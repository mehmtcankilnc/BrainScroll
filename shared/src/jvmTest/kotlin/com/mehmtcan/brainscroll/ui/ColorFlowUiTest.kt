package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.up
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.colorflow.FlowPuzzle
import com.mehmtcan.brainscroll.game.colorflow.FlowSolver
import com.mehmtcan.brainscroll.ui.colorflow.FlowBoardSize
import com.mehmtcan.brainscroll.ui.colorflow.FlowCovered
import com.mehmtcan.brainscroll.ui.colorflow.FlowDotCells
import com.mehmtcan.brainscroll.ui.colorflow.FlowJoinedCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The color flow page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class ColorFlowUiTest {

    private val games = listOf(GameKind.Word, GameKind.ColorFlow)

    private fun ComposeUiTest.openBoard(reduceMotion: Boolean? = null) {
        start(reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("colorFlowBoard").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.board() = onNodeWithTag("colorFlowBoard").fetchSemanticsNode().config

    private fun ComposeUiTest.puzzle(): FlowPuzzle {
        val cells = board()[FlowDotCells]
        return FlowPuzzle(board()[FlowBoardSize], cells.chunked(2).map { it[0] to it[1] })
    }

    /** A finger put down on the first cell of [path] and dragged through the others. */
    private fun ComposeUiTest.draw(path: List<Int>) {
        val n = board()[FlowBoardSize]
        onNodeWithTag("colorFlowBoard").performTouchInput {
            val cell = width / n.toFloat()
            fun at(c: Int) = Offset((c % n + 0.5f) * cell, (c / n + 0.5f) * cell)
            down(at(path.first()))
            path.drop(1).forEach { moveTo(at(it)) }
            up()
        }
        mainClock.advanceTimeBy(300)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Bütün renkler birleşti!", "Every color is joined!")

    @Test
    fun theBoardShowsTheCountersAndNothingIsDrawnYet() = runComposeUiTest {
        openBoard()
        snap("340_colorflow_start")
        assertTrue(hasAnyText("Renk Akışı", "Color Flow"))
        assertEquals(0, board()[FlowCovered])
        assertEquals(0, board()[FlowJoinedCount])
    }

    @Test
    fun draggingFromADotDrawsItsPath() = runComposeUiTest {
        openBoard()
        val way = assertNotNull(FlowSolver.solve(puzzle()))
        draw(way[0])
        assertEquals(way[0].size, board()[FlowCovered])
        assertEquals(1, board()[FlowJoinedCount])
        onNodeWithTag("colorFlowRestart").performClick()
        waitForIdle()
        assertEquals(0, board()[FlowCovered])
    }

    @Test
    fun joiningEveryColorAndFillingTheBoardWins() = runComposeUiTest {
        openBoard()
        val way = assertNotNull(FlowSolver.solve(puzzle()))
        way.forEach { draw(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("341_colorflow_solved")
        assertTrue(won())
        assertTrue(hasAnyText("${way.size} çizgi", "${way.size} strokes"))
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun theBoardAndTheButtonFitAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.ColorFlow), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("342_colorflow_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("colorFlowBoard", "colorFlowRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }
}
