package com.mehmtcan.brainscroll.game.matchsticks

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/** A matchstick sum that is wrong, and the one move that puts it right. */
class MatchBoard(val puzzle: MatchPuzzle, val solution: MatchMove)

/**
 * Makes the sums: a right sum is picked, one stick of it is moved somewhere else, and the result is kept when it still reads as a
 * sum, is wrong, and exactly one move of one stick puts it right (so there is a single answer, and it is the way back). The level
 * says whether that move stays inside a symbol and whether the result has two digits. The board only depends on the seed, but it is
 * saved as the sum itself, so a later change of this does not change a saved game.
 */
object MatchGenerator {
    private const val TRIES = 40_000

    fun generate(seed: Int, difficulty: Difficulty): MatchBoard = generate(seed, GameLevels.matchsticks(difficulty))

    fun generate(seed: Int, shape: GameLevels.MatchShape): MatchBoard {
        val random = Random(seed)
        repeat(TRIES) {
            val right = rightSum(random, shape.twoDigitResult)
            val away = MatchRules.moves(right).random(random)
            val cells = MatchRules.apply(right, away)
            if (!MatchRules.isValid(cells) || MatchRules.isTrue(cells)) return@repeat
            val wins = MatchRules.winningMoves(cells)
            if (wins.size != 1) return@repeat
            val solution = wins.single()
            if (MatchRules.staysInSymbol(solution) != shape.sameSymbol) return@repeat
            return MatchBoard(MatchPuzzle(cells), solution)
        }
        error("no sum found")
    }

    /** A right sum with numbers from 1 to 9 on its left side: `A + B = C`, `A - B = C`, or (two digits) `A + B = CD` with the result from 10 to 18. */
    private fun rightSum(random: Random, twoDigits: Boolean): List<Int> {
        while (true) {
            // No zero on the left side: `0 + 6 = 6` is no puzzle.
            val first = 1 + random.nextInt(9)
            val second = 1 + random.nextInt(9)
            val plus = twoDigits || random.nextBoolean()
            val result = if (plus) first + second else first - second
            if (result < 0 || (twoDigits && result < 10) || (!twoDigits && result > 9)) continue
            return MatchRules.cellsOf(first, plus, second, result)
        }
    }
}
