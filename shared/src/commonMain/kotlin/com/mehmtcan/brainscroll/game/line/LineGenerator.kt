package com.mehmtcan.brainscroll.game.line

/**
 * Makes the boards of the one-line game (docs/tek-cizgi-plan.md).
 *
 * First a random line that fills the whole grid is made (a Hamiltonian path), by "backbite" moves: start from a snake
 * and, over and over, join one end of the line to a free-standing neighbor and turn the part behind it round. Then the
 * numbers are put on that line: number 1 at its start, the last number at its end, the others at random places between.
 * A board is only kept when the solver finds exactly one line. While there are more, a number is added (the best of a
 * few places tried, the one that leaves the fewest lines); once the line is the only one, numbers that are not needed
 * are taken off again, so a board has about 6 numbers on 5x5, 8 on 6x6 and 10 on 7x7.
 *
 * The board only depends on the seed and the algorithm of [SplitMix64], and a saved game keeps the board itself.
 */
object LineGenerator {
    /** How many lines are tried before the board is filled with numbers (it never comes to that on the sizes in use, see the tests). */
    private const val MAX_ATTEMPTS = 200

    /** How many places for the next number are tried, and how many lines are counted for each (more is the same as many). */
    private const val TRIES = 6
    private const val COUNT_LIMIT = 30

    /**
     * [checkpoints] is the number of numbers to start with, [cap] the most the search may use. The board that comes out
     * has the numbers it needs for one line, which can be fewer or more than [checkpoints].
     */
    fun generate(seed: Long, size: Int, checkpoints: Int, cap: Int = size * size): LinePuzzle {
        require(size >= 3) { "a board needs some room" }
        require(checkpoints in 2..cap) { "a few numbers, no more than the cap" }
        val random = SplitMix64(seed)
        val cells = size * size
        repeat(MAX_ATTEMPTS) {
            val path = randomPath(random, size)
            val chosen = mutableSetOf(0, cells - 1)
            while (chosen.size < checkpoints) chosen += 1 + random.nextInt(cells - 2)
            while (true) {
                val cps = chosen.sorted().map { path[it] }.toIntArray()
                val lines = LineSolver.count(size, cps, 2)
                if (lines == 1) return LinePuzzle(size, minimal(random, size, path, chosen), path)
                if (chosen.size >= cap) break
                var best = -1
                var bestLines = Int.MAX_VALUE
                repeat(TRIES) {
                    val index = 1 + random.nextInt(cells - 2)
                    if (index in chosen) return@repeat
                    val tried = (chosen + index).sorted().map { path[it] }.toIntArray()
                    val left = LineSolver.count(size, tried, COUNT_LIMIT)
                    if (left < bestLines) { best = index; bestLines = left }
                }
                if (best >= 0) chosen += best
            }
        }
        // A board with a number on every cell has one line; it is a poor puzzle but a valid one.
        val path = randomPath(random, size)
        return LinePuzzle(size, path.copyOf(), path)
    }

    /** Takes numbers off, in random order, while the line stays the only one. The two ends always stay. */
    private fun minimal(random: SplitMix64, size: Int, path: IntArray, chosen: Set<Int>): IntArray {
        val kept = chosen.toMutableSet()
        val order = chosen.filter { it != 0 && it != path.size - 1 }.toMutableList()
        for (i in order.indices.reversed()) order.swap(i, random.nextInt(i + 1))
        for (index in order) {
            kept -= index
            val cps = kept.sorted().map { path[it] }.toIntArray()
            if (LineSolver.count(size, cps, 2) != 1) kept += index
        }
        return kept.sorted().map { path[it] }.toIntArray()
    }

    private fun MutableList<Int>.swap(a: Int, b: Int) {
        val t = this[a]; this[a] = this[b]; this[b] = t
    }

    /** A line through every cell, starting somewhere and ending somewhere else, made of steps to a neighbor. */
    fun randomPath(random: SplitMix64, size: Int): IntArray {
        val cells = size * size
        var path = IntArray(cells) { index ->
            val row = index / size
            val column = index % size
            row * size + if (row % 2 == 0) column else size - 1 - column
        }
        val position = IntArray(cells)
        repeat(cells * 30) {
            if (random.nextBoolean()) path = path.reversedArray()
            for (i in path.indices) position[path[i]] = i
            val end = path[cells - 1]
            val row = end / size
            val column = end % size
            val options = buildList {
                if (row > 0) add(end - size)
                if (column > 0) add(end - 1)
                if (column < size - 1) add(end + 1)
                if (row < size - 1) add(end + size)
            }
            val neighbor = options[random.nextInt(options.size)]
            val index = position[neighbor]
            // The neighbor right before the end is already joined; any other one turns the tail round.
            if (index < cells - 2) {
                val turned = path.copyOf()
                for (i in index + 1 until cells) turned[i] = path[cells - 1 - (i - index - 1)]
                path = turned
            }
        }
        return path
    }
}
