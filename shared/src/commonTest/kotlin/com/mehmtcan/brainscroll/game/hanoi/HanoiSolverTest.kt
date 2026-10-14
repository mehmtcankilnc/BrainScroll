package com.mehmtcan.brainscroll.game.hanoi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HanoiSolverTest {

    /** Every position of [disks] disks: for each disk (the smallest first) the tower it is on. */
    private fun positions(disks: Int): List<IntArray> {
        var all = listOf(IntArray(0))
        repeat(disks) { all = all.flatMap { p -> (0 until 3).map { tower -> p + tower } } }
        return all
    }

    /** The classic closed form, worked out from the biggest disk down, as an independent check of the search. */
    private fun formula(pegs: IntArray, goal: Int): Int {
        fun f(disk: Int, target: Int): Int {
            if (disk < 0) return 0
            if (pegs[disk] == target) return f(disk - 1, target)
            val other = 3 - pegs[disk] - target
            return f(disk - 1, other) + (1 shl disk)
        }
        return f(pegs.size - 1, goal)
    }

    @Test
    fun theWholeStackOnOneTowerNeedsTwoToTheNMinusOneMoves() {
        for (disks in 2..6) assertEquals((1 shl disks) - 1, HanoiSolver.distance(IntArray(disks) { 0 }, 2), "$disks disks")
        assertEquals(15, HanoiSolver.distance(IntArray(4) { 1 }, 0))
    }

    @Test
    fun aSolvedPositionNeedsNoMove() {
        assertTrue(HanoiSolver.shortest(IntArray(4) { 2 }, 2).isEmpty())
    }

    @Test
    fun theSearchAgreesWithTheClosedFormOnEveryPositionOfFourDisks() {
        for (pegs in positions(4)) for (goal in 0..2) {
            assertEquals(formula(pegs, goal), HanoiSolver.distance(pegs, goal), "pegs ${pegs.toList()} goal $goal")
        }
    }

    @Test
    fun theMovesOfAShortestWayAreLegalAndEndOnTheGoal() {
        for (pegs in positions(4).filterIndexed { i, _ -> i % 7 == 0 }) for (goal in 0..2) {
            val now = pegs.copyOf()
            for (move in HanoiSolver.shortest(pegs, goal)) {
                val disk = HanoiSolver.topDisk(now, move.from)
                assertTrue(disk >= 0 && HanoiSolver.canGo(now, disk, move.to), "an illegal move $move from ${pegs.toList()}")
                now[disk] = move.to
            }
            assertTrue(now.all { it == goal })
        }
    }

    @Test
    fun theLegalMovesFollowTheRules() {
        // Disk 0 (smallest) and 2 on tower 0, disk 1 on tower 1: the top of tower 0 is disk 0, of tower 1 is disk 1.
        val pegs = intArrayOf(0, 1, 0)
        val moves = HanoiSolver.legalMoves(pegs).toSet()
        assertEquals(setOf(HanoiMove(0, 1), HanoiMove(0, 2), HanoiMove(1, 2)), moves, "disk 1 cannot go onto disk 0, nothing moves from the empty tower")
        assertEquals(-1, HanoiSolver.topDisk(pegs, 2))
    }
}
