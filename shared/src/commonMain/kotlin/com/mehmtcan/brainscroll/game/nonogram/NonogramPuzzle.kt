package com.mehmtcan.brainscroll.game.nonogram

/**
 * A hand drawn picture on a [size] by [size] board. [pattern] holds `size * size` characters read row by row, `#` for a filled cell
 * and `.` for an empty one. The lists are generated (see NonogramPictures.kt).
 */
class NonogramPicture(val id: String, val nameTr: String, val nameEn: String, val size: Int, pattern: String) {
    init {
        require(pattern.length == size * size) { "a cell for every place" }
    }

    /** The filled cells, numbered row by row from the top left. */
    val cells: Set<Int> = pattern.indices.filterTo(HashSet()) { pattern[it] == '#' }

    /** The same picture turned left to right. */
    val mirroredCells: Set<Int> = cells.mapTo(HashSet()) { (it / size) * size + (size - 1 - it % size) }
}

object NonogramPictures {
    /** The pictures of the 8 by 8 board: the medium level, and the ones every board was made from before levels existed. */
    val all: List<NonogramPicture> get() = of(NonogramPuzzle.DEFAULT_SIZE)

    /** The pictures drawn for a board of [size] (5 easy, 8 medium, 10 hard), or none if no picture was drawn for that size. */
    fun of(size: Int): List<NonogramPicture> = when (size) {
        5 -> NONOGRAM_PICTURES_5
        8 -> NONOGRAM_PICTURES_8
        10 -> NONOGRAM_PICTURES_10
        else -> emptyList()
    }

    /** The picture whose filled [cells] a board of [size] shows, turned or not, or null. */
    fun find(size: Int, cells: Set<Int>): NonogramPicture? = of(size).firstOrNull { it.cells == cells || it.mirroredCells == cells }
}

/**
 * A mini nonogram: which cells of the square board are filled in the picture. The player only sees the numbers of the rows and
 * columns. The picture can be shown turned left to right ([mirrored]), which changes the numbers.
 *
 * [encode] writes it as a short text: `8:3c7effff7e3c0000:0` is the 64 bit picture in hex and whether it is turned; the board of
 * 10 by 10 needs 100 bits and is written as 25 hex digits (the top 36 bits, then the low 64).
 */
class NonogramPuzzle(val size: Int, val solution: Set<Int>, val mirrored: Boolean = false) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "a small board" }
        require(solution.isNotEmpty() && solution.all { it in 0 until size * size }) { "cells are on the board" }
    }

    val rowClues: List<List<Int>> = List(size) { r -> lineClues(List(size) { c -> r * size + c in solution }) }
    val columnClues: List<List<Int>> = List(size) { c -> lineClues(List(size) { r -> r * size + c in solution }) }

    /** The picture this board shows, if it is one of the drawn ones. */
    val picture: NonogramPicture? get() = NonogramPictures.find(size, solution)

    fun encode(): String {
        val low = bits(0, 64)
        val text = if (size * size <= 64) hex(low, 16) else hex(bits(64, 100), 9) + hex(low, 16)
        return "$size:$text:${if (mirrored) 1 else 0}"
    }

    private fun bits(from: Int, to: Int): Long = solution.filter { it in from until to }.fold(0L) { bits, cell -> bits or (1L shl (cell - from)) }

    companion object {
        const val DEFAULT_SIZE = 8
        const val MIN_SIZE = 3

        /** The picture is kept in 100 bits. */
        const val MAX_SIZE = 10

        private fun hex(value: Long, digits: Int) = value.toULong().toString(16).padStart(digits, '0')

        /** The board of [picture], turned left to right if [mirrored]. */
        fun of(picture: NonogramPicture, mirrored: Boolean): NonogramPuzzle =
            NonogramPuzzle(picture.size, if (mirrored) picture.mirroredCells else picture.cells, mirrored)

        /** Null if [text] is not a board that [encode] wrote. */
        fun decode(text: String): NonogramPuzzle? {
            val parts = text.split(':')
            if (parts.size != 3) return null
            val size = parts[0].toIntOrNull() ?: return null
            if (size !in MIN_SIZE..MAX_SIZE) return null
            val big = size * size > 64
            if (parts[1].length != if (big) 25 else 16) return null
            val low = parts[1].takeLast(16).toULongOrNull(16)?.toLong() ?: return null
            val high = if (big) parts[1].take(9).toULongOrNull(16)?.toLong() ?: return null else 0L
            val mirrored = when (parts[2]) {
                "0" -> false
                "1" -> true
                else -> return null
            }
            val cells = (0 until size * size).filterTo(HashSet()) { cell ->
                if (cell < 64) low and (1L shl cell) != 0L else high and (1L shl (cell - 64)) != 0L
            }
            // Bits beyond the board are not part of a board this game wrote.
            val total = size * size
            if (total < 64 && (low ushr total) != 0L) return null
            if (big && (high ushr (total - 64)) != 0L) return null
            if (cells.isEmpty()) return null
            return NonogramPuzzle(size, cells, mirrored)
        }
    }
}
