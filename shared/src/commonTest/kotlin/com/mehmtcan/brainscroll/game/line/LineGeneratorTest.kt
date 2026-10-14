package com.mehmtcan.brainscroll.game.line

import com.mehmtcan.brainscroll.time.IstanbulDay
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class LineGeneratorTest {

    private fun assertWholeLine(path: IntArray, size: Int, label: String) {
        assertEquals(size * size, path.size, label)
        assertEquals(size * size, path.toSet().size, "every cell once, $label")
        for (i in 1 until path.size) {
            val steps = abs(path[i] / size - path[i - 1] / size) + abs(path[i] % size - path[i - 1] % size)
            assertEquals(1, steps, "step $i is to a neighbor, $label")
        }
    }

    @Test
    fun theRandomNumbersAreTheStandardSplitMix64OnesAndAlwaysTheSame() {
        val random = SplitMix64(0)
        assertEquals(0xE220A8397B1DCDAFUL, random.nextLong().toULong())
        assertEquals(0x6E789E6AA1B965F4UL, random.nextLong().toULong())
        val a = SplitMix64(123)
        val b = SplitMix64(123)
        repeat(20) { assertEquals(a.nextInt(1000), b.nextInt(1000)) }
        assertTrue((1..200).all { SplitMix64(it.toLong()).nextInt(7) in 0..6 })
    }

    @Test
    fun aRandomPathFillsTheGridWithStepsToNeighborsAndIsNotAlwaysTheSnake() {
        val snakes = mutableSetOf<List<Int>>()
        for (size in 3..7) for (seed in 1L..40L) {
            val path = LineGenerator.randomPath(SplitMix64(seed), size)
            assertWholeLine(path, size, "size $size seed $seed")
            snakes += path.toList()
        }
        assertTrue(snakes.size > 150, "the paths differ: ${snakes.size}")
    }

    @Test
    fun everyBoardHasExactlyOneLineAndItsSolutionIsThatLine() {
        for (size in 4..7) for (seed in 1L..8L) {
            val puzzle = LineGenerator.generate(seed, size, checkpoints = size - 1)
            val label = "size $size seed $seed"
            assertWholeLine(puzzle.solution, size, label)
            assertEquals(1, LineSolver.count(size, puzzle.checkpoints, 2), label)
            assertContentEquals(puzzle.solution, assertNotNull(LineSolver.solve(size, puzzle.checkpoints)), label)
            assertEquals(puzzle.solution.first(), puzzle.checkpoints.first(), label)
            assertEquals(puzzle.solution.last(), puzzle.checkpoints.last(), label)
            assertTrue(puzzle.checkpoints.size in 2..size * 2, "$label has ${puzzle.checkpoints.size} numbers")
        }
    }

    @Test
    fun theNumbersLieOnTheSolutionInOrder() {
        val puzzle = LineGenerator.generate(5, 6, 5)
        val positions = puzzle.checkpoints.map { puzzle.solution.indexOf(it) }
        assertEquals(positions.sorted(), positions)
        assertEquals(positions.toSet().size, positions.size)
    }

    @Test
    fun theBoardsKeepOnlyTheNumbersTheyNeed() {
        for ((size, most) in listOf(5 to 9, 6 to 12, 7 to 15)) {
            val sizes = (1L..10L).map { LineGenerator.generate(it, size, 4).checkpoints.size }
            assertTrue(sizes.all { it in 3..most }, "size $size numbers $sizes")
            // And not one of them can go: without any number, the line is no longer the only one.
            for (seed in 1L..3L) {
                val puzzle = LineGenerator.generate(seed, size, 4)
                for (skip in 1 until puzzle.checkpoints.size - 1) {
                    val fewer = puzzle.checkpoints.filterIndexed { index, _ -> index != skip }.toIntArray()
                    assertTrue(LineSolver.count(size, fewer, 2) != 1, "size $size seed $seed: number ${skip + 1} is not needed")
                }
            }
        }
    }

    @Test
    fun theSameSeedAlwaysGivesTheSameBoardAndOtherSeedsOtherOnes() {
        val a = LineGenerator.generate(77, 6, 5)
        val b = LineGenerator.generate(77, 6, 5)
        assertContentEquals(a.solution, b.solution)
        assertContentEquals(a.checkpoints, b.checkpoints)
        assertTrue(!a.solution.contentEquals(LineGenerator.generate(78, 6, 5).solution))
    }

    @Test
    fun aBoardCanNotBeTooSmallOrHaveTooFewOrTooManyNumbers() {
        kotlin.test.assertFailsWith<IllegalArgumentException> { LineGenerator.generate(1, 2, 2) }
        kotlin.test.assertFailsWith<IllegalArgumentException> { LineGenerator.generate(1, 5, 1) }
        kotlin.test.assertFailsWith<IllegalArgumentException> { LineGenerator.generate(1, 5, checkpoints = 11, cap = 10) }
    }

    @Test
    fun theWeekGrowsFromFiveBySevenAndMondayIsDayThree() {
        assertEquals("2026-10-14", IstanbulDay.format(20740))
        assertEquals(2, LineSchedule.weekday(20740), "2026-10-14 is a Wednesday")
        assertEquals(3, LineSchedule.weekday(0), "1970-01-01 was a Thursday")
        assertEquals(6, LineSchedule.weekday(-4), "1969-12-28 was a Sunday")
        assertEquals(0, LineSchedule.weekday(20738), "20738 is a Monday")
        assertEquals(listOf(5, 5, 5, 6, 6, 7, 7), (20738L..20744L).map { LineSchedule.specFor(it).size })
        assertEquals(listOf(4, 4, 4, 5, 5, 6, 6), (20738L..20744L).map { LineSchedule.specFor(it).checkpoints })
    }

    @Test
    fun everyDailyBoardOfTheFirstYearHasOneLine() {
        val start = 20_740L
        val started = TimeSource.Monotonic.markNow()
        for (day in start until start + 365) {
            val puzzle = LineSchedule.puzzleFor(day)
            val spec = LineSchedule.specFor(day)
            assertEquals(spec.size, puzzle.size, "day $day")
            assertTrue(puzzle.checkpoints.size in 3..puzzle.size * 2, "day $day has ${puzzle.checkpoints.size} numbers")
            assertEquals(1, LineSolver.count(puzzle.size, puzzle.checkpoints, 2), "day $day")
            assertWholeLine(puzzle.solution, puzzle.size, "day $day")
        }
        println("365 daily boards made and checked in ${started.elapsedNow()}")
    }

    @Test
    fun theDailyBoardIsTheSameEveryTimeItIsAsked() {
        assertContentEquals(LineSchedule.puzzleFor(20741).solution, LineSchedule.puzzleFor(20741).solution)
        assertTrue(!LineSchedule.puzzleFor(20741).solution.contentEquals(LineSchedule.puzzleFor(20742).solution))
    }
}
