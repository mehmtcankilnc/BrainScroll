package com.mehmtcan.brainscroll.game.fruitmath

import com.mehmtcan.brainscroll.game.BitText

/** A piece of a line: a fruit (optionally squared) or an operation sign. */
sealed interface FruitToken {
    /** The fruit numbered [index] (0 until the number of fruits of the board), squared when [squared] (shown with a small 2). */
    data class Fruit(val index: Int, val squared: Boolean = false) : FruitToken

    /** An operation sign: `+`, `×` or `÷`. */
    data class Op(val symbol: Char) : FruitToken

    /** The fruit [base] to the power of the fruit [exponent] (the exponent is shown small, up and to the right of the base). */
    data class Power(val base: Int, val exponent: Int) : FruitToken
}

/** One line of the board: what is on its left, and what it comes to. */
data class FruitLine(val tokens: List<FruitToken>, val total: Int) {
    /** How many fruits are on the line. */
    val fruitCount: Int get() = tokens.count { it is FruitToken.Fruit }
}

/** How a line after the first reads. The first line is a [Sum] (so many of the first fruit) or [SquareOnly] (the first fruit squared). */
enum class FruitLineKind {
    /** Some of the fruit before and some of its own, added up: `A + A + B`. */
    Sum,

    /** The fruit before times its own: `A × B`. */
    Product,

    /** The fruit before divided by its own: `A ÷ B` (it divides without a rest). */
    Quotient,

    /** The fruit before squared, and its own added: `A² + B`. */
    SquarePlus,

    /** The first fruit squared: `A²`. Only on the first line. */
    SquareOnly,

    /** The fruit before (worth 2, 3 or 4) to the power of its own: `A^B`. */
    PowerOf,

    /** Its own fruit (worth 2, 3 or 4) to the power of the fruit before: `B^A`. */
    PowerBase,
}

/** How the last line reads. */
enum class FruitQuestionKind {
    /** Every fruit added up (one of them twice when there are three fruits). */
    SumAll,

    /** The first two fruits multiplied and the third added: `a × b + c`. */
    ProductPlus,

    /** The first fruit to the power of the second: `a^b`. */
    PowerOnly,
}

/** The last line, the one with the question mark. [kind] says how it reads; the fruits [a], [b] and [c] are the ones a product or a power uses. */
data class FruitQuestion(
    val kind: FruitQuestionKind,
    /** The fruit that is on the last line twice when all are added up (the other ones once); -1 for none. */
    val twice: Int,
    val a: Int,
    val b: Int,
    val c: Int,
)

/**
 * A board of fruit equations: a few kinds of fruit, each worth a whole number, and lines that say what some fruits come to. The player
 * works the value of each fruit out and types the answer of the last line, which has a ? where its total is.
 *
 * The fruits are chained: the first line has only the first fruit, every other line has the fruit before and its own, so each line gives
 * the value of the next fruit (see [readValues]) and there is one way to read the values. [fruits] says which of the eight fruits of the
 * game each one is (a number from 0 to 7, all different), [values] what each is worth. [kinds] is how each line reads and [counts] how many
 * of the fruit before and of its own a [FruitLineKind.Sum] line has (a first line only has the first number). [question] is the last line.
 *
 * [encode] is the saved text: the number of fruits, the fruits, the values, the lines and the question, six bits to a character: 13
 * characters for the biggest board.
 */
