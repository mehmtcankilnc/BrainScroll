package com.mehmtcan.brainscroll.game.slide

import kotlin.math.abs
import kotlin.random.Random

enum class SlideStatus { Playing, Won }

/** What a tap did. */
enum class SlideResult {
    /** Nothing happened: the empty cell, a tile that is not in line with the empty cell, out of range, or the game is over. Not counted. */
    Ignored,

    /** One or more tiles slid. */
    Moved,

    /** The slide put the last tile in its place. */
    Solved,
}

/** The picture of a board for the UI. Cells are numbered row by row from 0. */
data class SlideState(
    val cols: Int,
    val rows: Int,
    /** For every cell: the number on the tile in it (1 up to cols * rows - 1), or 0 for the empty cell. */
    val tiles: List<Int>,
    /** Taps that slid tiles. A restart does not give them back. */
    val moves: Int,
    val status: SlideStatus,
    /** False while the board is as it was at the start. */
    val canRestart: Boolean,
) {
    /** True if the tile in [cell] is where it belongs in the finished picture. */
    fun isHome(cell: Int) = tiles[cell] == cell + 1
}

/**
 * The rules of the sliding puzzle, free of UI and time.
 *
 * The tiles 1, 2, 3... sit in order with the empty cell last. A tap on a tile in the same row or column as the empty
 * cell slides it, and the tiles between it and the empty cell, one step toward the empty cell; that is one move. The
 * board is made by sliding tiles at random from the finished picture, so every board can be solved. It only depends on
 * [seed], [cols], [rows] and [scramble], so a saved game can be rebuilt exactly.
 */
class SlideGame(
    val seed: Int,
    val cols: Int = DEFAULT_COLS,
    val rows: Int = DEFAULT_ROWS,
    val scramble: Int = DEFAULT_SCRAMBLE,
) {
    private val start: IntArray
    private val tiles: IntArray
    private var moves = 0

    init {
        require(cols >= 2 && rows >= 2) { "a board needs some room" }
        require(scramble >= 1) { "the board needs some shuffling" }
        start = generate(seed, cols, rows, scramble)
        tiles = start.copyOf()
    }

    val isFinished: Boolean get() = isSolved(tiles)

    val state: SlideState
        get() = SlideState(
            cols = cols,
            rows = rows,
            tiles = tiles.toList(),
            moves = moves,
            status = if (isFinished) SlideStatus.Won else SlideStatus.Playing,
            canRestart = !isFinished && !tiles.contentEquals(start),
        )

    /** Taps the tile in [cell]. */
    fun tap(cell: Int): SlideResult {
        if (isFinished || cell !in tiles.indices || tiles[cell] == 0) return SlideResult.Ignored
        val blank = tiles.indexOf(0)
        val step = when {
            cell / cols == blank / cols -> if (cell < blank) 1 else -1
            cell % cols == blank % cols -> (if (cell < blank) 1 else -1) * cols
            else -> return SlideResult.Ignored
        }
        // Slide from the empty cell back toward the tapped tile: each tile moves one step into the gap.
        var gap = blank
        while (gap != cell) {
            tiles[gap] = tiles[gap - step]
            gap -= step
        }
        tiles[cell] = 0
        moves++
        return if (isFinished) SlideResult.Solved else SlideResult.Moved
    }

    /** Puts the tiles back as they were at the start. The moves stay counted. Returns false if nothing had changed. */
    fun restart(): Boolean {
        if (!state.canRestart) return false
        start.copyInto(tiles)
        return true
    }

    companion object {
        /** Three columns and four rows: eleven tiles, tall like a phone. */
        const val DEFAULT_COLS = 3
        const val DEFAULT_ROWS = 4

        /** How many random slides the board is made with. */
        const val DEFAULT_SCRAMBLE = 80

        /** A board whose tiles are on average closer to home than this is too easy to be worth a page. */
        private const val MIN_DISTANCE = 14

        /** The extra random slides tried before settling for a board that is still too easy. */
        private const val MAX_EXTRA = 500

        private fun solvedBoard(cols: Int, rows: Int) = IntArray(cols * rows) { if (it == cols * rows - 1) 0 else it + 1 }

        fun isSolved(tiles: IntArray) = tiles.indices.all { tiles[it] == if (it == tiles.size - 1) 0 else it + 1 }

        /** The sum, over all tiles, of how many rows and columns each is away from its place. */
        fun distance(tiles: IntArray, cols: Int): Int =
            tiles.indices.sumOf { cell ->
                val tile = tiles[cell]
                if (tile == 0) 0 else abs((tile - 1) / cols - cell / cols) + abs((tile - 1) % cols - cell % cols)
            }

        /**
         * A board made by [scramble] random single-tile slides from the finished picture (never taking back the last
         * one), then more until it is not too close to finished. The same arguments always give the same board.
         */
        fun generate(seed: Int, cols: Int, rows: Int, scramble: Int): IntArray {
            val random = Random(seed)
            val board = solvedBoard(cols, rows)
            var blank = board.size - 1
            var previous = -1
            var steps = 0
            while (steps < scramble || (distance(board, cols) < MIN_DISTANCE && steps < scramble + MAX_EXTRA)) {
                val options = neighbors(blank, cols, rows).filter { it != previous }
                val next = options[random.nextInt(options.size)]
                board[blank] = board[next]
                board[next] = 0
                previous = blank
                blank = next
                steps++
            }
            return board
        }

        private fun neighbors(cell: Int, cols: Int, rows: Int): List<Int> {
            val r = cell / cols
            val c = cell % cols
            return buildList {
                if (r > 0) add(cell - cols)
                if (r < rows - 1) add(cell + cols)
                if (c > 0) add(cell - 1)
                if (c < cols - 1) add(cell + 1)
            }
        }
    }
}
