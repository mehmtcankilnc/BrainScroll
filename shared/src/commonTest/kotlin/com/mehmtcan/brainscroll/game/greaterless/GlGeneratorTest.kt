package com.mehmtcan.brainscroll.game.greaterless

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GlGeneratorTest {
    @Test
    fun everyLevelMakesBoardsWithExactlyOneSolution() {
        for (difficulty in Difficulty.entries) {
            val shape = GameLevels.greaterLess(difficulty)
            for (seed in 0 until 25) {
                val puzzle = GlGenerator.generate(seed, difficulty)
                assertEquals(shape.size, puzzle.size)
                assertTrue(puzzle.givenCount <= shape.given + 7, "$difficulty seed $seed has ${puzzle.givenCount} given numbers")
                assertTrue(puzzle.signCount <= shape.signs, "$difficulty seed $seed has ${puzzle.signCount} signs")
                val solutions = GlSolver.solutions(puzzle, 3)
                assertEquals(1, solutions.size, "$difficulty seed $seed has one solution")
                assertTrue(GlRules.isSolved(puzzle, solutions[0]), "$difficulty seed $seed")
                // The given numbers are part of the solution.
                for (cell in puzzle.given.indices) if (puzzle.isGiven(cell)) assertEquals(puzzle.given[cell], solutions[0][cell])
            }
        }
    }

    @Test
    fun aBoardCanBeFinishedTheWayAPlayerDoesIt() {
        for (difficulty in Difficulty.entries) {
            val shape = GameLevels.greaterLess(difficulty)
            for (seed in 0 until 15) {
                val puzzle = GlGenerator.generate(seed, difficulty)
                val byLogic = assertNotNull(GlSolver.solveByLogic(puzzle, shape.lookahead), "$difficulty seed $seed")
                assertContentEquals(GlSolver.uniqueSolution(puzzle), byLogic)
            }
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoard() {
        for (difficulty in Difficulty.entries) assertEquals(GlGenerator.generate(7, difficulty), GlGenerator.generate(7, difficulty))
    }

    @Test
    fun aBoardIsSavedAndReadBackInAtMost32Characters() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 25) {
                val puzzle = GlGenerator.generate(seed, difficulty)
                val text = puzzle.encode()
                assertTrue(text.length <= 32, "$difficulty seed $seed: ${text.length} characters")
                assertEquals(puzzle, GlPuzzle.decode(text))
            }
        }
        assertNull(GlPuzzle.decode(""))
        assertNull(GlPuzzle.decode("!!!"))
        assertNull(GlPuzzle.decode("000"), "a board that runs out")
    }

    @Test
    fun aBoardThatCannotBeIsRefused() {
        fun refused(block: () -> GlPuzzle) = runCatching(block).isFailure
        val none = List(16) { 0 }
        assertTrue(refused { GlPuzzle(3, List(9) { 0 }, List(9) { 0 }, List(9) { 0 }) }, "too small")
        assertTrue(refused { GlPuzzle(4, List(15) { 0 }, none, none) })
        assertTrue(refused { GlPuzzle(4, List(16) { 5 }, none, none) }, "a number above the size")
        assertTrue(refused { GlPuzzle(4, none, List(16) { if (it == 3) 1 else 0 }, none) }, "a sign to the right of the last column")
        assertTrue(refused { GlPuzzle(4, none, none, List(16) { if (it == 12) 2 else 0 }) }, "a sign below the last row")
        assertTrue(!GlPuzzle(7, List(49) { 7 }, List(49) { 0 }, List(49) { 0 }).canBeSaved, "49 given numbers do not fit 32 characters")
    }

    @Test
    fun aBoardWithTwoWaysIsNotUnique() {
        val puzzle = GlPuzzle(4, List(16) { 0 }, List(16) { 0 }, List(16) { 0 })
        assertTrue(GlSolver.solutions(puzzle, 5).size > 1)
        assertNull(GlSolver.uniqueSolution(puzzle))
    }

    @Test
    fun theRulesSeeRepeatedNumbersAndSignsThatAreNotTrue() {
        val puzzle = GlPuzzle(4, List(16) { 0 }, List(16) { if (it == 0) GlPuzzle.LESS else 0 }, List(16) { if (it == 1) GlPuzzle.MORE else 0 })
        // 3 in the corner against a "smaller than the next" sign with a 2 beside it; and the same 4 twice in a column.
        val grid = IntArray(16)
        grid[0] = 3
        grid[1] = 2
        assertEquals(setOf(0, 1), GlRules.conflicts(puzzle, grid))
        val twice = IntArray(16)
        twice[0] = 1
        twice[8] = 1
        assertEquals(setOf(0, 8), GlRules.conflicts(puzzle, twice))
        // The vertical sign (cell 1 bigger than the cell below it, 5) with 2 over 3.
        val vertical = IntArray(16)
        vertical[1] = 2
        vertical[5] = 3
        assertEquals(setOf(1, 5), GlRules.conflicts(puzzle, vertical))
        // Empty cells never break a sign.
        val half = IntArray(16)
        half[0] = 4
        assertTrue(GlRules.conflicts(puzzle, half).isEmpty())
    }

    @Test
    fun theRulesPruneWhatTheSignsAndLinesLeave() {
        // 1 < cell 1 on a 4 by 4 board: the cell cannot be 1, and the one to its left cannot be 4.
        val puzzle = GlPuzzle(4, List(16) { 0 }, List(16) { if (it == 0) GlPuzzle.LESS else 0 }, List(16) { 0 })
        val sets = GlSolver.sets(puzzle)
        assertTrue(GlSolver.propagate(puzzle, sets))
        assertEquals(0, sets[0] and (1 shl 4))
        assertEquals(0, sets[1] and (1 shl 1))
        // A row with 1, 2, 3 leaves 4 for the last cell.
        val row = GlPuzzle(4, List(16) { if (it < 3) it + 1 else 0 }, List(16) { 0 }, List(16) { 0 })
        val rowSets = GlSolver.sets(row)
        assertTrue(GlSolver.propagate(row, rowSets))
        assertEquals(4, GlSolver.valueOf(rowSets[3]))
    }

    @Test
    fun aRandomSquareHasEveryNumberOnceInEveryLine() {
        for (n in 4..7) {
            val grid = GlSolver.randomSquare(Random(n), n)
            for (line in GlRules.lines(n)) assertEquals((1..n).toSet(), line.map { grid[it] }.toSet())
        }
    }
}
