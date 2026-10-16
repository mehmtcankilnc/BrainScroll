package com.mehmtcan.brainscroll.game.blockout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockOutSolverTest {

    /** The target in the middle row and an upright piece of two cells at column 3 in its way. */
    private val BLOCKED = "....." + "....." + "XX.A." + "...A." + "....."

    private fun puzzle(text: String) = assertNotNull(BlockOutPuzzle.decode(text))

    @Test
    fun theShortestWayClearsTheBlockerAndThenDrivesTheTarget() {
        val p = puzzle(BLOCKED)
        val way = assertNotNull(BlockOutSolver.shortest(p.size, p.shapes, p.start))
        assertEquals(2, way.size)
        assertEquals(1, way[0].piece, "A first")
        assertEquals(0, way[1].piece)
        assertEquals(3, way[1].to)
    }

    @Test
    fun aBoardThatIsSolvedHasAnEmptyWay() {
        val p = puzzle("....." + "....." + "...XX" + "....." + ".....")
        assertEquals(emptyList(), BlockOutSolver.shortest(p.size, p.shapes, p.start))
    }

    @Test
    fun aBoardWhereTheTargetCanNeverGetOutHasNoWay() {
        // An upright piece as long as the board, right in the way, cannot go anywhere.
        val p = puzzle("...A." + "...A." + "XX.A." + "...A." + "...A.")
        assertNull(BlockOutSolver.shortest(p.size, p.shapes, p.start))
    }

    @Test
    fun theRangeOfAPieceStopsAtTheEdgeAndAtOtherPieces() {
        val p = puzzle(BLOCKED)
        assertEquals(0..1, BlockOutSolver.range(p.size, p.shapes, p.start, 0))
        assertEquals(0..3, BlockOutSolver.range(p.size, p.shapes, p.start, 1))
    }

    @Test
    fun theWayIsReallyShortestAndEveryMoveInItIsLegal() {
        val board = BlockOutGenerator.generate(3)
        val p = board.puzzle
        val way = assertNotNull(BlockOutSolver.shortest(p.size, p.shapes, p.start))
        assertEquals(board.par, way.size)
        val pos = p.start.copyOf()
        for (move in way) {
            assertTrue(move.to in BlockOutSolver.range(p.size, p.shapes, pos, move.piece), "$move is legal")
            pos[move.piece] = move.to
        }
        assertTrue(BlockOutSolver.isSolved(p.size, p.shapes, pos))
    }
}
