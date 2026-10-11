package com.mehmtcan.brainscroll.game.pipes

enum class PipesStatus { Playing, Won }

/** What a tap did. */
enum class PipesResult {
    /** Nothing happened: out of range, or the game is over. Not counted. */
    Ignored,

    /** The tile turned a quarter clockwise. */
    Turned,

    /** The turn closed the last open end. */
    Solved,
}

/** The picture of a board for the UI. Cells are numbered row by row from 0. */
data class PipesState(
    val size: Int,
    /** For every cell, its open sides (see [PipesGenerator]). */
    val tiles: List<Int>,
    /** For every cell, the open sides that meet a side of the neighbor; the others are open ends. */
    val matched: List<Int>,
    /** Taps so far. A restart does not give them back. */
    val moves: Int,
    /** The fewest taps that solve the board from the start. */
    val par: Int,
    val status: PipesStatus,
    /** False while the board is as it was at the start. */
    val canRestart: Boolean,
)

/**
 * The rules of the pipes game, free of UI and time.
 *
 * A tap turns a tile a quarter clockwise. The game is won when no tile has an open end: every open side meets an open
 * side of the neighbor. There is no losing. A restart puts the tiles back as they were and keeps the moves.
 */
class PipesGame(start: IntArray, val par: Int) {
    val size: Int = kotlin.math.sqrt(start.size.toDouble()).toInt()
    private val first = start.copyOf()
    private val tiles = start.copyOf()
    private var moves = 0

    init {
        require(size >= 3 && size * size == start.size) { "the tiles make a square board" }
        require(start.all { it in 0..15 }) { "a tile is a number of open sides" }
    }

    val isFinished: Boolean get() = PipesGenerator.isSolved(tiles, size)

    val state: PipesState
        get() = PipesState(
            size = size,
            tiles = tiles.toList(),
            matched = PipesGenerator.matched(tiles, size).toList(),
            moves = moves,
            par = par,
            status = if (isFinished) PipesStatus.Won else PipesStatus.Playing,
            canRestart = !isFinished && !tiles.contentEquals(first),
        )

    /** Turns the tile in [cell] a quarter clockwise. */
    fun tap(cell: Int): PipesResult {
        if (isFinished || cell !in tiles.indices) return PipesResult.Ignored
        tiles[cell] = PipesGenerator.rotate(tiles[cell])
        moves++
        return if (isFinished) PipesResult.Solved else PipesResult.Turned
    }

    /** Puts the tiles back as they were at the start. The moves stay counted. Returns false if nothing had changed. */
    fun restart(): Boolean {
        if (!state.canRestart) return false
        first.copyInto(tiles)
        return true
    }
}
