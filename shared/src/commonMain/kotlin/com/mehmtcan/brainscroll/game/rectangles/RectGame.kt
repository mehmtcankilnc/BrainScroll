package com.mehmtcan.brainscroll.game.rectangles

enum class RectStatus { Playing, Won }

/** What laying a rectangle did. */
enum class RectResult {
    /** Nothing changed (a board that is won, a rectangle that is not on the board). */
    Ignored,

    /** The rectangle is not allowed: it does not hold exactly one number, is not as big as the number, or covers another rectangle. */
    Rejected,

    /** The rectangle was laid and the board is not covered yet. */
    Placed,

    /** The rectangle was laid and the board is covered: it is won. */
    Solved,
}

/** The picture of the board for the UI. */
data class RectState(
    val puzzle: RectPuzzle,
    /** The rectangles laid so far, the oldest first. */
    val rects: List<Rect>,
    /** Rectangles laid so far (taking one off does not give it back). */
    val moves: Int,
    val status: RectStatus,
) {
    /** The rectangle that covers the cell ([column], [row]), if there is one. */
    fun rectAt(column: Int, row: Int): Rect? = rects.firstOrNull { it.contains(column, row) }

    /** How many cells are covered. */
    val covered: Int get() = rects.sumOf { it.area }
}

/**
 * The rules of the rectangles, free of UI and time. A rectangle is allowed when it holds exactly one number, its area is that number and
 * it does not touch another rectangle. The board is won when every cell is covered; since the board has only one solution, that is it.
 * There is no losing.
 */
class RectGame(private val puzzle: RectPuzzle) {
    private val rects = ArrayList<Rect>()
    private var moves = 0
    private var won = false

    val isFinished: Boolean get() = won

    val state: RectState get() = RectState(puzzle, rects.toList(), moves, if (won) RectStatus.Won else RectStatus.Playing)

    /** Lays [rect]. */
    fun place(rect: Rect): RectResult {
        if (won) return RectResult.Ignored
        val n = puzzle.size
        if (rect.w < 1 || rect.h < 1 || rect.x < 0 || rect.y < 0 || rect.x + rect.w > n || rect.y + rect.h > n) return RectResult.Ignored
        if (!puzzle.isFilledRight(rect) || rects.any { it.overlaps(rect) }) return RectResult.Rejected
        rects += rect
        moves++
        if (rects.sumOf { it.area } == n * n) {
            won = true
            return RectResult.Solved
        }
        return RectResult.Placed
    }

    /** Takes off the rectangle that covers the cell ([column], [row]). Returns false when there is none (or the board is won). */
    fun remove(column: Int, row: Int): Boolean {
        if (won) return false
        return rects.removeAll { it.contains(column, row) }
    }

    /** Takes every rectangle off. Returns false when there were none (or the board is won). */
    fun restart(): Boolean {
        if (won || rects.isEmpty()) return false
        rects.clear()
        return true
    }

    /**
     * Puts back what a saved game holds: the [saved] rectangles and the [savedMoves] made. Returns false if that cannot be a game of this
     * board (a rectangle that is not allowed, two that overlap, one that covers the board, fewer moves than rectangles): nothing is
     * changed then.
     */
    fun restore(saved: List<Rect>, savedMoves: Int): Boolean {
        if (savedMoves < saved.size) return false
        for ((i, r) in saved.withIndex()) {
            if (!puzzle.isFilledRight(r) || saved.take(i).any { it.overlaps(r) }) return false
        }
        if (saved.sumOf { it.area } == puzzle.size * puzzle.size) return false
        rects.clear()
        rects += saved
        moves = savedMoves
        return true
    }
}
