package com.mehmtcan.brainscroll.game.ballslide

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/** A board of ball slide with the fewest slides that win it (see [BallSlideSolver]). */
class BallSlideBoard(val puzzle: BallSlidePuzzle, val par: Int)

/**
 * Makes the boards of ball slide: walls on random cells, then a few random starts; the search
 * finds the fewest slides that paint everything from a start, and a board is kept if some start can win it in a number of slides
 * the level asks for. Walls that leave no start that wins are thrown away. The board only depends on the seed, but it is saved as
 * the board itself, so a later change of this does not change a saved game.
 */
object BallSlideGenerator {
    private const val MAX_ATTEMPTS = 3000
    private const val STARTS_PER_WALLS = 6

    fun generate(seed: Int, difficulty: Difficulty): BallSlideBoard = generate(seed, GameLevels.ballSlide(difficulty))

    fun generate(seed: Int, shape: GameLevels.BallSlideShape): BallSlideBoard {
        val random = Random(seed)
        val n = shape.size
        repeat(MAX_ATTEMPTS) {
            val wallCount = shape.minWalls + random.nextInt(shape.maxWalls - shape.minWalls + 1)
            val walls = MutableList(n * n) { false }
            for (cell in (0 until n * n).shuffled(random).take(wallCount)) walls[cell] = true
            val open = walls.indices.filter { !walls[it] }
            for (start in open.shuffled(random).take(STARTS_PER_WALLS)) {
                val puzzle = BallSlidePuzzle(n, walls, start)
                val par = BallSlideSolver.par(puzzle) ?: continue
                if (par in shape.minPar..shape.maxPar) return BallSlideBoard(puzzle, par)
            }
        }
        error("no board found")
    }
}
