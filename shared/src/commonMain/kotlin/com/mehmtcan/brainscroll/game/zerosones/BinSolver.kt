package com.mehmtcan.brainscroll.game.zerosones

import kotlin.random.Random

/**
 * Finds the ways to fill a board. It fills the cells the rules force (a pair of the same digit has the other digit on both sides, a
 * line that has all its 0s or all its 1s gets the other digit in the rest), and when nothing is forced it tries 0 and then 1 in the
 * first empty cell.
 */
object BinSolver {
    /** Up to [limit] ways to fill the board: the filled grids. */
    fun solutions(puzzle: BinPuzzle, limit: Int): List<IntArray> = solutions(puzzle.size, puzzle.given.toIntArray(), limit)

    /** The same for a board of [n] by [n] cells that starts as [start] (-1 for the cells left to fill). */
    fun solutions(n: Int, start: IntArray, limit: Int): List<IntArray> {
        val found = ArrayList<IntArray>()
        fun rec(grid: IntArray) {
            if (found.size >= limit) return
            if (!propagate(n, grid)) return
            val empty = grid.indexOfFirst { it < 0 }
            if (empty < 0) {
                found += grid
                return
            }
            for (digit in 0..1) {
                val next = grid.copyOf()
                next[empty] = digit
                rec(next)
            }
        }
        rec(start.copyOf())
        return found
    }

    /**
     * Fills the board the way a player can: by the rules alone and, if [lookahead] is on, also by trying a digit in a cell and seeing
     * that the rules break (so the other digit it is). Returns the filled grid, or null if the board cannot be finished that way. A board
     * that is finished this way has only one way to be filled, since every step was forced.
     */
    fun solveByLogic(n: Int, start: IntArray, lookahead: Boolean): IntArray? {
        val grid = start.copyOf()
        while (true) {
            if (!propagate(n, grid)) return null
            if (grid.all { it >= 0 }) return grid
            if (!lookahead) return null
            var progress = false
            for (cell in grid.indices) {
                if (grid[cell] >= 0) continue
                for (digit in 0..1) {
                    val trial = grid.copyOf()
                    trial[cell] = digit
                    if (!propagate(n, trial)) {
                        grid[cell] = 1 - digit
                        progress = true
                        break
                    }
                }
                if (progress) break
            }
            if (!progress) return null
        }
    }

    /** The filled board if it has exactly one way to be filled, else null. */
    fun uniqueSolution(puzzle: BinPuzzle): IntArray? = solutions(puzzle, 2).singleOrNull()

    /** A random filled board of [n] by [n] that follows the rules. */
    fun randomSolution(random: Random, n: Int): IntArray {
        fun rec(grid: IntArray): IntArray? {
            if (!propagate(n, grid)) return null
            val empty = grid.indexOfFirst { it < 0 }
            if (empty < 0) return grid
            for (digit in listOf(0, 1).shuffled(random)) {
                val next = grid.copyOf()
                next[empty] = digit
                rec(next)?.let { return it }
            }
            return null
        }
        return rec(IntArray(n * n) { -1 }) ?: error("no board")
    }

    /**
     * Fills what the rules force, again and again, in [grid]. Returns false if the grid breaks a rule (so it cannot be finished).
     * [forcedOnce] is what one round of the rules forces on a grid, without chaining them.
     */
    fun propagate(n: Int, grid: IntArray): Boolean {
        while (true) {
            if (BinRules.conflicts(n, grid).isNotEmpty()) return false
            val forced = forcedOnce(n, grid)
            if (forced.isEmpty()) return true
            for ((cell, digit) in forced) {
                if (grid[cell] >= 0 && grid[cell] != digit) return false
                grid[cell] = digit
            }
        }
    }

    /** The cells the rules force in [grid] as it is now, with their digit: the first thing a player can see. */
    fun forcedOnce(n: Int, grid: IntArray): List<Pair<Int, Int>> = forcedWhy(n, grid).map { it.cell to it.digit }

    /** Which rule forced a cell: two of a digit side by side, a digit on both sides of the cell, a line that has half of a digit. */
    const val PAIR = 1
    const val SPLIT = 2
    const val HALF = 3

    /** A cell the rules force, with its [digit], the [rule] that did it and the cells the rule rests on ([evidence]). */
    class Forced(val cell: Int, val digit: Int, val rule: Int, val evidence: List<Int>)

    /** The same as [forcedOnce], with the reason of each cell. A cell forced twice keeps the last reason, like its digit. */
    fun forcedWhy(n: Int, grid: IntArray): List<Forced> {
        val out = LinkedHashMap<Int, Forced>()
        for (line in BinRules.lines(n)) {
            // Two of a digit next to each other (or with one cell between) leave the other digit for the cell that would make three.
            for (i in 0..n - 3) {
                val a = grid[line[i]]
                val b = grid[line[i + 1]]
                val c = grid[line[i + 2]]
                if (a >= 0 && a == b && c < 0) out[line[i + 2]] = Forced(line[i + 2], 1 - a, PAIR, listOf(line[i], line[i + 1]))
                if (b >= 0 && b == c && a < 0) out[line[i]] = Forced(line[i], 1 - b, PAIR, listOf(line[i + 1], line[i + 2]))
                if (a >= 0 && a == c && b < 0) out[line[i + 1]] = Forced(line[i + 1], 1 - a, SPLIT, listOf(line[i], line[i + 2]))
            }
            // A line that has half of a digit already gets the other digit in the rest.
            for (digit in 0..1) {
                if (line.count { grid[it] == digit } == n / 2) {
                    val holders = line.filter { grid[it] == digit }
                    for (cell in line) if (grid[cell] < 0) out[cell] = Forced(cell, 1 - digit, HALF, holders)
                }
            }
        }
        return out.values.toList()
    }
}
