package com.mehmtcan.brainscroll.game.ballslide

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BallSlideGameTest {

    private val U = BallDir.UP
    private val R = BallDir.RIGHT
    private val D = BallDir.DOWN
    private val L = BallDir.LEFT

    // A 3 by 3 board with a wall in the middle and the ball in the top left corner.
    private val puzzle = BallSlidePuzzle(3, List(9) { it == 4 }, start = 0)

    @Test
    fun theBallSlidesToTheWallOrTheEdgeAndPaintsWhatItPasses() {
        val game = BallSlideGame(puzzle)
        assertEquals(1, game.state.paintedCount, "the start is painted")
        assertEquals(BallSlideResult.Moved, game.slide(R))
        assertEquals(2, game.state.ball)
        assertEquals(listOf(1, 2), game.state.lastPath)
        assertEquals(3, game.state.paintedCount)
        // The wall in the middle stops a slide down the middle column at once.
        assertEquals(BallSlideResult.Moved, game.slide(D))
        assertEquals(8, game.state.ball)
    }

    @Test
    fun aSlideIntoAWallOrTheEdgeDoesNothingAndIsNotCounted() {
        val game = BallSlideGame(puzzle)
        assertEquals(BallSlideResult.Ignored, game.slide(U))
        assertEquals(BallSlideResult.Ignored, game.slide(L))
        assertEquals(BallSlideResult.Ignored, game.slide(7))
        assertEquals(0, game.state.moves)
        game.slide(R)
        assertEquals(1, game.state.moves)
    }

    @Test
    fun paintingEveryOpenCellWins() {
        val game = BallSlideGame(puzzle)
        assertEquals(BallSlideResult.Moved, game.slide(R))
        assertEquals(BallSlideResult.Moved, game.slide(D))
        assertEquals(BallSlideResult.Moved, game.slide(L))
        assertEquals(BallSlideResult.Solved, game.slide(U))
        assertEquals(BallSlideStatus.Won, game.state.status)
        assertEquals(4, game.state.moves)
        assertEquals(BallSlideResult.Ignored, game.slide(R), "the game is over")
        assertFalse(game.undo())
    }

    @Test
    fun undoTakesTheLastSlideBackButTheSlidesStayCounted() {
        val game = BallSlideGame(puzzle)
        assertFalse(game.undo(), "nothing to take back")
        game.slide(R)
        game.slide(D)
        assertTrue(game.state.canUndo)
        assertTrue(game.undo())
        assertEquals(2, game.state.ball)
        assertEquals(3, game.state.paintedCount)
        assertEquals(emptyList(), game.state.lastPath)
        assertEquals(2, game.state.moves)
    }

    @Test
    fun anUndoSaysWhichSlideItTookBackSoTheScreenCanRunTheBallBack() {
        val game = BallSlideGame(puzzle)
        game.slide(R)
        game.slide(D)
        assertEquals(0, game.state.undos)
        assertTrue(game.undo())
        assertEquals(1, game.state.undos)
        assertEquals(listOf(5, 8), game.state.undone, "the cells of the slide down, the one the ball stopped on last")
        assertEquals(2, game.state.undoneStart, "the ball goes back to the cell the slide began on")
        game.slide(D)
        assertEquals(emptyList(), game.state.undone, "a new slide clears it")
        assertEquals(1, game.state.undos)
    }

    @Test
    fun theStateKeepsWhatWasPaintedBeforeTheLastSlideSoACellPassedAgainNeverLooksUnpainted() {
        val game = BallSlideGame(puzzle)
        assertEquals(game.state.painted, game.state.paintedBefore, "no slide yet")
        game.slide(R)
        game.slide(D)
        game.slide(L)
        game.slide(U) // goes over cell 3 (new) and cell 0 (the start, painted from the beginning)
        assertEquals(listOf(3, 0), game.state.lastPath)
        assertTrue(game.state.paintedBefore[0], "the start was painted before the slide")
        assertFalse(game.state.paintedBefore[3], "cell 3 is painted by this slide for the first time")
        assertTrue(game.state.painted[3])

        // (This board is won by that slide, so the undo is tried on a game that is still going.)
        val going = BallSlideGame(puzzle)
        going.slide(R)
        going.slide(D)
        assertTrue(going.undo())
        assertEquals(going.state.painted, going.state.paintedBefore, "after an undo there is no last slide")
    }

    @Test
    fun restartPutsTheBallOnItsStartWithOnlyThatCellPainted() {
        val game = BallSlideGame(puzzle)
        assertFalse(game.restart(), "nothing done yet")
        game.slide(R)
        game.slide(D)
        assertTrue(game.restart())
        assertEquals(0, game.state.ball)
        assertEquals(1, game.state.paintedCount)
        assertEquals(2, game.state.moves, "the slides stay counted")
        assertFalse(game.state.canRestart)
    }

    @Test
    fun theSolverFindsTheFewestSlidesAndAWayThatWins() {
        val par = assertNotNull(BallSlideSolver.par(puzzle))
        assertEquals(4, par)
        val way = assertNotNull(BallSlideSolver.shortest(puzzle, puzzle.start, 1L shl puzzle.start))
        val game = BallSlideGame(puzzle)
        way.forEach { game.slide(it) }
        assertTrue(game.isFinished)
    }

    @Test
    fun theSolverSolvesFromWhereTheBoardIsNowAndSaysWhenThereIsNoWay() {
        val game = BallSlideGame(puzzle)
        game.slide(R)
        val painted = game.state.painted.indices.fold(0L) { acc, i -> if (game.state.painted[i]) acc or (1L shl i) else acc }
        assertEquals(3, BallSlideSolver.shortest(puzzle, game.state.ball, painted)?.size)
        // Two cells sealed off by walls can never be painted.
        val sealed = BallSlidePuzzle(3, listOf(false, true, false, true, true, false, false, false, false), start = 0)
        assertNull(BallSlideSolver.par(sealed))
    }

    @Test
    fun theTextOfABoardIsReadBackAndBadTextIsRefused() {
        val back = assertNotNull(BallSlidePuzzle.decode(puzzle.encode()))
        assertEquals(puzzle.walls, back.walls)
        assertEquals(0, back.start)
        assertNull(BallSlidePuzzle.decode(""))
        assertNull(BallSlidePuzzle.decode("!!"))
        assertNull(BallSlidePuzzle.decode("0"))
        assertTrue(BallSlidePuzzle(6, List(36) { it == 7 }, 0).encode().length <= 32)
    }

    @Test
    fun aBrokenBoardIsRefused() {
        assertFailsWith<IllegalArgumentException> { BallSlidePuzzle(3, List(8) { false }, 0) }
        assertFailsWith<IllegalArgumentException> { BallSlidePuzzle(3, List(9) { it == 0 }, 0) }
        assertFailsWith<IllegalArgumentException> { BallSlidePuzzle(3, List(9) { false }, 9) }
    }
}
