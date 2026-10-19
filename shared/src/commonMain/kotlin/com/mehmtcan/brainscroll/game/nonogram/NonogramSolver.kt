package com.mehmtcan.brainscroll.game.nonogram

/** The numbers of a line: the lengths of its runs of filled cells, in order. A line with nothing filled has no numbers. */
fun lineClues(cells: List<Boolean>): List<Int> {
    val clues = ArrayList<Int>()
    var run = 0
    for (filled in cells) {
        if (filled) {
            run++
        } else if (run > 0) {
            clues += run
            run = 0
        }
    }
    if (run > 0) clues += run
    return clues
}

/** What the logic of the rows and columns can tell, with no guessing. */
object NonogramSolver {
    const val UNKNOWN = -1
    const val EMPTY = 0
    const val FILLED = 1

    /**
     * What is certain about a line: [known] holds [UNKNOWN], [EMPTY] or [FILLED] per cell. Every way to place the runs of [clues]
     * that agrees with it is tried; a cell that is the same in all of them is certain. Returns null if no placement fits (the
     * known cells contradict the numbers).
     */
    fun solveLine(known: IntArray, clues: List<Int>): IntArray? {
        val n = known.size
        var common: IntArray? = null
        val line = IntArray(n)

        fun consistent(from: Int, to: Int, value: Int): Boolean {
            for (i in from until to) if (known[i] != UNKNOWN && known[i] != value) return false
            return true
        }

        fun place(run: Int, pos: Int) {
            if (run == clues.size) {
                if (!consistent(pos, n, EMPTY)) return
                for (i in pos until n) line[i] = EMPTY
                val current = common
                if (current == null) {
                    common = line.copyOf()
                } else {
                    for (i in 0 until n) if (current[i] != line[i]) current[i] = UNKNOWN
                }
                return
            }
            val length = clues[run]
            val rest = clues.drop(run + 1).sum() + (clues.size - run - 1)
            var start = pos
            while (start + length + rest <= n) {
                if (consistent(pos, start, EMPTY) && consistent(start, start + length, FILLED)) {
                    for (i in pos until start) line[i] = EMPTY
                    for (i in start until start + length) line[i] = FILLED
                    if (run + 1 < clues.size) {
                        if (start + length < n && consistent(start + length, start + length + 1, EMPTY)) {
                            line[start + length] = EMPTY
                            place(run + 1, start + length + 1)
                        }
                    } else {
                        place(run + 1, start + length)
                    }
                }
                start++
            }
        }
        place(0, 0)
        return common
    }

    /**
     * Works the whole board out from the numbers alone, rows and columns in turn until nothing changes. Returns the cells in the
     * order they became certain (each with its value, [EMPTY] or [FILLED]), and whether every cell was reached.
     */
    fun deduce(size: Int, rowClues: List<List<Int>>, columnClues: List<List<Int>>): Pair<List<Pair<Int, Int>>, Boolean> {
        val board = IntArray(size * size) { UNKNOWN }
        val order = ArrayList<Pair<Int, Int>>()
        var changed = true
        while (changed) {
            changed = false
            for (line in 0 until 2 * size) {
                val isRow = line < size
                val index = if (isRow) line else line - size
                val cellOf = { i: Int -> if (isRow) index * size + i else i * size + index }
                val known = IntArray(size) { board[cellOf(it)] }
                val next = solveLine(known, if (isRow) rowClues[index] else columnClues[index]) ?: return order to false
                for (i in 0 until size) {
                    if (known[i] == UNKNOWN && next[i] != UNKNOWN) {
                        board[cellOf(i)] = next[i]
                        order += cellOf(i) to next[i]
                        changed = true
                    }
                }
            }
        }
        return order to (order.size == size * size)
    }

    /** True if the picture ([rows]: filled or not) can be worked out from its numbers alone. */
    fun isSolvable(rows: List<List<Boolean>>): Boolean {
        val size = rows.size
        val columns = List(size) { c -> List(size) { r -> rows[r][c] } }
        return deduce(size, rows.map(::lineClues), columns.map(::lineClues)).second
    }
}
