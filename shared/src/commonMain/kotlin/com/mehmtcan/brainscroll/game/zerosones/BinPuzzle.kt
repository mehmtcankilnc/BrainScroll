package com.mehmtcan.brainscroll.game.zerosones

import com.mehmtcan.brainscroll.game.BitText

/**
 * A board of zeros and ones (Binairo): a square of [size] by [size] cells that the player fills with 0 and 1 so that no row or column
 * has three of the same digit side by side, every row and column has as many 0s as 1s, and no two rows (and no two columns) are the
 * same. [given] holds the cells that are filled at the start (0 or 1) and -1 for the ones left to the player, row after row.
 * The board is saved as text (see [encode]): which cells are given, then the digit of each of them.
 */
class BinPuzzle(val size: Int, val given: List<Int>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE && size % 2 == 0) { "size $size" }
        require(given.size == size * size) { "cells ${given.size}" }
        require(given.all { it in -1..1 }) { "a cell is not -1, 0 or 1" }
        require(given.count { it >= 0 } <= maxGiven(size)) { "too many given cells" }
    }

    /** How many cells are given. */
    val givenCount: Int get() = given.count { it >= 0 }

    fun isGiven(cell: Int): Boolean = given[cell] >= 0

    /** The size (3 bits), one bit for every cell (is it given), then the digit of every given cell (1 bit): 10 by 10 with 38 given is 141 bits. */
    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 3)
        for (v in given) writer.put(if (v >= 0) 1 else 0, 1)
        for (v in given) if (v >= 0) writer.put(v, 1)
        return writer.text()
    }

    override fun equals(other: Any?) = other is BinPuzzle && size == other.size && given == other.given

    override fun hashCode() = 31 * size + given.hashCode()

    companion object {
        const val MIN_SIZE = 4
        const val MAX_SIZE = 10

        /** The most given cells a board can have and still be saved in the 32 characters (192 bits) the cloud table keeps. */
        fun maxGiven(size: Int): Int = 192 - 3 - size * size

        /** Reads what [encode] wrote. Null if it is not a board (so a saved game of another kind starts a new one). */
        fun decode(text: String): BinPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(3) ?: return null) + MIN_SIZE
            if (size > MAX_SIZE) return null
            val has = List(size * size) { reader.take(1) ?: return null }
            val given = has.map { if (it == 1) (reader.take(1) ?: return null) else -1 }
            return runCatching { BinPuzzle(size, given) }.getOrNull()
        }
    }
}
