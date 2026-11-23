package com.mehmtcan.brainscroll.game.zerosones

/** The rules of the board, for a grid of cells (-1 empty, 0, 1), row after row. */
object BinRules {
    /** The cell numbers of every line of a board of [n] by [n]: the rows, then the columns. */
    fun lines(n: Int): List<IntArray> = List(n) { r -> IntArray(n) { c -> r * n + c } } + List(n) { c -> IntArray(n) { r -> r * n + c } }

    /**
     * The cells that break a rule: three of the same digit side by side, a digit that stands more than half the times in its line, and
     * the cells of two lines that are filled and the same.
     */
    fun conflicts(n: Int, grid: IntArray): Set<Int> {
        val out = HashSet<Int>()
        val lines = lines(n)
        for (line in lines) {
            for (i in 0..n - 3) {
                val a = grid[line[i]]
                if (a >= 0 && grid[line[i + 1]] == a && grid[line[i + 2]] == a) {
                    out += line[i]
                    out += line[i + 1]
                    out += line[i + 2]
                }
            }
            for (digit in 0..1) {
                if (line.count { grid[it] == digit } > n / 2) for (cell in line) if (grid[cell] == digit) out += cell
            }
        }
        // Two filled rows (or columns) that are the same.
        for (group in listOf(lines.subList(0, n), lines.subList(n, 2 * n))) {
            val full = group.filter { line -> line.all { grid[it] >= 0 } }
            for (i in full.indices) for (j in i + 1 until full.size) {
                if (full[i].indices.all { grid[full[i][it]] == grid[full[j][it]] }) {
                    for (cell in full[i]) out += cell
                    for (cell in full[j]) out += cell
                }
            }
        }
        return out
    }

    /** Whether every cell is filled and no rule is broken: the board is solved. */
    fun isSolved(n: Int, grid: IntArray): Boolean = grid.all { it >= 0 } && conflicts(n, grid).isEmpty()
}
