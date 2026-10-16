package com.mehmtcan.brainscroll.game.sudoku

/**
 * Which cells belong together on a 9 by 9 board: the nine rows, the nine columns and the nine regions. [regionOf] gives for every
 * cell (numbered row by row from 0) its region, 0..8. In the classic sudoku the regions are the 3 by 3 boxes; in the jigsaw
 * sudoku they are connected shapes of nine cells that fit together like pieces of a puzzle.
 *
 * The rules are the same for both: every row, column and region holds the digits 1 to 9 once.
 */
class SudokuLayout(val regionOf: IntArray) {
    init {
        require(regionOf.size == CELLS) { "81 cells" }
        require((0 until SIZE).all { region -> regionOf.count { it == region } == SIZE }) { "nine regions of nine cells" }
    }

    /** The 27 units, each as its 9 cells: the rows (0..8), the columns (9..17) and the regions (18..26). */
    val units: List<IntArray> =
        List(SIZE) { r -> IntArray(SIZE) { r * SIZE + it } } +
            List(SIZE) { c -> IntArray(SIZE) { it * SIZE + c } } +
            List(SIZE) { region -> (0 until CELLS).filter { regionOf[it] == region }.toIntArray() }

    /** The cells that share a row, a column or a region with a cell, not the cell itself. */
    val peers: List<IntArray> = List(CELLS) { cell ->
        (0 until CELLS).filter { other ->
            other != cell && (other / SIZE == cell / SIZE || other % SIZE == cell % SIZE || regionOf[other] == regionOf[cell])
        }.toIntArray()
    }

    /** True for the classic 3 by 3 boxes. */
    val isStandard: Boolean get() = regionOf.contentEquals(STANDARD.regionOf)

    companion object {
        const val SIZE = 9
        const val CELLS = 81

        /** The classic layout: the nine 3 by 3 boxes. */
        val STANDARD = SudokuLayout(IntArray(CELLS) { (it / SIZE / 3) * 3 + (it % SIZE) / 3 })

        /** A layout from the regions of a board, as the list the UI gets in the state of a game. */
        fun of(regions: List<Int>): SudokuLayout = SudokuLayout(regions.toIntArray())
    }
}
