package com.mehmtcan.brainscroll.game.tubes

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TubesGameTest {

    /** Two colors, capacity 4, two empty tubes. Bottom first. */
    private val start = listOf(
        listOf(0, 1, 0, 1),
        listOf(1, 0, 1, 0),
        emptyList(),
        emptyList(),
    )

    private fun game() = TubesGame(start, capacity = 4)

    @Test
    fun aPourMovesTheWholeRunOnTopOntoAnEmptyTube() {
        val g = TubesGame(listOf(listOf(0, 1, 1), listOf(0, 0, 0), listOf(1, 1), emptyList()), capacity = 4)
        assertEquals(PourResult.Poured, g.pour(0, 3))
        assertEquals(listOf(listOf(0), listOf(0, 0, 0), listOf(1, 1), listOf(1, 1)), g.state.tubes)
    }

    @Test
    fun aPourOnlyMovesAsMuchAsFits() {
        // The run of two 0s on tube 2 meets only one free place on tube 0.
        val g = TubesGame(listOf(listOf(1, 0, 0), listOf(1, 1, 1), listOf(0, 0)), capacity = 4)
        assertEquals(PourResult.Poured, g.pour(2, 0))
        assertEquals(listOf(listOf(1, 0, 0, 0), listOf(1, 1, 1), listOf(0)), g.state.tubes)
    }

    @Test
    fun aPourOntoAnotherColorOrAFullTubeOrItselfIsIgnored() {
        val g = game()
        assertEquals(PourResult.Ignored, g.pour(0, 1), "top colors differ (1 on 0)")
        assertEquals(PourResult.Ignored, g.pour(0, 0))
        assertEquals(PourResult.Ignored, g.pour(2, 3), "an empty tube has nothing to pour")
        assertEquals(PourResult.Ignored, g.pour(0, 9))
        assertEquals(0, g.state.moves, "ignored pours are not moves")
    }

    @Test
    fun aFullTubeCannotTakeMoreEvenOfTheSameColor() {
        // Both tubes end in a 0, but tube 1 is full.
        val g = TubesGame(listOf(listOf(1, 0), listOf(2, 0), listOf(1, 2)), capacity = 2)
        assertEquals(PourResult.Ignored, g.pour(0, 1))
    }

    @Test
    fun pouringAOneColorTubeOntoAnEmptyOneIsNotAllowed() {
        val g = TubesGame(listOf(listOf(0, 0), listOf(0, 0, 1, 1), listOf(1, 1), emptyList()), capacity = 4)
        assertEquals(PourResult.Ignored, g.pour(0, 3), "it only wastes a move")
        assertEquals(PourResult.Poured, g.pour(1, 3), "a mixed tube may")
    }

    @Test
    fun aMoveCountsAndUndoTakesItBackWithoutGivingTheMoveBack() {
        val g = game()
        assertFalse(g.state.canUndo)
        assertEquals(PourResult.Poured, g.pour(0, 2))
        assertEquals(1, g.state.moves)
        assertTrue(g.state.canUndo)
        assertTrue(g.undo())
        assertEquals(start, g.state.tubes)
        assertEquals(1, g.state.moves, "undo does not hand the move back")
        assertFalse(g.undo(), "nothing left to take back")
    }

    @Test
    fun restartPutsEveryTubeBackAndKeepsTheMoves() {
        val g = game()
        assertFalse(g.restart(), "nothing to restart yet")
        g.pour(0, 2)
        g.pour(1, 3)
        assertTrue(g.state.canRestart)
        assertTrue(g.restart())
        assertEquals(start, g.state.tubes)
        assertEquals(2, g.state.moves)
        assertFalse(g.state.canUndo, "restart clears what could be undone")
        assertFalse(g.state.canRestart)
    }

    @Test
    fun solvingABoardWinsIt() {
        val g = TubesGame(listOf(listOf(0, 0, 0), listOf(0), emptyList()), capacity = 4)
        assertEquals(TubesStatus.Playing, g.state.status)
        assertEquals(PourResult.Poured, g.pour(1, 0))
        assertEquals(TubesStatus.Won, g.state.status)
        assertTrue(g.isFinished)
        assertEquals(PourResult.Ignored, g.pour(0, 2))
        assertFalse(g.undo(), "a won board is final")
        assertFalse(g.restart())
    }

    @Test
    fun aBoardNeedsEveryColorToFillExactlyOneTube() {
        assertFailsWith<IllegalArgumentException> { TubesGame(listOf(listOf(0, 0, 1), listOf(1)), capacity = 4) }
        assertFailsWith<IllegalArgumentException> { TubesGame(listOf(listOf(0, 0, 0, 0, 0)), capacity = 4) }
    }

    @Test
    fun pouredDoesNotChangeItsInput() {
        val tubes = start
        assertNull(TubesGame.poured(tubes, 0, 1, 4))
        TubesGame.poured(tubes, 0, 2, 4)
        assertEquals(listOf(0, 1, 0, 1), tubes[0])
    }

    @Test
    fun newLayoutsAreSolvableShuffledAndNeverFinishedAtTheStart() {
        repeat(25) { seed ->
            val layout = TubesGame.newLayout(Random(seed))
            assertEquals(TubesGame.COLORS + TubesGame.EMPTY_TUBES, layout.size)
            assertEquals(TubesGame.EMPTY_TUBES, layout.count { it.isEmpty() })
            val counts = layout.flatten().groupingBy { it }.eachCount()
            assertEquals(TubesGame.COLORS, counts.size)
            assertTrue(counts.values.all { it == TubesGame.DEFAULT_CAPACITY })
            assertFalse(TubesGame.isSolved(layout, TubesGame.DEFAULT_CAPACITY))
            assertTrue(layout.none { it.isNotEmpty() && it.all { c -> c == it.first() } }, "no tube starts finished")
            assertTrue(TubesGame.canSolve(layout, TubesGame.DEFAULT_CAPACITY))
        }
    }

    @Test
    fun theSolverSaysNoToAnImpossibleBoard() {
        // Two colors and no empty tube: nothing can be poured at all.
        assertFalse(TubesGame.canSolve(listOf(listOf(0, 1, 0, 1), listOf(1, 0, 1, 0)), 4))
    }
}
