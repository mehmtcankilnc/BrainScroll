package com.mehmtcan.brainscroll.game.lonenumber

/** The three rules of the board, for a set of shaded cells (cell numbers, row * size + column). */
object LoneRules {
    /** The cells that are not shaded and hold a number that another cell left in their row or column holds too. */
    fun repeated(puzzle: LonePuzzle, shaded: Set<Int>): Set<Int> {
        val n = puzzle.size
        val out = HashSet<Int>()
        for (cell in 0 until n * n) {
            if (cell in shaded) continue
            val row = cell / n
            val column = cell % n
            val number = puzzle.numbers[cell]
            for (k in 0 until n) {
                val inRow = row * n + k
                if (inRow != cell && inRow !in shaded && puzzle.numbers[inRow] == number) out += cell
                val inColumn = k * n + column
                if (inColumn != cell && inColumn !in shaded && puzzle.numbers[inColumn] == number) out += cell
            }
        }
        return out
    }

    /** The shaded cells that touch another shaded cell by an edge. */
    fun touching(puzzle: LonePuzzle, shaded: Set<Int>): Set<Int> =
        shaded.filterTo(HashSet()) { cell -> puzzle.neighbours(cell).any { it in shaded } }

    /** Whether the cells that are not shaded are all joined in one piece (a board with none left is not). */
    fun isJoined(puzzle: LonePuzzle, shaded: Set<Int>): Boolean {
        val n = puzzle.size
        val start = (0 until n * n).firstOrNull { it !in shaded } ?: return false
        val seen = HashSet<Int>()
        val stack = ArrayDeque<Int>()
        stack.add(start)
        seen += start
        while (stack.isNotEmpty()) {
            val cell = stack.removeLast()
            for (next in puzzle.neighbours(cell)) if (next !in shaded && seen.add(next)) stack.add(next)
        }
        return seen.size == n * n - shaded.size
    }

    /** Whether [shaded] follows the three rules, so the board is solved. */
    fun isSolved(puzzle: LonePuzzle, shaded: Set<Int>): Boolean =
        repeated(puzzle, shaded).isEmpty() && touching(puzzle, shaded).isEmpty() && isJoined(puzzle, shaded)
}
