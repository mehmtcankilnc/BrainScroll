package com.mehmtcan.brainscroll.ui.home

import androidx.compose.foundation.Canvas
import com.mehmtcan.brainscroll.ui.queens.drawCrown
import com.mehmtcan.brainscroll.ui.tents.drawTent
import com.mehmtcan.brainscroll.ui.tents.drawTree
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.lights.LightsGame
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.game.mines.MinesGame
import com.mehmtcan.brainscroll.game.mines.MinesState
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator
import com.mehmtcan.brainscroll.game.slide.SlideGame
import com.mehmtcan.brainscroll.game.sudoku.JigsawGenerator
import com.mehmtcan.brainscroll.ui.blockout.drawTargetMark
import com.mehmtcan.brainscroll.ui.sudoku.drawRegionBorders
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.game.target.TargetGenerator
import com.mehmtcan.brainscroll.game.tubes.TubesGame
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.components.TileState
import com.mehmtcan.brainscroll.ui.memory.CardShape
import com.mehmtcan.brainscroll.ui.memory.symbolColor
import com.mehmtcan.brainscroll.ui.mines.drawFlag
import com.mehmtcan.brainscroll.ui.mines.numberColor
import com.mehmtcan.brainscroll.ui.pipes.drawPipes
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import kotlin.random.Random

/**
 * A small picture of a game for the home tab and the feed editor. Each one is drawn with the colors, shapes and numbers
 * of the real game screen, and the boards come from the real game logic (a mines board with its first open done, a real
 * sudoku, the pipes of a generated board, ...), so the pictures are what the player will actually see. They are still
 * pictures: nothing in them reacts, and a screen reader skips them (the card around them has the game's name).
 */
@Composable
fun GameThumbnail(kind: GameKind, wordLanguage: Language, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .clearAndSetSemantics { }
            .graphicsLayer()
            .clip(Radius.control)
            .background(colors.bgPage)
            .border(1.dp, colors.borderSubtle, Radius.control),
        contentAlignment = Alignment.Center,
    ) {
        val side = minOf(maxWidth, maxHeight)
        when (kind) {
            GameKind.Word -> WordThumb(side, wordLanguage)
            GameKind.Memory -> MemoryThumb(side)
            GameKind.Tubes -> TubesThumb(side)
            GameKind.Mines -> MinesThumb(side)
            GameKind.Sudoku -> SudokuThumb(side)
            GameKind.Lights -> LightsThumb(side)
            GameKind.Slide -> SlideThumb(side)
            GameKind.Target -> TargetThumb(side)
            GameKind.Pipes -> PipesThumb(side)
            GameKind.Hanoi -> HanoiThumb(side)
            GameKind.Queens -> QueensThumb(side)
            GameKind.Tents -> TentsThumb(side)
            GameKind.Jigsaw -> JigsawThumb(side)
            GameKind.BlockOut -> BlockOutThumb(side)
            GameKind.Crossword -> CrosswordThumb(side)
            GameKind.Line -> LineThumb(side)
        }
    }
}

/** A number or letter whose size follows the picture, not the text size setting: it has to fit its cell. */
@Composable
private fun Mark(text: String, size: Dp, color: Color, weight: FontWeight = FontWeight.Bold) {
    val density = LocalDensity.current
    BasicText(
        text = text,
        style = BrainScrollTheme.typography.label.copy(color = color, fontSize = with(density) { size.toSp() }, fontWeight = weight),
    )
}

@Composable
private fun Cell(
    size: Dp,
    fill: Color,
    border: Color? = null,
    borderWidth: Dp = 1.dp,
    radius: Dp = size * 0.2f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(radius)
    Box(
        modifier = Modifier
            .size(size)
            .background(fill, shape)
            .then(if (border != null) Modifier.border(borderWidth, border, shape) else Modifier),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** A rectangle of equal cells; [cell] draws the one at a row and a column. */
@Composable
private fun Grid(rows: Int, cols: Int, gap: Dp, cell: @Composable (row: Int, col: Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (c in 0 until cols) cell(r, c)
            }
        }
    }
}

