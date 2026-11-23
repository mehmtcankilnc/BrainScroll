package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_fences_start
import brainscroll.shared.generated.resources.howto_fences_tech
import brainscroll.shared.generated.resources.howto_card_fences_1
import brainscroll.shared.generated.resources.howto_card_fences_2
import brainscroll.shared.generated.resources.howto_card_fences_3
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world

/**
 * The three steps of the fences card: the loop is drawn round some squares of a small board (one closed path, back where it started), a
 * number counts the sides of its square that are on the loop, and an edge goes line, cross, empty with taps.
 */
internal val FencesHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_fences_1, { t -> FencesDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_fences_2, { t -> FencesDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_fences_3, { t -> FencesDemo(step = 2, t = t) }),
        sceneStep(Res.string.howto_fences_tech, FencesFullScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_fences_start, FencesStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

// A 3 by 3 board of squares, 4 by 4 dots. The loop goes round the squares 0, 1, 4 and 7 (row after row).
private val Loop = listOf(0 to 0, 2 to 0, 2 to 3, 1 to 3, 1 to 1, 0 to 1, 0 to 0)
private val Clues = mapOf(0 to 3, 1 to 2, 4 to 2, 7 to 3, 3 to 1, 8 to 1)

@Composable
internal fun FencesDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.Fences.world
    val fade = demoFade(t)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Step 0: the loop is drawn piece by piece. Step 1: the whole loop is there and the square with the 3 lights up with its three sides.
    // Step 2: no loop, one edge goes line, cross, empty.
    val draw = demoEase(demoSegment(t, 0.1f, 0.8f))
    val grow = demoEase(demoSegment(t, 0.25f, 0.55f))
    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            val pad = size.width * 0.1f
            val step3 = (size.width - 2 * pad) / 3f
            fun at(x: Int, y: Int) = Offset(pad + x * step3, pad + y * step3)
            if (step == 1) drawRect(world.copy(alpha = 0.3f * grow), at(0, 0), Size(step3, step3))
            // The loop (step 0: as far as it is drawn; step 1: all of it, the sides of the first square thick).
            if (step < 2) {
                val pieces = Loop.size - 1
                val reach = if (step == 0) draw * pieces else pieces.toFloat()
                for (i in 0 until pieces) {
                    val part = (reach - i).coerceIn(0f, 1f)
                    if (part <= 0f) continue
                    val a = at(Loop[i].first, Loop[i].second)
                    val b = at(Loop[i + 1].first, Loop[i + 1].second)
                    val end = Offset(a.x + (b.x - a.x) * part, a.y + (b.y - a.y) * part)
                    // The loop pieces 0, 4 and 5 hold the top, bottom and left sides of the first square.
                    val onFirst = step == 1 && (i == 0 || i == 4 || i == 5)
                    drawLine(world, a, end, size.width * (if (onFirst) 0.06f else 0.04f), StrokeCap.Round)
                }
            } else {
                // One edge, the bottom of the last square, goes line (first third), cross (second), empty (last).
                val a = at(2, 3)
                val b = at(3, 3)
                when {
                    t < 0.2f -> Unit
                    t < 0.45f -> drawLine(world, a, b, size.width * 0.045f, StrokeCap.Round)
                    t < 0.7f -> {
                        val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                        val r = step3 * 0.12f
                        drawLine(colors.textSecondary, Offset(mid.x - r, mid.y - r), Offset(mid.x + r, mid.y + r), size.width * 0.02f, StrokeCap.Round)
                        drawLine(colors.textSecondary, Offset(mid.x - r, mid.y + r), Offset(mid.x + r, mid.y - r), size.width * 0.02f, StrokeCap.Round)
                    }
                    else -> Unit
                }
            }
            for (y in 0..3) for (x in 0..3) drawCircle(colors.textSecondary, size.width * 0.022f, at(x, y))
            if (step != 2) {
                for ((c, clue) in Clues) {
                    val layout = measurer.measure(clue.toString(), TextStyle(fontSize = with(density) { (step3 * 0.42f).toSp() }, fontWeight = FontWeight.Bold, color = colors.textPrimary))
                    drawText(layout, topLeft = Offset(pad + (c % 3) * step3 + (step3 - layout.size.width) / 2f, pad + (c / 3) * step3 + (step3 - layout.size.height) / 2f))
                }
            }
        }
    }
}
