package com.mehmtcan.brainscroll.ui.howto

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.sudoku.JigsawGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuPuzzle
import com.mehmtcan.brainscroll.ui.theme.world

// The pictures of the deduction games (the grids): each game has one for a way of solving it and one for the first move. A scene is a small
// board and a script of what happens on it, so the pictures cost little to draw (see DemoScene.kt).

private fun DemoSceneBuilder.clue(r: Int, c: Int, text: String = "") = set(r, c, SceneFill.Clue, text)

private fun DemoSceneBuilder.hold(r: Int, c: Int, mark: SceneMark) = set(r, c, SceneFill.Empty, mark = mark)

// ---------------------------------------------------------------------------------------------------------------------------------
// Nonogram: a 4 by 4 picture with its numbers; the board is the lower right part of a 6 by 6 grid, the numbers fill the first two rows and columns.
//   . X X X X        rows:    4 / 2 / 1 1 / 3
//   . . X X .        columns: 1 1 / 2 1 / 4 / 1 1
//   . X . X .
//   . . X X X
private val NonogramRows = listOf(listOf(4), listOf(2), listOf(1, 1), listOf(3))
private val NonogramColumns = listOf(listOf(1, 1), listOf(2, 1), listOf(4), listOf(1, 1))

private fun nonogramScene(script: DemoSceneBuilder.() -> Unit) = demoScene(6, 6, focus = GameKind.Nonogram.world) {
    for (r in 0 until 6) for (c in 0 until 6) if (r < 2 || c < 2) clue(r, c)
    NonogramRows.forEachIndexed { i, numbers -> numbers.reversed().forEachIndexed { k, n -> clue(2 + i, 1 - k, n.toString()) } }
    NonogramColumns.forEachIndexed { j, numbers -> numbers.reversed().forEachIndexed { k, n -> clue(1 - k, 2 + j, n.toString()) } }
    script()
}

/** Technique: the numbers and the gaps between them are as long as the line, so every cell of it is known. */
internal val NonogramFullScene = nonogramScene {
    ring(0.06f, 0.5f, SceneRole.Evidence, 0 to 3, 1 to 3)
    tint(0.08f, 0.94f, SceneRole.Focus, (2..5).map { cell(it, 3) })
    mark(0.3f, 2, 3, SceneMark.Fill)
    mark(0.4f, 3, 3, SceneMark.Fill)
    mark(0.5f, 4, 3, SceneMark.Cross)
    mark(0.6f, 5, 3, SceneMark.Fill)
}