// --- Word puzzle: the grid of guesses with the three kinds of result ---

private val WordTr = listOf(
    "KALEM" to listOf(TileState.Absent, TileState.Pending, TileState.Absent, TileState.Correct, TileState.Absent),
    "BİLET" to listOf(TileState.Absent, TileState.Absent, TileState.Pending, TileState.Correct, TileState.Pending),
    "SA" to listOf(TileState.Filled, TileState.Filled, TileState.Empty, TileState.Empty, TileState.Empty),
    "" to List(5) { TileState.Empty },
)
private val WordEn = listOf(
    "CRANE" to listOf(TileState.Absent, TileState.Pending, TileState.Absent, TileState.Absent, TileState.Correct),
    "PLATE" to listOf(TileState.Absent, TileState.Absent, TileState.Pending, TileState.Correct, TileState.Correct),
    "SH" to listOf(TileState.Filled, TileState.Filled, TileState.Empty, TileState.Empty, TileState.Empty),
    "" to List(5) { TileState.Empty },
)

@Composable
private fun WordThumb(side: Dp, language: Language) {
    val colors = BrainScrollTheme.colors
    val gap = side * 0.04f
    val cell = (side * 0.84f - gap * 4) / 5
    val rows = if (language == Language.TR) WordTr else WordEn
    Grid(rows = 4, cols = 5, gap = gap) { r, c ->
        val (word, states) = rows[r]
        val state = states[c]
        val letter = word.getOrNull(c)?.toString() ?: ""
        when (state) {
            TileState.Correct -> Cell(cell, colors.correctFill) { Mark(letter, cell * 0.55f, colors.correctOn) }
            TileState.Pending -> Cell(cell, Color.Transparent, colors.pendingFill, 1.5.dp) { Mark(letter, cell * 0.55f, colors.pendingFill) }
            TileState.Absent -> Cell(cell, colors.absentFill) { Mark(letter, cell * 0.55f, colors.absentOn) }
            TileState.Filled -> Cell(cell, Color.Transparent, colors.borderStrong, 1.5.dp) { Mark(letter, cell * 0.55f, colors.textPrimary) }
            TileState.Empty -> Cell(cell, Color.Transparent, colors.borderSubtle, 1.5.dp)
        }
    }
}

// --- Memory match: face-down cards, a pair that matched and a card that is face up ---

private enum class Face { Down, Up, Matched }

private val MemoryCards = listOf(
    Face.Down to CardSymbol.Circle, Face.Matched to CardSymbol.Heart, Face.Down to CardSymbol.Star,
    Face.Up to CardSymbol.Circle, Face.Down to CardSymbol.Moon, Face.Matched to CardSymbol.Heart,
    Face.Down to CardSymbol.Plus, Face.Down to CardSymbol.Star, Face.Down to CardSymbol.Diamond,
)

@Composable
private fun MemoryThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val gap = side * 0.06f
    val cell = (side * 0.82f - gap * 2) / 3
    Grid(rows = 3, cols = 3, gap = gap) { r, c ->
        val (face, symbol) = MemoryCards[r * 3 + c]
        when (face) {
            Face.Down -> Cell(cell, colors.bgSurface, colors.borderStrong.copy(alpha = 0.5f), 1.dp)
            Face.Up -> Cell(cell, colors.bgSurface, colors.pendingFill, 2.dp) { CardShape(symbol, symbolColor(symbol), Modifier.size(cell * 0.55f)) }
            Face.Matched -> Cell(cell, colors.correctFill) { CardShape(symbol, colors.correctOn, Modifier.size(cell * 0.55f)) }
        }
    }
}

// --- Color tubes: tubes of stacked shapes in the colors of the real game ---

private val TubesSample: List<List<Int>> by lazy { TubesGame.newLayout(Random(5)).take(5) }

