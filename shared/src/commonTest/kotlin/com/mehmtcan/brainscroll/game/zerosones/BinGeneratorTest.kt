package com.mehmtcan.brainscroll.game.zerosones

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BinGeneratorTest {
    @Test
    fun everyLevelMakesBoardsWithExactlyOneSolution() {
        for (difficulty in Difficulty.entries) {
            val shape = GameLevels.zerosOnes(difficulty)
            for (seed in 0 until 25) {
                val puzzle = BinGenerator.generate(seed, difficulty)
                assertEquals(shape.size, puzzle.size)
                assertTrue(puzzle.givenCount <= shape.given, "$difficulty seed $seed has ${puzzle.givenCount} given cells")
                val solutions = BinSolver.solutions(puzzle, 3)
                assertEquals(1, solutions.size, "$difficulty seed $seed has one solution")
                assertTrue(BinRules.isSolved(puzzle.size, solutions[0]), "$difficulty seed $seed")
                // The given cells are part of the solution.
                for (cell in puzzle.given.indices) if (puzzle.isGiven(cell)) assertEquals(puzzle.given[cell], solutions[0][cell])
            }
        }
    }

    @Test
    fun aBoardCanBeFinishedTheWayAPlayerDoesIt() {
        for (difficulty in Difficulty.entries) {
            val shape = GameLevels.zerosOnes(difficulty)
            for (seed in 0 until 15) {
                val puzzle = BinGenerator.generate(seed, difficulty)
                val byLogic = assertNotNull(BinSolver.solveByLogic(puzzle.size, puzzle.given.toIntArray(), shape.lookahead), "$difficulty seed $seed")
                assertContentEquals(BinSolver.uniqueSolution(puzzle), byLogic)
            }
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoard() {
        for (difficulty in Difficulty.entries) assertEquals(BinGenerator.generate(7, difficulty), BinGenerator.generate(7, difficulty))
    }

    @Test
    fun aBoardIsSavedAndReadBackInAtMost32Characters() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 25) {
                val puzzle = BinGenerator.generate(seed, difficulty)
                val text = puzzle.encode()
                assertTrue(text.length <= 32, "$difficulty seed $seed: ${text.length} characters")
                assertEquals(puzzle, BinPuzzle.decode(text))
            }
        }
        assertNull(BinPuzzle.decode(""))
        assertNull(BinPuzzle.decode("!!!"))
        assertNull(BinPuzzle.decode("000"), "a board that runs out")
    }

    @Test
    fun aBoardWithTooManyGivenCellsCannotBeSaved() {
        val full = List(100) { 0 }
        assertTrue(runCatching { BinPuzzle(10, full) }.isFailure, "100 given cells do not fit 32 characters")
        assertTrue(runCatching { BinPuzzle(10, List(100) { if (it < BinPuzzle.maxGiven(10)) 0 else -1 }) }.isSuccess)
        assertTrue(BinPuzzle(10, List(100) { if (it < BinPuzzle.maxGiven(10)) 0 else -1 }).encode().length <= 32)
    }

    @Test
    fun aBoardThatCannotBeIsRefused() {
        fun refused(block: () -> BinPuzzle) = runCatching(block).isFailure
        assertTrue(refused { BinPuzzle(5, List(25) { -1 }) }, "odd side")
        assertTrue(refused { BinPuzzle(12, List(144) { -1 }) }, "too big")
        assertTrue(refused { BinPuzzle(4, List(15) { -1 }) })
        assertTrue(refused { BinPuzzle(4, List(16) { 2 }) })
    }

    @Test
    fun aBoardWithTwoWaysIsNotUnique() {
        val puzzle = BinPuzzle(4, List(16) { -1 })
        assertTrue(BinSolver.solutions(puzzle, 5).size > 1)
        assertNull(BinSolver.uniqueSolution(puzzle))
    }

    @Test
    fun theRulesSeeThreeInARowTooManyAndSameLines() {
        // Row 0 has three 1s side by side, which also leaves the row with more than half 1s.
        val grid = IntArray(16) { -1 }
        grid[0] = 1
        grid[1] = 1
        grid[2] = 1
        assertEquals(setOf(0, 1, 2), BinRules.conflicts(4, grid))
        // Two filled rows that are the same.
        val same = intArrayOf(0, 1, 0, 1, 0, 1, 0, 1, -1, -1, -1, -1, -1, -1, -1, -1)
        assertTrue(BinRules.conflicts(4, same).containsAll((0..7).toSet()))
        // A line with a digit more than half the times.
        val many = IntArray(16) { -1 }
        many[0] = 0
        many[1] = 1
        many[2] = 0
        many[3] = 0
        assertTrue(0 in BinRules.conflicts(4, many))
    }

    @Test
    fun theRulesForceTheCellsThePlayerSees() {
        // Two 1s side by side: the cell next to them is 0.
        val a = IntArray(16) { -1 }
        a[0] = 1
        a[1] = 1
        assertTrue((2 to 0) in BinSolver.forcedOnce(4, a))
        // Two 0s in a row of four: the rest are 1s.
        val b = IntArray(16) { -1 }
        b[4] = 0
        b[6] = 0
        assertTrue((5 to 1) in BinSolver.forcedOnce(4, b))
        assertTrue((7 to 1) in BinSolver.forcedOnce(4, b))
    }

    @Test
    fun aRandomSolutionFollowsTheRules() {
        for (n in listOf(4, 6, 8, 10)) assertTrue(BinRules.isSolved(n, BinSolver.randomSolution(Random(n), n)))
    }
}
