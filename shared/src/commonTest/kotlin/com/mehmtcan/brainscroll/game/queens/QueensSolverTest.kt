package com.mehmtcan.brainscroll.game.queens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueensSolverTest {

    @Test
    fun aFiveByFiveBoardHasTenSolutionsAndAnEightByEightBoardNinetyTwo() {
        assertEquals(10, QueensSolver.solutions(5).size)
        assertEquals(92, QueensSolver.solutions(8).size)
        assertEquals(2, QueensSolver.solutions(4).size)
    }

    @Test
    fun everySolutionHasNoAttackingPair() {
        for (solution in QueensSolver.solutions(6)) {
            val queens = BooleanArray(36)
            for (cell in solution) queens[cell] = true
            assertTrue(QueensSolver.attacked(6, queens).none { it })
        }
    }

    @Test
    fun solutionsKeepTheFixedQueens() {
        val all = QueensSolver.solutions(5)
        val fixed = setOf(all.first()[0], all.first()[2])
        val withFixed = QueensSolver.solutions(5, fixed)
        assertTrue(withFixed.isNotEmpty())
        assertTrue(withFixed.all { solution -> fixed.all { it in solution } })
        assertEquals(all.count { solution -> fixed.all { it in solution } }, withFixed.size)
    }

    @Test
    fun fixedQueensThatAttackEachOtherLeaveNoSolution() {
        assertTrue(QueensSolver.solutions(5, setOf(0, 6)).isEmpty())
    }
}
