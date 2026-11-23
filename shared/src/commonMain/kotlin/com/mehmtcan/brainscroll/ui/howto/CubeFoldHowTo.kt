package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_cubefold_1
import brainscroll.shared.generated.resources.howto_card_cubefold_2
import brainscroll.shared.generated.resources.howto_card_cubefold_3
import brainscroll.shared.generated.resources.howto_cubefold_mirror
import brainscroll.shared.generated.resources.howto_cubefold_opposite
import brainscroll.shared.generated.resources.howto_cubefold_start
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_title_spoiler
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.cubefold.NetSquare
import com.mehmtcan.brainscroll.ui.cubefold.drawCube
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world

/**
 * The six steps of the cube folding card, drawn with the net and the cubes of the game itself: a net and the cube it folds to, the three
 * faces of a corner, how to tap, two faces that are opposite faces can not be seen together, a cube whose faces turn the wrong way is
 * wrong, and where to start.
 */
internal val CubeFoldHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_cubefold_1, { t -> CubeFoldDemo(step = 0, t = t) }, cycleMillis = 4200),
        HowToStep(Res.string.howto_card_cubefold_2, { t -> CubeFoldDemo(step = 1, t = t) }, cycleMillis = 4200),
        HowToStep(Res.string.howto_card_cubefold_3, { t -> CubeFoldDemo(step = 2, t = t) }, cycleMillis = 4200),
        HowToStep(Res.string.howto_cubefold_opposite, { t -> CubeFoldDemo(step = 3, t = t) }, cycleMillis = 4800, title = Res.string.howto_title_solve),
        HowToStep(Res.string.howto_cubefold_mirror, { t -> CubeFoldDemo(step = 4, t = t) }, cycleMillis = 4800, title = Res.string.howto_title_solve),
        HowToStep(Res.string.howto_cubefold_start, { t -> CubeFoldDemo(step = 5, t = t) }, cycleMillis = 4800, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

/**
 * The net of the pictures: a cross. Squares (row, column, symbol): top (0,1), left (1,0), middle (1,1), right (1,2), bottom (2,1), end (3,1).
 * Folded away from the viewer, with the middle square as the front, the opposite pairs are top and bottom, left and right, front and end.
 */
private val DemoNet = listOf(
    Triple(0, 1, 1), Triple(1, 0, 3), Triple(1, 1, 0), Triple(1, 2, 2), Triple(2, 1, 4), Triple(3, 1, 5),
)

/** The corner seen from the front, top and right: (top, left, right) = (top square, middle square, right square). */
private val Real = listOf(1, 0, 2)

/** The same three faces turned the wrong way round. */
private val Mirrored = listOf(1, 2, 0)

/** The top and the bottom square together: two opposite faces. */
private val WithOpposite = listOf(1, 4, 0)

private val PairColors = listOf(Color(0xFFFFD84F), Color(0xFF5ED38A), Color(0xFF5AA9FF))

@Composable
internal fun CubeFoldDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.CubeFold.world
    val fade = demoFade(t)
    val handPainter = rememberHandPainter()
    val density = LocalDensity.current
    val cell = 38.dp
    val netGap = 4.dp
    val cube = 88.dp
    val cubeGap = 8.dp
    val between = 24.dp

    // The cubes of the step, with how they end up: 1 right, 2 wrong.
    val cubes: List<Pair<List<Int>, Int>> = when (step) {
        0, 1 -> listOf(Real to 1)
        2 -> listOf(Mirrored to 2, Real to 1)
        3, 5 -> listOf(WithOpposite to 2, Real to 1)
        else -> listOf(Mirrored to 2, Real to 1)
    }
    val verdict = demoSegment(t, 0.62f, 0.72f)

    // The squares of the net that are ringed, with the colour of the ring and when it appears.
    class Ring(val square: Int, val color: Color, val from: Float, val to: Float)
    val rings: List<Ring> = when (step) {
        0 -> emptyList()
        1 -> listOf(Ring(0, world, 0.1f, 1f), Ring(2, world, 0.3f, 1f), Ring(3, world, 0.5f, 1f))
        3, 5 -> listOf(
            Ring(0, PairColors[0], 0.1f, 1f), Ring(4, PairColors[0], 0.1f, 1f),
            Ring(2, PairColors[1], 0.3f, 1f), Ring(5, PairColors[1], 0.3f, 1f),
            Ring(1, PairColors[2], 0.5f, 1f), Ring(3, PairColors[2], 0.5f, 1f),
        )
        4 -> listOf(Ring(0, world, 0.1f, 1f), Ring(2, world, 0.1f, 1f), Ring(3, world, 0.1f, 1f))
        else -> emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(between), verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(netGap)) {
                for (row in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(netGap)) {
                        for (column in 0 until 3) {
                            val square = DemoNet.firstOrNull { it.first == row && it.second == column }
                            if (square == null) Spacer(Modifier.size(cell)) else NetSquare(square.third, cell)
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(cubeGap)) {
                for ((view, _) in cubes) {
                    Box(Modifier.size(cube)) {
                        Canvas(Modifier.fillMaxSize()) { drawCube(view, colors, Offset(size.width / 2f, size.height / 2f), size.minDimension * 0.44f) }
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val c = cell.toPx()
            val g = netGap.toPx()
            val b = cube.toPx()
            val cg = cubeGap.toPx()
            val gapBetween = between.toPx()
            val netW = 3 * c + 2 * g
            val netH = 4 * c + 3 * g
            val cubesH = cubes.size * b + (cubes.size - 1) * cg
            val contentW = netW + gapBetween + b
            val contentH = maxOf(netH, cubesH)
            val left = (size.width - contentW) / 2f
            val top = (size.height - contentH) / 2f
            val netTop = top + (contentH - netH) / 2f
            val cubesTop = top + (contentH - cubesH) / 2f
            val cubeLeft = left + netW + gapBetween

            // The rings of the net.
            for (ring in rings) {
                val appear = demoSegment(t, ring.from, ring.from + 0.08f)
                if (appear <= 0f) continue
                val (row, column, _) = DemoNet[ring.square]
                drawRoundRect(
                    ring.color.copy(alpha = appear),
                    Offset(left + column * (c + g) - 2f, netTop + row * (c + g) - 2f),
                    Size(c + 4f, c + 4f),
                    CornerRadius(c * 0.22f),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
            // The verdict on every cube: a green frame for the right one, a red cross over a wrong one (steps 2 to 5).
            if (step >= 2) cubes.forEachIndexed { k, (_, outcome) ->
                val y = cubesTop + k * (b + cg)
                if (outcome == 1) {
                    drawRoundRect(Color(0xFF5ED38A).copy(alpha = verdict), Offset(cubeLeft, y), Size(b, b), CornerRadius(b * 0.12f), style = Stroke(width = 3.dp.toPx()))
                } else {
                    val inset = b * 0.18f
                    val red = Color(0xFFFF6B6B).copy(alpha = verdict)
                    drawLine(red, Offset(cubeLeft + inset, y + inset), Offset(cubeLeft + b - inset, y + b - inset), 4.dp.toPx())
                    drawLine(red, Offset(cubeLeft + b - inset, y + inset), Offset(cubeLeft + inset, y + b - inset), 4.dp.toPx())
                }
            }
            // The hand taps the right cube in step 2.
            if (step == 2) {
                val right = cubes.indexOfFirst { it.second == 1 }
                val target = Offset(cubeLeft + b / 2f, cubesTop + right * (b + cg) + b / 2f)
                val (hand, press) = demoHand(t, listOf(DemoTap(0.45f, target)), Offset(size.width, size.height))
                drawHandCursor(handPainter, hand, b * 0.7f, press, alpha = fade)
            }
        }
    }
}
