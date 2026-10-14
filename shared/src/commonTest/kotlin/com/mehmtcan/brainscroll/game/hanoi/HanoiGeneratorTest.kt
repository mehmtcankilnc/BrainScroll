package com.mehmtcan.brainscroll.game.hanoi

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HanoiGeneratorTest {

    private val seeds = (1..200).toList()

    @Test
    fun everyPuzzleIsASpreadOfFourDisksNotYetSolvedAndNotTooEasy() {
        for (seed in seeds) {
            val puzzle = HanoiGenerator.generate(seed)
            assertEquals(4, puzzle.start.size)
            assertTrue(puzzle.start.all { it in 0..2 } && puzzle.goal in 0..2, "seed $seed")
            assertFalse(puzzle.start.all { it == puzzle.goal }, "seed $seed starts solved")
            assertTrue(puzzle.par >= HanoiGenerator.DEFAULT_MIN_PAR, "seed $seed par ${puzzle.par}")
            assertEquals(HanoiSolver.distance(puzzle.start, puzzle.goal), puzzle.par, "seed $seed")
        }
    }

    @Test
    fun theSameSeedAlwaysGivesTheSamePuzzleAndOtherSeedsOtherOnes() {
        val a = HanoiGenerator.generate(7)
        val b = HanoiGenerator.generate(7)
        assertContentEquals(a.start, b.start)
        assertEquals(a.goal, b.goal)
        assertEquals(a.par, b.par)
        val distinct = seeds.map { HanoiGenerator.generate(it) }.map { it.start.toList() to it.goal }.toSet()
        assertTrue(distinct.size > 40, "the puzzles differ: ${distinct.size}")
    }

    @Test
    fun theGoalTowerAndTheDifficultyAreSpreadOut() {
        val puzzles = seeds.map { HanoiGenerator.generate(it) }
        assertEquals(setOf(0, 1, 2), puzzles.map { it.goal }.toSet())
        assertTrue(puzzles.map { it.par }.toSet().size >= 6, "pars ${puzzles.map { it.par }.toSortedSet()}")
        assertTrue(puzzles.average { it.par.toDouble() } in 7.0..13.0, "the average par is ${puzzles.average { it.par.toDouble() }}")
    }

    private inline fun <T> List<T>.average(value: (T) -> Double) = sumOf(value) / size

    @Test
    fun aHigherMinimumGivesHarderPuzzles() {
        for (seed in 1..20) assertTrue(HanoiGenerator.generate(seed, minPar = 11).par >= 11, "seed $seed")
    }

    @Test
    fun otherNumbersOfDisksWork() {
        for (disks in 2..6) assertEquals(disks, HanoiGenerator.generate(3, disks = disks, minPar = 1).start.size)
        assertFailsWith<IllegalArgumentException> { HanoiGenerator.generate(1, disks = 1) }
    }
}
