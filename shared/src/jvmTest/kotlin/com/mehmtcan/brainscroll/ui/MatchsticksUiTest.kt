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
import com.mehmtcan.brainscroll.game.matchsticks.MatchMove
import com.mehmtcan.brainscroll.game.matchsticks.MatchRules
import com.mehmtcan.brainscroll.ui.matchsticks.MatchSticksCells
import com.mehmtcan.brainscroll.ui.matchsticks.matchSlotTapPoint
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The matchstick page in the feed, driven through the real screen. The sum is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class MatchsticksUiTest {

    private val games = listOf(GameKind.Word, GameKind.Matchsticks)

    private fun ComposeUiTest.openBoard(reduceMotion: Boolean? = null) {
        start(reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("matchsticksBoard").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.cells(): List<Int> = onAllNodesWithTag("matchsticksBoard")[0].fetchSemanticsNode().config[MatchSticksCells]

    /** A tap on the stick (or empty place) [slot], the way a finger does it. */
    private fun ComposeUiTest.tapSlot(slot: com.mehmtcan.brainscroll.game.matchsticks.MatchSlot) {
        val n = cells().size
        onAllNodesWithTag("matchsticksBoard")[0].performTouchInput { click(matchSlotTapPoint(slot, n, width.toFloat())) }
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.play(move: MatchMove) {
        tapSlot(move.from)
        tapSlot(move.to)
        mainClock.advanceTimeBy(600)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Eşitlik doğru!", "The sum is right!")

    private fun ComposeUiTest.wrongMove(): MatchMove {
        val start = cells()
        return MatchRules.moves(start).first { !MatchRules.isTrue(MatchRules.apply(start, it)) }
    }

    @Test
    fun theBoardShowsAWrongSumOnABoard() = runComposeUiTest {
        openBoard()
        snap("350_matchsticks_start")
        assertTrue(hasAnyText("Kibrit Çöpü", "Matchsticks"))
        assertTrue(!MatchRules.isTrue(cells()), "the sum on the board is wrong")
    }

    @Test
    fun movingTheRightStickWins() = runComposeUiTest {
        openBoard()
        val win = MatchRules.winningMoves(cells()).single()
        play(win)
        snap("351_matchsticks_solved")
        assertTrue(won())
        assertTrue(hasAnyText("00:00"), "the result shows the time")
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun aWrongMoveHasToBePutBackAndThenTheRightOneWins() = runComposeUiTest {
        openBoard()
        val start = cells()
        play(wrongMove())
        assertTrue(cells() != start, "the stick moved")
        assertTrue(hasAnyText("Eşitlik hâlâ yanlış. Çöpü geri al ve yeniden dene", "Still wrong. Put the stick back and try again"))
        // Until it is put back a tap on the board does nothing.
        val whileWrong = cells()
        play(MatchRules.moves(cells()).first())
        assertEquals(whileWrong, cells(), "no other move until the stick is put back")
        onAllNodesWithTag("matchsticksUndo")[0].performClick()
        mainClock.advanceTimeBy(600)
        waitForIdle()
        assertEquals(start, cells())
        play(MatchRules.winningMoves(start).single())
        assertTrue(won())
    }

    @Test
    fun thinkingBeforeTheOnlyMoveCountsInTheTime() = runComposeUiTest {
        openBoard()
        // The player looks at the sum for a while and then makes the one move that solves it: that is not "no time".
        Thread.sleep(1_300)
        play(MatchRules.winningMoves(cells()).single())
        assertTrue(won())
        assertTrue(hasAnyText("00:01", "00:02"), "the time includes the time spent looking")
    }

    @Test
    fun withReducedMotionTheMoveShowsAtOnce() = runComposeUiTest {
        openBoard(reduceMotion = true)
        val win = MatchRules.winningMoves(cells()).single()
        tapSlot(win.from)
        tapSlot(win.to)
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(won())
    }

    @Test
    fun theBoardAndTheButtonFitAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Matchsticks), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("352_matchsticks_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("matchsticksBoard", "matchsticksUndo")) {
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
        start(games = listOf(GameKind.Matchsticks), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        play(MatchRules.winningMoves(cells()).single())
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(won())
        snap("353_matchsticks_small_solved")
        assertInside("matchsticksBoard")
        assertInside("matchsticksResultCard")
        assertAbove("matchsticksBoard", "matchsticksResultCard")
    }

    @Test
    fun aStickCanBeMovedWithTheTalkBackPlaceCursor() = runComposeUiTest {
        openBoard()
        val node = onAllNodesWithTag("matchsticksBoard")[0]
        fun actions() = node.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions)!!
        assertEquals(3, actions().size, "next, previous and touch")
        val start = cells()
        val slots = MatchRules.slotsOf(start.size)
        val move = wrongMove()
        var at = 0
        fun goTo(slot: com.mehmtcan.brainscroll.game.matchsticks.MatchSlot) {
            val to = slots.indexOf(slot)
            while (at != to) {
                runOnIdle { actions()[0].action() }
                waitForIdle()
                at = (at + 1) % slots.size
            }
            runOnIdle { actions()[2].action() }
            waitForIdle()
        }
        goTo(move.from)
        goTo(move.to)
        mainClock.advanceTimeBy(600)
        waitForIdle()
        assertEquals(MatchRules.apply(start, move), cells(), "the stick moved")
    }
}
