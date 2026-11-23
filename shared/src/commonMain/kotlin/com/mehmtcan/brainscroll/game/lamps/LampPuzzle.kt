package com.mehmtcan.brainscroll.game.lamps

import com.mehmtcan.brainscroll.game.BitText

/**
 * A board of lamps (Akari): a square of [size] by [size] cells, some of them walls. The player puts lamps on the white cells so that
 * every white cell is lit (a lamp lights its row and its column, up to the next wall), no two lamps light each other, and a wall that
 * has a number has that many lamps on the cells next to it (by an edge). [cells] holds, row after row, [WHITE] for a white cell, the
 * number 0 to 4 for a wall with a number, and [BLANK_WALL] for a wall without one. The board is saved as text (see [encode]).
 */
class LampPuzzle(val size: Int, val cells: List<Int>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "size $size" }
        require(cells.size == size * size) { "cells ${cells.size}" }
        require(cells.all { it == WHITE || it in 0..BLANK_WALL }) { "a cell is not white, a wall or a number" }
        require(wallCount <= maxWalls(size)) { "too many walls" }
    }

    /** How many walls the board has. */
    val wallCount: Int get() = cells.count { it != WHITE }

    /** The cells that are not walls. */
    val whiteCells: List<Int> = cells.indices.filter { cells[it] == WHITE }

    fun isWall(cell: Int): Boolean = cells[cell] != WHITE

    /** The number on a wall, or -1 for a white cell and for a wall without a number. */
    fun numberAt(cell: Int): Int = if (cells[cell] in 0..MAX_NUMBER) cells[cell] else -1

    /** The cells (row * size + column) next to [cell] by an edge. */
    fun neighbours(cell: Int): List<Int> {
        val column = cell % size
        val row = cell / size
        val out = ArrayList<Int>(4)
        if (row > 0) out += cell - size
        if (row < size - 1) out += cell + size
        if (column > 0) out += cell - 1
        if (column < size - 1) out += cell + 1
        return out
    }

    private val seenCache = arrayOfNulls<List<Int>>(size * size)

    /** The white cells a lamp on [cell] lights besides its own cell: along its row and its column until a wall. */
    fun seenFrom(cell: Int): List<Int> = seenCache[cell] ?: computeSeen(cell).also { seenCache[cell] = it }

    private fun computeSeen(cell: Int): List<Int> {
        val out = ArrayList<Int>()
        val column = cell % size
        val row = cell / size
        for ((dr, dc) in DIRECTIONS) {
            var r = row + dr
            var c = column + dc
            while (r in 0 until size && c in 0 until size && cells[r * size + c] == WHITE) {
                out += r * size + c
                r += dr
                c += dc
            }
        }
        return out
    }

    /** The size (3 bits), one bit for every cell (is it a wall), then 3 bits for every wall (its number, or 5): 10 by 10 with 20 walls is 163 bits. */
    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 3)
        for (v in cells) writer.put(if (v != WHITE) 1 else 0, 1)
        for (v in cells) if (v != WHITE) writer.put(v, 3)
        return writer.text()
    }

    override fun equals(other: Any?) = other is LampPuzzle && size == other.size && cells == other.cells

    override fun hashCode() = 31 * size + cells.hashCode()

    companion object {
        const val WHITE = -1
        const val BLANK_WALL = 5
        const val MAX_NUMBER = 4
        const val MIN_SIZE = 4
        const val MAX_SIZE = 10
        private val DIRECTIONS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

        /** The most walls a board can have and still be saved in the 32 characters (192 bits) the cloud table keeps. */
        fun maxWalls(size: Int): Int = (192 - 3 - size * size) / 3

        /** Reads what [encode] wrote. Null if it is not a board (so a saved game of another kind starts a new one). */
        fun decode(text: String): LampPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(3) ?: return null) + MIN_SIZE
            if (size > MAX_SIZE) return null
            val isWall = List(size * size) { reader.take(1) ?: return null }
            val cells = isWall.map { if (it == 1) (reader.take(3) ?: return null) else WHITE }
            return runCatching { LampPuzzle(size, cells) }.getOrNull()
        }
    }
}
