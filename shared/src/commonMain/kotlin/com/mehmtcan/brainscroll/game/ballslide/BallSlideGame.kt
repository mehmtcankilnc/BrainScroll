package com.mehmtcan.brainscroll.game.ballslide

enum class BallSlideStatus { Playing, Won }

/** What a slide did. */
enum class BallSlideResult {
    /** Nothing happened: a wall or the edge is right in front of the ball, or the game is over. Not counted. */
    Ignored,

    /** The ball slid and painted. */
    Moved,

    /** The slide painted the last cell. */
    Solved,
}

/** The picture of the board for the UI. Cells are numbered row by row from the top left. */
data class BallSlideState(
    val size: Int,
    val walls: List<Boolean>,
    val painted: List<Boolean>,
    /** The cell the ball is on. */
    val ball: Int,
    /** The cells the last slide passed (the one the ball stopped on included); empty before the first slide and after an undo or a restart. */
    val lastPath: List<Int>,
    /**
     * The cells of the slide that the last undo took back (the first one passed first, the one the ball had stopped on last), and the
     * cell the ball goes back to; empty unless the last action was an undo. [undos] counts the undos so far, so a screen can tell a
     * new one from the old one.
     */
    val undone: List<Int>,
    val undoneStart: Int,
    val undos: Int,
    /** Slides so far. An undo or a restart does not give them back. */
    val moves: Int,
    /** The fewest slides that win the board from its start. */
    val par: Int,
    /** How many cells there are to paint, and how many are painted. */
    val openCount: Int,
    val paintedCount: Int,
    val status: BallSlideStatus,
    val canUndo: Boolean,
    /** False while the board is as it was at the start. */
    val canRestart: Boolean,
)

/**
 * The rules of ball slide, free of UI and time.
 *
 * A slide sends the ball one way until a wall or the edge stops it, and paints every cell on the way. The board is won when every
 * cell that is not a wall is painted. There is no losing: an undo takes the last slide back, a restart puts the ball on its start
 * with only that cell painted; neither gives the slides back.
 */
class BallSlideGame(private val puzzle: BallSlidePuzzle, val par: Int = BallSlideSolver.par(puzzle) ?: 0) {
    private class Step(val ball: Int, val painted: BooleanArray, val path: List<Int>)

    private var ball = puzzle.start
    private val painted = BooleanArray(puzzle.cellCount).also { it[puzzle.start] = true }
    private val history = ArrayList<Step>()
    private var lastPath: List<Int> = emptyList()
    private var undone: List<Int> = emptyList()
    private var undoneStart = puzzle.start
    private var undos = 0
    private var moves = 0

    val isFinished: Boolean get() = painted.indices.all { puzzle.walls[it] || painted[it] }

    val state: BallSlideState
        get() = BallSlideState(
            size = puzzle.size,
            walls = puzzle.walls,
            painted = painted.toList(),
            ball = ball,
            lastPath = lastPath,
            undone = undone,
            undoneStart = undoneStart,
            undos = undos,
            moves = moves,
            par = par,
            openCount = puzzle.openCount,
            paintedCount = painted.count { it },
            status = if (isFinished) BallSlideStatus.Won else BallSlideStatus.Playing,
            canUndo = !isFinished && history.isNotEmpty(),
            canRestart = !isFinished && history.isNotEmpty(),
        )

    fun slide(dir: Int): BallSlideResult {
        if (isFinished || dir !in 0..3) return BallSlideResult.Ignored
        val path = puzzle.slide(ball, dir)
        if (path.isEmpty()) return BallSlideResult.Ignored
        history += Step(ball, painted.copyOf(), path)
        path.forEach { painted[it] = true }
        ball = path.last()
        lastPath = path
        undone = emptyList()
        moves++
        return if (isFinished) BallSlideResult.Solved else BallSlideResult.Moved
    }

    /** Takes the last slide back. Returns false if there is none. */
    fun undo(): Boolean {
        if (isFinished || history.isEmpty()) return false
        val step = history.removeAt(history.lastIndex)
        ball = step.ball
        step.painted.copyInto(painted)
        lastPath = emptyList()
        undone = step.path
        undoneStart = step.ball
        undos++
        return true
    }

    /** Puts the ball on its start with only that cell painted. Returns false if nothing was done yet. */
    fun restart(): Boolean {
        if (isFinished || history.isEmpty()) return false
        history.clear()
        painted.fill(false)
        painted[puzzle.start] = true
        ball = puzzle.start
        lastPath = emptyList()
        undone = emptyList()
        return true
    }
}
