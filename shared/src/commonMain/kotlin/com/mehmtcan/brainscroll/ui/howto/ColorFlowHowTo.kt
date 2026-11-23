package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_colorflow_start
import brainscroll.shared.generated.resources.howto_colorflow_tech
import brainscroll.shared.generated.resources.howto_card_colorflow_1
import brainscroll.shared.generated.resources.howto_card_colorflow_2
import brainscroll.shared.generated.resources.howto_card_colorflow_3
import com.mehmtcan.brainscroll.ui.colorflow.drawFlowBoard
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the color flow card: draw from a dot to its twin, a new path cuts the one it crosses, fill every cell. */
internal val ColorFlowHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_colorflow_1, { t -> ColorFlowDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_colorflow_2, { t -> ColorFlowDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_colorflow_3, { t -> ColorFlowDemo(step = 2, t = t) }),
        HowToStep(Res.string.howto_colorflow_tech, { t -> ColorFlowDemo(step = 3, t = t) }, title = Res.string.howto_title_solve),
        HowToStep(Res.string.howto_colorflow_start, { t -> ColorFlowDemo(step = 4, t = t) }, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private const val N = 4

// A 4x4 board and its way: red down the left column, blue over the top and down, green in the middle, yellow along the bottom right.
private val Way = listOf(
    listOf(0, 4, 8, 12),
    listOf(1, 2, 3, 7),
    listOf(5, 6, 10, 9),
    listOf(11, 15, 14, 13),
)
private val Dots = Way.map { it.first() to it.last() }

/** What is drawn before the stroke of a step, the stroke itself (color and cells), and where it ends. */
private class Stroke(val color: Int, val cells: List<Int>)

private val Steps: List<Pair<List<List<Int>>, Stroke>> = listOf(
    // 1: red is drawn from one dot to the other.
    List(4) { emptyList<Int>() } to Stroke(0, Way[0]),
    // 2: blue already runs through the middle; green is drawn across it and cuts it.
    List(4) { if (it == 1) listOf(1, 2, 6, 7) else emptyList<Int>() } to Stroke(2, listOf(9, 10, 6, 5)),
    // 3: all but yellow is done; yellow fills the last cells.
    List(4) { if (it == 3) emptyList<Int>() else Way[it] } to Stroke(3, Way[3]),
    // 4: the blue dot has other colours on its left and below, so its path can only go right.
    List(4) { emptyList<Int>() } to Stroke(1, Way[1]),
    // 5: the yellow dot at the bottom has red on its left and green above, so its path starts to the right.
    List(4) { emptyList<Int>() } to Stroke(3, Way[3].reversed()),
)

@Composable
internal fun ColorFlowDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    val (before, stroke) = Steps[step]
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val side = minOf(size.width, size.height) * 0.84f
        val cell = side / N
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        fun center(c: Int) = Offset(left + (c % N + 0.5f) * cell, top + (c / N + 0.5f) * cell)

        val start = 0.2f
        val end = 0.7f
        val progress = demoEase(demoSegment(t, start, end))
        val shown = (1 + (progress * (stroke.cells.size - 1) + 0.0001f).toInt()).coerceAtMost(stroke.cells.size)
        val drawing = t >= start
        val paths = before.map { it }.toMutableList()
        if (drawing) {
            val prefix = stroke.cells.take(shown)
            // The new path takes its cells from the paths it crosses: those are cut off at the first cell taken.
            paths.indices.forEach { other ->
                if (other == stroke.color) return@forEach
                val cut = paths[other].indexOfFirst { it in prefix }
                if (cut >= 0) paths[other] = paths[other].take(cut).takeIf { it.size >= 2 }.orEmpty()
            }
            paths[stroke.color] = prefix
        }
        drawFlowBoard(N, left, top, cell, paths, Dots, colors.bgSurface, colors.borderStrong.copy(alpha = 0.35f), fade)

        // The hand waits on the first dot, presses and drags along the stroke.
        val index = (progress * (stroke.cells.size - 1)).coerceIn(0f, (stroke.cells.size - 1).toFloat())
        val whole = index.toInt().coerceAtMost(stroke.cells.size - 2)
        val a = center(stroke.cells[whole])
        val b = center(stroke.cells[whole + 1])
        val f = index - whole
        val hand = Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
        drawHandCursor(handPainter, hand, cell * 0.95f, demoSegment(t, 0.1f, 0.2f).let { if (it in 0.01f..0.99f) it else 0f }, alpha = fade)
    }
}
