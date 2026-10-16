package com.mehmtcan.brainscroll.game.tents

import kotlin.random.Random

/**
 * Makes the boards of the tents puzzle (docs/yeni-oyunlar-plan.md): tents are put at random so that none touch, every tent gets a
 * tree next to it, and the board is kept only if the trees and the numbers leave exactly one way to place the tents (the
 * solver checks). The board only depends on the seed, but it is saved as the board itself, so a later change of this
 * algorithm does not change a saved game.
 */
object TentsGenerator {
    /** How many tents (and trees) a board of the default size has. */
    const val DEFAULT_TENTS = 8

    /** How many random boards are tried for one seed before giving up (it never comes to that for 6 by 6). */
    private const val MAX_ATTEMPTS = 20_000

    fun generate(seed: Int, size: Int = TentsPuzzle.DEFAULT_SIZE, tents: Int = DEFAULT_TENTS): TentsPuzzle {
        require(size in TentsPuzzle.MIN_SIZE..TentsPuzzle.MAX_SIZE) { "a small board" }
        val random = Random(seed)
        repeat(MAX_ATTEMPTS) {
            val puzzle = candidate(random, size, tents) ?: return@repeat
            val found = TentsSolver.solutions(size, puzzle.trees, puzzle.rowTargets, puzzle.columnTargets)
            if (found.size == 1) return puzzle
        }
        error("no board with one solution found")
    }

    /** Tents that do not touch, each with a tree of its own next to it, or null if the random choices led nowhere. */
    private fun candidate(random: Random, size: Int, count: Int): TentsPuzzle? {
        val tents = ArrayList<Int>()
        for (cell in (0 until size * size).shuffled(random)) {
            if (tents.size == count) break
            if (tents.none { TentsRules.touch(size, it, cell) }) tents += cell
        }
        if (tents.size < count) return null
        val trees = HashSet<Int>()
        for (tent in tents.shuffled(random)) {
            val room = TentsRules.straightNeighbors(size, tent).filter { it !in tents && it !in trees }
            if (room.isEmpty()) return null
            trees += room.random(random)
        }
        return TentsPuzzle(size, trees, tents.toSet())
    }
}
