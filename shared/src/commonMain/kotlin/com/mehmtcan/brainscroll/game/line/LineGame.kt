package com.mehmtcan.brainscroll.game.line

enum class LineStatus { Playing, Won }

/** What a touch did. */
enum class LineResult {
    /** Nothing happened: out of range, not next to the line, the game is over, or the line has not begun on number 1. */
    Ignored,

    /** The line began on number 1. */
    Started,

    /** The line grew by one cell. */
    Extended,

    /** The line shrank by its last cell because the finger went back over it. Free, it is part of drawing. */
    Retracted,

    /** The line was cut back to the touched cell. Counts as an undo. */
    Cut,

    /** The cell holds a number that is not the next one. */
    WrongOrder,

    /** The cell holds the last number but other cells are still free. */
    EndTooEarly,

    /** The touch filled the last cell. */
    Solved,
}

/** The picture of a board for the UI. Cells are numbered row by row from 0; the line is in the order it was drawn. */
data class LineState(
    val size: Int,
    val path: List<Int>,
    /** How many numbers the line has passed. */
    val reached: Int,
    val undos: Int,
    val hintsUsed: Int,
    val status: LineStatus,
    val canUndo: Boolean,
    val canRestart: Boolean,
)

/** Stars of a solved board (docs/tek-cizgi-plan.md). */
object LineScoring {
    const val MAX_STARS = 3

    /** 3 stars without an undo, 2 with up to three, 1 with more; a hint takes one star off, never below 0. */
    fun stars(undos: Int, hintsUsed: Int): Int {
        val base = when {
            undos == 0 -> 3
            undos <= 3 -> 2
            else -> 1
        }
        return if (hintsUsed > 0) base - 1 else base
    }

    /**
     * The one number a result keeps as its guess and the server gets as its moves: 1 + undos (up to 99) + 100 * hints (up
     * to 9). It is never 0 (the server wants at least one move), and the server works the stars out of it the same way
     * (`line_stars` in supabase/migrations/20261014090000_line_daily.sql).
     */
    fun encode(undos: Int, hintsUsed: Int): Int = 1 + undos.coerceIn(0, 99) + 100 * hintsUsed.coerceIn(0, 9)

    fun undosOf(code: Int): Int = (code - 1).coerceAtLeast(0) % 100

    fun hintsOf(code: Int): Int = (code - 1).coerceAtLeast(0) / 100

    fun starsOf(code: Int): Int = stars(undosOf(code), hintsOf(code))
}

/**
 * The rules of the one-line game, free of UI and time.
 *
 * The line begins on number 1 and grows one cell at a time to a free neighbor. A number can only be entered when it is
 * the next one, and the last number only when it is the last free cell. The game is won when every cell is filled.
 * There is no losing. Going back over the end of the line takes that cell off for free; the undo button, a tap on an
 * earlier cell of the line (which cuts it back) and a restart each count as an undo.
 */
class LineGame(val puzzle: LinePuzzle) {
    private val path = ArrayList<Int>()
    private var reached = 0
    private var undos = 0
    private var hintsUsed = 0
    private val size = puzzle.size

    val isFinished: Boolean get() = path.size == puzzle.cells

    val state: LineState
        get() = LineState(
            size = size,
            path = path.toList(),
            reached = reached,
            undos = undos,
            hintsUsed = hintsUsed,
            status = if (isFinished) LineStatus.Won else LineStatus.Playing,
            canUndo = !isFinished && path.size > 1,
            canRestart = !isFinished && path.isNotEmpty(),
        )

    /** A finger moving over [cell] while drawing: grows the line, or shortens it when it goes back. Never cuts. */
    fun drag(cell: Int): LineResult = touch(cell, allowCut = false)

    /** A tap on [cell]: like [drag], and a tap on an earlier cell of the line cuts the line back to it. */
    fun tap(cell: Int): LineResult = touch(cell, allowCut = true)

    private fun touch(cell: Int, allowCut: Boolean): LineResult {
        if (isFinished || cell !in 0 until puzzle.cells) return LineResult.Ignored
        if (path.isEmpty()) {
            if (cell != puzzle.checkpoints[0]) return LineResult.Ignored
            path += cell
            reached = 1
            return LineResult.Started
        }
        val head = path.last()
        if (cell == head) return LineResult.Ignored
        if (path.size >= 2 && cell == path[path.size - 2]) {
            removeLast()
            return LineResult.Retracted
        }
        val at = path.indexOf(cell)
        if (at >= 0) {
            if (!allowCut) return LineResult.Ignored
            while (path.size > at + 1) removeLast()
            undos++
            return LineResult.Cut
        }
        if (!areNeighbors(head, cell)) return LineResult.Ignored
        val number = puzzle.numbers[cell]
        if (number > 0) {
            if (number != reached + 1) return LineResult.WrongOrder
            if (number == puzzle.lastNumber && path.size != puzzle.cells - 1) return LineResult.EndTooEarly
            reached++
        }
        path += cell
        return if (isFinished) LineResult.Solved else LineResult.Extended
    }

    /** Takes the last cell off the line (never number 1 itself). Counts as an undo. Returns false if there is nothing to take off. */
    fun undo(): Boolean {
        if (isFinished || path.size <= 1) return false
        removeLast()
        undos++
        return true
    }

    /** Clears the line. Counts as an undo. Returns false if the line had not begun. */
    fun restart(): Boolean {
        if (isFinished || path.isEmpty()) return false
        path.clear()
        reached = 0
        undos++
        return true
    }

    /**
     * The next cells of the one line that solves the board from what is drawn, [count] at most (from number 1 if nothing is
     * drawn). Null if the game is over or the drawn line leads nowhere (then an undo is the hint). Counts as a hint
     * only when it gives cells.
     */
    fun hint(count: Int = 3): List<Int>? {
        if (isFinished) return null
        val full = LineSolver.solve(size, puzzle.checkpoints, path.toIntArray()) ?: return null
        hintsUsed++
        return full.drop(path.size).take(count)
    }

    private fun removeLast() {
        val cell = path.removeAt(path.size - 1)
        if (puzzle.numbers[cell] > 0) reached--
    }

    private fun areNeighbors(a: Int, b: Int): Boolean {
        val rows = kotlin.math.abs(a / size - b / size)
        val columns = kotlin.math.abs(a % size - b % size)
        return rows + columns == 1
    }
}
