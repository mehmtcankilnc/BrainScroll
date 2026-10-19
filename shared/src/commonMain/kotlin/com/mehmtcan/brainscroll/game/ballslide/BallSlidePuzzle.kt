package com.mehmtcan.brainscroll.game.ballslide

import com.mehmtcan.brainscroll.game.BitText

/** The four ways the ball can slide. Stored as the number. */
object BallDir {
    const val UP = 0
    const val RIGHT = 1
    const val DOWN = 2
    const val LEFT = 3

    /** Row and column step of each direction. */
    val dRow = intArrayOf(-1, 0, 1, 0)
    val dColumn = intArrayOf(0, 1, 0, -1)
}

/**
 * A board of ball slide: a square with some wall cells, and the cell the ball starts on. The ball
 * slides in one of four directions until a wall or the edge stops it, painting every cell it passes; the board is won when every
 * cell that is not a wall is painted. A valid board can be won (see [BallSlideSolver]).
 *
 * [encode] is the saved text: the size, the walls (a bit a cell) and the start cell, six bits to a character, 8 characters at the
 * most.
 */
class BallSlidePuzzle(val size: Int, val walls: List<Boolean>, val start: Int) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "a small board" }
        require(walls.size == size * size) { "a mark for every cell" }
        require(start in walls.indices && !walls[start]) { "the ball starts on an open cell" }
    }

    val cellCount: Int get() = size * size

    /** How many cells have to be painted: every cell that is not a wall. */
    val openCount: Int get() = walls.count { !it }

    /**
     * Where the ball ends and the cells it passes (the one it ends on included, the one it starts on not) when it slides from
     * [from] in direction [dir]. The path is empty when a wall or the edge is right there.
     */
    fun slide(from: Int, dir: Int): List<Int> {
        val path = ArrayList<Int>()
        var row = from / size
        var column = from % size
        while (true) {
            val nextRow = row + BallDir.dRow[dir]
            val nextColumn = column + BallDir.dColumn[dir]
            if (nextRow !in 0 until size || nextColumn !in 0 until size || walls[nextRow * size + nextColumn]) break
            row = nextRow
            column = nextColumn
            path += row * size + column
        }
        return path
    }

    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 2)
        for (wall in walls) writer.put(if (wall) 1 else 0, 1)
        writer.put(start, 6)
        return writer.text()
    }

    companion object {
        const val MIN_SIZE = 3
        const val MAX_SIZE = 6

        /** Null if [text] is not a board that [encode] wrote. */
        fun decode(text: String): BallSlidePuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(2) ?: return null) + MIN_SIZE
            val walls = ArrayList<Boolean>()
            repeat(size * size) { walls += (reader.take(1) ?: return null) == 1 }
            val start = reader.take(6) ?: return null
            return try {
                BallSlidePuzzle(size, walls, start)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
