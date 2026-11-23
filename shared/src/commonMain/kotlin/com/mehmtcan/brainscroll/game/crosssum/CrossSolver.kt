package com.mehmtcan.brainscroll.game.crosssum

/**
 * What a hint knows about a cell: the [digit] that has to go there, how it follows ([rule], see below), and the cells of the block or
 * blocks that say so ([evidence]).
 *
 * Rules: [LAST] the block has only this cell left, so the sum leaves one digit. [ONE_BLOCK] the sum of one block and the digits already
 * in it leave only this digit for the cell. [TWO_BLOCKS] each block alone allows several digits, the two together only this one.
 * [CHAIN] a longer chain of such steps: the blocks round the cell leave this digit.
 */
class CrossHint(val cell: Int, val digit: Int, val rule: Int, val evidence: List<Int>)

/**
 * The reasoning of a player who works with the sums, free of guessing. It keeps for every cell the digits that can still go there (as
 * bits, bit `d` for the digit d). A block with a sum allows a digit in a cell only if the block can be filled with different digits
 * that add up to the sum, with that digit in that cell and the other cells holding digits they can still take. That is done for every
 * block again and again until nothing changes. A cell with one digit left is as good as filled. A board is finished by this when every
 * cell has one digit left; the generator keeps only such boards, so the answer is always the one this reasoning finds.
 */
object CrossSolver {
    const val LAST = 1
    const val ONE_BLOCK = 0
    const val TWO_BLOCKS = 2
    const val CHAIN = 3

    private const val ALL = 0x3FE

    /**
     * The digits that can still go in each cell (bits), after the reasoning above has gone as far as it can, starting from the digits in
     * [start] (0 for a cell that is empty). Null if the digits in [start] cannot be right (some block has no way left).
     */
    fun candidates(puzzle: CrossPuzzle, start: IntArray): IntArray? {
        val cand = IntArray(puzzle.white.size) { if (puzzle.white[it]) (if (start[it] > 0) 1 shl start[it] else ALL) else 0 }
        val runs = puzzle.runs
        var changed = true
        while (changed) {
            changed = false
            for (run in runs) {
                val possible = blockPossible(run, cand) ?: return null
                for (i in run.cells.indices) {
                    val cell = run.cells[i]
                    val narrowed = cand[cell] and possible[i]
                    if (narrowed == 0) return null
                    if (narrowed != cand[cell]) {
                        cand[cell] = narrowed
                        changed = true
                    }
                }
            }
        }
        return cand
    }

    /** The digits of the answer that this reasoning finds from the sums and the given digits, or null if it cannot finish. */
    fun solve(puzzle: CrossPuzzle): IntArray? {
        val start = IntArray(puzzle.white.size) { if (puzzle.given[it]) puzzle.solution[it] else 0 }
        val cand = candidates(puzzle, start) ?: return null
        val out = IntArray(cand.size)
        for (cell in puzzle.whiteCells) {
            val bits = cand[cell]
            if (bits and (bits - 1) != 0) return null
            out[cell] = Integer.numberOfTrailingZeros(bits)
        }
        return out
    }

    /**
     * The next digit a player can put with certainty, given the digits on the board now ([values], 0 for empty; they have to be right).
     * It takes the easiest way: the last cell of a block, then a cell one block decides, then one two blocks decide, then any cell the
     * longer reasoning decides. Null when nothing is left to put or the digits on the board cannot be right.
     */
    fun hint(puzzle: CrossPuzzle, values: IntArray): CrossHint? {
        val empty = puzzle.whiteCells.filter { values[it] == 0 }
        if (empty.isEmpty()) return null
        val all = IntArray(values.size) { if (puzzle.white[it]) (if (values[it] > 0) 1 shl values[it] else ALL) else 0 }
        // What each block alone allows in each of its cells.
        val alone = HashMap<CrossRun, IntArray>()
        for (run in puzzle.runs) alone[run] = blockPossible(run, all) ?: return null
        fun bitsOf(run: CrossRun, cell: Int) = alone.getValue(run)[run.cells.indexOf(cell)] and all[cell]
        fun single(bits: Int) = bits != 0 && bits and (bits - 1) == 0
        val digitOf = { bits: Int -> Integer.numberOfTrailingZeros(bits) }

        // 1. The last cell of a block.
        for (cell in empty) for (run in puzzle.runsOf(cell)) {
            if (run.cells.count { values[it] == 0 } == 1) return CrossHint(cell, digitOf(bitsOf(run, cell)), LAST, run.cells.filter { it != cell })
        }
        // 2. One block alone leaves one digit (the block with the fewest empty cells first).
        var best: CrossHint? = null
        var bestEmpty = Int.MAX_VALUE
        for (cell in empty) for (run in puzzle.runsOf(cell)) {
            val bits = bitsOf(run, cell)
            val open = run.cells.count { values[it] == 0 }
            if (single(bits) && open < bestEmpty) {
                best = CrossHint(cell, digitOf(bits), ONE_BLOCK, run.cells.filter { it != cell })
                bestEmpty = open
            }
        }
        if (best != null) return best
        // 3. Each block allows several digits, the two together one.
        for (cell in empty) {
            val runs = puzzle.runsOf(cell)
            val bits = runs.fold(ALL) { acc, run -> acc and bitsOf(run, cell) }
            if (single(bits)) return CrossHint(cell, digitOf(bits), TWO_BLOCKS, runs.flatMap { run -> run.cells.filter { it != cell } }.distinct())
        }
        // 4. A longer chain.
        val cand = candidates(puzzle, values) ?: return null
        for (cell in empty) {
            if (single(cand[cell])) return CrossHint(cell, digitOf(cand[cell]), CHAIN, puzzle.runsOf(cell).flatMap { run -> run.cells.filter { it != cell } }.distinct())
        }
        return null
    }

    /**
     * For each cell of [run] (in order), the digits that some filling of the whole block with different digits adding up to its sum
     * allows there, each cell taking a digit it can still take (bits of [cand]). Null if there is no such filling.
     */
    private fun blockPossible(run: CrossRun, cand: IntArray): IntArray? {
        val k = run.cells.size
        val possible = IntArray(k)
        val masks = IntArray(k) { cand[run.cells[it]] }
        fun fill(i: Int, used: Int, left: Int, chosen: IntArray) {
            if (i == k) {
                if (left == 0) for (j in 0 until k) possible[j] = possible[j] or (1 shl chosen[j])
                return
            }
            val remaining = k - i - 1
            var bits = masks[i] and used.inv()
            while (bits != 0) {
                val d = Integer.numberOfTrailingZeros(bits)
                bits = bits and (bits - 1)
                val rest = left - d
                if (rest < 0) break
                // The cells still to fill need at least 1 + 2 + ... and at most 9 + 8 + ... between them.
                if (remaining == 0) {
                    if (rest != 0) continue
                } else {
                    if (rest < remaining * (remaining + 1) / 2 || rest > remaining * (19 - remaining) / 2) continue
                }
                chosen[i] = d
                fill(i + 1, used or (1 shl d), rest, chosen)
            }
        }
        fill(0, 0, run.sum, IntArray(k))
        return if (possible.any { it == 0 }) null else possible
    }
}
