package com.mehmtcan.brainscroll.game.crosssum

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class CrossGeneratorTest {

    @Test
    fun everyLevelMakesBoardsThatReasoningFinishesWithFewGivenDigits() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.crossSum(level)
            for (seed in 0 until 25) {
                val p = CrossGenerator.generate(seed, level)
                assertEquals(shape.rows, p.rows)
                assertEquals(shape.columns, p.columns)
                assertTrue(p.givenCells.size <= shape.maxGivens, "$level $seed: ${p.givenCells.size} given digits")
                assertTrue(p.encode().length <= 32, "$level $seed: ${p.encode().length} characters")
                // The reasoning alone, from the sums and the given digits, finds the answer the board was made from.
                val found = assertNotNull(CrossSolver.solve(p), "$level $seed is finished by reasoning")
                for (cell in p.whiteCells) assertEquals(p.solution[cell], found[cell], "$level $seed cell $cell")
            }
        }
    }

    @Test
    fun theBoardsHaveBlackCellsAndBlocksOfAtLeastTwoCells() {
        for (level in Difficulty.entries) for (seed in 0 until 15) {
            val p = CrossGenerator.generate(seed, level)
            assertTrue(p.white.any { !it }, "$level $seed has a black cell")
            assertTrue(p.runs.all { it.cells.size >= 2 && it.sum in 3..45 })
            assertTrue(p.runs.all { run -> run.cells.map { p.solution[it] }.toSet().size == run.cells.size })
            for (cell in p.whiteCells) assertEquals(2, p.runsOf(cell).size, "a white cell is in a block along its row and one down its column")
        }
    }

    @Test
    fun theSameSeedGivesTheSameBoardAndOtherSeedsGiveOthers() {
        for (level in Difficulty.entries) {
            assertEquals(CrossGenerator.generate(3, level), CrossGenerator.generate(3, level))
            val boards = (0 until 10).map { CrossGenerator.generate(it, level) }.toSet()
            assertTrue(boards.size >= 9, "$level: ${boards.size} different boards in 10 seeds")
        }
    }

    @Test
    fun aBoardIsSavedAndReadBack() {
        for (level in Difficulty.entries) for (seed in 0 until 10) {
            val p = CrossGenerator.generate(seed, level)
            assertEquals(p, CrossPuzzle.decode(p.encode()))
        }
        assertEquals(null, CrossPuzzle.decode(""))
        assertEquals(null, CrossPuzzle.decode("!!!"))
    }

    @Test
    fun theBiggestBoardsAreFastToMake() {
        val start = TimeSource.Monotonic.markNow()
        val times = ArrayList<Long>()
        repeat(40) {
            val one = TimeSource.Monotonic.markNow()
            CrossGenerator.generate(it, Difficulty.Hard)
            times += one.elapsedNow().inWholeMilliseconds
        }
        println("CROSS hard: 40 boards in ${start.elapsedNow()}, slowest ${times.max()} ms, median ${times.sorted()[20]} ms")
        assertTrue(times.max() < 3_000, "the slowest hard board took ${times.max()} ms")
    }

    @Test
    fun howManyDigitsTheLevelsGive() {
        for (level in Difficulty.entries) {
            val start = TimeSource.Monotonic.markNow()
            val givens = (0 until 30).map { CrossGenerator.generate(it, level).givenCells.size }
            println("CROSS $level: given digits over 30 boards ${givens.groupingBy { it }.eachCount().toSortedMap()} in ${start.elapsedNow()}")
            assertTrue(givens.average() <= GameLevels.crossSum(level).maxGivens - 0.5, "$level needs ${givens.average()} given digits on average")
        }
    }
}
