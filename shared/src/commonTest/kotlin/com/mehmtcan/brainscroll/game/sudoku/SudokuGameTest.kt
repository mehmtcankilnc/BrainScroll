package com.mehmtcan.brainscroll.game.sudoku

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SudokuGameTest {

    private val seeds = (1..40).toList()

    private fun units(): List<List<Int>> =
        List(9) { r -> List(9) { r * 9 + it } } +
            List(9) { c -> List(9) { it * 9 + c } } +
            List(9) { b -> List(9) { ((b / 3) * 3 + it / 3) * 9 + (b % 3) * 3 + it % 3 } }

    /** How many solutions [grid] has, counting up to [limit] (plain backtracking, independent of the generator). */
    private fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val cell = grid.indices.firstOrNull { grid[it] == 0 } ?: return 1
        var found = 0
        for (digit in 1..9) {
            if (SudokuGenerator.peers(cell).any { grid[it] == digit }) continue
            grid[cell] = digit
            found += countSolutions(grid, limit - found)
            grid[cell] = 0
            if (found >= limit) break
        }
        return found
    }

    @Test
    fun everySolutionIsAValidGridAndTheGivensAreFromIt() {
        for (seed in seeds) {
            val puzzle = SudokuGenerator.generate(seed)
            for (unit in units()) {
                assertEquals((1..9).toSet(), unit.map { puzzle.solution[it] }.toSet(), "seed $seed")
            }
            for (cell in 0 until 81) {
                assertTrue(puzzle.givens[cell] == 0 || puzzle.givens[cell] == puzzle.solution[cell], "seed $seed cell $cell")
            }
        }
    }

    @Test
    fun everyBoardIsFinishedByLogicAloneAndHasExactlyOneSolution() {
        for (seed in seeds.take(12)) {
            val puzzle = SudokuGenerator.generate(seed)
            assertContentEquals(puzzle.solution, SudokuGenerator.solveBySingles(puzzle.givens), "seed $seed")
            assertEquals(1, countSolutions(puzzle.givens.copyOf()), "seed $seed has more than one solution")
        }
    }

    @Test
    fun theSameSeedAlwaysGivesTheSameBoardAndOtherSeedsGiveOtherOnes() {
        val a = SudokuGenerator.generate(7)
        val b = SudokuGenerator.generate(7)
        assertContentEquals(a.givens, b.givens)
        assertContentEquals(a.solution, b.solution)
        assertTrue(!SudokuGenerator.generate(8).givens.contentEquals(a.givens))
    }

    @Test
    fun aBoardHasAboutTheAskedNumberOfGivens() {
        for (seed in seeds) {
            val clues = SudokuGenerator.generate(seed).givens.count { it != 0 }
            assertTrue(clues in SudokuGenerator.DEFAULT_CLUES..SudokuGenerator.DEFAULT_CLUES + 6, "seed $seed has $clues givens")
        }
        // More givens asked for means an easier board with at least that many.
        assertTrue(SudokuGenerator.generate(3, clues = 50).givens.count { it != 0 } in 50..52)
    }

    @Test
    fun theSolverGivesUpOnABoardThatNeedsAGuess() {
        // An empty grid has no single to start from.
        assertEquals(null, SudokuGenerator.solveBySingles(IntArray(81)))
    }

    // --- Rules ---

    private fun game(seed: Int = 5) = SudokuGame(seed)

    private fun solutionOf(seed: Int = 5) = SudokuGenerator.generate(seed).solution

    private fun SudokuGame.emptyCells() = state.values.indices.filter { state.values[it] == 0 }

    /** Some digit other than the solution's, for [cell]. */
    private fun wrongDigit(cell: Int, seed: Int = 5) = if (solutionOf(seed)[cell] == 1) 2 else 1

    @Test
    fun aNewBoardIsPlayingWithNoMistakes() {
        val state = game().state
        assertEquals(SudokuStatus.Playing, state.status)
        assertEquals(0, state.mistakes)
        assertEquals(3, state.maxMistakes)
        assertEquals(0, state.moves)
        assertTrue(state.wrong.isEmpty())
        assertEquals(state.givens, state.values)
    }

    @Test
    fun aCorrectDigitIsPlacedCountsAMoveAndIsFinal() {
        val g = game()
        val cell = g.emptyCells().first()
        val digit = solutionOf()[cell]
        assertEquals(SudokuResult.Correct, g.place(cell, digit))
        assertEquals(digit, g.state.values[cell])
        assertEquals(1, g.state.moves)
        assertEquals(SudokuResult.Ignored, g.place(cell, wrongDigit(cell)), "a right digit can not be replaced")
        assertEquals(SudokuResult.Ignored, g.erase(cell))
        assertEquals(digit, g.state.values[cell])
        assertEquals(0, g.state.mistakes)
    }

    @Test
    fun aWrongDigitIsAMistakeShowsAsWrongAndCanBeReplacedOrErased() {
        val g = game()
        val cell = g.emptyCells().first()
        assertEquals(SudokuResult.Mistake, g.place(cell, wrongDigit(cell)))
        assertEquals(1, g.state.mistakes)
        assertEquals(setOf(cell), g.state.wrong)
        assertEquals(SudokuStatus.Playing, g.state.status)

        assertEquals(SudokuResult.Ignored, g.place(cell, wrongDigit(cell)), "the same digit again is nothing")
        assertEquals(1, g.state.mistakes)

        assertEquals(SudokuResult.Changed, g.erase(cell))
        assertEquals(0, g.state.values[cell])
        assertTrue(g.state.wrong.isEmpty())
        assertEquals(SudokuResult.Correct, g.place(cell, solutionOf()[cell]))
    }

    @Test
    fun theThirdMistakeLosesAndTheBoardTakesNothingMore() {
        val g = game()
        val cells = g.emptyCells()
        g.place(cells[0], wrongDigit(cells[0]))
        g.place(cells[1], wrongDigit(cells[1]))
        assertEquals(SudokuStatus.Playing, g.state.status)
        g.place(cells[2], wrongDigit(cells[2]))
        assertEquals(SudokuStatus.Lost, g.state.status)
        assertTrue(g.isFinished)
        assertEquals(SudokuResult.Ignored, g.place(cells[3], solutionOf()[cells[3]]))
        assertEquals(SudokuResult.Ignored, g.toggleNote(cells[3], 4))
        assertEquals(SudokuResult.Ignored, g.erase(cells[0]))
    }

    @Test
    fun fillingEveryCellRightWins() {
        val g = game()
        val solution = solutionOf()
        for (cell in g.emptyCells()) {
            assertEquals(SudokuStatus.Playing, g.state.status)
            g.place(cell, solution[cell])
        }
        assertEquals(SudokuStatus.Won, g.state.status)
        assertEquals(0, g.state.mistakes)
        assertEquals(solution.toList(), g.state.values)
    }

    @Test
    fun aWrongDigitMustBeFixedBeforeTheBoardIsWon() {
        val g = game()
        val solution = solutionOf()
        val cells = g.emptyCells()
        g.place(cells[0], wrongDigit(cells[0]))
        for (cell in cells.drop(1)) g.place(cell, solution[cell])
        assertEquals(SudokuStatus.Playing, g.state.status, "one wrong cell is left")
        g.erase(cells[0])
        g.place(cells[0], solution[cells[0]])
        assertEquals(SudokuStatus.Won, g.state.status)
    }

    @Test
    fun givensCanNotBeChangedAndBadInputIsIgnored() {
        val g = game()
        val given = g.state.givens.indexOfFirst { it != 0 }
        assertEquals(SudokuResult.Ignored, g.place(given, 1))
        assertEquals(SudokuResult.Ignored, g.erase(given))
        assertEquals(SudokuResult.Ignored, g.toggleNote(given, 1))
        val empty = g.emptyCells().first()
        assertEquals(SudokuResult.Ignored, g.place(empty, 0))
        assertEquals(SudokuResult.Ignored, g.place(empty, 10))
        assertEquals(SudokuResult.Ignored, g.place(-1, 1))
        assertEquals(SudokuResult.Ignored, g.place(81, 1))
        assertEquals(SudokuResult.Ignored, g.toggleNote(empty, 10))
        assertEquals(0, g.state.moves)
        assertEquals(0, g.state.mistakes)
    }

    @Test
    fun notesToggleOnAndOffOnlyOnEmptyCellsAndAreNotMoves() {
        val g = game()
        val cell = g.emptyCells().first()
        assertEquals(SudokuResult.Changed, g.toggleNote(cell, 3))
        assertEquals(SudokuResult.Changed, g.toggleNote(cell, 7))
        assertEquals((1 shl 2) or (1 shl 6), g.state.notes[cell])
        assertEquals(SudokuResult.Changed, g.toggleNote(cell, 3))
        assertEquals(1 shl 6, g.state.notes[cell])
        assertEquals(0, g.state.moves)

        assertEquals(SudokuResult.Changed, g.erase(cell), "erase clears the notes")
        assertEquals(0, g.state.notes[cell])
        assertEquals(SudokuResult.Ignored, g.erase(cell), "nothing left to erase")

        g.place(cell, wrongDigit(cell))
        assertEquals(SudokuResult.Ignored, g.toggleNote(cell, 2), "a cell with a digit takes no notes")
    }

    @Test
    fun placingADigitClearsItsOwnNotesAndTheSameNoteAroundIt() {
        val g = game()
        val solution = solutionOf()
        val cell = g.emptyCells().first()
        val digit = solution[cell]
        val peer = SudokuGenerator.peers(cell).first { g.state.values[it] == 0 }
        val farAway = g.emptyCells().first { it != cell && it !in SudokuGenerator.peers(cell) }
        g.toggleNote(cell, 4)
        g.toggleNote(peer, digit)
        g.toggleNote(peer, if (digit == 9) 1 else digit + 1)
        g.toggleNote(farAway, digit)

        g.place(cell, digit)
        assertEquals(0, g.state.notes[cell])
        assertEquals(1 shl (if (digit == 9) 0 else digit), g.state.notes[peer], "only the other note is left around")
        assertEquals(1 shl (digit - 1), g.state.notes[farAway], "a cell that does not see it keeps the note")
    }

    @Test
    fun aWrongDigitDoesNotTouchTheNotesAround() {
        val g = game()
        val cell = g.emptyCells().first()
        val peer = SudokuGenerator.peers(cell).first { g.state.values[it] == 0 }
        val digit = wrongDigit(cell)
        g.toggleNote(peer, digit)
        g.place(cell, digit)
        assertEquals(1 shl (digit - 1), g.state.notes[peer])
    }

    @Test
    fun aGameNeedsSaneSettings() {
        assertFailsWith<IllegalArgumentException> { SudokuGame(1, clues = 5) }
        assertFailsWith<IllegalArgumentException> { SudokuGame(1, clues = 81) }
        assertFailsWith<IllegalArgumentException> { SudokuGame(1, maxMistakes = 0) }
        assertNotNull(SudokuGame(1, clues = 45, maxMistakes = 5))
    }
}
