package com.mehmtcan.brainscroll.game.sudoku

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JigsawGeneratorTest {

    @Test
    fun aLayoutHasNineConnectedRegionsOfNineCells() {
        for (seed in 0 until 50) {
            val layout = JigsawGenerator.randomLayout(Random(seed))
            for (region in 0 until 9) {
                assertEquals(9, layout.regionOf.count { it == region }, "seed $seed region $region")
                assertTrue(JigsawGenerator.connected(layout.regionOf, region), "seed $seed region $region is in one piece")
            }
        }
    }

    @Test
    fun theLayoutsAreNotTheClassicBoxesNorTheRowsAndTheyDiffer() {
        val layouts = (0 until 20).map { JigsawGenerator.randomLayout(Random(it)) }
        assertTrue(layouts.none { it.isStandard })
        assertTrue(layouts.map { it.regionOf.toList() }.toSet().size == 20)
        // Not nine stripes: most regions reach into more than one row.
        for (layout in layouts) {
            val spread = (0 until 9).count { region -> (0 until 81).filter { layout.regionOf[it] == region }.map { it / 9 }.toSet().size > 1 }
            assertTrue(spread >= 6, "regions are mixed over rows: $spread")
        }
    }

    @Test
    fun aBoardIsValidAndSolvableBySinglesAndKeepsItsGivens() {
        for (seed in 0 until 30) {
            val puzzle = JigsawGenerator.generate(seed)
            val layout = puzzle.layout
            // The full grid follows the rules for rows, columns and regions.
            for (unit in layout.units) assertEquals((1..9).toSet(), unit.map { puzzle.solution[it] }.toSet(), "seed $seed")
            // The givens are a part of the solution, and singles finish the board.
            for (cell in 0 until 81) if (puzzle.givens[cell] != 0) assertEquals(puzzle.solution[cell], puzzle.givens[cell])
            assertEquals(puzzle.solution.toList(), assertNotNull(SudokuGenerator.solveBySingles(puzzle.givens, layout)).toList(), "seed $seed")
            assertTrue(puzzle.givens.count { it != 0 } >= JigsawGenerator.DEFAULT_CLUES, "seed $seed keeps at least the floor")
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoard() {
        val a = JigsawGenerator.generate(11)
        val b = JigsawGenerator.generate(11)
        assertEquals(a.layout.regionOf.toList(), b.layout.regionOf.toList())
        assertEquals(a.givens.toList(), b.givens.toList())
        assertEquals(a.solution.toList(), b.solution.toList())
    }

    @Test
    fun theBoardsAreCutDownWellBelowTheClassicNumberOfGivensWhenSinglesAllowIt() {
        val counts = (0 until 20).map { seed -> JigsawGenerator.generate(seed).givens.count { it != 0 } }
        assertTrue(counts.all { it in JigsawGenerator.DEFAULT_CLUES..60 }, "$counts")
    }
}
