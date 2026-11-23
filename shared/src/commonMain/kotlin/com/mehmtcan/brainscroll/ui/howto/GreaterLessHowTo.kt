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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import brainscroll.shared.generated.resources.howto_greaterless_start
import brainscroll.shared.generated.resources.howto_greaterless_tech
import brainscroll.shared.generated.resources.howto_card_greaterless_1
import brainscroll.shared.generated.resources.howto_card_greaterless_2
import brainscroll.shared.generated.resources.howto_card_greaterless_3
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world

/**
 * The three steps of the greater and less card: a row that has two numbers leaves the third for its last cell, a sign that puts the
 * bigger number on one side of a given number, and a tap that goes through the numbers one after the other.
 */
internal val GreaterLessHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_greaterless_1, { t -> GreaterLessDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_greaterless_2, { t -> GreaterLessDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_greaterless_3, { t -> GreaterLessDemo(step = 2, t = t) }),
        sceneStep(Res.string.howto_greaterless_tech, GlSignScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_greaterless_start, GlStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

// A 3 by 3 board, 0 where the player has still to put a number. The finished board is 2 1 3 / 3 2 1 / 1 3 2.
private val DemoGiven = listOf(
    2, 1, 0,
    0, 2, 1,
    1, 3, 0,
)
private val DemoSolution = listOf(2, 1, 3, 3, 2, 1, 1, 3, 2)

private const val Gap = 0.5f

/** A chevron whose tip is on [toward] (-1 left or up, 1 right or down) of the middle of [at]. */
private fun DrawScope.chevron(at: Offset, side: Float, horizontal: Boolean, toward: Float, color: Color, alpha: Float) {
    val a = side * 0.42f
    val back = a * 0.6f
    val path = Path().apply {
        if (horizontal) {
            moveTo(at.x - toward * back, at.y - a)
            lineTo(at.x + toward * back, at.y)
            lineTo(at.x - toward * back, at.y + a)
        } else {
            moveTo(at.x - a, at.y - toward * back)
            lineTo(at.x, at.y + toward * back)
            lineTo(at.x + a, at.y - toward * back)
        }
    }
    drawPath(path, color.copy(alpha = alpha), style = Stroke(width = side * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
internal fun GreaterLessDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.GreaterLess.world
    val fade = demoFade(t)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Step 0: the first row has 2 and 1, so the last cell of it is 3. Step 1: the sign says the left cell of the middle row is bigger than
    // the 2 beside it, so it is 3. Step 2: the board shrinks to make room for the number keys; the last cell is selected, then the key 2 is tapped
    // and the 2 appears in it.
    val grow = demoEase(demoSegment(t, 0.25f, 0.55f))
    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            // In step 2 the board is smaller and the keys sit under it, as on the real page.
            val boardWidth = if (step == 2) size.width * 0.68f else size.width
            val left = (size.width - boardWidth) / 2f
            val cell = boardWidth / (3f + 2f * Gap)
            val gap = cell * Gap
            val inset = 1.5.dp.toPx()
            val corner = CornerRadius(5.dp.toPx())
            val digitSize = with(density) { (cell * 0.5f).toSp() }
            fun origin(index: Int) = Offset(left + (index % 3) * (cell + gap), (index / 3) * (cell + gap))
            for (index in 0 until 9) {
                val given = DemoGiven[index]
                val o = origin(index)
                val topLeft = Offset(o.x + inset / 2, o.y + inset / 2)
                val box = Size(cell - inset, cell - inset)
                val target = step == 0 && index == 2 || step == 1 && index == 3
                val cycle = step == 2 && index == 8
                // What the cell shows: its given number, the one the step finds, or in step 2 the 2 the key puts there.
                val shown = when {
                    given > 0 -> given
                    target -> DemoSolution[index]
                    cycle -> if (t < 0.5f) 0 else 2
                    // What the earlier steps found stays on the board.
                    step >= 1 && index == 2 || step == 2 -> DemoSolution[index]
                    else -> 0
                }
                val reason = step == 0 && (index == 0 || index == 1) || step == 1 && (index == 4 || index == 3)
                val appearing = target || cycle && t >= 0.1f
                drawRoundRect(if (given > 0) colors.bgRaised else colors.bgSurface, topLeft, box, corner)
                if (reason) drawRoundRect(world.copy(alpha = 0.3f * grow), topLeft, box, corner)
                if (appearing) drawRoundRect(if (cycle) colors.textPrimary else world.copy(alpha = grow), topLeft, box, corner, style = Stroke(2.5.dp.toPx()))
                if (shown > 0 && (!target || grow > 0.3f)) {
                    val layout = measurer.measure(shown.toString(), TextStyle(fontSize = digitSize, fontWeight = FontWeight.Bold, color = colors.textPrimary))
                    drawText(layout, topLeft = Offset(o.x + (cell - layout.size.width) / 2f, o.y + (cell - layout.size.height) / 2f))
                }
            }
            // The signs: in step 1 only the one that matters (the left cell of the middle row is the bigger), later the board has two more.
            if (step >= 1) {
                val middle = Offset(origin(3).x + cell + gap / 2f, origin(3).y + cell / 2f)
                chevron(middle, gap, horizontal = true, toward = 1f, color = if (step == 1) world else colors.textSecondary, alpha = if (step == 1) maxOf(0.4f, grow) else 1f)
            }
            if (step == 2) {
                // The number keys under the board: the key 2 lights up when it is tapped.
                val keyGap = 6.dp.toPx()
                val keyWidth = (boardWidth - 2 * keyGap) / 3f
                val keyHeight = size.height * 0.2f
                val keyTop = size.height - keyHeight
                for (key in 1..3) {
                    val x = left + (key - 1) * (keyWidth + keyGap)
                    val pressed = key == 2 && t >= 0.3f && t < 0.5f
                    val ready = t >= 0.1f
                    val keyBox = Size(keyWidth - inset, keyHeight - inset)
                    val keyTopLeft = Offset(x + inset / 2, keyTop + inset / 2)
                    drawRoundRect(if (pressed) world.copy(alpha = 0.5f).compositeOver(colors.bgSurface) else colors.bgSurface, keyTopLeft, keyBox, corner)
                    drawRoundRect((if (ready) world else colors.borderStrong).copy(alpha = if (ready) 1f else 0.5f), keyTopLeft, keyBox, corner, style = Stroke(1.5.dp.toPx()))
                    val layout = measurer.measure(key.toString(), TextStyle(fontSize = with(density) { (keyHeight * 0.45f).toSp() }, fontWeight = FontWeight.Bold, color = colors.textPrimary))
                    drawText(layout, topLeft = Offset(x + (keyWidth - layout.size.width) / 2f, keyTop + (keyHeight - layout.size.height) / 2f))
                }
                val above = Offset(origin(2).x + cell / 2f, origin(2).y + cell + gap / 2f)
                chevron(above, gap, horizontal = false, toward = 1f, color = colors.textSecondary, alpha = 1f)
                val beside = Offset(origin(6).x + cell + gap / 2f, origin(6).y + cell / 2f)
                chevron(beside, gap, horizontal = true, toward = -1f, color = colors.textSecondary, alpha = 1f)
            }
        }
    }
}
