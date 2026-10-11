package com.mehmtcan.brainscroll.game.pipes

import com.mehmtcan.brainscroll.game.pipes.PipesGenerator.EAST
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator.NORTH
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator.SOUTH
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator.WEST
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PipesGeneratorTest {

    private val seeds = (1..60).toList()

    /** The fewest taps by trying every combination of turns, for small boards only (4^cells). */
    private fun bruteMinTaps(start: IntArray, size: Int): Int? {
        var best: Int? = null
        val turns = IntArray(start.size)
        fun walk(cell: Int) {
            if (cell == start.size) {
                val tiles = IntArray(start.size) { var t = start[it]; repeat(turns[it]) { t = PipesGenerator.rotate(t) }; t }
                if (PipesGenerator.isSolved(tiles, size)) {
                    val cost = turns.sum()
                    if (best == null || cost < best!!) best = cost
                }
                return
            }
            for (k in 0..3) { turns[cell] = k; walk(cell + 1) }
        }
        walk(0)
        return best
    }

    private fun turned(start: IntArray, turns: IntArray) = IntArray(start.size) { var t = start[it]; repeat(turns[it]) { t = PipesGenerator.rotate(t) }; t }

    @Test
    fun aQuarterTurnMovesEverySideOneStepRoundClockwise() {
        assertEquals(EAST, PipesGenerator.rotate(NORTH))
        assertEquals(SOUTH, PipesGenerator.rotate(EAST))
        assertEquals(WEST, PipesGenerator.rotate(SOUTH))
        assertEquals(NORTH, PipesGenerator.rotate(WEST))
        assertEquals(EAST or SOUTH, PipesGenerator.rotate(NORTH or EAST))
        for (tile in 0..15) {
            var t = tile
            repeat(4) { t = PipesGenerator.rotate(t) }
            assertEquals(tile, t, "four turns are no turn")
        }
    }

    @Test
    fun theFinishedBoardIsATreeThatReachesEveryCellWithNoFourWayCell() {
        for (seed in seeds) {
            val size = 5
            val tiles = PipesGenerator.spanningTree(Random(seed), size)
            val pipes = tiles.sumOf { it.countOneBits() } / 2
            assertEquals(size * size - 1, pipes, "a tree has one pipe less than it has cells, seed $seed")
            assertTrue(tiles.all { it.countOneBits() in 1..3 }, "no cell without a pipe or with four, seed $seed")
            assertTrue(PipesGenerator.isSolved(tiles, size), "every side meets a side, seed $seed")
            // Reaching every cell: a walk from cell 0 along the pipes visits them all.
            val seen = mutableSetOf(0)
            val stack = ArrayDeque(listOf(0))
            while (stack.isNotEmpty()) {
                val cell = stack.removeLast()
                val next = buildList {
                    if (tiles[cell] and NORTH != 0) add(cell - size)
                    if (tiles[cell] and SOUTH != 0) add(cell + size)
                    if (tiles[cell] and EAST != 0) add(cell + 1)
                    if (tiles[cell] and WEST != 0) add(cell - 1)
                }
                for (n in next) if (seen.add(n)) stack += n
            }
            assertEquals(size * size, seen.size, "seed $seed")
        }
    }

    @Test
    fun theFinishedBoardMixesAllFourKindsOfTile() {
        val kinds = (1..40).flatMap { PipesGenerator.spanningTree(Random(it), 5).toList() }.groupBy { it.countOneBits() }.keys
        assertEquals(setOf(1, 2, 3), kinds)
    }

    @Test
    fun theFewestTapsAgreeWithABruteForceOnManySmallBoards() {
        val random = Random(3)
        var solvable = 0
        repeat(40) {
            val start = IntArray(9) { random.nextInt(16) }
            val expected = bruteMinTaps(start, 3)
            assertEquals(expected, PipesGenerator.minTaps(start, 3), "tiles ${start.toList()}")
            if (expected != null) solvable++
        }
        // And boards made from a tree are always solvable.
        repeat(20) {
            val puzzle = PipesGenerator.generate(random.nextInt(), size = 3, minPar = 0)
            assertEquals(bruteMinTaps(puzzle.start, 3), puzzle.par)
            assertNotNull(PipesGenerator.minTaps(puzzle.start, 3))
        }
    }

    @Test
    fun aBoardWithNoWayToCloseTheEndsHasNoSolution() {
        assertNull(PipesGenerator.minTaps(IntArray(9) { NORTH }, 3), "nine end pieces can not pair up")
        assertEquals(0, PipesGenerator.minTaps(IntArray(9), 3), "a board of closed tiles has no ends to close")
    }

    @Test
    fun theSolutionIsARealWayToCloseEveryEndAndHasThatManyTaps() {
        for (seed in seeds) {
            val puzzle = PipesGenerator.generate(seed)
            val turns = assertNotNull(PipesGenerator.solve(puzzle.start, 5), "seed $seed")
            assertEquals(puzzle.par, turns.sum(), "seed $seed")
            assertTrue(PipesGenerator.isSolved(turned(puzzle.start, turns), 5), "seed $seed")
        }
    }

    @Test
    fun everyPuzzleIsAShuffledTreeNotYetSolvedAndNotTooEasy() {
        for (seed in seeds) {
            val puzzle = PipesGenerator.generate(seed)
            assertEquals(25, puzzle.start.size)
            assertFalse(PipesGenerator.isSolved(puzzle.start, 5), "seed $seed")
            assertTrue(puzzle.par >= PipesGenerator.DEFAULT_MIN_PAR, "seed $seed par ${puzzle.par}")
            assertEquals(24, puzzle.start.sumOf { it.countOneBits() } / 2, "the shuffling keeps the number of pipes, seed $seed")
        }
    }

    @Test
    fun theSameSeedAlwaysGivesTheSamePuzzleAndOtherSeedsOtherOnes() {
        assertContentEquals(PipesGenerator.generate(7).start, PipesGenerator.generate(7).start)
        assertEquals(PipesGenerator.generate(7).par, PipesGenerator.generate(7).par)
        assertTrue(!PipesGenerator.generate(7).start.contentEquals(PipesGenerator.generate(8).start))
    }

    @Test
    fun aHigherMinimumGivesHarderBoards() {
        for (seed in 1..10) {
            assertTrue(PipesGenerator.generate(seed, minPar = 30).par >= 30, "seed $seed")
        }
    }

    @Test
    fun theParsAreSpreadOutAndTheBoardsAreQuickToMake() {
        val pars = seeds.map { PipesGenerator.generate(it).par }
        assertTrue(pars.max() > pars.min() + 5, "the pars differ: ${pars.min()}..${pars.max()}")
        assertTrue(pars.average() in 20.0..40.0, "an average puzzle takes a fair number of taps: ${pars.average()}")
    }

    @Test
    fun aBoardCanNotBeTooSmall() {
        kotlin.test.assertFailsWith<IllegalArgumentException> { PipesGenerator.generate(1, size = 2) }
    }
}
