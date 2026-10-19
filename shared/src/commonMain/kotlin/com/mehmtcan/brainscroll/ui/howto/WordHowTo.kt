package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_word_1
import brainscroll.shared.generated.resources.key_enter
import org.jetbrains.compose.resources.stringResource
import brainscroll.shared.generated.resources.howto_card_word_2
import brainscroll.shared.generated.resources.howto_card_word_3
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.wordle.ENTER_UNITS
import com.mehmtcan.brainscroll.ui.wordle.SPECIAL_UNITS
import com.mehmtcan.brainscroll.ui.wordle.keyboardRows

/** The three steps of the word card: type a word and press enter, the colors say what is right, and the next try is the word. */
internal val WordHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_word_1, { t -> WordDemo(step = 0, t = t) }, cycleMillis = 6400),
        HowToStep(Res.string.howto_card_word_2, { t -> WordDemo(step = 1, t = t) }, cycleMillis = 4800),
        HowToStep(Res.string.howto_card_word_3, { t -> WordDemo(step = 2, t = t) }, cycleMillis = 6400),
    ),
)

private const val COLUMNS = 5
private const val ROWS = 4

// What each letter of a try says: 2 right place (green), 1 other place (yellow), 0 not in the word (gray).
/** The first try of the picture, what its letters say, and the word that was hidden, in the language of the puzzle. */
private class DemoWords(val first: String, val firstResult: List<Int>, val answer: String)

private fun wordsOf(language: Language) = when (language) {
    Language.EN -> DemoWords("CRANE", listOf(0, 1, 2, 0, 2), "SHARE")
    Language.TR -> DemoWords("KİTAP", listOf(2, 0, 0, 1, 0), "KALEM")
}

private const val FLIP = 0.07f

