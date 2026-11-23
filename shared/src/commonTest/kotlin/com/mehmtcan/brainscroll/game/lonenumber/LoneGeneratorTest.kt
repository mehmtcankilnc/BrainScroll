package com.mehmtcan.brainscroll.game.lonenumber

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoneGeneratorTest {
    @Test
    fun everyLevelMakesBoardsWithExactlyOneSolution() {
        for (difficulty in Difficulty.entries) {
            val shape = GameLevels.loneNumber(difficulty)
            for (seed in 0 until 25) {
                val puzzle = LoneGenerator.generate(seed, difficulty)
                assertEquals(shape.size, puzzle.size)
                val solutions = LoneSolver.solutions(puzzle, 3)
                assertEquals(1, solutions.size, "$difficulty seed $seed has one solution")
                // The solution follows the three rules.
                assertTrue(LoneRules.isSolved(puzzle, solutions[0]), "$difficulty seed $seed")
                assertTrue(solutions[0].isNotEmpty(), "a board with nothing to shade is no puzzle")
            }
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoard() {
        for (difficulty in Difficulty.entries) assertEquals(LoneGenerator.generate(7, difficulty), LoneGenerator.generate(7, difficulty))
    }

    @Test
    fun aBoardIsSavedAndReadBackInAtMost32Characters() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 25) {
                val puzzle = LoneGenerator.generate(seed, difficulty)
                val text = puzzle.encode()
                assertTrue(text.length <= 32, "$difficulty seed $seed: ${text.length} characters")
                assertEquals(puzzle, LonePuzzle.decode(text))
            }
        }
        assertNull(LonePuzzle.decode(""))
        assertNull(LonePuzzle.decode("!!!"))
        assertNull(LonePuzzle.decode("000"), "a board that runs out")
    }

    @Test
    fun theBiggestBoardFitsTheSavedAnswer() {
        val biggest = LonePuzzle(LonePuzzle.MAX_SIZE, List(LonePuzzle.MAX_SIZE * LonePuzzle.MAX_SIZE) { 1 })
        assertTrue(biggest.encode().length <= 32, "${biggest.encode().length} characters")
    }

    @Test
    fun aBoardWithTwoWaysIsNotUnique() {
        // Every row and column holds each number once: nothing has to be shaded, but the rules do not forbid shading some cells that
        // keep the rest joined, so the unshaded board and others are all solutions.
        val latin = List(16) { (it / 4 + it % 4) % 4 + 1 }
        val puzzle = LonePuzzle(4, latin)
        assertTrue(LoneSolver.solutions(puzzle, 5).size > 1)
        assertNull(LoneSolver.uniqueSolution(puzzle))
    }

    @Test
    fun aBoardThatCannotBeIsRefused() {
        fun refused(block: () -> LonePuzzle) = runCatching(block).isFailure
        assertTrue(refused { LonePuzzle(3, List(9) { 1 }) })
        assertTrue(refused { LonePuzzle(4, List(15) { 1 }) })
        assertTrue(refused { LonePuzzle(4, List(16) { 5 }) })
        assertTrue(refused { LonePuzzle(8, List(64) { 1 }) })
    }

    @Test
    fun theRulesSeeRepeatsTouchingAndSplitBoards() {
        // A small hand-made board: the first row holds 1 twice, and the middle column of cells can be shaded to cut the board in two.
        val puzzle = LonePuzzle(4, listOf(1, 1, 2, 3, 2, 3, 1, 4, 3, 4, 4, 2, 4, 2, 3, 1))
        assertEquals(setOf(0, 1), LoneRules.repeated(puzzle, emptySet()).filter { it < 2 }.toSet())
        assertEquals(setOf(0, 1), LoneRules.touching(puzzle, setOf(0, 1, 7)))
        // Shading the middle column of cells splits the board.
        assertFalse(LoneRules.isJoined(puzzle, setOf(1, 5, 9, 13)))
    }
}
