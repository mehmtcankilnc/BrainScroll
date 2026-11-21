package com.mehmtcan.brainscroll.game.untangle

import com.mehmtcan.brainscroll.game.BitText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The board is a square this many units on a side; a position is a whole number of units from the top left corner. */
const val UNTANGLE_BOARD = 1000

/** How near the edge of the board a dot can be put, in units. */
const val UNTANGLE_MARGIN = 40

/**
 * A tangle: dots and the lines between them (a graph that can be drawn with no line crossing another), and where the dots lie at the
 * start. The player drags the dots until no two lines cross.
 *
 * [edges] are pairs of dots (the lower number first, no pair twice). [slots] says where each dot lies at the start: the dots are put on
 * a circle, [slots] gives the place on it (0 until the number of dots) of every dot, so it is a shuffle of the dots.
 *
 * [encode] is the saved text: the number of dots, one bit for every pair of dots (is there a line), and four bits for every dot (its
 * place on the circle): 26 characters for 14 dots.
 */
class UntanglePuzzle(val nodeCount: Int, val edges: List<Pair<Int, Int>>, val slots: List<Int>) {
    init {
        require(nodeCount in MIN_NODES..MAX_NODES) { "a few dots" }
        require(edges.all { (a, b) -> a in 0 until b && b < nodeCount }) { "lines join two dots, lower number first" }
        require(edges.toSet().size == edges.size) { "no line twice" }
        require(slots.size == nodeCount && slots.toSet() == (0 until nodeCount).toSet()) { "a place on the circle for every dot" }
    }

    /** Where the dots are when the board is given: flattened as x, y of dot 0, x, y of dot 1, and so on. */
    val start: List<Int> = List(nodeCount * 2) { i ->
        val angle = -PI / 2 + 2 * PI * slots[i / 2] / nodeCount
        val center = UNTANGLE_BOARD / 2.0
        val radius = UNTANGLE_BOARD / 2.0 - UNTANGLE_MARGIN - 60
        (center + (if (i % 2 == 0) cos(angle) else sin(angle)) * radius).roundToInt()
    }

    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(nodeCount, 4)
        val set = edges.toSet()
        for (a in 0 until nodeCount) for (b in a + 1 until nodeCount) writer.put(if ((a to b) in set) 1 else 0, 1)
        for (slot in slots) writer.put(slot, 4)
        return writer.text()
    }

    override fun equals(other: Any?) = other is UntanglePuzzle && other.nodeCount == nodeCount && other.edges == edges && other.slots == slots

    override fun hashCode() = 31 * (31 * nodeCount + edges.hashCode()) + slots.hashCode()

    companion object {
        const val MIN_NODES = 4
        const val MAX_NODES = 14

        /** Null if [text] is not a board that [encode] wrote. */
        fun decode(text: String): UntanglePuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val n = reader.take(4) ?: return null
            if (n !in MIN_NODES..MAX_NODES) return null
            val edges = ArrayList<Pair<Int, Int>>()
            for (a in 0 until n) for (b in a + 1 until n) if ((reader.take(1) ?: return null) == 1) edges += a to b
            val slots = ArrayList<Int>()
            repeat(n) { slots += reader.take(4) ?: return null }
            return try {
                UntanglePuzzle(n, edges, slots)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}

/** The rules of the lines: which of them cross, for any place of the dots. All whole-number arithmetic, so there is no rounding. */
object UntangleRules {
    private fun side(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int): Int {
        val v = (bx - ax).toLong() * (cy - ay) - (by - ay).toLong() * (cx - ax)
        return if (v > 0) 1 else if (v < 0) -1 else 0
    }

    /** True if the lines a-b and c-d cross (they meet at a point that is the end of neither). */
    fun cross(positions: List<Int>, a: Int, b: Int, c: Int, d: Int): Boolean {
        val ax = positions[2 * a]; val ay = positions[2 * a + 1]
        val bx = positions[2 * b]; val by = positions[2 * b + 1]
        val cx = positions[2 * c]; val cy = positions[2 * c + 1]
        val dx = positions[2 * d]; val dy = positions[2 * d + 1]
        val o1 = side(ax, ay, bx, by, cx, cy)
        val o2 = side(ax, ay, bx, by, dx, dy)
        val o3 = side(cx, cy, dx, dy, ax, ay)
        val o4 = side(cx, cy, dx, dy, bx, by)
        return o1 != 0 && o2 != 0 && o3 != 0 && o4 != 0 && o1 != o2 && o3 != o4
    }

    private fun share(e: Pair<Int, Int>, f: Pair<Int, Int>) = e.first == f.first || e.first == f.second || e.second == f.first || e.second == f.second

    /** The lines (their numbers in [edges]) that cross another one. */
    fun crossingEdges(positions: List<Int>, edges: List<Pair<Int, Int>>): Set<Int> {
        val out = HashSet<Int>()
        for (i in edges.indices) for (j in i + 1 until edges.size) {
            if (share(edges[i], edges[j])) continue
            if (cross(positions, edges[i].first, edges[i].second, edges[j].first, edges[j].second)) {
                out += i
                out += j
            }
        }
        return out
    }

    /** How many pairs of lines cross. */
    fun crossingPairs(positions: List<Int>, edges: List<Pair<Int, Int>>): Int {
        var count = 0
        for (i in edges.indices) for (j in i + 1 until edges.size) {
            if (share(edges[i], edges[j])) continue
            if (cross(positions, edges[i].first, edges[i].second, edges[j].first, edges[j].second)) count++
        }
        return count
    }
}
