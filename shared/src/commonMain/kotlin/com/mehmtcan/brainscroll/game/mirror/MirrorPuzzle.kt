package com.mehmtcan.brainscroll.game.mirror

import com.mehmtcan.brainscroll.game.BitText

/** The line (or the point) a board is mirrored on. */
enum class MirrorAxis {
    /** A vertical line in the middle: the left half is given, the right half is to be filled. */
    Vertical,

    /** A horizontal line in the middle: the top half is given, the bottom half is to be filled. */
    Horizontal,

    /** The diagonal from the top left to the bottom right: the cells on and above it are given, the ones below are to be filled. */
    Diagonal,

    /** The middle of the board (a half turn): the first half of the cells is given, the other half is to be filled. */
    Point,
}

/**
 * A board of mirror: a square of [size] by [size] cells, each painted or empty. One half is given ([pattern] holds its cells, the cells
 * of the other half are false). The player paints the other half so that the board is the same on both sides of the [axis]. The
 * answer is not a search: every cell of the other half is the picture of its [partner], so there is exactly one.
 * The board is saved as text (see [encode]): one bit for each given cell, a few bits for the size and the axis.
 */
class MirrorPuzzle(val size: Int, val axis: MirrorAxis, val pattern: List<Boolean>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE && size % 2 == 0) { "size $size" }
        require(pattern.size == size * size) { "pattern ${pattern.size}" }
        require(pattern.indices.all { isGiven(it) || !pattern[it] }) { "the half to fill starts empty" }
    }

    /** True for a cell of the given half (it never changes). */
    fun isGiven(cell: Int): Boolean {
        val row = cell / size
        val column = cell % size
        return when (axis) {
            MirrorAxis.Vertical -> column < size / 2
            MirrorAxis.Horizontal -> row < size / 2
            MirrorAxis.Diagonal -> column >= row
            MirrorAxis.Point -> cell < size * size / 2
        }
    }

    /** The cell that [cell] is the picture of (a cell of the given half for a cell to fill). */
    fun partner(cell: Int): Int {
        val row = cell / size
        val column = cell % size
        return when (axis) {
            MirrorAxis.Vertical -> row * size + (size - 1 - column)
            MirrorAxis.Horizontal -> (size - 1 - row) * size + column
            MirrorAxis.Diagonal -> column * size + row
            MirrorAxis.Point -> size * size - 1 - cell
        }
    }

    /** The cells the player fills, in order. */
    val fillCells: List<Int> by lazy { pattern.indices.filter { !isGiven(it) } }

    /** What a cell of the half to fill has to be: the same as its partner. */
    fun target(cell: Int): Boolean = pattern[partner(cell)]

    /** How many cells of the half to fill have to be painted. */
    val paintedTarget: Int by lazy { fillCells.count { target(it) } }

    /** The size and the axis, then one bit for every given cell: 8 by 8 is 5 + 36 bits, 7 characters. */
    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 3)
        writer.put(axis.ordinal, 2)
        for (cell in pattern.indices) if (isGiven(cell)) writer.put(if (pattern[cell]) 1 else 0, 1)
        return writer.text()
    }

    override fun equals(other: Any?) = other is MirrorPuzzle && size == other.size && axis == other.axis && pattern == other.pattern

    override fun hashCode() = 31 * (31 * size + axis.hashCode()) + pattern.hashCode()

    companion object {
        const val MIN_SIZE = 4
        const val MAX_SIZE = 8

        /** Reads what [encode] wrote. Null if it is not a board (so a saved game of another kind starts a new one). */
        fun decode(text: String): MirrorPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(3) ?: return null) + MIN_SIZE
            if (size > MAX_SIZE || size % 2 != 0) return null
            val axis = MirrorAxis.entries.getOrNull(reader.take(2) ?: return null) ?: return null
            val bare = shape(size, axis)
            val pattern = MutableList(size * size) { false }
            for (cell in pattern.indices) if (bare.isGiven(cell)) pattern[cell] = (reader.take(1) ?: return null) == 1
            return runCatching { MirrorPuzzle(size, axis, pattern) }.getOrNull()
        }

        /** An empty board of this shape (to ask which cells are given). */
        fun shape(size: Int, axis: MirrorAxis) = MirrorPuzzle(size, axis, List(size * size) { false })
    }
}
