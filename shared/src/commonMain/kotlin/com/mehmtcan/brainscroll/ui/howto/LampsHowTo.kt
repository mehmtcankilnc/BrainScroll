package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_lamps_start
import brainscroll.shared.generated.resources.howto_lamps_tech
import brainscroll.shared.generated.resources.howto_card_lamps_1
import brainscroll.shared.generated.resources.howto_card_lamps_2
import brainscroll.shared.generated.resources.howto_card_lamps_3
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import com.mehmtcan.brainscroll.ui.theme.GuideColor
import com.mehmtcan.brainscroll.ui.theme.world

/**
 * The three steps of the lamps card: a lamp lights its row and column up to the walls, two lamps that see each other turn red, a number on
 * a wall counts the lamps next to it (the board is solved with every cell lit).
 */
internal val LampsHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_lamps_1, { t -> LampsDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_lamps_2, { t -> LampsDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_lamps_3, { t -> LampsDemo(step = 2, t = t) }),
        HowToStep(Res.string.howto_lamps_tech, { t -> LampsDemo(step = 3, t = t) }, title = Res.string.howto_title_solve),
        HowToStep(Res.string.howto_lamps_start, { t -> LampsDemo(step = 4, t = t) }, cycleMillis = 4800, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

// A 4 by 4 board: the walls are the cells 0, 5 and 15 (the last one with a 2). The solution has lamps on 1, 4, 11 and 14.
private val DemoWalls = setOf(0, 5, 15)

/** The cells a lamp on the cell lights, its own cell first. */
private val DemoLight = mapOf(
    1 to listOf(1, 2, 3),
    4 to listOf(4, 8, 12),
    11 to listOf(11, 10, 9, 8, 7, 3),
    14 to listOf(14, 13, 12, 10, 6, 2),
    13 to listOf(13, 12, 14, 9),
)

@Composable
internal fun LampsDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.Lamps.world
    val fade = demoFade(t)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Step 0: one lamp, its light spreads along its row and column. Step 1: a second lamp in the same row, both turn red.
    // Step 2: the whole board is lit with four lamps, and the wall with a 2 has two lamps next to it.
    val grow = demoEase(demoSegment(t, 0.2f, 0.55f))
    val lamps: List<Pair<Int, Float>> = when (step) {
        0 -> listOf(14 to grow)
        1 -> listOf(14 to 1f, 13 to grow)
        // Step 3: a lamp whose light is stopped by the walls beside it. Step 4: the wall with a 2 gets its two lamps.
        3 -> listOf(1 to grow)
        4 -> listOf(11 to demoEase(demoSegment(t, 0.25f, 0.45f)), 14 to demoEase(demoSegment(t, 0.5f, 0.7f)))
        else -> listOf(14 to 1f, 1 to demoEase(demoSegment(t, 0.1f, 0.3f)), 4 to demoEase(demoSegment(t, 0.25f, 0.45f)), 11 to demoEase(demoSegment(t, 0.4f, 0.6f)))
    }
    val clash = step == 1 && grow > 0.5f
    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            val cell = size.width / 4f
            val inset = 1.5.dp.toPx()
            val corner = CornerRadius(5.dp.toPx())
            val numberSize = with(density) { (cell * 0.5f).toSp() }
            // How much each cell is lit: the strongest of the lamps that light it.
            val light = FloatArray(16)
            for ((lamp, amount) in lamps) for (lit in DemoLight.getValue(lamp)) light[lit] = maxOf(light[lit], amount)
            for (index in 0 until 16) {
                val topLeft = Offset((index % 4) * cell + inset / 2, (index / 4) * cell + inset / 2)
                val box = Size(cell - inset, cell - inset)
                if (index in DemoWalls) {
                    drawRoundRect(colors.borderStrong, topLeft, box, corner)
                    if (index == 15) {
                        val allLit = (step == 2 || step == 4) && lamps.all { it.second >= 1f }
                        val layout = measurer.measure("2", TextStyle(fontSize = numberSize, fontWeight = FontWeight.Bold, color = if (allLit) colors.correctFill else colors.textPrimary))
                        drawText(layout, topLeft = Offset((index % 4) * cell + (cell - layout.size.width) / 2f, (index / 4) * cell + (cell - layout.size.height) / 2f))
                    }
                    continue
                }
                drawRoundRect(colors.bgSurface, topLeft, box, corner)
                if (light[index] > 0f) drawRoundRect(world.copy(alpha = 0.3f * light[index]), topLeft, box, corner)
            }
            // The walls the step is about, ringed like the hint does it: the wall that stops the light, the wall with the number.
            if (step >= 3) {
                val wall = if (step == 3) 5 else 15
                drawRoundRect(
                    GuideColor.copy(alpha = 0.95f), Offset((wall % 4) * cell + inset / 2, (wall / 4) * cell + inset / 2), Size(cell - inset, cell - inset), corner,
                    style = Stroke(2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 4.dp.toPx()))),
                )
            }
            for ((lamp, amount) in lamps) {
                if (amount <= 0f) continue
                val center = Offset((lamp % 4) * cell + cell / 2f, (lamp / 4) * cell + cell / 2f)
                val bulb = if (clash && (lamp == 14 || lamp == 13)) colors.error else world
                drawCircle(bulb.copy(alpha = 0.35f * amount), radius = cell * 0.46f, center = center)
                drawCircle(bulb.copy(alpha = amount), radius = cell * 0.28f * (0.6f + 0.4f * amount), center = center)
            }
        }
    }
}
