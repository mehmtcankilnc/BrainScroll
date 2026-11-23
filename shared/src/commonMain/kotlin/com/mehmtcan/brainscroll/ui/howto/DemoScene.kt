package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.mines.drawFlag
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.GuideColor
import com.mehmtcan.brainscroll.ui.theme.BrainScrollColors
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion

/** What a cell of a scene is made of: the unopened cell of a mine board, an opened one, a given number, a plain cell. */
enum class SceneFill { Empty, Hidden, Open, Given, Clue, Black }

/** A mark on a cell: a flag, a mine, a cross, a dot. */
enum class SceneMark { None, Flag, Mine, Cross, Dot, Fill, Shade, Tree, Tent, Queen, Lamp, ArrowUp, ArrowRight, ArrowDown, ArrowLeft, ChevronUp, ChevronRight, ChevronDown, ChevronLeft, Node }

/** What a ring or a tint on cells means: [Target] is what the guide points at, [Evidence] the cells it rests on, the rest colour a whole line or group. */
enum class SceneRole { Target, Evidence, Focus, Good, Bad, Present, Exact, Elsewhere, Absent }

/** The look of one cell before anything happens. [ink] is the colour of the text, null for the normal one. */
class SceneCell(val text: String = "", val fill: SceneFill = SceneFill.Empty, val mark: SceneMark = SceneMark.None, val ink: Color? = null)

/** Something that happens in a scene at the time [at] of its loop (0 to 1). */
sealed class SceneEvent(val at: Float)

/** [text] appears in [cell] (it pops in). With [pencil] it is small, as a note. */
class ScenePlace(at: Float, val cell: Int, val text: String, val ink: Color? = null, val fill: SceneFill? = null) : SceneEvent(at)

/** The hidden [cell] is opened and shows [text] (it pops in). */
class SceneOpen(at: Float, val cell: Int, val text: String, val ink: Color? = null) : SceneEvent(at)

/** The opened [cell] turns face down again (a memory card that did not match). */
class SceneClose(at: Float, val cell: Int) : SceneEvent(at)

/** The [cell] gets a [mark] (a flag, a cross...). */
class SceneSetMark(at: Float, val cell: Int, val mark: SceneMark) : SceneEvent(at)

/** The [cells] are ringed (see [SceneRole]) from [at] to [until]: the ring fades in and out. */
class SceneRing(at: Float, val until: Float, val cells: IntArray, val role: SceneRole) : SceneEvent(at)

/** The [cells] are tinted from [at] to [until]. */
class SceneTint(at: Float, val until: Float, val cells: IntArray, val role: SceneRole) : SceneEvent(at)

/** The hand taps [cell] at [at]. */
class SceneTapAt(at: Float, val cell: Int) : SceneEvent(at)

/** A line along one [side] of [cell] (0 up, 1 right, 2 down, 3 left) is drawn at [at] and stays; with [cross] it is a small cross there instead: no line. */
class SceneEdge(at: Float, val cell: Int, val side: Int, val cross: Boolean = false) : SceneEvent(at)

/** A straight line between the middles of [a] and [b] from [at] to [until]; [role] gives its colour (Bad is red: a line that crosses another). */
class SceneLine(at: Float, val until: Float, val a: Int, val b: Int, val role: SceneRole) : SceneEvent(at)

/** The pipe of [cell] turns into [mask] (the sides it reaches, as bits: 1 up, 2 right, 4 down, 8 left) at [at]. */
class ScenePipe(at: Float, val cell: Int, val mask: Int) : SceneEvent(at)

/**
 * A small picture of a board with a script: the cells as they start and the events that happen over one loop of the picture. It is
 * data only; [DemoSceneView] draws it. [thickEvery] draws a thicker line every that many cells (the boxes of a sudoku), 0 for none.
 */
