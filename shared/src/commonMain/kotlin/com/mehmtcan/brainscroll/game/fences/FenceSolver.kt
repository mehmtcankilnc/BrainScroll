package com.mehmtcan.brainscroll.game.fences

/**
 * Finds the loops of a board. The state of a search is a mark for every edge: [UNKNOWN], [LINE] or [CROSS] (no line here). The rules
 * that fill the marks are the ones a player uses: a square with all its number of lines gets crosses on its other sides, a square that
 * needs all its free sides gets lines on them, a dot with two lines gets crosses on its other edges, a dot with one line and one free
 * edge gets a line there, a dot that no line can leave (one free edge and no line) gets a cross, and an edge that would close a loop
 * before the loop is the whole answer gets a cross. On the harder levels a "try and see" step is allowed too: a mark that makes the
 * rules break is the other one.
 */
object FenceSolver {
    const val UNKNOWN = 0
    const val LINE = 1
    const val CROSS = 2

    /** Prunes [marks] by the rules, again and again, until nothing changes. Returns false if the marks break a rule (so no loop is left). */
    fun propagate(puzzle: FencePuzzle, marks: IntArray): Boolean {
        val g = puzzle.geometry
        val parent = IntArray(g.dots)
        val perRoot = IntArray(g.dots)
        while (true) {
            var changed = false
            for (cell in puzzle.clues.indices) {
                val clue = puzzle.clues[cell]
                if (clue < 0) continue
                var lines = 0
                var unknown = 0
                for (e in g.cellEdges[cell]) when (marks[e]) {
                    LINE -> lines++
                    UNKNOWN -> unknown++
                }
                if (lines > clue || lines + unknown < clue) return false
                if (unknown == 0) continue
                if (lines == clue) {
                    for (e in g.cellEdges[cell]) if (marks[e] == UNKNOWN) marks[e] = CROSS
                    changed = true
                } else if (lines + unknown == clue) {
                    for (e in g.cellEdges[cell]) if (marks[e] == UNKNOWN) marks[e] = LINE
                    changed = true
                }
            }
            for (dot in 0 until g.dots) {
                var lines = 0
                var unknown = 0
                for (e in g.dotEdges[dot]) when (marks[e]) {
                    LINE -> lines++
                    UNKNOWN -> unknown++
                }
                if (lines > 2 || (lines == 1 && unknown == 0)) return false
                if (unknown == 0) continue
                if (lines == 2) {
                    for (e in g.dotEdges[dot]) if (marks[e] == UNKNOWN) marks[e] = CROSS
                    changed = true
                } else if (lines == 1 && unknown == 1) {
                    for (e in g.dotEdges[dot]) if (marks[e] == UNKNOWN) marks[e] = LINE
                    changed = true
                } else if (lines == 0 && unknown == 1) {
                    for (e in g.dotEdges[dot]) if (marks[e] == UNKNOWN) marks[e] = CROSS
                    changed = true
                }
            }
            // The lines so far: which dots they join, and whether they already close a loop.
            for (d in parent.indices) parent[d] = d
            fun find(d: Int): Int {
                var x = d
                while (parent[x] != x) {
                    parent[x] = parent[parent[x]]
                    x = parent[x]
                }
                return x
            }
            var cycles = 0
            var total = 0
            for (e in 0 until g.edges) if (marks[e] == LINE) {
                total++
                val a = find(g.edgeDots[e][0])
                val b = find(g.edgeDots[e][1])
                if (a == b) cycles++ else parent[a] = b
            }
            if (total > 0) {
                perRoot.fill(0)
                for (e in 0 until g.edges) if (marks[e] == LINE) perRoot[find(g.edgeDots[e][0])]++
            }
            if (cycles > 0) {
                // A loop is only right if it is all the lines there are and every number is met: then nothing else is a line.
                if (cycles > 1) return false
                val root = find(g.edgeDots[(0 until g.edges).first { marks[it] == LINE }][0])
                if (perRoot[root] != total) return false
                if (!cluesMet(puzzle, marks, -1)) return false
                for (e in 0 until g.edges) if (marks[e] == UNKNOWN) {
                    marks[e] = CROSS
                    changed = true
                }
            } else if (total > 0) {
                for (e in 0 until g.edges) {
                    if (marks[e] != UNKNOWN) continue
                    val a = find(g.edgeDots[e][0])
                    if (a != find(g.edgeDots[e][1])) continue
                    // This edge would close a loop: only fine if the path it closes holds all the lines there are and the loop meets every number.
                    if (!(perRoot[a] == total && cluesMet(puzzle, marks, e))) {
                        marks[e] = CROSS
                        changed = true
                    }
                }
            }
            if (!changed) return true
        }
    }

    /** Whether every number is met by the lines, with [extra] (an edge number, or -1) counted as a line too. */
    private fun cluesMet(puzzle: FencePuzzle, marks: IntArray, extra: Int): Boolean {
        val g = puzzle.geometry
        for (cell in puzzle.clues.indices) {
            val clue = puzzle.clues[cell]
            if (clue < 0) continue
            var lines = 0
            for (e in g.cellEdges[cell]) if (marks[e] == LINE || e == extra) lines++
            if (lines != clue) return false
        }
        return true
    }

    /**
     * Prunes [marks] as far as the rules (and, if [lookahead] is on, trying a mark on an edge) can. Returns false if the marks break a
     * rule. It does not say whether the board is finished: look at the marks.
     */
    fun refine(puzzle: FencePuzzle, marks: IntArray, lookahead: Boolean): Boolean {
        if (!propagate(puzzle, marks)) return false
        if (!lookahead) return true
        var progress = true
        while (progress) {
            progress = false
            for (e in marks.indices) {
                if (marks[e] != UNKNOWN) continue
                for (v in LINE..CROSS) {
                    val trial = marks.copyOf()
                    trial[e] = v
                    if (!propagate(puzzle, trial)) {
                        marks[e] = if (v == LINE) CROSS else LINE
                        if (!propagate(puzzle, marks)) return false
                        progress = true
                        break
                    }
                }
            }
        }
        return true
    }

    /** The marks of the finished board (every edge a line or a cross) if the rules (and the try and see) finish it, else null. */
    fun solveByLogic(puzzle: FencePuzzle, lookahead: Boolean): IntArray? {
        val marks = IntArray(puzzle.geometry.edges)
        if (!refine(puzzle, marks, lookahead)) return null
        if (marks.any { it == UNKNOWN }) return null
        return marks.takeIf { FenceRules.isSolved(puzzle, BooleanArray(it.size) { e -> it[e] == LINE }) }
    }

    /** Up to [limit] loops that fit the board, as flags for the edges that are lines. */
    fun solutions(puzzle: FencePuzzle, limit: Int): List<BooleanArray> {
        val found = ArrayList<BooleanArray>()
        fun rec(marks: IntArray) {
            if (found.size >= limit) return
            if (!propagate(puzzle, marks)) return
            val open = marks.indexOfFirst { it == UNKNOWN }
            if (open < 0) {
                val lines = BooleanArray(marks.size) { marks[it] == LINE }
                if (FenceRules.isSolved(puzzle, lines)) found += lines
                return
            }
            for (v in LINE..CROSS) {
                val next = marks.copyOf()
                next[open] = v
                rec(next)
            }
        }
        rec(IntArray(puzzle.geometry.edges))
        return found
    }

    /** The loop of the board as flags for the edges, if it has exactly one. */
    fun uniqueSolution(puzzle: FencePuzzle): BooleanArray? = solutions(puzzle, 2).singleOrNull()
}
