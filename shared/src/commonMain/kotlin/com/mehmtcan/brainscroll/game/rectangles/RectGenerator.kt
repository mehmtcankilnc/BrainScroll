package com.mehmtcan.brainscroll.game.rectangles

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards: the square is cut into rectangles at random (none bigger than the level allows), a number is put on a cell of every
 * rectangle, and the board is kept only when the solver finds exactly one way to divide it. The board only depends on the seed, but it is
 * saved as the board itself, so a later change of this does not change a saved game.
 */
object RectGenerator {
    private const val CUTS = 600
    private const val PLACEMENTS = 25

    fun generate(seed: Int, difficulty: Difficulty): RectPuzzle = generate(seed, GameLevels.rectangles(difficulty))

    fun generate(seed: Int, shape: GameLevels.RectShape): RectPuzzle {
        val random = Random(seed)
        repeat(CUTS) {
            val pieces = cut(random, shape)
            if (pieces.size !in shape.minPieces..RectPuzzle.MAX_CLUES) return@repeat
            repeat(PLACEMENTS) {
                val numbers = IntArray(shape.size * shape.size)
                for (piece in pieces) {
                    val column = piece.x + random.nextInt(piece.w)
                    val row = piece.y + random.nextInt(piece.h)
                    numbers[row * shape.size + column] = piece.area
                }
                val puzzle = RectPuzzle(shape.size, numbers.toList())
                if (RectSolver.uniqueSolution(puzzle) != null) return puzzle
            }
        }
        error("no board found")
    }

    private fun cut(random: Random, shape: GameLevels.RectShape): List<Rect> {
        val out = ArrayList<Rect>()
        fun split(x: Int, y: Int, w: Int, h: Int) {
            val area = w * h
            if (area <= shape.maxArea && (area <= 3 || random.nextInt(100) < 35)) {
                out += Rect(x, y, w, h)
                return
            }
            val across = w > h || (w == h && random.nextBoolean())
            if (across && w > 1) {
                val c = 1 + random.nextInt(w - 1)
                split(x, y, c, h)
                split(x + c, y, w - c, h)
            } else if (h > 1) {
                val c = 1 + random.nextInt(h - 1)
                split(x, y, w, c)
                split(x, y + c, w, h - c)
            } else {
                out += Rect(x, y, w, h)
            }
        }
        split(0, 0, shape.size, shape.size)
        return out
    }
}
