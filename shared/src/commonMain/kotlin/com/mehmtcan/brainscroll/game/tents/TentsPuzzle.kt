package com.mehmtcan.brainscroll.game.tents

/**
 * A finished tents board: where the trees are and where the tents go. The row and column numbers follow from the tents.
 * It is the whole puzzle: the player sees the trees and the numbers, and has to find the tents.
 *
 * [encode] writes it as a short text (e.g. `6:2a40c1:90408`) that keeps the saved result of a game within the 32 characters the
 * cloud table allows, and [decode] reads it back.
 */
class TentsPuzzle(val size: Int, val trees: Set<Int>, val tents: Set<Int>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "a small board" }
        require(trees.all { it in 0 until size * size } && tents.all { it in 0 until size * size }) { "cells are on the board" }
        require(trees.none { it in tents }) { "a tree and a tent do not share a cell" }
        require(trees.isNotEmpty() && trees.size == tents.size) { "as many tents as trees" }
        require(tents.none { a -> tents.any { b -> TentsRules.touch(size, a, b) } }) { "no two tents touch" }
        require(TentsRules.hasMatching(size, trees, tents)) { "every tree has its tent" }
    }

    val rowTargets: List<Int> = TentsRules.rowCounts(size, tents).toList()
    val columnTargets: List<Int> = TentsRules.columnCounts(size, tents).toList()

    fun encode(): String = "$size:${mask(trees).toString(16)}:${mask(tents).toString(16)}"

    companion object {
        const val DEFAULT_SIZE = 6
        const val MIN_SIZE = 4

        /** The board is kept in one number of 49 bits. */
        const val MAX_SIZE = 7

        private fun mask(cells: Set<Int>): Long = cells.fold(0L) { bits, cell -> bits or (1L shl cell) }

        private fun cellsOf(mask: Long, size: Int): Set<Int> = (0 until size * size).filterTo(HashSet()) { mask and (1L shl it) != 0L }

        /** Null if [text] is not a board that [encode] wrote (or not a board that follows the rules). */
        fun decode(text: String): TentsPuzzle? {
            val parts = text.split(':')
            if (parts.size != 3) return null
            val size = parts[0].toIntOrNull() ?: return null
            if (size !in MIN_SIZE..MAX_SIZE) return null
            val trees = parts[1].toLongOrNull(16) ?: return null
            val tents = parts[2].toLongOrNull(16) ?: return null
            if (trees < 0 || tents < 0) return null
            return try {
                TentsPuzzle(size, cellsOf(trees, size), cellsOf(tents, size))
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
