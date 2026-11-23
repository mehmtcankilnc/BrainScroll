package com.mehmtcan.brainscroll.game.mirror

enum class MirrorStatus { Playing, Won }

/** What a tap did. */
enum class MirrorResult {
    /** Nothing changed (a board that is won, a cell of the given half, a cell that is not on the board). */
    Ignored,

    /** The cell was painted. */
    Painted,

    /** The cell was emptied. */
    Erased,

    /** The tap left the board the same on both sides: it is won. */
    Solved,
}

/** The picture of the board for the UI. */
data class MirrorState(
    val puzzle: MirrorPuzzle,
    /** Whether every cell is painted, row after row (the given half first as it was given). */
    val marks: List<Boolean>,
    /** Taps that changed a cell so far. */
    val moves: Int,
    val status: MirrorStatus,
) {
    /** How many cells of the half to fill are painted. */
    val painted: Int get() = puzzle.fillCells.count { marks[it] }

    /** The cells of the half to fill that do not match their partner yet. */
    val mismatched: List<Int> get() = puzzle.fillCells.filter { marks[it] != puzzle.target(it) }
}

/**
 * The rules of the mirror, free of UI and time. A tap on a cell of the half to fill paints it, a tap on a painted one empties it; the
 * given half is never changed. The board is won when every cell of the half to fill is like its partner. There is no losing.
 */
class MirrorGame(private val puzzle: MirrorPuzzle) {
    private val marks = BooleanArray(puzzle.size * puzzle.size) { puzzle.isGiven(it) && puzzle.pattern[it] }
    private var moves = 0
    private var won = false

    val isFinished: Boolean get() = won

    val state: MirrorState get() = MirrorState(puzzle, marks.toList(), moves, if (won) MirrorStatus.Won else MirrorStatus.Playing)

    /** Taps the cell number [cell] (row * size + column). */
    fun tap(cell: Int): MirrorResult {
        if (won || cell !in marks.indices || puzzle.isGiven(cell)) return MirrorResult.Ignored
        marks[cell] = !marks[cell]
        moves++
        if (puzzle.fillCells.all { marks[it] == puzzle.target(it) }) {
            won = true
            return MirrorResult.Solved
        }
        return if (marks[cell]) MirrorResult.Painted else MirrorResult.Erased
    }

    /** Empties every cell of the half to fill. Returns false when there was nothing painted (or the board is won). */
    fun restart(): Boolean {
        if (won || puzzle.fillCells.none { marks[it] }) return false
        for (cell in puzzle.fillCells) marks[cell] = false
        return true
    }

    /**
     * Puts back what a saved game holds: the cells of the half to fill, in order, and the [savedMoves] made. Returns false if that
     * cannot be a game of this board (the wrong number of cells, fewer moves than painted cells, a board that is solved): nothing
     * is changed then.
     */
    fun restore(saved: List<Boolean>, savedMoves: Int): Boolean {
        val cells = puzzle.fillCells
        if (saved.size != cells.size || savedMoves < saved.count { it }) return false
        if (cells.indices.all { saved[it] == puzzle.target(cells[it]) }) return false
        cells.forEachIndexed { i, cell -> marks[cell] = saved[i] }
        moves = savedMoves
        return true
    }
}
