package com.mehmtcan.brainscroll.game.line

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LineGameTest {

    /** A 3x3 board with number 1 on cell 0, number 2 on cell 5 and number 3 on cell 8. */
    private fun small() = LineGame(LinePuzzle(3, intArrayOf(0, 5, 8), intArrayOf(0, 1, 2, 5, 4, 3, 6, 7, 8)))

    private fun LineGame.draw(vararg cells: Int) = cells.map { drag(it) }

    @Test
    fun theLineMustBeginOnNumberOne() {
        val game = small()
        assertEquals(LineResult.Ignored, game.drag(4))
        assertEquals(LineResult.Ignored, game.tap(8))
        assertTrue(game.state.path.isEmpty())
        assertEquals(LineResult.Started, game.drag(0))
        assertEquals(listOf(0), game.state.path)
        assertEquals(1, game.state.reached)
    }

    @Test
    fun theLineGrowsOneCellAtATimeToAFreeNeighbor() {
        val game = small()
        game.drag(0)
        assertEquals(LineResult.Ignored, game.drag(4), "no diagonals")
        assertEquals(LineResult.Ignored, game.drag(2), "no jumps")
        assertEquals(LineResult.Ignored, game.drag(0), "the head itself")
        assertEquals(LineResult.Extended, game.drag(1))
        assertEquals(LineResult.Extended, game.drag(4))
        assertEquals(listOf(0, 1, 4), game.state.path)
        assertEquals(LineResult.Ignored, game.drag(-1))
        assertEquals(LineResult.Ignored, game.drag(9))
    }

    @Test
    fun numbersAreEnteredInOrder() {
        val game = small()
        game.draw(0, 3, 6, 7)
        assertEquals(LineResult.WrongOrder, game.drag(8), "number 3 before number 2")
        assertEquals(listOf(0, 3, 6, 7), game.state.path)
        assertEquals(1, game.state.reached)
    }

    @Test
    fun theLastNumberWaitsUntilItIsTheLastFreeCell() {
        val game = small()
        game.draw(0, 1, 2, 5)
        assertEquals(2, game.state.reached)
        assertEquals(LineResult.EndTooEarly, game.drag(8))
        assertEquals(LineResult.Extended, game.drag(4))
    }

    @Test
    fun goingBackOverTheEndOfTheLineIsFree() {
        val game = small()
        game.draw(0, 1, 2, 5)
        assertEquals(LineResult.Retracted, game.drag(2))
        assertEquals(listOf(0, 1, 2), game.state.path)
        assertEquals(1, game.state.reached, "number 2 is no longer passed")
        assertEquals(0, game.state.undos)
        assertEquals(LineResult.Extended, game.drag(5))
        assertEquals(2, game.state.reached)
    }

    @Test
    fun aTapOnAnEarlierCellCutsTheLineAndCountsAsAnUndoButADragDoesNot() {
        val game = small()
        game.draw(0, 1, 2, 5, 4)
        assertEquals(LineResult.Ignored, game.drag(1), "a finger that jumps back does not cut")
        assertEquals(5, game.state.path.size)
        assertEquals(LineResult.Cut, game.tap(1))
        assertEquals(listOf(0, 1), game.state.path)
        assertEquals(1, game.state.reached)
        assertEquals(1, game.state.undos)
    }

    @Test
    fun undoTakesTheLastCellOffButNeverNumberOne() {
        val game = small()
        assertFalse(game.undo())
        game.drag(0)
        assertFalse(game.undo())
        game.draw(1, 2)
        assertTrue(game.undo())
        assertEquals(listOf(0, 1), game.state.path)
        assertEquals(1, game.state.undos)
    }

    @Test
    fun restartClearsTheLineAndCountsAsAnUndo() {
        val game = small()
        assertFalse(game.restart())
        game.draw(0, 1, 2)
        assertTrue(game.state.canRestart)
        assertTrue(game.restart())
        assertTrue(game.state.path.isEmpty())
        assertEquals(0, game.state.reached)
        assertEquals(1, game.state.undos)
        assertFalse(game.state.canRestart)
        assertEquals(LineResult.Started, game.drag(0))
    }

    @Test
    fun fillingEveryCellOnTheLastNumberWinsAndTheGameStops() {
        val game = small()
        val results = game.draw(0, 1, 2, 5, 4, 3, 6, 7, 8)
        assertEquals(LineResult.Solved, results.last())
        assertTrue(game.isFinished)
        assertEquals(LineStatus.Won, game.state.status)
        assertEquals(3, game.state.reached)
        assertEquals(LineResult.Ignored, game.drag(7))
        assertEquals(LineResult.Ignored, game.tap(1))
        assertFalse(game.undo())
        assertFalse(game.restart())
        assertNull(game.hint())
        assertFalse(game.state.canUndo)
    }

    @Test
    fun aHintGivesTheNextCellsOfTheSolutionAndCountsOnlyWhenItGivesSome() {
        val puzzle = LineSchedule.puzzleFor(20740)
        val game = LineGame(puzzle)
        assertContentEquals(puzzle.solution.take(3), game.hint())
        assertEquals(1, game.state.hintsUsed)
        game.drag(puzzle.solution[0])
        game.drag(puzzle.solution[1])
        assertContentEquals(puzzle.solution.drop(2).take(3), game.hint(3))
        assertEquals(2, game.state.hintsUsed)
        assertContentEquals(puzzle.solution.drop(2).take(1), game.hint(1))
    }

    @Test
    fun aHintOnALineThatLeadsNowhereGivesNothingAndIsNotCounted() {
        val puzzle = LineSchedule.puzzleFor(20740)
        val game = LineGame(puzzle)
        game.drag(puzzle.solution[0])
        // Walk to any free neighbor that is not the solution's next cell until the line has no way on.
        val wrong = (0 until puzzle.cells).first { cell ->
            cell != puzzle.solution[1] && LineGame(puzzle).also { it.drag(puzzle.solution[0]) }.drag(cell) == LineResult.Extended &&
                LineSolver.solve(puzzle.size, puzzle.checkpoints, intArrayOf(puzzle.solution[0], cell)) == null
        }
        game.drag(wrong)
        assertNull(game.hint())
        assertEquals(0, game.state.hintsUsed)
    }

    @Test
    fun theScoreCodeKeepsTheUndosAndTheHintsAndIsNeverZero() {
        assertEquals(1, LineScoring.encode(0, 0))
        assertEquals(2, LineScoring.encode(1, 0))
        assertEquals(101, LineScoring.encode(0, 1))
        assertEquals(1000, LineScoring.encode(500, 500), "kept within what the server expects")
        for (undos in 0..120) for (hints in 0..12) {
            val code = LineScoring.encode(undos, hints)
            assertTrue(code in 1..1000)
            assertEquals(undos.coerceAtMost(99), LineScoring.undosOf(code))
            assertEquals(hints.coerceAtMost(9), LineScoring.hintsOf(code))
            assertEquals(LineScoring.stars(undos, hints), LineScoring.starsOf(code), "undos $undos hints $hints")
        }
    }

    @Test
    fun starsFollowTheUndosAndAHintTakesOneOff() {
        assertEquals(3, LineScoring.stars(undos = 0, hintsUsed = 0))
        assertEquals(2, LineScoring.stars(undos = 1, hintsUsed = 0))
        assertEquals(2, LineScoring.stars(undos = 3, hintsUsed = 0))
        assertEquals(1, LineScoring.stars(undos = 4, hintsUsed = 0))
        assertEquals(2, LineScoring.stars(undos = 0, hintsUsed = 1))
        assertEquals(2, LineScoring.stars(undos = 0, hintsUsed = 5), "the number of hints does not matter")
        assertEquals(1, LineScoring.stars(undos = 2, hintsUsed = 1))
        assertEquals(0, LineScoring.stars(undos = 9, hintsUsed = 1))
    }
}
