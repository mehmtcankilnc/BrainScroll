package com.mehmtcan.brainscroll.game.blockout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockOutGameTest {

    /**
     * A 5 by 5 board:
     *   . . . . .
     *   . . . . .
     *   X X . A .     the target X lies in the middle row, A stands in its way at column 3
     *   . . . A .
     *   . . . . .
     */
    private val text = "....." + "....." + "XX.A." + "...A." + "....."

    private val puzzle = assertNotNull(BlockOutPuzzle.decode(text))

    private fun game() = BlockOutGame(puzzle, par = 2)

    @Test
    fun theBoardIsReadInReadingOrderTheTargetFirstAndWrittenBackTheSame() {
        assertEquals(2, puzzle.shapes.size)
        assertTrue(puzzle.shapes[0].isTarget)
        assertEquals(PieceShape(horizontal = false, line = 3, length = 2), puzzle.shapes[1])
        assertEquals(text, puzzle.encode())
        assertTrue(puzzle.encode().length <= 32)
    }

    @Test
    fun aPieceSlidesAnyDistanceAlongItsLengthAndAWholeSlideIsOneMove() {
        val g = game()
        assertEquals(BlockOutResult.Moved, g.move(1, 0), "A goes two cells up in one go")
        assertEquals(1, g.state.moves)
        assertEquals(0, g.state.pieces[1].pos)
    }

    @Test
    fun aPieceCannotGoThroughAnotherOneOrOffTheBoardOrStayWhereItIs() {
        val g = game()
        assertEquals(BlockOutResult.Ignored, g.move(0, 3), "A is in the way")
        assertEquals(BlockOutResult.Ignored, g.move(0, 0), "it is there already")
        assertEquals(BlockOutResult.Ignored, g.move(1, 4), "off the board")
        assertEquals(BlockOutResult.Ignored, g.move(1, -1))
        assertEquals(BlockOutResult.Ignored, g.move(7, 0), "no such piece")
        assertEquals(0, g.state.moves)
        assertEquals(0..1, g.state.pieces[0].minPos..g.state.pieces[0].maxPos)
    }

    @Test
    fun clearingTheWayAndDrivingTheTargetToTheExitWins() {
        val g = game()
        g.move(1, 0)
        assertEquals(BlockOutResult.Solved, g.move(0, 3))
        assertTrue(g.isFinished)
        assertEquals(BlockOutStatus.Won, g.state.status)
        assertEquals(2, g.state.moves)
        assertEquals(BlockOutResult.Ignored, g.move(1, 3), "a finished game takes no more moves")
    }

    @Test
    fun aRestartPutsThePiecesBackAndKeepsTheMoves() {
        val g = game()
        assertFalse(g.state.canRestart)
        assertFalse(g.restart())
        g.move(1, 0)
        assertTrue(g.state.canRestart)
        assertTrue(g.restart())
        assertEquals(2, g.state.pieces[1].pos)
        assertEquals(1, g.state.moves)
    }

    @Test
    fun theExitIsInTheMiddleRow() {
        assertEquals(2, game().state.exitRow)
    }

    @Test
    fun aBoardThatBreaksTheRulesIsNotReadAndDecodingIsStrict() {
        assertNull(BlockOutPuzzle.decode("short"))
        assertNull(BlockOutPuzzle.decode("....." + "....." + "XX.X." + "....." + "....."), "a piece in two parts")
        assertNull(BlockOutPuzzle.decode("....." + "....." + "....." + "....." + "....."), "no target")
        assertNull(BlockOutPuzzle.decode("....." + "....." + "XX..." + "A...." + "....."), "a piece of one cell")
        assertNull(BlockOutPuzzle.decode("XX..." + "....." + "....." + "....." + "....."), "a target that is not in the middle row")
        assertFailsWith<IllegalArgumentException> { BlockOutPuzzle(5, listOf(PieceShape(true, 2, 2, true), PieceShape(true, 2, 2)), intArrayOf(0, 1)) }
    }
}
