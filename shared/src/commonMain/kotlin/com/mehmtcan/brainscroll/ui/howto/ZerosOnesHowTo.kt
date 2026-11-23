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
import brainscroll.shared.generated.resources.howto_zerosones_start
import brainscroll.shared.generated.resources.howto_zerosones_tech
import brainscroll.shared.generated.resources.howto_card_zerosones_1
import brainscroll.shared.generated.resources.howto_card_zerosones_2
import brainscroll.shared.generated.resources.howto_card_zerosones_3
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world

/**
 * The three steps of the zeros and ones card: a board with some digits given, a pair of the same digit that leaves the other digit for
 * the next cell (no three alike), a line that already has half of a digit and gets the other digit in the rest (as many 0s as 1s).
 */
internal val ZerosOnesHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_zerosones_1, { t -> ZerosOnesDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_zerosones_2, { t -> ZerosOnesDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_zerosones_3, { t -> ZerosOnesDemo(step = 2, t = t) }),
        sceneStep(Res.string.howto_zerosones_tech, BinHalfScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_zerosones_start, BinStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

// A 4 by 4 board, -1 where the player has still to put a digit. The finished board is
// 0 1 1 0 / 1 1 0 0 / 0 0 1 1 / 1 0 0 1.
private val DemoGiven = listOf(
    0, 1, -1, -1,
    1, 1, -1, -1,
    0, 0, -1, -1,
    -1, -1, -1, 1,
)

@Composable
internal fun ZerosOnesDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.ZerosOnes.world
    val fade = demoFade(t)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Step 0: only the given digits. Step 1: the two 1s side by side in the second row are marked and the cell next to them gets a 0.
    // Step 2: the third row has two 0s, half of its cells, so the rest are 1s.
    val grow = when (step) {
        0 -> 0f
        else -> demoEase(demoSegment(t, 0.25f, 0.55f))
    }
    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            val cell = size.width / 4f
            val inset = 1.5.dp.toPx()
            val corner = CornerRadius(5.dp.toPx())
            val digitSize = with(density) { (cell * 0.5f).toSp() }
            for (index in 0 until 16) {
                val row = index / 4
                val column = index % 4
                val topLeft = Offset(column * cell + inset / 2, row * cell + inset / 2)
                val box = Size(cell - inset, cell - inset)
                val given = DemoGiven[index]
                // The digit that appears during the step, and which cells are marked as the reason.
                val shown = when {
                    given >= 0 -> given
                    step == 1 && index == 6 -> 0
                    step == 2 && (index == 10 || index == 11) -> 1
                    else -> -1
                }
                val appearing = given < 0 && shown >= 0
                val markedReason = (step == 1 && (index == 4 || index == 5)) || (step == 2 && (index == 8 || index == 9))
                drawRoundRect(if (given >= 0) colors.bgRaised else colors.bgSurface, topLeft, box, corner)
                if (markedReason) drawRoundRect(world.copy(alpha = 0.3f * grow), topLeft, box, corner)
                if (appearing) drawRoundRect(world.copy(alpha = grow), topLeft, box, corner, style = Stroke(2.5.dp.toPx()))
                if (shown >= 0 && (!appearing || grow > 0.3f)) {
                    val layout = measurer.measure(shown.toString(), TextStyle(fontSize = digitSize, fontWeight = FontWeight.Bold, color = colors.textPrimary))
                    drawText(layout, topLeft = Offset(column * cell + (cell - layout.size.width) / 2f, row * cell + (cell - layout.size.height) / 2f))
                }
            }
        }
    }
}
