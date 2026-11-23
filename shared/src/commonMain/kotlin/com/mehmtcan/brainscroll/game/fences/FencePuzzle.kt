package com.mehmtcan.brainscroll.game.fences

import com.mehmtcan.brainscroll.game.BitText

/**
 * The shape of a board of [size] by [size] squares: its dots, and the edges that join two dots next to each other. The edges are
 * numbered the horizontal ones first (row of dots, then column of the square: `row * size + column`, `size + 1` rows of `size` edges),
 * then the vertical ones (`size * (size + 1) + row * (size + 1) + column`, `size` rows of `size + 1` edges). A dot is
 * `row * (size + 1) + column`, a square `row * size + column`.
 */
class FenceGeometry private constructor(val size: Int) {
    val dotSide = size + 1
    val dots = dotSide * dotSide
    val horizontal = size * dotSide
    val edges = 2 * horizontal

    /** The two dots of every edge. */
    val edgeDots: Array<IntArray> = Array(edges) { e ->
        if (e < horizontal) {
            val row = e / size
            val column = e % size
            intArrayOf(row * dotSide + column, row * dotSide + column + 1)
        } else {
            val i = e - horizontal
            val row = i / dotSide
            val column = i % dotSide
            intArrayOf(row * dotSide + column, (row + 1) * dotSide + column)
        }
    }

    /** The four edges around every square: top, bottom, left, right. */
    val cellEdges: Array<IntArray> = Array(size * size) { cell ->
        val row = cell / size
        val column = cell % size
        intArrayOf(
            row * size + column,
            (row + 1) * size + column,
            horizontal + row * dotSide + column,
            horizontal + row * dotSide + column + 1,
        )
    }

    /** The edges that end at every dot (two to four). */
    val dotEdges: Array<IntArray> = run {
        val lists = Array(dots) { ArrayList<Int>(4) }
        for (e in 0 until edges) for (d in edgeDots[e]) lists[d] += e
        Array(dots) { lists[it].toIntArray() }
    }

    /** The squares an edge is a side of (one or two). */
    val edgeCells: Array<IntArray> = run {
        val lists = Array(edges) { ArrayList<Int>(2) }
        for (cell in 0 until size * size) for (e in cellEdges[cell]) lists[e] += cell
        Array(edges) { lists[it].toIntArray() }
    }

    fun isHorizontal(edge: Int) = edge < horizontal

    companion object {
        private val cache = arrayOfNulls<FenceGeometry>(16)

        fun of(size: Int): FenceGeometry = cache[size] ?: FenceGeometry(size).also { cache[size] = it }
    }
}

/**
 * A board of fences (Slitherlink): a square of [size] by [size] squares, some with a number from 0 to 3. The player draws one closed
 * loop along the edges of the squares, so that a square with a number has that many of its four sides on the loop. The loop never
 * crosses itself or branches. [clues] holds the number of every square, row after row, and -1 for a square with none.
 * The board is saved as text (see [encode]).
 */
class FencePuzzle(val size: Int, val clues: List<Int>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "size $size" }
        require(clues.size == size * size) { "cells ${clues.size}" }
        require(clues.all { it in -1..3 }) { "a clue is not -1 to 3" }
    }

    val geometry: FenceGeometry get() = FenceGeometry.of(size)

    /** How many squares have a number. */
    val clueCount: Int get() = clues.count { it >= 0 }

    fun hasClue(cell: Int): Boolean = clues[cell] >= 0

    /** Whether the board fits the 32 characters the cloud table keeps for a saved board. */
    val canBeSaved: Boolean get() = bits(size, clueCount) <= MAX_BITS

    /** The size (3 bits), then for every square 1 bit (has it a number) and, if so, the number (2 bits): 8 by 8 with 40 numbers is 147 bits. */
    fun encode(): String {
        check(canBeSaved) { "the board is too big to be saved" }
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 3)
        for (v in clues) {
            writer.put(if (v >= 0) 1 else 0, 1)
            if (v >= 0) writer.put(v, 2)
        }
        return writer.text()
    }

    override fun equals(other: Any?) = other is FencePuzzle && size == other.size && clues == other.clues

    override fun hashCode() = 31 * size + clues.hashCode()

    companion object {
        const val MIN_SIZE = 4
        const val MAX_SIZE = 9

        /** The saved board has to fit the 32 characters (192 bits) of the cloud table. */
        const val MAX_BITS = 192

        /** How many bits a board with this many numbers takes (see [encode]). */
        fun bits(size: Int, clues: Int): Int = 3 + size * size + 2 * clues

        /** Reads what [encode] wrote. Null if it is not a board (so a saved game of another kind starts a new one). */
        fun decode(text: String): FencePuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(3) ?: return null) + MIN_SIZE
            if (size > MAX_SIZE) return null
            val clues = List(size * size) { if ((reader.take(1) ?: return null) == 1) (reader.take(2) ?: return null) else -1 }
            return runCatching { FencePuzzle(size, clues) }.getOrNull()
        }
    }
}
