package com.mehmtcan.brainscroll.game.line

/**
 * A board of the one-line game: a [size] x [size] grid whose cells are numbered row by row from 0.
 *
 * [checkpoints] holds, in order, the cell of number 1, of number 2, and so on. A line that starts on number 1, passes
 * the numbers in order, fills every cell exactly once and ends on the last number solves it. [solution] is such a line
 * (the only one, see [LineSolver]); the game never shows it, only hints take from it.
 */
class LinePuzzle(val size: Int, val checkpoints: IntArray, val solution: IntArray) {
    val cells: Int get() = size * size

    /** For every cell, its number (1-based) or 0 when it has none. */
    val numbers: IntArray = IntArray(size * size).also { numbers ->
        checkpoints.forEachIndexed { index, cell -> numbers[cell] = index + 1 }
    }

    val lastNumber: Int get() = checkpoints.size

    init {
        require(size >= 3) { "a board needs some room" }
        require(checkpoints.size >= 2) { "a line needs a start and an end" }
        require(checkpoints.all { it in 0 until size * size } && checkpoints.toSet().size == checkpoints.size) { "numbers sit on different cells" }
        require(solution.size == size * size) { "the solution fills every cell" }
    }

    /** The board as saved with an unfinished game: the size, then the cell of each number in order, e.g. `7:3,10,25`. */
    fun encode(): String = "$size:${checkpoints.joinToString(",")}"

    companion object {
        /**
         * Reads what [encode] wrote and finds the line again with the solver (the solution is not saved). Null if the text
         * is not a board or the board has no line.
         */
        fun decode(text: String): LinePuzzle? {
            val parts = text.split(":")
            if (parts.size != 2) return null
            val size = parts[0].toIntOrNull() ?: return null
            if (size !in 3..MAX_SIZE) return null
            val checkpoints = parts[1].split(",").map { it.toIntOrNull() ?: return null }.toIntArray()
            if (checkpoints.size < 2 || checkpoints.any { it !in 0 until size * size } || checkpoints.toSet().size != checkpoints.size) return null
            val solution = LineSolver.solve(size, checkpoints) ?: return null
            return LinePuzzle(size, checkpoints, solution)
        }

        private const val MAX_SIZE = 9
    }
}
