package com.mehmtcan.brainscroll.game.blockout

enum class BlockOutStatus { Playing, Won }

/** What a move did. */
enum class BlockOutResult {
    /** Nothing happened: the piece cannot get there (another piece is in the way, or the edge), it is already there, or the game is over. Not counted. */
    Ignored,

    /** A piece slid, by one cell or more: it is one move. */
    Moved,

    /** The target reached the exit. */
    Solved,
}

/** A piece as the UI sees it. [pos] is the column of its left cell (or the row of its top cell); it can slide anywhere from [minPos] to [maxPos]. */
data class BlockPiece(
    val id: Int,
    val horizontal: Boolean,
    /** The row (horizontal piece) or column (upright piece) it lies in. */
    val line: Int,
    val pos: Int,
    val length: Int,
    val isTarget: Boolean,
    val minPos: Int,
    val maxPos: Int,
)

/** The picture of the board for the UI. */
data class BlockOutState(
    val size: Int,
    val pieces: List<BlockPiece>,
    /** Moves so far. A restart does not give them back. */
    val moves: Int,
    /** The fewest moves that solve the board from the start. */
    val par: Int,
    val status: BlockOutStatus,
    /** False while the pieces are where they were at the start. */
    val canRestart: Boolean,
) {
    /** The row of the exit: the middle one, on the right edge. */
    val exitRow: Int get() = size / 2
}

/**
 * The rules of block out, free of UI and time (docs/yeni-oyunlar-plan.md).
 *
 * Pieces slide only along their own length, on a board where none may overlap. The game is won when the target piece reaches the
 * exit on the right edge of the middle row. A slide of one or several cells in one go is one move. There is no losing. A restart
 * puts the pieces back as they were and keeps the moves.
 */
class BlockOutGame(private val puzzle: BlockOutPuzzle, val par: Int) {
    private val pos = puzzle.start.copyOf()
    private var moves = 0

    val isFinished: Boolean get() = BlockOutSolver.isSolved(puzzle.size, puzzle.shapes, pos)

    val state: BlockOutState
        get() {
            val finished = isFinished
            return BlockOutState(
                size = puzzle.size,
                pieces = puzzle.shapes.indices.map { piece ->
                    val shape = puzzle.shapes[piece]
                    val range = BlockOutSolver.range(puzzle.size, puzzle.shapes, pos, piece)
                    BlockPiece(piece, shape.horizontal, shape.line, pos[piece], shape.length, shape.isTarget, range.first, range.last)
                },
                moves = moves,
                par = par,
                status = if (finished) BlockOutStatus.Won else BlockOutStatus.Playing,
                canRestart = !finished && !pos.contentEquals(puzzle.start),
            )
        }

    /** Slides [piece] to the place [to] (the column of its left cell, or the row of its top cell), if nothing is in the way. */
    fun move(piece: Int, to: Int): BlockOutResult {
        if (isFinished || piece !in puzzle.shapes.indices || to == pos[piece]) return BlockOutResult.Ignored
        if (to !in BlockOutSolver.range(puzzle.size, puzzle.shapes, pos, piece)) return BlockOutResult.Ignored
        pos[piece] = to
        moves++
        return if (isFinished) BlockOutResult.Solved else BlockOutResult.Moved
    }

    /** Puts the pieces back as they were at the start. The moves stay counted. Returns false if nothing had moved. */
    fun restart(): Boolean {
        if (!state.canRestart) return false
        puzzle.start.copyInto(pos)
        return true
    }
}
