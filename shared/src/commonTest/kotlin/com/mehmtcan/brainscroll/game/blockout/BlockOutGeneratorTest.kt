package com.mehmtcan.brainscroll.game.blockout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlockOutGeneratorTest {

    @Test
    fun everyBoardIsSolvableAndNeedsAtLeastEightMoves() {
        for (seed in 0 until 25) {
            val board = BlockOutGenerator.generate(seed)
            val p = board.puzzle
            val way = assertNotNull(BlockOutSolver.shortest(p.size, p.shapes, p.start), "seed $seed")
            assertEquals(board.par, way.size, "seed $seed")
            assertTrue(board.par >= BlockOutGenerator.DEFAULT_MIN_PAR, "seed $seed needs ${board.par}")
        }
    }

    @Test
    fun theTargetStartsInTheMiddleRowAwayFromTheExit() {
        for (seed in 0 until 25) {
            val p = BlockOutGenerator.generate(seed).puzzle
            val target = p.shapes[p.targetIndex]
            assertEquals(p.size / 2, target.line)
            assertTrue(target.horizontal)
            assertTrue(p.start[p.targetIndex] + target.length < p.size, "seed $seed starts away from the exit")
        }
    }

    @Test
    fun aBoardIsWrittenAsShortTextAndReadBack() {
        for (seed in 0 until 25) {
            val p = BlockOutGenerator.generate(seed).puzzle
            val text = p.encode()
            assertEquals(25, text.length)
            val back = assertNotNull(BlockOutPuzzle.decode(text), "seed $seed")
            assertEquals(text, back.encode())
            assertEquals(p.shapes, back.shapes)
            assertEquals(p.start.toList(), back.start.toList())
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoardAndOthersDiffer() {
        assertEquals(BlockOutGenerator.generate(9).puzzle.encode(), BlockOutGenerator.generate(9).puzzle.encode())
        assertTrue((0 until 20).map { BlockOutGenerator.generate(it).puzzle.encode() }.toSet().size == 20)
    }
}
