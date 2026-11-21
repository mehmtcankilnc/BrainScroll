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
import com.mehmtcan.brainscroll.game.untangle.UNTANGLE_BOARD
import com.mehmtcan.brainscroll.game.untangle.UntangleEmbedder
import com.mehmtcan.brainscroll.ui.untangle.UntangleCrossingKey
import com.mehmtcan.brainscroll.ui.untangle.UntangleEdgesKey
import com.mehmtcan.brainscroll.ui.untangle.UntanglePositionsKey
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The untangle page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class UntangleUiTest {

    private val games = listOf(GameKind.Word, GameKind.Untangle)

    private fun ComposeUiTest.openBoard(reduceMotion: Boolean? = null) {
        start(reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("untangleBoard").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.board() = onAllNodesWithTag("untangleBoard")[0].fetchSemanticsNode().config

    private fun ComposeUiTest.positions() = board()[UntanglePositionsKey]

    private fun ComposeUiTest.crossing() = board()[UntangleCrossingKey]

    /** A drag of dot [node] to ([x], [y]) in units of the board, the way a finger does it. */
    private fun ComposeUiTest.drag(node: Int, x: Int, y: Int) {
        val from = positions().let { it[2 * node] to it[2 * node + 1] }
        onAllNodesWithTag("untangleBoard")[0].performTouchInput {
            val unit = width / UNTANGLE_BOARD.toFloat()
            down(Offset(from.first * unit, from.second * unit))
            moveTo(Offset((from.first + x) / 2f * unit, (from.second + y) / 2f * unit))
            moveTo(Offset(x * unit, y * unit))
            up()
        }
        mainClock.advanceTimeBy(500)
        waitForIdle()
    }

    private fun ComposeUiTest.drawing(): List<Int> {
        val edges = board()[UntangleEdgesKey].chunked(2).map { it[0] to it[1] }
        return assertNotNull(UntangleEmbedder.embed(positions().size / 2, edges, 1))
    }

    private fun ComposeUiTest.won() = hasAnyText("Düğüm çözüldü!", "Untangled!")

    @Test
    fun theBoardShowsTheCrossingsAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("370_untangle_start")
        assertTrue(hasAnyText("Düğüm Çöz", "Untangle"))
        assertTrue(crossing() > 0)
        assertTrue(!won())
    }

    @Test
    fun draggingADotMovesItAndTheRestartPutsItBack() = runComposeUiTest {
        openBoard()
        val start = positions()
        drag(0, 500, 500)
        assertEquals(500, positions()[0], "the dot is where the finger left it")
        assertEquals(500, positions()[1])
        onAllNodesWithTag("untangleRestart")[0].performClick()
        waitForIdle()
        assertEquals(start, positions())
    }

    @Test
    fun aTapAwayFromEveryDotDoesNothing() = runComposeUiTest {
        openBoard()
        val start = positions()
        // The middle of the board is empty at the start (the dots lie on a circle round it).
        onAllNodesWithTag("untangleBoard")[0].performTouchInput {
            down(center)
            moveTo(Offset(center.x + 40f, center.y + 40f))
            up()
        }
        mainClock.advanceTimeBy(300)
        waitForIdle()
        assertEquals(start, positions())
    }

    @Test
    fun movingTheDotsToADrawingWithNoCrossingWins() = runComposeUiTest {
        openBoard()
        val place = drawing()
        for (node in 0 until positions().size / 2) {
            if (won()) break
            drag(node, place[2 * node], place[2 * node + 1])
        }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("371_untangle_solved")
        assertTrue(won())
        assertEquals(0, crossing())
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun thinkingBeforeTheFirstDragCountsInTheTime() = runComposeUiTest {
        openBoard()
        Thread.sleep(1_300)
        val place = drawing()
        for (node in 0 until positions().size / 2) {
            if (won()) break
            drag(node, place[2 * node], place[2 * node + 1])
        }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(won())
        assertTrue(hasAnyText("00:01", "00:02", "00:03"), "the time includes the time spent looking")
    }

    @Test
    fun withReducedMotionTheWinShowsQuickly() = runComposeUiTest {
        openBoard(reduceMotion = true)
        val place = drawing()
        for (node in 0 until positions().size / 2) {
            if (won()) break
            drag(node, place[2 * node], place[2 * node + 1])
        }
        mainClock.advanceTimeBy(700)
        waitForIdle()
        assertTrue(won())
    }

    @Test
    fun theBoardAndTheButtonFitAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Untangle), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("372_untangle_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("untangleBoard", "untangleRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }

    /** [upper] ends where [lower] begins or higher: the two are not drawn over each other. */
    private fun ComposeUiTest.assertAbove(upper: String, lower: String) {
        val u = onAllNodesWithTag(upper).fetchSemanticsNodes().first().boundsInRoot
        val l = onAllNodesWithTag(lower).fetchSemanticsNodes().first().boundsInRoot
        assertTrue(u.bottom <= l.top + 1f, "$upper (to ${u.bottom}) is not over $lower (from ${l.top})")
    }

    private fun ComposeUiTest.assertInside(tag: String) {
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val nodes = onAllNodesWithTag(tag).fetchSemanticsNodes()
        assertTrue(nodes.isNotEmpty(), "$tag is there")
        val b = nodes.first().boundsInRoot
        assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
    }

    @Test
    fun theResultFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Untangle), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        val place = drawing()
        for (node in 0 until positions().size / 2) {
            if (won()) break
            drag(node, place[2 * node], place[2 * node + 1])
        }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(won())
        snap("373_untangle_small_solved")
        assertInside("untangleBoard")
        assertInside("untangleResultCard")
        assertAbove("untangleBoard", "untangleResultCard")
    }

    @Test
    fun aDotCanBePickedAndMovedWithTalkBackActions() = runComposeUiTest {
        openBoard()
        val actions = board().getOrNull(SemanticsActions.CustomActions)!!
        assertEquals(5, actions.size, "next dot and four moves")
        val before = positions()
        // The first dot, left or right whichever has room.
        val index = if (before[0] > UNTANGLE_BOARD / 2) 1 else 2
        runOnIdle { actions[index].action() }
        waitForIdle()
        assertTrue(positions()[0] != before[0], "the dot moved a step")
        // Pick the next dot and move it up or down.
        runOnIdle { actions[0].action() }
        waitForIdle()
        val mid = positions()
        runOnIdle { board().getOrNull(SemanticsActions.CustomActions)!![if (mid[3] > UNTANGLE_BOARD / 2) 3 else 4].action() }
        waitForIdle()
        assertTrue(positions()[3] != mid[3], "the second dot moved a step")
        assertEquals(mid[0], positions()[0], "the first dot stayed")
    }
}
