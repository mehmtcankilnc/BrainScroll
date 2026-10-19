package com.mehmtcan.brainscroll.game.colorflow

import com.mehmtcan.brainscroll.game.BitText

/**
 * A board of color flow: a square and, for each color, the two cells its dots are on. The
 * player joins the two dots of every color with a path (a chain of cells that touch by a side), the paths never share a cell, and
 * together they cover every cell. A valid board has such paths (see [FlowSolver]).
 *
 * [encode] is the saved text: the size, the number of colors, and the two cells of each color (6 bits a cell), six bits to a
 * character: 15 characters for the biggest board.
 */
class FlowPuzzle(val size: Int, val pairs: List<Pair<Int, Int>>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "a small board" }
        require(pairs.size in MIN_COLORS..MAX_COLORS) { "a few colors" }
        val cells = pairs.flatMap { listOf(it.first, it.second) }
        require(cells.all { it in 0 until size * size }) { "dots are on the board" }
        require(cells.toSet().size == cells.size) { "no cell holds two dots" }
    }

    val colorCount: Int get() = pairs.size

    /** The color whose dot is on [cell], or -1. */
    fun colorAt(cell: Int): Int = pairs.indexOfFirst { it.first == cell || it.second == cell }

    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 2)
        writer.put(pairs.size - MIN_COLORS, 3)
        for ((a, b) in pairs) {
            writer.put(a, 6)
            writer.put(b, 6)
        }
        return writer.text()
    }

    companion object {
        const val MIN_SIZE = 4
        const val MAX_SIZE = 7
        const val MIN_COLORS = 2
        const val MAX_COLORS = 8

        /** Null if [text] is not a board that [encode] wrote. */
        fun decode(text: String): FlowPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(2) ?: return null) + MIN_SIZE
            val colors = (reader.take(3) ?: return null) + MIN_COLORS
            val pairs = ArrayList<Pair<Int, Int>>()
            repeat(colors) {
                val a = reader.take(6) ?: return null
                val b = reader.take(6) ?: return null
                pairs += a to b
            }
            return try {
                FlowPuzzle(size, pairs)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
