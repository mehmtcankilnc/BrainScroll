package com.mehmtcan.brainscroll.game.colorflow

/** What a search found: the ways it found (a path of cells for each color, from the first dot to the second), and whether it ran to the end. */
class FlowSearch(val solutions: List<List<List<Int>>>, val complete: Boolean)

/**
 * Solving color flow by search: the colors one after the other (the one whose dots are closest first), each color tries every
 * path from its first dot to its second over the free cells. After every step a cell that no path could still pass through (it
 * has fewer than two neighbors that are free or the end of a path still to be drawn) cuts that branch off. A search stops at
 * [nodeLimit] steps; then [FlowSearch.complete] is false, and what it did not find it did not rule out.
 */
object FlowSolver {
    const val DEFAULT_NODE_LIMIT = 150_000

    fun search(puzzle: FlowPuzzle, limit: Int, nodeLimit: Int = DEFAULT_NODE_LIMIT): FlowSearch {
        val n = puzzle.size
        val cells = n * n
        val order = puzzle.pairs.indices.sortedBy { c ->
            val (a, b) = puzzle.pairs[c]
            kotlin.math.abs(a / n - b / n) + kotlin.math.abs(a % n - b % n)
        }
        // A cell is taken when a path passes it; a dot is taken from the start (only its own path may step onto it).
        val taken = BooleanArray(cells)
        // True for a taken cell that a path can still link to: the head of the path being drawn, its goal, and the dots of the colors
        // that are not drawn yet. A cell next to such a cell has a neighbor to be joined to.
        val open = BooleanArray(cells)
        for ((a, b) in puzzle.pairs) {
            taken[a] = true
            taken[b] = true
            open[a] = true
            open[b] = true
        }
        val neighbors = Array(cells) { cell ->
            val r = cell / n
            val c = cell % n
            listOfNotNull(
                if (r > 0) cell - n else null,
                if (r < n - 1) cell + n else null,
                if (c > 0) cell - 1 else null,
                if (c < n - 1) cell + 1 else null,
            ).toIntArray()
        }
        val paths = Array(puzzle.colorCount) { ArrayList<Int>() }
        val found = ArrayList<List<List<Int>>>()
        var nodes = 0
        var ranOut = false

        /** True if some free cell can no longer be covered. */
        fun deadCell(): Boolean {
            for (cell in 0 until cells) {
                if (taken[cell]) continue
                var room = 0
                for (nb in neighbors[cell]) if (!taken[nb] || open[nb]) room++
                if (room < 2) return true
            }
            return false
        }

        fun stop() = found.size >= limit || ranOut

        fun solveColor(index: Int) {
            if (stop()) return
            if (index == order.size) {
                if ((0 until cells).all { taken[it] }) found += paths.map { it.toList() }
                return
            }
            val color = order[index]
            val (start, goal) = puzzle.pairs[color]
            val path = paths[color]
            path.clear()
            path += start

            fun extend() {
                if (++nodes > nodeLimit) {
                    ranOut = true
                    return
                }
                val head = path.last()
                for (nb in neighbors[head]) {
                    if (nb == goal) {
                        // The path is done: its last cell and the goal are linked, nothing more can join them.
                        path += nb
                        open[head] = false
                        open[goal] = false
                        if (!deadCell()) solveColor(index + 1)
                        open[head] = true
                        open[goal] = true
                        path.removeAt(path.lastIndex)
                    } else if (!taken[nb]) {
                        taken[nb] = true
                        path += nb
                        open[head] = false
                        open[nb] = true
                        if (!deadCell()) extend()
                        open[nb] = false
                        open[head] = true
                        path.removeAt(path.lastIndex)
                        taken[nb] = false
                    }
                    if (stop()) return
                }
            }
            extend()
            path.clear()
        }

        solveColor(0)
        return FlowSearch(found, !ranOut)
    }

    /** A way to join every color and cover the board, or null if there is none (or the search gave up). */
    fun solve(puzzle: FlowPuzzle, nodeLimit: Int = DEFAULT_NODE_LIMIT): List<List<Int>>? = search(puzzle, 1, nodeLimit).solutions.firstOrNull()
}
