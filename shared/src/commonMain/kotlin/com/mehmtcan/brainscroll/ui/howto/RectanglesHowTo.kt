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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_rectangles_1
import brainscroll.shared.generated.resources.howto_card_rectangles_2
import brainscroll.shared.generated.resources.howto_card_rectangles_3
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the rectangles card: a board of numbers, a rectangle drawn around a number with that many cells, the rest covered. */
internal val RectanglesHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_rectangles_1, { t -> RectanglesDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_rectangles_2, { t -> RectanglesDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_rectangles_3, { t -> RectanglesDemo(step = 2, t = t) }),
    ),
)

// A 4 by 4 board cut into five rectangles (4, 2, 4, 4 and 2 cells); each number sits in the top left cell of its rectangle.
private class DemoRect(val x: Int, val y: Int, val w: Int, val h: Int, val number: Int, val color: Color)

private val DemoRects = listOf(
    DemoRect(0, 0, 2, 2, 4, Color(0xFF4CAF50)),
    DemoRect(2, 0, 2, 1, 2, Color(0xFF29B6F6)),
    DemoRect(2, 1, 2, 2, 4, Color(0xFFFFA726)),
    DemoRect(0, 2, 2, 2, 4, Color(0xFFAB47BC)),
    DemoRect(2, 3, 2, 1, 2, Color(0xFFEC407A)),
)

@Composable
internal fun RectanglesDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val fade = demoFade(t)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Step 0: only the numbers. Step 1: the first rectangle grows around its number. Step 2: the others follow one after the other.
    val laid = when (step) {
        0 -> 0f
        1 -> demoEase(demoSegment(t, 0.15f, 0.5f))
        else -> 1f + 4f * demoSegment(t, 0.1f, 0.8f)
    }
    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            val cell = size.width / 4f
            val inset = 1.5.dp.toPx()
            val corner = CornerRadius(5.dp.toPx())
            for (row in 0 until 4) for (col in 0 until 4) {
                drawRoundRect(colors.bgSurface, Offset(col * cell + inset / 2, row * cell + inset / 2), Size(cell - inset, cell - inset), corner)
            }
            for ((i, r) in DemoRects.withIndex()) {
                val grown = (laid - i).coerceIn(0f, 1f)
                if (grown <= 0f) continue
                val topLeft = Offset(r.x * cell + inset, r.y * cell + inset)
                val box = Size((r.w * cell - 2 * inset) * grown, (r.h * cell - 2 * inset) * grown)
                drawRoundRect(r.color.copy(alpha = 0.38f), topLeft, box, corner)
                drawRoundRect(r.color, topLeft, box, corner, style = Stroke(2.5.dp.toPx()))
            }
            val numberSize = with(density) { (cell * 0.42f).toSp() }
            for (r in DemoRects) {
                // The number sits in one cell of its rectangle, the top left one.
                val layout = measurer.measure(r.number.toString(), TextStyle(fontSize = numberSize, fontWeight = FontWeight.Bold, color = colors.textPrimary))
                drawText(layout, topLeft = Offset(r.x * cell + (cell - layout.size.width) / 2f, r.y * cell + (cell - layout.size.height) / 2f))
            }
        }
    }
}
