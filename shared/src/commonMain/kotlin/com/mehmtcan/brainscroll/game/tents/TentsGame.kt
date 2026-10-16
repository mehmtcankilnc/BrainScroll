package com.mehmtcan.brainscroll.game.tents

enum class TentsStatus { Playing, Won }

/** What a tap did. */
enum class TentsResult {
    /** Nothing happened: a tree, a cell that does not exist, or the game is over. */
    Ignored,

    /** The cell now holds a tent. */
    Tent,

    /** The cell now holds the grass mark (the player's note that there is no tent here). */
    Grass,

    /** The cell is empty again. */
    Cleared,

    /** The tent put there finished the board. */
    Solved,
}

/** What a cell shows. */
object TentsMark {
    const val EMPTY = 0
    const val TENT = 1
    const val GRASS = 2
}

/** The picture of the board for the UI. Cells are numbered row by row from the top left. */
data class TentsState(
    val size: Int,
    val trees: List<Boolean>,
    /** Per cell: [TentsMark.EMPTY], [TentsMark.TENT] or [TentsMark.GRASS]. A tree cell is always empty. */
    val marks: List<Int>,
    /** True for every tent that breaks a rule (see [TentsRules.conflicts]). */
    val conflicts: List<Boolean>,
    /** How many tents each row and each column must hold. */
    val rowTargets: List<Int>,
    val columnTargets: List<Int>,
    /** How many tents each row and column holds now. */
    val rowCounts: List<Int>,
    val columnCounts: List<Int>,
    /** Tents put so far, and how many there must be (as many as trees). */
    val tentCount: Int,
    val treeCount: Int,
    /** Taps so far (not shown, and not in the statistics: this game counts time only). */
    val taps: Int,
    val status: TentsStatus,
    /** False while the board is as it was at the start. */
    val canRestart: Boolean,
)

/**
 * The rules of the tents puzzle, free of UI and time (docs/yeni-oyunlar-plan.md).
 *
 * Every tree gets a tent next to it (above, below or beside it), every tent belongs to one tree, no two tents touch, even
 * at a corner, and each row and column holds as many tents as its number says. A tap cycles a cell: empty, tent, grass, empty.
 * The grass is only the player's note and takes no part in the rules. There is no losing. A restart clears every mark.
 */
class TentsGame(private val puzzle: TentsPuzzle) {
    private val size = puzzle.size
    private val marks = IntArray(size * size)
    private var taps = 0

    private val rowTargets = puzzle.rowTargets
    private val columnTargets = puzzle.columnTargets

    private fun tents(): Set<Int> = marks.indices.filterTo(HashSet()) { marks[it] == TentsMark.TENT }

    val isFinished: Boolean get() = TentsRules.isSolved(size, puzzle.trees, tents(), rowTargets, columnTargets)

    val state: TentsState
        get() {
            val tents = tents()
            val rows = TentsRules.rowCounts(size, tents).toList()
            val columns = TentsRules.columnCounts(size, tents).toList()
            val conflicts = TentsRules.conflicts(size, puzzle.trees, tents, rowTargets, columnTargets)
            val finished = TentsRules.isSolved(size, puzzle.trees, tents, rowTargets, columnTargets)
            return TentsState(
                size = size,
                trees = List(size * size) { it in puzzle.trees },
                marks = marks.toList(),
                conflicts = List(size * size) { it in conflicts },
                rowTargets = rowTargets,
                columnTargets = columnTargets,
                rowCounts = rows,
                columnCounts = columns,
                tentCount = tents.size,
                treeCount = puzzle.trees.size,
                taps = taps,
                status = if (finished) TentsStatus.Won else TentsStatus.Playing,
                canRestart = !finished && marks.any { it != TentsMark.EMPTY },
            )
        }

    /** Cycles [cell]: empty, tent, grass, empty. */
    fun tap(cell: Int): TentsResult {
        if (isFinished || cell !in 0 until size * size || cell in puzzle.trees) return TentsResult.Ignored
        taps++
        marks[cell] = (marks[cell] + 1) % 3
        // Taking a tent away (the grass step) can finish the board too, when it was the one extra tent.
        if (isFinished) return TentsResult.Solved
        return when (marks[cell]) {
            TentsMark.TENT -> TentsResult.Tent
            TentsMark.GRASS -> TentsResult.Grass
            else -> TentsResult.Cleared
        }
    }

    /** Clears every mark. The taps stay counted. Returns false if the board was clear already. */
    fun restart(): Boolean {
        if (!state.canRestart) return false
        marks.fill(TentsMark.EMPTY)
        return true
    }
}
