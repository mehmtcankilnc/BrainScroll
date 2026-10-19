package com.mehmtcan.brainscroll.game.axissum

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards of axis sum: random numbers, a random choice of the ones to erase (every row
 * and column keeps at least one number and loses at least one, so every line has something to do), the sums worked out from the
 * rest. A board is kept only if the search finds exactly one way to erase (see [AxisSumSolver]); otherwise another is made. The
 * board only depends on the seed, but it is saved as the board itself, so a later change of this does not change a saved game.
 */
object AxisSumGenerator {
    private const val MAX_ATTEMPTS = 4000

    fun generate(seed: Int, difficulty: Difficulty): AxisSumPuzzle = generate(seed, GameLevels.axisSum(difficulty))

    fun generate(seed: Int, shape: GameLevels.AxisSumShape): AxisSumPuzzle {
        val random = Random(seed)
        val n = shape.size
        repeat(MAX_ATTEMPTS) {
            val digits = List(n * n) { 1 + random.nextInt(shape.maxDigit) }
            val erase = (n * n * shape.eraseShare).toInt().coerceIn(n, n * n - n)
            val keep = MutableList(n * n) { true }
            for (cell in (0 until n * n).shuffled(random).take(erase)) keep[cell] = false
            val everyLineHasBoth = (0 until n).all { r -> (0 until n).any { keep[r * n + it] } && (0 until n).any { !keep[r * n + it] } } &&
                (0 until n).all { c -> (0 until n).any { keep[it * n + c] } && (0 until n).any { !keep[it * n + c] } }
            if (!everyLineHasBoth) return@repeat
            val puzzle = AxisSumPuzzle(n, digits, keep)
            if (AxisSumSolver.unique(puzzle) != null) return puzzle
        }
        error("no board found")
    }
}
