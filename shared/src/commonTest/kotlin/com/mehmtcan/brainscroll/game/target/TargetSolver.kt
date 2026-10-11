package com.mehmtcan.brainscroll.game.target

/** One move as the game takes it: the ids of the two numbers and the operation. */
data class Move(val first: Int, val op: Op, val second: Int)

/**
 * A plain iterative deepening search, for tests only. It plays the same ids the game hands out (the starting numbers
 * are 0, 1, 2... and each result gets the next one), so what it returns can be given to the game as it is. It is
 * written apart from [TargetGenerator] on purpose: the tests compare the two.
 */
object TargetSolver {

    /** The moves that reach [target] from [numbers] in the fewest operations, or null if it cannot be done. */
    fun solve(numbers: List<Int>, target: Int): List<Move>? {
        for (depth in 1 until numbers.size) {
            val tokens = numbers.mapIndexed { index, value -> Token(index, value) }
            search(tokens, numbers.size, target, depth, ArrayList())?.let { return it }
        }
        return null
    }

    private fun search(tokens: List<Token>, nextId: Int, target: Int, left: Int, path: MutableList<Move>): List<Move>? {
        for (a in tokens) for (b in tokens) {
            if (a.id == b.id) continue
            for (op in Op.entries) {
                val result = op.apply(a.value, b.value) ?: continue
                path += Move(a.id, op, b.id)
                if (result == target) return path.toList()
                if (left > 1) {
                    val rest = tokens.filter { it.id != a.id && it.id != b.id } + Token(nextId, result)
                    search(rest, nextId + 1, target, left - 1, path)?.let { return it }
                }
                path.removeAt(path.size - 1)
            }
        }
        return null
    }

    /** Every value any sequence of operations can make from [numbers], with the fewest operations, by plain recursion. */
    fun bruteReachable(numbers: List<Int>): Map<Int, Int> {
        val best = mutableMapOf<Int, Int>()
        fun walk(values: List<Int>, depth: Int) {
            for (i in values.indices) for (j in values.indices) {
                if (i == j) continue
                for (op in Op.entries) {
                    val result = op.apply(values[i], values[j]) ?: continue
                    if (best[result]?.let { it > depth + 1 } != false) best[result] = depth + 1
                    walk(values.filterIndexed { index, _ -> index != i && index != j } + result, depth + 1)
                }
            }
        }
        walk(numbers, 0)
        return best
    }
}
