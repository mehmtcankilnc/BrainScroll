package com.mehmtcan.brainscroll.game.tents

/** Finds the ways to finish a tents board, and what can be worked out from a board without guessing. */
object TentsSolver {

    /**
     * Every different set of tents that finishes the board (the trees and the numbers), but it stops looking once it has found
     * [limit] of them: for the puzzle to be good there must be exactly one.
     */
    fun solutions(size: Int, trees: Set<Int>, rowTargets: List<Int>, columnTargets: List<Int>, limit: Int = 2): List<Set<Int>> {
        val treeList = trees.sorted()
        val found = LinkedHashSet<Set<Int>>()
        val tents = HashSet<Int>()
        val rows = IntArray(size)
        val columns = IntArray(size)

        fun place(tree: Int) {
            if (found.size >= limit) return
            if (tree == treeList.size) {
                if (rows.toList() == rowTargets && columns.toList() == columnTargets) found += tents.toSet()
                return
            }
            for (cell in TentsRules.straightNeighbors(size, treeList[tree])) {
                if (cell in trees || cell in tents) continue
                if (rows[cell / size] >= rowTargets[cell / size] || columns[cell % size] >= columnTargets[cell % size]) continue
                if (tents.any { TentsRules.touch(size, it, cell) }) continue
                tents += cell
                rows[cell / size]++
                columns[cell % size]++
                place(tree + 1)
                tents -= cell
                rows[cell / size]--
                columns[cell % size]--
            }
        }
        place(0)
        return found.toList()
    }

    /**
     * The tents that follow from the board by plain reasoning, in reading order, not counting those in [known]: no tent where no
     * tree is next to the cell, none next to a tent, a row or column that has all its tents (the rest of it is free of them) or
     * only just enough room left (all of it is tents), and a tree that has no tent yet and only one place left for it. [known]
     * are tents that are certain.
     */
    fun deduce(size: Int, trees: Set<Int>, rowTargets: List<Int>, columnTargets: List<Int>, known: Set<Int>): List<Int> {
        // 1 tent, 0 no tent, -1 not known yet
        val state = IntArray(size * size) { -1 }
        for (cell in state.indices) {
            if (cell in trees || TentsRules.straightNeighbors(size, cell).none { it in trees }) state[cell] = 0
        }
        for (cell in known) state[cell] = 1
        var changed = true
        fun set(cell: Int, value: Int) {
            if (state[cell] == -1) {
                state[cell] = value
                changed = true
            }
        }
        while (changed) {
            changed = false
            for (cell in state.indices) {
                if (state[cell] != 1) continue
                for (other in state.indices) if (TentsRules.touch(size, cell, other)) set(other, 0)
            }
            for (line in 0 until size) {
                for (byRow in listOf(true, false)) {
                    val cells = (0 until size).map { if (byRow) line * size + it else it * size + line }
                    val target = if (byRow) rowTargets[line] else columnTargets[line]
                    val tents = cells.count { state[it] == 1 }
                    val unknown = cells.filter { state[it] == -1 }
                    if (tents == target) unknown.forEach { set(it, 0) } else if (tents + unknown.size == target) unknown.forEach { set(it, 1) }
                }
            }
            for (tree in trees) {
                val around = TentsRules.straightNeighbors(size, tree)
                if (around.any { state[it] == 1 }) continue
                val open = around.filter { state[it] == -1 }
                if (open.size == 1) set(open.single(), 1)
            }
        }
        return state.indices.filter { state[it] == 1 && it !in known }
    }
}
