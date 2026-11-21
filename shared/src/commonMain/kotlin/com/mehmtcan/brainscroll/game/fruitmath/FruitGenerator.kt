package com.mehmtcan.brainscroll.game.fruitmath

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards: the fruits are picked at random with different values, and the lines chain one to the next (see [FruitPuzzle]), each
 * one of the kinds the level has (a line is only given a kind that works out for the two fruits: a product that is not too big, a
 * quotient that divides without a rest, a square that is not too big). The board is kept when it has as many lines that are not plain
 * sums as the level wants, no line comes to more than the level allows and the answer is not too small. The board only depends on the
 * seed, but it is saved as the board itself, so a later change of this does not change a saved game.
 */
object FruitGenerator {
    private const val TRIES = 6_000

    fun generate(seed: Int, difficulty: Difficulty): FruitPuzzle = generate(seed, GameLevels.fruitMath(difficulty))

    fun generate(seed: Int, shape: GameLevels.FruitShape): FruitPuzzle {
        val random = Random(seed)
        repeat(TRIES) {
            val n = shape.fruits
            val fruits = (0 until FruitPuzzle.KINDS).shuffled(random).take(n)
            val values = (1..shape.maxValue).shuffled(random).take(n)
            val kinds = ArrayList<FruitLineKind>()
            val counts = ArrayList<Pair<Int, Int>>()
            // The first line: so many of the first fruit, or the first fruit squared.
            val opening = shape.kinds.filter { it == FruitLineKind.Sum || it == FruitLineKind.SquareOnly }
            val first = opening.random(random)
            kinds += first
            counts += (if (first == FruitLineKind.Sum) 2 + random.nextInt(3) else 1) to 1
            for (i in 1 until n) {
                val before = values[i - 1]
                val own = values[i]
                val fits = shape.kinds.filter {
                    when (it) {
                        FruitLineKind.Sum -> true
                        FruitLineKind.Product -> before * own <= shape.maxTotal
                        FruitLineKind.Quotient -> before % own == 0
                        FruitLineKind.SquarePlus -> before * before + own <= shape.maxTotal
                        FruitLineKind.SquareOnly -> false
                        FruitLineKind.PowerOf -> FruitPuzzle.canPower(before, own) && FruitPuzzle.power(before, own) <= shape.maxTotal
                        FruitLineKind.PowerBase -> FruitPuzzle.canPower(own, before) && FruitPuzzle.power(own, before) <= shape.maxTotal
                    }
                }
                // Lines that are not sums are what makes a harder board: they are picked more often than the sum.
                val others = fits.filter { it != FruitLineKind.Sum }
                val kind = if (others.isNotEmpty() && random.nextInt(100) < 70) others.random(random) else FruitLineKind.Sum
                kinds += kind
                counts += if (kind == FruitLineKind.Sum) (1 + random.nextInt(2)) to (1 + random.nextInt(2)) else 1 to 1
            }
            if (kinds.count { it != FruitLineKind.Sum } < shape.special) return@repeat
            if (kinds.count { it == FruitLineKind.PowerOf || it == FruitLineKind.PowerBase } < shape.minPowers) return@repeat
            val question = when (shape.questions.toList().random(random)) {
                FruitQuestionKind.ProductPlus -> {
                    val three = (0 until n).shuffled(random).take(3)
                    FruitQuestion(FruitQuestionKind.ProductPlus, -1, three[0], three[1], three[2])
                }
                FruitQuestionKind.PowerOnly -> {
                    // A base worth 2, 3 or 4 and another fruit as its exponent, if there is such a pair.
                    val pairs = (0 until n).flatMap { a -> (0 until n).filter { it != a }.map { b -> a to b } }.filter { (a, b) -> FruitPuzzle.canPower(values[a], values[b]) }
                    if (pairs.isEmpty()) return@repeat
                    val (a, b) = pairs.random(random)
                    FruitQuestion(FruitQuestionKind.PowerOnly, -1, a, b, 0)
                }
                FruitQuestionKind.SumAll -> FruitQuestion(FruitQuestionKind.SumAll, if (n == 3) random.nextInt(n) else -1, 0, 0, 0)
            }
            val puzzle = try {
                FruitPuzzle(fruits, values, kinds, counts, question)
            } catch (e: IllegalArgumentException) {
                return@repeat
            }
            if (puzzle.lines.any { it.total > shape.maxTotal } || puzzle.answer > shape.maxTotal || puzzle.answer < shape.minAnswer) return@repeat
            return puzzle
        }
        error("no board found")
    }
}
