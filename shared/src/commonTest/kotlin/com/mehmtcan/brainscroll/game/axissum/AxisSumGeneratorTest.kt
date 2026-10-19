package com.mehmtcan.brainscroll.game.axissum

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AxisSumGeneratorTest {

    @Test
    fun everyBoardOfEveryLevelHasExactlyOneWayAndTheRightShape() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.axisSum(level)
            for (seed in 0 until 80) {
                val p = AxisSumGenerator.generate(seed, level)
                assertEquals(shape.size, p.size)
                assertTrue(p.digits.all { it in 1..shape.maxDigit }, "$level seed $seed numbers")
                val ways = AxisSumSolver.solutions(p.size, p.digits, p.rowTargets, p.columnTargets, limit = 3)
                assertEquals(1, ways.size, "$level seed $seed has ${ways.size} ways")
                assertEquals(p.keep, ways.single())
                // Every line has a number that stays and one that goes.
                for (line in 0 until p.size) {
                    assertTrue((0 until p.size).any { p.keep[line * p.size + it] } && (0 until p.size).any { !p.keep[line * p.size + it] }, "row $line")
                    assertTrue((0 until p.size).any { p.keep[it * p.size + line] } && (0 until p.size).any { !p.keep[it * p.size + line] }, "column $line")
                }
            }
        }
    }

    @Test
    fun theSameSeedGivesTheSameBoardAndOthersDiffer() {
        assertEquals(AxisSumGenerator.generate(5, Difficulty.Medium).encode(), AxisSumGenerator.generate(5, Difficulty.Medium).encode())
        val boards = (0 until 40).map { AxisSumGenerator.generate(it, Difficulty.Medium).encode() }.toSet()
        assertTrue(boards.size >= 38, "boards repeat: ${boards.size} of 40")
    }

    @Test
    fun theSavedTextFitsTheServerLimitAndReadsBack() {
        for (level in Difficulty.entries) {
            for (seed in 0 until 80) {
                val p = AxisSumGenerator.generate(seed, level)
                val text = p.encode()
                assertTrue(text.length <= 32, "$level $text is ${text.length} long")
                val back = assertNotNull(AxisSumPuzzle.decode(text))
                assertEquals(p.digits, back.digits)
                assertEquals(p.keep, back.keep)
            }
        }
    }

    @Test
    fun theBoardsAreNotGivenAway() {
        // At the start every row and column is over its sum, so nothing is solved before the first tap.
        for (level in Difficulty.entries) {
            for (seed in 0 until 30) {
                assertTrue(!AxisSumGame(AxisSumGenerator.generate(seed, level)).isFinished, "$level seed $seed")
            }
        }
    }
}
