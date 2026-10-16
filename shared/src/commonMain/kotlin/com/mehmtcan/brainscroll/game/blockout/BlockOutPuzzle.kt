package com.mehmtcan.brainscroll.game.blockout

/**
 * The shape of one piece: it slides along its own row ([horizontal]) or column and nowhere else. [line] is the row or column it
 * lies in, [length] the number of cells it covers. The target piece is the one that has to be driven out of the exit.
 */
data class PieceShape(val horizontal: Boolean, val line: Int, val length: Int, val isTarget: Boolean = false)

/**
 * A block out board: its size, its pieces and where each of them starts. A piece's place is one number, [start], the column of its
 * left cell (or the row of its top cell for an upright piece); that is all that changes when it slides.
 *
 * The target piece lies in the middle row, and the exit is on the right edge of that row: the game is won when the target
 * touches the right edge. The pieces are kept in the order of their first cell (reading order), so a board has one way of being
 * written, and a piece is known by its number in that order.
 *
 * [encode] writes the board as a short text of the cells (e.g. `AA..B.C..B.CXX.B...D...DEE` for 5 by 5), kept short for the
 * 32 characters of the cloud table; [decode] reads it back.
 */
class BlockOutPuzzle(val size: Int, val shapes: List<PieceShape>, val start: IntArray) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "a small board" }
        require(shapes.size == start.size && shapes.isNotEmpty()) { "a place for every piece" }
        require(shapes.count { it.isTarget } == 1) { "one target piece" }
        require(shapes.all { it.length in 2..size && it.line in 0 until size }) { "pieces fit the board" }
        val target = shapes.single { it.isTarget }
        require(target.horizontal && target.line == size / 2) { "the target lies in the middle row" }
        val occupied = BooleanArray(size * size)
        for (piece in shapes.indices) {
            val shape = shapes[piece]
            require(start[piece] in 0..size - shape.length) { "a piece stays on the board" }
            for (cell in cellsOf(size, shape, start[piece])) {
                require(!occupied[cell]) { "pieces do not overlap" }
                occupied[cell] = true
            }
        }
        val firsts = shapes.indices.map { firstCell(size, shapes[it], start[it]) }
        require(firsts == firsts.sorted()) { "pieces are in reading order" }
    }

    val targetIndex: Int = shapes.indexOfFirst { it.isTarget }

    /** The piece letters of each cell, '.' for an empty one and 'X' for the target; the other pieces are A, B, C ... in order. */
    fun encode(): String {
        val letters = CharArray(size * size) { '.' }
        var next = 'A'
        for (piece in shapes.indices) {
            val letter = if (shapes[piece].isTarget) 'X' else next++
            for (cell in cellsOf(size, shapes[piece], start[piece])) letters[cell] = letter
        }
        return letters.concatToString()
    }

    companion object {
        const val DEFAULT_SIZE = 5
        const val MIN_SIZE = 4
        const val MAX_SIZE = 6

        /** The cells a piece covers when its place is [pos]. */
        fun cellsOf(size: Int, shape: PieceShape, pos: Int): List<Int> =
            List(shape.length) { if (shape.horizontal) shape.line * size + pos + it else (pos + it) * size + shape.line }

        fun firstCell(size: Int, shape: PieceShape, pos: Int): Int = if (shape.horizontal) shape.line * size + pos else pos * size + shape.line

        /** Puts pieces given in any order into reading order, which is what a puzzle needs. */
        fun of(size: Int, shapes: List<PieceShape>, start: IntArray): BlockOutPuzzle {
            val order = shapes.indices.sortedBy { firstCell(size, shapes[it], start[it]) }
            return BlockOutPuzzle(size, order.map { shapes[it] }, IntArray(order.size) { start[order[it]] })
        }

        /** Null if [text] is not a board that [encode] wrote (or not a board that follows the rules). */
        fun decode(text: String): BlockOutPuzzle? {
            val size = when (text.length) {
                16 -> 4
                25 -> 5
                36 -> 6
                else -> return null
            }
            val shapes = ArrayList<PieceShape>()
            val starts = ArrayList<Int>()
            for (letter in text.filter { it != '.' }.toSet()) {
                if (letter != 'X' && letter !in 'A'..'Z') return null
                val cells = text.indices.filter { text[it] == letter }
                if (cells.size < 2) return null
                val horizontal = cells[1] - cells[0] == 1
                val step = if (horizontal) 1 else size
                if (cells.indices.any { cells[it] != cells[0] + it * step }) return null
                // A horizontal piece must not wrap round the end of a row.
                if (horizontal && cells.first() / size != cells.last() / size) return null
                shapes += PieceShape(horizontal, if (horizontal) cells[0] / size else cells[0] % size, cells.size, letter == 'X')
                starts += if (horizontal) cells[0] % size else cells[0] / size
            }
            return try {
                of(size, shapes, starts.toIntArray())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
