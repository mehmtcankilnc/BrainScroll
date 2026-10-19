package com.mehmtcan.brainscroll.game.arrows

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ArrowsGeneratorTest {

    @Test
    fun everyBoardOfEveryLevelCanBeClearedHasTheRightArrowsAndAFairNumberOfFreeOnes() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.arrows(level)
            for (seed in 0 until 150) {
                val p = ArrowsGenerator.generate(seed, level)
                assertEquals(shape.size, p.size)
                assertTrue(p.arrowCount in shape.minArrows..shape.maxArrows, "$level seed $seed has ${p.arrowCount} arrows")
                assertTrue(p.arrows.all { it.length in 1..shape.maxLength }, "$level seed $seed")
                assertEquals(p.arrowCount, ArrowsSolver.order(p).size, "$level seed $seed")
                val free = ArrowsRules.free(p, BooleanArray(p.arrowCount) { true }).size
                assertTrue(free in shape.minFree..shape.maxFree, "$level seed $seed has $free free arrows")
            }
        }
    }

    @Test
    fun theSameSeedGivesTheSameBoardAndOthersDiffer() {
        assertEquals(ArrowsGenerator.generate(5, Difficulty.Medium).arrows, ArrowsGenerator.generate(5, Difficulty.Medium).arrows)
        val boards = (0 until 40).map { ArrowsGenerator.generate(it, Difficulty.Medium).encode() }.toSet()
        assertTrue(boards.size >= 38, "boards repeat: ${boards.size} of 40")
    }

    @Test
    fun theBoardsHaveWindingArrows() {
        for (level in Difficulty.entries) {
            val bent = (0 until 40).count { seed ->
                val p = ArrowsGenerator.generate(seed, level)
                p.arrows.any { a -> (1 until a.length).map { ArrowsPuzzle.stepDir(p.size, a.cells[it - 1], a.cells[it]) }.distinct().size > 1 }
            }
            assertTrue(bent >= 30, "$level: only $bent of 40 boards have an arrow with an elbow")
        }
    }

    @Test
    fun theSavedTextFitsTheServerLimitAndReadsBack() {
        for (level in Difficulty.entries) {
            for (seed in 0 until 150) {
                val p = ArrowsGenerator.generate(seed, level)
                val text = p.encode()
                assertTrue(text.length <= 32, "$level $text is ${text.length} long")
                assertEquals(p.arrows, assertNotNull(ArrowsPuzzle.decode(text)).arrows)
            }
        }
    }

    @Test
    fun solverOrderClearsTheWholeBoardWithoutMistakes() {
        val p = ArrowsGenerator.generate(11, Difficulty.Hard)
        val game = ArrowsGame(p)
        val order = ArrowsSolver.order(p)
        assertEquals(p.arrowCount, order.size)
        for (arrow in order) assertTrue(game.tap(p.arrows[arrow].head) != ArrowsResult.Blocked)
        assertTrue(game.isFinished)
        assertEquals(0, game.state.mistakes)
    }
}
