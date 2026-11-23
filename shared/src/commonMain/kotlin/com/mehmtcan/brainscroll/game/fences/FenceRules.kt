package com.mehmtcan.brainscroll.game.fences

/** The rules of the board, for the lines the player drew: a flag for every edge, true when it holds a line. */
object FenceRules {
    /** How many lines are on the four sides of a square. */
    fun sideLines(puzzle: FencePuzzle, lines: BooleanArray, cell: Int): Int = puzzle.geometry.cellEdges[cell].count { lines[it] }

    /** How many lines end at every dot. */
    fun degrees(puzzle: FencePuzzle, lines: BooleanArray): IntArray {
        val g = puzzle.geometry
        val out = IntArray(g.dots)
        for (e in 0 until g.edges) if (lines[e]) for (d in g.edgeDots[e]) out[d]++
        return out
    }

    /** The dots where three or four lines meet. */
    fun branching(puzzle: FencePuzzle, lines: BooleanArray): Set<Int> {
        val degree = degrees(puzzle, lines)
        return degree.indices.filterTo(HashSet()) { degree[it] > 2 }
    }

    /** The squares that have more lines on their sides than their number. */
    fun overfull(puzzle: FencePuzzle, lines: BooleanArray): Set<Int> =
        puzzle.clues.indices.filterTo(HashSet()) { puzzle.hasClue(it) && sideLines(puzzle, lines, it) > puzzle.clues[it] }

    /** The squares that have exactly as many lines on their sides as their number. */
    fun satisfied(puzzle: FencePuzzle, lines: BooleanArray): Set<Int> =
        puzzle.clues.indices.filterTo(HashSet()) { puzzle.hasClue(it) && sideLines(puzzle, lines, it) == puzzle.clues[it] }

    /** Whether the lines are one closed loop: every dot has none or two, and all the lines are in one piece. */
    fun isLoop(puzzle: FencePuzzle, lines: BooleanArray): Boolean {
        val g = puzzle.geometry
        val degree = degrees(puzzle, lines)
        if (degree.any { it != 0 && it != 2 }) return false
        val start = (0 until g.edges).firstOrNull { lines[it] } ?: return false
        // Walk from the dots of one line over every line they reach; a loop reaches all of them.
        val seen = BooleanArray(g.edges)
        val stack = ArrayDeque<Int>()
        stack.addLast(start)
        seen[start] = true
        while (stack.isNotEmpty()) {
            val e = stack.removeLast()
            for (d in g.edgeDots[e]) for (next in g.dotEdges[d]) if (lines[next] && !seen[next]) {
                seen[next] = true
                stack.addLast(next)
            }
        }
        return (0 until g.edges).all { !lines[it] || seen[it] }
    }

    /** Whether the lines are one closed loop and every number is right: the board is solved. */
    fun isSolved(puzzle: FencePuzzle, lines: BooleanArray): Boolean =
        puzzle.clues.indices.all { !puzzle.hasClue(it) || sideLines(puzzle, lines, it) == puzzle.clues[it] } && isLoop(puzzle, lines)
}
