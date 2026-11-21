package com.mehmtcan.brainscroll.game.matchsticks

enum class MatchStatus { Playing, Won }

/** What a move did. */
enum class MatchResult {
    /** Nothing changed (a stick that is not there, a place that is taken, a sum that has to be put back first). */
    Ignored,

    /** The stick moved, and the sum is still wrong. */
    Moved,

    /** The stick moved and the sum is right. */
    Solved,
}

/** The picture of the sum for the UI. */
data class MatchState(
    /** The sticks of each symbol as they are now (see [MatchPuzzle]). */
    val cells: List<Int>,
    /** The sticks of each symbol as the board began. */
    val start: List<Int>,
    /** True while a stick is off its place: the sum has to be put back before another move. */
    val moved: Boolean,
    /** Moves made so far; putting the stick back does not give them back. */
    val moves: Int,
    val status: MatchStatus,
    /** True when the sum reads as a sum (it may still be wrong). */
    val readable: Boolean,
    val canUndo: Boolean,
)

/**
 * The rules of the matchstick sum, free of UI and time. One stick is moved; if the sum is right the board is won, if it is not,
 * the stick has to be put back (the move stays counted) before the next try. There is no losing.
 */
class MatchGame(private val puzzle: MatchPuzzle) {
    private var cells: List<Int> = puzzle.cells
    private var moves = 0

    private val moved: Boolean get() = cells != puzzle.cells

    val isFinished: Boolean get() = moved && MatchRules.isTrue(cells)

    val state: MatchState
        get() = MatchState(
            cells = cells,
            start = puzzle.cells,
            moved = moved,
            moves = moves,
            status = if (isFinished) MatchStatus.Won else MatchStatus.Playing,
            readable = MatchRules.isValid(cells),
            canUndo = moved && !isFinished,
        )

    /** Takes the stick on [move]`.from` and lays it on `.to`. */
    fun move(move: MatchMove): MatchResult {
        if (isFinished || moved) return MatchResult.Ignored
        val from = move.from
        val to = move.to
        if (from.cell !in cells.indices || to.cell !in cells.indices) return MatchResult.Ignored
        if (from.bit !in 0 until MatchRules.width(from.cell) || to.bit !in 0 until MatchRules.width(to.cell)) return MatchResult.Ignored
        if ((cells[from.cell] shr from.bit) and 1 == 0 || (cells[to.cell] shr to.bit) and 1 == 1) return MatchResult.Ignored
        cells = MatchRules.apply(cells, move)
        moves++
        return if (isFinished) MatchResult.Solved else MatchResult.Moved
    }

    /** Puts the stick back. Returns false if no stick is off its place (or the board is won). */
    fun undo(): Boolean {
        if (isFinished || !moved) return false
        cells = puzzle.cells
        return true
    }

    /**
     * Puts back what [state] shows, for a saved game. Returns false if [savedCells] cannot come from this board with at most one
     * stick moved: nothing is changed then.
     */
    fun restore(savedCells: List<Int>, savedMoves: Int): Boolean {
        if (savedMoves < 0 || savedCells.size != puzzle.cells.size) return false
        for ((i, mask) in savedCells.withIndex()) if (mask !in 0 until (1 shl MatchRules.width(i))) return false
        val changed = savedCells.indices.sumOf { (savedCells[it] xor puzzle.cells[it]).countOneBits() }
        val sticks = savedCells.sumOf { it.countOneBits() }
        if (changed != 0 && (changed != 2 || sticks != puzzle.cells.sumOf { it.countOneBits() })) return false
        cells = savedCells
        moves = savedMoves
        return true
    }
}
