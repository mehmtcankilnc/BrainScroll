package com.mehmtcan.brainscroll.game.greaterless

import kotlin.random.Random

/**
 * Finds the ways to fill a board. Every cell keeps the set of numbers that can still stand in it (bit v of a number is the number v).
 * The rules that prune the sets are the ones a player uses: a number that stands in a cell is out of the rest of its row and column, a
 * number that fits one cell only of a row or column stands there, and a sign puts a floor under the bigger cell and a ceiling over the
 * smaller one. On the harder levels a "try and see" step is allowed too: a number that makes the rules break in a cell is out of it.
 */
object GlSolver {
    private val lineSets = Array(GlPuzzle.MAX_SIZE + 1) { if (it >= GlPuzzle.MIN_SIZE) GlRules.lines(it) else emptyList() }

    /** The bit set of every number of a board of [n] by [n]. */
    private fun all(n: Int) = (1 shl (n + 1)) - 2

    private fun single(set: Int) = set != 0 && set and (set - 1) == 0

    /** The sets a board starts with: the whole range for an empty cell, the number alone for a cell that holds one. */
    fun sets(puzzle: GlPuzzle, grid: IntArray = puzzle.given.toIntArray()): IntArray =
        IntArray(puzzle.size * puzzle.size) { if (grid[it] > 0) 1 shl grid[it] else all(puzzle.size) }

    /** The number a set holds when it is one number, else 0. */
    fun valueOf(set: Int): Int = if (single(set)) set.countTrailingZeroBits() else 0

    /**
     * Prunes [sets] by the rules, again and again, until nothing changes. Returns false if a cell is left with no number (so the board
     * cannot be finished).
     */
    fun propagate(puzzle: GlPuzzle, sets: IntArray): Boolean = propagate(puzzle.size, GlRules.links(puzzle), sets)

    private fun propagate(n: Int, links: List<Triple<Int, Int, Int>>, sets: IntArray): Boolean {
        val lines = lineSets[n]
        while (true) {
            var changed = false
            for (cell in sets.indices) {
                val set = sets[cell]
                if (set == 0) return false
                if (!single(set)) continue
                val row = cell / n
                val column = cell % n
                for (k in 0 until n) {
                    val inRow = row * n + k
                    if (inRow != cell && sets[inRow] and set != 0) {
                        sets[inRow] = sets[inRow] and set.inv()
                        if (sets[inRow] == 0) return false
                        changed = true
                    }
                    val inColumn = k * n + column
                    if (inColumn != cell && sets[inColumn] and set != 0) {
                        sets[inColumn] = sets[inColumn] and set.inv()
                        if (sets[inColumn] == 0) return false
                        changed = true
                    }
                }
            }
            for (line in lines) {
                for (v in 1..n) {
                    var count = 0
                    var at = -1
                    for (cell in line) if (sets[cell] and (1 shl v) != 0) {
                        count++
                        at = cell
                    }
                    if (count == 0) return false
                    if (count == 1 && sets[at] != 1 shl v) {
                        sets[at] = sets[at] and (1 shl v)
                        changed = true
                    }
                }
            }
            for ((a, b, sign) in links) {
                val small = if (sign == GlPuzzle.LESS) a else b
                val big = if (sign == GlPuzzle.LESS) b else a
                if (sets[small] == 0 || sets[big] == 0) return false
                val top = 31 - sets[big].countLeadingZeroBits()
                val trimmedSmall = sets[small] and ((1 shl top) - 1)
                if (trimmedSmall != sets[small]) {
                    sets[small] = trimmedSmall
                    changed = true
                }
                if (trimmedSmall == 0) return false
                val bottom = sets[small].countTrailingZeroBits()
                val trimmedBig = sets[big] and ((1 shl (bottom + 1)) - 1).inv()
                if (trimmedBig != sets[big]) {
                    sets[big] = trimmedBig
                    changed = true
                }
                if (trimmedBig == 0) return false
            }
            if (!changed) return true
        }
    }

    /** Up to [limit] ways to fill the board: the filled grids. */
    fun solutions(puzzle: GlPuzzle, limit: Int): List<IntArray> {
        val n = puzzle.size
        val links = GlRules.links(puzzle)
        val found = ArrayList<IntArray>()
        fun rec(sets: IntArray) {
            if (found.size >= limit) return
            if (!propagate(n, links, sets)) return
            var pick = -1
            for (cell in sets.indices) {
                if (single(sets[cell])) continue
                if (pick < 0 || sets[cell].countOneBits() < sets[pick].countOneBits()) pick = cell
            }
            if (pick < 0) {
                found += IntArray(sets.size) { valueOf(sets[it]) }
                return
            }
            for (v in 1..n) if (sets[pick] and (1 shl v) != 0) {
                val next = sets.copyOf()
                next[pick] = 1 shl v
                rec(next)
            }
        }
        rec(sets(puzzle))
        return found
    }

    /** The filled board if it has exactly one way to be filled, else null. */
    fun uniqueSolution(puzzle: GlPuzzle): IntArray? = solutions(puzzle, 2).singleOrNull()

    /**
     * Fills the board the way a player can: by the rules alone and, if [lookahead] is on, also by trying a number in a cell and seeing
     * that the rules break (so it is not that number). Returns the filled grid, or null if the board cannot be finished that way. A board
     * that is finished this way has only one way to be filled, since every step was forced.
     */
    fun solveByLogic(puzzle: GlPuzzle, lookahead: Boolean): IntArray? {
        val sets = sets(puzzle)
        if (!refine(puzzle, sets, lookahead)) return null
        return IntArray(sets.size) { valueOf(sets[it]) }.takeIf { grid -> grid.all { it > 0 } }
    }

    /**
     * Prunes [sets] as far as the rules (and, if [lookahead] is on, trying a number) can. Returns false if a cell is left with no number.
     * It does not say whether the board is finished: look at the sets.
     */
    fun refine(puzzle: GlPuzzle, sets: IntArray, lookahead: Boolean): Boolean {
        val n = puzzle.size
        val links = GlRules.links(puzzle)
        if (!propagate(n, links, sets)) return false
        if (!lookahead) return true
        var progress = true
        while (progress) {
            progress = false
            for (cell in sets.indices) {
                if (single(sets[cell])) continue
                for (v in 1..n) {
                    if (sets[cell] and (1 shl v) == 0) continue
                    val trial = sets.copyOf()
                    trial[cell] = 1 shl v
                    if (!propagate(n, links, trial)) {
                        sets[cell] = sets[cell] and (1 shl v).inv()
                        if (sets[cell] == 0 || !propagate(n, links, sets)) return false
                        progress = true
                    }
                }
            }
        }
        return true
    }

    /** A random filled board of [n] by [n]: every number once in every row and column. */
    fun randomSquare(random: Random, n: Int): IntArray {
        val grid = IntArray(n * n)
        fun rec(cell: Int): Boolean {
            if (cell == n * n) return true
            val row = cell / n
            val column = cell % n
            for (v in (1..n).shuffled(random)) {
                if ((0 until column).any { grid[row * n + it] == v } || (0 until row).any { grid[it * n + column] == v }) continue
                grid[cell] = v
                if (rec(cell + 1)) return true
            }
            grid[cell] = 0
            return false
        }
        check(rec(0))
        return grid
    }
}
