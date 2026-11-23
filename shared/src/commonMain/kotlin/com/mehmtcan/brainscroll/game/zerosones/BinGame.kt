package com.mehmtcan.brainscroll.game.zerosones

enum class BinStatus { Playing, Won }

/** What a tap did. */
enum class BinResult {
    /** Nothing changed (a board that is won, a given cell, a cell that is not on the board). */
    Ignored,

    /** The cell now holds 0. */
    Zero,

    /** The cell now holds 1. */
    One,

    /** The cell was emptied. */
    Cleared,

    /** The cell got its digit and every cell is filled right: the board is won. */
    Solved,
}

/** The picture of the board for the UI. */
data class BinState(
    val puzzle: BinPuzzle,
    /** What every cell holds now, given cells included: -1 empty, 0 or 1. */
    val cells: List<Int>,
    /** Taps that changed a cell so far (taking a digit off counts). */
    val moves: Int,
    val status: BinStatus,
) {
    /** The cells that break a rule right now. */
    val conflicts: Set<Int> get() = BinRules.conflicts(puzzle.size, cells.toIntArray())

    /** How many cells are filled. */
    val filled: Int get() = cells.count { it >= 0 }
}

/**
 * The rules of the zeros and ones, free of UI and time. A tap on a cell that is not given goes empty, 0, 1, empty... The board is won when
 * every cell is filled and no rule is broken; since the board has only one solution, that is it. There is no losing and no tap is
 * refused: a rule that is broken shows on the board.
 */
class BinGame(private val puzzle: BinPuzzle) {
    private val cells = puzzle.given.toIntArray()
    private var moves = 0
    private var won = false

    val isFinished: Boolean get() = won

    val state: BinState get() = BinState(puzzle, cells.toList(), moves, if (won) BinStatus.Won else BinStatus.Playing)

    /** Taps the cell number [cell] (row * size + column). */
    fun tap(cell: Int): BinResult {
        if (won || cell !in cells.indices || puzzle.isGiven(cell)) return BinResult.Ignored
        val next = when (cells[cell]) {
            -1 -> 0
            0 -> 1
            else -> -1
        }
        cells[cell] = next
        moves++
        if (next >= 0 && BinRules.isSolved(puzzle.size, cells)) {
            won = true
            return BinResult.Solved
        }
        return when (next) {
            0 -> BinResult.Zero
            1 -> BinResult.One
            else -> BinResult.Cleared
        }
    }

    /** Empties every cell the player filled. Returns false when there were none (or the board is won). */
    fun restart(): Boolean {
        if (won) return false
        var any = false
        for (i in cells.indices) if (!puzzle.isGiven(i) && cells[i] >= 0) {
            cells[i] = -1
            any = true
        }
        return any
    }

    /**
     * Puts back what a saved game holds: the digit of every cell (the given ones are kept as they are and must come back as they were)
     * and the [savedMoves] made. Returns false if that cannot be a game of this board (the wrong number of cells, a given cell changed,
     * fewer moves than filled cells, a board that is solved): nothing is changed then.
     */
    fun restore(saved: List<Int>, savedMoves: Int): Boolean {
        if (saved.size != cells.size || saved.any { it !in -1..1 }) return false
        if (saved.indices.any { puzzle.isGiven(it) && saved[it] != puzzle.given[it] }) return false
        if (savedMoves < saved.indices.count { !puzzle.isGiven(it) && saved[it] >= 0 }) return false
        if (BinRules.isSolved(puzzle.size, saved.toIntArray())) return false
        saved.forEachIndexed { i, v -> cells[i] = v }
        moves = savedMoves
        return true
    }
}
