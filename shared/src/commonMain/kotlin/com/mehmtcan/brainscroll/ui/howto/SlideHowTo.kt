package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_slide_1
import brainscroll.shared.generated.resources.howto_card_slide_2
import brainscroll.shared.generated.resources.howto_card_slide_3
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the sliding puzzle card: a tile next to the empty space slides in, tiles in line slide together, and the order. */
internal val SlideHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_slide_1, { t -> SlideDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_slide_2, { t -> SlideDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_slide_3, { t -> SlideDemo(step = 2, t = t) }, cycleMillis = 4800),
    ),
)

private const val SIZE = 3

// Rows of the board: 0 is the empty space. The finished picture is 1 2 3 / 4 5 6 / 7 8 _.
private val Near = listOf(1, 2, 3, 4, 5, 0, 7, 8, 6)
private val Far = listOf(1, 2, 3, 0, 4, 5, 7, 8, 6)

private const val SLIDE = 0.1f

@Composable
internal fun SlideDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    // The board at the start, and the taps (cell and time). A tap slides the tile and the ones between it and the empty cell.
    val start = if (step == 0) Near else Far
    val taps: List<Pair<Int, Float>> = when (step) {
        0 -> listOf(8 to 0.3f)
        1 -> listOf(5 to 0.3f)
        else -> listOf(5 to 0.15f, 8 to 0.5f)
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.12f
        val gap = size.width * 0.025f
        val cell = (size.width - 2 * pad - gap * (SIZE - 1)) / SIZE
        val top = (size.height - (cell * SIZE + gap * (SIZE - 1))) / 2f
        fun origin(r: Int, c: Int) = Offset(pad + c * (cell + gap), top + r * (cell + gap))
        fun center(r: Int, c: Int) = origin(r, c) + Offset(cell / 2f, cell / 2f)
        val corner = CornerRadius(cell * 0.14f)

        // The board before each tap, found by playing the taps that are done; the tap that is going on moves its tiles.
        val board = start.toMutableList()
        // For the tiles that are moving now: tile -> (from cell, to cell, how far).
        val moving = HashMap<Int, Triple<Int, Int, Float>>()
        for ((cellTapped, at) in taps) {
            val empty = board.indexOf(0)
            val er = empty / SIZE
            val ec = empty % SIZE
            val tr = cellTapped / SIZE
            val tc = cellTapped % SIZE
            // The cells from the tapped tile to the one next to the empty cell, in the line of the empty cell.
            val line = ArrayList<Int>()
            if (tr == er) {
                val stepC = if (tc < ec) 1 else -1
                var c = tc
                while (c != ec) { line += tr * SIZE + c; c += stepC }
            } else {
                val stepR = if (tr < er) 1 else -1
                var r = tr
                while (r != er) { line += r * SIZE + tc; r += stepR }
            }
            val towards = if (tr == er) (if (tc < ec) 1 else -1) else (if (tr < er) SIZE else -SIZE)
            if (t >= at + SLIDE) {
                // Done: every tile of the line has moved one cell towards the empty cell.
                for (cellIndex in line.reversed()) board[cellIndex + towards] = board[cellIndex]
                board[line.first()] = 0
            } else if (t >= at) {
                val p = demoEase(demoSegment(t, at, at + SLIDE))
                for (cellIndex in line) moving[board[cellIndex]] = Triple(cellIndex, cellIndex + towards, p)
            }
        }

        fun home(tile: Int, cellIndex: Int) = tile == cellIndex + 1
        // A tile: a quiet tile with its number; green number and border when it is in its place.
        fun drawTile(number: Int, at: Offset, isHome: Boolean) {
            drawRoundRect(colors.bgSurface.copy(alpha = fade), at, Size(cell, cell), corner)
            val edge = if (isHome) colors.correctFill.copy(alpha = 0.6f) else colors.borderStrong.copy(alpha = 0.5f)
            drawRoundRect(edge.copy(alpha = edge.alpha * fade), at, Size(cell, cell), corner, style = Stroke(width = 1.5.dp.toPx()))
            drawCenteredText(measurer, number.toString(), at + Offset(cell / 2f, cell / 2f), cell * 0.42f, (if (isHome) colors.correctFill else colors.textPrimary).copy(alpha = fade))
        }
        for (cellIndex in 0 until SIZE * SIZE) {
            val tile = board[cellIndex]
            if (tile == 0 || moving.containsKey(tile)) continue
            drawTile(tile, origin(cellIndex / SIZE, cellIndex % SIZE), home(tile, cellIndex))
        }
        for ((tile, move) in moving) {
            val (from, to, p) = move
            val a = origin(from / SIZE, from % SIZE)
            val b = origin(to / SIZE, to % SIZE)
            drawTile(tile, Offset(a.x + (b.x - a.x) * p, a.y + (b.y - a.y) * p), home(tile, to) && p >= 1f)
        }
        // The empty space: only its outline.
        val emptyCell = board.indexOf(0).takeIf { moving.isEmpty() }
        if (emptyCell != null) {
            drawRoundRect(colors.borderStrong.copy(alpha = 0.18f * fade), origin(emptyCell / SIZE, emptyCell % SIZE), Size(cell, cell), corner, style = Stroke(width = 1.dp.toPx()))
        }

        val (hand, press) = demoHand(t, taps.map { (c, at) -> DemoTap(at, center(c / SIZE, c % SIZE)) }, Offset(size.width, size.height))
        drawHandCursor(handPainter, hand, cell * 0.95f, press, alpha = fade)
    }
}
