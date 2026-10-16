package com.mehmtcan.brainscroll.game.line

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LineSolverTest {

    /** Every line by plain trying, with no cutting of branches: the rules only. */
    private fun bruteCount(size: Int, checkpoints: IntArray): Int {
        val cells = size * size
        val number = IntArray(cells).also { n -> checkpoints.forEachIndexed { i, c -> n[c] = i + 1 } }
        val visited = BooleanArray(cells)
        var count = 0
        fun walk(head: Int, depth: Int, reached: Int) {
            if (depth == cells) { count++; return }
            val row = head / size
            val column = head % size
            val next = buildList {
                if (row > 0) add(head - size)
                if (row < size - 1) add(head + size)
                if (column > 0) add(head - 1)
                if (column < size - 1) add(head + 1)
            }
            for (cell in next) {
                if (visited[cell]) continue
                val n = number[cell]
                if (n > 0 && (n != reached + 1 || (n == checkpoints.size && depth != cells - 1))) continue
                visited[cell] = true
                walk(cell, depth + 1, if (n > 0) reached + 1 else reached)
                visited[cell] = false
            }
        }
        visited[checkpoints[0]] = true
        walk(checkpoints[0], 1, 1)
        return count
    }

    @Test
    fun theCountAgreesWithPlainTryingOnManySmallBoards() {
        val random = SplitMix64(11)
        var solvable = 0
        repeat(150) {
            val size = 3 + random.nextInt(2)
            val cells = size * size
            val wanted = 2 + random.nextInt(3)
            val checkpoints = mutableListOf<Int>()
            while (checkpoints.size < wanted) {
                val cell = random.nextInt(cells)
                if (cell !in checkpoints) checkpoints += cell
            }
            val expected = bruteCount(size, checkpoints.toIntArray())
            assertEquals(expected, LineSolver.count(size, checkpoints.toIntArray(), limit = Int.MAX_VALUE), "size $size numbers $checkpoints")
            assertEquals(minOf(expected, 2), LineSolver.count(size, checkpoints.toIntArray(), limit = 2))
            if (expected > 0) solvable++
        }
        assertTrue(solvable > 20, "the sample holds boards with lines too: $solvable")
    }

    @Test
    fun aSolveFindsALineThatFollowsTheRules() {
        val puzzle = LineFixtures.board
        val line = assertNotNull(LineSolver.solve(puzzle.size, puzzle.checkpoints))
        assertContentEquals(puzzle.solution, line, "the board has one line, so it is that one")
    }

    @Test
    fun aSolveContinuesAPrefixAndRefusesOneThatBreaksARuleOrLeadsNowhere() {
        val puzzle = LineFixtures.board
        val solution = puzzle.solution
        val half = solution.copyOf(solution.size / 2)
        assertContentEquals(solution, LineSolver.solve(puzzle.size, puzzle.checkpoints, half))
        assertNull(LineSolver.solve(puzzle.size, puzzle.checkpoints, intArrayOf(solution[1])), "does not begin on number 1")
        assertNull(LineSolver.solve(puzzle.size, puzzle.checkpoints, intArrayOf(solution[0], solution[2])), "a jump")
        assertNull(LineSolver.solve(puzzle.size, puzzle.checkpoints, intArrayOf(solution[0], solution[0])), "a cell twice")
    }

    @Test
    fun aBoardWithNoLineHasNoSolution() {
        // Number 2 on the middle of a 3x3 board and the end on a corner: the parity of the grid does not fit.
        assertEquals(0, LineSolver.count(3, intArrayOf(0, 4, 1)))
        assertNull(LineSolver.solve(3, intArrayOf(0, 4, 1)))
    }
}
