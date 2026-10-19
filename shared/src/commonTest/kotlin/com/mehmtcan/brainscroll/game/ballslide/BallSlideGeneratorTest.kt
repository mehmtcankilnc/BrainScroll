package com.mehmtcan.brainscroll.game.ballslide

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BallSlideGeneratorTest {

    @Test
    fun everyBoardOfEveryLevelCanBeWonInTheStatedNumberOfSlides() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.ballSlide(level)
            for (seed in 0 until 40) {
                val board = BallSlideGenerator.generate(seed, level)
                val p = board.puzzle
                assertEquals(shape.size, p.size)
                assertTrue(p.walls.count { it } in shape.minWalls..shape.maxWalls, "$level seed $seed walls")
                assertTrue(board.par in shape.minPar..shape.maxPar, "$level seed $seed par ${board.par}")
                // The shortest way really paints everything.
                val way = assertNotNull(BallSlideSolver.shortest(p, p.start, 1L shl p.start))
                assertEquals(board.par, way.size)
                val game = BallSlideGame(p)
                way.forEach { game.slide(it) }
                assertTrue(game.isFinished, "$level seed $seed")
            }
        }
    }

    @Test
    fun theSameSeedGivesTheSameBoardAndOthersDiffer() {
        assertEquals(BallSlideGenerator.generate(5, Difficulty.Medium).puzzle.encode(), BallSlideGenerator.generate(5, Difficulty.Medium).puzzle.encode())
        val boards = (0 until 40).map { BallSlideGenerator.generate(it, Difficulty.Medium).puzzle.encode() }.toSet()
        assertTrue(boards.size >= 38, "boards repeat: ${boards.size} of 40")
    }

    @Test
    fun theSavedTextFitsTheServerLimitAndReadsBack() {
        for (level in Difficulty.entries) {
            for (seed in 0 until 40) {
                val p = BallSlideGenerator.generate(seed, level).puzzle
                val text = p.encode()
                assertTrue(text.length <= 32, "$text is ${text.length} long")
                val back = assertNotNull(BallSlidePuzzle.decode(text))
                assertEquals(p.walls, back.walls)
                assertEquals(p.start, back.start)
            }
        }
    }
}
