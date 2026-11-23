package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_pipes_start
import brainscroll.shared.generated.resources.howto_pipes_tech
import brainscroll.shared.generated.resources.howto_card_pipes_1
import brainscroll.shared.generated.resources.howto_card_pipes_2
import brainscroll.shared.generated.resources.howto_card_pipes_3
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator
import com.mehmtcan.brainscroll.ui.pipes.drawPipes
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the pipes card: a tap turns a tile, open ends have a ring, and all the pipes joined. */
internal val PipesHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_pipes_1, { t -> PipesDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_pipes_2, { t -> PipesDemo(step = 1, t = t) }, cycleMillis = 4800),
        HowToStep(Res.string.howto_card_pipes_3, { t -> PipesDemo(step = 2, t = t) }, cycleMillis = 6400),
        sceneStep(Res.string.howto_pipes_tech, PipesEdgeScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_pipes_start, PipesStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private const val SIZE = 3
private const val N = PipesGenerator.NORTH
private const val E = PipesGenerator.EAST
private const val S = PipesGenerator.SOUTH
private const val W = PipesGenerator.WEST

// The solved network of nine tiles (a tree, nothing closed in a ring): row by row.
private val Solved = listOf(
    E or S, W or E or S, W or S,
    N or S, N, N or S,
    N or E, W, N,
)

private const val TURN = 0.08f

/** A tap of the hand on a tile at time [at]. */
private class DemoTurn(val row: Int, val column: Int, val at: Float)

private fun turned(mask: Int, times: Int): Int {
    var m = mask
    repeat(times) { m = PipesGenerator.rotate(m) }
    return m
}

@Composable
internal fun PipesDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    // The tiles that start turned away from their place (each is the solved tile turned a number of quarters) and the taps that put them right.
    val away = IntArray(SIZE * SIZE)
    val taps: List<DemoTurn>
    when (step) {
        0 -> {
            away[4] = 3 // the middle tile points west, not north: one tap turns it right
            taps = listOf(DemoTurn(1, 1, 0.25f))
        }
        1 -> {
            away[5] = 1 // the tile at the right of the middle row lies across
            away[6] = 2 // the bottom left tile needs two turns
            taps = listOf(DemoTurn(1, 2, 0.15f), DemoTurn(2, 0, 0.4f), DemoTurn(2, 0, 0.62f))
        }
        else -> {
            away[1] = 3
            away[3] = 1
            away[0] = 3
            away[6] = 2
            away[5] = 1
            taps = listOf(
                DemoTurn(0, 0, 0.06f), DemoTurn(0, 1, 0.19f), DemoTurn(1, 0, 0.32f),
                DemoTurn(2, 0, 0.45f), DemoTurn(2, 0, 0.58f), DemoTurn(1, 2, 0.71f),
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.1f
        val gap = size.width * 0.02f
        val cell = (size.width - 2 * pad - gap * (SIZE - 1)) / SIZE
        val top = (size.height - (cell * SIZE + gap * (SIZE - 1))) / 2f
        fun origin(r: Int, c: Int) = Offset(pad + c * (cell + gap), top + r * (cell + gap))
        fun center(r: Int, c: Int) = origin(r, c) + Offset(cell / 2f, cell / 2f)

        // The tiles now: the mask a tile has when its last finished turn is done, and how far a turn that is going on has gone.
        val masks = IntArray(SIZE * SIZE) { i -> turned(Solved[i], away[i]) }
        val angles = FloatArray(SIZE * SIZE)
        for (tap in taps) {
            val i = tap.row * SIZE + tap.column
            when {
                t >= tap.at + TURN -> masks[i] = PipesGenerator.rotate(masks[i])
                t >= tap.at -> angles[i] = 90f * demoEase(demoSegment(t, tap.at, tap.at + TURN))
            }
        }
        // A side is joined when the neighbor has the opposite side open (a tile that is turning counts as it was).
        fun joined(r: Int, c: Int): Int {
            val m = masks[r * SIZE + c]
            var out = 0
            if (m and N != 0 && r > 0 && masks[(r - 1) * SIZE + c] and S != 0) out = out or N
            if (m and S != 0 && r < SIZE - 1 && masks[(r + 1) * SIZE + c] and N != 0) out = out or S
            if (m and W != 0 && c > 0 && masks[r * SIZE + c - 1] and E != 0) out = out or W
            if (m and E != 0 && c < SIZE - 1 && masks[r * SIZE + c + 1] and W != 0) out = out or E
            return out
        }

        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            val o = origin(r, c)
            val corner = CornerRadius(cell * 0.14f)
            drawRoundRect(colors.bgSurface.copy(alpha = fade), o, Size(cell, cell), corner)
            drawRoundRect(colors.borderStrong.copy(alpha = 0.3f * fade), o, Size(cell, cell), corner, style = Stroke(width = 1.dp.toPx()))
            val i = r * SIZE + c
            val matched = joined(r, c)
            rotate(angles[i], pivot = center(r, c)) {
                inset(o.x, o.y, size.width - (o.x + cell), size.height - (o.y + cell)) {
                    drawPipes(masks[i], matched, colors.correctFill.copy(alpha = fade), colors.borderStrong.copy(alpha = fade), colors.textSecondary.copy(alpha = fade))
                }
            }
        }

        val (hand, press) = demoHand(t, taps.map { DemoTap(it.at, center(it.row, it.column)) }, Offset(size.width, size.height))
        drawHandCursor(handPainter, hand, cell * 0.95f, press, alpha = fade)
    }
}
