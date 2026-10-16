package com.mehmtcan.brainscroll.game.queens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueensGeneratorTest {

    @Test
    fun aBoardHasTwoLockedQueensThatCanBeFinished() {
        for (seed in 0 until 100) {
            val puzzle = QueensGenerator.generate(seed)
            assertEquals(5, puzzle.size)
            assertEquals(2, puzzle.locked.size)
            assertTrue(QueensSolver.solutions(5, puzzle.locked).isNotEmpty(), "seed $seed has a solution")
            assertEquals(3, QueensGame(puzzle.size, puzzle.locked).par)
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoardAndOthersDiffer() {
        val a = QueensGenerator.generate(7)
        val b = QueensGenerator.generate(7)
        assertEquals(a.locked, b.locked)
        assertTrue((0 until 50).map { QueensGenerator.generate(it).locked }.toSet().size > 10, "boards are not all the same")
    }

    @Test
    fun theSizeAndTheNumberOfLockedQueensAreParameters() {
        val puzzle = QueensGenerator.generate(3, size = 7, locked = 3)
        assertEquals(7, puzzle.size)
        assertEquals(3, puzzle.locked.size)
        assertTrue(QueensSolver.solutions(7, puzzle.locked).isNotEmpty())
    }
}
