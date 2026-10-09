package com.mehmtcan.brainscroll.game.lights

import kotlin.random.Random

enum class LightsStatus { Playing, Won }

/** What a tap did. */
enum class LightsResult {
    /** Nothing happened: out of range, or the game is over. Not counted. */
    Ignored,

    /** The cell and its neighbors were switched. */
    Toggled,

    /** The switch put out the last light. */
    Solved,
}

/** The picture of a board for the UI. Cells are numbered row by row from 0. */
data class LightsState(
    val size: Int,
    val lit: List<Boolean>,
    /** Taps so far. A restart does not give them back. */
    val moves: Int,
    /** The board was made with this many taps from the dark board, so it can be solved in at most this many. */
    val par: Int,
    val status: LightsStatus,
    /** False while the board is as it was at the start. */
    val canRestart: Boolean,
)

/**
 * The rules of Lights Out, free of UI and time.
 *
 * A tap switches the cell and the cells above, below, left and right of it. The board is made backwards: starting
 * from the dark board, [taps] different cells are tapped. Taps cancel each other out when repeated and do not depend
 * on their order, so tapping the same cells again puts every light out: every board can be solved, in at most [taps]
 * taps. The board only depends on [seed], [size] and [taps], so a saved game can be rebuilt exactly.
 */
class LightsGame(
    val seed: Int,
    val size: Int = DEFAULT_SIZE,
    val taps: Int = DEFAULT_TAPS,
) {
    private val start: BooleanArray
    private val lit: BooleanArray
    private var moves = 0

    init {
        require(size >= 3) { "a board needs some room" }
        require(taps in 1..size * size) { "taps must fit on the board" }
        start = generate(seed, size, taps)
        lit = start.copyOf()
    }

    val isFinished: Boolean get() = lit.none { it }

    val state: LightsState
        get() = LightsState(
            size = size,
            lit = lit.toList(),
            moves = moves,
            par = taps,
            status = if (isFinished) LightsStatus.Won else LightsStatus.Playing,
            canRestart = !isFinished && !lit.contentEquals(start),
        )

    /** Taps [cell]: it and its neighbors switch. */
    fun tap(cell: Int): LightsResult {
        if (isFinished || cell !in lit.indices) return LightsResult.Ignored
        press(lit, size, cell)
        moves++
        return if (isFinished) LightsResult.Solved else LightsResult.Toggled
    }

    /** Puts the lights back as they were at the start. The moves stay counted. Returns false if nothing had changed. */
    fun restart(): Boolean {
        if (!state.canRestart) return false
        start.copyInto(lit)
        return true
    }

    companion object {
        const val DEFAULT_SIZE = 5
        const val DEFAULT_TAPS = 7

        /** The board for [seed] (see [scramble]). */
        fun generate(seed: Int, size: Int, taps: Int): BooleanArray = scramble(seed, size, taps).board

        /** The cells a board was made with: tapping them again puts every light out. */
        fun solutionTaps(seed: Int, size: Int, taps: Int): List<Int> = scramble(seed, size, taps).taps

        class Scramble(val board: BooleanArray, val taps: List<Int>)

        /**
         * A board that is not already dark and has a few lights on, with the taps that made it. The same arguments
         * always give the same result.
         */
        private fun scramble(seed: Int, size: Int, taps: Int): Scramble {
            val random = Random(seed)
            val cells = size * size
            var result = Scramble(BooleanArray(cells), emptyList())
            repeat(MAX_ATTEMPTS) {
                val board = BooleanArray(cells)
                val chosen = (0 until cells).shuffled(random).take(taps)
                chosen.forEach { press(board, size, it) }
                result = Scramble(board, chosen)
                if (board.count { it } >= MIN_LIT) return result
            }
            return result
        }

        /** A board with fewer lights than this is too easy to be worth a page. */
        private const val MIN_LIT = 4

        /** A board that is dark enough to need another try is rare; this only stops odd settings from looping forever. */
        private const val MAX_ATTEMPTS = 1_000

        private fun press(board: BooleanArray, size: Int, cell: Int) {
            val r = cell / size
            val c = cell % size
            board[cell] = !board[cell]
            if (r > 0) board[cell - size] = !board[cell - size]
            if (r < size - 1) board[cell + size] = !board[cell + size]
            if (c > 0) board[cell - 1] = !board[cell - 1]
            if (c < size - 1) board[cell + 1] = !board[cell + 1]
        }
    }
}
