package com.mehmtcan.brainscroll.game.axissum

enum class AxisSumStatus { Playing, Won }

/** What a tap did. */
enum class AxisSumResult {
    /** Nothing happened: a cell that does not exist, or the game is over. */
    Ignored,

    /** The number was erased. */
    Erased,

    /** The number was brought back. */
    Restored,

    /** The tap made every row and column sum right. */
    Solved,
}

/** The picture of the board for the UI. Cells are numbered row by row from the top left. */
data class AxisSumState(
    val size: Int,
    val digits: List<Int>,
    /** True for a number that is erased now. */
    val erased: List<Boolean>,
    /** What the numbers that are left add up to in each row and column, and what they have to add up to. */
    val rowSums: List<Int>,
    val columnSums: List<Int>,
    val rowTargets: List<Int>,
    val columnTargets: List<Int>,
    /** Taps so far (a restart does not give them back). */
    val taps: Int,
    /** How many numbers the board needs erased, and how many are erased now. */
    val eraseGoal: Int,
    val erasedCount: Int,
    val status: AxisSumStatus,
    /** False while nothing is erased. */
    val canRestart: Boolean,
)

/**
 * The rules of axis sum, free of UI and time.
 *
 * A tap on a number erases it, a tap on an erased one brings it back. The board is won when the numbers that are left add up to
 * the sum of every row and every column. There is no losing; a restart brings every number back and the taps stay counted.
 */
class AxisSumGame(private val puzzle: AxisSumPuzzle) {
    private val size = puzzle.size
    private val erased = BooleanArray(size * size)
    private var taps = 0

    private fun rowSum(r: Int) = (0 until size).sumOf { c -> if (erased[r * size + c]) 0 else puzzle.digits[r * size + c] }
    private fun columnSum(c: Int) = (0 until size).sumOf { r -> if (erased[r * size + c]) 0 else puzzle.digits[r * size + c] }

    val isFinished: Boolean
        get() = (0 until size).all { rowSum(it) == puzzle.rowTargets[it] && columnSum(it) == puzzle.columnTargets[it] }

    val state: AxisSumState
        get() = AxisSumState(
            size = size,
            digits = puzzle.digits,
            erased = erased.toList(),
            rowSums = List(size) { rowSum(it) },
            columnSums = List(size) { columnSum(it) },
            rowTargets = puzzle.rowTargets,
            columnTargets = puzzle.columnTargets,
            taps = taps,
            eraseGoal = puzzle.eraseCount,
            erasedCount = erased.count { it },
            status = if (isFinished) AxisSumStatus.Won else AxisSumStatus.Playing,
            canRestart = !isFinished && erased.any { it },
        )

    fun tap(cell: Int): AxisSumResult {
        if (isFinished || cell !in erased.indices) return AxisSumResult.Ignored
        erased[cell] = !erased[cell]
        taps++
        return when {
            isFinished -> AxisSumResult.Solved
            erased[cell] -> AxisSumResult.Erased
            else -> AxisSumResult.Restored
        }
    }

    /** Brings every number back. The taps stay counted. Returns false if nothing was erased. */
    fun restart(): Boolean {
        if (!state.canRestart) return false
        erased.fill(false)
        return true
    }
}
