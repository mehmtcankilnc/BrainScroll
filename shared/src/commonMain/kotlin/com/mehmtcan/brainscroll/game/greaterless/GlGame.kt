package com.mehmtcan.brainscroll.game.greaterless

enum class GlStatus { Playing, Won }

/** What a tap did. */
enum class GlResult {
    /** Nothing changed (a board that is won, a given cell, a cell that is not on the board). */
    Ignored,

    /** The cell now holds the number. */
    Placed,

    /** The cell was emptied. */
    Cleared,

    /** The tap left every cell filled right: the board is won. */
    Solved,
}

/** The picture of the board for the UI. */
data class GlState(
    val puzzle: GlPuzzle,
    /** What every cell holds now, given cells included: 0 empty, else its number. */
    val cells: List<Int>,
    /** Taps that changed a cell so far (taking a number off counts). */
    val moves: Int,
    val status: GlStatus,
) {
    /** The cells that break a rule right now. */
    val conflicts: Set<Int> get() = GlRules.conflicts(puzzle, cells.toIntArray())

    /** How many cells are filled. */
    val filled: Int get() = cells.count { it > 0 }
}

/**
 * The rules of the greater and less, free of UI and time. The player puts a number in a cell that is not given, or empties it (0). The board is won when every cell is filled and no rule is broken; since the board has only one solution, that is it. There is
 * no losing and no tap is refused: a rule that is broken shows on the board.
 */
class GlGame(private val puzzle: GlPuzzle) {
    private val cells = puzzle.given.toIntArray()
    private var moves = 0
    private var won = false

    val isFinished: Boolean get() = won

    val state: GlState get() = GlState(puzzle, cells.toList(), moves, if (won) GlStatus.Won else GlStatus.Playing)

    /** Puts [value] (1 to the size, or 0 to empty the cell) in the cell number [cell] (row * size + column). */
    fun set(cell: Int, value: Int): GlResult {
        if (won || cell !in cells.indices || puzzle.isGiven(cell) || value !in 0..puzzle.size || cells[cell] == value) return GlResult.Ignored
        cells[cell] = value
        moves++
        // Any move can finish the board: a number that was wrong may be the one taken off or changed last.
        if (GlRules.isSolved(puzzle, cells)) {
            won = true
            return GlResult.Solved
        }
        return if (cells[cell] == 0) GlResult.Cleared else GlResult.Placed
    }

    /** Empties every cell the player filled. Returns false when there were none (or the board is won). */
    fun restart(): Boolean {
        if (won) return false
        var any = false
        for (i in cells.indices) if (!puzzle.isGiven(i) && cells[i] > 0) {
            cells[i] = 0
            any = true
        }
        return any
    }

    /**
     * Puts back what a saved game holds: the number of every cell (the given ones are kept as they are and must come back as they were)
     * and the [savedMoves] made. Returns false if that cannot be a game of this board (the wrong number of cells, a given cell changed,
     * fewer moves than filled cells, a board that is solved): nothing is changed then.
     */
    fun restore(saved: List<Int>, savedMoves: Int): Boolean {
        if (saved.size != cells.size || saved.any { it !in 0..puzzle.size }) return false
        if (saved.indices.any { puzzle.isGiven(it) && saved[it] != puzzle.given[it] }) return false
        if (savedMoves < saved.indices.count { !puzzle.isGiven(it) && saved[it] > 0 }) return false
        if (GlRules.isSolved(puzzle, saved.toIntArray())) return false
        saved.forEachIndexed { i, v -> cells[i] = v }
        moves = savedMoves
        return true
    }
}
