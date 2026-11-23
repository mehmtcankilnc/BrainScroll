package com.mehmtcan.brainscroll.game.lamps

/** The rules of the board, for a set of lamps (a flag for every cell, row after row). */
object LampRules {
    /** The cells that are lit: the cells of the lamps and every white cell a lamp sees. */
    fun lit(puzzle: LampPuzzle, lamps: BooleanArray): BooleanArray {
        val lit = BooleanArray(lamps.size)
        for (cell in lamps.indices) if (lamps[cell]) {
            lit[cell] = true
            for (seen in puzzle.seenFrom(cell)) lit[seen] = true
        }
        return lit
    }

    /** The lamps that light another lamp (so they are lit by it as well). */
    fun clashing(puzzle: LampPuzzle, lamps: BooleanArray): Set<Int> {
        val out = HashSet<Int>()
        for (cell in lamps.indices) if (lamps[cell]) {
            for (seen in puzzle.seenFrom(cell)) if (lamps[seen]) {
                out += cell
                out += seen
            }
        }
        return out
    }

    /** How many lamps are on the cells next to the wall [wall]. */
    fun lampsAround(puzzle: LampPuzzle, lamps: BooleanArray, wall: Int): Int = puzzle.neighbours(wall).count { lamps[it] }

    /** The numbered walls that have more lamps next to them than their number. */
    fun overfull(puzzle: LampPuzzle, lamps: BooleanArray): Set<Int> =
        puzzle.cells.indices.filterTo(HashSet()) { puzzle.numberAt(it) >= 0 && lampsAround(puzzle, lamps, it) > puzzle.numberAt(it) }

    /** The numbered walls that have exactly their number of lamps next to them. */
    fun satisfied(puzzle: LampPuzzle, lamps: BooleanArray): Set<Int> =
        puzzle.cells.indices.filterTo(HashSet()) { puzzle.numberAt(it) >= 0 && lampsAround(puzzle, lamps, it) == puzzle.numberAt(it) }

    /** Whether every white cell is lit, no two lamps light each other and every number is right: the board is solved. */
    fun isSolved(puzzle: LampPuzzle, lamps: BooleanArray): Boolean {
        if (lamps.indices.any { lamps[it] && puzzle.isWall(it) }) return false
        val lit = lit(puzzle, lamps)
        if (puzzle.whiteCells.any { !lit[it] }) return false
        if (clashing(puzzle, lamps).isNotEmpty()) return false
        return puzzle.cells.indices.all { puzzle.numberAt(it) < 0 || lampsAround(puzzle, lamps, it) == puzzle.numberAt(it) }
    }
}
