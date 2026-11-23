package com.mehmtcan.brainscroll.game.lamps

/**
 * Finds the ways to light a board. The cells of the solver are [UNKNOWN], [LAMP] or [NONE] (a cell that cannot hold a lamp). It follows
 * the rules the way a player does (a cell a lamp lights holds no lamp, a cell that only one place can light gets a lamp there, a number
 * that is full leaves the cells around its wall empty, a number that needs all its free cells gets lamps in them), and when nothing is
 * forced it tries a lamp and then no lamp in the first unknown cell.
 */
object LampSolver {
    const val UNKNOWN = 0
    const val LAMP = 1
    const val NONE = 2

    /** Up to [limit] ways to light the board: for each, a flag for every cell that holds a lamp. */
    fun solutions(puzzle: LampPuzzle, limit: Int): List<BooleanArray> {
        val found = ArrayList<BooleanArray>()
        fun rec(state: IntArray) {
            if (found.size >= limit) return
            if (!propagate(puzzle, state)) return
            val next = puzzle.whiteCells.firstOrNull { state[it] == UNKNOWN }
            if (next == null) {
                val lamps = BooleanArray(state.size) { state[it] == LAMP }
                if (LampRules.isSolved(puzzle, lamps)) found += lamps
                return
            }
            for (choice in intArrayOf(LAMP, NONE)) {
                val copy = state.copyOf()
                copy[next] = choice
                rec(copy)
            }
        }
        rec(IntArray(puzzle.size * puzzle.size))
        return found
    }

    /** The lamps of the board if it has exactly one way to be lit, else null. */
    fun uniqueSolution(puzzle: LampPuzzle): BooleanArray? = solutions(puzzle, 2).singleOrNull()

    /**
     * Lights the board the way a player can: by the rules alone and, if [lookahead] is on, also by trying a lamp in a cell and seeing that the
     * rules break (so there is none). Returns the lamps, or null if the board cannot be finished that way. A board that is finished this
     * way has only one way to be lit, since every step was forced.
     */
    fun solveByLogic(puzzle: LampPuzzle, lookahead: Boolean): BooleanArray? {
        val state = IntArray(puzzle.size * puzzle.size)
        while (true) {
            if (!propagate(puzzle, state)) return null
            val open = puzzle.whiteCells.filter { state[it] == UNKNOWN }
            if (open.isEmpty()) {
                val lamps = BooleanArray(state.size) { state[it] == LAMP }
                return lamps.takeIf { LampRules.isSolved(puzzle, it) }
            }
            if (!lookahead) return null
            var progress = false
            for (cell in open) {
                if (state[cell] != UNKNOWN) continue
                val withLamp = state.copyOf()
                withLamp[cell] = LAMP
                val withNone = state.copyOf()
                withNone[cell] = NONE
                val lampFails = !propagate(puzzle, withLamp)
                val noneFails = !propagate(puzzle, withNone)
                if (lampFails && noneFails) return null
                if (lampFails || noneFails) {
                    state[cell] = if (lampFails) NONE else LAMP
                    progress = true
                    if (!propagate(puzzle, state)) return null
                }
            }
            if (!progress) return null
        }
    }

    /** Fills what the rules force in [state], again and again. Returns false if the state breaks a rule (so it cannot be finished). */
    fun propagate(puzzle: LampPuzzle, state: IntArray): Boolean {
        while (true) {
            if (contradiction(puzzle, state)) return false
            val forced = forcedOnce(puzzle, state)
            if (forced.isEmpty()) return true
            for ((cell, value) in forced) {
                if (state[cell] != UNKNOWN && state[cell] != value) return false
                state[cell] = value
            }
        }
    }

    /** Whether [state] breaks a rule: a lamp that another lamp lights, a cell nothing can light, a number that cannot be right. */
    fun contradiction(puzzle: LampPuzzle, state: IntArray): Boolean {
        for (cell in puzzle.whiteCells) {
            if (state[cell] == LAMP && puzzle.seenFrom(cell).any { state[it] == LAMP }) return true
        }
        for (cell in puzzle.whiteCells) {
            if (state[cell] == LAMP || puzzle.seenFrom(cell).any { state[it] == LAMP }) continue
            // Nothing lights this cell yet: some cell in its row or column (or it itself) must still be able to hold a lamp.
            if (state[cell] == NONE && puzzle.seenFrom(cell).none { state[it] == UNKNOWN }) return true
        }
        for (wall in puzzle.cells.indices) {
            val number = puzzle.numberAt(wall)
            if (number < 0) continue
            val around = puzzle.neighbours(wall).filter { !puzzle.isWall(it) }
            val lamps = around.count { state[it] == LAMP }
            val open = around.count { state[it] == UNKNOWN }
            if (lamps > number || lamps + open < number) return true
        }
        return false
    }

    /** The cells the rules force in [state] as it is now, with what they become: the first thing a player can see. */
    fun forcedOnce(puzzle: LampPuzzle, state: IntArray): List<Pair<Int, Int>> = forcedWhy(puzzle, state).map { it.cell to it.value }

    /** Which rule forced a cell: a lamp lights it, a dark cell that only this cell (or one cell) can light, a number that is full or needs every free cell. */
    const val LIT = 0
    const val ONLY_SELF = 1
    const val ONLY_OTHER = 2
    const val WALL_FULL = 3
    const val WALL_NEEDS = 4

    /**
     * A cell the rules force: what it becomes ([value]), the [rule] and the one cell the rule rests on ([evidence]: the lamp that lights it,
     * the dark cell that nothing else can light, the numbered wall), or -1.
     */
    class Forced(val cell: Int, val value: Int, val rule: Int, val evidence: Int)

    /** The same as [forcedOnce], with the reason of each cell. A cell forced twice keeps the last reason, like its value. */
    fun forcedWhy(puzzle: LampPuzzle, state: IntArray): List<Forced> {
        val out = LinkedHashMap<Int, Forced>()
        // A cell that a lamp lights holds no lamp.
        for (cell in puzzle.whiteCells) if (state[cell] == LAMP) {
            for (seen in puzzle.seenFrom(cell)) if (state[seen] == UNKNOWN) out[seen] = Forced(seen, NONE, LIT, cell)
        }
        // A cell that nothing lights yet and that only one cell can light has a lamp there.
        for (cell in puzzle.whiteCells) {
            if (state[cell] == LAMP || puzzle.seenFrom(cell).any { state[it] == LAMP }) continue
            val candidates = ArrayList<Int>(2)
            if (state[cell] == UNKNOWN) candidates += cell
            for (seen in puzzle.seenFrom(cell)) if (state[seen] == UNKNOWN) candidates += seen
            if (candidates.size == 1) out[candidates[0]] = Forced(candidates[0], LAMP, if (candidates[0] == cell) ONLY_SELF else ONLY_OTHER, cell)
        }
        // A number that has all its lamps leaves the cells around its wall empty; one that needs all the free cells gets lamps in them.
        for (wall in puzzle.cells.indices) {
            val number = puzzle.numberAt(wall)
            if (number < 0) continue
            val around = puzzle.neighbours(wall).filter { !puzzle.isWall(it) }
            val lamps = around.count { state[it] == LAMP }
            val open = around.filter { state[it] == UNKNOWN }
            if (open.isEmpty()) continue
            if (lamps == number) for (cell in open) out[cell] = Forced(cell, NONE, WALL_FULL, wall)
            else if (lamps + open.size == number) for (cell in open) out[cell] = Forced(cell, LAMP, WALL_NEEDS, wall)
        }
        return out.values.toList()
    }
}
