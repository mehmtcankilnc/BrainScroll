package com.mehmtcan.brainscroll.game.arrows

enum class ArrowsStatus { Playing, Won }

/** What a tap did. */
enum class ArrowsResult {
    /** Nothing happened: an empty cell, a cell that does not exist, or the game is over. */
    Ignored,

    /** The arrow left the board. */
    Escaped,

    /** Another arrow is in the way: a mistake. */
    Blocked,

    /** The arrow that left was the last one. */
    Solved,
}

/** One arrow for the UI: its cells from the tail to the head, the way the head points, and whether it can leave now. */
data class ArrowState(val cells: List<Int>, val dir: Int, val alive: Boolean, val free: Boolean) {
    val head: Int get() = cells.last()
}

/** The picture of the board for the UI. Cells are numbered row by row from the top left. */
data class ArrowsState(
    val size: Int,
    val arrows: List<ArrowState>,
    /** Arrows still on the board. */
    val remaining: Int,
    val total: Int,
    /** Taps on a blocked arrow so far. */
    val mistakes: Int,
    /** The arrow of the last blocked tap and the cell in its way (its arrow is [lastBlocker]), or -1. */
    val lastBlocked: Int,
    val lastBlockCell: Int,
    val lastBlocker: Int,
    val status: ArrowsStatus,
    /** False while every arrow is still there. */
    val canRestart: Boolean,
)

/**
 * The rules of arrow escape, free of UI and time.
 *
 * A tap on an arrow takes it off the board if nothing is in front of its head, all the way to the edge: it slides along its own
 * path and out. If another arrow is in the way, it is a mistake. The board is won when every arrow has left. There is no
 * losing; a restart puts the arrows back and the mistakes stay counted.
 */
class ArrowsGame(private val puzzle: ArrowsPuzzle) {
    private val alive = BooleanArray(puzzle.arrowCount) { true }
    private var mistakes = 0
    private var lastBlocked = -1
    private var lastBlockCell = -1
    private var lastBlocker = -1

    val isFinished: Boolean get() = alive.none { it }

    val state: ArrowsState
        get() {
            val free = ArrowsRules.free(puzzle, alive).toSet()
            val remaining = alive.count { it }
            return ArrowsState(
                size = puzzle.size,
                arrows = puzzle.arrows.mapIndexed { i, a -> ArrowState(a.cells, a.dir, alive[i], i in free) },
                remaining = remaining,
                total = puzzle.arrowCount,
                mistakes = mistakes,
                lastBlocked = lastBlocked,
                lastBlockCell = lastBlockCell,
                lastBlocker = lastBlocker,
                status = if (remaining == 0) ArrowsStatus.Won else ArrowsStatus.Playing,
                canRestart = remaining in 1 until puzzle.arrowCount,
            )
        }

    /** Taps [cell]: any cell of an arrow that is still on the board taps that arrow. */
    fun tap(cell: Int): ArrowsResult {
        if (isFinished || cell !in puzzle.owner.indices) return ArrowsResult.Ignored
        val arrow = puzzle.owner[cell]
        if (arrow < 0 || !alive[arrow]) return ArrowsResult.Ignored
        val blockCell = ArrowsRules.blockingCell(puzzle, alive, arrow)
        if (blockCell >= 0) {
            mistakes++
            lastBlocked = arrow
            lastBlockCell = blockCell
            lastBlocker = puzzle.owner[blockCell]
            return ArrowsResult.Blocked
        }
        alive[arrow] = false
        return if (alive.none { it }) ArrowsResult.Solved else ArrowsResult.Escaped
    }

    /** Puts every arrow back. The mistakes stay counted. Returns false if nothing had left yet. */
    fun restart(): Boolean {
        if (!state.canRestart) return false
        alive.fill(true)
        return true
    }
}
