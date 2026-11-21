package com.mehmtcan.brainscroll.game.fruitmath

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FruitMathTest {

    private val sum = FruitLineKind.Sum

    // Three fruits worth 2, 3 and 4. Lines: three of the first (6), the first times the second (6), the second squared and the third (13).
    private val small = FruitPuzzle(
        fruits = listOf(0, 1, 2),
        values = listOf(2, 3, 4),
        kinds = listOf(sum, FruitLineKind.Product, FruitLineKind.SquarePlus),
        counts = listOf(3 to 1, 1 to 1, 1 to 1),
        question = FruitQuestion(FruitQuestionKind.SumAll, twice = 1, a = 0, b = 0, c = 0),
    )

    // Fruits worth 6, 3 and 2. Lines: the first squared (36), the first divided by the second (2), one of the second and two of the third (7).
    private val big = FruitPuzzle(
        fruits = listOf(4, 5, 6),
        values = listOf(6, 3, 2),
        kinds = listOf(FruitLineKind.SquareOnly, FruitLineKind.Quotient, sum),
        counts = listOf(1 to 1, 1 to 1, 1 to 2),
        question = FruitQuestion(FruitQuestionKind.ProductPlus, twice = -1, a = 0, b = 1, c = 2),
    )

    @Test
    fun theLinesAddUpAndTheLastOneIsTheAnswer() {
        assertEquals(listOf(6, 6, 13), small.lines.map { it.total })
        assertEquals(
            listOf(FruitToken.Fruit(0), FruitToken.Op('×'), FruitToken.Fruit(1)),
            small.lines[1].tokens,
        )
        assertEquals(
            listOf(FruitToken.Fruit(1, squared = true), FruitToken.Op('+'), FruitToken.Fruit(2)),
            small.lines[2].tokens,
        )
        // One of every fruit and a second of the middle one: 2 + 3 + 4 + 3.
        assertEquals(12, small.answer)
        assertEquals(4, small.last.fruitCount)
    }

    @Test
    fun squaresQuotientsAndAProductOnTheLastLineAreWorkedOut() {
        assertEquals(listOf(36, 2, 3 + 2 * 2), big.lines.map { it.total })
        assertEquals(listOf(FruitToken.Fruit(0, squared = true)), big.lines[0].tokens)
        // The last line: the first times the second, and the third added: 6 × 3 + 2.
        assertEquals(20, big.answer)
        assertEquals(listOf(FruitToken.Fruit(0), FruitToken.Op('×'), FruitToken.Fruit(1), FruitToken.Op('+'), FruitToken.Fruit(2)), big.last.tokens)
    }

    // Fruits worth 2, 3 and 4. Lines: three of the first (6); the first to the power of the second (2^3 = 8); the third to the power of the second (4^3 = 64).
    private val powers = FruitPuzzle(
        fruits = listOf(0, 1, 2),
        values = listOf(2, 3, 4),
        kinds = listOf(sum, FruitLineKind.PowerOf, FruitLineKind.PowerBase),
        counts = listOf(3 to 1, 1 to 1, 1 to 1),
        question = FruitQuestion(FruitQuestionKind.PowerOnly, -1, 2, 0, 0),
    )

    @Test
    fun aFruitCanBeRaisedToThePowerOfAnotherOnALineAndOnTheLastLine() {
        assertEquals(listOf(6, 8, 64), powers.lines.map { it.total })
        assertEquals(listOf(FruitToken.Power(0, 1)), powers.lines[1].tokens, "the first fruit, the second as its exponent")
        assertEquals(listOf(FruitToken.Power(2, 1)), powers.lines[2].tokens, "the third fruit is the base here")
        // The last line asks the third fruit (4) to the power of the first (2): 16.
        assertEquals(16, powers.answer)
        assertEquals(powers.values, powers.readValues())
        assertEquals(powers, FruitPuzzle.decode(powers.encode()))
    }

    @Test
    fun aPowerThatCannotBeReadIsRefused() {
        fun refused(block: () -> FruitPuzzle) = runCatching(block).isFailure
        val sumAll = FruitQuestion(FruitQuestionKind.SumAll, 1, 0, 0, 0)
        // The base has to be 2, 3 or 4: 5 to the power of 2 is no line.
        assertTrue(refused { FruitPuzzle(listOf(0, 1, 2), listOf(5, 2, 4), listOf(sum, FruitLineKind.PowerOf, sum), listOf(3 to 1, 1 to 1, 1 to 1), sumAll) })
        // The exponent has to be 2 or more.
        assertTrue(refused { FruitPuzzle(listOf(0, 1, 2), listOf(2, 1, 4), listOf(sum, FruitLineKind.PowerOf, sum), listOf(3 to 1, 1 to 1, 1 to 1), sumAll) })
        // A power that is more than three digits: 4 to the power of 9 and 3 to the power of 9 are too big.
        assertTrue(refused { FruitPuzzle(listOf(0, 1, 2), listOf(4, 9, 2), listOf(sum, FruitLineKind.PowerOf, sum), listOf(3 to 1, 1 to 1, 1 to 1), sumAll) })
        // The question may not ask a power of a base above 4, or a fruit to its own power.
        assertTrue(refused { FruitPuzzle(listOf(0, 1, 2), listOf(2, 3, 4), listOf(sum, sum, sum), listOf(3 to 1, 1 to 1, 1 to 1), FruitQuestion(FruitQuestionKind.PowerOnly, -1, 1, 1, 0)) })
    }

    @Test
    fun theLinesGiveTheValuesOfTheFruitsOneAfterTheOther() {
        assertEquals(small.values, small.readValues())
        assertEquals(big.values, big.readValues())
    }

    @Test
    fun aBoardThatDoesNotWorkOutIsRefused() {
        fun refused(block: () -> FruitPuzzle) = runCatching(block).isFailure
        val twelve = FruitQuestion(FruitQuestionKind.SumAll, -1, 0, 0, 0)
        assertTrue(refused { FruitPuzzle(listOf(0, 1, 2), listOf(7, 3, 2), listOf(FruitLineKind.SquareOnly, FruitLineKind.Quotient, sum), listOf(1 to 1, 1 to 1, 1 to 1), twelve) }, "7 does not divide by 3")
        assertTrue(refused { FruitPuzzle(listOf(0, 0, 2), small.values, small.kinds, small.counts, small.question) }, "the same fruit twice")
        assertTrue(refused { FruitPuzzle(small.fruits, small.values, listOf(sum, FruitLineKind.SquareOnly, sum), small.counts, small.question) }, "a lone square on a later line")
        assertTrue(refused { FruitPuzzle(small.fruits, small.values, listOf(FruitLineKind.Product, sum, sum), small.counts, small.question) }, "the first line is a sum or a square")
        assertTrue(refused { FruitPuzzle(small.fruits, small.values, small.kinds, small.counts, FruitQuestion(FruitQuestionKind.ProductPlus, -1, 0, 0, 1)) }, "a product of a fruit with itself")
        assertTrue(refused { FruitPuzzle(small.fruits, listOf(2, 3, 0), small.kinds, small.counts, small.question) }, "a fruit worth nothing")
    }

    @Test
    fun theBoardIsSavedAsTextWithinTheLimitAndReadBack() {
        assertEquals(small, FruitPuzzle.decode(small.encode()))
        assertEquals(big, FruitPuzzle.decode(big.encode()))
        for (level in Difficulty.entries) for (seed in 0 until 80) {
            val puzzle = FruitGenerator.generate(seed, level)
            val text = puzzle.encode()
            assertTrue(text.length <= 32, "$level $text is ${text.length} long")
            assertEquals(puzzle, FruitPuzzle.decode(text))
        }
        assertNull(FruitPuzzle.decode("!!"))
        assertNull(FruitPuzzle.decode(""))
    }

    @Test
    fun everyGeneratedBoardHasOneReadingOfItsValuesAndKeepsToItsLevel() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.fruitMath(level)
            for (seed in 0 until 250) {
                val p = FruitGenerator.generate(seed, level)
                assertEquals(shape.fruits, p.fruitCount)
                assertEquals(p.values, p.readValues(), "$level seed $seed: the lines give the values back")
                assertEquals(p.fruitCount, p.values.toSet().size, "the fruits are worth different amounts")
                assertTrue(p.values.all { it in 1..shape.maxValue })
                assertTrue(p.lines.all { it.total in 1..shape.maxTotal }, "$level seed $seed")
                assertTrue(p.answer in shape.minAnswer..shape.maxTotal, "$level seed $seed answer ${p.answer}")
                assertTrue(p.kinds.all { it in shape.kinds }, "$level seed $seed")
                assertTrue(p.kinds.count { it != FruitLineKind.Sum } >= shape.special, "$level seed $seed has enough lines that are not sums")
                assertTrue(p.question.kind in shape.questions, "$level seed $seed")
            }
        }
    }

    @Test
    fun theLevelsDifferInTheirOperations() {
        val easy = (0 until 100).map { FruitGenerator.generate(it, Difficulty.Easy) }
        assertTrue(easy.all { p -> p.kinds.all { it == FruitLineKind.Sum } }, "easy is sums only")
        val medium = (0 until 100).map { FruitGenerator.generate(it, Difficulty.Medium) }
        assertTrue(medium.any { p -> FruitLineKind.Product in p.kinds }, "medium has products")
        assertTrue(medium.all { p -> p.kinds.none { it == FruitLineKind.Quotient || it == FruitLineKind.SquarePlus || it == FruitLineKind.SquareOnly } })
        val hard = (0 until 200).map { FruitGenerator.generate(it, Difficulty.Hard) }
        val used = hard.flatMap { it.kinds }.toSet()
        assertTrue(FruitLineKind.Quotient in used && FruitLineKind.SquarePlus in used && FruitLineKind.SquareOnly in used && FruitLineKind.Product in used, "hard has them all: $used")
        assertTrue(hard.any { it.question.kind == FruitQuestionKind.ProductPlus } && hard.any { it.question.kind == FruitQuestionKind.PowerOnly }, "hard asks a product and a power now and then")
        assertTrue(hard.all { it.fruitCount == 4 }, "no more than four fruits")
        assertTrue(hard.all { p -> p.kinds.any { it == FruitLineKind.PowerOf || it == FruitLineKind.PowerBase } }, "every hard board has a power line")
        assertTrue(FruitLineKind.PowerOf in used && FruitLineKind.PowerBase in used, "both ways of a power are used: $used")
    }

    @Test
    fun theSameSeedMakesTheSameBoard() {
        for (level in Difficulty.entries) assertEquals(FruitGenerator.generate(5, level), FruitGenerator.generate(5, level))
    }

    @Test
    fun aWrongAnswerIsCountedAndTheRightOneWins() {
        val game = FruitGame(small)
        assertEquals(FruitResult.Wrong, game.submit(13))
        assertEquals(FruitResult.Wrong, game.submit(13), "the same wrong answer counts again")
        assertEquals(2, game.state.mistakes)
        assertEquals(FruitResult.Ignored, game.submit(-1), "not an answer")
        assertEquals(FruitResult.Ignored, game.submit(5_000))
        assertEquals(FruitStatus.Playing, game.state.status)
        assertEquals(FruitResult.Solved, game.submit(12))
        assertEquals(FruitStatus.Won, game.state.status)
        assertTrue(game.isFinished)
        assertEquals(FruitResult.Ignored, game.submit(7), "the game is over")
        assertEquals(2, game.state.mistakes)
    }

    @Test
    fun aSavedGameIsRestoredOnlyIfItCouldHaveHappened() {
        val game = FruitGame(small)
        assertTrue(game.restore(3))
        assertEquals(3, game.state.mistakes)
        assertFalse(FruitGame(small).restore(-1))
    }
}
