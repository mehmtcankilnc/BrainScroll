package com.mehmtcan.brainscroll.game.guide

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.mirror.MirrorGame
import com.mehmtcan.brainscroll.game.mirror.MirrorGenerator
import com.mehmtcan.brainscroll.game.mirror.MirrorResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MirrorGuideTest {

    @Test
    fun everyStepPointsAtACellThatIsWrongAndNamesItsPartnerAsTheReason() {
        for (level in Difficulty.entries) for (seed in 0 until 12) {
            val puzzle = MirrorGenerator.generate(seed, level)
            val game = MirrorGame(puzzle)
            var guard = 0
            while (!game.isFinished && guard++ < 200) {
                val step = assertNotNull(MirrorGuide.step(game.state), "$level $seed")
                val cell = step.cells.single()
                assertTrue(!puzzle.isGiven(cell), "the guide points at the half to fill")
                assertTrue(game.state.marks[cell] != puzzle.target(cell), "the cell is wrong now")
                assertEquals(listOf(puzzle.partner(cell)), step.evidence, "the reason is the cell across")
                assertEquals(if (puzzle.target(cell)) GuideReason.MirrorPaint else GuideReason.MirrorEmpty, step.reason)
                assertNotNull(game.tap(cell).takeIf { it != MirrorResult.Ignored })
            }
            assertTrue(game.isFinished, "following the guide solves the board")
        }
    }

    @Test
    fun aCellToPaintComesBeforeACellToEmpty() {
        val puzzle = MirrorGenerator.generate(3, Difficulty.Easy)
        val game = MirrorGame(puzzle)
        val wrong = puzzle.fillCells.first { !puzzle.target(it) }
        game.tap(wrong)
        assertEquals(GuideReason.MirrorPaint, MirrorGuide.step(game.state)!!.reason)
    }

    @Test
    fun thereIsNoStepOnASolvedBoard() {
        val puzzle = MirrorGenerator.generate(1, Difficulty.Easy)
        val game = MirrorGame(puzzle)
        puzzle.fillCells.filter { puzzle.target(it) }.forEach { game.tap(it) }
        assertNull(MirrorGuide.step(game.state))
    }
}
