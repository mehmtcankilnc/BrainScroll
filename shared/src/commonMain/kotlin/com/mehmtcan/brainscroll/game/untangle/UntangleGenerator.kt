package com.mehmtcan.brainscroll.game.untangle

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/** A tangle, and a way to lay its dots so that no line crosses another (the one it was made from). */
class UntangleBoard(val puzzle: UntanglePuzzle, val solution: List<Int>)

/**
 * Makes the tangles: dots are put at random places (not too near each other), every pair of dots is tried shortest first and
 * the line is kept if it crosses none kept so far, which leaves a drawing with no crossing; the shortest lines of it are kept (so
 * many, at least), then lines are added until the dots are all joined and every dot has two. That is a tangle that can be undone. Then
 * the dots are put on a circle in a random order, and the result is kept when at least the level's number of pairs of lines cross.
 * The board only depends on the seed, but it is saved as the board itself, so a later change of this does not change a saved game.
 */
object UntangleGenerator {
    private const val LAYOUTS = 400
    private const val SHUFFLES = 80
    private const val MIN_DISTANCE_SQUARED = 150 * 150
    private const val EDGE_OF_PLACING = UNTANGLE_MARGIN + 60

    fun generate(seed: Int, difficulty: Difficulty): UntangleBoard = generate(seed, GameLevels.untangle(difficulty))

    fun generate(seed: Int, shape: GameLevels.UntangleShape): UntangleBoard {
        val random = Random(seed)
        repeat(LAYOUTS) { layout(random, shape)?.let { return it } }
        error("no board found")
    }

    private fun layout(random: Random, shape: GameLevels.UntangleShape): UntangleBoard? {
        val n = shape.nodes
        val positions = ArrayList<Int>()
        var guard = 0
        while (positions.size < 2 * n && guard++ < 5_000) {
            val x = EDGE_OF_PLACING + random.nextInt(UNTANGLE_BOARD - 2 * EDGE_OF_PLACING)
            val y = EDGE_OF_PLACING + random.nextInt(UNTANGLE_BOARD - 2 * EDGE_OF_PLACING)
            val far = (0 until positions.size / 2).all { k ->
                val dx = positions[2 * k] - x
                val dy = positions[2 * k + 1] - y
                dx * dx + dy * dy >= MIN_DISTANCE_SQUARED
            }
            if (far) {
                positions += x
                positions += y
            }
        }
        if (positions.size < 2 * n) return null
        fun dist(a: Int, b: Int): Int {
            val dx = positions[2 * a] - positions[2 * b]
            val dy = positions[2 * a + 1] - positions[2 * b + 1]
            return dx * dx + dy * dy
        }
        val candidates = ArrayList<Pair<Int, Int>>()
        for (a in 0 until n) for (b in a + 1 until n) candidates += a to b
        candidates.sortWith(compareBy({ dist(it.first, it.second) }, { it.first }, { it.second }))
        // Every line that crosses none before it (shortest first): a drawing with as many lines as it can have.
        val planar = ArrayList<Pair<Int, Int>>()
        for (e in candidates) {
            if (planar.none { f -> !(e.first == f.first || e.first == f.second || e.second == f.first || e.second == f.second) && UntangleRules.cross(positions, e.first, e.second, f.first, f.second) }) planar += e
        }
        val kept = LinkedHashSet<Pair<Int, Int>>(planar.take(shape.edges))
        // Join the dots (a line from the rest of the planar ones whenever it joins two parts), then give every dot two lines.
        val parent = IntArray(n) { it }
        fun find(x: Int): Int {
            var r = x
            while (parent[r] != r) r = parent[r]
            return r
        }
        for ((a, b) in kept) parent[find(a)] = find(b)
        for (e in planar) if (e !in kept && find(e.first) != find(e.second)) {
            kept += e
            parent[find(e.first)] = find(e.second)
        }
        val degree = IntArray(n)
        for ((a, b) in kept) { degree[a]++; degree[b]++ }
        for (e in planar) if (e !in kept && (degree[e.first] < 2 || degree[e.second] < 2)) {
            kept += e
            degree[e.first]++
            degree[e.second]++
        }
        if (kept.size < n + 2) return null
        val edges = kept.toList().sortedWith(compareBy({ it.first }, { it.second }))
        repeat(SHUFFLES) {
            val slots = (0 until n).shuffled(random)
            val puzzle = UntanglePuzzle(n, edges, slots)
            if (UntangleRules.crossingPairs(puzzle.start, edges) >= shape.minCrossings) return UntangleBoard(puzzle, positions)
        }
        return null
    }
}
