package com.mehmtcan.brainscroll.game.lonenumber

enum class LoneStatus { Playing, Won }

/** What the mark of a cell is. Tapping a cell goes empty, shaded, circled, empty... */
enum class LoneMark { Empty, Shaded, Circled }

/** What a tap did. */
enum class LoneResult {
    /** Nothing changed (a board that is won, a cell that is not on the board). */
    Ignored,

    /** The cell was shaded and the board is not solved yet. */
    Shaded,

    /** The cell was circled: the player says that number stays. */
    Circled,

    /** The mark of the cell was taken off. */
    Cleared,

    /** The cell cannot be shaded: a cell next to it is shaded already. */
    Rejected,

    /** The tap left the board solved (the last cell shaded, or a wrong one turned into a circle): it is won. */
    Solved,
}

/** The picture of the board for the UI. */
data class LoneState(
    val puzzle: LonePuzzle,
    /** The mark of every cell, row after row. */
    val marks: List<LoneMark>,
    /** Taps that changed a cell so far (taking a mark off counts). */
    val moves: Int,
    val status: LoneStatus,
) {
    /** The cells that are shaded. */
    val shaded: Set<Int> get() = marks.indices.filterTo(HashSet()) { marks[it] == LoneMark.Shaded }

    /** The cells that are not shaded and hold a number that another cell left in their row or column holds too. */
    val repeated: Set<Int> get() = LoneRules.repeated(puzzle, shaded)
}

/**
 * The rules of the lone numbers, free of UI and time. A tap on an empty cell shades it (unless a cell next to it is shaded: that is
 * refused), a tap on a shaded cell circles it, a tap on a circled cell empties it. The board is won when the cells left follow the
 * rules: no number twice in a row or column, all in one piece (shaded cells never touch, the game does not allow it). Since the board
 * has only one solution, that is it. There is no losing.
 */
class LoneGame(private val puzzle: LonePuzzle) {
    private val marks = Array(puzzle.size * puzzle.size) { LoneMark.Empty }
    private var moves = 0
    private var won = false

    val isFinished: Boolean get() = won

    val state: LoneState get() = LoneState(puzzle, marks.toList(), moves, if (won) LoneStatus.Won else LoneStatus.Playing)

    /** Taps the cell number [cell] (row * size + column). */
    fun tap(cell: Int): LoneResult {
        if (won || cell !in marks.indices) return LoneResult.Ignored
        val before = marks[cell]
        if (before == LoneMark.Empty && puzzle.neighbours(cell).any { marks[it] == LoneMark.Shaded }) return LoneResult.Rejected
        marks[cell] = when (before) {
            LoneMark.Empty -> LoneMark.Shaded
            LoneMark.Shaded -> LoneMark.Circled
            LoneMark.Circled -> LoneMark.Empty
        }
        moves++
        // Any tap can finish the board: turning a wrong shaded cell into a circle is the last step as often as shading one is.
        if (LoneRules.isSolved(puzzle, shadedCells())) {
            won = true
            return LoneResult.Solved
        }
        return when (marks[cell]) {
            LoneMark.Shaded -> LoneResult.Shaded
            LoneMark.Circled -> LoneResult.Circled
            LoneMark.Empty -> LoneResult.Cleared
        }
    }

    /** Takes every mark off. Returns false when there were none (or the board is won). */
    fun restart(): Boolean {
        if (won || marks.all { it == LoneMark.Empty }) return false
        marks.fill(LoneMark.Empty)
        return true
    }

    /**
     * Puts back what a saved game holds: the [saved] marks and the [savedMoves] made. Returns false if that cannot be a game of this
     * board (the wrong number of cells, two shaded cells that touch, a board that is solved, fewer moves than marks): nothing is
     * changed then.
     */
    fun restore(saved: List<LoneMark>, savedMoves: Int): Boolean {
        if (saved.size != marks.size) return false
        val shaded = saved.indices.filterTo(HashSet()) { saved[it] == LoneMark.Shaded }
        if (LoneRules.touching(puzzle, shaded).isNotEmpty()) return false
        if (savedMoves < saved.count { it != LoneMark.Empty }) return false
        if (LoneRules.isSolved(puzzle, shaded)) return false
        saved.forEachIndexed { i, mark -> marks[i] = mark }
        moves = savedMoves
        return true
    }

    private fun shadedCells(): Set<Int> = marks.indices.filterTo(HashSet()) { marks[it] == LoneMark.Shaded }
}
