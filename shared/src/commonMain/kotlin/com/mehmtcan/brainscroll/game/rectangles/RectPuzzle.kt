package com.mehmtcan.brainscroll.game.rectangles

import com.mehmtcan.brainscroll.game.BitText

/** A rectangle of cells: its top left cell ([x] across, [y] down) and its [w] width and [h] height. */
data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
    val area: Int get() = w * h

    fun contains(column: Int, row: Int): Boolean = column in x until x + w && row in y until y + h

    fun overlaps(o: Rect): Boolean = x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h

    /** The cell numbers (row * size + column) it covers. */
    fun cells(size: Int): List<Int> = (y until y + h).flatMap { row -> (x until x + w).map { column -> row * size + column } }
}

/**
 * A board of rectangles (Shikaku): a square of [size] by [size] cells, some with a number. The player divides the board into rectangles,
 * each holding exactly one number, and that number is its area. [numbers] holds the number of every cell (0 for none), row after row.
 * The numbers add up to the number of cells, so the rectangles cover everything. The board is saved as text (see [encode]).
 */
class RectPuzzle(val size: Int, val numbers: List<Int>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "size $size" }
        require(numbers.size == size * size) { "numbers ${numbers.size}" }
        require(numbers.all { it in 0..MAX_NUMBER }) { "a number is too big" }
        require(numbers.sum() == size * size) { "the numbers add up to ${numbers.sum()}" }
        require(numbers.count { it > 0 } <= MAX_CLUES) { "too many numbers" }
    }

    /** The cells that hold a number, in order. */
    val clueCells: List<Int> = numbers.indices.filter { numbers[it] > 0 }

    fun numberAt(column: Int, row: Int): Int = numbers[row * size + column]

    /** Whether [rect] is a rectangle of this board that holds one number and is as big as it says (it may still be the wrong one). */
    fun isFilledRight(rect: Rect): Boolean {
        if (rect.x < 0 || rect.y < 0 || rect.x + rect.w > size || rect.y + rect.h > size || rect.w < 1 || rect.h < 1) return false
        val inside = rect.cells(size).filter { numbers[it] > 0 }
        return inside.size == 1 && numbers[inside[0]] == rect.area
    }

    /** One bit for every cell (is there a number), then the numbers less one in 4 bits each: at most 8x8 and 24 numbers fit 32 characters. */
    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 3)
        for (n in numbers) writer.put(if (n > 0) 1 else 0, 1)
        for (n in numbers) if (n > 0) writer.put(n - 1, 4)
        return writer.text()
    }

    override fun equals(other: Any?) = other is RectPuzzle && size == other.size && numbers == other.numbers

    override fun hashCode() = 31 * size + numbers.hashCode()

    companion object {
        const val MIN_SIZE = 4
        const val MAX_SIZE = 8
        const val MAX_NUMBER = 16
        const val MAX_CLUES = 24

        /** Reads what [encode] wrote. Null if it is not a board (so a saved game of another kind starts a new one). */
        fun decode(text: String): RectPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(3) ?: return null) + MIN_SIZE
            if (size > MAX_SIZE) return null
            val has = List(size * size) { reader.take(1) ?: return null }
            val numbers = has.map { if (it == 1) (reader.take(4) ?: return null) + 1 else 0 }
            return runCatching { RectPuzzle(size, numbers) }.getOrNull()
        }
    }
}
