package com.mehmtcan.brainscroll.game.untangle

enum class UntangleStatus { Playing, Won }

/** What letting go of a dot did. */
enum class UntangleResult {
    /** The dot was not moved (or the board is won). */
    Ignored,

    /** The dot was moved and some lines still cross. */
    Moved,

    /** The dot was moved and no line crosses another. */
    Solved,
}

/** The picture of the board for the UI. */
data class UntangleState(
    val nodeCount: Int,
    val edges: List<Pair<Int, Int>>,
    /** Where the dots are now: x, y of dot 0, x, y of dot 1, and so on, in units of the board (see [UNTANGLE_BOARD]). */
    val positions: List<Int>,
    /** The lines (their numbers in [edges]) that cross another one right now. */
    val crossing: Set<Int>,
    /** How many pairs of lines cross right now. */
    val crossingPairs: Int,
    /** Dots let go of so far. A restart does not give them back. */
    val moves: Int,
    val status: UntangleStatus,
    /** False while the dots lie as the board was given. */
    val canRestart: Boolean,
)

/**
 * The rules of the tangle, free of UI and time. Dots are dragged ([move] while the finger is down, [drop] when it is lifted); the board
 * is won when a dot is let go of and no line crosses another. A board with no crossings in the middle of a drag is not won yet: the
 * player may still move the dot on. There is no losing; a restart puts the dots back where they were given, and the moves stay counted.
 */
class UntangleGame(private val puzzle: UntanglePuzzle) {
    private var positions: List<Int> = puzzle.start
    private var moves = 0
    private var finished = false

    /** Where the dot being dragged started, so a drag that ends where it began is no move; null between drags. */
    private var dragStart: List<Int>? = null

    val isFinished: Boolean get() = finished

    val state: UntangleState
        get() = UntangleState(
            nodeCount = puzzle.nodeCount,
            edges = puzzle.edges,
            positions = positions,
            crossing = UntangleRules.crossingEdges(positions, puzzle.edges),
            crossingPairs = UntangleRules.crossingPairs(positions, puzzle.edges),
            moves = moves,
            status = if (finished) UntangleStatus.Won else UntangleStatus.Playing,
            canRestart = !finished && positions != puzzle.start,
        )

    /** True while a dot is being dragged (a [move] was made and no [drop] yet). */
    val isDragging: Boolean get() = dragStart != null

    /** Puts dot [node] at ([x], [y]) (kept inside the board). Returns true if the dot is now somewhere else than before. */
    fun move(node: Int, x: Int, y: Int): Boolean {
        if (finished || node !in 0 until puzzle.nodeCount) return false
        val nx = x.coerceIn(UNTANGLE_MARGIN, UNTANGLE_BOARD - UNTANGLE_MARGIN)
        val ny = y.coerceIn(UNTANGLE_MARGIN, UNTANGLE_BOARD - UNTANGLE_MARGIN)
        if (positions[2 * node] == nx && positions[2 * node + 1] == ny) return false
        if (dragStart == null) dragStart = positions
        positions = positions.toMutableList().also {
            it[2 * node] = nx
            it[2 * node + 1] = ny
        }
        return true
    }

    /** The finger is lifted: the drag counts if a dot is somewhere else than where the drag began. */
    fun drop(): UntangleResult {
        val start = dragStart ?: return UntangleResult.Ignored
        dragStart = null
        if (start == positions) return UntangleResult.Ignored
        moves++
        if (UntangleRules.crossingPairs(positions, puzzle.edges) == 0) {
            finished = true
            return UntangleResult.Solved
        }
        return UntangleResult.Moved
    }

    /** Puts the dots back where the board was given. Returns false if they already lie so (or the board is won). */
    fun restart(): Boolean {
        if (finished || positions == puzzle.start) return false
        positions = puzzle.start
        dragStart = null
        return true
    }

    /**
     * Puts back what [state] shows, for a saved game. Returns false if [saved] is not a place of this board's dots (the wrong number,
     * a dot off the board) or has no crossing at all (a board in that state would have been won and not saved): nothing is changed then.
     */
    fun restore(saved: List<Int>, savedMoves: Int): Boolean {
        if (savedMoves < 0 || saved.size != puzzle.nodeCount * 2) return false
        if (saved.any { it !in UNTANGLE_MARGIN..UNTANGLE_BOARD - UNTANGLE_MARGIN }) return false
        if (UntangleRules.crossingPairs(saved, puzzle.edges) == 0) return false
        positions = saved
        moves = savedMoves
        dragStart = null
        return true
    }
}
