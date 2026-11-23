package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_colorseq_1
import brainscroll.shared.generated.resources.howto_card_colorseq_2
import brainscroll.shared.generated.resources.howto_card_colorseq_3
import brainscroll.shared.generated.resources.howto_colorseq_start
import brainscroll.shared.generated.resources.howto_colorseq_tech
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_title_spoiler
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.colorseq.SeqButton
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world

/**
 * The five steps of the color sequence card, drawn with the buttons of the game itself: the buttons light up in order, the same buttons are
 * touched in the same order, every round has one more light, the lights are held in threes, and in the first round there is one light.
 */
internal val ColorSeqHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_colorseq_1, { t -> ColorSeqDemo(step = 0, t = t) }, cycleMillis = 4200),
        HowToStep(Res.string.howto_card_colorseq_2, { t -> ColorSeqDemo(step = 1, t = t) }, cycleMillis = 4200),
        HowToStep(Res.string.howto_card_colorseq_3, { t -> ColorSeqDemo(step = 2, t = t) }, cycleMillis = 4600),
        HowToStep(Res.string.howto_colorseq_tech, { t -> ColorSeqDemo(step = 3, t = t) }, cycleMillis = 5200, title = Res.string.howto_title_solve),
        HowToStep(Res.string.howto_colorseq_start, { t -> ColorSeqDemo(step = 4, t = t) }, cycleMillis = 4200, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

/** A button of the picture that is lit from [from] to [to] of the loop. */
private class Light(val button: Int, val from: Float, val to: Float)

private fun lights(buttons: List<Int>, first: Float, every: Float, length: Float, pauseAfter: Int = 0, pause: Float = 0f) =
    buttons.mapIndexed { k, b -> Light(b, first + k * every + (if (pauseAfter in 1..k) pause else 0f), first + k * every + length + (if (pauseAfter in 1..k) pause else 0f)) }

private val Watch = lights(listOf(2, 0, 3), first = 0.1f, every = 0.24f, length = 0.16f)
private val Repeat = listOf(2, 0, 3)
private val RepeatAt = listOf(0.2f, 0.45f, 0.7f)
private val Grow = lights(listOf(2), first = 0.08f, every = 0.2f, length = 0.14f) + lights(listOf(2, 0), first = 0.56f, every = 0.2f, length = 0.14f)
private val Threes = lights(listOf(0, 1, 3, 2, 0, 3), first = 0.06f, every = 0.12f, length = 0.09f, pauseAfter = 3, pause = 0.1f)
private val Single = lights(listOf(1), first = 0.1f, every = 0.2f, length = 0.2f)

@Composable
internal fun ColorSeqDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.ColorSeq.world
    val fade = demoFade(t)
    val handPainter = rememberHandPainter()
    val button = 64.dp
    val gap = 10.dp
    val taps: List<Pair<Int, Float>> = when (step) {
        1 -> Repeat.zip(RepeatAt)
        4 -> listOf(1 to 0.6f)
        else -> emptyList()
    }
    val windows = when (step) {
        0 -> Watch
        2 -> Grow
        3 -> Threes
        4 -> Single
        else -> emptyList()
    }
    val lit = windows.firstOrNull { t >= it.from && t <= it.to }?.button
        ?: taps.firstOrNull { t >= it.second && t <= it.second + 0.13f }?.first
    // Under the board a row of dots counts the lights: one more every round (step 2), in threes (step 3).
    val dots: List<List<Boolean>> = when (step) {
        2 -> listOf(List(if (t < 0.5f) 1 else 2) { true })
        3 -> listOf(Threes.take(3), Threes.drop(3)).map { group -> group.map { t >= it.from } }
        else -> emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                for (row in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (col in 0 until 2) {
                            val index = row * 2 + col
                            SeqButton(index = index, lit = lit == index, ring = null, size = button)
                        }
                    }
                }
            }
            if (dots.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    for (group in dots) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (on in group) Box(Modifier.size(10.dp).background(if (on) world else colors.bgRaised, CircleShape))
                        }
                    }
                }
            }
        }
        if (taps.isNotEmpty()) {
            Canvas(Modifier.fillMaxSize()) {
                val b = button.toPx()
                val g = gap.toPx()
                val unit = b
                val cx = size.width / 2f
                // The board is as high as two buttons and a gap; the dots (when there are any) are not part of a card with a hand.
                val cy = size.height / 2f
                fun center(index: Int) = Offset(cx + ((index % 2) - 0.5f) * (b + g), cy + ((index / 2) - 0.5f) * (b + g))
                val stops = taps.map { DemoTap(it.second, center(it.first)) }
                val (hand, press) = demoHand(t, stops, Offset(size.width, size.height))
                drawHandCursor(handPainter, hand, unit * 0.95f, press, alpha = fade)
            }
        }
    }
}
