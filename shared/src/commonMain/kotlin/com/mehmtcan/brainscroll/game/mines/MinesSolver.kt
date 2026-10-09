package com.mehmtcan.brainscroll.game.mines

/** What pure logic can tell about a board in view: cells that cannot be mines, and cells that must be. */
class Deduction(val safe: Set<Int>, val mines: Set<Int>)

/**
 * Solves minesweeper by reasoning only, the way a careful player does. The board generator uses it to throw away
 * boards that need a guess, and the tests use it to play a board to the end.
 *
 * Two rules, repeated until nothing new turns up:
 * 1. A number whose remaining mines equal zero makes all its other neighbors safe; equal to the cells left, all mines.
 * 2. If the unknown neighbors of one number are among those of another, the difference of the two counts says
 *    something about the cells that only the bigger one touches.
 */
object MinesSolver {

    /**
     * [numbers] holds, for each cell, the count of mines around it if the cell is open, and -1 if it is not.
     * Returns everything that follows; cells that are open are never in the result.
     */
    fun deduce(rows: Int, cols: Int, numbers: IntArray): Deduction {
        val mines = mutableSetOf<Int>()
        val safe = mutableSetOf<Int>()
        val around = Array(numbers.size) { MinesGame.neighbors(it, rows, cols) }

        class Constraint(val cells: Set<Int>, val remaining: Int)

        var progress = true
        while (progress) {
            progress = false
            val constraints = mutableListOf<Constraint>()
            for (cell in numbers.indices) {
                if (numbers[cell] < 0) continue
                val unknown = around[cell].filter { numbers[it] < 0 && it !in mines && it !in safe }
                if (unknown.isEmpty()) continue
                constraints += Constraint(unknown.toSet(), numbers[cell] - around[cell].count { it in mines })
            }
            for (c in constraints) {
                when {
                    c.remaining == 0 -> progress = safe.addAll(c.cells) || progress
                    c.remaining == c.cells.size -> progress = mines.addAll(c.cells) || progress
                }
            }
            if (progress) continue
            for (a in constraints) for (b in constraints) {
                if (a === b || a.cells.size >= b.cells.size || !b.cells.containsAll(a.cells)) continue
                val difference = b.cells - a.cells
                val extra = b.remaining - a.remaining
                when {
                    extra == 0 -> progress = safe.addAll(difference) || progress
                    extra == difference.size -> progress = mines.addAll(difference) || progress
                }
            }
        }
        return Deduction(safe, mines)
    }
}
