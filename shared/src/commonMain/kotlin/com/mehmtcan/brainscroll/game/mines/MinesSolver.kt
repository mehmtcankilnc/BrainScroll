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

    /** The reason a cell is safe or a mine, in the numbers of its neighbours: [rule] 0 (more steps than one number), see [why]. */
    class Why(val rule: Int, val number: Int, val evidence: List<Int>)

    const val SAFE_COMPLETE = 1
    const val MINE_FILLS = 2

    /**
     * What one number around [cell] says about it, given everything that was [found]: a number that already has all its mines among the
     * cells known to be mines makes the others around it safe ([SAFE_COMPLETE], evidence: the number and those mines); a number with
     * exactly as many hidden cells as mines makes all of them mines ([MINE_FILLS], evidence: the number). Rule 0 when no single number
     * says it (a pair of numbers, or a longer chain).
     */
    fun why(rows: Int, cols: Int, numbers: IntArray, found: Deduction, cell: Int): Why {
        val isMine = cell in found.mines
        for (c in MinesGame.neighbors(cell, rows, cols)) {
            if (numbers[c] < 0) continue
            val hidden = MinesGame.neighbors(c, rows, cols).filter { numbers[it] < 0 }
            val knownMines = hidden.filter { it in found.mines }
            if (!isMine && knownMines.size == numbers[c]) return Why(SAFE_COMPLETE, numbers[c], listOf(c) + knownMines)
            val open = hidden.filter { it !in found.safe }
            if (isMine && open.size == numbers[c]) return Why(MINE_FILLS, numbers[c], listOf(c))
        }
        return Why(0, 0, emptyList())
    }

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
