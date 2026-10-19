package com.mehmtcan.brainscroll.game.colorflow

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/** A board of color flow with a way to win it (a path for each color, from its first dot to its second). */
class FlowBoard(val puzzle: FlowPuzzle, val solution: List<List<Int>>)

/**
 * Makes the boards of color flow: the square is cut into winding paths with no cell left over
 * (each path starts at the cell with the fewest free neighbors and grows by random steps), and the two ends of every path become
 * the dots of a color. The cutting is a way to win, so every board can be won. The search then counts the ways: a board with
 * exactly one is kept; when none of the cuttings tried has only one (or the search is too slow to tell), the first one is
 * kept, since it can be won all the same. The board only depends on the seed, but it is saved as the board itself, so a later change
 * of this does not change a saved game.
 */
object FlowGenerator {
    private const val MAX_CUTS = 400
    private const val UNIQUE_TRIES = 300
    private const val SEARCH_NODES = 60_000

    fun generate(seed: Int, difficulty: Difficulty): FlowBoard = generate(seed, GameLevels.colorFlow(difficulty))

    fun generate(seed: Int, shape: GameLevels.FlowShape): FlowBoard {
        val random = Random(seed)
        var first: FlowBoard? = null
        var tries = 0
        repeat(MAX_CUTS) {
            val cut = cut(random, shape) ?: return@repeat
            val puzzle = FlowPuzzle(shape.size, cut.map { it.first() to it.last() })
            val board = FlowBoard(puzzle, cut)
            if (first == null) first = board
            val search = FlowSolver.search(puzzle, limit = 2, nodeLimit = SEARCH_NODES)
            if (search.complete && search.solutions.size == 1) return board
            if (++tries >= UNIQUE_TRIES) return first!!
        }
        return first ?: error("no board found")
    }

    /** The square cut into paths, or null when the cutting got stuck (a cell with no way on) or made too few or too many colors. */
    private fun cut(random: Random, shape: GameLevels.FlowShape): List<List<Int>>? {
        val n = shape.size
        val cells = n * n
        val free = BooleanArray(cells) { true }
        val neighbors = Array(cells) { cell ->
            val r = cell / n
            val c = cell % n
            listOfNotNull(
                if (r > 0) cell - n else null,
                if (r < n - 1) cell + n else null,
                if (c > 0) cell - 1 else null,
                if (c < n - 1) cell + 1 else null,
            )
        }
        fun freeNeighbors(cell: Int) = neighbors[cell].count { free[it] }
        val minLength = 3
        val maxLength = cells / shape.minColors + 3
        val paths = ArrayList<List<Int>>()
        var left = cells
        while (left > 0) {
            // Start where it is hardest to fit a path in: the free cell with the fewest free neighbors.
            val candidates = (0 until cells).filter { free[it] }
            val fewest = candidates.minOf { freeNeighbors(it) }
            val start = candidates.filter { freeNeighbors(it) == fewest }.random(random)
            val wanted = minLength + random.nextInt(maxLength - minLength + 1)
            val path = arrayListOf(start)
            free[start] = false
            left--
            while (path.size < wanted) {
                val options = neighbors[path.last()].filter { free[it] }
                if (options.isEmpty()) break
                // Mostly the neighbor that has the fewest free neighbors (so no cell is left alone), sometimes any.
                val next = if (random.nextInt(10) < 7) {
                    val least = options.minOf { freeNeighbors(it) }
                    options.filter { freeNeighbors(it) == least }.random(random)
                } else {
                    options.random(random)
                }
                free[next] = false
                left--
                path += next
            }
            if (path.size < minLength) return null
            paths += path
            if (paths.size > shape.maxColors) return null
        }
        return paths.takeIf { it.size in shape.minColors..shape.maxColors }
    }
}