@Composable
internal fun WordDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    val enterLabel = stringResource(Res.string.key_enter)
    val language = LocalHowToWordLanguage.current
    val words = wordsOf(language)
    val keyRows = keyboardRows(language)
    val Try1 = words.first to words.firstResult
    val Answer = words.answer
    // The rows shown: each is a word, what each letter says, when its letters are typed (one after the other), when ENTER is pressed.
    class Row(val word: String, val result: List<Int>?, val typeAt: Float, val typeStep: Float, val enterAt: Float)
    val rows: List<Row>
    val before: List<Pair<String, List<Int>>>
    when (step) {
        0 -> {
            before = emptyList()
            rows = listOf(Row(words.first, null, 0.08f, 0.1f, 0.68f))
        }
        1 -> {
            before = emptyList()
            rows = listOf(Row(words.first, words.firstResult, -1f, 0f, 0.15f))
        }
        else -> {
            before = listOf(Try1)
            rows = listOf(Row(Answer, List(5) { 2 }, 0.06f, 0.09f, 0.62f))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val w = size.width
        val h = size.height
        val tile = w * 0.12f
        val tileGap = w * 0.02f
        val gridLeft = (w - (tile * COLUMNS + tileGap * (COLUMNS - 1))) / 2f
        val gridTop = h * 0.05f
        fun tileOrigin(row: Int, column: Int) = Offset(gridLeft + column * (tile + tileGap), gridTop + row * (tile + tileGap))
        val corner = CornerRadius(tile * 0.12f)

        fun drawTile(o: Offset, letter: Char?, state: Int, pop: Float = 1f) {
            // state: -1 empty, -2 typed (not sent), 0 gray, 1 yellow, 2 green
            val c = o + Offset(tile / 2f, tile / 2f)
            scale(pop, pop, pivot = c) {
                when (state) {
                    2 -> drawRoundRect(colors.correctFill.copy(alpha = fade), o, Size(tile, tile), corner)
                    0 -> drawRoundRect(colors.absentFill.copy(alpha = fade), o, Size(tile, tile), corner)
                    1 -> drawRoundRect(colors.pendingFill.copy(alpha = fade), o, Size(tile, tile), corner, style = Stroke(width = 2.5.dp.toPx()))
                    -2 -> drawRoundRect(colors.borderStrong.copy(alpha = fade), o, Size(tile, tile), corner, style = Stroke(width = 2.dp.toPx()))
                    else -> drawRoundRect(colors.borderSubtle.copy(alpha = fade), o, Size(tile, tile), corner, style = Stroke(width = 2.dp.toPx()))
                }
                if (letter != null) {
                    val content = when (state) {
                        2 -> colors.correctOn
                        0 -> colors.absentOn
                        1 -> colors.pendingFill
                        else -> colors.textPrimary
                    }
                    drawCenteredText(measurer, letter.toString(), c, tile * 0.55f, content.copy(alpha = fade))
                }
            }
        }

        // The tries that were sent before, then the try of this step, then empty rows.
        for (r in 0 until ROWS) for (c in 0 until COLUMNS) drawTile(tileOrigin(r, c), null, -1)
        before.forEachIndexed { r, (word, result) ->
            for (c in 0 until COLUMNS) drawTile(tileOrigin(r, c), word[c], result[c])
        }
        val rowIndex = before.size
        val row = rows.first()
        for (c in 0 until COLUMNS) {
            val typedAt = if (row.typeAt < 0f) -1f else row.typeAt + c * row.typeStep
            val typed = typedAt < 0f || t >= typedAt
            if (!typed) continue
            val pop = if (typedAt >= 0f) demoPop(t, typedAt, 0.04f).coerceAtLeast(0.01f) else 1f
            // After ENTER every tile turns over, one after the other, and shows its color.
            val flipAt = row.enterAt + c * 0.05f
            val sent = row.result != null && t >= flipAt + FLIP / 2f
            val squash = if (row.result != null && t >= flipAt && t < flipAt + FLIP) abs(1f - 2f * demoSegment(t, flipAt, flipAt + FLIP)) else 1f
            val o = tileOrigin(rowIndex, c)
            val center = o + Offset(tile / 2f, tile / 2f)
            scale(1f, squash.coerceAtLeast(0.05f), pivot = center) {
                drawTile(o, row.word[c], if (sent) row.result!![c] else -2, pop)
            }
        }

        // The keyboard, laid out as the real one (WordleKeyboard): the first row is centered, the backspace (red) ends the second
        // row, and ENTER (green) ends the third; the letters of the third row are wider so that it is as long as the others.
        val keyH = h * 0.085f
        val keyGap = w * 0.012f
        val keysTop = h * 0.64f
        val rowWidth = w * 0.94f
        val widest = maxOf(keyRows[0].length.toFloat(), keyRows[1].length + SPECIAL_UNITS, keyRows[2].length + SPECIAL_UNITS)
        val thirdWeight = (widest - ENTER_UNITS) / keyRows[2].length
        val sentLetters = HashMap<Char, Int>()
        (before + (if (row.result != null && t >= row.enterAt + 0.3f) listOf(row.word to row.result) else emptyList())).forEach { (word, result) ->
            word.forEachIndexed { i, ch -> sentLetters[ch] = maxOf(sentLetters[ch] ?: -1, result[i]) }
        }
        // The place (center and width) of every key.
        val letterKeys = HashMap<Char, Pair<Offset, Float>>()
        var backspaceKey = Offset.Zero to 0f
        var enterKey = Offset.Zero to 0f
        keyRows.forEachIndexed { rowIndex, letters ->
            // The pieces of the row, left to right: a letter is a key, a negative number a space of that many units.
            val pieces = ArrayList<Pair<Char?, Float>>()
            val free = widest - letters.length
            if (rowIndex == 0 && free > 0f) pieces += null to -(free / 2f)
            letters.forEach { pieces += it to (if (rowIndex == 2) thirdWeight else 1f) }
            when (rowIndex) {
                0 -> if (free > 0f) pieces += null to -(free / 2f)
                1 -> pieces += '' to SPECIAL_UNITS
                else -> pieces += '' to ENTER_UNITS
            }
            val total = pieces.sumOf { kotlin.math.abs(it.second).toDouble() }.toFloat()
            val unit = (rowWidth - keyGap * (pieces.size - 1)) / total
            var x = (w - rowWidth) / 2f
            val y = keysTop + rowIndex * (keyH + keyGap) + keyH / 2f
            for ((key, weight) in pieces) {
                val width = kotlin.math.abs(weight) * unit
                val center = Offset(x + width / 2f, y)
                when (key) {
                    null -> Unit
                    '' -> backspaceKey = center to width
                    '' -> enterKey = center to width
                    else -> letterKeys[key] = center to width
                }
                x += width + keyGap
            }
        }
        fun keyCenter(ch: Char): Offset = letterKeys.getValue(ch).first
        val enterCenter = enterKey.first
        for ((ch, place) in letterKeys) {
            val (c, keyW) = place
            val result = sentLetters[ch]
            val fill = when (result) {
                2 -> colors.correctFill
                else -> colors.bgSurface
            }
            val content = when (result) {
                2 -> colors.correctOn
                1 -> colors.pendingFill
                0 -> colors.textDisabled
                else -> colors.textPrimary
            }
            val o = Offset(c.x - keyW / 2f, c.y - keyH / 2f)
            drawRoundRect(fill.copy(alpha = fade), o, Size(keyW, keyH), CornerRadius(keyH * 0.2f))
            if (result == 1) drawRoundRect(colors.pendingFill.copy(alpha = fade), o, Size(keyW, keyH), CornerRadius(keyH * 0.2f), style = Stroke(width = 1.5.dp.toPx()))
            drawCenteredText(measurer, ch.toString(), c, keyH * 0.5f, content.copy(alpha = fade))
        }
        // The backspace (red) at the end of the second row and ENTER (green) at the end of the third.
        val (backCenter, backWidth) = backspaceKey
        drawRoundRect(colors.error.copy(alpha = fade), Offset(backCenter.x - backWidth / 2f, backCenter.y - keyH / 2f), Size(backWidth, keyH), CornerRadius(keyH * 0.2f))
        drawCenteredText(measurer, "←", backCenter, keyH * 0.5f, colors.textPrimary.copy(alpha = fade))
        val (_, enterWidth) = enterKey
        drawRoundRect(colors.correctFill.copy(alpha = fade), Offset(enterCenter.x - enterWidth / 2f, enterCenter.y - keyH / 2f), Size(enterWidth, keyH), CornerRadius(keyH * 0.2f))
        drawCenteredText(measurer, enterLabel, enterCenter, keyH * 0.34f, colors.correctOn.copy(alpha = fade))

        // The hand types the letters one after the other and presses ENTER (in step 2 only the colors are shown).
        val stops = ArrayList<DemoTap>()
        if (row.typeAt >= 0f) {
            for (c in 0 until COLUMNS) stops += DemoTap(row.typeAt + c * row.typeStep, keyCenter(row.word[c]))
            stops += DemoTap(row.enterAt, enterCenter)
        } else {
            stops += DemoTap(row.enterAt, enterCenter)
        }
        val (hand, press) = demoHand(t, stops, Offset(w, h), travel = 0.045f, pressLength = 0.04f)
        drawHandCursor(handPainter, hand, w * 0.1f, press, alpha = fade)
    }
}

private fun abs(x: Float): Float = if (x < 0f) -x else x
