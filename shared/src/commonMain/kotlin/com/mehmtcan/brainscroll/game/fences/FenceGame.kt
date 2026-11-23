package com.mehmtcan.brainscroll.game.fences

enum class FenceStatus { Playing, Won }

/** What a tap did. */
enum class FenceResult {
    /** Nothing changed (a board that is won, an edge that is not on the board). */
    Ignored,

    /** A line was drawn on the edge. */
    Line,

    /** The edge got a cross: "no line here" (a note for the player, it changes nothing in the rules). */
    Cross,

    /** The mark of the edge was taken off. */
    Cleared,

    /** The tap left the lines one closed loop that meets every number: it is won. */
    Solved,
}

/** The picture of the board for the UI. */
data class FenceState(
    val puzzle: FencePuzzle,
    /** The mark of every edge ([FenceSolver.UNKNOWN] for none, [FenceSolver.LINE], [FenceSolver.CROSS]). */
    val marks: List<Int>,
    /** Taps that changed an edge so far (taking a mark off counts). */
    val moves: Int,
    val status: FenceStatus,
) {
    /** A flag for every edge that holds a line. */
    val lines: BooleanArray get() = BooleanArray(marks.size) { marks[it] == FenceSolver.LINE }

    /** The dots where three or four lines meet. */
    val branching: Set<Int> get() = FenceRules.branching(puzzle, lines)

    /** The squares that have more lines on their sides than their number. */
    val overfull: Set<Int> get() = FenceRules.overfull(puzzle, lines)

    /** The squares that have exactly as many lines on their sides as their number. */
    val satisfied: Set<Int> get() = FenceRules.satisfied(puzzle, lines)

    /** How many lines are drawn. */
    val lineCount: Int get() = marks.count { it == FenceSolver.LINE }
}

/**
 * The rules of the fences, free of UI and time. A tap on an edge goes empty, line, cross, empty... The board is won when the lines are
 * one closed loop and every number is right; since the board has only one loop, that is it. There is no losing and no tap is refused:
 * a rule that is broken shows on the board.
 */
class FenceGame(private val puzzle: FencePuzzle) {
    private val marks = IntArray(puzzle.geometry.edges)
    private var moves = 0
    private var won = false

    val isFinished: Boolean get() = won

    val state: FenceState get() = FenceState(puzzle, marks.toList(), moves, if (won) FenceStatus.Won else FenceStatus.Playing)

    /** Taps the edge number [edge] (see [FenceGeometry]). */
    fun tap(edge: Int): FenceResult {
        if (won || edge !in marks.indices) return FenceResult.Ignored
        marks[edge] = when (marks[edge]) {
            FenceSolver.UNKNOWN -> FenceSolver.LINE
            FenceSolver.LINE -> FenceSolver.CROSS
            else -> FenceSolver.UNKNOWN
        }
        moves++
        // Any tap can finish the board: taking a wrong line off (or turning it into a cross) is the last step as often as drawing one is.
        if (FenceRules.isSolved(puzzle, BooleanArray(marks.size) { marks[it] == FenceSolver.LINE })) {
            won = true
            return FenceResult.Solved
        }
        return when (marks[edge]) {
            FenceSolver.LINE -> FenceResult.Line
            FenceSolver.CROSS -> FenceResult.Cross
            else -> FenceResult.Cleared
        }
    }

    /** Takes every mark off. Returns false when there were none (or the board is won). */
    fun restart(): Boolean {
        if (won || marks.all { it == FenceSolver.UNKNOWN }) return false
        marks.fill(FenceSolver.UNKNOWN)
        return true
    }

    /**
     * Puts back what a saved game holds: the [saved] marks and the [savedMoves] made. Returns false if that cannot be a game of this board
     * (the wrong number of edges, a mark that is none of the three, fewer moves than marks, a board that is solved): nothing is changed then.
     */
    fun restore(saved: List<Int>, savedMoves: Int): Boolean {
        if (saved.size != marks.size || saved.any { it !in FenceSolver.UNKNOWN..FenceSolver.CROSS }) return false
        if (savedMoves < saved.count { it != FenceSolver.UNKNOWN }) return false
        if (FenceRules.isSolved(puzzle, BooleanArray(saved.size) { saved[it] == FenceSolver.LINE })) return false
        saved.forEachIndexed { i, mark -> marks[i] = mark }
        moves = savedMoves
        return true
    }
}