/** First step: a number as long as the whole line fills it. */
internal val NonogramStartScene = nonogramScene {
    ring(0.06f, 0.46f, SceneRole.Evidence, 2 to 1)
    tint(0.08f, 0.5f, SceneRole.Focus, (2..5).map { cell(2, it) })
    for (k in 0 until 4) mark(0.18f + k * 0.07f, 2, 2 + k, SceneMark.Fill)
    ring(0.5f, 0.94f, SceneRole.Evidence, 1 to 4)
    tint(0.52f, 0.94f, SceneRole.Focus, (2..5).map { cell(it, 4) })
    for (k in 1 until 4) mark(0.62f + k * 0.07f, 2 + k, 4, SceneMark.Fill)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Tents: a 4 by 4 board in a 5 by 5 grid (the counts of the rows and columns on the top and left edge). The trees are in the cells 0, 7, 8 and 15.
// The answer: tents at (0,1) (0,3) (3,0) (3,2) of the board, so the counts are 1 1 1 1 across and 2 0 0 2 down.
private fun tentsScene(script: DemoSceneBuilder.() -> Unit) = demoScene(5, 5, focus = GameKind.Tents.world) {
    clue(0, 0)
    listOf(1, 1, 1, 1).forEachIndexed { i, n -> clue(0, 1 + i, n.toString()) }
    listOf(2, 0, 0, 2).forEachIndexed { i, n -> clue(1 + i, 0, n.toString()) }
    for ((r, c) in listOf(1 to 1, 2 to 4, 3 to 1, 4 to 4)) hold(r, c, SceneMark.Tree)
    script()
}

/** Technique: tents do not touch, not even at a corner, so the cells round a tent are grass. */
internal val TentsTouchScene = tentsScene {
    hold(1, 2, SceneMark.Tent)
    ring(0.06f, 0.5f, SceneRole.Evidence, 1 to 2)
    for ((k, at) in listOf(1 to 3, 2 to 1, 2 to 2, 2 to 3).withIndex()) mark(0.2f + k * 0.08f, at.first, at.second, SceneMark.Cross)
}

/** First step: a row or a column with a 0 has no tent, and the tree that has one free cell left gets its tent there. */
internal val TentsStartScene = tentsScene {
    ring(0.06f, 0.46f, SceneRole.Evidence, 2 to 0, 3 to 0)
    for ((k, at) in listOf(2 to 1, 2 to 2, 2 to 3, 3 to 2, 3 to 3, 3 to 4).withIndex()) mark(0.14f + k * 0.05f, at.first, at.second, SceneMark.Cross)
    ring(0.5f, 0.94f, SceneRole.Target, 1 to 2)
    mark(0.66f, 1, 2, SceneMark.Tent)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Queens: a 4 by 4 board. The answer is (0,1) (1,3) (2,0) (3,2).
private fun queensScene(script: DemoSceneBuilder.() -> Unit) = demoScene(4, 4, focus = GameKind.Queens.world, script = script)

/** Technique: a queen shuts its row, its column and both diagonals, and a row with one free cell left holds a queen. */
internal val QueensReachScene = queensScene {
    hold(0, 1, SceneMark.Queen)
    ring(0.06f, 0.3f, SceneRole.Evidence, 0 to 1)
    val shut = listOf(0 to 0, 0 to 2, 0 to 3, 1 to 1, 2 to 1, 3 to 1, 1 to 0, 1 to 2, 2 to 3)
    for ((k, at) in shut.withIndex()) mark(0.12f + k * 0.045f, at.first, at.second, SceneMark.Cross)
    ring(0.6f, 0.94f, SceneRole.Target, 1 to 3)
    mark(0.72f, 1, 3, SceneMark.Queen)
}

/** First step: begin with the locked queens, shut what they reach, and look for a row with one cell left. */
internal val QueensStartScene = queensScene {
    hold(0, 1, SceneMark.Queen)
    hold(2, 0, SceneMark.Queen)
    ring(0.04f, 0.28f, SceneRole.Evidence, 0 to 1, 2 to 0)
    for ((k, at) in listOf(1 to 0, 1 to 1, 1 to 2).withIndex()) mark(0.12f + k * 0.05f, at.first, at.second, SceneMark.Cross)
    ring(0.3f, 0.5f, SceneRole.Target, 1 to 3)
    mark(0.4f, 1, 3, SceneMark.Queen)
    for ((k, at) in listOf(3 to 0, 3 to 1, 3 to 3).withIndex()) mark(0.5f + k * 0.04f, at.first, at.second, SceneMark.Cross)
    ring(0.64f, 0.94f, SceneRole.Target, 3 to 2)
    mark(0.78f, 3, 2, SceneMark.Queen)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Lone Number: a 4 by 4 board. The cells (0,3) (2,3) (3,0) are shaded in the answer.
private fun loneScene(script: DemoSceneBuilder.() -> Unit) = demoScene(4, 4, focus = GameKind.LoneNumber.world) {
    row(0, "2133")
    row(1, "3214")
    row(2, "1421")
    row(3, "4342")
    script()
}

/** Technique: a shaded cell cannot touch another, so the cells beside it stay. */
internal val LoneNeighborsScene = loneScene {
    ring(0.06f, 0.4f, SceneRole.Evidence, 0 to 2, 0 to 3)
    tap(0.3f, 0, 3)
    mark(0.4f, 0, 3, SceneMark.Shade)
    ring(0.5f, 0.94f, SceneRole.Good, 0 to 2, 1 to 3)
}

/** First step: a number that is alone in its row and its column is safe; only the numbers that come twice can be shaded. */
internal val LoneStartScene = loneScene {
    ring(0.06f, 0.5f, SceneRole.Evidence, 0 to 2, 0 to 3, 2 to 0, 2 to 3, 3 to 0, 3 to 2)
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 3)
    tap(0.62f, 0, 3)
    mark(0.7f, 0, 3, SceneMark.Shade)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Zeros and Ones: a 4 by 4 board; the answer is 0110 / 1100 / 0011 / 1001.
private fun binScene(script: DemoSceneBuilder.() -> Unit) = demoScene(4, 4, focus = GameKind.ZerosOnes.world) {
    row(0, "01..")
    row(1, "11..")
    row(2, "00..")
    row(3, "...1")
    script()
}

/** Technique: a line that holds half of its cells of one digit gets the other digit in the rest. */
internal val BinHalfScene = binScene {
    ring(0.04f, 0.46f, SceneRole.Evidence, 0 to 0, 2 to 0)
    tint(0.05f, 0.48f, SceneRole.Focus, (0..3).map { cell(it, 0) })
    ring(0.2f, 0.48f, SceneRole.Target, 3 to 0)
    place(0.3f, 3, 0, "1")
    ring(0.52f, 0.94f, SceneRole.Evidence, 0 to 1, 1 to 1)
    tint(0.53f, 0.94f, SceneRole.Focus, (0..3).map { cell(it, 1) })
    ring(0.6f, 0.94f, SceneRole.Target, 3 to 1)
    place(0.7f, 3, 1, "0")
}

/** First step: two of the same digit side by side; the next cell takes the other digit, and the line is half full. */
internal val BinStartScene = binScene {
    ring(0.06f, 0.4f, SceneRole.Evidence, 2 to 0, 2 to 1)
    ring(0.2f, 0.5f, SceneRole.Target, 2 to 2)
    place(0.34f, 2, 2, "1")
    ring(0.5f, 0.94f, SceneRole.Target, 2 to 3)
    place(0.7f, 2, 3, "1")
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Greater and Less: a 3 by 3 board drawn on a 5 by 5 grid, the signs sit in the cells between. 2 1 3 / 3 2 1 / 1 3 2.
private fun glScene(script: DemoSceneBuilder.() -> Unit) = demoScene(5, 5, focus = GameKind.GreaterLess.world) {
    for (r in 0 until 5) for (c in 0 until 5) if (r % 2 == 1 || c % 2 == 1) clue(r, c)
    set(0, 0, SceneFill.Given, "2")
    set(0, 2, SceneFill.Given, "1")
    set(2, 2, SceneFill.Given, "2")
    set(2, 4, SceneFill.Given, "1")
    set(4, 0, SceneFill.Given, "1")
    set(4, 2, SceneFill.Given, "3")
    // The three signs: the left cell of the middle row is bigger, the top cell of the right column is bigger, the left cell of the last row is smaller.
    hold2(2, 1, SceneMark.ChevronRight)
    hold2(1, 4, SceneMark.ChevronDown)
    hold2(4, 1, SceneMark.ChevronLeft)
    script()
}

private fun DemoSceneBuilder.hold2(r: Int, c: Int, mark: SceneMark) = set(r, c, SceneFill.Clue, mark = mark)

/** Technique: a sign says which neighbour is bigger; next to a 2 the bigger one has to be 3. */
internal val GlSignScene = glScene {
    ring(0.06f, 0.5f, SceneRole.Evidence, 2 to 1, 2 to 2)
    ring(0.5f, 0.94f, SceneRole.Target, 2 to 0)
    place(0.62f, 2, 0, "3")
}

/** First step: a row (or column) that has all but one number: the last cell is the number that is missing. */
internal val GlStartScene = glScene {
    ring(0.06f, 0.5f, SceneRole.Evidence, 0 to 0, 0 to 2)
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 4)
    place(0.62f, 0, 4, "3")
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Fences: a 3 by 3 board of squares with dots at the corners. The loop goes round the squares 0, 1, 4 and 7.
private fun fencesScene(script: DemoSceneBuilder.() -> Unit) = demoScene(3, 3, focus = GameKind.Fences.world) {
    dots()
    row(0, "32.")
    row(1, "12.")
    row(2, ".31")
    script()
}

/** Technique: when a square has all the lines its number asks for, its other sides get no line. */
internal val FencesFullScene = fencesScene {
    ring(0.06f, 0.5f, SceneRole.Evidence, 0 to 0)
    edge(0.14f, 0, 0, 0)
    edge(0.24f, 0, 0, 3)
    edge(0.34f, 0, 0, 2)
    noEdge(0.52f, 0, 0, 1)
}

/** First step: a 3 in a corner needs both outer sides of the corner, because it cannot reach three sides without them. */
internal val FencesStartScene = fencesScene {
    ring(0.06f, 0.94f, SceneRole.Target, 0 to 0)
    edge(0.3f, 0, 0, 0)
    edge(0.5f, 0, 0, 3)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Rectangles: a 4 by 4 board with the numbers 4 at (0,0), 2 at (0,2), 4 at (1,2), 4 at (2,0) and 2 at (3,2).
private fun rectScene(script: DemoSceneBuilder.() -> Unit) = demoScene(4, 4, focus = GameKind.Rectangles.world) {
    set(0, 0, SceneFill.Empty, "4")
    set(0, 2, SceneFill.Empty, "2")
    set(1, 2, SceneFill.Empty, "4")
    set(2, 0, SceneFill.Empty, "4")
    set(3, 2, SceneFill.Empty, "2")
    script()
}

private fun DemoSceneBuilder.cornerBox() = listOf(cell(0, 0), cell(0, 1), cell(1, 0), cell(1, 1))

/** Technique: with the corner done, a 2 that has a number below it and a cell taken beside it takes the free cell on its other side. */
internal val RectNeighborScene = rectScene {
    tint(0.02f, 0.94f, SceneRole.Good, cornerBox())
    ring(0.1f, 0.5f, SceneRole.Evidence, 0 to 1, 1 to 2)
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 3)
    tint(0.58f, 0.94f, SceneRole.Focus, listOf(cell(0, 2), cell(0, 3)))
}

/** First step: a big number in a corner, with other numbers in the way: only one shape fits. */
internal val RectStartScene = rectScene {
    ring(0.06f, 0.5f, SceneRole.Evidence, 0 to 2, 2 to 0)
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 0, 0 to 1, 1 to 0, 1 to 1)
    tint(0.6f, 0.94f, SceneRole.Good, cornerBox())
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Axis Sum: a 3 by 3 board with the sums on the top and the left. 4 7 2 / 6 1 8 / 3 9 5; the 7 and the 8 are the ones to erase.
private fun axisScene(script: DemoSceneBuilder.() -> Unit) = demoScene(4, 4, focus = GameKind.AxisSum.world) {
    clue(0, 0)
    listOf(13, 10, 7).forEachIndexed { i, n -> clue(0, 1 + i, n.toString()) }
    listOf(6, 7, 17).forEachIndexed { i, n -> clue(1 + i, 0, n.toString()) }
    listOf("472", "618", "395").forEachIndexed { r, text -> text.forEachIndexed { c, ch -> set(1 + r, 1 + c, SceneFill.Empty, ch.toString()) } }
    script()
}

/** Technique: the line is too big by the difference between its sum and its target, and that difference is one of its numbers. */
internal val AxisDifferenceScene = axisScene {
    ring(0.06f, 0.5f, SceneRole.Evidence, 1 to 0)
    tint(0.08f, 0.5f, SceneRole.Focus, listOf(cell(1, 1), cell(1, 2), cell(1, 3)))
    ring(0.5f, 0.94f, SceneRole.Target, 1 to 2)
    tap(0.6f, 1, 2)
    mark(0.7f, 1, 2, SceneMark.Shade)
}

/** First step: a line whose sum is already its target keeps all its numbers. */
internal val AxisStartScene = axisScene {
    ring(0.06f, 0.5f, SceneRole.Evidence, 3 to 0)
    tint(0.08f, 0.5f, SceneRole.Good, listOf(cell(3, 1), cell(3, 2), cell(3, 3)))
    ring(0.52f, 0.94f, SceneRole.Evidence, 0 to 1)
    tint(0.54f, 0.94f, SceneRole.Good, listOf(cell(1, 1), cell(2, 1), cell(3, 1)))
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Arrows: four arrows on a 4 by 4 board. Every arrow has a colour, its head has a little arrow. C is in front of B and D, so they wait for it.
private fun arrowsScene(script: DemoSceneBuilder.() -> Unit) = demoScene(4, 4, focus = GameKind.Arrows.world) {
    regions("0 0 0 8", "1 1 8 3", "8 1 8 3", "8 2 2 2")
    hold(0, 2, SceneMark.ArrowRight)
    hold(2, 1, SceneMark.ArrowDown)
    hold(3, 3, SceneMark.ArrowRight)
    hold(2, 3, SceneMark.ArrowDown)
    script()
}

/** Technique: an arrow with another arrow in front of its head waits; take the one in the way out first. */
internal val ArrowsChainScene = arrowsScene {
    ring(0.06f, 0.46f, SceneRole.Evidence, 3 to 1, 3 to 3)
    ring(0.46f, 0.94f, SceneRole.Target, 3 to 1, 3 to 2, 3 to 3)
    tap(0.54f, 3, 2)
    ring(0.7f, 0.94f, SceneRole.Good, 2 to 1, 2 to 3)
}

/** First step: begin with an arrow that has nothing in front of its head. */
internal val ArrowsStartScene = arrowsScene {
    ring(0.06f, 0.5f, SceneRole.Evidence, 0 to 3)
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 0, 0 to 1, 0 to 2)
    tap(0.62f, 0, 1)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Pipes: a 3 by 3 network (bits: 1 up, 2 right, 4 down, 8 left): 6 e c / 5 1 5 / 3 8 1 when it is done.
private fun pipesScene(rows: Array<String>, script: DemoSceneBuilder.() -> Unit) = demoScene(3, 3, focus = GameKind.Pipes.world) {
    pipes(rows[0], rows[1], rows[2])
    script()
}

/** Technique: no pipe may point out of the board; turn the ones that do until they point at a neighbour. */
internal val PipesEdgeScene = pipesScene(arrayOf("3ec", "515", "341")) {
    ring(0.06f, 0.4f, SceneRole.Target, 2 to 1)
    tap(0.2f, 2, 1)
    turn(0.26f, 2, 1, 8)
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 0)
    tap(0.6f, 0, 0)
    turn(0.66f, 0, 0, 6)
}

/** First step: a corner. A bent pipe in a corner fits one way only, with both its ends inside the board. */
internal val PipesStartScene = pipesScene(arrayOf("9ec", "515", "381")) {
    ring(0.06f, 0.9f, SceneRole.Target, 0 to 0)
    tap(0.18f, 0, 0)
    turn(0.24f, 0, 0, 3)
    tap(0.42f, 0, 0)
    turn(0.48f, 0, 0, 6)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Jigsaw: the real board of the other pictures of the game, from the generator, so the shapes are the ones the game draws.
private val JigsawPuzzle: SudokuPuzzle by lazy { JigsawGenerator.generate(seed = 7, clues = 44) }

private fun jigsawScene(script: DemoSceneBuilder.(SudokuPuzzle) -> Unit): DemoScene {
    val puzzle = JigsawPuzzle
    val region = puzzle.layout.regionOf
    return demoScene(9, 9, focus = GameKind.Jigsaw.world) {
        regions(*Array(9) { r -> (0 until 9).joinToString("") { region[r * 9 + it].toString() } })
        for (i in 0 until 81) if (puzzle.givens[i] != 0) set(i / 9, i % 9, SceneFill.Given, puzzle.givens[i].toString())
        script(puzzle)
    }
}

private class Pick(val cell: Int, val evidence: List<Int>)

/** The blank cell that only one digit can fill, with one cell for each of the other digits that rule the rest out; [better] says which blank is the best to show. */
private fun single(puzzle: SudokuPuzzle, regionFirst: Boolean, better: (cell: Int, evidence: List<Int>) -> Int): Pick? {
    val region = puzzle.layout.regionOf
    var best: Pick? = null
    var bestScore = Int.MIN_VALUE
    for (cell in 0 until 81) {
        if (puzzle.givens[cell] != 0) continue
        val peers = (0 until 81).filter { it != cell && puzzle.givens[it] != 0 && (it / 9 == cell / 9 || it % 9 == cell % 9 || region[it] == region[cell]) }
        val digits = peers.map { puzzle.givens[it] }.toSet()
        if (digits.size != 8) continue
        // One peer for each digit, a cell of the region first.
        val evidence = digits.map { d -> peers.filter { puzzle.givens[it] == d }.let { list -> list.firstOrNull { (region[it] == region[cell]) == regionFirst } ?: list.first() } }
        val score = better(cell, evidence)
        if (score > bestScore) {
            bestScore = score
            best = Pick(cell, evidence)
        }
    }
    return best
}

private fun regionCells(puzzle: SudokuPuzzle, of: Int) = (0 until 81).filter { puzzle.layout.regionOf[it] == puzzle.layout.regionOf[of] }

/** Technique: the row, the column and the shape of a cell show eight of the nine digits; the ninth goes there. */
internal val JigsawSingleScene: DemoScene by lazy {
    jigsawScene { puzzle ->
        val pick = single(puzzle, regionFirst = false) { cell, evidence -> evidence.count { puzzle.layout.regionOf[it] != puzzle.layout.regionOf[cell] } }
        if (pick != null) {
            tint(0.06f, 0.94f, SceneRole.Focus, regionCells(puzzle, pick.cell))
            ringCells(0.12f, 0.5f, SceneRole.Evidence, pick.evidence)
            ringCells(0.5f, 0.94f, SceneRole.Target, listOf(pick.cell))
            place(0.7f, pick.cell / 9, pick.cell % 9, puzzle.solution[pick.cell].toString())
        }
    }
}

/** First step: the shape that has the most numbers already; its last cells are the easiest ones on the board. */
internal val JigsawStartScene: DemoScene by lazy {
    jigsawScene { puzzle ->
        val region = puzzle.layout.regionOf
        // The fewer blanks the shape has, the better; only blanks with one possible digit count.
        val pick = single(puzzle, regionFirst = true) { cell, _ -> -(0 until 81).count { puzzle.givens[it] == 0 && region[it] == region[cell] } }
        if (pick != null) {
            val shape = regionCells(puzzle, pick.cell)
            tint(0.06f, 0.94f, SceneRole.Focus, shape)
            ringCells(0.12f, 0.5f, SceneRole.Evidence, shape.filter { puzzle.givens[it] != 0 })
            ringCells(0.5f, 0.94f, SceneRole.Target, listOf(pick.cell))
            place(0.7f, pick.cell / 9, pick.cell % 9, puzzle.solution[pick.cell].toString())
        }
    }
}

/** Every picture of this file, with a name for the screenshots of the tests. */
internal val DeductionScenes: List<Pair<String, DemoScene>> by lazy {
    listOf(
        "nonogram_full" to NonogramFullScene, "nonogram_start" to NonogramStartScene,
        "tents_touch" to TentsTouchScene, "tents_start" to TentsStartScene,
        "queens_reach" to QueensReachScene, "queens_start" to QueensStartScene,
        "lone_neighbors" to LoneNeighborsScene, "lone_start" to LoneStartScene,
        "bin_half" to BinHalfScene, "bin_start" to BinStartScene,
        "gl_sign" to GlSignScene, "gl_start" to GlStartScene,
        "fences_full" to FencesFullScene, "fences_start" to FencesStartScene,
        "rect_neighbor" to RectNeighborScene, "rect_start" to RectStartScene,
        "axis_difference" to AxisDifferenceScene, "axis_start" to AxisStartScene,
        "arrows_chain" to ArrowsChainScene, "arrows_start" to ArrowsStartScene,
        "pipes_edge" to PipesEdgeScene, "pipes_start" to PipesStartScene,
        "jigsaw_single" to JigsawSingleScene, "jigsaw_start" to JigsawStartScene,
    )
}
