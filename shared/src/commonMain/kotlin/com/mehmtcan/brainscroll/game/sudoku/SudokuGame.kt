package com.mehmtcan.brainscroll.game.sudoku

enum class SudokuStatus { Playing, Won, Lost }

/** What a move did. */
enum class SudokuResult {
    /** Nothing happened: a given or a correct cell, the same digit again, out of range, or the game is over. Not counted. */
    Ignored,

    /** A note or an erase changed. */
    Changed,

    /** A digit was placed and it is the right one. */
    Correct,

    /** A digit was placed and it is wrong: it counts as a mistake. */
    Mistake,
}

/** The picture of a board for the UI. Cells are numbered row by row from 0. */
data class SudokuState(
    /** Per cell: the digit the board started with, 0 if empty. */
    val givens: List<Int>,
    /** Per cell: the digit now in it (givens included), 0 if empty. */
    val values: List<Int>,
    /** Per cell: the pencil notes as a bit mask (bit 0 is digit 1). Only empty cells have notes. */
    val notes: List<Int>,
    /** The cells holding a wrong digit. */
    val wrong: Set<Int>,
    /** Digits placed so far (notes and erasing are not moves). */
    val moves: Int,
    val mistakes: Int,
    val maxMistakes: Int,
    val status: SudokuStatus,
) {
    fun isGiven(cell: Int) = givens[cell] != 0
}

/**
 * The rules of sudoku, free of UI and time.
 *
 * The board comes from [SudokuGenerator] and depends only on [seed] and [clues]. A digit that differs from the solution
 * is a mistake and the third one loses. A correct digit is final, so only wrong digits and notes can be taken back.
 */
class SudokuGame(
    val seed: Int,
    val clues: Int = SudokuGenerator.DEFAULT_CLUES,
    val maxMistakes: Int = DEFAULT_MAX_MISTAKES,
) {
    private val puzzle = SudokuGenerator.generate(seed, clues)
    private val values = puzzle.givens.copyOf()
    private val notes = IntArray(SudokuGenerator.CELLS)
    private var moves = 0
    private var mistakes = 0
    private var status = SudokuStatus.Playing

    init {
        require(maxMistakes >= 1) { "a game needs at least one allowed mistake" }
        require(clues in 17..80) { "too few or too many givens" }
    }

    val isFinished: Boolean get() = status != SudokuStatus.Playing

    val state: SudokuState
        get() = SudokuState(
            givens = puzzle.givens.toList(),
            values = values.toList(),
            notes = notes.toList(),
            wrong = wrongCells(),
            moves = moves,
            mistakes = mistakes,
            maxMistakes = maxMistakes,
            status = status,
        )

    private fun wrongCells(): Set<Int> =
        (0 until SudokuGenerator.CELLS).filterTo(mutableSetOf()) { values[it] != 0 && values[it] != puzzle.solution[it] }

    /** True if [cell] can be changed: it is on the board, not a given, not already right, and the game goes on. */
    fun isEditable(cell: Int): Boolean =
        !isFinished && cell in 0 until SudokuGenerator.CELLS && puzzle.givens[cell] == 0 && values[cell] != puzzle.solution[cell]

    /** Puts [digit] (1..9) in [cell]. A wrong one counts as a mistake. */
    fun place(cell: Int, digit: Int): SudokuResult {
        if (!isEditable(cell) || digit !in 1..9 || values[cell] == digit) return SudokuResult.Ignored
        values[cell] = digit
        notes[cell] = 0
        moves++
        if (digit != puzzle.solution[cell]) {
            mistakes++
            if (mistakes >= maxMistakes) status = SudokuStatus.Lost
            return SudokuResult.Mistake
        }
        // The digit is settled: it cannot be a candidate anywhere around it any more.
        val bit = 1 shl (digit - 1)
        for (peer in SudokuGenerator.peers(cell)) notes[peer] = notes[peer] and bit.inv()
        if ((0 until SudokuGenerator.CELLS).all { values[it] == puzzle.solution[it] }) status = SudokuStatus.Won
        return SudokuResult.Correct
    }

    /** Switches the pencil note [digit] on or off in the empty cell [cell]. */
    fun toggleNote(cell: Int, digit: Int): SudokuResult {
        if (!isEditable(cell) || digit !in 1..9 || values[cell] != 0) return SudokuResult.Ignored
        notes[cell] = notes[cell] xor (1 shl (digit - 1))
        return SudokuResult.Changed
    }

    /** Takes back a wrong digit, or clears the notes of an empty cell. */
    fun erase(cell: Int): SudokuResult {
        if (!isEditable(cell) || (values[cell] == 0 && notes[cell] == 0)) return SudokuResult.Ignored
        values[cell] = 0
        notes[cell] = 0
        return SudokuResult.Changed
    }

    companion object {
        const val DEFAULT_MAX_MISTAKES = 3
    }
}
