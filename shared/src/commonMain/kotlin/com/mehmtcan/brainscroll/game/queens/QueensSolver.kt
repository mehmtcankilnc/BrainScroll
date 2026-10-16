package com.mehmtcan.brainscroll.game.queens

import kotlin.math.abs

/** Finds the ways to put the queens on a board (a plain search, quick for the sizes in use) and which queens attack each other. */
object QueensSolver {

    /** True for every queen that shares a row, a column or a diagonal with another queen. [queens] is row by row. */
    fun attacked(size: Int, queens: BooleanArray): BooleanArray {
        val cells = queens.indices.filter { queens[it] }
        val result = BooleanArray(queens.size)
        for (i in cells.indices) for (j in i + 1 until cells.size) {
            val a = cells[i]
            val b = cells[j]
            val rows = a / size - b / size
            val columns = a % size - b % size
            if (rows == 0 || columns == 0 || abs(rows) == abs(columns)) {
                result[a] = true
                result[b] = true
            }
        }
        return result
    }

    /** Every full placement of [size] queens that holds all the cells of [fixed], each as the list of its cells in row order. */
    fun solutions(size: Int, fixed: Set<Int> = emptySet()): List<List<Int>> {
        val found = mutableListOf<List<Int>>()
        val columns = IntArray(size) { -1 }
        fun place(row: Int) {
            if (row == size) {
                found += List(size) { it * size + columns[it] }
                return
            }
            for (column in 0 until size) {
                val cell = row * size + column
                // A row that has a fixed queen can only hold that one.
                val fixedInRow = fixed.firstOrNull { it / size == row }
                if (fixedInRow != null && fixedInRow != cell) continue
                var free = true
                for (above in 0 until row) {
                    val other = columns[above]
                    if (other == column || abs(other - column) == row - above) { free = false; break }
                }
                if (!free) continue
                columns[row] = column
                place(row + 1)
            }
        }
        place(0)
        return found
    }
}
