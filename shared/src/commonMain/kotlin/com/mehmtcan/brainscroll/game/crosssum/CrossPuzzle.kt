package com.mehmtcan.brainscroll.game.crosssum

import com.mehmtcan.brainscroll.game.BitText

/**
 * A block of white cells in a row or a column that has a sum: the numbers in it are different and add up to [sum]. [cells] are the
 * cells of the block (row * columns + column of the white board), [across] says if it runs along a row. The sum is written in the clue
 * cell just before the block: [clueRow] and [clueColumn] are the place of that cell on the frame, the board with one more row on the top
 * and one more column on the left (so a cell of the board at (row, column) is at (row + 1, column + 1) on the frame).
 */
class CrossRun(val cells: List<Int>, val sum: Int, val across: Boolean, val clueRow: Int, val clueColumn: Int)

/**
 * A board of cross sums (Kakuro): a rectangle of [rows] by [columns] cells, each white (a number goes there) or black. Every run of
 * white cells along a row or a column is at least two cells long and is a block with a sum, shown in the clue cell before it (a black
 * cell, or the frame round the board). The player writes the digits 1 to 9 so that the digits of a block differ and add up to its
 * sum. [solution] holds the answer (0 on a black cell); the sums come from it. [given] marks the white cells that are filled to start
 * with. Boards are made so that the rules alone lead to the answer from the sums and the given digits (see [CrossGenerator]).
 *
 * A board is saved as text (see [encode]): the size, one bit a cell for the white ones, and for every white cell one bit for given and
 * four bits for its digit. The biggest board is [MAX_BITS] bits, so it fits the 32 characters the cloud table keeps.
 */
class CrossPuzzle(val rows: Int, val columns: Int, val white: List<Boolean>, val solution: List<Int>, val given: List<Boolean>) {
    init {
        require(rows in MIN_SIDE..MAX_SIDE && columns in MIN_SIDE..MAX_SIDE) { "size $rows x $columns" }
        require(white.size == rows * columns && solution.size == white.size && given.size == white.size) { "sizes" }
        require(bits() <= MAX_BITS) { "too big to save: ${bits()} bits" }
        for (cell in white.indices) {
            if (white[cell]) require(solution[cell] in 1..9) { "digit of $cell" } else require(solution[cell] == 0 && !given[cell]) { "black cell $cell" }
        }
    }

    /** The cells of the white board where a number goes, in order. */
    val whiteCells: List<Int> by lazy { white.indices.filter { white[it] } }

    /** The cells that start filled. */
    val givenCells: List<Int> by lazy { white.indices.filter { given[it] } }

    /** The cells the player fills, in order. */
    val openCells: List<Int> by lazy { white.indices.filter { white[it] && !given[it] } }

    fun isGiven(cell: Int): Boolean = given[cell]

    /** Every block of the board, the ones along rows first. A board that has a white cell on its own in a row or column cannot be made. */
    val runs: List<CrossRun> by lazy {
        val out = ArrayList<CrossRun>()
        for (row in 0 until rows) {
            var column = 0
            while (column < columns) {
                if (!white[row * columns + column]) { column++; continue }
                val start = column
                while (column < columns && white[row * columns + column]) column++
                require(column - start >= 2) { "a white cell alone in row $row" }
                val cells = (start until column).map { row * columns + it }
                out += CrossRun(cells, cells.sumOf { solution[it] }, true, row + 1, start)
            }
        }
        for (column in 0 until columns) {
            var row = 0
            while (row < rows) {
                if (!white[row * columns + column]) { row++; continue }
                val start = row
                while (row < rows && white[row * columns + column]) row++
                require(row - start >= 2) { "a white cell alone in column $column" }
                val cells = (start until row).map { it * columns + column }
                out += CrossRun(cells, cells.sumOf { solution[it] }, false, start, column + 1)
            }
        }
        out.forEach { run -> require(run.cells.map { solution[it] }.toSet().size == run.cells.size) { "a digit twice in a block" } }
        out
    }

    /** The two blocks a white cell belongs to: the one along its row, then the one along its column. */
    fun runsOf(cell: Int): List<CrossRun> = runs.filter { cell in it.cells }

    private fun bits(): Int {
        val whites = white.count { it }
        return 6 + rows * columns + whites * 5
    }

    /** The size (3 bits each), which cells are white, then for every white cell a bit for given and the digit less one in 4 bits. */
    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(rows - MIN_SIDE, 3)
        writer.put(columns - MIN_SIDE, 3)
        for (cell in white.indices) writer.put(if (white[cell]) 1 else 0, 1)
        for (cell in white.indices) if (white[cell]) {
            writer.put(if (given[cell]) 1 else 0, 1)
            writer.put(solution[cell] - 1, 4)
        }
        return writer.text()
    }

    override fun equals(other: Any?) =
        other is CrossPuzzle && rows == other.rows && columns == other.columns && white == other.white && solution == other.solution && given == other.given

    override fun hashCode() = ((31 * rows + columns) * 31 + white.hashCode()) * 31 + solution.hashCode() + given.hashCode()

    companion object {
        const val MIN_SIDE = 3
        const val MAX_SIDE = 6

        /** The biggest saved board: 32 characters of 6 bits. */
        const val MAX_BITS = 32 * 6

        /** Reads what [encode] wrote. Null if it is not a board (so a saved game of another kind starts a new one). */
        fun decode(text: String): CrossPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val rows = (reader.take(3) ?: return null) + MIN_SIDE
            val columns = (reader.take(3) ?: return null) + MIN_SIDE
            if (rows > MAX_SIDE || columns > MAX_SIDE) return null
            val white = List(rows * columns) { (reader.take(1) ?: return null) == 1 }
            val solution = MutableList(rows * columns) { 0 }
            val given = MutableList(rows * columns) { false }
            for (cell in white.indices) if (white[cell]) {
                given[cell] = (reader.take(1) ?: return null) == 1
                solution[cell] = (reader.take(4) ?: return null) + 1
            }
            return runCatching { CrossPuzzle(rows, columns, white, solution, given).also { it.runs } }.getOrNull()
        }
    }
}
