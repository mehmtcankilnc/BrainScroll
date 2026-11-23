package com.mehmtcan.brainscroll.game.crosssum

enum class CrossStatus { Playing, Won }

/** What putting a digit did. */
enum class CrossResult {
    /** Nothing changed (a board that is won, a cell that is given or not on the board, a digit that is not 0 to 9). */
    Ignored,

    /** A digit was put in a cell. */
    Placed,

    /** The digit of a cell was taken out. */
    Cleared,

    /** The digit left the board filled in and every block right: it is won. */
    Solved,
}

/** The picture of the board for the UI. */
data class CrossState(
    val puzzle: CrossPuzzle,
    /** The digit of every cell of the white board, row after row (0 for an empty cell and for a black one; given cells hold their digit). */
    val values: List<Int>,
    /** Digits put or taken out so far. */
    val moves: Int,
    val status: CrossStatus,
) {
    /** How many white cells hold a digit (the given ones too). */
    val filled: Int get() = puzzle.whiteCells.count { values[it] > 0 }

    /**
     * The cells that break a rule now: the cells of a block that holds a digit twice, that has already more than its sum, or that is
     * full and does not add up to its sum.
     */
    val conflicts: Set<Int>
        get() {
            val out = HashSet<Int>()
            for (run in puzzle.runs) {
                val digits = run.cells.map { values[it] }.filter { it > 0 }
                val sum = digits.sum()
                val twice = digits.toSet().size != digits.size
                val full = digits.size == run.cells.size
                if (twice || sum > run.sum || (full && sum != run.sum)) out += run.cells
            }
            return out
        }
}

/**
 * The rules of the cross sums, free of UI and time. A digit goes in any white cell that is not given (the same key again, or 0, takes it
 * out). The board is won when every white cell holds a digit and every block has different digits that add up to its sum. Since the
 * board has only one such filling, that is the answer. There is no losing.
 */
class CrossGame(private val puzzle: CrossPuzzle) {
    private val values = IntArray(puzzle.white.size) { if (puzzle.given[it]) puzzle.solution[it] else 0 }
    private var moves = 0
    private var won = false

    val isFinished: Boolean get() = won

    val state: CrossState get() = CrossState(puzzle, values.toList(), moves, if (won) CrossStatus.Won else CrossStatus.Playing)

    /** Puts [digit] (1 to 9, or 0 to empty the cell) in the cell number [cell] (row * columns + column of the white board). */
    fun put(cell: Int, digit: Int): CrossResult {
        if (won || cell !in values.indices || !puzzle.white[cell] || puzzle.given[cell] || digit !in 0..9 || values[cell] == digit) return CrossResult.Ignored
        values[cell] = digit
        moves++
        if (isSolved()) {
            won = true
            return CrossResult.Solved
        }
        return if (digit == 0) CrossResult.Cleared else CrossResult.Placed
    }

    /** Takes every digit the player put out. Returns false when there were none (or the board is won). */
    fun restart(): Boolean {
        if (won || puzzle.openCells.none { values[it] > 0 }) return false
        for (cell in puzzle.openCells) values[cell] = 0
        return true
    }

    /**
     * Puts back what a saved game holds: the digits of the cells the player fills ([saved], in order) and the [savedMoves] made. Returns
     * false if that cannot be a game of this board (the wrong number of cells, a digit above 9, fewer moves than digits, a board that is
         * solved): nothing is changed then.
     */
    fun restore(saved: List<Int>, savedMoves: Int): Boolean {
        val cells = puzzle.openCells
        if (saved.size != cells.size || saved.any { it !in 0..9 } || savedMoves < saved.count { it > 0 }) return false
        val before = values.copyOf()
        cells.forEachIndexed { i, cell -> values[cell] = saved[i] }
        if (isSolved()) {
            before.copyInto(values)
            return false
        }
        moves = savedMoves
        return true
    }

    private fun isSolved(): Boolean = puzzle.runs.all { run ->
        val digits = run.cells.map { values[it] }
        digits.all { it > 0 } && digits.toSet().size == digits.size && digits.sum() == run.sum
    }
}
