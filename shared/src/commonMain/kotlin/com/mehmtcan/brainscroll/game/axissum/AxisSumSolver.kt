package com.mehmtcan.brainscroll.game.axissum

/**
 * Solving axis sum by search: row by row, each row takes one of the ways to keep numbers that make its sum, and a way is
 * dropped as soon as a column sum is over its target or can no longer reach it with what is left below. For boards up to 6 by 6
 * this is instant.
 */
object AxisSumSolver {
    /**
     * Up to [limit] ways to keep numbers so that every row and column makes its sum, each as a list of "kept" marks per cell.
     * Two means there is more than one, which is what the generator wants to know.
     */
    fun solutions(size: Int, digits: List<Int>, rowTargets: List<Int>, columnTargets: List<Int>, limit: Int = 2): List<List<Boolean>> {
        // For every row, the ways (as bit masks of the kept cells) that make its sum.
        val ways = List(size) { r ->
            (1 until (1 shl size)).filter { mask -> (0 until size).sumOf { c -> if (mask shr c and 1 == 1) digits[r * size + c] else 0 } == rowTargets[r] }
        }
        // What is left in each column below row r (the most the rest of the column can still add).
        val below = Array(size + 1) { IntArray(size) }
        for (r in size - 1 downTo 0) for (c in 0 until size) below[r][c] = below[r + 1][c] + digits[r * size + c]

        val found = ArrayList<List<Boolean>>()
        val chosen = IntArray(size)
        val columnSums = IntArray(size)

        fun search(row: Int) {
            if (found.size >= limit) return
            if (row == size) {
                if ((0 until size).all { columnSums[it] == columnTargets[it] }) {
                    found += List(size * size) { i -> chosen[i / size] shr (i % size) and 1 == 1 }
                }
                return
            }
            for (mask in ways[row]) {
                var ok = true
                for (c in 0 until size) {
                    if (mask shr c and 1 == 1) columnSums[c] += digits[row * size + c]
                    if (columnSums[c] > columnTargets[c] || columnSums[c] + below[row + 1][c] < columnTargets[c]) ok = false
                }
                if (ok) {
                    chosen[row] = mask
                    search(row + 1)
                }
                for (c in 0 until size) if (mask shr c and 1 == 1) columnSums[c] -= digits[row * size + c]
                if (found.size >= limit) return
            }
        }
        search(0)
        return found
    }

    /** The one way to keep numbers that makes every sum, or null when there is none or more than one. */
    fun unique(puzzle: AxisSumPuzzle): List<Boolean>? =
        solutions(puzzle.size, puzzle.digits, puzzle.rowTargets, puzzle.columnTargets, limit = 2).singleOrNull()
}
