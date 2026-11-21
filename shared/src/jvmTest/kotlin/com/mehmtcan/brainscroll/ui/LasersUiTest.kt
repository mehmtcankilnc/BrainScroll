package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.lasers.LaserRules
import com.mehmtcan.brainscroll.ui.lasers.LaserLitKey
import com.mehmtcan.brainscroll.ui.lasers.LaserMirrorsKey
import com.mehmtcan.brainscroll.ui.lasers.LaserRowKey
import com.mehmtcan.brainscroll.ui.lasers.LaserTargetsKey
import com.mehmtcan.brainscroll.ui.lasers.laserCellCenter
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The laser mirrors page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class LasersUiTest {

    private val games = listOf(GameKind.Word, GameKind.Lasers)

    private fun ComposeUiTest.openBoard(reduceMotion: Boolean? = null) {
        start(reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("lasersBoard").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.board() = onAllNodesWithTag("lasersBoard")[0].fetchSemanticsNode().config

    private fun ComposeUiTest.mirrors() = board()[LaserMirrorsKey]

    private fun ComposeUiTest.size() = kotlin.math.sqrt(mirrors().size.toDouble()).toInt()

    /** A tap on the middle of [cell], the way a finger does it. */
    private fun ComposeUiTest.tap(cell: Int) {
        val n = size()
        onAllNodesWithTag("lasersBoard")[0].performTouchInput { click(laserCellCenter(cell, n, width.toFloat())) }
        mainClock.advanceTimeBy(700)
        waitForIdle()
    }

    /** The mirrors to turn, in cell numbers, to win the board in the fewest turns. */
    private fun ComposeUiTest.winning(): List<Int> {
        val m = mirrors()
        val targets = board()[LaserTargetsKey].toSet()
        val turns = LaserRules.fewestTurns(size(), board()[LaserRowKey], m, targets)!!
        return m.indices.filter { m[it] != 0 }.zip(turns).filter { it.second }.map { it.first }
    }

    private fun ComposeUiTest.won() = hasAnyText("Bütün hedefler yandı!", "Every target is lit!")

    @Test
    fun theBoardShowsTheLitTargetsAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("360_lasers_start")
        assertTrue(hasAnyText("Lazer Aynalar", "Laser Mirrors"))
        assertTrue(board()[LaserLitKey] < board()[LaserTargetsKey].size)
        assertTrue(!won())
    }

    @Test
    fun tappingAMirrorTurnsIt() = runComposeUiTest {
        openBoard()
        val before = mirrors()
        val cell = before.indexOfFirst { it != 0 }
        tap(cell)
        assertEquals(LaserRules.turned(before[cell]), mirrors()[cell])
        // An empty cell does nothing.
        val after = mirrors()
        tap(after.indexOfFirst { it == 0 })
        assertEquals(after, mirrors())
        onAllNodesWithTag("lasersRestart")[0].performClick()
        mainClock.advanceTimeBy(700)
        waitForIdle()
        assertEquals(before, mirrors())
    }

    @Test
    fun turningTheRightMirrorsWins() = runComposeUiTest {
        openBoard()
        winning().forEach { tap(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("361_lasers_solved")
        assertTrue(won())
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun thinkingBeforeTheFirstTapCountsInTheTime() = runComposeUiTest {
        openBoard()
        Thread.sleep(1_300)
        winning().forEach { tap(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(won())
        assertTrue(hasAnyText("00:01", "00:02"), "the time includes the time spent looking")
    }

    @Test
    fun withReducedMotionTheWinShowsQuickly() = runComposeUiTest {
        openBoard(reduceMotion = true)
        winning().forEach { tap(it) }
        mainClock.advanceTimeBy(700)
        waitForIdle()
        assertTrue(won())
    }

    @Test
    fun theBoardAndTheButtonFitAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Lasers), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("362_lasers_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("lasersBoard", "lasersRestart")) {
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
        start(games = listOf(GameKind.Lasers), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        winning().forEach { tap(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("363_lasers_small_solved")
        assertTrue(won())
        assertInside("lasersBoard")
        assertInside("lasersResultCard")
        assertAbove("lasersBoard", "lasersResultCard")
    }

    @Test
    fun everyMirrorCanBeTurnedWithATalkBackAction() = runComposeUiTest {
        openBoard()
        val actions = board().getOrNull(SemanticsActions.CustomActions)!!
        val before = mirrors()
        assertEquals(before.count { it != 0 }, actions.size, "one action for every mirror")
        val first = before.indexOfFirst { it != 0 }
        runOnIdle { actions[0].action() }
        mainClock.advanceTimeBy(700)
        waitForIdle()
        assertTrue(mirrors()[first] != before[first], "the first mirror turned")
    }
}
