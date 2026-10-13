package com.mehmtcan.brainscroll.game.guide

import com.mehmtcan.brainscroll.game.slide.SlideGame
import com.mehmtcan.brainscroll.game.slide.SlideState
import com.mehmtcan.brainscroll.game.slide.SlideStatus
import com.mehmtcan.brainscroll.game.target.Op
import com.mehmtcan.brainscroll.game.target.TargetState
import com.mehmtcan.brainscroll.game.target.TargetStatus
import com.mehmtcan.brainscroll.game.target.Token
import com.mehmtcan.brainscroll.game.tubes.TubesGame
import com.mehmtcan.brainscroll.game.tubes.TubesState
import com.mehmtcan.brainscroll.game.tubes.TubesStatus
import kotlin.math.abs

/**
 * The guide of the sliding puzzle: the first step of the shortest way, found by iterative deepening A* with the sum of
 * the distances of the tiles from home as its estimate. It stops after [maxNodes] positions and then says nothing, so a
 * very hard position never hangs the app.
 */
object SlideGuide {
    private const val FOUND = -1
    private const val ABORTED = -2

    fun step(state: SlideState, maxNodes: Int = 3_000_000): GuideStep? {
        if (state.status != SlideStatus.Playing) return null
        val cols = state.cols
        val rows = state.rows
        val board = state.tiles.toIntArray()
        if (SlideGame.isSolved(board)) return null
        val blank = board.indexOf(0)

        var nodes = 0

        fun estimate(): Int = SlideGame.distance(board, cols)

        fun neighbors(cell: Int): List<Int> = buildList {
            val r = cell / cols
            val c = cell % cols
            if (r > 0) add(cell - cols)
            if (r < rows - 1) add(cell + cols)
            if (c > 0) add(cell - 1)
            if (c < cols - 1) add(cell + 1)
        }

        // Depth first down to the bound. Returns FOUND, ABORTED, or the smallest estimate that went over the bound.
        fun search(blankAt: Int, previous: Int, depth: Int, bound: Int): Int {
            val f = depth + estimate()
            if (f > bound) return f
            if (SlideGame.isSolved(board)) return FOUND
            if (++nodes > maxNodes) return ABORTED
            var smallest = Int.MAX_VALUE
            for (next in neighbors(blankAt)) {
                if (next == previous) continue
                board[blankAt] = board[next]
                board[next] = 0
                val result = search(next, blankAt, depth + 1, bound)
                board[next] = board[blankAt]
                board[blankAt] = 0
                if (result == FOUND || result == ABORTED) return result
                smallest = minOf(smallest, result)
            }
            return smallest
        }

        var bound = estimate()
        while (true) {
            var smallest = Int.MAX_VALUE
            // The first slide of each way is tried by hand, to know which one the bound is found with.
            for (first in neighbors(blank)) {
                board[blank] = board[first]
                board[first] = 0
                val result = search(first, blank, 1, bound)
                board[first] = board[blank]
                board[blank] = 0
                if (result == ABORTED) return null
                if (result == FOUND) return GuideStep(GuideReason.SlideTile, listOf(first))
                smallest = minOf(smallest, result)
            }
            if (smallest == Int.MAX_VALUE) return null
            bound = smallest
        }
    }
}

/**
 * The guide of the color tubes: the first pour of a way that finishes them, found breadth first (so the way is short).
 * When no way is found from the position the player is in, it says the tubes are stuck.
 */
object TubesGuide {
    private class Node(val tubes: List<List<Int>>, val parent: Node?, val from: Int, val to: Int)

    fun step(state: TubesState, maxStates: Int = 200_000): GuideStep? {
        if (state.status != TubesStatus.Playing) return null
        val capacity = state.capacity
        fun key(tubes: List<List<Int>>) = tubes.map { it.joinToString("") }.sorted().joinToString("|")

        val root = Node(state.tubes, null, -1, -1)
        val seen = HashSet<String>()
        seen += key(root.tubes)
        val queue = ArrayDeque<Node>()
        queue.addLast(root)
        while (queue.isNotEmpty() && seen.size <= maxStates) {
            val node = queue.removeFirst()
            if (TubesGame.isSolved(node.tubes, capacity)) {
                var first = node
                while (first.parent != null && first.parent !== root) first = first.parent!!
                return if (first.parent == null) null else GuideStep(GuideReason.TubesPour, listOf(first.from, first.to))
            }
            for (from in node.tubes.indices) for (to in node.tubes.indices) {
                val next = TubesGame.poured(node.tubes, from, to, capacity) ?: continue
                if (seen.add(key(next))) queue.addLast(Node(next, node, from, to))
            }
        }
        return GuideStep(GuideReason.TubesStuck)
    }
}

/**
 * The guide of the target number: the first operation of the fewest that reach the target from the numbers on the table
 * now, found breadth first. When no way is left it says so ([GuideReason.TargetStuck]), and the player starts over. It points at the two numbers (by their ids) and says the operation.
 */
object TargetGuide {
    private class Node(val tokens: List<Token>, val first: Triple<Int, Op, Int>?, val nextId: Int)

    fun step(state: TargetState): GuideStep? {
        if (state.status != TargetStatus.Playing) return null
        val seen = HashSet<List<Int>>()
        seen += state.tokens.map { it.value }.sorted()
        var frontier = listOf(Node(state.tokens, null, (state.tokens.maxOfOrNull { it.id } ?: -1) + 1))
        while (frontier.isNotEmpty()) {
            val next = mutableListOf<Node>()
            for (node in frontier) {
                for (a in node.tokens) for (b in node.tokens) {
                    if (a.id == b.id) continue
                    for (op in Op.entries) {
                        // Adding and multiplying do not care about the order: each pair once.
                        if ((op == Op.Add || op == Op.Mul) && a.id > b.id) continue
                        val result = op.apply(a.value, b.value) ?: continue
                        val first = node.first ?: Triple(a.id, op, b.id)
                        if (result == state.target) return found(state, first)
                        val rest = node.tokens.filter { it.id != a.id && it.id != b.id } + Token(node.nextId, result)
                        if (seen.add(rest.map { it.value }.sorted())) next += Node(rest, first, node.nextId + 1)
                    }
                }
            }
            frontier = next
        }
        return GuideStep(GuideReason.TargetStuck)
    }

    private fun found(state: TargetState, move: Triple<Int, Op, Int>): GuideStep {
        val a = state.tokens.first { it.id == move.first }
        val b = state.tokens.first { it.id == move.third }
        return GuideStep(GuideReason.TargetCombine, listOf(a.id, b.id), listOf(a.value, b.value), move.second.symbol.toString())
    }
}
