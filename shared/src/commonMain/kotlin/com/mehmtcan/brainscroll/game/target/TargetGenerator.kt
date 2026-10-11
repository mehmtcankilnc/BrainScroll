package com.mehmtcan.brainscroll.game.target

import kotlin.random.Random

/** The four operations. [symbol] is what is saved; the screen shows × and ÷. */
enum class Op(val symbol: Char) {
    Add('+'),
    Sub('-'),
    Mul('*'),
    Div('/'),
    ;

    /**
     * [a] op [b], or null when the result is not allowed: it must be a whole number above zero (a subtraction that
     * reaches zero or goes below, or a division that leaves a rest, is not a move).
     */
    fun apply(a: Int, b: Int): Int? = when (this) {
        Add -> a + b
        Sub -> if (a > b) a - b else null
        Mul -> a * b
        Div -> if (b != 0 && a % b == 0) a / b else null
    }

    companion object {
        fun fromSymbol(symbol: Char): Op? = entries.firstOrNull { it.symbol == symbol }
    }
}

/** A puzzle: the starting numbers, the target, and the fewest operations that reach it. */
class TargetPuzzle(val numbers: List<Int>, val target: Int, val par: Int)

/**
 * Makes the puzzles of the target number game (docs/decisions.md "Hedef sayı").
 *
 * The idea: draw the numbers first, then work out by a full search every value that can be made from them, and pick
 * the target from those. So a target is always reachable, and the search also gives the exact fewest operations that
 * reach it (the "par"). The target is a two digit number that is not one of the starting numbers and needs at least
 * [minSteps] operations, so it is never one tap away. The puzzle only depends on the seed (and [minSteps]), so it can
 * be made again, but it is saved as numbers and target, so a later change of this algorithm does not change a saved game.
 */
object TargetGenerator {
    const val NUMBER_COUNT = 4
    const val MIN_TARGET = 10
    const val MAX_TARGET = 99

    /** The target needs at least this many operations. Higher is harder (3 means all four numbers are used). */
    const val DEFAULT_MIN_STEPS = 2

    /** How many draws of numbers are tried before the rule is relaxed (a draw with no fitting target is rare). */
    private const val MAX_DRAWS = 200

    fun generate(seed: Int, minSteps: Int = DEFAULT_MIN_STEPS): TargetPuzzle {
        val random = Random(seed)
        var steps = minSteps
        while (true) {
            repeat(MAX_DRAWS) {
                val numbers = List(NUMBER_COUNT) { random.nextInt(1, 10) }
                val best = reachable(numbers)
                val candidates = best.filter { (value, depth) ->
                    value in MIN_TARGET..MAX_TARGET && depth >= steps && value !in numbers
                }.keys.sorted()
                if (candidates.isNotEmpty()) {
                    val target = candidates[random.nextInt(candidates.size)]
                    return TargetPuzzle(numbers, target, best.getValue(target))
                }
            }
            // Almost never reached: with this few operations required there is always a draw that fits.
            check(steps > 1) { "no puzzle found" }
            steps--
        }
    }

    /** The fewest operations that make [target] from [numbers] (not all numbers have to be used), or null if it cannot be made. */
    fun minSteps(numbers: List<Int>, target: Int): Int? = reachable(numbers)[target]?.takeIf { target !in numbers }

    /**
     * Every value that can be made from [numbers] with the allowed moves, and the fewest operations that make it. The
     * numbers themselves are not in the result. A breadth first search over what is left on the table after each move.
     */
    fun reachable(numbers: List<Int>): Map<Int, Int> {
        val best = mutableMapOf<Int, Int>()
        val seen = mutableSetOf(numbers.sorted())
        var frontier = listOf(numbers.sorted())
        var depth = 0
        while (frontier.isNotEmpty()) {
            depth++
            val next = mutableListOf<List<Int>>()
            for (state in frontier) {
                for (i in state.indices) for (j in state.indices) {
                    if (i == j) continue
                    for (op in Op.entries) {
                        // Addition and multiplication do not care about the order, so each pair is tried once.
                        if ((op == Op.Add || op == Op.Mul) && i > j) continue
                        val result = op.apply(state[i], state[j]) ?: continue
                        if (result !in best) best[result] = depth
                        val rest = state.filterIndexed { index, _ -> index != i && index != j } + result
                        val key = rest.sorted()
                        if (seen.add(key)) next += key
                    }
                }
            }
            frontier = next
        }
        return best
    }
}
