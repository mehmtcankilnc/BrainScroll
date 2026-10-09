package com.mehmtcan.brainscroll.game.slide

import kotlin.math.abs

/**
 * A plain IDA* solver with the Manhattan distance, for tests only: it finds the cells to tap, one slide at a time, that
 * finish a board. It also proves that a generated board can be solved.
 */
object SlideSolver {

    /** The cells to tap in order (each a single-tile slide) that solve [tiles] (0 is the empty cell). */
    fun solve(tiles: List<Int>, cols: Int, rows: Int): List<Int> {
        val board = tiles.toIntArray()
        var limit = h(board, cols)
        val path = ArrayList<Int>()
        while (true) {
            val next = search(board, board.indexOf(0), -1, 0, limit, cols, rows, path)
            if (next == FOUND) return path.toList()
            check(next != Int.MAX_VALUE) { "no solution" }
            limit = next
        }
    }

    private const val FOUND = -1

    private fun h(board: IntArray, cols: Int): Int = SlideGame.distance(board, cols)

    private fun search(board: IntArray, blank: Int, previous: Int, g: Int, limit: Int, cols: Int, rows: Int, path: MutableList<Int>): Int {
        val f = g + h(board, cols)
        if (f > limit) return f
        if (SlideGame.isSolved(board)) return FOUND
        var min = Int.MAX_VALUE
        val r = blank / cols
        val c = blank % cols
        val moves = buildList {
            if (r > 0) add(blank - cols)
            if (r < rows - 1) add(blank + cols)
            if (c > 0) add(blank - 1)
            if (c < cols - 1) add(blank + 1)
        }
        for (next in moves) {
            if (next == previous) continue
            board[blank] = board[next]
            board[next] = 0
            path += next
            val result = search(board, next, blank, g + 1, limit, cols, rows, path)
            if (result == FOUND) return FOUND
            path.removeAt(path.size - 1)
            board[next] = board[blank]
            board[blank] = 0
            if (result < min) min = result
        }
        return min
    }
}
