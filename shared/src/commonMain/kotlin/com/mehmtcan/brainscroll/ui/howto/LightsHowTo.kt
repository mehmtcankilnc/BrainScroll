package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_lights_start
import brainscroll.shared.generated.resources.howto_lights_tech
import brainscroll.shared.generated.resources.howto_card_lights_1
import brainscroll.shared.generated.resources.howto_card_lights_2
import brainscroll.shared.generated.resources.howto_card_lights_3
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the lights card: a tap switches a light and its neighbors, on goes out and off comes on, put them all out. */
internal val LightsHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_lights_1, { t -> LightsDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_lights_2, { t -> LightsDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_lights_3, { t -> LightsDemo(step = 2, t = t) }, cycleMillis = 4800),
        sceneStep(Res.string.howto_lights_tech, LightsFlipScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_lights_start, LightsStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private const val SIZE = 3

/** A tap of the hand on a light at time [at]. */
private class DemoLightTap(val row: Int, val column: Int, val at: Float)

@Composable
internal fun LightsDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    val start: Set<Pair<Int, Int>>
    val taps: List<DemoLightTap>
    when (step) {
        0 -> {
            start = emptySet()
            taps = listOf(DemoLightTap(1, 1, 0.3f))
        }
        1 -> {
            start = setOf(0 to 0, 0 to 1)
            taps = listOf(DemoLightTap(0, 1, 0.3f))
        }
        else -> {
            start = setOf(0 to 0, 0 to 1, 1 to 0, 2 to 2, 2 to 1, 1 to 2)
            taps = listOf(DemoLightTap(0, 0, 0.15f), DemoLightTap(2, 2, 0.5f))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.12f
        val gap = size.width * 0.025f
        val cell = (size.width - 2 * pad - gap * (SIZE - 1)) / SIZE
        val top = (size.height - (cell * SIZE + gap * (SIZE - 1))) / 2f
        fun origin(r: Int, c: Int) = Offset(pad + c * (cell + gap), top + r * (cell + gap))
        fun center(r: Int, c: Int) = origin(r, c) + Offset(cell / 2f, cell / 2f)
        val corner = CornerRadius(cell * 0.16f)

        // How lit each light is (0 off, 1 on): a tap starts it moving to the other state, fast.
        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            var lit = if (r to c in start) 1f else 0f
            for (tap in taps) {
                val affected = (r == tap.row && c == tap.column) || (kotlin.math.abs(r - tap.row) + kotlin.math.abs(c - tap.column) == 1)
                if (affected && t >= tap.at) {
                    val p = demoEase(demoSegment(t, tap.at, tap.at + 0.06f))
                    // The state before this tap flips to the other one.
                    lit = if (lit == 1f) 1f - p else p
                }
            }
            val o = origin(r, c)
            val fill = lerp(colors.bgSurface, colors.pendingFill, lit)
            val edge = lerp(colors.borderStrong.copy(alpha = 0.5f), colors.pendingFill, lit)
            drawRoundRect(fill.copy(alpha = fade), o, Size(cell, cell), corner)
            drawRoundRect(edge.copy(alpha = edge.alpha * fade), o, Size(cell, cell), corner, style = Stroke(width = 1.5.dp.toPx()))
            val ct = center(r, c)
            // The bulb: a bright disc when on, a dim ring when off.
            if (lit > 0.5f) drawCircle(colors.pendingOn.copy(alpha = fade), cell * 0.21f, ct)
            else drawCircle(colors.borderStrong.copy(alpha = 0.4f * fade), cell * 0.15f, ct, style = Stroke(width = 2.dp.toPx()))
        }

        val (hand, press) = demoHand(t, taps.map { DemoTap(it.at, center(it.row, it.column)) }, Offset(size.width, size.height))
        drawHandCursor(handPainter, hand, cell * 0.95f, press, alpha = fade)
    }
}
