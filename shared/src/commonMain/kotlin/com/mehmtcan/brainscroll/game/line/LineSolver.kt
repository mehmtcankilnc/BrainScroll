package com.mehmtcan.brainscroll.game.line

import kotlin.math.abs

/**
 * Finds lines for the one-line game: a depth-first search from number 1 that moves one cell at a time (no diagonals),
 * visits numbers in order, and may only enter the last number when it is the last free cell.
 *
 * The search cuts a branch early when it can no longer work: the free cells are not all reachable from the head, a free
 * cell has fewer than two ways in and out (only the last cell of the line may), the numbers are too far apart for the
 * cells that are left, or the parity of the grid does not fit.
 */
object LineSolver {

    /** How many lines solve the board, counting no further than [limit]. */
    fun count(size: Int, checkpoints: IntArray, limit: Int = 2): Int = Search(size, checkpoints, limit).also { it.run(IntArray(0)) }.found

    /**
     * The first line that solves the board and starts with [prefix] (cells already drawn, beginning on number 1), or null
     * when the prefix breaks a rule or leads nowhere.
     */
    fun solve(size: Int, checkpoints: IntArray, prefix: IntArray = IntArray(0)): IntArray? =
        Search(size, checkpoints, 1).also { it.run(prefix) }.first

    private class Search(val size: Int, val checkpoints: IntArray, val limit: Int) {
        private val cells = size * size
        private val lastNumber = checkpoints.size
        private val finalCell = checkpoints.last()
        private val number = IntArray(cells).also { numbers -> checkpoints.forEachIndexed { index, cell -> numbers[cell] = index + 1 } }
        private val neighbors: Array<IntArray> = Array(cells) { cell ->
            val row = cell / size
            val column = cell % size
            // Not buildList: inside its block `size` would be the size of the list being built.
            val result = ArrayList<Int>(4)
            if (row > 0) result.add(cell - size)
            if (column > 0) result.add(cell - 1)
            if (column < size - 1) result.add(cell + 1)
            if (row < size - 1) result.add(cell + size)
            result.toIntArray()
        }
        private val visited = BooleanArray(cells)
        private val path = IntArray(cells)
        private val stack = IntArray(cells)
        private val stamp = IntArray(cells)
        private var stampValue = 0

        var found = 0
            private set
        var first: IntArray? = null
            private set

        fun run(prefix: IntArray) {
            var depth = 0
            var reached = 0
            for (cell in prefix) {
                val valid = if (depth == 0) cell == checkpoints[0] else
                    cell in 0 until cells && !visited[cell] && neighbors[path[depth - 1]].contains(cell) && canEnter(cell, depth, reached)
                if (!valid) return
                visited[cell] = true
                path[depth++] = cell
                if (number[cell] > 0) reached++
            }
            if (depth == 0) {
                visited[checkpoints[0]] = true
                path[0] = checkpoints[0]
                depth = 1
                reached = 1
            }
            walk(path[depth - 1], depth, reached)
        }

        private fun canEnter(cell: Int, depth: Int, reached: Int): Boolean {
            val n = number[cell]
            return n == 0 || (n == reached + 1 && (n < lastNumber || depth == cells - 1))
        }

        private fun walk(head: Int, depth: Int, reached: Int) {
            if (depth == cells) {
                found++
                if (first == null) first = path.copyOf()
                return
            }
            if (!feasible(head, depth, reached)) return
            for (next in neighbors[head]) {
                if (visited[next] || !canEnter(next, depth, reached)) continue
                visited[next] = true
                path[depth] = next
                walk(next, depth + 1, if (number[next] > 0) reached + 1 else reached)
                visited[next] = false
                if (found >= limit) return
            }
        }

        private fun distance(a: Int, b: Int) = abs(a / size - b / size) + abs(a % size - b % size)

        private fun feasible(head: Int, depth: Int, reached: Int): Boolean {
            val free = cells - depth
            // The numbers still to come, in order, need at least this many steps; the rest of the line has `free` steps.
            var steps = 0
            var from = head
            for (index in reached until lastNumber) {
                steps += distance(from, checkpoints[index])
                from = checkpoints[index]
            }
            if (steps > free) return false
            if ((distance(head, finalCell) - free) % 2 != 0) return false

            // Every free cell must be reachable, and at most one (the end of the line) may have a single way in and out.
            stampValue++
            var top = 0
            var reachable = 0
            var ends = 0
            for (start in neighbors[head]) {
                if (!visited[start] && stamp[start] != stampValue) {
                    stamp[start] = stampValue
                    stack[top++] = start
                }
            }
            while (top > 0) {
                val cell = stack[--top]
                reachable++
                var ways = 0
                for (next in neighbors[cell]) {
                    if (!visited[next]) {
                        ways++
                        if (stamp[next] != stampValue) {
                            stamp[next] = stampValue
                            stack[top++] = next
                        }
                    } else if (next == head) {
                        ways++
                    }
                }
                if (ways <= 1) {
                    if (ways == 0 || cell != finalCell || ++ends > 1) return false
                }
            }
            return reachable == free
        }
    }
}
