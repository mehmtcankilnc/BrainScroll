package com.mehmtcan.brainscroll.game.pipes

import kotlin.random.Random

/** A puzzle: the tiles as they start (see [PipesGenerator] for the numbers) and the fewest taps that solve them. */
class PipesPuzzle(val start: IntArray, val par: Int)

/**
 * Makes the boards of the pipes game (docs/decisions.md "Boru bağla").
 *
 * A tile is a number from 0 to 15, the sum of its open sides: [NORTH] 1, [EAST] 2, [SOUTH] 4, [WEST] 8. Turning a tile
 * clockwise moves every open side one step round (north to east, east to south, ...), see [rotate].
 *
 * The idea: first make a finished board, a tree of pipes that reaches every cell and has no loop and no cell with four
 * pipes; then turn every tile by a random number of quarter turns. The finished board is built by randomized Kruskal:
 * the neighbor pairs of the grid are shuffled and a pair is joined when that makes no loop and neither cell has three
 * pipes yet. Then a search over every way to turn the tiles gives the exact fewest taps that solve the board (the "par"),
 * and boards that are too close to solved are made again. The board only depends on the seed, but it is saved as the
 * tiles themselves, so a later change of this algorithm does not change a saved game.
 */
object PipesGenerator {
    const val NORTH = 1
    const val EAST = 2
    const val SOUTH = 4
    const val WEST = 8

    const val SIZE = 5

    /** A board that needs fewer taps than this is too easy to be worth a page. */
    const val DEFAULT_MIN_PAR = 15

    /** How many shufflings are tried before settling for the last one (it never comes to that on a 5x5 board). */
    private const val MAX_ATTEMPTS = 1_000

    /** One quarter turn clockwise. */
    fun rotate(tile: Int): Int = ((tile shl 1) or (tile shr 3)) and 15

    fun generate(seed: Int, size: Int = SIZE, minPar: Int = DEFAULT_MIN_PAR): PipesPuzzle {
        require(size >= 3) { "a board needs some room" }
        val random = Random(seed)
        var last: PipesPuzzle? = null
        repeat(MAX_ATTEMPTS) {
            val solved = spanningTree(random, size)
            val start = IntArray(size * size) { cell ->
                var tile = solved[cell]
                repeat(random.nextInt(4)) { tile = rotate(tile) }
                tile
            }
            val par = checkNotNull(minTaps(start, size)) { "a board made from a tree can always be solved" }
            last = PipesPuzzle(start, par)
            if (par >= minPar) return last!!
        }
        return last!!
    }

    /** The finished board: a tree over every cell, no cell with more than three pipes. */
    fun spanningTree(random: Random, size: Int): IntArray {
        val cells = size * size
        while (true) {
            val edges = buildList {
                for (cell in 0 until cells) {
                    if (cell % size < size - 1) add(cell to cell + 1)
                    if (cell / size < size - 1) add(cell to cell + size)
                }
            }.shuffled(random)
            val parent = IntArray(cells) { it }
            fun find(x: Int): Int {
                var root = x
                while (parent[root] != root) root = parent[root]
                return root
            }
            val pipes = IntArray(cells)
            val degree = IntArray(cells)
            var joined = 0
            for ((a, b) in edges) {
                if (degree[a] >= 3 || degree[b] >= 3) continue
                val rootA = find(a)
                val rootB = find(b)
                if (rootA == rootB) continue
                parent[rootA] = rootB
                if (b == a + 1) { pipes[a] = pipes[a] or EAST; pipes[b] = pipes[b] or WEST } else { pipes[a] = pipes[a] or SOUTH; pipes[b] = pipes[b] or NORTH }
                degree[a]++
                degree[b]++
                joined++
            }
            if (joined == cells - 1) return pipes
        }
    }

    /**
     * For every tile, the sides that meet a side of the neighbor. A side that leaves the board, or faces a side that is
     * closed, is open (not in the result).
     */
    fun matched(tiles: IntArray, size: Int): IntArray = IntArray(tiles.size) { cell ->
        val row = cell / size
        val column = cell % size
        var result = 0
        if (tiles[cell] and NORTH != 0 && row > 0 && tiles[cell - size] and SOUTH != 0) result = result or NORTH
        if (tiles[cell] and SOUTH != 0 && row < size - 1 && tiles[cell + size] and NORTH != 0) result = result or SOUTH
        if (tiles[cell] and EAST != 0 && column < size - 1 && tiles[cell + 1] and WEST != 0) result = result or EAST
        if (tiles[cell] and WEST != 0 && column > 0 && tiles[cell - 1] and EAST != 0) result = result or WEST
        result
    }

    /** True when no tile has an open end. */
    fun isSolved(tiles: IntArray, size: Int): Boolean {
        val met = matched(tiles, size)
        return tiles.indices.all { met[it] == tiles[it] }
    }

    /** The fewest clockwise taps that leave no open end, or null if there is no way (see [solve]). */
    fun minTaps(start: IntArray, size: Int): Int? = solve(start, size)?.sum()

    /**
     * How many quarter turns each tile needs in the cheapest way to leave no open end, over every arrangement that does
     * (there can be several), or null if there is none. A search tile by tile, row by row: a tile may only be turned so
     * that its left and upper sides agree with the tiles already placed, and nothing leaves the board.
     */
    fun solve(start: IntArray, size: Int): IntArray? {
        val cells = start.size
        val placed = IntArray(cells)
        val turned = IntArray(cells)
        var best: IntArray? = null
        var bestCost = Int.MAX_VALUE

        fun place(cell: Int, cost: Int) {
            if (cost >= bestCost) return
            if (cell == cells) {
                bestCost = cost
                best = turned.copyOf()
                return
            }
            val row = cell / size
            val column = cell % size
            var tile = start[cell]
            val tried = IntArray(4) { -1 }
            for (turns in 0..3) {
                // A turn that gives a shape already tried costs more than the first, so it is skipped.
                if (tried.none { it == tile }) {
                    tried[turns] = tile
                    val west = column > 0 && placed[cell - 1] and EAST != 0
                    val north = row > 0 && placed[cell - size] and SOUTH != 0
                    val fits = (tile and WEST != 0) == west &&
                        (tile and NORTH != 0) == north &&
                        (column < size - 1 || tile and EAST == 0) &&
                        (row < size - 1 || tile and SOUTH == 0)
                    if (fits) {
                        placed[cell] = tile
                        turned[cell] = turns
                        place(cell + 1, cost + turns)
                    }
                }
                tile = rotate(tile)
            }
        }

        place(0, 0)
        return best
    }
}
