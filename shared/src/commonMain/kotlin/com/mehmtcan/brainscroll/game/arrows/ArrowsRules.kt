package com.mehmtcan.brainscroll.game.arrows

/** The four ways an arrow can point. Stored as the number. */
object ArrowDir {
    const val UP = 0
    const val RIGHT = 1
    const val DOWN = 2
    const val LEFT = 3

    /** Row and column step of each direction. */
    val dRow = intArrayOf(-1, 0, 1, 0)
    val dColumn = intArrayOf(0, 1, 0, -1)
}

/** The rules of arrow escape that need no state, shared by the game, the solver and the generator. */
object ArrowsRules {
    /**
     * The first cell in front of the head of arrow [arrow] (the way it points, to the edge of the board) that holds a part of
     * an arrow that is still on the board [alive] (its own body included: an arrow that points at itself never leaves), or -1
     * if the way to the edge is clear.
     */
    fun blockingCell(puzzle: ArrowsPuzzle, alive: BooleanArray, arrow: Int): Int {
        val a = puzzle.arrows[arrow]
        val n = puzzle.size
        var row = a.head / n + ArrowDir.dRow[a.dir]
        var column = a.head % n + ArrowDir.dColumn[a.dir]
        while (row in 0 until n && column in 0 until n) {
            val owner = puzzle.owner[row * n + column]
            if (owner >= 0 && alive[owner]) return row * n + column
            row += ArrowDir.dRow[a.dir]
            column += ArrowDir.dColumn[a.dir]
        }
        return -1
    }

    /** The arrow in the way of arrow [arrow], or null if it can leave. */
    fun blocker(puzzle: ArrowsPuzzle, alive: BooleanArray, arrow: Int): Int? =
        blockingCell(puzzle, alive, arrow).takeIf { it >= 0 }?.let { puzzle.owner[it] }

    /** The arrows on the board that can leave now, in order. */
    fun free(puzzle: ArrowsPuzzle, alive: BooleanArray): List<Int> =
        alive.indices.filter { alive[it] && blockingCell(puzzle, alive, it) < 0 }

    /**
     * Takes free arrows off the board one after the other (the first free one each time). An arrow that leaves never blocks
     * another one, it only frees it, so this order is a way out whenever there is one at all. Returns the order, and the arrows
     * left stuck (empty when the board can be cleared).
     */
    fun clearOrder(puzzle: ArrowsPuzzle, present: BooleanArray): Pair<List<Int>, List<Int>> {
        val alive = present.copyOf()
        val order = ArrayList<Int>()
        while (true) {
            val next = free(puzzle, alive).firstOrNull() ?: break
            alive[next] = false
            order += next
        }
        return order to alive.indices.filter { alive[it] }
    }
}
