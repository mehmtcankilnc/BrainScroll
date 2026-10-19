package com.mehmtcan.brainscroll.game.arrows

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards of arrow escape: arrows are drawn one after the other as random winding paths
 * on the empty cells, and an arrow is only kept if the way in front of its head is clear of every arrow drawn before it. Read
 * backwards that is a way out: the last arrow drawn leaves first (nothing else is in its way), then the one before it, and so
 * on, so every board can be cleared. The board is kept only if a fair number of arrows can leave at the start and the text that
 * saves it fits the server. The board only depends on the seed, but it is saved as the board itself, so a later change of this
 * algorithm does not change a saved game.
 */
object ArrowsGenerator {
    private const val MAX_ATTEMPTS = 400
    private const val MAX_FAILED_PLACEMENTS = 60
    private const val CANDIDATES = 12

    /** The longest saved text the cloud table keeps. */
    private const val MAX_TEXT = 32

    /** A board at [difficulty] (see [GameLevels.arrows]). */
    fun generate(seed: Int, difficulty: Difficulty): ArrowsPuzzle = generate(seed, GameLevels.arrows(difficulty))

    fun generate(seed: Int, shape: GameLevels.ArrowsShape = GameLevels.arrows(Difficulty.Medium)): ArrowsPuzzle {
        val random = Random(seed)
        val n = shape.size
        repeat(MAX_ATTEMPTS) {
            val taken = BooleanArray(n * n)
            val arrows = ArrayList<ArrowPath>()
            // The cells in front of the head of each arrow, and whether something is on them already (it can not leave then).
            val rays = ArrayList<Set<Int>>()
            val blocked = ArrayList<Boolean>()
            var failed = 0
            while (arrows.size < shape.maxArrows && failed < MAX_FAILED_PLACEMENTS) {
                // Several tries: the one that blocks the most arrows that could leave makes the board the most tangled.
                var best: ArrowPath? = null
                var bestScore = -1
                repeat(CANDIDATES) {
                    val candidate = place(random, n, taken, shape) ?: return@repeat
                    val score = 4 * rays.indices.count { !blocked[it] && candidate.cells.any { c -> c in rays[it] } } + ray(n, candidate).size
                    if (score > bestScore) {
                        best = candidate
                        bestScore = score
                    }
                }
                val arrow = best
                if (arrow == null) {
                    failed++
                } else {
                    for (i in rays.indices) if (arrow.cells.any { it in rays[i] }) blocked[i] = true
                    arrows += arrow
                    rays += ray(n, arrow)
                    blocked += false
                    arrow.cells.forEach { taken[it] = true }
                }
            }
            if (arrows.size < shape.minArrows) return@repeat
            val puzzle = ArrowsPuzzle(n, arrows)
            val free = ArrowsRules.free(puzzle, BooleanArray(arrows.size) { true }).size
            if (free in shape.minFree..shape.maxFree && puzzle.encode().length <= MAX_TEXT) return puzzle
        }
        error("no board found")
    }

    /** One more arrow: a random winding path whose head has a clear way out past the arrows already on the board, or null. */
    private fun place(random: Random, n: Int, taken: BooleanArray, shape: GameLevels.ArrowsShape): ArrowPath? {
        val empty = taken.indices.filter { !taken[it] }
        if (empty.isEmpty()) return null
        val start = empty[random.nextInt(empty.size)]
        val length = shape.minLength + random.nextInt(shape.maxLength - shape.minLength + 1)
        val cells = arrayListOf(start)
        while (cells.size < length) {
            val last = cells.last()
            val next = (0 until 4).map { d -> d to (last / n + ArrowDir.dRow[d]) * n + (last % n + ArrowDir.dColumn[d]) }
                .filter { (d, cell) ->
                    val row = last / n + ArrowDir.dRow[d]
                    val column = last % n + ArrowDir.dColumn[d]
                    row in 0 until n && column in 0 until n && !taken[cell] && cell !in cells
                }
            if (next.isEmpty()) break
            cells += next[random.nextInt(next.size)].second
        }
        if (cells.size < shape.minLength) return null
        val dir = if (cells.size == 1) random.nextInt(4) else ArrowsPuzzle.stepDir(n, cells[cells.size - 2], cells.last())
        // The way out: no arrow on the board, and none of its own body, in front of the head.
        var row = cells.last() / n + ArrowDir.dRow[dir]
        var column = cells.last() % n + ArrowDir.dColumn[dir]
        while (row in 0 until n && column in 0 until n) {
            val cell = row * n + column
            if (taken[cell] || cell in cells) return null
            row += ArrowDir.dRow[dir]
            column += ArrowDir.dColumn[dir]
        }
        return ArrowPath(cells, dir)
    }

    /** The cells in front of the head of [arrow], to the edge of the board. */
    private fun ray(n: Int, arrow: ArrowPath): Set<Int> {
        val out = HashSet<Int>()
        var row = arrow.head / n + ArrowDir.dRow[arrow.dir]
        var column = arrow.head % n + ArrowDir.dColumn[arrow.dir]
        while (row in 0 until n && column in 0 until n) {
            out += row * n + column
            row += ArrowDir.dRow[arrow.dir]
            column += ArrowDir.dColumn[arrow.dir]
        }
        return out
    }
}
