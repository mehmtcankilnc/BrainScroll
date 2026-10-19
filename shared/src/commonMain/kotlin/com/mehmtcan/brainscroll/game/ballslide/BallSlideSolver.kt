package com.mehmtcan.brainscroll.game.ballslide

/**
 * Solving ball slide by breadth first search over (where the ball is, which cells are painted): every move is one of four
 * slides, so the first state in which every open cell is painted is reached by the fewest moves. A search stops at [NODE_LIMIT]
 * states, so a board that is too big to settle quickly counts as unsolved (the generator throws it away).
 */
object BallSlideSolver {
    const val NODE_LIMIT = 400_000

    /**
     * The directions of a shortest way to paint every cell, from the ball on [from] with the cells in [painted] (a bit per cell)
     * already painted; empty if it is done already, null if there is no way (or the search gave up).
     */
    fun shortest(puzzle: BallSlidePuzzle, from: Int, painted: Long): List<Int>? {
        val n = puzzle.cellCount
        val all = (0 until n).filter { !puzzle.walls[it] }.fold(0L) { acc, i -> acc or (1L shl i) }
        if (painted and all == all) return emptyList()
        // For every cell and direction: where the slide ends and which cells it paints.
        val destination = IntArray(n * 4) { -1 }
        val paint = LongArray(n * 4)
        for (cell in 0 until n) {
            if (puzzle.walls[cell]) continue
            for (dir in 0 until 4) {
                val path = puzzle.slide(cell, dir)
                if (path.isEmpty()) continue
                destination[cell * 4 + dir] = path.last()
                paint[cell * 4 + dir] = path.fold(0L) { acc, i -> acc or (1L shl i) }
            }
        }
        // A state is the painted cells (up to 36 bits) and the cell of the ball (6 bits).
        fun key(mask: Long, cell: Int) = (mask shl 6) or cell.toLong()
        val startMask = painted or (1L shl from)
        val parent = HashMap<Long, Long>()
        val moveInto = HashMap<Long, Int>()
        val first = key(startMask, from)
        parent[first] = -1L
        var frontier = arrayListOf(first)
        while (frontier.isNotEmpty()) {
            val next = ArrayList<Long>()
            for (state in frontier) {
                val cell = (state and 63L).toInt()
                val mask = state shr 6
                for (dir in 0 until 4) {
                    val to = destination[cell * 4 + dir]
                    if (to < 0) continue
                    val newMask = mask or paint[cell * 4 + dir]
                    val newState = key(newMask, to)
                    if (newState in parent) continue
                    parent[newState] = state
                    moveInto[newState] = dir
                    if (newMask and all == all) {
                        val way = ArrayList<Int>()
                        var current = newState
                        while (current != first) {
                            way += moveInto.getValue(current)
                            current = parent.getValue(current)
                        }
                        return way.asReversed()
                    }
                    next += newState
                }
            }
            if (parent.size > NODE_LIMIT) return null
            frontier = next
        }
        return null
    }

    /** The fewest slides that paint the whole board from its start, or null if it cannot be won. */
    fun par(puzzle: BallSlidePuzzle): Int? = shortest(puzzle, puzzle.start, 1L shl puzzle.start)?.size
}