@Composable
private fun TubesThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val gap = side * 0.05f
    val tubeWidth = (side * 0.86f - gap * 4) / 5
    val tubeHeight = side * 0.8f
    val capacity = TubesGame.DEFAULT_CAPACITY
    val slot = tubeHeight / capacity
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        TubesSample.forEach { tube ->
            val shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = tubeWidth / 2, bottomEnd = tubeWidth / 2, topStart = tubeWidth * 0.12f, topEnd = tubeWidth * 0.12f)
            Column(
                modifier = Modifier
                    .size(tubeWidth, tubeHeight)
                    .clip(shape)
                    .background(colors.bgSurface)
                    .border(1.dp, colors.borderStrong.copy(alpha = 0.6f), shape),
                verticalArrangement = Arrangement.Bottom,
            ) {
                tube.asReversed().forEach { color ->
                    val symbol = CardSymbol.entries[color % CardSymbol.entries.size]
                    Box(Modifier.size(tubeWidth, slot).background(symbolColor(symbol)), contentAlignment = Alignment.Center) {
                        CardShape(symbol, colors.correctOn.copy(alpha = 0.55f), Modifier.size(tubeWidth * 0.5f))
                    }
                }
            }
        }
    }
}

// --- Minesweeper: a board after the first open, with numbers and one flag ---

private val MinesSample: MinesState by lazy {
    val game = MinesGame(seed = 3)
    game.open(27)
    val shown = game.state
    val flagged = shown.numbers.indices.firstOrNull { cell ->
        shown.numbers[cell] < 0 && MinesGame.neighbors(cell, shown.rows, shown.cols).any { shown.numbers[it] > 0 }
    }
    if (flagged != null) game.toggleFlag(flagged)
    game.state
}

@Composable
private fun MinesThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val state = MinesSample
    val gap = side * 0.015f
    val cell = (side * 0.9f - gap * (state.cols - 1)) / state.cols
    Grid(rows = state.rows, cols = state.cols, gap = gap) { r, c ->
        val index = r * state.cols + c
        val number = state.numbers[index]
        when {
            index in state.flagged -> Cell(cell, colors.bgRaised, colors.borderStrong.copy(alpha = 0.6f), 0.5.dp, cell * 0.2f) {
                Canvas(Modifier.size(cell * 0.7f)) { drawFlag(colors.textPrimary, colors.error) }
            }
            number < 0 -> Cell(cell, colors.bgRaised, colors.borderStrong.copy(alpha = 0.6f), 0.5.dp, cell * 0.2f)
            else -> Cell(cell, colors.bgSurface, null, 1.dp, cell * 0.2f) {
                if (number > 0) Mark(number.toString(), cell * 0.7f, numberColor(number, colors))
            }
        }
    }
}

// --- Sudoku: the top left box of a real board, given numbers and two the player entered ---

private val SudokuSample by lazy { SudokuGenerator.generate(7) }

@Composable
private fun SudokuThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val puzzle = SudokuSample
    val shown = 3
    val gap = side * 0.04f
    val cell = (side * 0.84f - gap * (shown - 1)) / shown
    // The first two empty cells in the picture show the right number, as if the player had entered them.
    val entered = (0 until shown * shown).map { (it / shown) * 9 + it % shown }.filter { puzzle.givens[it] == 0 }.take(2).toSet()
    Grid(rows = shown, cols = shown, gap = gap) { r, c ->
        val index = r * 9 + c
        Cell(cell, colors.bgSurface, colors.borderStrong.copy(alpha = 0.3f), 0.5.dp, cell * 0.2f) {
            when {
                puzzle.givens[index] != 0 -> Mark(puzzle.givens[index].toString(), cell * 0.62f, colors.textPrimary)
                index in entered -> Mark(puzzle.solution[index].toString(), cell * 0.62f, colors.streakAnswer, FontWeight.Medium)
            }
        }
    }
}

