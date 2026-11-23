package com.mehmtcan.brainscroll.ui.howto

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.world

// One real puzzle for all the pictures of the sudoku card, so the numbers a step talks about are the same ones the next step looks at.
private val Rows = listOf(
    "53..7....",
    "6..195...",
    ".98....6.",
    "8...6...3",
    "4..8.3..1",
    "7...2...6",
    ".6....28.",
    "...419..5",
    "....8..79",
)

private fun sudokuScene(script: DemoSceneBuilder.() -> Unit) =
    demoScene(9, 9, thickEvery = 3, focus = GameKind.Sudoku.world) {
        Rows.forEachIndexed { r, text -> row(r, text) }
        script()
    }

/** Goal: a row, a column and a box light up one after the other. */
internal val SudokuGoalScene = sudokuScene {
    tint(0.08f, 0.34f, SceneRole.Focus, rowCells(4))
    tint(0.36f, 0.62f, SceneRole.Focus, columnCells(4))
    tint(0.64f, 0.9f, SceneRole.Focus, boxCells(4, 4))
}

/** Technique 1: an empty cell whose row, column and box already hold every other digit. */
internal val SudokuSingleScene = sudokuScene {
    tint(0.06f, 0.55f, SceneRole.Focus, rowCells(4) + columnCells(4) + boxCells(4, 4))
    // One cell for each of the eight other digits: 4, 8, 3 and 1 in the row, 7, 9, 6 and 2 in the column.
    ring(0.12f, 0.62f, SceneRole.Evidence, 4 to 0, 4 to 3, 4 to 5, 4 to 8, 0 to 4, 1 to 4, 3 to 4, 5 to 4)
    ring(0.45f, 0.94f, SceneRole.Target, 4 to 4)
    place(0.7f, 4, 4, "5")
}

/** Technique 2: a digit that only one cell of a box can take, because the same digit blocks the others. */
internal val SudokuPlaceScene = sudokuScene {
    tint(0.06f, 0.7f, SceneRole.Focus, boxCells(0, 4))
    // The 8 in row 2 and the 8 in column 3 keep the 8 out of the other empty cells of the box.
    ring(0.14f, 0.6f, SceneRole.Evidence, 2 to 2, 4 to 3)
    mark(0.3f, 0, 3, SceneMark.Cross)
    mark(0.34f, 2, 3, SceneMark.Cross)
    mark(0.38f, 2, 4, SceneMark.Cross)
    mark(0.42f, 2, 5, SceneMark.Cross)
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 5)
    place(0.72f, 0, 5, "8")
}

/** First step: take a digit that is already on the board many times, and find where its last copy goes. */
internal val SudokuStartScene = sudokuScene {
    ring(0.06f, 0.52f, SceneRole.Evidence, 2 to 2, 3 to 0, 4 to 3, 6 to 7, 8 to 4)
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 5)
    place(0.74f, 0, 5, "8")
}
