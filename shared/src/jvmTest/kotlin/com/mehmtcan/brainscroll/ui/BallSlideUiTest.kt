package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeRight
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.ballslide.BallDir
import com.mehmtcan.brainscroll.game.ballslide.BallSlidePuzzle
import com.mehmtcan.brainscroll.game.ballslide.BallSlideSolver
import com.mehmtcan.brainscroll.ui.ballslide.BallSlideBallCell
import com.mehmtcan.brainscroll.ui.ballslide.BallSlidePainted
import com.mehmtcan.brainscroll.ui.ballslide.BallSlideWalls
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The ball slide page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class BallSlideUiTest {

    private val games = listOf(GameKind.Word, GameKind.BallSlide)

    private fun ComposeUiTest.openBoard(haptics: RecordingHaptics? = null, reduceMotion: Boolean? = null) {
        start(haptics = haptics, reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("ballSlideBoard").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.board() = onNodeWithTag("ballSlideBoard").fetchSemanticsNode().config

    private fun ComposeUiTest.ball() = board()[BallSlideBallCell]

    private fun ComposeUiTest.painted() = board()[BallSlidePainted]

    /** A swipe on the board in the direction [dir]. */
    private fun ComposeUiTest.press(dir: Int) {
        onNodeWithTag("ballSlideBoard").performTouchInput {
            when (dir) {
                BallDir.RIGHT -> swipeRight()
                BallDir.LEFT -> swipeLeft()
                BallDir.UP -> swipeUp()
                else -> swipeDown()
            }
        }
        mainClock.advanceTimeBy(800)
        waitForIdle()
    }

    /** The slides of a shortest way from where the ball is, worked out from the walls and the ball on the screen. */
    private fun ComposeUiTest.way(): List<Int> {
        val walls = board()[BallSlideWalls]
        val size = kotlin.math.sqrt(walls.size.toDouble()).toInt()
        val puzzle = BallSlidePuzzle(size, walls, ball())
        return assertNotNull(BallSlideSolver.shortest(puzzle, ball(), 1L shl ball()))
    }

    private fun ComposeUiTest.won() = hasAnyText("Bütün hücreler boyandı!", "Every cell is painted!")

    @Test
    fun theBoardShowsTheCountersAndTheBallStartsPainted() = runComposeUiTest {
        openBoard()
        snap("330_ballslide_start")
        assertTrue(hasAnyText("Kayan Top", "Ball Slide"))
        assertTrue(hasAnyText("Kaydırma 0", "Slides 0"))
        assertEquals(1, painted())
    }

    @Test
    fun aSwipeSlidesTheBallAndPaintsTheWay() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openBoard(haptics)
        val start = ball()
        val dir = way().first()
        haptics.kinds.clear()
        press(dir)
        assertTrue(ball() != start)
        assertTrue(painted() > 1)
        assertTrue(hasAnyText("Kaydırma 1", "Slides 1"))
        assertEquals(listOf(HapticKind.Light), haptics.kinds)
    }

    @Test
    fun undoTakesTheSlideBackAndRestartStartsAgain() = runComposeUiTest {
        openBoard()
        val start = ball()
        val dirs = way()
        press(dirs[0])
        press(dirs[1])
        onNodeWithTag("ballSlideUndo").performClick()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(painted() > 1)
        onNodeWithTag("ballSlideRestart").performClick()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertEquals(start, ball())
        assertEquals(1, painted())
    }

    @Test
    fun paintingEverythingWinsAndShowsTheSlidesAndTheTime() = runComposeUiTest {
        openBoard()
        val dirs = way()
        dirs.forEach { press(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("331_ballslide_solved")
        assertTrue(won())
        assertTrue(hasAnyText("${dirs.size} kaydırma", "${dirs.size} slides"))
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun theWinBuzzesOnlyAfterTheBallHasStopped() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openBoard(haptics)
        val dirs = way()
        dirs.dropLast(1).forEach { press(it) }
        haptics.kinds.clear()
        mainClock.autoAdvance = false
        onNodeWithTag("ballSlideBoard").performTouchInput {
            when (dirs.last()) {
                BallDir.RIGHT -> swipeRight()
                BallDir.LEFT -> swipeLeft()
                BallDir.UP -> swipeUp()
                else -> swipeDown()
            }
        }
        mainClock.advanceTimeBy(100)
        assertTrue(HapticKind.Success !in haptics.kinds, "no success buzz yet: ${haptics.kinds}")
        mainClock.advanceTimeBy(1_200)
        assertTrue(HapticKind.Success in haptics.kinds, "the success buzz comes after: ${haptics.kinds}")
    }

    @Test
    fun withReducedMotionTheWinShowsQuickly() = runComposeUiTest {
        openBoard(reduceMotion = true)
        way().forEach { press(it) }
        mainClock.advanceTimeBy(800)
        assertTrue(won())
    }

    @Test
    fun theBoardAndTheButtonsFitAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.BallSlide), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("332_ballslide_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("ballSlideBoard", "ballSlideUndo", "ballSlideRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }
}