// --- Lights out: lit and dark lights ---

private val LightsSample by lazy { LightsGame(seed = 2).state }

@Composable
private fun LightsThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val state = LightsSample
    val gap = side * 0.04f
    val cell = (side * 0.84f - gap * (state.size - 1)) / state.size
    Grid(rows = state.size, cols = state.size, gap = gap) { r, c ->
        if (state.lit[r * state.size + c]) {
            Cell(cell, colors.pendingFill, null, 1.dp, cell * 0.25f) { Box(Modifier.size(cell * 0.42f).background(colors.pendingOn, CircleShape)) }
        } else {
            Cell(cell, colors.bgSurface, colors.borderStrong.copy(alpha = 0.5f), 1.dp, cell * 0.25f) {
                Box(Modifier.size(cell * 0.3f).border(1.5.dp, colors.borderStrong.copy(alpha = 0.4f), CircleShape))
            }
        }
    }
}

// --- Sliding puzzle: numbered tiles, the ones in their place in green ---

private val SlideSample by lazy { SlideGame(seed = 4).state }

@Composable
private fun SlideThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val state = SlideSample
    val gap = side * 0.04f
    val tile = minOf((side * 0.84f - gap * (state.cols - 1)) / state.cols, (side * 0.86f - gap * (state.rows - 1)) / state.rows)
    Grid(rows = state.rows, cols = state.cols, gap = gap) { r, c ->
        val index = r * state.cols + c
        val number = state.tiles[index]
        if (number == 0) {
            Box(Modifier.size(tile))
        } else {
            val home = state.isHome(index)
            Cell(tile, colors.bgSurface, if (home) colors.correctFill.copy(alpha = 0.6f) else colors.borderStrong.copy(alpha = 0.5f), 1.dp, tile * 0.2f) {
                Mark(number.toString(), tile * 0.5f, if (home) colors.correctFill else colors.textPrimary)
            }
        }
    }
}

// --- Target number: the target and the four numbers on the table ---

private val TargetSample by lazy { TargetGenerator.generate(9) }

@Composable
private fun TargetThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val puzzle = TargetSample
    val gap = side * 0.04f
    val tile = (side * 0.86f - gap * 3) / 4
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(side * 0.08f)) {
        Box(
            Modifier
                .background(colors.bgSurface, androidx.compose.foundation.shape.RoundedCornerShape(side * 0.08f))
                .size(side * 0.46f, side * 0.34f),
            contentAlignment = Alignment.Center,
        ) { Mark(puzzle.target.toString(), side * 0.26f, colors.pendingFill) }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            puzzle.numbers.forEach { number ->
                Cell(tile, colors.bgSurface, colors.borderStrong.copy(alpha = 0.5f), 1.dp, tile * 0.2f) { Mark(number.toString(), tile * 0.5f, colors.textPrimary) }
            }
        }
    }
}

// --- Pipes: the pipes of a shuffled board, joined sides green and open ends orange ---

private val PipesSample by lazy { PipesGenerator.generate(3).start }

@Composable
private fun PipesThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val tiles = PipesSample
    val size = PipesGenerator.SIZE
    val matched = PipesGenerator.matched(tiles, size)
    val gap = side * 0.02f
    val tile = (side * 0.9f - gap * (size - 1)) / size
    Grid(rows = size, cols = size, gap = gap) { r, c ->
        val index = r * size + c
        Cell(tile, colors.bgSurface, colors.borderStrong.copy(alpha = 0.3f), 0.5.dp, tile * 0.2f) {
            Canvas(Modifier.size(tile)) { drawPipes(tiles[index], matched[index], colors.correctFill, colors.borderStrong, colors.textSecondary) }
        }
    }
}

