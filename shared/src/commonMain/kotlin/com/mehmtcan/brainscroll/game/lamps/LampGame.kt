package com.mehmtcan.brainscroll.game.lamps

enum class LampStatus { Playing, Won }

/** What the player put on a white cell. Tapping a cell goes empty, lamp, dot, empty... */
enum class LampMark { Empty, Lamp, Dot }

/** What a tap did. */
enum class LampResult {
    /** Nothing changed (a board that is won, a wall, a cell that is not on the board). */
    Ignored,

    /** A lamp was put on the cell. */
    Lamp,

    /** The cell got a dot: "no lamp here" (a note for the player, it changes nothing in the rules). */
    Dot,

    /** The mark of the cell was taken off. */
    Cleared,

    /** The tap left the board solved (the last lamp put, or the last wrong lamp taken off): it is won. */
    Solved,
}

/** The picture of the board for the UI. */
data class LampState(
    val puzzle: LampPuzzle,
    /** The mark of every cell, row after row (walls are always empty). */
    val marks: List<LampMark>,
    /** Taps that changed a cell so far (taking a mark off counts). */
    val moves: Int,
    val status: LampStatus,
) {
    /** A flag for every cell that holds a lamp. */
    val lamps: BooleanArray get() = BooleanArray(marks.size) { marks[it] == LampMark.Lamp }

    /** A flag for every cell that is lit. */
    val lit: BooleanArray get() = LampRules.lit(puzzle, lamps)

    /** The lamps that light another lamp. */
    val clashing: Set<Int> get() = LampRules.clashing(puzzle, lamps)

    /** The numbered walls that have more lamps next to them than their number. */
    val overfull: Set<Int> get() = LampRules.overfull(puzzle, lamps)

    /** The numbered walls that have exactly their number of lamps next to them. */
    val satisfied: Set<Int> get() = LampRules.satisfied(puzzle, lamps)

    /** How many white cells are lit. */
    val litCount: Int get() = lit.let { lit -> puzzle.whiteCells.count { lit[it] } }
}

/**
 * The rules of the lamps, free of UI and time. A tap on a white cell goes empty, lamp, dot, empty... The board is won when every white
 * cell is lit, no two lamps light each other and every number is right; since the board has only one solution, that is it. There is no
 * losing and no tap is refused: a rule that is broken shows on the board.
 */
class LampGame(private val puzzle: LampPuzzle) {
    private val marks = Array(puzzle.size * puzzle.size) { LampMark.Empty }
    private var moves = 0
    private var won = false

    val isFinished: Boolean get() = won

    val state: LampState get() = LampState(puzzle, marks.toList(), moves, if (won) LampStatus.Won else LampStatus.Playing)

    /** Taps the cell number [cell] (row * size + column). */
    fun tap(cell: Int): LampResult {
        if (won || cell !in marks.indices || puzzle.isWall(cell)) return LampResult.Ignored
        val next = when (marks[cell]) {
            LampMark.Empty -> LampMark.Lamp
            LampMark.Lamp -> LampMark.Dot
            LampMark.Dot -> LampMark.Empty
        }
        marks[cell] = next
        moves++
        // Any tap can finish the board: taking a wrong lamp off (or turning it into a dot) is the last step as often as putting one is.
        if (LampRules.isSolved(puzzle, BooleanArray(marks.size) { marks[it] == LampMark.Lamp })) {
            won = true
            return LampResult.Solved
        }
        return when (next) {
            LampMark.Lamp -> LampResult.Lamp
            LampMark.Dot -> LampResult.Dot
            LampMark.Empty -> LampResult.Cleared
        }
    }

    /** Takes every mark off. Returns false when there were none (or the board is won). */
    fun restart(): Boolean {
        if (won || marks.all { it == LampMark.Empty }) return false
        marks.fill(LampMark.Empty)
        return true
    }

    /**
     * Puts back what a saved game holds: the [saved] marks and the [savedMoves] made. Returns false if that cannot be a game of this board
     * (the wrong number of cells, a mark on a wall, fewer moves than marks, a board that is solved): nothing is changed then.
     */
    fun restore(saved: List<LampMark>, savedMoves: Int): Boolean {
        if (saved.size != marks.size) return false
        if (saved.indices.any { puzzle.isWall(it) && saved[it] != LampMark.Empty }) return false
        if (savedMoves < saved.count { it != LampMark.Empty }) return false
        if (LampRules.isSolved(puzzle, BooleanArray(saved.size) { saved[it] == LampMark.Lamp })) return false
        saved.forEachIndexed { i, mark -> marks[i] = mark }
        moves = savedMoves
        return true
    }
}
