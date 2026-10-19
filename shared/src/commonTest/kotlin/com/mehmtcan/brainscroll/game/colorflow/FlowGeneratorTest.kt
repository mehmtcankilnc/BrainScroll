package com.mehmtcan.brainscroll.game.colorflow

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FlowGeneratorTest {

    private fun coversEverything(n: Int, paths: List<List<Int>>): Boolean {
        val cells = paths.flatten()
        return cells.size == n * n && cells.toSet().size == n * n
    }

    private fun connected(n: Int, path: List<Int>) = (1 until path.size).all { kotlin.math.abs(path[it] / n - path[it - 1] / n) + kotlin.math.abs(path[it] % n - path[it - 1] % n) == 1 }

    @Test
    fun everyBoardOfEveryLevelHasAWayThatJoinsEveryColorAndCoversTheBoard() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.colorFlow(level)
            for (seed in 0 until 25) {
                val board = FlowGenerator.generate(seed, level)
                val p = board.puzzle
                assertEquals(shape.size, p.size)
                assertTrue(p.colorCount in shape.minColors..shape.maxColors, "$level seed $seed colors ${p.colorCount}")
                assertTrue(coversEverything(p.size, board.solution), "$level seed $seed covers")
                board.solution.forEachIndexed { color, path ->
                    assertTrue(connected(p.size, path), "$level seed $seed path $color")
                    assertEquals(p.pairs[color].first, path.first())
                    assertEquals(p.pairs[color].second, path.last())
                }
            }
        }
    }

    @Test
    fun theSolverFindsAWayForEveryBoardAndTheSmallerBoardsHaveOnlyOne() {
        for (level in Difficulty.entries) {
            var unique = 0
            for (seed in 0 until 15) {
                val p = FlowGenerator.generate(seed, level).puzzle
                val way = assertNotNull(FlowSolver.solve(p), "$level seed $seed")
                assertTrue(coversEverything(p.size, way))
                val search = FlowSolver.search(p, 2, 60_000)
                if (search.complete && search.solutions.size == 1) unique++
            }
            // The big boards fall back to a board that can be won in more than one way when the search finds no single one in time.
            if (level != Difficulty.Hard) assertEquals(15, unique, "$level boards that have one way")
        }
    }

    @Test
    fun theSameSeedGivesTheSameBoardAndOthersDiffer() {
        assertEquals(FlowGenerator.generate(5, Difficulty.Medium).puzzle.encode(), FlowGenerator.generate(5, Difficulty.Medium).puzzle.encode())
        val boards = (0 until 30).map { FlowGenerator.generate(it, Difficulty.Medium).puzzle.encode() }.toSet()
        assertTrue(boards.size >= 29, "boards repeat: ${boards.size} of 30")
    }

    @Test
    fun theSavedTextFitsTheServerLimitAndReadsBack() {
        for (level in Difficulty.entries) {
            for (seed in 0 until 25) {
                val p = FlowGenerator.generate(seed, level).puzzle
                val text = p.encode()
                assertTrue(text.length <= 32, "$text is ${text.length} long")
                val back = assertNotNull(FlowPuzzle.decode(text))
                assertEquals(p.pairs, back.pairs)
                assertEquals(p.size, back.size)
            }
        }
    }
}
