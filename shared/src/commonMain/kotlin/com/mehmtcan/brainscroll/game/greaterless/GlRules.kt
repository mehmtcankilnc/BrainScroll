package com.mehmtcan.brainscroll.game.greaterless

/** The rules of the board, for a grid of numbers (0 empty, 1 to the size), row after row. */
object GlRules {
    /** The cell numbers of every line of a board of [n] by [n]: the rows, then the columns. */
    fun lines(n: Int): List<IntArray> = List(n) { r -> IntArray(n) { c -> r * n + c } } + List(n) { c -> IntArray(n) { r -> r * n + c } }

    /** Whether the sign [sign] (see [GlPuzzle.LESS]) is false for the numbers [a] and [b] (the cell and the one next to it). 0 is not a number: never false. */
    fun breaks(sign: Int, a: Int, b: Int): Boolean = a > 0 && b > 0 && when (sign) {
        GlPuzzle.LESS -> a >= b
        GlPuzzle.MORE -> a <= b
        else -> false
    }

    /** The two cells every sign sits between: the cell, and the cell on its right (or below it), with the sign. */
    fun links(puzzle: GlPuzzle): List<Triple<Int, Int, Int>> {
        val n = puzzle.size
        val out = ArrayList<Triple<Int, Int, Int>>()
        for (cell in 0 until n * n) {
            if (puzzle.right[cell] > 0) out += Triple(cell, cell + 1, puzzle.right[cell])
            if (puzzle.down[cell] > 0) out += Triple(cell, cell + n, puzzle.down[cell])
        }
        return out
    }

    /** The cells that break a rule: a number that stands twice in its row or column, and the two cells of a sign that is not true. */
    fun conflicts(puzzle: GlPuzzle, grid: IntArray): Set<Int> {
        val out = HashSet<Int>()
        for (line in lines(puzzle.size)) {
            for (i in line.indices) for (j in i + 1 until line.size) {
                if (grid[line[i]] > 0 && grid[line[i]] == grid[line[j]]) {
                    out += line[i]
                    out += line[j]
                }
            }
        }
        for ((a, b, sign) in links(puzzle)) if (breaks(sign, grid[a], grid[b])) {
            out += a
            out += b
        }
        return out
    }

    /** Whether every cell is filled and no rule is broken: the board is solved. */
    fun isSolved(puzzle: GlPuzzle, grid: IntArray): Boolean = grid.all { it > 0 } && conflicts(puzzle, grid).isEmpty()
}
