package com.mehmtcan.brainscroll.ui.sudoku

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draws the borders between the regions of a jigsaw board as thick lines that run through the gaps between the cells.
 *
 * The cells sit on a lattice of squares [step] wide, the first cell with its top left corner at [origin]; a border runs along a
 * line of that lattice (so through the middle of the gap) wherever two neighbouring cells belong to different regions. Where
 * exactly two borders meet at a right angle the corner is rounded with [radius], so the line turns the way the corners of the
 * cells do and does not end in a notch; where three or four meet (or a border runs on straight) the lines simply cross. A border
 * ends at the edge of the board, where the frame of the board takes over.
 *
 * [regionOf] gives the region of every cell of a [size] by [size] board, row by row.
 */
fun DrawScope.drawRegionBorders(
    regionOf: IntArray,
    size: Int,
    step: Float,
    origin: Offset,
    thickness: Float,
    radius: Float,
    color: Color,
) {
    // The border pieces around the lattice point (x, y), 0..size on both axes. A point on the edge of the board has fewer.
    fun up(x: Int, y: Int) = y >= 1 && x in 1 until size && regionOf[(y - 1) * size + x - 1] != regionOf[(y - 1) * size + x]
    fun down(x: Int, y: Int) = y < size && x in 1 until size && regionOf[y * size + x - 1] != regionOf[y * size + x]
    fun left(x: Int, y: Int) = x >= 1 && y in 1 until size && regionOf[(y - 1) * size + x - 1] != regionOf[y * size + x - 1]
    fun right(x: Int, y: Int) = x < size && y in 1 until size && regionOf[(y - 1) * size + x] != regionOf[y * size + x]

    fun point(x: Int, y: Int) = Offset(origin.x + x * step, origin.y + y * step)

    /** True if the corner at (x, y) is rounded: exactly one vertical and one horizontal border meet there. */
    fun rounded(x: Int, y: Int): Boolean {
        val vertical = listOf(up(x, y), down(x, y)).count { it }
        val horizontal = listOf(left(x, y), right(x, y)).count { it }
        return vertical == 1 && horizontal == 1
    }

    val cut = minOf(radius, step / 2f)
    val path = Path()
    // The straight pieces, each shortened at the ends that are rounded corners.
    for (y in 0..size) for (x in 0 until size) {
        // The piece between (x, y) and (x + 1, y), a horizontal border.
        if (!(right(x, y) && left(x + 1, y))) continue
        val start = point(x, y).let { if (rounded(x, y)) it.copy(x = it.x + cut) else it }
        val end = point(x + 1, y).let { if (rounded(x + 1, y)) it.copy(x = it.x - cut) else it }
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)
    }
    for (y in 0 until size) for (x in 0..size) {
        // The piece between (x, y) and (x, y + 1), a vertical border.
        if (!(down(x, y) && up(x, y + 1))) continue
        val start = point(x, y).let { if (rounded(x, y)) it.copy(y = it.y + cut) else it }
        val end = point(x, y + 1).let { if (rounded(x, y + 1)) it.copy(y = it.y - cut) else it }
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)
    }
    // The rounded corners: from the end of one piece to the start of the other, with the lattice point as the control point.
    for (y in 1 until size) for (x in 1 until size) {
        if (!rounded(x, y)) continue
        val here = point(x, y)
        val horizontalEnd = if (right(x, y)) Offset(here.x + cut, here.y) else Offset(here.x - cut, here.y)
        val verticalEnd = if (down(x, y)) Offset(here.x, here.y + cut) else Offset(here.x, here.y - cut)
        path.moveTo(horizontalEnd.x, horizontalEnd.y)
        path.quadraticTo(here.x, here.y, verticalEnd.x, verticalEnd.y)
    }
    drawPath(path, color, style = Stroke(width = thickness, cap = StrokeCap.Butt))
}
