package com.mehmtcan.brainscroll.game.matchsticks

import com.mehmtcan.brainscroll.game.BitText

/** A place for a stick: [cell] is the symbol (see [MatchPuzzle]) and [bit] the place in it (a digit has seven, a sign two). */
data class MatchSlot(val cell: Int, val bit: Int)

/** Taking the stick on [from] and laying it on [to]. */
data class MatchMove(val from: MatchSlot, val to: MatchSlot)

/**
 * A sum made of matchsticks that is wrong by one stick: `A ± B = C` with one-digit numbers, or `A + B = CD` with a two-digit
 * result. The player takes one stick and lays it somewhere else so that the sum is right. The sum is a list of symbols, each
 * one the sticks it has as bits ([cells]): first number, sign, second number, then the result (one or two digits). The
 * equals sign is always there and has no sticks to move.
 *
 * A digit is seven places (the seven segments `a`..`g`, bit 0 is `a`): the usual shapes, 6 and 9 with their tails, 7 without the
 * top left stick. A sign is two places: bit 0 is the flat stick (a minus), bit 1 the upright one (it makes a plus).
 *
 * [encode] is the saved text: whether the result has two digits and the sticks of each symbol, six bits to a character, so a
 * board is at most 6 characters.
 */
class MatchPuzzle(val cells: List<Int>) {
    init {
        require(cells.size == SHORT || cells.size == LONG) { "four or five symbols" }
        for ((i, mask) in cells.withIndex()) require(mask in 0 until (1 shl MatchRules.width(i))) { "sticks of a symbol" }
    }

    /** Every place there is, counted symbol after symbol (a slot's number is its place in this list). */
    val slots: List<MatchSlot> = MatchRules.slotsOf(cells.size)

    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(if (cells.size == LONG) 1 else 0, 1)
        for ((i, mask) in cells.withIndex()) writer.put(mask, MatchRules.width(i))
        return writer.text()
    }

    override fun equals(other: Any?) = other is MatchPuzzle && other.cells == cells

    override fun hashCode() = cells.hashCode()

    companion object {
        const val SHORT = 4
        const val LONG = 5

        /** Null if [text] is not a board that [encode] wrote. */
        fun decode(text: String): MatchPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = if ((reader.take(1) ?: return null) == 1) LONG else SHORT
            val cells = ArrayList<Int>()
            for (i in 0 until size) cells += reader.take(MatchRules.width(i)) ?: return null
            return try {
                MatchPuzzle(cells)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}

/** What the sticks of a sum read as. [result] is the number the sum is equal to, [value] what the left side comes to. */
class MatchReading(val first: Int, val plus: Boolean, val second: Int, val result: Int) {
    val value: Int get() = if (plus) first + second else first - second
    val isTrue: Boolean get() = value == result
}

/** The rules of the sticks: which symbols exist, how a sum reads, and the moves there are. */
object MatchRules {
    /** The sticks of the digits 0 to 9 (bit 0 is the top stick, then clockwise, bit 6 the middle one). */
    private val DIGITS = intArrayOf(
        0b0111111, 0b0000110, 0b1011011, 0b1001111, 0b1100110,
        0b1101101, 0b1111101, 0b0000111, 0b1111111, 0b1101111,
    )

    const val MINUS = 1
    const val PLUS = 3

    fun digitMask(digit: Int): Int = DIGITS[digit]

    /** The digit the sticks [mask] show, or -1 if they show none. */
    fun digitOf(mask: Int): Int = DIGITS.indexOf(mask)

    /** Every place of a sum with [count] symbols, symbol after symbol. */
    fun slotsOf(count: Int): List<MatchSlot> = (0 until count).flatMap { c -> (0 until width(c)).map { MatchSlot(c, it) } }

    /** How many places the symbol [cell] has: the sign (symbol 1) has two, every digit seven. */
    fun width(cell: Int): Int = if (cell == 1) 2 else 7

    /** What the symbols read as, or null if one of them is not a digit or a sign, or a two-digit result starts with a 0. */
    fun read(cells: List<Int>): MatchReading? {
        if (cells.size != MatchPuzzle.SHORT && cells.size != MatchPuzzle.LONG) return null
        val first = digitOf(cells[0])
        val second = digitOf(cells[2])
        val plus = when (cells[1]) {
            PLUS -> true
            MINUS -> false
            else -> return null
        }
        if (first < 0 || second < 0) return null
        var result = digitOf(cells[3])
        if (result < 0) return null
        if (cells.size == MatchPuzzle.LONG) {
            val units = digitOf(cells[4])
            if (units < 0 || result == 0) return null
            result = result * 10 + units
        }
        return MatchReading(first, plus, second, result)
    }

    fun isValid(cells: List<Int>): Boolean = read(cells) != null

    fun isTrue(cells: List<Int>): Boolean = read(cells)?.isTrue == true

    fun apply(cells: List<Int>, move: MatchMove): List<Int> {
        val next = cells.toMutableList()
        next[move.from.cell] = next[move.from.cell] and (1 shl move.from.bit).inv()
        next[move.to.cell] = next[move.to.cell] or (1 shl move.to.bit)
        return next
    }

    /** Every way to take a stick and lay it on an empty place (the sum may not read as anything after it). */
    fun moves(cells: List<Int>): List<MatchMove> {
        val out = ArrayList<MatchMove>()
        for (c in cells.indices) for (b in 0 until width(c)) {
            if ((cells[c] shr b) and 1 == 0) continue
            for (d in cells.indices) for (e in 0 until width(d)) {
                if ((cells[d] shr e) and 1 == 1 || (c == d && b == e)) continue
                out += MatchMove(MatchSlot(c, b), MatchSlot(d, e))
            }
        }
        return out
    }

    /** The moves after which the sum is right. */
    fun winningMoves(cells: List<Int>): List<MatchMove> = moves(cells).filter { isTrue(apply(cells, it)) }

    /** True if [move] only takes a stick from a symbol to put it back on the same symbol. */
    fun staysInSymbol(move: MatchMove): Boolean = move.from.cell == move.to.cell

    /** The symbols of the sum [first] [plus] [second] = [result] (a two-digit result when it is 10 or more). */
    fun cellsOf(first: Int, plus: Boolean, second: Int, result: Int): List<Int> {
        val head = listOf(DIGITS[first], if (plus) PLUS else MINUS, DIGITS[second])
        return if (result >= 10) head + listOf(DIGITS[result / 10], DIGITS[result % 10]) else head + DIGITS[result]
    }
}
