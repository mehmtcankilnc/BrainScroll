package com.mehmtcan.brainscroll.game.lonenumber

import com.mehmtcan.brainscroll.game.BitText

/**
 * A board of lone numbers (Hitori): a square of [size] by [size] cells, each with a number from 1 to [size]. The player shades the cells
 * that repeat a number, so that no number stays twice in a row or a column among the cells left. The shaded cells may not touch each other
 * (by an edge) and the cells left must all be joined in one piece. [numbers] holds the number of every cell, row after row. The board is
 * saved as text (see [encode]): three bits a number, so the biggest board (7 by 7) fits the 32 characters the cloud table keeps.
 */
class LonePuzzle(val size: Int, val numbers: List<Int>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "size $size" }
        require(numbers.size == size * size) { "numbers ${numbers.size}" }
        require(numbers.all { it in 1..size }) { "a number is out of range" }
    }

    fun numberAt(column: Int, row: Int): Int = numbers[row * size + column]

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

    /** The size, then every number less one in 3 bits: 7 by 7 is 150 bits, 25 characters. */
    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 3)
        for (n in numbers) writer.put(n - 1, 3)
        return writer.text()
    }

    override fun equals(other: Any?) = other is LonePuzzle && size == other.size && numbers == other.numbers

    override fun hashCode() = 31 * size + numbers.hashCode()

    companion object {
        const val MIN_SIZE = 4
        const val MAX_SIZE = 7

        /** Reads what [encode] wrote. Null if it is not a board (so a saved game of another kind starts a new one). */
        fun decode(text: String): LonePuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(3) ?: return null) + MIN_SIZE
            if (size > MAX_SIZE) return null
            val numbers = List(size * size) { (reader.take(3) ?: return null) + 1 }
            return runCatching { LonePuzzle(size, numbers) }.getOrNull()
        }
    }
}
