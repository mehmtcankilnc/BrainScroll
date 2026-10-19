package com.mehmtcan.brainscroll.game.colorflow

enum class FlowStatus { Playing, Won }

/** What a stroke (a finger put down on the board, moved and lifted) did. */
enum class FlowResult {
    /** Nothing changed (the finger started on an empty cell, or only touched a dot that had no path). */
    Ignored,

    /** The paths changed. */
    Moved,

    /** The stroke finished the board. */
    Solved,
}

/** The picture of the board for the UI. Cells are numbered row by row from the top left. */
data class FlowState(
    val size: Int,
    /** The two cells of the dots of each color. */
    val pairs: List<Pair<Int, Int>>,
    /** The path of each color from the dot it began at, as far as it goes now (empty when there is none). */
    val paths: List<List<Int>>,
    /** True for a color whose path joins its two dots. */
    val joined: List<Boolean>,
    /** The color being drawn now, or -1. */
    val active: Int,
    /** How many cells hold a path, and how many cells there are. */
    val covered: Int,
    val cellCount: Int,
    /** Strokes that changed something so far. A restart does not give them back. */
    val moves: Int,
    val status: FlowStatus,
    /** False while there are no paths. */
    val canRestart: Boolean,
)

/**
 * The rules of color flow, free of UI and time.
 *
 * A stroke starts on a dot (its path begins again there) or on a cell of a path (the path goes on from that cell) and moves from cell
 * to cell. A step onto the cell it came from takes the step back; onto a cell of its own path shortens the path back to that cell;
 * onto a cell of another color's path cuts that path there; onto the dot of another color is not allowed. A path that reaches the
 * other dot of its color is joined and does not go on. The board is won when every color is joined and every cell holds a path.
 * A tap on a dot clears its path. There is no losing; a restart clears every path, and the strokes stay counted.
 */
class FlowGame(private val puzzle: FlowPuzzle) {
    private val n = puzzle.size
    private val paths = Array(puzzle.colorCount) { ArrayList<Int>() }

    /** The color whose path holds the cell, or -1. */
    private val owner = IntArray(n * n) { -1 }
    private var active = -1
    private var moves = 0
    private var strokeChanged = false

    private fun joined(color: Int) = paths[color].size >= 2 && paths[color].last() == puzzle.pairs[color].let { (a, b) -> if (paths[color].first() == a) b else a }

    val isFinished: Boolean get() = (0 until puzzle.colorCount).all { joined(it) } && owner.all { it >= 0 }

    val state: FlowState
        get() = FlowState(
            size = n,
            pairs = puzzle.pairs,
            paths = paths.map { it.toList() },
            joined = List(puzzle.colorCount) { joined(it) },
            active = active,
            covered = owner.count { it >= 0 },
            cellCount = n * n,
            moves = moves,
            status = if (isFinished) FlowStatus.Won else FlowStatus.Playing,
            canRestart = !isFinished && paths.any { it.isNotEmpty() },
        )

    /** Removes the cells of path [color] from index [from] on. A path left with only its dot is removed altogether. */
    private fun cut(color: Int, from: Int) {
        val path = paths[color]
        while (path.size > from) owner[path.removeAt(path.lastIndex)] = -1
        if (path.size == 1) owner[path.removeAt(0)] = -1
    }

    /** A stroke begins on [cell]. Returns false if it cannot begin there (an empty cell, a finished board). */
    fun start(cell: Int): Boolean {
        if (isFinished || cell !in owner.indices) return false
        strokeChanged = false
        val dot = puzzle.colorAt(cell)
        if (dot >= 0) {
            if (paths[dot].isNotEmpty()) {
                cut(dot, 0)
                strokeChanged = true
            }
            paths[dot] += cell
            owner[cell] = dot
            active = dot
            return true
        }
        val color = owner[cell]
        if (color < 0) return false
        // The path goes on from this cell: what came after it is taken away.
        val index = paths[color].indexOf(cell)
        if (index < paths[color].size - 1) {
            cut(color, index + 1)
            strokeChanged = true
        }
        active = color
        return true
    }

    /** The finger moves onto the cell [cell], next to the end of the path being drawn. Returns true if the board changed. */
    fun extend(cell: Int): Boolean {
        val color = active
        if (color < 0 || cell !in owner.indices) return false
        val path = paths[color]
        if (path.isEmpty() || joined(color)) return false
        val head = path.last()
        if (kotlin.math.abs(head / n - cell / n) + kotlin.math.abs(head % n - cell % n) != 1) return false
        // A step back onto the cell the path came from takes the step back.
        if (path.size >= 2 && path[path.size - 2] == cell) {
            owner[path.removeAt(path.lastIndex)] = -1
            strokeChanged = true
            return true
        }
        val dot = puzzle.colorAt(cell)
        if (dot >= 0 && dot != color) return false
        if (dot == color) {
            if (cell == path.first()) return false
            path += cell
            owner[cell] = color
            strokeChanged = true
            return true
        }
        val other = owner[cell]
        if (other == color) {
            // Onto its own path: the path goes back to that cell, which is its end again.
            cut(color, path.indexOf(cell) + 1)
            strokeChanged = true
            return true
        }
        // Onto the path of another color: that path is cut off at this cell, which this path takes.
        if (other >= 0) cut(other, paths[other].indexOf(cell))
        path += cell
        owner[cell] = color
        strokeChanged = true
        return true
    }

    /** The finger is lifted. A path of one cell (a tap on a dot) is cleared. Returns what the stroke did. */
    fun end(): FlowResult {
        val color = active
        active = -1
        if (color >= 0 && paths[color].size == 1) cut(color, 0)
        if (!strokeChanged) return FlowResult.Ignored
        strokeChanged = false
        moves++
        return if (isFinished) FlowResult.Solved else FlowResult.Moved
    }

    /** Clears every path. The strokes stay counted. Returns false if there was no path. */
    fun restart(): Boolean {
        if (isFinished || paths.all { it.isEmpty() }) return false
        paths.forEach { it.clear() }
        owner.fill(-1)
        active = -1
        strokeChanged = false
        return true
    }

    /**
     * Puts back what [state] shows (the paths and the strokes so far), for a saved game. Returns false if the paths are not ones this
     * board can hold: nothing is changed then.
     */
    fun restore(savedPaths: List<List<Int>>, savedMoves: Int): Boolean {
        if (savedPaths.size != puzzle.colorCount || savedMoves < 0) return false
        val used = BooleanArray(n * n)
        for ((color, path) in savedPaths.withIndex()) {
            if (path.isEmpty()) continue
            val (a, b) = puzzle.pairs[color]
            if (path.size < 2 || (path.first() != a && path.first() != b)) return false
            for (i in path.indices) {
                val cell = path[i]
                if (cell !in used.indices || used[cell]) return false
                used[cell] = true
                if (i > 0 && kotlin.math.abs(path[i - 1] / n - cell / n) + kotlin.math.abs(path[i - 1] % n - cell % n) != 1) return false
                val dot = puzzle.colorAt(cell)
                // A path only meets dots of its own color, and its other dot only at its very end.
                if (dot >= 0 && dot != color) return false
                if (dot == color && i != 0 && i != path.lastIndex) return false
            }
        }
        paths.forEach { it.clear() }
        owner.fill(-1)
        for ((color, path) in savedPaths.withIndex()) {
            paths[color].addAll(path)
            path.forEach { owner[it] = color }
        }
        moves = savedMoves
        active = -1
        strokeChanged = false
        return true
    }
}