class FruitPuzzle(
    val fruits: List<Int>,
    val values: List<Int>,
    val kinds: List<FruitLineKind>,
    val counts: List<Pair<Int, Int>>,
    val question: FruitQuestion,
) {
    init {
        val n = fruits.size
        require(n in MIN_FRUITS..MAX_FRUITS) { "a few fruits" }
        require(values.size == n && values.all { it in 1..MAX_VALUE }) { "a value for every fruit" }
        require(fruits.all { it in 0 until KINDS } && fruits.toSet().size == n) { "different fruits" }
        require(kinds.size == n && counts.size == n) { "a kind and counts for every line" }
        require(kinds[0] == FruitLineKind.Sum || kinds[0] == FruitLineKind.SquareOnly) { "the first line is a sum or a square" }
        for (i in 1 until n) require(kinds[i] != FruitLineKind.SquareOnly) { "a square alone only opens the board" }
        require(counts[0].first in 1..MAX_FIRST_COUNT) { "a count for the first line" }
        for (i in 1 until n) {
            val (a, b) = counts[i]
            require(if (kinds[i] == FruitLineKind.Sum) a in 1..MAX_COUNT && b in 1..MAX_COUNT else a == 1 && b == 1) { "counts of line $i" }
            require(kinds[i] != FruitLineKind.Quotient || values[i - 1] % values[i] == 0) { "a quotient with no rest" }
            if (kinds[i] == FruitLineKind.PowerOf) require(canPower(values[i - 1], values[i])) { "a power of line $i" }
            if (kinds[i] == FruitLineKind.PowerBase) require(canPower(values[i], values[i - 1])) { "a power of line $i" }
        }
        val q = question
        when (q.kind) {
            FruitQuestionKind.ProductPlus -> require(listOf(q.a, q.b, q.c).all { it in 0 until n } && setOf(q.a, q.b, q.c).size == 3) { "three different fruits" }
            FruitQuestionKind.PowerOnly -> require(q.a in 0 until n && q.b in 0 until n && q.a != q.b && canPower(values[q.a], values[q.b])) { "a power to ask" }
            FruitQuestionKind.SumAll -> require(q.twice in -1 until n) { "a fruit of the last line" }
        }
    }

    val fruitCount: Int get() = fruits.size

    /** The lines above the question, in order. */
    val lines: List<FruitLine> = List(fruits.size) { i ->
        val v = values
        when (kinds[i]) {
            FruitLineKind.SquareOnly -> FruitLine(listOf(FruitToken.Fruit(0, squared = true)), v[0] * v[0])
            FruitLineKind.Product -> FruitLine(listOf(FruitToken.Fruit(i - 1), FruitToken.Op('×'), FruitToken.Fruit(i)), v[i - 1] * v[i])
            FruitLineKind.Quotient -> FruitLine(listOf(FruitToken.Fruit(i - 1), FruitToken.Op('÷'), FruitToken.Fruit(i)), v[i - 1] / v[i])
            FruitLineKind.SquarePlus -> FruitLine(listOf(FruitToken.Fruit(i - 1, squared = true), FruitToken.Op('+'), FruitToken.Fruit(i)), v[i - 1] * v[i - 1] + v[i])
            FruitLineKind.PowerOf -> FruitLine(listOf(FruitToken.Power(i - 1, i)), power(v[i - 1], v[i]))
            FruitLineKind.PowerBase -> FruitLine(listOf(FruitToken.Power(i, i - 1)), power(v[i], v[i - 1]))
            FruitLineKind.Sum -> {
                val parts = if (i == 0) listOf(0 to counts[0].first) else listOf((i - 1) to counts[i].first, i to counts[i].second)
                FruitLine(sumTokens(parts), parts.sumOf { (f, c) -> v[f] * c })
            }
        }
    }

    /** The last line. Its total is the answer. */
    val last: FruitLine = when (question.kind) {
        FruitQuestionKind.ProductPlus -> FruitLine(
            listOf(FruitToken.Fruit(question.a), FruitToken.Op('×'), FruitToken.Fruit(question.b), FruitToken.Op('+'), FruitToken.Fruit(question.c)),
            values[question.a] * values[question.b] + values[question.c],
        )
        FruitQuestionKind.PowerOnly -> FruitLine(listOf(FruitToken.Power(question.a, question.b)), power(values[question.a], values[question.b]))
        FruitQuestionKind.SumAll -> {
            val parts = fruits.indices.map { it to (if (it == question.twice) 2 else 1) }
            FruitLine(sumTokens(parts), parts.sumOf { (f, c) -> values[f] * c })
        }
    }

    val answer: Int get() = last.total

    init {
        require(answer in 1..MAX_ANSWER) { "an answer to type" }
        require(lines.all { it.total >= 1 }) { "lines that come to something" }
    }

    /**
     * The values of the fruits as the lines give them, one after the other (the first line gives the first fruit, each next line the next
     * fruit), or null if a line does not give a whole number. They are the [values] for a board that is made right.
     */
    fun readValues(): List<Int>? {
        val read = IntArray(fruits.size)
        val first = lines[0]
        read[0] = if (kinds[0] == FruitLineKind.SquareOnly) {
            val root = kotlin.math.sqrt(first.total.toDouble()).toInt()
            if (root * root != first.total) return null else root
        } else {
            if (first.total % counts[0].first != 0) return null else first.total / counts[0].first
        }
        for (i in 1 until fruits.size) {
            val t = lines[i].total
            val p = read[i - 1]
            val next = when (kinds[i]) {
                FruitLineKind.Sum -> {
                    val rest = t - p * counts[i].first
                    if (rest <= 0 || rest % counts[i].second != 0) return null else rest / counts[i].second
                }
                FruitLineKind.Product -> if (t % p != 0) return null else t / p
                FruitLineKind.Quotient -> if (t == 0 || p % t != 0) return null else p / t
                FruitLineKind.SquarePlus -> t - p * p
                // The base is known: the exponent is the one number it has to be raised to.
                FruitLineKind.PowerOf -> (2..MAX_VALUE).singleOrNull { power(p, it) == t } ?: return null
                // The exponent is known: the base is the one number (2, 3 or 4) that gives the total.
                FruitLineKind.PowerBase -> (2..MAX_BASE).singleOrNull { power(it, p) == t } ?: return null
                FruitLineKind.SquareOnly -> return null
            }
            if (next < 1) return null
            read[i] = next
        }
        return read.toList()
    }

    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(fruits.size - MIN_FRUITS, 2)
        fruits.forEach { writer.put(it, 3) }
        values.forEach { writer.put(it, 4) }
        for (i in fruits.indices) {
            writer.put(kinds[i].ordinal, 3)
            if (kinds[i] == FruitLineKind.Sum) {
                if (i == 0) writer.put(counts[0].first - 1, 2) else { writer.put(counts[i].first - 1, 1); writer.put(counts[i].second - 1, 1) }
            }
        }
        writer.put(question.kind.ordinal, 2)
        when (question.kind) {
            FruitQuestionKind.ProductPlus -> {
                writer.put(question.a, 3)
                writer.put(question.b, 3)
                writer.put(question.c, 3)
            }
            FruitQuestionKind.PowerOnly -> {
                writer.put(question.a, 3)
                writer.put(question.b, 3)
            }
            FruitQuestionKind.SumAll -> writer.put(question.twice + 1, 3)
        }
        return writer.text()
    }

    override fun equals(other: Any?) = other is FruitPuzzle && other.encode() == encode()

    override fun hashCode() = encode().hashCode()

    companion object {
        const val MIN_FRUITS = 3
        const val MAX_FRUITS = 5

        /** The kinds of fruit there are. */
        const val KINDS = 8
        const val MAX_VALUE = 9
        const val MAX_FIRST_COUNT = 4
        const val MAX_COUNT = 2

        /** The biggest answer: three digits. */
        const val MAX_ANSWER = 999

        /** The biggest base of a power: a fruit worth 2, 3 or 4 is raised to the power of another. */
        const val MAX_BASE = 4

        /** [base] to the power of [exponent], with no overflow (a number far above any answer when it is too big). */
        internal fun power(base: Int, exponent: Int): Int {
            var result = 1L
            repeat(exponent) {
                result *= base
                if (result > 1_000_000L) return 1_000_000
            }
            return result.toInt()
        }

        /** True if a fruit worth [base] can be raised to the power of a fruit worth [exponent] on a line: a base from 2 to 4, an exponent of 2 or more and a result of three digits. */
        internal fun canPower(base: Int, exponent: Int): Boolean = base in 2..MAX_BASE && exponent >= 2 && power(base, exponent) <= MAX_ANSWER

        private fun sumTokens(parts: List<Pair<Int, Int>>): List<FruitToken> {
            val out = ArrayList<FruitToken>()
            for ((fruit, count) in parts) repeat(count) {
                if (out.isNotEmpty()) out += FruitToken.Op('+')
                out += FruitToken.Fruit(fruit)
            }
            return out
        }

        /** Null if [text] is not a board that [encode] wrote. */
        fun decode(text: String): FruitPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val n = (reader.take(2) ?: return null) + MIN_FRUITS
            if (n > MAX_FRUITS) return null
            val fruits = List(n) { reader.take(3) ?: return null }
            val values = List(n) { reader.take(4) ?: return null }
            val kinds = ArrayList<FruitLineKind>()
            val counts = ArrayList<Pair<Int, Int>>()
            for (i in 0 until n) {
                val kind = FruitLineKind.entries.getOrNull(reader.take(3) ?: return null) ?: return null
                kinds += kind
                counts += when {
                    kind != FruitLineKind.Sum -> 1 to 1
                    i == 0 -> ((reader.take(2) ?: return null) + 1) to 1
                    else -> ((reader.take(1) ?: return null) + 1) to ((reader.take(1) ?: return null) + 1)
                }
            }
            val questionKind = FruitQuestionKind.entries.getOrNull(reader.take(2) ?: return null) ?: return null
            val question = when (questionKind) {
                FruitQuestionKind.ProductPlus -> FruitQuestion(questionKind, -1, reader.take(3) ?: return null, reader.take(3) ?: return null, reader.take(3) ?: return null)
                FruitQuestionKind.PowerOnly -> FruitQuestion(questionKind, -1, reader.take(3) ?: return null, reader.take(3) ?: return null, 0)
                FruitQuestionKind.SumAll -> FruitQuestion(questionKind, (reader.take(3) ?: return null) - 1, 0, 0, 0)
            }
            return try {
                FruitPuzzle(fruits, values, kinds, counts, question)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
