package com.mehmtcan.brainscroll.game.axissum

import com.mehmtcan.brainscroll.game.BitText

/**
 * A board of axis sum: a square of numbers (1 to 9) and, for every row and column, the sum the numbers that are left must make.
 * [keep] is the one way to erase numbers that makes every sum; a valid board has no other (see [AxisSumSolver]), so it can be
 * solved by thinking alone. The sums are worked out from the numbers and [keep], they are not stored.
 *
 * [encode] is the saved text: the size, then for every cell its number and whether it stays, 5 bits a cell, six bits to a
 * character: 31 characters for the biggest board (6 by 6), under the 32 the cloud table allows.
 */
class AxisSumPuzzle(val size: Int, val digits: List<Int>, val keep: List<Boolean>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "a small board" }
        require(digits.size == size * size && keep.size == size * size) { "a number and a mark for every cell" }
        require(digits.all { it in 1..9 }) { "numbers from 1 to 9" }
        require(keep.any { it }) { "something stays" }
    }

    /** The sum each row must make: what the kept numbers of the row add up to. */
    val rowTargets: List<Int> = List(size) { r -> (0 until size).sumOf { c -> if (keep[r * size + c]) digits[r * size + c] else 0 } }
    val columnTargets: List<Int> = List(size) { c -> (0 until size).sumOf { r -> if (keep[r * size + c]) digits[r * size + c] else 0 } }

    /** How many numbers have to be erased. */
    val eraseCount: Int get() = keep.count { !it }

    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 2)
        for (i in digits.indices) {
            writer.put(digits[i] - 1, 4)
            writer.put(if (keep[i]) 1 else 0, 1)
        }
        return writer.text()
    }

    companion object {
        const val MIN_SIZE = 3
        const val MAX_SIZE = 6

        /** Null if [text] is not a board that [encode] wrote. */
        fun decode(text: String): AxisSumPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(2) ?: return null) + MIN_SIZE
            val digits = ArrayList<Int>()
            val keep = ArrayList<Boolean>()
            repeat(size * size) {
                digits += (reader.take(4) ?: return null) + 1
                keep += (reader.take(1) ?: return null) == 1
            }
            return try {
                AxisSumPuzzle(size, digits, keep)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
