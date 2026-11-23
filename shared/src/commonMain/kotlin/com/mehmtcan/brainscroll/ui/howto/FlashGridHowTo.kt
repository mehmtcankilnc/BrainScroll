package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_flashgrid_start
import brainscroll.shared.generated.resources.howto_flashgrid_tech
import brainscroll.shared.generated.resources.howto_card_flashgrid_1
import brainscroll.shared.generated.resources.howto_card_flashgrid_2
import brainscroll.shared.generated.resources.howto_card_flashgrid_3
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world

/** The three steps of the flash grid card: some tiles light up, they go dark and are touched from memory, the next round has one more. */
internal val FlashGridHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_flashgrid_1, { t -> FlashGridDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_flashgrid_2, { t -> FlashGridDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_flashgrid_3, { t -> FlashGridDemo(step = 2, t = t) }),
        sceneStep(Res.string.howto_flashgrid_tech, FlashShapeScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_flashgrid_start, FlashStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private val First = setOf(1, 4, 8)
private val Second = setOf(0, 4, 5, 7)

@Composable
internal fun FlashGridDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val lit = GameKind.FlashGrid.world
    val fade = demoFade(t)
    // Step 0: three tiles light up (and stay lit). Step 1: the tiles are dark and are found one after the other, going green.
    // Step 2: a round of four tiles lights up.
    val found = if (step == 1) First.sorted().filterIndexed { i, _ -> demoSegment(t, 0.3f + 0.2f * i, 0.4f + 0.2f * i) >= 1f }.toSet() else emptySet()
    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (col in 0 until 3) {
                        val index = row * 3 + col
                        val color = when (step) {
                            0 -> if (index in First && demoSegment(t, 0.1f, 0.2f) > 0f) lit else colors.bgRaised
                            1 -> if (index in found) colors.correctFill else colors.bgRaised
                            else -> if (index in Second && demoSegment(t, 0.1f, 0.2f) > 0f) lit else colors.bgRaised
                        }
                        Box(Modifier.size(46.dp).background(color, RoundedCornerShape(10.dp)))
                    }
                }
            }
        }
    }
}
