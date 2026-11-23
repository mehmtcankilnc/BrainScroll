package com.mehmtcan.brainscroll.game.mirror

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MirrorGeneratorTest {

    @Test
    fun everyLevelMakesABoardOfItsShapeWithSomethingToPaintAndNotEverything() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.mirror(level)
            for (seed in 0 until 60) {
                val p = MirrorGenerator.generate(seed, level)
                assertEquals(shape.size, p.size, "$level size")
                assertTrue(p.axis in shape.axes, "$level axis ${p.axis}")
                val fill = p.fillCells.size
                assertTrue(p.paintedTarget >= p.size / 2 + 2, "$level $seed: enough to paint (${p.paintedTarget})")
                assertTrue(p.paintedTarget <= fill - p.size / 2, "$level $seed: not everything (${p.paintedTarget} of $fill)")
                assertTrue(p.encode().length <= 32)
            }
        }
    }

    @Test
    fun theSameSeedGivesTheSameBoardAndOtherSeedsGiveOthers() {
        for (level in Difficulty.entries) {
            assertEquals(MirrorGenerator.generate(7, level), MirrorGenerator.generate(7, level))
            val boards = (0 until 30).map { MirrorGenerator.generate(it, level) }.toSet()
            assertTrue(boards.size >= 20, "$level: ${boards.size} different boards in 30 seeds")
        }
    }

    @Test
    fun theHarderLevelsUseOtherAxesToo() {
        val medium = (0 until 60).map { MirrorGenerator.generate(it, Difficulty.Medium).axis }.toSet()
        assertEquals(setOf(MirrorAxis.Vertical, MirrorAxis.Horizontal), medium)
        val hard = (0 until 60).map { MirrorGenerator.generate(it, Difficulty.Hard).axis }.toSet()
        assertEquals(setOf(MirrorAxis.Diagonal, MirrorAxis.Point), hard)
        assertEquals(setOf(MirrorAxis.Vertical), (0 until 20).map { MirrorGenerator.generate(it, Difficulty.Easy).axis }.toSet())
    }

    @Test
    fun theBiggestBoardIsFastToMake() {
        // The board is made on a background thread, but it should not take long at all.
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        repeat(200) { MirrorGenerator.generate(it, Difficulty.Hard) }
        assertTrue(start.elapsedNow().inWholeMilliseconds < 2_000, "200 hard boards in ${start.elapsedNow()}")
    }
}
