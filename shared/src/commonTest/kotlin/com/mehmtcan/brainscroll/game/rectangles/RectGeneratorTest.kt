package com.mehmtcan.brainscroll.game.rectangles

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RectGeneratorTest {
    @Test
    fun everyLevelMakesBoardsWithExactlyOneSolution() {
        for (difficulty in Difficulty.entries) {
            val shape = GameLevels.rectangles(difficulty)
            for (seed in 0 until 25) {
                val puzzle = RectGenerator.generate(seed, difficulty)
                assertEquals(shape.size, puzzle.size)
                assertTrue(puzzle.clueCells.size >= shape.minPieces, "$difficulty seed $seed has ${puzzle.clueCells.size} numbers")
                assertTrue(puzzle.numbers.max() <= shape.maxArea)
                val solutions = RectSolver.solve(puzzle, 3)
                assertEquals(1, solutions.size, "$difficulty seed $seed has one solution")
                // The solution covers every cell once and every rectangle is what its number says.
                val cells = solutions[0].flatMap { it.cells(puzzle.size) }
                assertEquals(puzzle.size * puzzle.size, cells.toSet().size)
                assertEquals(cells.size, cells.toSet().size)
                assertTrue(solutions[0].all { puzzle.isFilledRight(it) })
            }
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoard() {
        for (difficulty in Difficulty.entries) assertEquals(RectGenerator.generate(7, difficulty), RectGenerator.generate(7, difficulty))
    }

    @Test
    fun aBoardIsSavedAndReadBackInAtMost32Characters() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 25) {
                val puzzle = RectGenerator.generate(seed, difficulty)
                val text = puzzle.encode()
                assertTrue(text.length <= 32, "$difficulty seed $seed: ${text.length} characters")
                assertEquals(puzzle, RectPuzzle.decode(text))
            }
        }
        assertNull(RectPuzzle.decode(""))
        assertNull(RectPuzzle.decode("!!!"))
        assertNull(RectPuzzle.decode("zzzzzzzzzzzzzzzzzzzzzzzz"), "numbers that do not add up")
    }

    @Test
    fun aBoardWithTwoWaysIsNotUnique() {
        // Two corners of a 4x4 board, each 8: both can be cut across or both down, so there is more than one solution.
        val numbers = MutableList(16) { 0 }
        numbers[0] = 8
        numbers[15] = 8
        val puzzle = RectPuzzle(4, numbers)
        assertTrue(RectSolver.solve(puzzle, 5).size > 1)
        assertNull(RectSolver.uniqueSolution(puzzle))
        assertNotNull(RectSolver.uniqueSolution(RectPuzzle(4, MutableList(16) { 1 })))
    }

    @Test
    fun aBoardThatCannotBeIsRefused() {
        fun refused(block: () -> RectPuzzle) = runCatching(block).isFailure
        assertTrue(refused { RectPuzzle(4, MutableList(16) { 0 }) })
        assertTrue(refused { RectPuzzle(3, MutableList(9) { 1 }) })
        assertTrue(refused { RectPuzzle(4, MutableList(15) { 1 }) })
    }
}
