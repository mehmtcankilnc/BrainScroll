package com.mehmtcan.brainscroll.game.rectangles

/**
 * Solves boards of rectangles by search: every number gets a rectangle that holds it and no other number, as big as the number says;
 * rectangles may not overlap. The numbers add up to the cells, so a board where every number has its rectangle is covered.
 */
object RectSolver {
    /** Every rectangle the number at [cell] could be (the right area, inside the board, holding no other number). */
    fun candidates(puzzle: RectPuzzle, cell: Int): List<Rect> {
        val n = puzzle.size
        val value = puzzle.numbers[cell]
        val column = cell % n
        val row = cell / n
        val out = ArrayList<Rect>()
        for (w in 1..value) {
            if (value % w != 0) continue
            val h = value / w
            if (w > n || h > n) continue
            for (x in maxOf(0, column - w + 1)..minOf(column, n - w)) {
                for (y in maxOf(0, row - h + 1)..minOf(row, n - h)) {
                    val rect = Rect(x, y, w, h)
                    if (rect.cells(n).count { puzzle.numbers[it] > 0 } == 1) out += rect
                }
            }
        }
        return out
    }

    /** Up to [limit] different solutions (each: the rectangle of every number, in the order of [RectPuzzle.clueCells]). */
    fun solve(puzzle: RectPuzzle, limit: Int = 2): List<List<Rect>> {
        val clues = puzzle.clueCells
        val all = clues.map { candidates(puzzle, it) }
        if (all.any { it.isEmpty() }) return emptyList()
        val chosen = arrayOfNulls<Rect>(clues.size)
        val taken = BooleanArray(puzzle.size * puzzle.size)
        val found = ArrayList<List<Rect>>()

        fun free(rect: Rect) = rect.cells(puzzle.size).none { taken[it] }

        fun search(left: Int) {
            if (found.size >= limit) return
            if (left == 0) {
                found += chosen.map { it!! }
                return
            }
            // The number with the fewest places to go next: a dead end shows up at once.
            var best = -1
            var bestOptions: List<Rect> = emptyList()
            for (i in clues.indices) {
                if (chosen[i] != null) continue
                val options = all[i].filter(::free)
                if (best < 0 || options.size < bestOptions.size) {
                    best = i
                    bestOptions = options
                    if (options.size <= 1) break
                }
            }
            for (rect in bestOptions) {
                val cells = rect.cells(puzzle.size)
                cells.forEach { taken[it] = true }
                chosen[best] = rect
                search(left - 1)
                chosen[best] = null
                cells.forEach { taken[it] = false }
                if (found.size >= limit) return
            }
        }
        search(clues.size)
        return found
    }

    /** The one solution of a board that has exactly one, else null. */
    fun uniqueSolution(puzzle: RectPuzzle): List<Rect>? = solve(puzzle, 2).singleOrNull()
}
