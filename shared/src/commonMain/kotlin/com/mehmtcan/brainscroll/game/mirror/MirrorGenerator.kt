package com.mehmtcan.brainscroll.game.mirror

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards: a random pattern for the given half, kept when there is enough to paint and not everything: a bit more than half a
 * row of cells to paint, and some cells left empty. The board only depends on the seed, but it is saved as the board itself, so a later
 * change of this does not change a saved game.
 */
object MirrorGenerator {
    private const val TRIES = 1_000

    fun generate(seed: Int, difficulty: Difficulty): MirrorPuzzle = generate(seed, GameLevels.mirror(difficulty))

    fun generate(seed: Int, shape: GameLevels.MirrorShape): MirrorPuzzle {
        val random = Random(seed)
        val n = shape.size
        val axis = shape.axes[random.nextInt(shape.axes.size)]
        val bare = MirrorPuzzle.shape(n, axis)
        val fill = bare.fillCells.size
        repeat(TRIES) {
            val density = 0.38 + random.nextDouble() * 0.2
            val pattern = List(n * n) { cell -> bare.isGiven(cell) && random.nextDouble() < density }
            val puzzle = MirrorPuzzle(n, axis, pattern)
            val paint = puzzle.paintedTarget
            if (paint >= n / 2 + 2 && paint <= fill - n / 2) return puzzle
        }
        // A pattern that always passes: every other given cell is painted.
        return MirrorPuzzle(n, axis, List(n * n) { cell -> bare.isGiven(cell) && (cell / n + cell % n) % 2 == 0 })
    }
}
