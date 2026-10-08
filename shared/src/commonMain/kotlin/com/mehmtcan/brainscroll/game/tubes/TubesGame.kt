package com.mehmtcan.brainscroll.game.tubes

import kotlin.random.Random

enum class TubesStatus { Playing, Won }

/** What a pour did. */
enum class PourResult {
    /** Nothing happened: the move is not allowed, or the game is over. Not counted. */
    Ignored,

    /** Liquid moved to the other tube. */
    Poured,
}

/**
 * The picture of the tubes for the UI. A tube is a list of colors from the bottom up; a color is a number from 0
 * (the UI turns it into a color and a shape).
 */
data class TubesState(
    val tubes: List<List<Int>>,
    val capacity: Int,
    /** Pours made so far. Undo and restart do not give them back: they are the price of trying again. */
    val moves: Int,
    val status: TubesStatus,
    val canUndo: Boolean,
    /** True when the tubes are not as they were at the start (restart would change something). */
    val canRestart: Boolean,
)

/**
 * The rules of the color tubes, free of UI and time.
 *
 * Pour the top color of one tube onto another. It only goes onto an empty tube or onto the same color, and only as
 * much as fits: the whole run of that color on top, or less if the other tube has less room. The game is won when
 * every tube is empty or full of one color. There is no way to lose; a stuck player can undo or restart
 * (docs/decisions.md).
 */
class TubesGame(
    private val start: List<List<Int>>,
    val capacity: Int = DEFAULT_CAPACITY,
) {
    private var tubes: List<List<Int>> = start
    private val history = ArrayDeque<List<List<Int>>>()
    private var moves = 0
    private var status = TubesStatus.Playing

    init {
        require(capacity > 0) { "a tube must hold something" }
        require(start.all { it.size <= capacity }) { "a tube holds at most $capacity" }
        val counts = start.flatten().groupingBy { it }.eachCount()
        require(counts.isNotEmpty() && counts.values.all { it == capacity }) { "every color must fill exactly one tube" }
        if (isSolved(start, capacity)) status = TubesStatus.Won
    }

    val state: TubesState
        get() = TubesState(tubes, capacity, moves, status, canUndo = history.isNotEmpty(), canRestart = tubes != start)

    val isFinished: Boolean get() = status != TubesStatus.Playing

    /** Pours tube [from] into tube [to]. */
    fun pour(from: Int, to: Int): PourResult {
        if (isFinished || from !in tubes.indices || to !in tubes.indices) return PourResult.Ignored
        val next = poured(tubes, from, to, capacity) ?: return PourResult.Ignored
        history.addLast(tubes)
        tubes = next
        moves++
        if (isSolved(tubes, capacity)) status = TubesStatus.Won
        return PourResult.Poured
    }

    /** Takes back the last pour (not the move count). False when there is nothing to take back. */
    fun undo(): Boolean {
        if (isFinished || history.isEmpty()) return false
        tubes = history.removeLast()
        return true
    }

    /** Puts every tube back as it was at the start (not the move count). False when nothing was changed. */
    fun restart(): Boolean {
        if (isFinished || tubes == start) return false
        tubes = start
        history.clear()
        return true
    }

    companion object {
        const val DEFAULT_CAPACITY = 4
        const val COLORS = 5
        const val EMPTY_TUBES = 2

        /**
         * The tubes after pouring [from] into [to], or null if that is not allowed. A pour that would only move a
         * single-color tube onto an empty one changes nothing useful and is not allowed either.
         */
        fun poured(tubes: List<List<Int>>, from: Int, to: Int, capacity: Int): List<List<Int>>? {
            if (from == to) return null
            val source = tubes[from]
            val target = tubes[to]
            if (source.isEmpty() || target.size >= capacity) return null
            val color = source.last()
            if (target.isNotEmpty() && target.last() != color) return null
            if (target.isEmpty() && source.all { it == color }) return null
            val run = source.takeLastWhile { it == color }.size
            val amount = minOf(run, capacity - target.size)
            return tubes.mapIndexed { i, tube ->
                when (i) {
                    from -> tube.dropLast(amount)
                    to -> tube + List(amount) { color }
                    else -> tube
                }
            }
        }

        fun isSolved(tubes: List<List<Int>>, capacity: Int): Boolean =
            tubes.all { it.isEmpty() || (it.size == capacity && it.all { c -> c == it.first() }) }

        /**
         * A shuffled start that can be solved: [colors] colors of [capacity] units each in as many tubes, plus
         * [empty] empty ones. Nothing is sorted yet, and no tube starts already finished. Solvability is checked by
         * [canSolve], so a player is never given an impossible board (undo and restart only help with a solvable one).
         */
        fun newLayout(random: Random, colors: Int = COLORS, capacity: Int = DEFAULT_CAPACITY, empty: Int = EMPTY_TUBES): List<List<Int>> {
            require(colors >= 2 && empty >= 1) { "need at least two colors and one empty tube" }
            while (true) {
                val units = (0 until colors).flatMap { c -> List(capacity) { c } }.shuffled(random)
                val tubes = units.chunked(capacity) + List(empty) { emptyList() }
                if (tubes.any { it.isNotEmpty() && it.all { c -> c == it.first() } }) continue
                if (canSolve(tubes, capacity)) return tubes
            }
        }

        /**
         * Searches for a way to finish [tubes]. Depth first over every state once (tubes in a different order count as
         * the same state). Gives up after [maxStates] states and says false, which only ever makes a board be thrown away.
         */
        fun canSolve(tubes: List<List<Int>>, capacity: Int, maxStates: Int = 100_000): Boolean {
            val seen = HashSet<String>()
            fun key(t: List<List<Int>>) = t.map { it.joinToString("") }.sorted().joinToString("|")
            val stack = ArrayDeque<List<List<Int>>>()
            stack.addLast(tubes)
            seen += key(tubes)
            while (stack.isNotEmpty() && seen.size <= maxStates) {
                val current = stack.removeLast()
                if (isSolved(current, capacity)) return true
                for (from in current.indices) for (to in current.indices) {
                    val next = poured(current, from, to, capacity) ?: continue
                    if (seen.add(key(next))) stack.addLast(next)
                }
            }
            return false
        }
    }
}
