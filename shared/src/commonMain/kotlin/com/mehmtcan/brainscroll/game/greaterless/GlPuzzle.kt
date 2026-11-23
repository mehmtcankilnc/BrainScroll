package com.mehmtcan.brainscroll.game.greaterless

import com.mehmtcan.brainscroll.game.BitText

/**
 * A board of greater and less (Futoshiki): a square of [size] by [size] cells that the player fills with 1 to [size] so that no number
 * is twice in a row or a column and every sign between two cells is true. [given] holds the numbers that stand at the start (0 for the
 * cells left to the player), row after row. [right] and [down] hold the signs: for a cell, the sign between it and the cell on its right
 * (or below it): 0 none, [LESS] the cell is the smaller one, [MORE] the cell is the bigger one. Signs of the last column (and row) are 0.
 * The board is saved as text (see [encode]).
 */
class GlPuzzle(val size: Int, val given: List<Int>, val right: List<Int>, val down: List<Int>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "size $size" }
        require(given.size == size * size && right.size == size * size && down.size == size * size) { "cells" }
        require(given.all { it in 0..size }) { "a number is not 0 to $size" }
        require(right.indices.all { right[it] in 0..2 && (right[it] == 0 || it % size < size - 1) }) { "a sign off the board" }
        require(down.indices.all { down[it] in 0..2 && (down[it] == 0 || it / size < size - 1) }) { "a sign off the board" }
    }

    /** How many numbers are given. */
    val givenCount: Int get() = given.count { it > 0 }

    /** How many signs there are. */
    val signCount: Int get() = right.count { it > 0 } + down.count { it > 0 }

    fun isGiven(cell: Int): Boolean = given[cell] > 0

    /** Whether the board fits the 32 characters the cloud table keeps for a saved board. */
    val canBeSaved: Boolean get() = bits(size, givenCount, signCount) <= MAX_BITS

    /**
     * The size (3 bits), then for every cell 1 bit (is it given) and, if so, its number (3 bits), then for every place between two cells
     * 1 bit (is there a sign) and, if so, 1 bit (which way): 6 by 6 with 3 given and 14 signs is 3 + 36 + 9 + 60 + 14 = 122 bits.
     */
    fun encode(): String {
        check(canBeSaved) { "the board is too big to be saved" }
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 3)
        for (v in given) {
            writer.put(if (v > 0) 1 else 0, 1)
            if (v > 0) writer.put(v, 3)
        }
        for (cell in 0 until size * size) {
            if (cell % size < size - 1) putSign(writer, right[cell])
            if (cell / size < size - 1) putSign(writer, down[cell])
        }
        return writer.text()
    }

    private fun putSign(writer: BitText.Writer, sign: Int) {
        writer.put(if (sign > 0) 1 else 0, 1)
        if (sign > 0) writer.put(sign - 1, 1)
    }

    override fun equals(other: Any?) = other is GlPuzzle && size == other.size && given == other.given && right == other.right && down == other.down

    override fun hashCode() = 31 * (31 * (31 * size + given.hashCode()) + right.hashCode()) + down.hashCode()

    companion object {
        const val MIN_SIZE = 4
        const val MAX_SIZE = 7

        /** The cell is the smaller one of the two the sign sits between. */
        const val LESS = 1

        /** The cell is the bigger one. */
        const val MORE = 2

        /** The saved board has to fit the 32 characters (192 bits) of the cloud table. */
        const val MAX_BITS = 192

        /** How many bits a board with these numbers takes (see [encode]). */
        fun bits(size: Int, given: Int, signs: Int): Int = 3 + size * size + 3 * given + 2 * size * (size - 1) + signs

        /** Reads what [encode] wrote. Null if it is not a board (so a saved game of another kind starts a new one). */
        fun decode(text: String): GlPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(3) ?: return null) + MIN_SIZE
            if (size > MAX_SIZE) return null
            val given = List(size * size) { if ((reader.take(1) ?: return null) == 1) (reader.take(3) ?: return null) else 0 }
            val right = IntArray(size * size)
            val down = IntArray(size * size)
            fun sign(): Int? {
                if ((reader.take(1) ?: return null) == 0) return 0
                return (reader.take(1) ?: return null) + 1
            }
            for (cell in 0 until size * size) {
                if (cell % size < size - 1) right[cell] = sign() ?: return null
                if (cell / size < size - 1) down[cell] = sign() ?: return null
            }
            return runCatching { GlPuzzle(size, given, right.toList(), down.toList()) }.getOrNull()
        }
    }
}
