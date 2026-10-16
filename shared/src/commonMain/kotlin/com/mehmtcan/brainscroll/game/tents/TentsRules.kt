package com.mehmtcan.brainscroll.game.tents

import kotlin.math.abs

/**
 * The rules of the tents puzzle that do not depend on a game in progress: who is next to whom, which tents break a rule,
 * and whether a set of tents is a finished board. Cells are numbered row by row from the top left.
 */
object TentsRules {

    /** The cells next to [cell] along a row or a column. */
    fun straightNeighbors(size: Int, cell: Int): List<Int> {
        val row = cell / size
        val column = cell % size
        val result = ArrayList<Int>(4)
        if (row > 0) result += cell - size
        if (row < size - 1) result += cell + size
        if (column > 0) result += cell - 1
        if (column < size - 1) result += cell + 1
        return result
    }

    /** True if two different cells touch, diagonals included. */
    fun touch(size: Int, a: Int, b: Int): Boolean =
        a != b && abs(a / size - b / size) <= 1 && abs(a % size - b % size) <= 1

    /** How many tents are in each row. */
    fun rowCounts(size: Int, tents: Collection<Int>): IntArray = IntArray(size).also { counts -> for (cell in tents) counts[cell / size]++ }

    /** How many tents are in each column. */
    fun columnCounts(size: Int, tents: Collection<Int>): IntArray = IntArray(size).also { counts -> for (cell in tents) counts[cell % size]++ }

    /** True if every tent can be given its own tree next to it (and so every tree its own tent, as there are as many of each). */
    fun hasMatching(size: Int, trees: Set<Int>, tents: Collection<Int>): Boolean {
        if (trees.size != tents.size) return false
        val tentList = tents.toList()
        val owner = HashMap<Int, Int>() // tree -> index of the tent that has it
        fun assign(tent: Int, seen: MutableSet<Int>): Boolean {
            for (tree in straightNeighbors(size, tentList[tent])) {
                if (tree !in trees || !seen.add(tree)) continue
                val other = owner[tree]
                if (other == null || assign(other, seen)) {
                    owner[tree] = tent
                    return true
                }
            }
            return false
        }
        return tentList.indices.all { assign(it, HashSet()) }
    }

    /**
     * The tents that break a rule: two that touch, one in a row or a column that already holds more tents than its number
     * says, and one with no tree next to it. [tents] are the cells that hold a tent.
     */
    fun conflicts(size: Int, trees: Set<Int>, tents: Set<Int>, rowTargets: List<Int>, columnTargets: List<Int>): Set<Int> {
        val rows = rowCounts(size, tents)
        val columns = columnCounts(size, tents)
        return tents.filterTo(HashSet()) { tent ->
            tents.any { touch(size, tent, it) } ||
                rows[tent / size] > rowTargets[tent / size] ||
                columns[tent % size] > columnTargets[tent % size] ||
                straightNeighbors(size, tent).none { it in trees }
        }
    }

    /** True if [tents] is a finished board: the numbers are met exactly, no two tents touch and every tree has its tent. */
    fun isSolved(size: Int, trees: Set<Int>, tents: Set<Int>, rowTargets: List<Int>, columnTargets: List<Int>): Boolean =
        tents.size == trees.size &&
            rowCounts(size, tents).toList() == rowTargets &&
            columnCounts(size, tents).toList() == columnTargets &&
            tents.none { tent -> tents.any { touch(size, tent, it) } } &&
            hasMatching(size, trees, tents)
}
