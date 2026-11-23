package com.mehmtcan.brainscroll.game.zerosones

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards: a full board that follows the rules is made at random, then its cells are taken away one by one in a random order,
 * and a cell stays away only when the board can still be finished the way a player does it (by the rules, and on the harder levels also
 * by trying a digit and seeing the rules break), which also means it has one way to be filled. It stops when the level's number of
 * given cells is reached (or when no more can go). The board only depends on the seed, but it is saved as the board itself, so a later
 * change of this does not change a saved game.
 */
object BinGenerator {
    fun generate(seed: Int, difficulty: Difficulty): BinPuzzle = generate(seed, GameLevels.zerosOnes(difficulty))

    fun generate(seed: Int, shape: GameLevels.BinShape): BinPuzzle {
        val random = Random(seed)
        val n = shape.size
        val solution = BinSolver.randomSolution(random, n)
        val given = solution.copyOf()
        var left = n * n
        for (cell in (0 until n * n).shuffled(random)) {
            if (left <= shape.given) break
            val digit = given[cell]
            given[cell] = -1
            if (BinSolver.solveByLogic(n, given, shape.lookahead) != null) left-- else given[cell] = digit
        }
        return BinPuzzle(n, given.toList())
    }
}
