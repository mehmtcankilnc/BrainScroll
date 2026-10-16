package com.mehmtcan.brainscroll.game.tents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TentsGeneratorTest {

    @Test
    fun everyBoardHasEightTreesAndExactlyOneSolution() {
        for (seed in 0 until 60) {
            val puzzle = TentsGenerator.generate(seed)
            assertEquals(6, puzzle.size)
            assertEquals(8, puzzle.trees.size)
            assertEquals(8, puzzle.tents.size)
            val found = TentsSolver.solutions(puzzle.size, puzzle.trees, puzzle.rowTargets, puzzle.columnTargets)
            assertEquals(1, found.size, "seed $seed has one way")
            assertTrue(TentsRules.isSolved(puzzle.size, puzzle.trees, puzzle.tents, puzzle.rowTargets, puzzle.columnTargets))
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoardAndOthersDiffer() {
        assertEquals(TentsGenerator.generate(7).encode(), TentsGenerator.generate(7).encode())
        assertTrue((0 until 30).map { TentsGenerator.generate(it).encode() }.toSet().size > 25, "boards are not all the same")
    }

    @Test
    fun theSizeAndTheNumberOfTentsAreParameters() {
        val puzzle = TentsGenerator.generate(3, size = 5, tents = 5)
        assertEquals(5, puzzle.size)
        assertEquals(5, puzzle.trees.size)
    }
}
