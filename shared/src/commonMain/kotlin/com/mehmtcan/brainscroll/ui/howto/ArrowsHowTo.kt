package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_arrows_start
import brainscroll.shared.generated.resources.howto_arrows_tech
import brainscroll.shared.generated.resources.howto_card_arrows_1
import brainscroll.shared.generated.resources.howto_card_arrows_2
import brainscroll.shared.generated.resources.howto_card_arrows_3
import com.mehmtcan.brainscroll.game.arrows.ArrowState
import com.mehmtcan.brainscroll.ui.arrows.arrowPalette
import com.mehmtcan.brainscroll.ui.arrows.cellsToEdge
import com.mehmtcan.brainscroll.ui.arrows.drawSnake
import com.mehmtcan.brainscroll.ui.arrows.polylineLength
import com.mehmtcan.brainscroll.ui.arrows.snakePoints
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the arrow escape card: slide an arrow out, an arrow in the way is a mistake, clear them all. */
internal val ArrowsHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_arrows_1, { t -> ArrowsDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_arrows_2, { t -> ArrowsDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_arrows_3, { t -> ArrowsDemo(step = 2, t = t) }, cycleMillis = 6400),
        sceneStep(Res.string.howto_arrows_tech, ArrowsChainScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_arrows_start, ArrowsStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private const val SIZE = 4

// A small board of four arrows, each a list of (row, column) from the tail to the head and the way the head points
// (0 up, 1 right, 2 down, 3 left). C is in front of B and D, so they are blocked until C has gone.
private val A = listOf(0 to 0, 0 to 1, 0 to 2) to 1
private val B = listOf(1 to 0, 1 to 1, 2 to 1) to 2
private val C = listOf(3 to 1, 3 to 2, 3 to 3) to 1
private val D = listOf(1 to 3, 2 to 3) to 2
private val Board = listOf(A, B, C, D)

private fun arrowState(of: Pair<List<Pair<Int, Int>>, Int>) = ArrowState(of.first.map { (r, c) -> r * SIZE + c }, of.second, alive = true, free = true)

/**
 * The picture of a step. [t] is the time of the loop: the hand comes to an arrow, taps it, and what the tap does is shown;
 * then the picture fades and starts over.
 */
@Composable
internal fun ArrowsDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val palette = arrowPalette(colors)
    val tile = colors.bgSurface
    val edge = colors.borderStrong.copy(alpha = 0.35f)
    val error = colors.error
    val handPainter = rememberHandPainter()
    Canvas(Modifier.fillMaxSize()) {
        val pad = size.width * 0.07f
        val cell = (size.width - 2 * pad) / SIZE
        val origin = Offset(pad, pad)
        fun center(r: Int, c: Int) = Offset(origin.x + (c + 0.5f) * cell, origin.y + (r + 0.5f) * cell)
        val fade = demoFade(t)
        val unit = cell * 0.95f

        // The board: a quiet tile for each cell, as on the real board.
        val gap = cell * 0.04f
        val corner = CornerRadius(cell * 0.18f)
        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            val topLeft = Offset(origin.x + c * cell + gap, origin.y + r * cell + gap)
            val tileSize = Size(cell - 2 * gap, cell - 2 * gap)
            drawRoundRect(tile.copy(alpha = fade), topLeft, tileSize, corner)
            drawRoundRect(edge.copy(alpha = edge.alpha * fade), topLeft, tileSize, corner, style = Stroke(width = 1.dp.toPx()))
        }

        // What each arrow is doing now: how far it has slid out, how far it bumped, whether it is red.
        val slide = FloatArray(Board.size)
        val bump = FloatArray(Board.size)
        val flash = BooleanArray(Board.size)
        // The hand: where it points, how hard it presses (0 not at all, 0 to 1 while it taps).
        var handFrom = center(3, 3) + Offset(cell * 1.4f, cell * 1.4f)
        var handTo = handFrom
        var handMove = 1f
        var press = 0f

        fun total(i: Int): Float {
            val arrow = arrowState(Board[i])
            return polylineLength(snakePoints(arrow, SIZE, cell).body) + (cellsToEdge(arrow, SIZE) + 1f) * cell
        }
        fun head(i: Int) = Board[i].first.last().let { center(it.first, it.second) }

        when (step) {
            0 -> {
                handTo = head(0)
                handMove = demoEase(demoSegment(t, 0.02f, 0.3f))
                press = demoSegment(t, 0.3f, 0.45f)
                slide[0] = total(0) * demoEase(demoSegment(t, 0.4f, 0.78f))
            }
            1 -> {
                handTo = head(1)
                handMove = demoEase(demoSegment(t, 0.02f, 0.3f))
                press = demoSegment(t, 0.3f, 0.45f)
                val out = demoEase(demoSegment(t, 0.38f, 0.5f))
                val back = demoEase(demoSegment(t, 0.5f, 0.64f))
                bump[1] = cell * 0.45f * (out - back)
                flash[2] = t in 0.4f..0.85f
            }
            else -> {
                // Four taps one after the other: C first (nothing is in front of it), then B, D and A.
                val order = listOf(2, 1, 3, 0)
                var from = handFrom
                order.forEachIndexed { slot, arrow ->
                    val start = 0.02f + slot * 0.24f
                    val target = head(arrow)
                    if (t >= start) {
                        handFrom = from
                        handTo = target
                        handMove = demoEase(demoSegment(t, start, start + 0.08f))
                        press = demoSegment(t, start + 0.08f, start + 0.14f)
                    }
                    slide[arrow] = total(arrow) * demoEase(demoSegment(t, start + 0.11f, start + 0.22f))
                    from = target
                }
            }
        }

        Board.forEachIndexed { i, shape ->
            val state = arrowState(shape)
            val points = snakePoints(state, SIZE, cell)
            val body = polylineLength(points.body)
            val color = if (flash[i]) error else palette[i % palette.size]
            // The arrows are drawn in the picture's own frame: the whole picture is the board, so nothing clips them but the card.
            clipRect(origin.x, origin.y, origin.x + SIZE * cell, origin.y + SIZE * cell) {
                translate(origin.x, origin.y) {
                    drawSnake(points, body, slide[i] + bump[i], state.dir, color.copy(alpha = color.alpha * fade), cell)
                }
            }
        }
        val hand = Offset(handFrom.x + (handTo.x - handFrom.x) * handMove, handFrom.y + (handTo.y - handFrom.y) * handMove)
        drawHandCursor(handPainter, hand, unit, press, alpha = fade)
    }
}
