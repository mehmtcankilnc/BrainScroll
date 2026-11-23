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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_memory_start
import brainscroll.shared.generated.resources.howto_memory_tech
import brainscroll.shared.generated.resources.howto_card_memory_1
import brainscroll.shared.generated.resources.howto_card_memory_2
import brainscroll.shared.generated.resources.howto_card_memory_3
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.ui.memory.drawSymbol
import com.mehmtcan.brainscroll.ui.memory.symbolColor
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/** The three steps of the memory card: turn a card over, a pair that matches stays open, a pair that does not turns back. */
internal val MemoryHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_memory_1, { t -> MemoryDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_memory_2, { t -> MemoryDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_memory_3, { t -> MemoryDemo(step = 2, t = t) }),
        sceneStep(Res.string.howto_memory_tech, MemoryRememberScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_memory_start, MemoryStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private const val COLUMNS = 4
private const val ROWS = 3

// The cards, row by row.
private val Layout = listOf(
    CardSymbol.Circle, CardSymbol.Heart, CardSymbol.Star, CardSymbol.Triangle,
    CardSymbol.Diamond, CardSymbol.Square, CardSymbol.Heart, CardSymbol.Circle,
    CardSymbol.Star, CardSymbol.Triangle, CardSymbol.Square, CardSymbol.Diamond,
)

private const val FLIP = 0.07f

/** A card turned face up at [up] (and face down again at [down] when that is not negative); matched cards turn green from [match]. */
private class DemoFlip(val card: Int, val up: Float, val down: Float = -1f, val match: Float = -1f)

@Composable
internal fun MemoryDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    val flips: List<DemoFlip>
    val taps: List<Pair<Float, Int>>
    when (step) {
        0 -> {
            flips = listOf(DemoFlip(1, 0.3f))
            taps = listOf(0.3f to 1)
        }
        1 -> {
            // The first card of the pair is already up; the second one is the same shape: both stay open, green.
            flips = listOf(DemoFlip(1, -1f, match = 0.5f), DemoFlip(6, 0.3f, match = 0.5f))
            taps = listOf(0.3f to 6)
        }
        else -> {
            // Two different shapes: they are looked at for a moment and turn back.
            flips = listOf(DemoFlip(0, 0.15f, down = 0.62f), DemoFlip(2, 0.38f, down = 0.62f))
            taps = listOf(0.15f to 0, 0.38f to 2)
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.09f
        val gap = size.width * 0.03f
        val card = minOf((size.width - 2 * pad - gap * (COLUMNS - 1)) / COLUMNS, (size.height - 2 * pad - gap * (ROWS - 1)) / ROWS)
        val left = (size.width - (card * COLUMNS + gap * (COLUMNS - 1))) / 2f
        val top = (size.height - (card * ROWS + gap * (ROWS - 1))) / 2f
        fun origin(i: Int) = Offset(left + (i % COLUMNS) * (card + gap), top + (i / COLUMNS) * (card + gap))
        fun center(i: Int) = origin(i) + Offset(card / 2f, card / 2f)
        val corner = CornerRadius(card * 0.16f)

        for (i in Layout.indices) {
            val flip = flips.firstOrNull { it.card == i }
            // The angle: 0 face down, 180 face up; a turn takes [FLIP].
            val angle = when {
                flip == null -> 0f
                flip.up < 0f -> if (flip.down in 0f..t) 180f * (1f - demoEase(demoSegment(t, flip.down, flip.down + FLIP))) else 180f
                t < flip.up -> 0f
                flip.down in 0f..t -> 180f * (1f - demoEase(demoSegment(t, flip.down, flip.down + FLIP)))
                else -> 180f * demoEase(demoSegment(t, flip.up, flip.up + FLIP))
            }
            val showFront = angle > 90f
            val matched = flip != null && flip.match in 0f..t
            val width = card * abs(cos(angle * PI.toFloat() / 180f))
            val c = center(i)
            val o = Offset(c.x - width / 2f, c.y - card / 2f)
            val pop = if (matched) demoPop(t, flip!!.match, 0.06f).coerceAtLeast(0.85f) else 1f
            scale(pop, pop, pivot = c) {
                val fill = if (showFront && matched) colors.correctFill else colors.bgSurface
                drawRoundRect(fill.copy(alpha = fade), o, Size(width, card), corner)
                if (showFront && !matched) drawRoundRect(colors.pendingFill.copy(alpha = fade), o, Size(width, card), corner, style = Stroke(width = 2.5.dp.toPx()))
                if (!showFront) {
                    drawRoundRect(colors.borderSubtle.copy(alpha = fade), o, Size(width, card), corner, style = Stroke(width = 1.dp.toPx()))
                    // The mark on the back: two soft lobes.
                    val brain = colors.textDisabled.copy(alpha = 0.45f * fade)
                    val r = card / 4f
                    drawCircle(brain, r * 0.52f * abs(cos(angle * PI.toFloat() / 180f)).coerceAtLeast(0.05f), Offset(c.x - r * 0.42f * abs(cos(angle * PI.toFloat() / 180f)), c.y))
                    drawCircle(brain, r * 0.52f * abs(cos(angle * PI.toFloat() / 180f)).coerceAtLeast(0.05f), Offset(c.x + r * 0.42f * abs(cos(angle * PI.toFloat() / 180f)), c.y))
                } else {
                    val symbol = Layout[i]
                    val side = card * 0.6f
                    val horizontal = width * 0.6f
                    inset(c.x - horizontal / 2f, c.y - side / 2f, size.width - (c.x + horizontal / 2f), size.height - (c.y + side / 2f)) {
                        drawSymbol(symbol, (if (matched) colors.correctOn else symbolColor(symbol, colors)).copy(alpha = fade))
                    }
                }
            }
        }

        val (hand, press) = demoHand(t, taps.map { (at, card2) -> DemoTap(at, center(card2)) }, Offset(size.width, size.height))
        drawHandCursor(handPainter, hand, card * 0.95f, press, alpha = fade)
    }
}
