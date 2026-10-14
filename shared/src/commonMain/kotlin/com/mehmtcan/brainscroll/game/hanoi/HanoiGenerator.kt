package com.mehmtcan.brainscroll.game.hanoi

import kotlin.random.Random

/** A puzzle: for every disk from the smallest the tower it starts on, the goal tower and the fewest moves that solve it. */
class HanoiPuzzle(val start: IntArray, val goal: Int, val par: Int)

/**
 * Makes the boards of the tower of Hanoi (docs/yeni-oyunlar-plan.md): the disks start spread over the towers at random
 * (every spread is a legal position), the goal tower is random too, and the fewest moves come from [HanoiSolver]. A board
 * that needs fewer than [DEFAULT_MIN_PAR] moves is too easy to be worth a page and is made again. The board only depends
 * on the seed, but it is saved as the position itself, so a later change of this algorithm does not change a saved game.
 */
object HanoiGenerator {
    const val DEFAULT_DISKS = 4
    const val DEFAULT_MIN_PAR = 5

    /** How many positions are tried before settling for the last one (it never comes to that for four disks). */
    private const val MAX_ATTEMPTS = 1_000

    fun generate(seed: Int, disks: Int = DEFAULT_DISKS, minPar: Int = DEFAULT_MIN_PAR): HanoiPuzzle {
        require(disks in 2..HanoiGame.MAX_DISKS) { "a few disks" }
        val random = Random(seed)
        var last: HanoiPuzzle? = null
        repeat(MAX_ATTEMPTS) {
            val start = IntArray(disks) { random.nextInt(HanoiGame.TOWERS) }
            val goal = random.nextInt(HanoiGame.TOWERS)
            val par = HanoiSolver.distance(start, goal)
            last = HanoiPuzzle(start, goal, par)
            if (par >= minPar) return last!!
        }
        return last!!
    }
}
