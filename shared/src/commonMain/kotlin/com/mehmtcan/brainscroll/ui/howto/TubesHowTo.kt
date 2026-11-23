package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_tubes_start
import brainscroll.shared.generated.resources.howto_tubes_tech
import brainscroll.shared.generated.resources.howto_card_tubes_1
import brainscroll.shared.generated.resources.howto_card_tubes_2
import brainscroll.shared.generated.resources.howto_card_tubes_3
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.ui.memory.drawSymbol
import com.mehmtcan.brainscroll.ui.memory.symbolColor
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the tubes card: lift a tube and pour into an empty one, pour onto the same color, sort every tube. */
internal val TubesHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_tubes_1, { t -> TubesDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_tubes_2, { t -> TubesDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_tubes_3, { t -> TubesDemo(step = 2, t = t) }, cycleMillis = 6400),
        HowToStep(Res.string.howto_tubes_tech, { t -> TubesDemo(step = 3, t = t) }, cycleMillis = 4800, title = Res.string.howto_title_solve),
        HowToStep(Res.string.howto_tubes_start, { t -> TubesDemo(step = 4, t = t) }, cycleMillis = 6400, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private const val CAPACITY = 4
private const val POUR = 0.12f

/** One pour of a picture: the tube it comes from and goes to, when each is tapped, and how many units move. */
private class DemoPour(val from: Int, val to: Int, val pickAt: Float, val dropAt: Float, val units: Int)

@Composable
internal fun TubesDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    // Tubes list their colors from the bottom up (a color is a number, and has its own shape too).
    val start: List<List<Int>>
    val pours: List<DemoPour>
    when (step) {
        0 -> {
            start = listOf(listOf(0, 0, 1, 1), listOf(0, 0, 1, 1), emptyList())
            pours = listOf(DemoPour(0, 2, 0.15f, 0.4f, 2))
        }
        1 -> {
            start = listOf(listOf(0, 0), listOf(0, 0, 1, 1), listOf(1, 1))
            pours = listOf(DemoPour(1, 2, 0.15f, 0.4f, 2))
        }
        2 -> {
            start = listOf(listOf(0, 0, 1, 1), listOf(0, 0, 1, 1), emptyList())
            pours = listOf(DemoPour(0, 2, 0.05f, 0.14f, 2), DemoPour(1, 2, 0.32f, 0.41f, 2), DemoPour(1, 0, 0.59f, 0.68f, 2))
        }
        // A finished tube is left alone: the two half tubes of one colour are put together, so one tube fills and one empties.
        3 -> {
            start = listOf(listOf(0, 0, 0, 0), listOf(1, 1), listOf(1, 1))
            pours = listOf(DemoPour(1, 2, 0.2f, 0.45f, 2))
        }
        // The first move: the top colour goes into the empty tube, which frees the colour under it for its own colour.
        else -> {
            start = listOf(listOf(0, 1, 0), listOf(1, 1), emptyList())
            pours = listOf(DemoPour(0, 2, 0.08f, 0.18f, 1), DemoPour(0, 1, 0.5f, 0.6f, 1))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.1f
        val gap = size.width * 0.07f
        val tubeW = minOf(size.width * 0.22f, (size.width - 2 * pad - 2 * gap) / 3f)
        val tubeH = size.height * 0.64f
        val rowLeft = (size.width - (tubeW * 3 + gap * 2)) / 2f
        val tubeTop = (size.height - tubeH) / 2f + size.height * 0.04f
        fun left(i: Int) = rowLeft + i * (tubeW + gap)
        val lift = size.width * 0.045f
        val padInside = size.width * 0.01f

        // The tubes now: the pours that are done have moved their colors; the one going on moves them a little at a time.
        val tubes = start.map { it.toMutableList() }
        var active: DemoPour? = null
        var progress = 0f
        for (p in pours) {
            when {
                t >= p.dropAt + POUR -> repeat(p.units) { tubes[p.to].add(tubes[p.from].removeAt(tubes[p.from].lastIndex)) }
                t >= p.pickAt -> {
                    active = p
                    progress = demoEase(demoSegment(t, p.dropAt, p.dropAt + POUR))
                }
            }
        }
        val moving = active
        val lifted = if (moving != null && t < moving.dropAt + POUR) moving.from else -1

        for (i in 0 until 3) {
            val rect = Rect(left(i), tubeTop - (if (i == lifted) lift else 0f), left(i) + tubeW, tubeTop + tubeH - (if (i == lifted) lift else 0f))
            val radius = tubeW / 2f
            val outer = Path().apply { addRoundRect(RoundRect(rect, topLeft = CornerRadius(6f), topRight = CornerRadius(6f), bottomLeft = CornerRadius(radius), bottomRight = CornerRadius(radius))) }
            drawPath(outer, colors.bgSurface.copy(alpha = fade))
            // The liquid, from the bottom: whole units, and a part of the next while a pour is going on.
            val slot = (rect.height - padInside * 2) / CAPACITY
            val contents = tubes[i]
            // Units drawn per layer: the one that is poured out shrinks and the one poured in grows.
            val layers = ArrayList<Pair<Int, Float>>()
            contents.forEachIndexed { level, color -> layers += color to 1f }
            if (moving != null && i == moving.from) {
                val n = layers.size
                for (k in 0 until moving.units) layers[n - 1 - k] = layers[n - 1 - k].first to (1f - progress)
            }
            if (moving != null && i == moving.to) {
                val color = tubes[moving.from].last()
                repeat(moving.units) { layers += color to progress }
            }
            val inner = Path().apply { addRoundRect(RoundRect(Rect(rect.left + padInside, rect.top + padInside, rect.right - padInside, rect.bottom - padInside), topLeft = CornerRadius(3f), topRight = CornerRadius(3f), bottomLeft = CornerRadius(radius - padInside), bottomRight = CornerRadius(radius - padInside))) }
            clipPath(inner) {
                var y = rect.bottom - padInside
                for ((color, units) in layers) {
                    if (units <= 0.01f) continue
                    val symbol = CardSymbol.entries[color % CardSymbol.entries.size]
                    val height = slot * units
                    drawRect(symbolColor(symbol, colors).copy(alpha = fade), Offset(rect.left + padInside, y - height), Size(tubeW - padInside * 2, height))
                    if (units >= 0.6f) {
                        val side = minOf(slot, tubeW) * 0.5f
                        val cx = rect.left + tubeW / 2f
                        val cy = y - height / 2f
                        inset(cx - side / 2f, cy - side / 2f, size.width - (cx + side / 2f), size.height - (cy + side / 2f)) {
                            drawSymbol(symbol, colors.correctOn.copy(alpha = 0.55f * fade))
                        }
                    }
                    y -= height
                }
            }
            val sorted = contents.size == CAPACITY && contents.all { it == contents.first() } && i != moving?.from && i != moving?.to
            val selected = i == lifted
            val edge = when {
                selected -> colors.pendingFill
                sorted -> colors.correctFill
                else -> colors.borderStrong
            }
            drawPath(outer, edge.copy(alpha = fade), style = Stroke(width = (if (selected) 2.5.dp else 1.5.dp).toPx()))
        }

        val stops = pours.flatMap { p ->
            listOf(
                DemoTap(p.pickAt, Offset(left(p.from) + tubeW / 2f, tubeTop + tubeH * 0.5f)),
                DemoTap(p.dropAt, Offset(left(p.to) + tubeW / 2f, tubeTop + tubeH * 0.5f)),
            )
        }
        val (hand, press) = demoHand(t, stops, Offset(size.width, size.height))
        drawHandCursor(handPainter, hand, size.width * 0.2f, press, alpha = fade)
    }
}