/** A small board of the one-line game: a snake of filled squares from number 1 to number 3. */
@Composable
private fun LineThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val size = 3
    val gap = side * 0.04f
    val tile = (side * 0.8f - gap * (size - 1)) / size
    // The line runs 0 1 2 / 5 4 3 / 6 7 8, so every square is filled; numbers sit on the start, the turn and the end.
    val numbers = mapOf(0 to "1", 5 to "2", 8 to "3")
    Grid(rows = size, cols = size, gap = gap) { r, c ->
        val index = r * size + c
        Cell(tile, colors.correctFill.copy(alpha = 0.35f), colors.borderStrong.copy(alpha = 0.3f), 0.5.dp, tile * 0.2f) {
            numbers[index]?.let { Mark(it, tile * 0.5f, colors.textPrimary) }
        }
    }
}

/** Three towers with a few disks: the big one at the bottom of the first, a smaller one on the second. */
@Composable
private fun HanoiThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val disks = listOf(listOf(2, 1, 0), listOf(), listOf(3))
    Canvas(Modifier.size(side * 0.9f)) {
        val tower = size.width / 3f
        val peg = size.width * 0.03f
        val diskHeight = size.height * 0.13f
        for (t in 0 until 3) {
            val center = t * tower + tower / 2f
            drawRoundRect(colors.borderStrong.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(center - peg / 2f, size.height * 0.18f), androidx.compose.ui.geometry.Size(peg, size.height * 0.72f), androidx.compose.ui.geometry.CornerRadius(peg / 2f))
            disks[t].forEachIndexed { level, disk ->
                val width = tower * (0.45f + 0.15f * disk)
                drawRoundRect(
                    com.mehmtcan.brainscroll.ui.memory.symbolColor(com.mehmtcan.brainscroll.game.memory.CardSymbol.entries[disk], colors),
                    androidx.compose.ui.geometry.Offset(center - width / 2f, size.height * 0.9f - diskHeight * (level + 1) - 2f * level),
                    androidx.compose.ui.geometry.Size(width, diskHeight),
                    androidx.compose.ui.geometry.CornerRadius(diskHeight / 2.5f),
                )
            }
        }
    }
}

/** A small 4x4 board like the game's: rounded cells in two tones, the crowns of a finished board, one of them white (locked). */
@Composable
private fun QueensThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val size = 4
    val queens = mapOf(0 * size + 1 to false, 1 * size + 3 to true, 2 * size + 0 to false, 3 * size + 2 to false)
    val gap = side * 0.03f
    val tile = (side * 0.9f - gap * (size - 1)) / size
    Grid(rows = size, cols = size, gap = gap) { r, c ->
        val fill = if ((r + c) % 2 == 1) colors.borderStrong.copy(alpha = 0.22f) else colors.bgSurface
        Cell(tile, fill, colors.borderStrong.copy(alpha = 0.35f), 0.5.dp, tile * 0.2f) {
            val locked = queens[r * size + c]
            if (locked != null) {
                Canvas(Modifier.size(tile * 0.7f)) { drawCrown(if (locked) colors.textPrimary else colors.pendingFill) }
            }
        }
    }
}

/** A small 4x4 board: trees and the tents next to them, on cells like the game's. */
@Composable
private fun TentsThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val size = 4
    // true is a tent, false a tree
    val pieces = mapOf(0 * size + 0 to false, 0 * size + 1 to true, 1 * size + 3 to false, 2 * size + 3 to true, 2 * size + 0 to true, 3 * size + 0 to false)
    val gap = side * 0.03f
    val tile = (side * 0.9f - gap * (size - 1)) / size
    Grid(rows = size, cols = size, gap = gap) { r, c ->
        Cell(tile, colors.bgSurface, colors.borderStrong.copy(alpha = 0.35f), 0.5.dp, tile * 0.2f) {
            val tent = pieces[r * size + c]
            if (tent != null) {
                Canvas(Modifier.size(tile * 0.72f)) { if (tent) drawTent(colors.pendingFill, colors.bgSurface) else drawTree(colors.correctFill, colors.borderStrong) }
            }
        }
    }
}

