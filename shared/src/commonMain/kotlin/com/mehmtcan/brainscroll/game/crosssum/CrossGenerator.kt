package com.mehmtcan.brainscroll.game.crosssum

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards: a pattern of white and black cells in which every run of white cells is at least two cells long, a random filling
 * with different digits in every block, and then the sums. A board is kept only when the reasoning of [CrossSolver] finishes it: if it
 * cannot, the digit of the cell it is least sure about is given to the player, again and again, until it can or there are more given
 * digits than the level allows (then the pattern is dropped). Many patterns are tried and the one that needs the fewest given digits
 * wins (the search stops early at a board that needs no more than the level wants). So every board has one answer that can be found by
 * reasoning. The board only depends on the seed, but it is saved as the board itself, so a later change of this does not change a
 * saved game.
 */
object CrossGenerator {
    private const val FILL_NODES = 20_000

    fun generate(seed: Int, difficulty: Difficulty): CrossPuzzle = generate(seed, GameLevels.crossSum(difficulty))

    fun generate(seed: Int, shape: GameLevels.CrossShape): CrossPuzzle {
        val random = Random(seed)
        val rows = shape.rows
        val columns = shape.columns
        val n = rows * columns
        // A board is saved in 32 characters (see CrossPuzzle): this many white cells fit.
        val maxWhite = (CrossPuzzle.MAX_BITS - 6 - n) / 5
        var best: CrossPuzzle? = null
        var bestGivens = shape.maxGivens + 1
        repeat(shape.attempts) {
            val white = pattern(random, rows, columns, shape.density, maxWhite) ?: return@repeat
            val solution = fill(random, white, blocks(rows, columns, white) ?: return@repeat) ?: return@repeat
            val base = CrossPuzzle(rows, columns, white, solution, List(n) { false })
            val start = IntArray(n)
            // Give digits until the reasoning finishes the board, but never more than the best board so far needs, less one.
            for (given in 0 until bestGivens) {
                val cand = CrossSolver.candidates(base, start) ?: return@repeat
                val open = base.whiteCells.filter { start[it] == 0 && cand[it] and (cand[it] - 1) != 0 }
                if (open.isEmpty()) {
                    best = CrossPuzzle(rows, columns, white, solution, List(n) { start[it] > 0 })
                    bestGivens = given
                    if (given <= shape.wantGivens) return best!!
                    return@repeat
                }
                // The cell with the fewest digits left that is not decided yet.
                val fewest = open.minOf { cand[it].countOneBits() }
                val pick = open.filter { cand[it].countOneBits() == fewest }.let { it[random.nextInt(it.size)] }
                start[pick] = solution[pick]
            }
        }
        return best ?: error("no board found")
    }

    /**
     * A pattern: all white to start, then random cells turned black as long as every run of white cells stays at least two cells long, until
     * about [density] of the cells are white (and no more than [maxWhite]). Null if that is not reached.
     */
    private fun pattern(random: Random, rows: Int, columns: Int, density: Double, maxWhite: Int): List<Boolean>? {
        val n = rows * columns
        val white = MutableList(n) { true }
        val wanted = maxOf(n - maxWhite, (n * (1 - density)).toInt() + random.nextInt(-1, 2))
        var blacks = 0
        for (cell in (0 until n).shuffled(random)) {
            if (blacks >= wanted) break
            white[cell] = false
            if (blocks(rows, columns, white) != null) blacks++ else white[cell] = true
        }
        return if (blacks >= wanted && white.count { it } >= n * 6 / 10) white else null
    }

    /** The blocks of a pattern (cells of every run of white cells along rows and columns), or null if a white cell stands alone in a row or column. */
    private fun blocks(rows: Int, columns: Int, white: List<Boolean>): List<List<Int>>? {
        val out = ArrayList<List<Int>>()
        for (row in 0 until rows) {
            var column = 0
            while (column < columns) {
                if (!white[row * columns + column]) { column++; continue }
                val start = column
                while (column < columns && white[row * columns + column]) column++
                if (column - start < 2) return null
                out += (start until column).map { row * columns + it }
            }
        }
        for (column in 0 until columns) {
            var row = 0
            while (row < rows) {
                if (!white[row * columns + column]) { row++; continue }
                val start = row
                while (row < rows && white[row * columns + column]) row++
                if (row - start < 2) return null
                out += (start until row).map { it * columns + column }
            }
        }
        return out
    }

    /** Different digits in every block, found by a randomised search that gives up after [FILL_NODES] steps. */
    private fun fill(random: Random, white: List<Boolean>, blocks: List<List<Int>>): List<Int>? {
        val cells = white.indices.filter { white[it] }
        val blocksOf = cells.associateWith { cell -> blocks.filter { cell in it } }
        val digits = IntArray(white.size)
        var nodes = 0
        fun place(i: Int): Boolean {
            if (i == cells.size) return true
            if (++nodes > FILL_NODES) return false
            val cell = cells[i]
            for (d in (1..9).shuffled(random)) {
                if (blocksOf.getValue(cell).any { block -> block.any { digits[it] == d } }) continue
                digits[cell] = d
                if (place(i + 1)) return true
                digits[cell] = 0
            }
            return false
        }
        return if (place(0)) digits.toList() else null
    }
}
