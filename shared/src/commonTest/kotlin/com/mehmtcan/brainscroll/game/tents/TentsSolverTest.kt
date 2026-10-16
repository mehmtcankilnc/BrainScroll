package com.mehmtcan.brainscroll.game.tents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TentsSolverTest {

    @Test
    fun aBoardWithOneWayHasExactlyOneSolution() {
        val puzzle = TentsGenerator.generate(1)
        val found = TentsSolver.solutions(puzzle.size, puzzle.trees, puzzle.rowTargets, puzzle.columnTargets)
        assertEquals(listOf(puzzle.tents), found)
    }

    @Test
    fun theReasoningOnlyGivesTentsThatAreInTheSolution() {
        for (seed in 0 until 40) {
            val puzzle = TentsGenerator.generate(seed)
            val derived = TentsSolver.deduce(puzzle.size, puzzle.trees, puzzle.rowTargets, puzzle.columnTargets, emptySet())
            assertTrue(puzzle.tents.containsAll(derived), "seed $seed: $derived against ${puzzle.tents}")
        }
    }

    @Test
    fun theReasoningGivenSomeRightTentsOnlyGivesTentsOfTheSolution() {
        for (seed in 0 until 20) {
            val puzzle = TentsGenerator.generate(seed)
            val known = puzzle.tents.take(3).toSet()
            val derived = TentsSolver.deduce(puzzle.size, puzzle.trees, puzzle.rowTargets, puzzle.columnTargets, known)
            assertTrue(puzzle.tents.containsAll(derived + known))
            assertTrue(derived.none { it in known })
        }
    }

    @Test
    fun theReasoningFindsATentWhenATreeHasOnlyOnePlaceLeft() {
        // A tree in a corner with tents allowed only below it: the tent is certain.
        val derived = TentsSolver.deduce(4, setOf(0), listOf(0, 1, 0, 0), listOf(1, 0, 0, 0), emptySet())
        assertEquals(listOf(4), derived)
    }
}