// --- Jigsaw sudoku: the regions of a real board, drawn like the board of the game (dark cells, thick rounded lines) ---

private val JigsawSample by lazy { JigsawGenerator.generate(56) }

/**
 * The numbers shown on the jigsaw picture: in a few regions the given cell nearest the middle of the region, and in two more the
 * right number of an empty cell, as if the player had entered it. Each pair is a cell and whether it is entered.
 */
private val JigsawShown by lazy {
    val puzzle = JigsawSample
    val regionOf = puzzle.layout.regionOf
    fun middleOf(region: Int, wanted: (Int) -> Boolean): Int? {
        val cells = (0 until 81).filter { regionOf[it] == region }
        val row = cells.sumOf { it / 9 } / 9f
        val column = cells.sumOf { it % 9 } / 9f
        return cells.filter(wanted).minByOrNull { (it / 9 - row) * (it / 9 - row) + (it % 9 - column) * (it % 9 - column) }
    }
    val given = (0 until 6).mapNotNull { region -> middleOf(region) { puzzle.givens[it] != 0 }?.let { it to false } }
    val entered = (6 until 8).mapNotNull { region -> middleOf(region) { puzzle.givens[it] == 0 }?.let { it to true } }
    given + entered
}

@Composable
private fun JigsawThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    val puzzle = JigsawSample
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    Canvas(Modifier.size(side * 0.88f)) {
        val n = 9
        val frame = size.width * 0.035f
        val gap = size.width * 0.018f
        val cell = (size.width - 2 * frame - (n - 1) * gap) / n
        val step = cell + gap
        val inner = androidx.compose.ui.geometry.Offset(frame, frame)
        val corner = androidx.compose.ui.geometry.CornerRadius(cell * 0.22f)
        drawRoundRect(colors.borderStrong.copy(alpha = 0.3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(frame * 1.8f))
        for (r in 0 until n) for (c in 0 until n) {
            drawRoundRect(colors.bgSurface, androidx.compose.ui.geometry.Offset(inner.x + c * step, inner.y + r * step), androidx.compose.ui.geometry.Size(cell, cell), corner)
        }
        // A few numbers: the givens in white, the entered ones in blue like in the game.
        for ((index, entered) in JigsawShown) {
            val digit = if (entered) puzzle.solution[index] else puzzle.givens[index]
            val text = measurer.measure(
                digit.toString(),
                androidx.compose.ui.text.TextStyle(fontSize = (cell * 1.05f).toSp(), fontWeight = if (entered) FontWeight.Medium else FontWeight.Bold),
            )
            val center = androidx.compose.ui.geometry.Offset(inner.x + (index % n) * step + cell / 2, inner.y + (index / n) * step + cell / 2)
            drawText(
                text,
                color = if (entered) colors.streakAnswer else colors.textPrimary,
                topLeft = androidx.compose.ui.geometry.Offset(center.x - text.size.width / 2f, center.y - text.size.height / 2f),
            )
        }
        drawRegionBorders(
            regionOf = puzzle.layout.regionOf,
            size = n,
            step = step,
            origin = androidx.compose.ui.geometry.Offset(inner.x - gap / 2, inner.y - gap / 2),
            thickness = frame * 0.6f,
            radius = cell * 0.55f,
            color = com.mehmtcan.brainscroll.ui.sudoku.boardLine(colors),
        )
        drawRoundRect(
            com.mehmtcan.brainscroll.ui.sudoku.boardLine(colors),
            topLeft = androidx.compose.ui.geometry.Offset(frame * 0.3f, frame * 0.3f),
            size = androidx.compose.ui.geometry.Size(size.width - frame * 0.6f, size.height - frame * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(frame * 1.6f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = frame * 0.6f),
        )
    }
}

// --- Block out: a small board with a few pieces, the orange target in the middle row and the exit beside it ---

@Composable
private fun BlockOutThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    // The pieces of a real-looking board: letters are pieces, X is the target, . is empty.
    val rows = listOf("ABB.C", "A.DDC", ".XXE.", "FF.E.", "..GG.")
    val palette = com.mehmtcan.brainscroll.ui.blockout.piecePalette(colors)
    Canvas(Modifier.size(side * 0.88f)) {
        val n = 5
        val gap = size.width * 0.03f
        val cell = (size.width - gap * (n - 1)) / n
        val step = cell + gap
        for (r in 0 until n) for (c in 0 until n) {
            drawRoundRect(colors.bgSurface, androidx.compose.ui.geometry.Offset(c * step, r * step), androidx.compose.ui.geometry.Size(cell, cell), androidx.compose.ui.geometry.CornerRadius(cell * 0.2f))
        }
        for (letter in rows.joinToString("").filter { it != '.' }.toSet()) {
            val cells = (0 until n * n).filter { rows[it / n][it % n] == letter }
            val first = cells.first()
            val last = cells.last()
            val left = (first % n) * step
            val top = (first / n) * step
            val width = (last % n - first % n) * step + cell
            val height = (last / n - first / n) * step + cell
            val color = if (letter == 'X') colors.streakDay else palette[(letter - 'A') % palette.size]
            drawRoundRect(color, androidx.compose.ui.geometry.Offset(left + gap * 0.3f, top + gap * 0.3f), androidx.compose.ui.geometry.Size(width - gap * 0.6f, height - gap * 0.6f), androidx.compose.ui.geometry.CornerRadius(cell * 0.25f))
            if (letter == 'X') {
                // The arrows of the target.
                val side = cell * 0.5f
                drawTargetMark(colors.bgPage, androidx.compose.ui.geometry.Offset(left + width / 2 - side / 2, top + height / 2 - side / 2), side)
            }
        }
        // The exit: a notch in the frame on the right of the middle row.
        drawRoundRect(colors.streakDay, androidx.compose.ui.geometry.Offset(size.width - gap * 0.5f, 2 * step + cell * 0.25f), androidx.compose.ui.geometry.Size(gap * 0.5f, cell * 0.5f))
    }
}

