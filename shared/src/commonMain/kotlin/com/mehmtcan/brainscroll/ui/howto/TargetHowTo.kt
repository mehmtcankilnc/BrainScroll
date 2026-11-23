package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_target_start
import brainscroll.shared.generated.resources.howto_target_tech
import brainscroll.shared.generated.resources.howto_card_target_1
import brainscroll.shared.generated.resources.howto_card_target_2
import brainscroll.shared.generated.resources.howto_card_target_3
import brainscroll.shared.generated.resources.target_label
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import org.jetbrains.compose.resources.stringResource

/** The three steps of the target card: a number, an operation and a number; the result is used again; numbers that do not fit are dimmed. */
internal val TargetHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_target_1, { t -> TargetDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_target_2, { t -> TargetDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_target_3, { t -> TargetDemo(step = 2, t = t) }),
        sceneStep(Res.string.howto_target_tech, TargetOvershootScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_target_start, TargetStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private val Signs = listOf("+", "−", "×", "÷")

private fun apply(a: Int, op: Int, b: Int): Int? = when (op) {
    0 -> a + b
    1 -> (a - b).takeIf { it > 0 }
    2 -> a * b
    else -> if (b != 0 && a % b == 0) a / b else null
}

@Composable
internal fun TargetDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    val targetLabel = stringResource(Res.string.target_label)
    // The numbers on the table, the target, the operation done before (the line under the numbers), and the move of this step:
    // the tiles tapped (by place), the operation key, and when each tap lands.
    val target: Int
    val numbers: List<Int>
    val before: String?
    val a: Int
    val b: Int
    val op: Int
    val times: Triple<Float, Float, Float>
    when (step) {
        0 -> {
            target = 17; numbers = listOf(3, 5, 2, 8); before = null
            a = 0; b = 1; op = 2; times = Triple(0.12f, 0.32f, 0.52f)
        }
        1 -> {
            target = 17; numbers = listOf(15, 2, 8); before = "3 × 5 = 15"
            a = 0; b = 1; op = 0; times = Triple(0.12f, 0.32f, 0.52f)
        }
        else -> {
            target = 2; numbers = listOf(6, 4, 9); before = null
            a = 0; b = 1; op = 1; times = Triple(0.12f, 0.32f, 0.55f)
        }
    }
    val result = apply(numbers[a], op, numbers[b]) ?: 0
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val w = size.width
        val h = size.height
        val (tapA, tapOp, tapB) = times
        val merged = t >= tapB + 0.05f
        val firstPicked = t >= tapA && !merged
        val opPicked = t >= tapOp && !merged
        val tokens = if (merged) numbers.filterIndexed { i, _ -> i != b }.mapIndexed { i, v -> if (i == a) result else v } else numbers
        val reached = merged && result == target

        // The target: a card with its name above the number, which turns green once it is reached.
        val cardW = w * 0.42f
        val cardH = h * 0.2f
        val cardTopLeft = Offset((w - cardW) / 2f, h * 0.05f)
        drawRoundRect(colors.bgSurface.copy(alpha = fade), cardTopLeft, Size(cardW, cardH), CornerRadius(w * 0.04f))
        drawCenteredText(measurer, targetLabel, Offset(w / 2f, cardTopLeft.y + cardH * 0.22f), w * 0.04f, colors.textSecondary.copy(alpha = fade), bold = false)
        drawCenteredText(measurer, target.toString(), Offset(w / 2f, cardTopLeft.y + cardH * 0.62f), w * 0.115f, (if (reached) colors.correctFill else colors.pendingFill).copy(alpha = fade))

        // The tiles: always laid out for the numbers the puzzle started with, so a merge does not move the others.
        val tile = minOf(w * 0.2f, (w * 0.86f - w * 0.03f * 3) / 4f)
        val rowWidth = tile * numbers.size + w * 0.03f * (numbers.size - 1)
        val rowLeft = (w - rowWidth) / 2f
        val rowTop = h * 0.36f
        fun tileTopLeft(i: Int) = Offset(rowLeft + i * (tile + w * 0.03f), rowTop)
        fun tileCenter(i: Int) = tileTopLeft(i) + Offset(tile / 2f, tile / 2f)
        // With a number and an operation picked, the numbers that make a move that is not allowed are dimmed.
        for ((i, value) in tokens.withIndex()) {
            val isFirst = i == a && firstPicked
            val dim = firstPicked && opPicked && i != a && apply(numbers[a], op, numbers[i]) == null
            val isResult = merged && i == a
            val pop = if (isResult) demoPop(t, tapB + 0.05f, 0.06f).coerceAtLeast(0.01f) else 1f
            val alphaTile = fade * (if (dim) 0.35f else 1f)
            val tl = tileTopLeft(i)
            val borderColor = when {
                isResult && reached -> colors.correctFill
                isFirst -> colors.pendingFill
                else -> colors.borderStrong.copy(alpha = 0.5f)
            }
            scale(pop, pop, pivot = tileCenter(i)) {
                drawRoundRect((if (isFirst) colors.bgRaised else colors.bgSurface).copy(alpha = alphaTile), tl, Size(tile, tile), CornerRadius(tile * 0.16f))
                drawRoundRect(borderColor.copy(alpha = borderColor.alpha * alphaTile), tl, Size(tile, tile), CornerRadius(tile * 0.16f), style = Stroke(width = (if (isFirst) 3.dp else 1.5.dp).toPx()))
                val digits = value.toString().length
                drawCenteredText(measurer, value.toString(), tileCenter(i), tile * (if (digits <= 2) 0.46f else 0.36f), (if (isResult && reached) colors.correctFill else colors.textPrimary).copy(alpha = alphaTile))
            }
        }
        // A tile that is merged away leaves its place empty.
        if (merged) {
            val gone = tokens.size
            for (i in gone until numbers.size) {
                val tl = tileTopLeft(i)
                drawRoundRect(colors.borderStrong.copy(alpha = 0.12f * fade), tl, Size(tile, tile), CornerRadius(tile * 0.16f), style = Stroke(width = 1.dp.toPx()))
            }
        }

        // The line of the last operation (and the one being done, once it is done).
        val line = if (merged) "${numbers[a]} ${Signs[op]} ${numbers[b]} = $result" else before
        if (line != null) drawCenteredText(measurer, line, Offset(w / 2f, h * 0.65f), w * 0.05f, colors.textSecondary.copy(alpha = fade), bold = false)

        // The four operation keys.
        val keyW = w * 0.19f
        val keyH = h * 0.11f
        val keyGap = w * 0.03f
        val keysLeft = (w - (keyW * 4 + keyGap * 3)) / 2f
        val keysTop = h * 0.76f
        fun keyCenter(i: Int) = Offset(keysLeft + i * (keyW + keyGap) + keyW / 2f, keysTop + keyH / 2f)
        for (i in 0 until 4) {
            val selected = opPicked && i == op
            val enabled = firstPicked || selected
            val tl = Offset(keyCenter(i).x - keyW / 2f, keysTop)
            val a2 = fade * (if (t >= tapA || merged) 1f else 0.5f)
            drawRoundRect((if (selected) colors.bgRaised else colors.bgSurface).copy(alpha = a2), tl, Size(keyW, keyH), CornerRadius(keyH * 0.25f))
            val edge = if (selected) colors.pendingFill else colors.borderStrong.copy(alpha = 0.6f)
            drawRoundRect(edge.copy(alpha = edge.alpha * a2), tl, Size(keyW, keyH), CornerRadius(keyH * 0.25f), style = Stroke(width = 1.5.dp.toPx()))
            drawCenteredText(measurer, Signs[i], keyCenter(i), keyH * 0.55f, colors.textPrimary.copy(alpha = a2))
        }

        val taps = listOf(DemoTap(tapA, tileCenter(a)), DemoTap(tapOp, keyCenter(op)), DemoTap(tapB, tileCenter(b)))
        val (hand, press) = demoHand(t, taps, Offset(w, h), travel = 0.1f)
        drawHandCursor(handPainter, hand, w * 0.2f, press, alpha = fade)
    }
}
