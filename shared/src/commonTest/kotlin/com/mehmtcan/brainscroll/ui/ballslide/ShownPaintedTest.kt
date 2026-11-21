package com.mehmtcan.brainscroll.ui.ballslide

import com.mehmtcan.brainscroll.game.ballslide.BallDir
import com.mehmtcan.brainscroll.game.ballslide.BallSlideGame
import com.mehmtcan.brainscroll.game.ballslide.BallSlidePuzzle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Which cells the board draws painted while the ball is going. */
class ShownPaintedTest {
    // 3 by 3, wall in the middle, ball in the top left corner.
    private val puzzle = BallSlidePuzzle(3, List(9) { it == 4 }, start = 0)

    @Test
    fun aCellThatIsPaintedAlreadyStaysPaintedWhileTheBallGoesOverItAgain() {
        val game = BallSlideGame(puzzle)
        game.slide(BallDir.RIGHT) // paints 1 and 2
        game.slide(BallDir.LEFT) // goes back over 1 and 0, which are painted already
        val state = game.state
        assertTrue(state.lastPath.containsAll(listOf(1, 0)))
        // At every moment of the slide, before the ball has reached any of them:
        for (passed in listOf(0f, 0.5f, 1f, 1.5f)) {
            for (cell in listOf(0, 1, 2)) {
                assertTrue(shownPainted(state, cell, sliding = true, passed = passed, undoing = false, cleared = 0f), "cell $cell at $passed")
            }
        }
    }

    @Test
    fun aCellThatTheSlidePaintsForTheFirstTimeWaitsForTheBall() {
        val game = BallSlideGame(puzzle)
        game.slide(BallDir.RIGHT) // paints 1 and 2, the ball ends on 2
        val state = game.state
        assertFalse(shownPainted(state, 1, sliding = true, passed = 0f, undoing = false, cleared = 0f), "not reached yet")
        assertFalse(shownPainted(state, 2, sliding = true, passed = 1f, undoing = false, cleared = 0f), "not reached yet")
        assertTrue(shownPainted(state, 1, sliding = true, passed = 1.5f, undoing = false, cleared = 0f), "the ball has passed it")
        assertTrue(shownPainted(state, 2, sliding = true, passed = 2f, undoing = false, cleared = 0f), "the ball has passed it")
        assertTrue(shownPainted(state, 0, sliding = true, passed = 0f, undoing = false, cleared = 0f), "the start is painted all along")
    }

    @Test
    fun theCellsOfASlideTakenBackStayPaintedUntilTheBallHasLeftThem() {
        val game = BallSlideGame(puzzle)
        game.slide(BallDir.RIGHT)
        game.slide(BallDir.DOWN)
        game.undo() // takes the slide down back: undone is 5 then 8
        val state = game.state
        assertTrue(shownPainted(state, 8, sliding = false, passed = 0f, undoing = true, cleared = 0f))
        assertFalse(shownPainted(state, 8, sliding = false, passed = 0f, undoing = true, cleared = 1.5f), "the ball has left it")
    }
}
