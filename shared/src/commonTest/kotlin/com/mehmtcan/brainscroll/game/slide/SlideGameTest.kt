package com.mehmtcan.brainscroll.game.slide

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlideGameTest {

    private val seeds = (1..40).toList()

    private fun game(seed: Int = 3) = SlideGame(seed)

    /** The number of pairs of tiles that are in the wrong order, the empty cell left out. */
    private fun inversions(tiles: List<Int>): Int {
        val list = tiles.filter { it != 0 }
        var count = 0
        for (i in list.indices) for (j in i + 1 until list.size) if (list[i] > list[j]) count++
        return count
    }

    private fun SlideGame.cells(number: Int) = state.tiles.indexOf(number)

    @Test
    fun everyBoardHasTheRightTilesAndIsNotSolvedYet() {
        for (seed in seeds) {
            val g = game(seed)
            assertEquals((0..11).toList(), g.state.tiles.sorted(), "seed $seed")
            assertFalse(g.isFinished, "seed $seed")
            assertTrue(SlideGame.distance(g.state.tiles.toIntArray(), 3) >= 14, "seed $seed is too easy")
        }
    }

    @Test
    fun everyBoardCanBeSolved() {
        // With an odd number of columns a board is solvable exactly when its tiles have an even number of inversions.
        for (seed in seeds) {
            assertEquals(0, inversions(game(seed).state.tiles) % 2, "seed $seed")
        }
        // And a real solver finds the way, taking the taps one by one.
        for (seed in seeds.take(8)) {
            val g = game(seed)
            val taps = SlideSolver.solve(g.state.tiles, 3, 4)
            for ((i, cell) in taps.withIndex()) {
                val result = g.tap(cell)
                assertEquals(if (i == taps.size - 1) SlideResult.Solved else SlideResult.Moved, result, "seed $seed step $i")
            }
            assertTrue(g.isFinished)
            assertEquals(taps.size, g.state.moves)
        }
    }

    @Test
    fun theSameSeedAlwaysGivesTheSameBoardAndOtherSeedsGiveOtherOnes() {
        assertContentEquals(SlideGame.generate(9, 3, 4, 80), SlideGame.generate(9, 3, 4, 80))
        assertTrue(!SlideGame.generate(9, 3, 4, 80).contentEquals(SlideGame.generate(10, 3, 4, 80)))
    }

    @Test
    fun aTapNextToTheEmptyCellSlidesThatTileIntoIt() {
        val g = game()
        val blank = g.cells(0)
        val neighbor = if (blank % 3 > 0) blank - 1 else blank + 1
        val number = g.state.tiles[neighbor]
        assertEquals(SlideResult.Moved, g.tap(neighbor))
        assertEquals(blank, g.cells(number))
        assertEquals(neighbor, g.cells(0))
        assertEquals(1, g.state.moves)
    }

    @Test
    fun aTapFurtherAlongTheColumnSlidesAllTilesBetweenAsOneMove() {
        val g = game()
        val blank = g.cells(0)
        val far = (if (blank / 3 <= 1) blank / 3 + 2 else blank / 3 - 2) * 3 + blank % 3
        val step = if (far < blank) 3 else -3
        val line = generateSequence(far) { it + step }.takeWhile { it != blank }.toList()
        assertEquals(2, line.size)
        val before = g.state.tiles
        assertEquals(SlideResult.Moved, g.tap(far))
        val after = g.state.tiles
        for (cell in line) assertEquals(before[cell], after[cell + step], "the tile from $cell moved one step")
        assertEquals(0, after[far])
        assertEquals(1, g.state.moves)
    }

    @Test
    fun tilesNotInLineWithTheEmptyCellAndTheEmptyCellItselfAreIgnored() {
        val g = game()
        val blank = g.cells(0)
        val off = (0 until 12).first { it / 3 != blank / 3 && it % 3 != blank % 3 }
        val before = g.state.tiles
        assertEquals(SlideResult.Ignored, g.tap(off))
        assertEquals(SlideResult.Ignored, g.tap(blank))
        assertEquals(SlideResult.Ignored, g.tap(-1))
        assertEquals(SlideResult.Ignored, g.tap(12))
        assertEquals(before, g.state.tiles)
        assertEquals(0, g.state.moves)
    }

    @Test
    fun theBoardTakesNothingMoreOnceSolved() {
        val g = game()
        for (cell in SlideSolver.solve(g.state.tiles, 3, 4)) g.tap(cell)
        assertEquals(SlideStatus.Won, g.state.status)
        assertEquals(SlideResult.Ignored, g.tap(8))
        assertFalse(g.restart())
        assertFalse(g.state.canRestart)
    }

    @Test
    fun restartPutsTheTilesBackButKeepsTheMoves() {
        val g = game()
        val start = g.state.tiles
        assertFalse(g.state.canRestart)
        assertFalse(g.restart(), "nothing to undo yet")
        val blank = g.cells(0)
        g.tap(if (blank >= 3) blank - 3 else blank + 3)
        assertTrue(g.state.canRestart)
        assertTrue(g.restart())
        assertEquals(start, g.state.tiles)
        assertEquals(1, g.state.moves)
        assertFalse(g.state.canRestart)
    }

    @Test
    fun aTileIsHomeWhenItsNumberMatchesItsCell() {
        val g = game()
        for (cell in 0 until 12) assertEquals(g.state.tiles[cell] == cell + 1, g.state.isHome(cell))
    }

    @Test
    fun aGameNeedsSaneSettings() {
        assertFailsWith<IllegalArgumentException> { SlideGame(1, cols = 1) }
        assertFailsWith<IllegalArgumentException> { SlideGame(1, rows = 1) }
        assertFailsWith<IllegalArgumentException> { SlideGame(1, scramble = 0) }
    }
}
