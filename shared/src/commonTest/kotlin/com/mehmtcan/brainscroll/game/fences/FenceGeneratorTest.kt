package com.mehmtcan.brainscroll.game.fences

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FenceGeneratorTest {
    @Test
    fun everyLevelMakesBoardsWithExactlyOneLoop() {
        for (difficulty in Difficulty.entries) {
            val shape = GameLevels.fences(difficulty)
            // The search for every loop of the big board is slow in a test: the small ones are searched, the big one is checked by the rules.
            val seeds = if (shape.size <= 6) 20 else 4
            for (seed in 0 until seeds) {
                val puzzle = FenceGenerator.generate(seed, difficulty)
                assertEquals(shape.size, puzzle.size)
                assertTrue(puzzle.clueCount <= shape.clues + 8, "$difficulty seed $seed has ${puzzle.clueCount} numbers")
                val solutions = FenceSolver.solutions(puzzle, 3)
                assertEquals(1, solutions.size, "$difficulty seed $seed has one loop")
                assertTrue(FenceRules.isSolved(puzzle, solutions[0]), "$difficulty seed $seed")
            }
        }
    }

    @Test
    fun aBoardCanBeFinishedTheWayAPlayerDoesIt() {
        for (difficulty in Difficulty.entries) {
            val shape = GameLevels.fences(difficulty)
            for (seed in 0 until 15) {
                val puzzle = FenceGenerator.generate(seed, difficulty)
                val byLogic = assertNotNull(FenceSolver.solveByLogic(puzzle, shape.lookahead), "$difficulty seed $seed")
                assertTrue(FenceRules.isSolved(puzzle, BooleanArray(byLogic.size) { byLogic[it] == FenceSolver.LINE }))
            }
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoard() {
        for (difficulty in Difficulty.entries) assertEquals(FenceGenerator.generate(7, difficulty), FenceGenerator.generate(7, difficulty))
    }

    @Test
    fun aBoardIsSavedAndReadBackInAtMost32Characters() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 25) {
                val puzzle = FenceGenerator.generate(seed, difficulty)
                val text = puzzle.encode()
                assertTrue(text.length <= 32, "$difficulty seed $seed: ${text.length} characters")
                assertEquals(puzzle, FencePuzzle.decode(text))
            }
        }
        assertNull(FencePuzzle.decode(""))
        assertNull(FencePuzzle.decode("!!!"))
        assertNull(FencePuzzle.decode("000"), "a board that runs out")
    }

    @Test
    fun aBoardThatCannotBeIsRefused() {
        fun refused(block: () -> FencePuzzle) = runCatching(block).isFailure
        assertTrue(refused { FencePuzzle(3, List(9) { -1 }) }, "too small")
        assertTrue(refused { FencePuzzle(10, List(100) { -1 }) }, "too big")
        assertTrue(refused { FencePuzzle(4, List(15) { -1 }) })
        assertTrue(refused { FencePuzzle(4, List(16) { 4 }) }, "a number above 3")
        assertTrue(!FencePuzzle(9, List(81) { 3 }).canBeSaved, "81 numbers do not fit 32 characters")
        assertTrue(FencePuzzle(8, List(64) { if (it < 60) 1 else -1 }).canBeSaved)
    }

    @Test
    fun theGeometryNumbersEveryEdgeAndDot() {
        val g = FenceGeometry.of(4)
        assertEquals(25, g.dots)
        assertEquals(40, g.edges)
        // The top edge of the first square, and its left edge.
        assertEquals(0, g.cellEdges[0][0])
        assertEquals(g.horizontal, g.cellEdges[0][2])
        // Every edge is a side of one or two squares, every dot has two to four edges.
        for (e in 0 until g.edges) assertTrue(g.edgeCells[e].size in 1..2)
        for (d in 0 until g.dots) assertTrue(g.dotEdges[d].size in 2..4)
    }

    @Test
    fun aBoardWithTwoLoopsIsNotUnique() {
        val puzzle = FencePuzzle(4, List(16) { -1 })
        assertTrue(FenceSolver.solutions(puzzle, 5).size > 1)
        assertNull(FenceSolver.uniqueSolution(puzzle))
    }

    @Test
    fun theRulesSeeBranchesNumbersAndLoops() {
        val puzzle = FencePuzzle(4, List(16) { if (it == 0) 2 else -1 })
        val g = puzzle.geometry
        val lines = BooleanArray(g.edges)
        // Three sides of the first square: more than its 2.
        lines[g.cellEdges[0][0]] = true
        lines[g.cellEdges[0][1]] = true
        lines[g.cellEdges[0][2]] = true
        assertEquals(setOf(0), FenceRules.overfull(puzzle, lines))
        assertTrue(FenceRules.satisfied(puzzle, lines).isEmpty())
        assertTrue(!FenceRules.isLoop(puzzle, lines))
        // Three lines at one dot branch.
        val star = BooleanArray(g.edges)
        for (e in g.dotEdges[g.dotSide + 1]) star[e] = true
        assertEquals(setOf(g.dotSide + 1), FenceRules.branching(puzzle, star))
        // The border of the first square is a loop.
        val ring = BooleanArray(g.edges)
        for (e in g.cellEdges[0]) ring[e] = true
        assertTrue(FenceRules.isLoop(puzzle, ring))
        assertTrue(!FenceRules.isSolved(puzzle, ring), "the number 2 is not met by four sides")
        // Two loops apart are not one loop.
        val two = ring.copyOf()
        for (e in g.cellEdges[g.size * 3 + 3]) two[e] = true
        assertTrue(!FenceRules.isLoop(puzzle, two))
    }

    @Test
    fun theRulesForceTheEdgesThePlayerSees() {
        // A 0 leaves no line on its four sides.
        val puzzle = FencePuzzle(4, List(16) { if (it == 5) 0 else -1 })
        val marks = IntArray(puzzle.geometry.edges)
        assertTrue(FenceSolver.propagate(puzzle, marks))
        for (e in puzzle.geometry.cellEdges[5]) assertEquals(FenceSolver.CROSS, marks[e])
        // A lone 3 in a corner of a board forces nothing.
        val corner = FencePuzzle(4, List(16) { if (it == 0) 3 else -1 })
        val m = IntArray(corner.geometry.edges)
        assertTrue(FenceSolver.propagate(corner, m))
        // Nothing is forced yet by a lone 3 in the corner: a 3 leaves one side free out of four, which is not enough to say which.
        assertEquals(4, corner.geometry.cellEdges[0].count { m[it] == FenceSolver.UNKNOWN })
    }

    @Test
    fun aRandomLoopIsOneClosedLoop() {
        for (n in 4..8) for (seed in 0 until 10) {
            val puzzle = FencePuzzle(n, List(n * n) { -1 })
            assertTrue(FenceRules.isLoop(puzzle, FenceGenerator.randomLoop(Random(seed), n)), "size $n seed $seed")
        }
    }

    @Test
    fun theSolverGivesTheSameLoopAsTheSearch() {
        for (seed in 0 until 8) {
            val puzzle = FenceGenerator.generate(seed, Difficulty.Easy)
            val byLogic = assertNotNull(FenceSolver.solveByLogic(puzzle, false))
            val loop = assertNotNull(FenceSolver.uniqueSolution(puzzle))
            assertContentEquals(loop, BooleanArray(byLogic.size) { byLogic[it] == FenceSolver.LINE })
        }
    }
}
