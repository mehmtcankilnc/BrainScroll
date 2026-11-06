package com.mehmtcan.brainscroll.ui.components

import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The small outline icons of the profile page (and the sign-in card). They are drawn, so they take the color they are given. */
enum class LineIcon { Bars, Heart, Trophy, People, Gear, Cloud, Person, Pencil, Globe, Speaker, Sort, Link, Copy, Check, Message }

/** One [LineIcon] drawn in a square of [size], as thin rounded lines in [color]. The drawing is made on a 24 by 24 grid. */
@Composable
fun LineIcon(icon: LineIcon, color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 24f
        val stroke = Stroke(width = 1.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (icon) {
            LineIcon.Bars -> {
                // Three bars, rising: filled, with soft corners.
                for ((i, top) in listOf(13f, 8f, 4f).withIndex()) {
                    val x = (4f + i * 6f) * u
                    drawRoundRect(color, Offset(x, top * u), Size(4f * u, (20f - top) * u), CornerRadius(1.2f * u))
                }
            }
            LineIcon.Heart -> drawPath(path(u) {
                moveTo(12f, 20f)
                cubicTo(5f, 15f, 3f, 11f, 3f, 8.5f)
                cubicTo(3f, 5.5f, 6f, 4f, 8.2f, 4f)
                cubicTo(10f, 4f, 11.4f, 5f, 12f, 6.4f)
                cubicTo(12.6f, 5f, 14f, 4f, 15.8f, 4f)
                cubicTo(18f, 4f, 21f, 5.5f, 21f, 8.5f)
                cubicTo(21f, 11f, 19f, 15f, 12f, 20f)
                close()
            }, color, style = stroke)
            LineIcon.Trophy -> {
                drawPath(path(u) {
                    moveTo(7f, 4f)
                    lineTo(17f, 4f)
                    cubicTo(17f, 9f, 15.5f, 12f, 12f, 12f)
                    cubicTo(8.5f, 12f, 7f, 9f, 7f, 4f)
                    close()
                }, color, style = stroke)
                drawPath(path(u) { moveTo(7f, 6f); cubicTo(3f, 6f, 3f, 10f, 8f, 10.5f) }, color, style = stroke)
                drawPath(path(u) { moveTo(17f, 6f); cubicTo(21f, 6f, 21f, 10f, 16f, 10.5f) }, color, style = stroke)
                drawLine(color, Offset(12f * u, 12f * u), Offset(12f * u, 20f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(8f * u, 20f * u), Offset(16f * u, 20f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            LineIcon.People -> {
                drawCircle(color, 3f * u, Offset(9f * u, 8f * u), style = stroke)
                drawPath(path(u) { moveTo(3f, 19f); cubicTo(3f, 15f, 6f, 14f, 9f, 14f); cubicTo(12f, 14f, 15f, 15f, 15f, 19f) }, color, style = stroke)
                drawCircle(color, 2.5f * u, Offset(16.5f * u, 9f * u), style = stroke)
                drawPath(path(u) { moveTo(16.5f, 14f); cubicTo(19f, 14f, 21f, 15.5f, 21f, 19f) }, color, style = stroke)
            }
            LineIcon.Gear -> {
                drawCircle(color, 3f * u, Offset(12f * u, 12f * u), style = stroke)
                drawCircle(color, 6.2f * u, Offset(12f * u, 12f * u), style = stroke)
                // Eight teeth around the ring.
                for (i in 0 until 8) {
                    val a = i * PI / 4
                    val from = Offset((12f + 6.2f * cos(a).toFloat()) * u, (12f + 6.2f * sin(a).toFloat()) * u)
                    val to = Offset((12f + 8.6f * cos(a).toFloat()) * u, (12f + 8.6f * sin(a).toFloat()) * u)
                    drawLine(color, from, to, strokeWidth = 2.4f * u, cap = StrokeCap.Round)
                }
            }
            LineIcon.Cloud -> drawPath(path(u) {
                moveTo(7.5f, 18f)
                lineTo(17f, 18f)
                cubicTo(19.8f, 18f, 21.5f, 16.2f, 21.5f, 14f)
                cubicTo(21.5f, 11.8f, 19.7f, 10.2f, 17.6f, 10.2f)
                cubicTo(17f, 7.2f, 14.8f, 5.5f, 12.3f, 5.5f)
                cubicTo(9.6f, 5.5f, 7.8f, 7.5f, 7.5f, 10.2f)
                cubicTo(5f, 10.5f, 3f, 12.2f, 3f, 14.4f)
                cubicTo(3f, 16.4f, 4.8f, 18f, 7.5f, 18f)
                close()
            }, color, style = stroke)
            LineIcon.Person -> {
                drawCircle(color, 4f * u, Offset(12f * u, 8f * u), style = stroke)
                drawPath(path(u) { moveTo(4f, 21f); cubicTo(4f, 16f, 8f, 14f, 12f, 14f); cubicTo(16f, 14f, 20f, 16f, 20f, 21f) }, color, style = stroke)
            }
            LineIcon.Globe -> {
                drawCircle(color, 9f * u, Offset(12f * u, 12f * u), style = stroke)
                drawLine(color, Offset(3f * u, 12f * u), Offset(21f * u, 12f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
                // The meridian: a narrow ellipse through both poles.
                drawPath(path(u) {
                    moveTo(12f, 3f)
                    cubicTo(7.5f, 7f, 7.5f, 17f, 12f, 21f)
                    cubicTo(16.5f, 17f, 16.5f, 7f, 12f, 3f)
                    close()
                }, color, style = stroke)
            }
            LineIcon.Speaker -> {
                drawPath(path(u) {
                    moveTo(4f, 9.5f)
                    lineTo(8f, 9.5f)
                    lineTo(13f, 5f)
                    lineTo(13f, 19f)
                    lineTo(8f, 14.5f)
                    lineTo(4f, 14.5f)
                    close()
                }, color, style = stroke)
                drawPath(path(u) { moveTo(16f, 9f); cubicTo(17.5f, 10.5f, 17.5f, 13.5f, 16f, 15f) }, color, style = stroke)
                drawPath(path(u) { moveTo(18.5f, 6.5f); cubicTo(21.5f, 9.5f, 21.5f, 14.5f, 18.5f, 17.5f) }, color, style = stroke)
            }
            LineIcon.Sort -> {
                drawLine(color, Offset(3f * u, 6f * u), Offset(13f * u, 6f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(3f * u, 12f * u), Offset(10f * u, 12f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(3f * u, 18f * u), Offset(7f * u, 18f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(18f * u, 5f * u), Offset(18f * u, 19f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawPath(path(u) { moveTo(14.5f, 15.5f); lineTo(18f, 19f); lineTo(21.5f, 15.5f) }, color, style = stroke)
            }
            LineIcon.Check -> drawPath(path(u) { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }, color, style = Stroke(width = 2.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
            LineIcon.Copy -> {
                drawRoundRect(color, Offset(9f * u, 9f * u), Size(11f * u, 11f * u), CornerRadius(2.5f * u), style = stroke)
                drawPath(path(u) { moveTo(15f, 5.5f); lineTo(15f, 6f); moveTo(6.5f, 15f); lineTo(6f, 15f); moveTo(9f, 5f); lineTo(6.5f, 5f); cubicTo(5.5f, 5f, 5f, 5.5f, 5f, 6.5f); lineTo(5f, 15f); cubicTo(5f, 15.6f, 5.2f, 15.7f, 6f, 15f) }, color, style = stroke)
            }
            LineIcon.Message -> {
                drawPath(path(u) {
                    moveTo(6f, 4f)
                    lineTo(18f, 4f)
                    cubicTo(19.7f, 4f, 21f, 5.3f, 21f, 7f)
                    lineTo(21f, 13f)
                    cubicTo(21f, 14.7f, 19.7f, 16f, 18f, 16f)
                    lineTo(10f, 16f)
                    lineTo(6f, 20f)
                    lineTo(6f, 16f)
                    cubicTo(4.3f, 16f, 3f, 14.7f, 3f, 13f)
                    lineTo(3f, 7f)
                    cubicTo(3f, 5.3f, 4.3f, 4f, 6f, 4f)
                    close()
                }, color, style = stroke)
                drawLine(color, Offset(7f * u, 8.5f * u), Offset(17f * u, 8.5f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(7f * u, 12f * u), Offset(13f * u, 12f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            LineIcon.Link -> {
                // Two links of a chain, tilted.
                rotate(-45f, pivot = Offset(12f * u, 12f * u)) {
                    drawRoundRect(color, Offset(2.5f * u, 8.5f * u), Size(11f * u, 7f * u), CornerRadius(3.5f * u), style = stroke)
                    drawRoundRect(color, Offset(10.5f * u, 8.5f * u), Size(11f * u, 7f * u), CornerRadius(3.5f * u), style = stroke)
                }
            }
            LineIcon.Pencil -> {
                drawPath(path(u) {
                    moveTo(5f, 19f)
                    lineTo(6f, 14.5f)
                    lineTo(16f, 4.5f)
                    lineTo(19.5f, 8f)
                    lineTo(9.5f, 18f)
                    close()
                }, color, style = stroke)
                drawLine(color, Offset(14f * u, 6.5f * u), Offset(17.5f * u, 10f * u), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
        }
    }
}

/** A path described on the 24 by 24 grid, scaled by [u] to the real size. */
private fun DrawScope.path(u: Float, build: ScaledPath.() -> Unit): Path {
    val path = Path()
    ScaledPath(path, u).build()
    return path
}

private class ScaledPath(private val path: Path, private val u: Float) {
    fun moveTo(x: Float, y: Float) = path.moveTo(x * u, y * u)
    fun lineTo(x: Float, y: Float) = path.lineTo(x * u, y * u)
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) = path.cubicTo(x1 * u, y1 * u, x2 * u, y2 * u, x3 * u, y3 * u)
    fun close() = path.close()
}
