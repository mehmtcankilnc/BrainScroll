package com.mehmtcan.brainscroll.game.untangle

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Finds a way to lay the dots of a tangle so that no line crosses another, by simulated annealing: the dots start at random places;
 * one dot at a time is tried at a new place and the move is kept if it does not add crossings (or, now and then, even if it does, so
 * that a bad corner can be left); the chance of that falls as the search goes on. It starts over if it ends with crossings left. Then
 * the drawing is made roomy: dots are nudged while no line crosses another, and a nudge is kept if no dot ends nearer to a line it is not
 * on (so a dot is never nearly on a line, where a few units of rounding would make a crossing). The boards are small (at most 14 dots) so
 * this takes a moment. The search only depends on the board, so it finds the same drawing every time.
 *
 * The guide uses it: the saved board does not keep the drawing it was made from.
 */
object UntangleEmbedder {
    private const val RESTARTS = 80
    private const val STEPS = 6_000
    private const val ROOM_STEPS = 2_500

    /** The place of every dot (x, y of dot 0, and so on) with no crossing, or null if none was found. */
    fun embed(puzzle: UntanglePuzzle): List<Int>? = embed(puzzle.nodeCount, puzzle.edges, puzzle.hashCode())

    /** The same for [n] dots and the lines [edges] (the search starts from [seed]). */
    fun embed(n: Int, edges: List<Pair<Int, Int>>, seed: Int): List<Int>? {
        val random = Random(seed)
        val incident = Array(n) { node -> edges.indices.filter { edges[it].first == node || edges[it].second == node } }
        val low = UNTANGLE_MARGIN
        val high = UNTANGLE_BOARD - UNTANGLE_MARGIN

        fun share(e: Pair<Int, Int>, f: Pair<Int, Int>) = e.first == f.first || e.first == f.second || e.second == f.first || e.second == f.second

        repeat(RESTARTS) {
            val pos = MutableList(2 * n) { low + random.nextInt(high - low + 1) }
            // The crossings that involve a line of [node].
            fun around(node: Int): Int {
                var count = 0
                for (i in incident[node]) for (j in edges.indices) {
                    if (j == i || share(edges[i], edges[j])) continue
                    if (UntangleRules.cross(pos, edges[i].first, edges[i].second, edges[j].first, edges[j].second)) count++
                }
                return count
            }
            var cost = UntangleRules.crossingPairs(pos, edges)
            var temperature = 2.0
            for (step in 0 until STEPS) {
                if (cost == 0) break
                val node = random.nextInt(n)
                val oldX = pos[2 * node]
                val oldY = pos[2 * node + 1]
                val before = around(node)
                if (random.nextInt(10) < 3) {
                    pos[2 * node] = low + random.nextInt(high - low + 1)
                    pos[2 * node + 1] = low + random.nextInt(high - low + 1)
                } else {
                    pos[2 * node] = (oldX + random.nextInt(-160, 161)).coerceIn(low, high)
                    pos[2 * node + 1] = (oldY + random.nextInt(-160, 161)).coerceIn(low, high)
                }
                val delta = around(node) - before
                if (delta <= 0 || random.nextDouble() < exp(-delta / temperature)) {
                    cost += delta
                } else {
                    pos[2 * node] = oldX
                    pos[2 * node + 1] = oldY
                }
                temperature = maxOf(0.05, temperature * 0.999)
            }
            if (cost == 0) {
                makeRoomy(pos, n, edges, incident, random, low, high)
                return pos
            }
        }
        return null
    }

    /** How near a dot comes to a line it is not on, at the nearest. */
    private fun clearance(pos: List<Int>, n: Int, edges: List<Pair<Int, Int>>): Double {
        var best = Double.MAX_VALUE
        for (i in 0 until n) for ((a, b) in edges) {
            if (a == i || b == i) continue
            val d = distanceToSegment(pos[2 * i], pos[2 * i + 1], pos[2 * a], pos[2 * a + 1], pos[2 * b], pos[2 * b + 1])
            if (d < best) best = d
        }
        return best
    }

    private fun distanceToSegment(px: Int, py: Int, ax: Int, ay: Int, bx: Int, by: Int): Double {
        val dx = (bx - ax).toDouble()
        val dy = (by - ay).toDouble()
        val lengthSquared = dx * dx + dy * dy
        val t = if (lengthSquared == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        return hypot(px - (ax + t * dx), py - (ay + t * dy))
    }

    /** Nudges the dots of a drawing with no crossing so that the nearest a dot comes to a line it is not on gets no smaller (it mostly grows). */
    private fun makeRoomy(pos: MutableList<Int>, n: Int, edges: List<Pair<Int, Int>>, incident: Array<List<Int>>, random: Random, low: Int, high: Int) {
        var best = clearance(pos, n, edges)
        repeat(ROOM_STEPS) {
            val node = random.nextInt(n)
            val oldX = pos[2 * node]
            val oldY = pos[2 * node + 1]
            pos[2 * node] = (oldX + random.nextInt(-50, 51)).coerceIn(low, high)
            pos[2 * node + 1] = (oldY + random.nextInt(-50, 51)).coerceIn(low, high)
            var crosses = false
            for (i in incident[node]) {
                for (j in edges.indices) {
                    if (j == i) continue
                    val e = edges[i]
                    val f = edges[j]
                    if (e.first == f.first || e.first == f.second || e.second == f.first || e.second == f.second) continue
                    if (UntangleRules.cross(pos, e.first, e.second, f.first, f.second)) {
                        crosses = true
                        break
                    }
                }
                if (crosses) break
            }
            val now = if (crosses) -1.0 else clearance(pos, n, edges)
            if (now >= best) {
                best = now
            } else {
                pos[2 * node] = oldX
                pos[2 * node + 1] = oldY
            }
        }
    }
}