// --- Crossword: a small arrow crossword, clue squares with an arrow and the letters of two answers ---

@Composable
private fun CrosswordThumb(side: Dp) {
    val colors = BrainScrollTheme.colors
    // # is a clue square, '>' a clue with an arrow to the right, 'v' a clue with an arrow down; other characters are letters.
    val rows = listOf(">KAR", "vA.A", "SU.Y", "O.EV").map { it }
    val size = 4
    val gap = side * 0.03f
    val tile = (side * 0.88f - gap * (size - 1)) / size
    Grid(rows = size, cols = size, gap = gap) { r, c ->
        when (val ch = rows[r][c]) {
            '>', 'v' -> Cell(tile, colors.bgRaised, colors.borderStrong.copy(alpha = 0.35f), 0.5.dp, tile * 0.15f) {
                Canvas(Modifier.size(tile * 0.5f)) {
                    val w = this.size.width
                    val arrow = androidx.compose.ui.graphics.Path().apply {
                        if (ch == '>') { moveTo(w * 0.2f, w * 0.1f); lineTo(w * 0.9f, w * 0.5f); lineTo(w * 0.2f, w * 0.9f) } else { moveTo(w * 0.1f, w * 0.2f); lineTo(w * 0.5f, w * 0.9f); lineTo(w * 0.9f, w * 0.2f) }
                        close()
                    }
                    drawPath(arrow, colors.streakDay)
                }
            }
            '.' -> Cell(tile, colors.bgSurface, colors.borderStrong.copy(alpha = 0.3f), 0.5.dp, tile * 0.15f)
            else -> Cell(tile, colors.bgSurface, colors.borderStrong.copy(alpha = 0.3f), 0.5.dp, tile * 0.15f) { Mark(ch.toString(), tile * 0.6f, colors.textPrimary) }
        }
    }
}