class DemoScene(
    val rows: Int,
    val cols: Int,
    val cells: List<SceneCell>,
    val events: List<SceneEvent>,
    val thickEvery: Int = 0,
    /** The colour of a number by its text (the numbers of a mine board are coloured), or null for the normal ink. */
    val numberInk: ((String, BrainScrollColors) -> Color)? = null,
    /** The colour that [SceneRole.Focus] tints with: the colour of the game. */
    val focus: Color = Color.Unspecified,
    /** The region of every cell (the shapes of a jigsaw, the colour areas of a queens board), or null for none. Regions are tinted and have a line between them. */
    val regions: IntArray? = null,
    /** The sides a pipe reaches in every cell at the start (see [ScenePipe]), or null for no pipes. */
    val pipes: IntArray? = null,
    /** Dots at the corners of the cells: the points a fence is drawn between. */
    val dots: Boolean = false,
    /** Every text is the size of the longest one (the words of a topic board): words of different sizes look wrong side by side. */
    val uniformText: Boolean = false,
) {
    init {
        require(cells.size == rows * cols) { "a cell for every place" }
        require(regions == null || regions.size == rows * cols) { "a region for every place" }
        require(pipes == null || pipes.size == rows * cols) { "a pipe for every place" }
    }

    fun index(row: Int, column: Int) = row * cols + column
}

/** Builds a [DemoScene] row by row and event by event. */
class DemoSceneBuilder(private val rows: Int, private val cols: Int) {
    private val cells = MutableList(rows * cols) { SceneCell() }
    private val events = ArrayList<SceneEvent>()
    private var regions: IntArray? = null
    private var pipes: IntArray? = null
    private var dots = false
    private var uniformText = false

    fun cell(row: Int, column: Int) = row * cols + column

    /** The regions of the cells, one text per row and a digit per cell (the same digit is the same region). */
    fun regions(vararg rows: String) {
        regions = IntArray(this.rows * cols) { rows[it / cols].filter { c -> c != ' ' }[it % cols] - '0' }
    }

    /** The pipes at the start: one text per row, a hex digit per cell (the bits of [ScenePipe]). */
    fun pipes(vararg rows: String) {
        pipes = IntArray(this.rows * cols) { rows[it / cols].filter { c -> c != ' ' }[it % cols].digitToInt(16) }
    }

    fun dots() {
        dots = true
    }

    fun uniformText() {
        uniformText = true
    }

    /** A line on a [side] of a cell, from [at] on (0f: from the start). */
    fun edge(at: Float, row: Int, column: Int, side: Int) {
        events += SceneEdge(at, cell(row, column), side)
    }

    fun noEdge(at: Float, row: Int, column: Int, side: Int) {
        events += SceneEdge(at, cell(row, column), side, cross = true)
    }

    fun line(at: Float, until: Float, role: SceneRole, from: Pair<Int, Int>, to: Pair<Int, Int>) {
        events += SceneLine(at, until, cell(from.first, from.second), cell(to.first, to.second), role)
    }

    fun turn(at: Float, row: Int, column: Int, mask: Int) {
        events += ScenePipe(at, cell(row, column), mask)
    }

    /** Sets the starting look of a cell. */
    fun set(row: Int, column: Int, fill: SceneFill, text: String = "", mark: SceneMark = SceneMark.None, ink: Color? = null) {
        cells[cell(row, column)] = SceneCell(text, fill, mark, ink)
    }

    /**
     * Sets a whole row from text: one character per cell, a digit is a [SceneFill.Given] number (or a [given] fill), a dot an empty cell, an
     * `h` a hidden one. Spaces are ignored.
     */
    fun row(row: Int, text: String, given: SceneFill = SceneFill.Given) {
        text.filter { it != ' ' }.forEachIndexed { column, ch ->
            cells[cell(row, column)] = when {
                ch == '.' -> SceneCell()
                ch == 'h' -> SceneCell(fill = SceneFill.Hidden)
                ch == 'f' -> SceneCell(fill = SceneFill.Hidden, mark = SceneMark.Flag)
                ch == 'm' -> SceneCell(fill = SceneFill.Hidden, mark = SceneMark.Mine)
                ch == '_' -> SceneCell(fill = SceneFill.Open)
                ch == '#' -> SceneCell(fill = SceneFill.Clue)
                else -> SceneCell(ch.toString(), given)
            }
        }
    }

    /** A dark cell of a cross sums board with the sum of the block along its row ([across], top right) and down its column ([down], bottom left). */
    fun clueCell(row: Int, column: Int, across: Int? = null, down: Int? = null) {
        set(row, column, SceneFill.Black, if (across == null && down == null) "" else "${across ?: ""}|${down ?: ""}")
    }

    /** [text] appears in a cell ([fill] gives the cell a look too: a tile that slides into a gap); an empty text clears the cell. */
    fun place(at: Float, row: Int, column: Int, text: String, ink: Color? = null, fill: SceneFill? = null) {
        events += ScenePlace(at, cell(row, column), text, ink, fill)
    }

