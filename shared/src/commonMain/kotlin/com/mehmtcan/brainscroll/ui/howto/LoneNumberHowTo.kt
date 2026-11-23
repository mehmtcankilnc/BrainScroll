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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_lonenumber_start
import brainscroll.shared.generated.resources.howto_lonenumber_tech
import brainscroll.shared.generated.resources.howto_card_lonenumber_1
import brainscroll.shared.generated.resources.howto_card_lonenumber_2
import brainscroll.shared.generated.resources.howto_card_lonenumber_3
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world

/** The three steps of the lone number card: a board with numbers that repeat, the repeated ones shaded, a circled number that stays. */
internal val LoneNumberHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_lonenumber_1, { t -> LoneNumberDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_lonenumber_2, { t -> LoneNumberDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_lonenumber_3, { t -> LoneNumberDemo(step = 2, t = t) }),
        sceneStep(Res.string.howto_lonenumber_tech, LoneNeighborsScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_lonenumber_start, LoneStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

// A solved 4 by 4 board: the cells 3, 11 and 12 are shaded, the cell 5 is circled.
private val DemoNumbers = listOf(
    2, 1, 3, 3,
    3, 2, 1, 4,
    1, 4, 2, 1,
    4, 3, 4, 2,
)
private val DemoShaded = listOf(3, 11, 12)
private const val DemoCircled = 5

@Composable
internal fun LoneNumberDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.LoneNumber.world
    val fade = demoFade(t)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Step 0: the two 3s of the top row repeat and are red. Step 1: one of them is shaded and the red goes away. Step 2: the whole board
    // is solved: all three shaded cells and the circle, the rules at work (shaded cells do not touch, the rest is one piece).
    val filled = when (step) {
        0 -> 0f
        1 -> demoEase(demoSegment(t, 0.2f, 0.6f)).coerceAtMost(1f)
        else -> 3f
    }
    val circle = if (step == 2) demoEase(demoSegment(t, 0.2f, 0.5f)) else 0f
    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            val cell = size.width / 4f
            val inset = 1.5.dp.toPx()
            val corner = CornerRadius(5.dp.toPx())
            val numberSize = with(density) { (cell * 0.46f).toSp() }
            for (index in 0 until 16) {
                val topLeft = Offset((index % 4) * cell + inset / 2, (index / 4) * cell + inset / 2)
                val box = Size(cell - inset, cell - inset)
                val shadeIndex = DemoShaded.indexOf(index)
                val shade = if (shadeIndex >= 0) (filled - shadeIndex).coerceIn(0f, 1f) else 0f
                drawRoundRect(colors.bgSurface, topLeft, box, corner)
                // The row that has the repeat is tinted while the card explains it.
                if (step < 2 && index / 4 == 0) drawRoundRect(world.copy(alpha = 0.16f), topLeft, box, corner)
                if (shade > 0f) drawRoundRect(world.copy(alpha = shade), topLeft, box, corner)
                // The two 3s of the top row are red until one of them is shaded.
                val repeated = (step == 0 && index in listOf(2, 3)) || (step == 1 && index == 2 && filled < 0.5f)
                val ink = when {
                    shade > 0.5f -> colors.bgPage.copy(alpha = 0.7f)
                    repeated -> colors.error
                    else -> colors.textPrimary
                }
                val layout = measurer.measure(DemoNumbers[index].toString(), TextStyle(fontSize = numberSize, fontWeight = FontWeight.Bold, color = ink))
                drawText(layout, topLeft = Offset((index % 4) * cell + (cell - layout.size.width) / 2f, (index / 4) * cell + (cell - layout.size.height) / 2f))
                if (index == DemoCircled && circle > 0f) {
                    drawCircle(
                        world.copy(alpha = circle),
                        radius = cell * 0.38f,
                        center = Offset((index % 4) * cell + cell / 2f, (index / 4) * cell + cell / 2f),
                        style = Stroke(2.5.dp.toPx()),
                    )
                }
            }
        }
    }
}
