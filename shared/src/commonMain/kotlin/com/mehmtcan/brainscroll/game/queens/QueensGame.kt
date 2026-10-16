package com.mehmtcan.brainscroll.game.queens

enum class QueensStatus { Playing, Won }

/** What a tap did. */
enum class QueensResult {
    /** Nothing happened: a locked queen, a cell that does not exist, or the game is over. Not counted. */
    Ignored,

    /** A queen was put on the cell. */
    Placed,

    /** The queen the player had put there was taken away. */
    Removed,

    /** The queen put there was the last one and nothing attacks anything. */
    Solved,
}

/** The picture of the board for the UI. Cells are numbered row by row from the top left. */
data class QueensState(
    val size: Int,
    /** True for every cell that holds a queen, locked or not. */
    val queens: List<Boolean>,
    /** True for the queens that were there from the start and cannot be taken away. */
    val locked: List<Boolean>,
    /** True for every queen that attacks another one (both of the two are marked). */
    val attacked: List<Boolean>,
    /** Taps so far. A restart does not give them back. */
    val moves: Int,
    /** The fewest taps that solve the puzzle: the queens the player still has to place. */
    val par: Int,
    val status: QueensStatus,
    /** False while the board is as it was at the start. */
    val canRestart: Boolean,
)

/**
 * The rules of the queens puzzle, free of UI and time (docs/yeni-oyunlar-plan.md).
 *
 * A square board and as many queens as it is wide: no two may share a row, a column or a diagonal. Some queens are [locked]
 * from the start. A tap puts a queen on an empty cell or takes away one the player put there. The game is won when the board
 * holds [size] queens and none attacks another. There is no losing. A restart takes away the queens of the player and keeps the moves.
 */
class QueensGame(val size: Int, locked: Set<Int>) {
    private val lockedCells = locked.toSet()
    private val queens = BooleanArray(size * size)
    private var moves = 0
    private var placed = 0

    init {
        require(size in MIN_SIZE..MAX_SIZE) { "a small board" }
        require(lockedCells.all { it in 0 until size * size }) { "locked queens are on the board" }
        require(lockedCells.size < size) { "there is something left to place" }
        for (cell in lockedCells) queens[cell] = true
    }

    val par: Int get() = size - lockedCells.size

    val isFinished: Boolean get() = queens.count { it } == size && QueensSolver.attacked(size, queens).none { it }

    val state: QueensState
        get() {
            val attacked = QueensSolver.attacked(size, queens)
            return QueensState(
                size = size,
                queens = queens.toList(),
                locked = List(size * size) { it in lockedCells },
                attacked = attacked.toList(),
                moves = moves,
                par = par,
                status = if (queens.count { it } == size && attacked.none { it }) QueensStatus.Won else QueensStatus.Playing,
                canRestart = placed > 0 && !isFinished,
            )
        }

    /** Puts a queen on [cell], or takes away the one the player put there. */
    fun toggle(cell: Int): QueensResult {
        if (isFinished || cell !in 0 until size * size || cell in lockedCells) return QueensResult.Ignored
        moves++
        if (queens[cell]) {
            queens[cell] = false
            placed--
            return QueensResult.Removed
        }
        queens[cell] = true
        placed++
        return if (isFinished) QueensResult.Solved else QueensResult.Placed
    }

    /** Takes away the queens of the player. The moves stay counted. Returns false if there were none. */
    fun restart(): Boolean {
        if (!state.canRestart) return false
        for (cell in queens.indices) if (cell !in lockedCells) queens[cell] = false
        placed = 0
        return true
    }

    companion object {
        const val DEFAULT_SIZE = 5
        const val MIN_SIZE = 4
        const val MAX_SIZE = 9
    }
}
