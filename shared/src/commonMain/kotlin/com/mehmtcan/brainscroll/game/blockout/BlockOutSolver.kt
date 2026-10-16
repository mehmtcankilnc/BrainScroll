package com.mehmtcan.brainscroll.game.blockout

/** One move: the piece [piece] slides along its line to the place [to] (the same number as [BlockOutPuzzle.start]). */
data class BlockMove(val piece: Int, val to: Int)

/** Where a piece can slide to, and the shortest way out. A board is small, so a breadth-first search over its positions is exact and quick. */
object BlockOutSolver {

    /** The cells held by the pieces, except [except] (-1 for none), for the pieces at [pos]. */
    fun occupied(size: Int, shapes: List<PieceShape>, pos: IntArray, except: Int = -1): BooleanArray {
        val taken = BooleanArray(size * size)
        for (piece in shapes.indices) {
            if (piece == except) continue
            for (cell in BlockOutPuzzle.cellsOf(size, shapes[piece], pos[piece])) taken[cell] = true
        }
        return taken
    }

    /** The places [piece] can slide to in one move, its own included: from the lowest to the highest it can reach before it meets another piece or the edge. */
    fun range(size: Int, shapes: List<PieceShape>, pos: IntArray, piece: Int): IntRange {
        val shape = shapes[piece]
        val taken = occupied(size, shapes, pos, piece)
        fun free(place: Int): Boolean = BlockOutPuzzle.cellsOf(size, shape, place).all { !taken[it] }
        var low = pos[piece]
        while (low > 0 && free(low - 1)) low--
        var high = pos[piece]
        while (high < size - shape.length && free(high + 1)) high++
        return low..high
    }

    fun isSolved(size: Int, shapes: List<PieceShape>, pos: IntArray): Boolean {
        val target = shapes.indexOfFirst { it.isTarget }
        return pos[target] + shapes[target].length == size
    }

    /**
     * A shortest list of moves that drives the target to the exit, empty if it is there, or null if the board cannot be solved.
     *
     * The search works on whole positions packed into one number (3 bits for the place of each piece) and on bit masks of the cells,
     * because the generator asks for the shortest way of thousands of boards and a position is looked at once per way to reach it.
     */
    fun shortest(size: Int, shapes: List<PieceShape>, start: IntArray): List<BlockMove>? {
        if (isSolved(size, shapes, start)) return emptyList()
        val count = shapes.size
        require(count <= 16 && size <= 7) { "a small board" }
        // masks[piece][place]: the cells the piece covers at that place, one bit per cell.
        val masks = Array(count) { piece -> LongArray(size - shapes[piece].length + 1) { place -> BlockOutPuzzle.cellsOf(size, shapes[piece], place).fold(0L) { bits, cell -> bits or (1L shl cell) } } }
        val target = shapes.indexOfFirst { it.isTarget }
        val goalPlace = size - shapes[target].length

        fun pack(pos: IntArray): Long {
            var key = 0L
            for (piece in 0 until count) key = key or (pos[piece].toLong() shl (3 * piece))
            return key
        }

        val startKey = pack(start)
        // For every position reached: where it came from and the move that led there (piece * 8 + place).
        val cameFrom = HashMap<Long, Long>()
        cameFrom[startKey] = -1L
        val queue = ArrayDeque<Long>()
        queue.addLast(startKey)
        val pos = IntArray(count)
        while (queue.isNotEmpty()) {
            val key = queue.removeFirst()
            var occupied = 0L
            for (piece in 0 until count) {
                pos[piece] = ((key shr (3 * piece)) and 7L).toInt()
                occupied = occupied or masks[piece][pos[piece]]
            }
            for (piece in 0 until count) {
                val own = masks[piece][pos[piece]]
                val others = occupied and own.inv()
                val last = masks[piece].size - 1
                // Every place the piece reaches by sliding one way and then the other, stopping at the first piece in the way.
                for (direction in intArrayOf(-1, 1)) {
                    var place = pos[piece] + direction
                    while (place in 0..last && masks[piece][place] and others == 0L) {
                        val next = (key and (7L shl (3 * piece)).inv()) or (place.toLong() shl (3 * piece))
                        if (next !in cameFrom) {
                            cameFrom[next] = (key shl 7) or (piece * 8 + place).toLong()
                            if (piece == target && place == goalPlace) return path(cameFrom, next, startKey)
                            queue.addLast(next)
                        }
                        place += direction
                    }
                }
            }
        }
        return null
    }

    private fun path(cameFrom: Map<Long, Long>, end: Long, startKey: Long): List<BlockMove> {
        val moves = ArrayList<BlockMove>()
        var at = end
        while (at != startKey) {
            val step = cameFrom.getValue(at)
            val code = (step and 127L).toInt()
            moves += BlockMove(code / 8, code % 8)
            at = step shr 7
        }
        return moves.reversed()
    }

    /** The fewest moves from [start]. */
    fun distance(size: Int, shapes: List<PieceShape>, start: IntArray): Int? = shortest(size, shapes, start)?.size
}
