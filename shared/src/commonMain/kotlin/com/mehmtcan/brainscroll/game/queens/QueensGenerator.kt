package com.mehmtcan.brainscroll.game.queens

import kotlin.random.Random

/** A puzzle: the size of the board and the cells of the queens that are locked from the start. */
class QueensPuzzle(val size: Int, val locked: Set<Int>)

/**
 * Makes the boards of the queens puzzle (docs/yeni-oyunlar-plan.md): one of the full placements, picked at random, and a few
 * of its queens locked. The player places the rest; the way they find may be another one than the one that was picked, which is
 * as good. The board only depends on the seed, but it is saved as the locked cells themselves, so a later change of this
 * algorithm does not change a saved game.
 */
object QueensGenerator {
    const val DEFAULT_LOCKED = 2

    fun generate(seed: Int, size: Int = QueensGame.DEFAULT_SIZE, locked: Int = DEFAULT_LOCKED): QueensPuzzle {
        require(size in QueensGame.MIN_SIZE..QueensGame.MAX_SIZE) { "a small board" }
        require(locked in 0 until size) { "something is left to place" }
        val random = Random(seed)
        val solution = QueensSolver.solutions(size).random(random)
        return QueensPuzzle(size, solution.shuffled(random).take(locked).toSet())
    }
}
