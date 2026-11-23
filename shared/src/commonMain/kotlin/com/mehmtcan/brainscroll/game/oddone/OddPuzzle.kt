package com.mehmtcan.brainscroll.game.oddone

import kotlin.random.Random

/** The rules a question may follow: four of its five numbers do, the odd one does not. [level] is the first level that uses the rule. */
enum class OddRule(val level: Int, private val test: (Int) -> Boolean) {
    Even(1, { it % 2 == 0 }),
    Odd(1, { it % 2 == 1 }),
    MultipleOf5(1, { it % 5 == 0 }),
    Below10(1, { it < 10 }),
    TwoDigits(1, { it >= 10 }),
    MultipleOf3(2, { it % 3 == 0 }),
    MultipleOf4(2, { it % 4 == 0 }),
    Prime(2, { isPrime(it) }),
    Square(2, { isSquare(it) }),
    MultipleOf7(3, { it % 7 == 0 }),
    DigitSumEven(3, { digitSum(it) % 2 == 0 }),
    Triangular(3, { it in TRIANGULAR }),
    Fibonacci(3, { it in FIBONACCI });

    fun holds(n: Int): Boolean = test(n)

    companion object {
        private val TRIANGULAR = setOf(1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78, 91)
        private val FIBONACCI = setOf(1, 2, 3, 5, 8, 13, 21, 34, 55, 89)

        private fun isPrime(n: Int): Boolean {
            if (n < 2) return false
            var i = 2
            while (i * i <= n) {
                if (n % i == 0) return false
                i++
            }
            return true
        }

        private fun isSquare(n: Int): Boolean {
            var k = 1
            while (k * k < n) k++
            return k * k == n
        }

        private fun digitSum(n: Int): Int = n.toString().sumOf { it - '0' }
    }
}

/** Five numbers, the one that does not follow the [rule] (at [oddIndex]) and the four that do. */
data class OddQuestion(val numbers: List<Int>, val oddIndex: Int, val rule: OddRule) {
    val odd: Int get() = numbers[oddIndex]
}

/**
 * A run of odd one out: [rounds] questions of one [level] (the level says which rules there are and how big the numbers are). Every
 * question comes from the [seed] and its number (see [question]), so a saved run needs only these numbers. The run is saved as text
 * (see [encode]).
 *
 * A question is made so that no other rule of the game singles out a different number: the player who finds a rule that fits four
 * numbers can trust that its odd one is the answer.
 */
class OddPuzzle(val level: Int, val rounds: Int, val seed: Int) {
    init {
        require(level in 1..3) { "level $level" }
        require(rounds in 1..MAX_ROUNDS) { "rounds $rounds" }
        require(seed >= 0) { "seed $seed" }
    }

    /** The question of round [round] (from 0). The same run always gives the same questions. */
    fun question(round: Int): OddQuestion {
        require(round in 0 until rounds) { "round $round" }
        val random = Random(seed.toLong() * 7_919L + round)
        val pool = (1..MAX_NUMBER[level - 1]).toList()
        val rules = OddRule.entries.filter { it.level <= level }
        repeat(TRIES) {
            val rule = rules[random.nextInt(rules.size)]
            val follows = pool.filter { rule.holds(it) }
            val breaks = pool.filter { !rule.holds(it) }
            if (follows.size < 4 || breaks.isEmpty()) return@repeat
            val four = follows.shuffled(random).take(4)
            val odd = breaks[random.nextInt(breaks.size)]
            val numbers = (four + odd).shuffled(random)
            val clash = OddRule.entries.any { other ->
                other != rule && numbers.filter { !other.holds(it) }.let { it.size == 1 && it[0] != odd }
            }
            if (!clash) return OddQuestion(numbers, numbers.indexOf(odd), rule)
        }
        return OddQuestion(listOf(2, 4, 6, 8, 9), 4, OddRule.Even)
    }

    /** The level, the number of rounds (hex) and the seed in base 36. */
    fun encode(): String = "$level${rounds.toString(16)}${seed.toString(36)}"

    override fun equals(other: Any?) = other is OddPuzzle && level == other.level && rounds == other.rounds && seed == other.seed

    override fun hashCode() = (31 * level + rounds) * 31 + seed

    companion object {
        const val MAX_ROUNDS = 15
        private const val TRIES = 500
        private val MAX_NUMBER = intArrayOf(30, 60, 99)

        /** Reads what [encode] wrote. Null if it is not a run (so a saved game of another kind starts a new one). */
        fun decode(text: String): OddPuzzle? {
            if (text.length < 3) return null
            val level = text[0].digitToIntOrNull() ?: return null
            val rounds = text[1].digitToIntOrNull(16) ?: return null
            val seed = text.substring(2).toIntOrNull(36) ?: return null
            return runCatching { OddPuzzle(level, rounds, seed) }.getOrNull()
        }
    }
}
