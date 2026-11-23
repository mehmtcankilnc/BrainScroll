package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_mines_goal
import brainscroll.shared.generated.resources.howto_mines_single
import brainscroll.shared.generated.resources.howto_mines_121
import brainscroll.shared.generated.resources.howto_mines_start
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_card_mines_1
import brainscroll.shared.generated.resources.howto_card_mines_2
import brainscroll.shared.generated.resources.howto_card_mines_3
import com.mehmtcan.brainscroll.ui.mines.drawFlag
import com.mehmtcan.brainscroll.ui.mines.numberColor
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.math.abs
import kotlin.math.max

/**
 * The steps of the mines card: the aim, tap to open (and the numbers), long-press to flag, then how it is solved (a 1 with one hidden
 * cell around it, a number that has all its flags, the 1-2-1 row) and, last, where to start.
 */
internal val MinesHowTo = HowToCardContent(
    listOf(
        sceneStep(Res.string.howto_mines_goal, MinesGoalScene),
        HowToStep(Res.string.howto_card_mines_1, { t -> MinesDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_mines_2, { t -> MinesDemo(step = 1, t = t) }),
        sceneStep(Res.string.howto_mines_single, MinesSingleScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_card_mines_3, MinesChordScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_mines_121, MinesPatternScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_mines_start, MinesStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private const val N = 5

// The two mines of the picture, and the numbers they make.
private val Mines = setOf(1 * N + 3, 3 * N + 1)

private fun neighbors(cell: Int): List<Int> {
    val r = cell / N
    val c = cell % N
    return (-1..1).flatMap { dr -> (-1..1).map { dc -> dr to dc } }
        .filter { it != 0 to 0 }
        .map { (dr, dc) -> r + dr to c + dc }
        .filter { (nr, nc) -> nr in 0 until N && nc in 0 until N }
        .map { (nr, nc) -> nr * N + nc }
}

private fun numberOf(cell: Int) = neighbors(cell).count { it in Mines }

/** The cells a tap on [cell] opens, with how far each is from it (a number stops the opening, a zero goes on to its neighbors). */
private fun opening(cell: Int): Map<Int, Int> {
    val out = LinkedHashMap<Int, Int>()
    out[cell] = 0
    val queue = ArrayDeque<Int>()
    if (numberOf(cell) == 0) queue += cell
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        for (n in neighbors(current)) {
            if (n in out || n in Mines) continue
            out[n] = out.getValue(current) + 1
            if (numberOf(n) == 0) queue += n
        }
    }
    return out
}

@Composable
internal fun MinesDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    // Cells that are open from the start (with the tap that opens the rest), the flag, and the taps of the hand.
    val firstOpening = opening(0)
    val openedAt = HashMap<Int, Float>() // cell -> time it opens (negative: from the start)
    var flagAt = -1f
    var flagCell = 3 * N + 1
    val stops = ArrayList<DemoTap>()
    var pressLength = 0.06f
    val tapCell: Int
    when (step) {
        0 -> {
            for ((cell, distance) in firstOpening) openedAt[cell] = 0.28f + 0.04f * distance
            tapCell = 0
        }
        1 -> {
            for (cell in firstOpening.keys) openedAt[cell] = -1f
            flagAt = 0.5f
            pressLength = 0.28f
            tapCell = flagCell
        }
        else -> {
            for (cell in firstOpening.keys) openedAt[cell] = -1f
            flagAt = -1f
            // A double tap on the number 1 above the flag opens the safe cells around it.
            val number = 2 * N + 1
            for (n in neighbors(number)) if (n !in Mines && n !in openedAt) openedAt[n] = 0.42f
            tapCell = number
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.09f
        val gap = size.width * 0.012f
        val cell = (size.width - 2 * pad - gap * (N - 1)) / N
        val top = (size.height - (cell * N + gap * (N - 1))) / 2f
        fun origin(c: Int) = Offset(pad + (c % N) * (cell + gap), top + (c / N) * (cell + gap))
        fun center(c: Int) = origin(c) + Offset(cell / 2f, cell / 2f)
        val corner = CornerRadius(cell * 0.16f)

        for (c in 0 until N * N) {
            val o = origin(c)
            val at = openedAt[c]
            val open = at != null && t >= max(at, 0f)
            val pop = if (at != null && at > 0f && open) demoPop(t, at, 0.06f).coerceAtLeast(0.01f) else 1f
            if (!open) {
                drawRoundRect(colors.bgRaised.copy(alpha = fade), o, Size(cell, cell), corner)
                drawRoundRect(colors.borderStrong.copy(alpha = 0.6f * fade), o, Size(cell, cell), corner, style = Stroke(width = 1.dp.toPx()))
            } else {
                scale(pop, pop, pivot = center(c)) {
                    drawRoundRect(colors.bgSurface.copy(alpha = fade), o, Size(cell, cell), corner)
                    val n = numberOf(c)
                    if (n > 0) drawCenteredText(measurer, n.toString(), center(c), cell * 0.5f, numberColor(n, colors).copy(alpha = fade))
                }
            }
        }
        // The flag: on the cell from [flagAt] (step 2), or from the start in step 3.
        val flagged = (step == 1 && flagAt >= 0f && t >= flagAt) || step == 2
        if (flagged) {
            val c = center(flagCell)
            val side = cell * 0.62f
            val pop = if (step == 1) demoPop(t, flagAt, 0.06f).coerceAtLeast(0.01f) else 1f
            scale(pop, pop, pivot = c) {
                inset(c.x - side / 2f, c.y - side / 2f, size.width - (c.x + side / 2f), size.height - (c.y + side / 2f)) {
                    drawFlag(colors.textPrimary.copy(alpha = fade), colors.error.copy(alpha = fade))
                }
            }
            // The cell under the flag stays closed.
        }

        when (step) {
            0 -> stops += DemoTap(0.25f, center(tapCell))
            1 -> stops += DemoTap(0.2f, center(tapCell))
            else -> {
                stops += DemoTap(0.25f, center(tapCell))
                stops += DemoTap(0.35f, center(tapCell))
            }
        }
        val (hand, press) = demoHand(t, stops, Offset(size.width, size.height), pressLength = pressLength)
        drawHandCursor(handPainter, hand, cell * 0.95f, press, alpha = fade)
    }
}
