package com.mehmtcan.brainscroll.game.lonenumber

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards: a Latin square (no number twice in any row or column) is made, some cells that do not touch are picked to be the
 * shaded ones, and each of them gets a number that one of the cells left in its row or column already holds. The board is kept only when
 * the solver finds exactly one way to shade it. The board only depends on the seed, but it is saved as the board itself, so a later
 * change of this does not change a saved game.
 */
object LoneGenerator {
    private const val TRIES = 50_000

    fun generate(seed: Int, difficulty: Difficulty): LonePuzzle = generate(seed, GameLevels.loneNumber(difficulty))

    fun generate(seed: Int, shape: GameLevels.LoneShape): LonePuzzle {
        val random = Random(seed)
        val n = shape.size
        repeat(TRIES) {
            val shaded = pickShaded(random, n, shape.shaded)
            val numbers = latinSquare(random, n).toIntArray()
            var fits = true
            for (cell in shaded) {
                val row = cell / n
                val column = cell % n
                val choices = ArrayList<Int>()
                for (k in 0 until n) {
                    val inRow = row * n + k
                    if (inRow != cell && inRow !in shaded) choices += numbers[inRow]
                    val inColumn = k * n + column
                    if (inColumn != cell && inColumn !in shaded) choices += numbers[inColumn]
                }
                if (choices.isEmpty()) {
                    fits = false
                    break
                }
                numbers[cell] = choices[random.nextInt(choices.size)]
            }
            if (!fits) return@repeat
            val puzzle = LonePuzzle(n, numbers.toList())
            if (LoneSolver.uniqueSolution(puzzle) != null) return puzzle
        }
        error("no board found")
    }

    private fun pickShaded(random: Random, n: Int, count: Int): Set<Int> {
        val shaded = HashSet<Int>()
        var attempts = 0
        while (shaded.size < count && attempts++ < 500) {
            val cell = random.nextInt(n * n)
            if (cell in shaded) continue
            val column = cell % n
            val touches = (column > 0 && cell - 1 in shaded) || (column < n - 1 && cell + 1 in shaded) ||
                (cell >= n && cell - n in shaded) || (cell + n < n * n && cell + n in shaded)
            if (!touches) shaded += cell
        }
        return shaded
    }

    private fun latinSquare(random: Random, n: Int): List<Int> {
        val symbols = (1..n).shuffled(random)
        val rows = (0 until n).shuffled(random)
        val columns = (0 until n).shuffled(random)
        return List(n * n) { symbols[(rows[it / n] + columns[it % n]) % n] }
    }
}