    fun open(at: Float, row: Int, column: Int, text: String, ink: Color? = null) {
        events += SceneOpen(at, cell(row, column), text, ink)
    }

    fun close(at: Float, row: Int, column: Int) {
        events += SceneClose(at, cell(row, column))
    }

    fun mark(at: Float, row: Int, column: Int, mark: SceneMark) {
        events += SceneSetMark(at, cell(row, column), mark)
    }

    fun ring(at: Float, until: Float, role: SceneRole, vararg cells: Pair<Int, Int>) {
        events += SceneRing(at, until, IntArray(cells.size) { cell(cells[it].first, cells[it].second) }, role)
    }

    fun ringCells(at: Float, until: Float, role: SceneRole, cells: List<Int>) {
        events += SceneRing(at, until, cells.toIntArray(), role)
    }

    fun tint(at: Float, until: Float, role: SceneRole, cells: List<Int>) {
        events += SceneTint(at, until, cells.toIntArray(), role)
    }

    /** The cells of row [row], of column [column] and of the [size] by [size] box that holds ([row], [column]). */
    fun rowCells(row: Int) = (0 until cols).map { cell(row, it) }

    fun columnCells(column: Int) = (0 until rows).map { cell(it, column) }

    fun boxCells(row: Int, column: Int, size: Int = 3) = (0 until size).flatMap { r -> (0 until size).map { c -> cell(row / size * size + r, column / size * size + c) } }

    fun tap(at: Float, row: Int, column: Int) {
        events += SceneTapAt(at, cell(row, column))
    }

    fun build(thickEvery: Int = 0, numberInk: ((String, BrainScrollColors) -> Color)? = null, focus: Color = Color.Unspecified) =
        DemoScene(rows, cols, cells.toList(), events.sortedBy { it.at }, thickEvery, numberInk, focus, regions, pipes, dots, uniformText)
}

/** Builds a scene: `demoScene(9, 9, thickEvery = 3) { row(0, "53..7....") ... }`. */
fun demoScene(rows: Int, cols: Int, thickEvery: Int = 0, numberInk: ((String, BrainScrollColors) -> Color)? = null, focus: Color = Color.Unspecified, script: DemoSceneBuilder.() -> Unit): DemoScene =
    DemoSceneBuilder(rows, cols).apply(script).build(thickEvery, numberInk, focus)

/**
 * The time of a picture as a state: read it in the draw phase and the picture moves without being composed again. Under reduced motion it
 * is the frame [REDUCED_DEMO_TIME] and nothing moves.
 */
@Composable
fun rememberDemoTimeState(cycleMillis: Int): State<Float> {
    if (LocalReduceMotion.current) return remember { androidx.compose.runtime.mutableFloatStateOf(REDUCED_DEMO_TIME) }
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "howToScene")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(cycleMillis, easing = androidx.compose.animation.core.LinearEasing),
            androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "howToSceneTime",
    )
}

/**
 * Draws a [DemoScene] for the time of [time]. Everything that depends on the size (where the cells are, the text laid out) is made once
 * per size; a frame only moves the numbers that change, so a picture of 81 cells costs about as much as one of 9.
 */
@Composable
fun DemoSceneView(scene: DemoScene, time: State<Float>, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val hand = rememberHandPainter()
    androidx.compose.foundation.layout.Box(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val frame = SceneFrame(scene, size, density, measurer, colors)
                onDrawBehind { frame.draw(this, time.value, hand) }
            },
    )
}

private class EdgeLine(val at: Float, val x1: Float, val y1: Float, val x2: Float, val y2: Float, val cross: Boolean)

private val RegionColors = listOf(
    Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFD54F), Color(0xFFBA68C8),
    Color(0xFF4DB6AC), Color(0xFFFF8A65), Color(0xFFA1887F), Color(0xFF90A4AE),
)

