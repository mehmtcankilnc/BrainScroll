package com.mehmtcan.brainscroll.game.sudoku

import kotlin.random.Random

/** A puzzle: the cells that are given (0 = empty) and the one solution. Cells are numbered row by row from 0. */
class SudokuPuzzle(val givens: IntArray, val solution: IntArray, val layout: SudokuLayout = SudokuLayout.STANDARD)

/**
 * Makes boards that logic alone can finish, the same promise as minesweeper (docs/decisions.md): every board is
 * solvable with naked and hidden singles, so there is exactly one solution and no guessing. The board only depends on
 * [generate]'s `seed` and `clues`, so a saved game can be rebuilt exactly.
 */
object SudokuGenerator {
    const val SIZE = 9
    const val CELLS = 81
    private const val ALL = 0x1FF

    /** The number of given cells of the one level there is for now. More givens is easier; difficulty comes later. */
    const val DEFAULT_CLUES = 36

    /**
     * A board with about [clues] given cells. Cells are taken away in a random order and a cell stays empty only if the
     * board remains solvable by singles; if that stops early, the board simply keeps a few more givens than asked.
     */
    fun generate(seed: Int, clues: Int = DEFAULT_CLUES): SudokuPuzzle {
        val random = Random(seed)
        val solution = solvedGrid(random)
        val givens = solution.copyOf()
        var left = CELLS
        for (cell in (0 until CELLS).shuffled(random)) {
            if (left <= clues) break
            val digit = givens[cell]
            givens[cell] = 0
            if (solveBySingles(givens) != null) left-- else givens[cell] = digit
        }
        return SudokuPuzzle(givens, solution)
    }

    /** A full valid grid: a fixed pattern with the digits, and the rows and columns within their bands, shuffled. */
    private fun solvedGrid(random: Random): IntArray {
        val digits = (1..SIZE).shuffled(random)
        val rows = shuffledLines(random)
        val cols = shuffledLines(random)
        val transpose = random.nextBoolean()
        return IntArray(CELLS) { cell ->
            val r = if (transpose) cols[cell % SIZE] else rows[cell / SIZE]
            val c = if (transpose) rows[cell / SIZE] else cols[cell % SIZE]
            digits[(r * 3 + r / 3 + c) % SIZE]
        }
    }

    /** The order of the 9 rows (or columns): the 3 bands shuffled, and the 3 lines inside each band shuffled. */
    private fun shuffledLines(random: Random): List<Int> =
        (0 until 3).shuffled(random).flatMap { band -> (0 until 3).shuffled(random).map { band * 3 + it } }

    /**
     * The solved grid if naked singles (a cell with one candidate) and hidden singles (a digit with one place in a row,
     * column or box) are enough to finish [grid], else null. [grid] is not changed.
     */
    fun solveBySingles(grid: IntArray, layout: SudokuLayout = SudokuLayout.STANDARD): IntArray? {
        val g = grid.copyOf()
        val rowMask = IntArray(SIZE)
        val colMask = IntArray(SIZE)
        val boxMask = IntArray(SIZE)
        for (cell in 0 until CELLS) if (g[cell] != 0) {
            val bit = 1 shl (g[cell] - 1)
            rowMask[cell / SIZE] = rowMask[cell / SIZE] or bit
            colMask[cell % SIZE] = colMask[cell % SIZE] or bit
            boxMask[layout.regionOf[cell]] = boxMask[layout.regionOf[cell]] or bit
        }
        var empty = g.count { it == 0 }

        fun candidates(cell: Int) = ALL and (rowMask[cell / SIZE] or colMask[cell % SIZE] or boxMask[layout.regionOf[cell]]).inv()

        fun place(cell: Int, bit: Int) {
            g[cell] = 32 - bit.countLeadingZeroBits()
            rowMask[cell / SIZE] = rowMask[cell / SIZE] or bit
            colMask[cell % SIZE] = colMask[cell % SIZE] or bit
            boxMask[layout.regionOf[cell]] = boxMask[layout.regionOf[cell]] or bit
            empty--
        }

        var progress = true
        while (empty > 0 && progress) {
            progress = false
            for (cell in 0 until CELLS) {
                if (g[cell] != 0) continue
                val cand = candidates(cell)
                if (cand == 0) return null
                if (cand and (cand - 1) == 0) { place(cell, cand); progress = true }
            }
            for (unit in layout.units) {
                for (digit in 0 until SIZE) {
                    val bit = 1 shl digit
                    var where = -1
                    var count = 0
                    for (cell in unit) if (g[cell] == 0 && candidates(cell) and bit != 0) { where = cell; count++ }
                    if (count == 1) { place(where, bit); progress = true }
                }
            }
        }
        return if (empty == 0) g else null
    }

    fun boxOf(cell: Int): Int = SudokuLayout.STANDARD.regionOf[cell]

    /** The 27 rows, columns and boxes of the classic board, each as its 9 cells. */
    val UNITS: List<IntArray> = SudokuLayout.STANDARD.units

    /** The cells that share a row, column or box with [cell] on the classic board, not [cell] itself. */
    fun peers(cell: Int): List<Int> = SudokuLayout.STANDARD.peers[cell].toList()
}
