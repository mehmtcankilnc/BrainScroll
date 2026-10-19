package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Text centered on [center], [fontPx] tall in pixels of the picture (it does not follow the text size setting: it has to fit). */
fun DrawScope.drawCenteredText(measurer: TextMeasurer, text: String, center: Offset, fontPx: Float, color: Color, bold: Boolean = true) {
    val layout = measurer.measure(text, TextStyle(color = color, fontSize = (fontPx / (density * fontScale)).sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal))
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}