/** What depends on the size of a picture, made once: the cell rectangles, the lines, the text laid out, and the events of every cell. */
private class SceneFrame(
    private val scene: DemoScene,
    size: Size,
    density: Float,
    measurer: TextMeasurer,
    private val colors: BrainScrollColors,
) {
    // A margin round the board: the picture sits in a box with rounded corners, and cells that reach the edge would have their corners cut off.
    private val margin = minOf(size.width, size.height) * 0.05f
    private val side = minOf((size.width - 2 * margin) / scene.cols, (size.height - 2 * margin) / scene.rows)
    private val left = (size.width - side * scene.cols) / 2f
    private val top = (size.height - side * scene.rows) / 2f
    private val gap = 1.5f * density
    private val corner = CornerRadius(side * 0.14f)
    private val cellSize = Size(side - gap, side - gap)
    private val count = scene.rows * scene.cols
    private val focus = if (scene.focus == Color.Unspecified) colors.pendingFill else scene.focus
    private val layouts = HashMap<String, TextLayoutResult>()
    /** The two small numbers of the dark cells of a cross sums board, laid out once. */
    private val sums = HashMap<String, TextLayoutResult>()
    private val perCell: Array<Array<SceneEvent>>
    private val rings: Array<SceneRing>
    private val tints: Array<SceneTint>
    private val taps: List<DemoTap>
    private val pad = side * 0.5f
    private val scratch = Path()
    /** The lines between regions as x1, y1, x2, y2 for each. */
    private val regionLines: FloatArray
    private val edges: Array<EdgeLine>
    private val lineEvents: Array<SceneLine>

    init {
        val style = TextStyle(fontSize = with(androidx.compose.ui.unit.Density(density)) { (side * 0.56f).toSp() }, fontWeight = FontWeight.Bold, color = Color.White)
        val texts = HashSet<String>()
        scene.cells.forEach { if (it.text.isNotEmpty()) texts += it.text }
        for (e in scene.events) {
            if (e is ScenePlace) texts += e.text
            if (e is SceneOpen) texts += e.text
        }
        // On a board with uniform text the scale that fits the longest word is the scale of every word.
        var uniform = 1f
        if (scene.uniformText) for (text in texts) {
            val width = measurer.measure(text, style, constraints = Constraints()).size.width
            if (width > side * 0.92f) uniform = minOf(uniform, side * 0.92f / width)
        }
        val textStyle = if (uniform < 1f) style.copy(fontSize = style.fontSize * uniform) else style
        val smallStyle = style.copy(fontSize = style.fontSize * 0.55f)
        for (cell in scene.cells) if (cell.fill == SceneFill.Black) {
            for (part in cell.text.split('|')) if (part.isNotEmpty() && part !in sums) sums[part] = measurer.measure(part, smallStyle, constraints = Constraints())
        }
        for (text in texts) {
            var layout = measurer.measure(text, textStyle, constraints = Constraints())
            // A long word is made smaller so that it stays inside its cell.
            if (layout.size.width > side * 0.92f) layout = measurer.measure(text, style.copy(fontSize = style.fontSize * (side * 0.92f / layout.size.width)), constraints = Constraints())
            layouts[text] = layout
        }
        val lists = Array(count) { ArrayList<SceneEvent>(2) }
        for (e in scene.events) {
            when (e) {
                is ScenePlace -> lists[e.cell] += e
                is SceneOpen -> lists[e.cell] += e
                is SceneClose -> lists[e.cell] += e
                is SceneSetMark -> lists[e.cell] += e
                is ScenePipe -> lists[e.cell] += e
                else -> Unit
            }
        }
        val reg = scene.regions
        val lines = ArrayList<Float>()
        if (reg != null) {
            for (r in 0 until scene.rows) for (c in 0 until scene.cols) {
                val here = reg[r * scene.cols + c]
                if (c + 1 < scene.cols && reg[r * scene.cols + c + 1] != here) lines += listOf(left + (c + 1) * side, top + r * side, left + (c + 1) * side, top + (r + 1) * side)
                if (r + 1 < scene.rows && reg[(r + 1) * scene.cols + c] != here) lines += listOf(left + c * side, top + (r + 1) * side, left + (c + 1) * side, top + (r + 1) * side)
            }
        }
        regionLines = lines.toFloatArray()
        lineEvents = scene.events.filterIsInstance<SceneLine>().toTypedArray()
        edges = scene.events.filterIsInstance<SceneEdge>().map { e ->
            val x = left + (e.cell % scene.cols) * side
            val y = top + (e.cell / scene.cols) * side
            when (e.side) {
                0 -> EdgeLine(e.at, x, y, x + side, y, e.cross)
                1 -> EdgeLine(e.at, x + side, y, x + side, y + side, e.cross)
                2 -> EdgeLine(e.at, x, y + side, x + side, y + side, e.cross)
                else -> EdgeLine(e.at, x, y, x, y + side, e.cross)
            }
        }.toTypedArray()
        perCell = Array(count) { lists[it].toTypedArray() }
        rings = scene.events.filterIsInstance<SceneRing>().toTypedArray()
        tints = scene.events.filterIsInstance<SceneTint>().toTypedArray()
        taps = scene.events.filterIsInstance<SceneTapAt>().map { DemoTap(it.at, Offset(left + (it.cell % scene.cols + 0.5f) * side, top + (it.cell / scene.cols + 0.5f) * side)) }
    }

    private fun origin(cell: Int) = Offset(left + (cell % scene.cols) * side + gap / 2f, top + (cell / scene.cols) * side + gap / 2f)

    /** How strong something that is on from [from] to [until] is at [t]: it fades in and out. */
    private fun strength(t: Float, from: Float, until: Float): Float {
        if (t < from || t > until) return 0f
        val edge = 0.04f
        return minOf(((t - from) / edge).coerceIn(0f, 1f), ((until - t) / edge).coerceIn(0f, 1f))
    }

    fun draw(scope: DrawScope, t: Float, hand: androidx.compose.ui.graphics.painter.Painter) = with(scope) {
        val fade = demoFade(t)
        for (cell in 0 until count) {
            val base = scene.cells[cell]
            var fill = base.fill
            var text = base.text
            var ink = base.ink
            var mark = base.mark
            var pipe = scene.pipes?.get(cell) ?: 0
            var pop = 1f
            val events = perCell[cell]
            for (e in events) {
                if (t < e.at) break
                when (e) {
                    is ScenePlace -> { text = e.text; ink = e.ink; pop = demoPop(t, e.at); if (e.fill != null) fill = e.fill else if (e.text.isEmpty()) fill = SceneFill.Empty }
                    is SceneOpen -> { fill = SceneFill.Open; text = e.text; ink = e.ink; pop = demoPop(t, e.at) }
                    is SceneClose -> { fill = SceneFill.Hidden; text = "" }
                    is SceneSetMark -> mark = e.mark
                    is ScenePipe -> pipe = e.mask
                    else -> Unit
                }
            }
            val o = origin(cell)
            val body = when (fill) {
                SceneFill.Hidden -> colors.bgRaised
                SceneFill.Open -> colors.bgPage
                SceneFill.Given -> colors.bgRaised
                SceneFill.Empty -> colors.bgSurface
                SceneFill.Clue -> Color.Transparent
                SceneFill.Black -> colors.bgPage
            }
            if (fill != SceneFill.Clue) {
                drawRoundRect(body.copy(alpha = fade), o, cellSize, corner)
                drawRoundRect(colors.borderStrong.copy(alpha = (if (fill == SceneFill.Hidden) 0.5f else 0.28f) * fade), o, cellSize, corner, style = Stroke(1.dp.toPx()))
                val reg = scene.regions
                if (reg != null) drawRoundRect(RegionColors[reg[cell] % RegionColors.size].copy(alpha = 0.3f * fade), o, cellSize, corner)
            }
            // The tints: a row, a column or a box that the step looks at.
            for (tint in tints) {
                if (cell !in tint.cells) continue
                val s = strength(t, tint.at, tint.until)
                if (s <= 0f) continue
                drawRoundRect(roleColor(tint.role).copy(alpha = 0.26f * s * fade), o, cellSize, corner)
            }
            if (text.isNotEmpty() && fill != SceneFill.Black) {
                val layout = layouts[text]
                if (layout != null) {
                    val color = ink ?: scene.numberInk?.invoke(text, colors) ?: if (fill == SceneFill.Clue) colors.textSecondary else colors.textPrimary
                    val center = Offset(o.x + cellSize.width / 2f, o.y + cellSize.height / 2f)
                    scale(pop, pop, pivot = center) {
                        drawText(layout, color = color.copy(alpha = fade), topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
                    }
                }
            }
            if (fill == SceneFill.Black && text.isNotEmpty()) {
                // A dark cell with sums: a line from the top left to the bottom right, the sum along the row at the top right, the one down the column at the bottom left.
                drawLine(colors.borderStrong.copy(alpha = 0.6f * fade), o, Offset(o.x + cellSize.width, o.y + cellSize.height), 1.dp.toPx())
                val parts = text.split('|')
                sums[parts[0]]?.let { drawText(it, color = colors.textSecondary.copy(alpha = fade), topLeft = Offset(o.x + cellSize.width - it.size.width - side * 0.06f, o.y + side * 0.04f)) }
                sums[parts.getOrElse(1) { "" }]?.let { drawText(it, color = colors.textSecondary.copy(alpha = fade), topLeft = Offset(o.x + side * 0.06f, o.y + cellSize.height - it.size.height - side * 0.04f)) }
            }
            if (pipe != 0) drawPipe(pipe, o, fade, pop)
            if (mark != SceneMark.None) drawMark(mark, o, fade)
        }
        // The lines between regions.
        var i = 0
        while (i < regionLines.size) {
            drawLine(colors.textPrimary.copy(alpha = 0.85f * fade), Offset(regionLines[i], regionLines[i + 1]), Offset(regionLines[i + 2], regionLines[i + 3]), 2.4f * density, StrokeCap.Round)
            i += 4
        }
        // The lines of a fence and the crosses that say there is none.
        for (e in edges) {
            if (t < e.at) continue
            val p = if (e.at <= 0f) 1f else ((t - e.at) / 0.05f).coerceIn(0f, 1f)
            if (e.cross) {
                val mx = (e.x1 + e.x2) / 2f
                val my = (e.y1 + e.y2) / 2f
                val r = side * 0.13f * p
                val ink = colors.textSecondary.copy(alpha = fade)
                drawLine(ink, Offset(mx - r, my - r), Offset(mx + r, my + r), 2f * density, StrokeCap.Round)
                drawLine(ink, Offset(mx - r, my + r), Offset(mx + r, my - r), 2f * density, StrokeCap.Round)
            } else {
                drawLine(focus.copy(alpha = fade), Offset(e.x1, e.y1), Offset(e.x1 + (e.x2 - e.x1) * p, e.y1 + (e.y2 - e.y1) * p), 4.2f * density, StrokeCap.Round)
            }
        }
        // The lines of a graph between its points.
        for (l in lineEvents) {
            val s = strength(t, l.at, l.until)
            if (s <= 0f) continue
            val a = Offset(left + (l.a % scene.cols + 0.5f) * side, top + (l.a / scene.cols + 0.5f) * side)
            val b = Offset(left + (l.b % scene.cols + 0.5f) * side, top + (l.b / scene.cols + 0.5f) * side)
            drawLine(roleColor(l.role).copy(alpha = 0.95f * s * fade), a, b, 3.2f * density, StrokeCap.Round)
        }
        if (scene.dots) {
            for (r in 0..scene.rows) for (c in 0..scene.cols) drawCircle(colors.textSecondary.copy(alpha = fade), 2.6f * density, Offset(left + c * side, top + r * side))
        }
        // The thick lines of the boxes.
        if (scene.thickEvery > 0) {
            val line = 2.2f * density
            for (k in 0..scene.cols step scene.thickEvery) drawLine(colors.borderStrong.copy(alpha = fade), Offset(left + k * side, top), Offset(left + k * side, top + scene.rows * side), line)
            for (k in 0..scene.rows step scene.thickEvery) drawLine(colors.borderStrong.copy(alpha = fade), Offset(left, top + k * side), Offset(left + scene.cols * side, top + k * side), line)
        }
        // The rings: the thing the guide points at, and the things it rests on.
        for (ring in rings) {
            val s = strength(t, ring.at, ring.until)
            if (s <= 0f) continue
            val beat = if (ring.role == SceneRole.Target) 0.8f + 0.2f * kotlin.math.sin(t * 40f) else 1f
            for (cell in ring.cells) {
                val o = origin(cell)
                if (ring.role == SceneRole.Absent) {
                    drawRoundRect(colors.borderStrong.copy(alpha = s * fade), o, cellSize, corner, style = Stroke(3.dp.toPx()))
                } else if (ring.role == SceneRole.Exact) {
                    drawRoundRect(colors.correctFill.copy(alpha = s * fade), o, cellSize, corner, style = Stroke(3.dp.toPx()))
                } else if (ring.role == SceneRole.Elsewhere) {
                    drawRoundRect(
                        colors.correctFill.copy(alpha = s * fade), o, cellSize, corner,
                        style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()))),
                    )
                } else if (ring.role == SceneRole.Target) {
                    drawRoundRect(GuideColor.copy(alpha = 0.18f * s * fade), o, cellSize, corner)
                    drawRoundRect(GuideColor.copy(alpha = beat * s * fade), o, cellSize, corner, style = Stroke(3.dp.toPx()))
                } else {
                    drawRoundRect(GuideColor.copy(alpha = 0.12f * s * fade), o, cellSize, corner)
                    drawRoundRect(
                        GuideColor.copy(alpha = 0.8f * s * fade), o, cellSize, corner,
                        style = Stroke(1.6f.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 4.dp.toPx()))),
                    )
                }
            }
        }
        if (taps.isNotEmpty()) {
            val (tip, press) = demoHand(t, taps, Offset(left + scene.cols * side * 0.9f, top + scene.rows * side * 0.95f))
            drawHandCursor(hand, tip, side, press, fade)
        }
    }

    private fun roleColor(role: SceneRole): Color = when (role) {
        SceneRole.Focus -> focus
        SceneRole.Good -> colors.correctFill
        SceneRole.Bad -> colors.error
        SceneRole.Present -> colors.pendingFill
        SceneRole.Exact, SceneRole.Elsewhere -> colors.correctFill
        else -> GuideColor
    }

    private fun DrawScope.triangle(color: Color, a: Offset, b: Offset, c: Offset) {
        scratch.rewind()
        scratch.moveTo(a.x, a.y)
        scratch.lineTo(b.x, b.y)
        scratch.lineTo(c.x, c.y)
        scratch.close()
        drawPath(scratch, color)
    }

    private fun DrawScope.arrow(center: Offset, r: Float, dx: Float, dy: Float, fade: Float) {
        val ink = colors.textPrimary.copy(alpha = fade)
        val tail = Offset(center.x - dx * r * 1.1f, center.y - dy * r * 1.1f)
        val tip = Offset(center.x + dx * r * 1.1f, center.y + dy * r * 1.1f)
        drawLine(ink, tail, tip, 2.6f * density, StrokeCap.Round)
        // The head: two short lines back from the tip, to either side.
        val back = r * 0.7f
        drawLine(ink, tip, Offset(tip.x - dx * back - dy * back, tip.y - dy * back + dx * back), 2.6f * density, StrokeCap.Round)
        drawLine(ink, tip, Offset(tip.x - dx * back + dy * back, tip.y - dy * back - dx * back), 2.6f * density, StrokeCap.Round)
    }

    /** The sign of a greater-less board: the tip of the "<" is on the side of the smaller number. */
    private fun DrawScope.chevron(center: Offset, r: Float, dx: Float, dy: Float, fade: Float) {
        val ink = colors.textPrimary.copy(alpha = fade)
        val tip = Offset(center.x + dx * r * 0.6f, center.y + dy * r * 0.6f)
        val back = r * 0.9f
        drawLine(ink, tip, Offset(tip.x - dx * back - dy * back, tip.y - dy * back + dx * back), 2.8f * density, StrokeCap.Round)
        drawLine(ink, tip, Offset(tip.x - dx * back + dy * back, tip.y - dy * back - dx * back), 2.8f * density, StrokeCap.Round)
    }

    /** The pipe of a cell: a line from the middle to every side in [mask]. */
    private fun DrawScope.drawPipe(mask: Int, o: Offset, fade: Float, pop: Float) {
        val center = Offset(o.x + cellSize.width / 2f, o.y + cellSize.height / 2f)
        val ink = focus.copy(alpha = fade)
        val reach = side * 0.5f
        val width = side * 0.2f
        if (mask and 1 != 0) drawLine(ink, center, Offset(center.x, center.y - reach), width, StrokeCap.Round)
        if (mask and 2 != 0) drawLine(ink, center, Offset(center.x + reach, center.y), width, StrokeCap.Round)
        if (mask and 4 != 0) drawLine(ink, center, Offset(center.x, center.y + reach), width, StrokeCap.Round)
        if (mask and 8 != 0) drawLine(ink, center, Offset(center.x - reach, center.y), width, StrokeCap.Round)
        drawCircle(ink, width * 0.5f, center)
    }

    private fun DrawScope.drawMark(mark: SceneMark, o: Offset, fade: Float) {
        val center = Offset(o.x + cellSize.width / 2f, o.y + cellSize.height / 2f)
        val r = side * 0.26f
        when (mark) {
            SceneMark.Flag -> inset(o.x, o.y, size.width - o.x - cellSize.width, size.height - o.y - cellSize.height) {
                drawFlag(colors.textPrimary.copy(alpha = fade), colors.error.copy(alpha = fade))
            }
            SceneMark.Mine -> {
                drawCircle(colors.textPrimary.copy(alpha = fade), r * 0.8f, center)
                for (k in 0 until 8) {
                    val a = k * 0.7854f
                    drawLine(colors.textPrimary.copy(alpha = fade), center, Offset(center.x + kotlin.math.cos(a) * r * 1.3f, center.y + kotlin.math.sin(a) * r * 1.3f), 1.6f * 2f, StrokeCap.Round)
                }
            }
            SceneMark.Cross -> {
                val ink = colors.textSecondary.copy(alpha = fade)
                drawLine(ink, Offset(center.x - r, center.y - r), Offset(center.x + r, center.y + r), 2.2f * 2f, StrokeCap.Round)
                drawLine(ink, Offset(center.x - r, center.y + r), Offset(center.x + r, center.y - r), 2.2f * 2f, StrokeCap.Round)
            }
            SceneMark.Dot -> drawCircle(colors.textSecondary.copy(alpha = fade), r * 0.35f, center)
            SceneMark.Fill -> drawRoundRect(focus.copy(alpha = 0.92f * fade), Offset(o.x + side * 0.1f, o.y + side * 0.1f), Size(cellSize.width - side * 0.2f, cellSize.height - side * 0.2f), corner)
            SceneMark.Shade -> drawRoundRect(colors.absentOn.copy(alpha = 0.92f * fade), Offset(o.x + side * 0.06f, o.y + side * 0.06f), Size(cellSize.width - side * 0.12f, cellSize.height - side * 0.12f), corner)
            SceneMark.Tree -> {
                drawLine(colors.streakDay.copy(alpha = fade), Offset(center.x, center.y + r * 0.2f), Offset(center.x, center.y + r * 1.0f), r * 0.4f, StrokeCap.Butt)
                triangle(colors.correctFill.copy(alpha = fade), Offset(center.x, center.y - r * 1.15f), Offset(center.x - r * 1.0f, center.y + r * 0.45f), Offset(center.x + r * 1.0f, center.y + r * 0.45f))
            }
            SceneMark.Tent -> triangle(focus.copy(alpha = fade), Offset(center.x, center.y - r * 1.0f), Offset(center.x - r * 1.2f, center.y + r * 0.95f), Offset(center.x + r * 1.2f, center.y + r * 0.95f))
            SceneMark.Queen -> {
                scratch.rewind()
                scratch.moveTo(center.x - r * 1.0f, center.y + r * 0.85f)
                scratch.lineTo(center.x + r * 1.0f, center.y + r * 0.85f)
                scratch.lineTo(center.x + r * 1.05f, center.y - r * 0.7f)
                scratch.lineTo(center.x + r * 0.5f, center.y + r * 0.05f)
                scratch.lineTo(center.x, center.y - r * 1.0f)
                scratch.lineTo(center.x - r * 0.5f, center.y + r * 0.05f)
                scratch.lineTo(center.x - r * 1.05f, center.y - r * 0.7f)
                scratch.close()
                drawPath(scratch, colors.textPrimary.copy(alpha = fade))
            }
            SceneMark.Lamp -> {
                drawCircle(colors.pendingFill.copy(alpha = 0.3f * fade), r * 1.5f, center)
                drawCircle(colors.pendingFill.copy(alpha = fade), r * 0.85f, center)
            }
            SceneMark.ArrowUp -> arrow(center, r, 0f, -1f, fade)
            SceneMark.ArrowRight -> arrow(center, r, 1f, 0f, fade)
            SceneMark.ArrowDown -> arrow(center, r, 0f, 1f, fade)
            SceneMark.ArrowLeft -> arrow(center, r, -1f, 0f, fade)
            SceneMark.Node -> {
                drawCircle(colors.textPrimary.copy(alpha = fade), r * 0.95f, center)
                drawCircle(colors.bgPage.copy(alpha = fade), r * 0.45f, center)
            }
            SceneMark.ChevronUp -> chevron(center, r, 0f, -1f, fade)
            SceneMark.ChevronRight -> chevron(center, r, 1f, 0f, fade)
            SceneMark.ChevronDown -> chevron(center, r, 0f, 1f, fade)
            SceneMark.ChevronLeft -> chevron(center, r, -1f, 0f, fade)
            SceneMark.None -> Unit
        }
    }
}
