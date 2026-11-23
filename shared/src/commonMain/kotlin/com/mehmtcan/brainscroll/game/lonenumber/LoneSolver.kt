package com.mehmtcan.brainscroll.game.lonenumber

/**
 * Finds the ways to shade a board. It goes row by row: every row has only a few ways to be shaded (no two shaded cells side by side, no
 * number twice among the cells left), and a row has to fit the one above it (no shaded cells on top of each other, no number twice in a
 * column). A way that ends with the cells left in one piece is a solution.
 */
object LoneSolver {
    /** Up to [limit] solutions: the sets of shaded cells. */
    fun solutions(puzzle: LonePuzzle, limit: Int): List<Set<Int>> {
        val n = puzzle.size
        val rowMasks = List(n) { row -> masksOf(puzzle, row) }
        val chosen = IntArray(n)
        val found = ArrayList<Set<Int>>()

        fun rec(row: Int) {
            if (found.size >= limit) return
            if (row == n) {
                val shaded = HashSet<Int>()
                for (r in 0 until n) for (c in 0 until n) if ((chosen[r] shr c) and 1 == 1) shaded += r * n + c
                if (LoneRules.isJoined(puzzle, shaded)) found += shaded
                return
            }
            for (mask in rowMasks[row]) {
                if (row > 0 && (mask and chosen[row - 1]) != 0) continue
                if (!columnsStayLone(puzzle, chosen, row, mask)) continue
                chosen[row] = mask
                rec(row + 1)
            }
        }
        rec(0)
        return found
    }

    /** The shaded cells of the board if it has exactly one way to be shaded, else null. */
    fun uniqueSolution(puzzle: LonePuzzle): Set<Int>? = solutions(puzzle, 2).singleOrNull()

    /** The masks (bit c is the cell in column c) that are fine for one row alone. */
    private fun masksOf(puzzle: LonePuzzle, row: Int): List<Int> {
        val n = puzzle.size
        val out = ArrayList<Int>()
        for (mask in 0 until (1 shl n)) {
            if ((mask and (mask shr 1)) != 0) continue
            var seen = 0
            var ok = true
            for (c in 0 until n) {
                if ((mask shr c) and 1 == 1) continue
                val bit = 1 shl puzzle.numbers[row * n + c]
                if ((seen and bit) != 0) {
                    ok = false
                    break
                }
                seen = seen or bit
            }
            if (ok) out += mask
        }
        return out
    }

    private fun columnsStayLone(puzzle: LonePuzzle, chosen: IntArray, row: Int, mask: Int): Boolean {
        val n = puzzle.size
        for (c in 0 until n) {
            if ((mask shr c) and 1 == 1) continue
            val number = puzzle.numbers[row * n + c]
            for (k in 0 until row) if ((chosen[k] shr c) and 1 == 0 && puzzle.numbers[k * n + c] == number) return false
        }
        return true
    }
}
