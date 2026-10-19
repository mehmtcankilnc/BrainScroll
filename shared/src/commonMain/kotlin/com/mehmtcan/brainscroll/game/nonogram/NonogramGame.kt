package com.mehmtcan.brainscroll.game.nonogram

enum class NonogramStatus { Playing, Won }

/** What a stroke did. */
enum class NonogramResult {
    /** Nothing happened: cells that are not on one line, off the board, a stroke that changes nothing, or the game is over. */
    Ignored,

    /** Some cells changed. */
    Changed,

    /** The stroke made the picture right. */
    Solved,
}

/** What a cell shows. */
object NonogramMark {
    const val EMPTY = 0
    const val FILLED = 1
    const val CROSS = 2
}

/** The picture of the board for the UI. Cells are numbered row by row from the top left. */
data class NonogramState(
    val size: Int,
    /** Per cell: [NonogramMark.EMPTY], [NonogramMark.FILLED] or [NonogramMark.CROSS]. */
    val marks: List<Int>,
    val rowClues: List<List<Int>>,
    val columnClues: List<List<Int>>,
    /** Per row and column: the filled cells match its numbers exactly (the numbers fade). */
    val rowDone: List<Boolean>,
    val columnDone: List<Boolean>,
    val filledCount: Int,
    val status: NonogramStatus,
    /** The picture, set once it is solved (null before, and null for a board that is not a drawn picture). */
    val picture: NonogramPicture?,
    /** False while the board is clear. */
    val canRestart: Boolean,
)

/**
 * The rules of the mini nonogram, free of UI and time.
 *
 * The numbers beside each row and above each column give the lengths of its runs of filled cells. The player fills cells and
 * crosses the ones that must stay empty (the crosses are only a note). The board is solved when the filled cells are exactly the
 * picture's. There is no losing. A restart clears the board.
 *
 * A stroke is a line of cells from one cell to another along a row or a column, all set to one mark; a single tap is a stroke of
 * one cell.
 */
class NonogramGame(private val puzzle: NonogramPuzzle) {
    private val size = puzzle.size
    private val marks = IntArray(size * size)

    private fun filled(): Set<Int> = marks.indices.filterTo(HashSet()) { marks[it] == NonogramMark.FILLED }

    val isFinished: Boolean get() = filled() == puzzle.solution

    private fun lineDone(clues: List<Int>, cells: List<Int>): Boolean = lineClues(cells.map { marks[it] == NonogramMark.FILLED }) == clues

    val state: NonogramState
        get() {
            val won = isFinished
            return NonogramState(
                size = size,
                marks = marks.toList(),
                rowClues = puzzle.rowClues,
                columnClues = puzzle.columnClues,
                rowDone = List(size) { r -> lineDone(puzzle.rowClues[r], List(size) { c -> r * size + c }) },
                columnDone = List(size) { c -> lineDone(puzzle.columnClues[c], List(size) { r -> r * size + c }) },
                filledCount = marks.count { it == NonogramMark.FILLED },
                status = if (won) NonogramStatus.Won else NonogramStatus.Playing,
                picture = if (won) puzzle.picture else null,
                canRestart = !won && marks.any { it != NonogramMark.EMPTY },
            )
        }

    /** The cells of the stroke from [from] to [to], or null if they are not on one row or column (or not on the board). */
    private fun line(from: Int, to: Int): List<Int>? {
        val total = size * size
        if (from !in 0 until total || to !in 0 until total) return null
        val (r1, c1) = from / size to from % size
        val (r2, c2) = to / size to to % size
        return when {
            r1 == r2 -> (minOf(c1, c2)..maxOf(c1, c2)).map { r1 * size + it }
            c1 == c2 -> (minOf(r1, r2)..maxOf(r1, r2)).map { it * size + c1 }
            else -> null
        }
    }

    /** Sets every cell from [from] to [to] (on one line) to [value], one of the [NonogramMark]s. */
    fun paint(from: Int, to: Int, value: Int): NonogramResult {
        if (isFinished || value !in 0..2) return NonogramResult.Ignored
        val cells = line(from, to) ?: return NonogramResult.Ignored
        var changed = false
        for (cell in cells) {
            if (marks[cell] != value) {
                marks[cell] = value
                changed = true
            }
        }
        if (!changed) return NonogramResult.Ignored
        return if (isFinished) NonogramResult.Solved else NonogramResult.Changed
    }

    /** Clears every mark. Returns false if the board was clear already. */
    fun restart(): Boolean {
        if (!state.canRestart) return false
        marks.fill(NonogramMark.EMPTY)
        return true
    }
}
