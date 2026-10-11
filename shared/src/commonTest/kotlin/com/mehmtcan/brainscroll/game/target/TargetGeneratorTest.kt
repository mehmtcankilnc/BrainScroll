package com.mehmtcan.brainscroll.game.target

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TargetGeneratorTest {

    @Test
    fun anOperationOnlyGivesWholeNumbersAboveZero() {
        assertEquals(9, Op.Add.apply(4, 5))
        assertEquals(20, Op.Mul.apply(4, 5))
        assertEquals(1, Op.Sub.apply(5, 4))
        assertNull(Op.Sub.apply(4, 5), "below zero")
        assertNull(Op.Sub.apply(5, 5), "zero")
        assertEquals(3, Op.Div.apply(6, 2))
        assertNull(Op.Div.apply(7, 2), "a rest is left")
        assertNull(Op.Div.apply(2, 6), "a fraction")
        assertNull(Op.Div.apply(5, 0), "no division by zero")
        assertEquals(5, Op.Div.apply(5, 1))
    }

    @Test
    fun theSymbolsAreSavedAndReadBack() {
        for (op in Op.entries) assertEquals(op, Op.fromSymbol(op.symbol))
        assertNull(Op.fromSymbol('x'))
    }

    @Test
    fun theExampleOfTheRules() {
        val reachable = TargetGenerator.reachable(listOf(2, 3, 6, 7))
        assertEquals(3, reachable[48], "6*2, 7-3, then 12*4")
        assertEquals(1, reachable[13], "6+7")
        assertEquals(3, TargetGenerator.minSteps(listOf(2, 3, 6, 7), 48))
        assertNull(TargetGenerator.minSteps(listOf(2, 3, 6, 7), 3), "a target that is already on the table is not a target")
        assertNull(TargetGenerator.minSteps(listOf(1, 1, 1, 1), 99))
    }

    @Test
    fun theSearchAgreesWithAPlainBruteForceOnManyNumberSets() {
        val random = Random(11)
        repeat(40) {
            val numbers = List(4) { random.nextInt(1, 10) }
            val fast = TargetGenerator.reachable(numbers)
            val slow = TargetSolver.bruteReachable(numbers)
            assertEquals(slow, fast, "numbers $numbers")
        }
    }

    @Test
    fun everyPuzzleHasATwoDigitTargetThatIsNotOnTheTableAndNeedsSomeWork() {
        for (seed in 1..300) {
            val puzzle = TargetGenerator.generate(seed)
            assertEquals(4, puzzle.numbers.size)
            assertTrue(puzzle.numbers.all { it in 1..9 }, "seed $seed")
            assertTrue(puzzle.target in 10..99, "seed $seed target ${puzzle.target}")
            assertTrue(puzzle.target !in puzzle.numbers, "seed $seed")
            assertTrue(puzzle.par >= TargetGenerator.DEFAULT_MIN_STEPS, "seed $seed par ${puzzle.par}")
        }
    }

    @Test
    fun theParIsTheTrueFewestOperationsAndAnActualSolutionHasThatLength() {
        for (seed in 1..60) {
            val puzzle = TargetGenerator.generate(seed)
            val solution = assertNotNull(TargetSolver.solve(puzzle.numbers, puzzle.target), "seed $seed")
            assertEquals(puzzle.par, solution.size, "seed $seed numbers ${puzzle.numbers} target ${puzzle.target}")
            val game = TargetGame(puzzle.numbers, puzzle.target, puzzle.par)
            for ((i, move) in solution.withIndex()) {
                assertEquals(if (i == solution.size - 1) TargetResult.Solved else TargetResult.Combined, game.combine(move.first, move.op, move.second))
            }
        }
    }

    @Test
    fun theSameSeedAlwaysGivesTheSamePuzzleAndOtherSeedsOtherOnes() {
        val a = TargetGenerator.generate(5)
        val b = TargetGenerator.generate(5)
        assertEquals(a.numbers, b.numbers)
        assertEquals(a.target, b.target)
        assertTrue((1..20).map { TargetGenerator.generate(it).target }.toSet().size > 5, "the targets vary")
    }

    @Test
    fun aHigherMinimumGivesHarderPuzzles() {
        for (seed in 1..40) {
            val puzzle = TargetGenerator.generate(seed, minSteps = 3)
            assertEquals(3, puzzle.par, "seed $seed: with four numbers three operations use all of them")
        }
    }

    @Test
    fun theTargetsDoNotAllLookAlike() {
        val pars = (1..200).map { TargetGenerator.generate(it).par }
        assertTrue(2 in pars && 3 in pars, "both two and three operation puzzles appear")
    }
}
