package com.mehmtcan.brainscroll.game.greaterless

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards: a full square is made at random with a sign between every two cells next to each other, then the given numbers and
 * the signs are taken away one by one in a random order, and one stays away only when the board can still be finished the way a player
 * does it (by the rules, and on the harder levels also by trying a number and seeing the rules break), which also means it has one way
 * to be filled. It stops when the level's numbers of given numbers and signs are reached (or when no more can go). The board only
 * depends on the seed, but it is saved as the board itself, so a later change of this does not change a saved game.
 */
object GlGenerator {
    fun generate(seed: Int, difficulty: Difficulty): GlPuzzle = generate(seed, GameLevels.greaterLess(difficulty))

    fun generate(seed: Int, shape: GameLevels.GlShape): GlPuzzle {
        // A board that is too rich to be saved is rare; the next seed gives another one.
        for (attempt in 0 until 20) {
            val puzzle = make(Random(seed + attempt * 7919), shape)
            if (puzzle.canBeSaved) return puzzle
        }
        error("no board that can be saved")
    }

    private fun make(random: Random, shape: GameLevels.GlShape): GlPuzzle {
        val n = shape.size
        val solution = GlSolver.randomSquare(random, n)
        val given = solution.copyOf()
        val right = IntArray(n * n) { cell -> if (cell % n < n - 1) signOf(solution[cell], solution[cell + 1]) else 0 }
        val down = IntArray(n * n) { cell -> if (cell / n < n - 1) signOf(solution[cell], solution[cell + n]) else 0 }
        var givenLeft = n * n
        var signsLeft = right.count { it > 0 } + down.count { it > 0 }
        // What can be taken away: a given number (kind 0) or a sign to the right (1) or below (2) of a cell.
        val items = (0 until n * n).flatMap { cell ->
            buildList {
                add(0 to cell)
                if (right[cell] > 0) add(1 to cell)
                if (down[cell] > 0) add(2 to cell)
            }
        }.shuffled(random)
        for ((kind, cell) in items) {
            if (givenLeft <= shape.given && signsLeft <= shape.signs) break
            if (kind == 0 && givenLeft <= shape.given) continue
            if (kind != 0 && signsLeft <= shape.signs) continue
            val old = when (kind) {
                0 -> given[cell]
                1 -> right[cell]
                else -> down[cell]
            }
            when (kind) {
                0 -> given[cell] = 0
                1 -> right[cell] = 0
                else -> down[cell] = 0
            }
            if (GlSolver.solveByLogic(GlPuzzle(n, given.toList(), right.toList(), down.toList()), shape.lookahead) != null) {
                if (kind == 0) givenLeft-- else signsLeft--
            } else {
                when (kind) {
                    0 -> given[cell] = old
                    1 -> right[cell] = old
                    else -> down[cell] = old
                }
            }
        }
        return GlPuzzle(n, given.toList(), right.toList(), down.toList())
    }

    private fun signOf(a: Int, b: Int) = if (a < b) GlPuzzle.LESS else GlPuzzle.MORE
}
