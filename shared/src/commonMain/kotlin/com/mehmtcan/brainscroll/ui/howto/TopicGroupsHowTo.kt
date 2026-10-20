package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.topicgroups.fitFontPx
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_topicgroups_1
import brainscroll.shared.generated.resources.howto_card_topicgroups_2
import brainscroll.shared.generated.resources.howto_card_topicgroups_3
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.topicgroups.topicGroupColors
import kotlin.math.sin

/** The three steps of the word groups card: pick four words of one topic, a right group is found, a wrong one is a mistake. */
internal val TopicGroupsHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_topicgroups_1, { t -> TopicGroupsDemo(step = 0, t = t) }, cycleMillis = 5600),
        HowToStep(Res.string.howto_card_topicgroups_2, { t -> TopicGroupsDemo(step = 1, t = t) }, cycleMillis = 4800),
        HowToStep(Res.string.howto_card_topicgroups_3, { t -> TopicGroupsDemo(step = 2, t = t) }, cycleMillis = 4800),
    ),
)

/** A group of the picture: its topic and its four words. */
private class DemoGroup(val title: String, val words: List<String>)

private fun groupsOf(language: Language): List<DemoGroup> = when (language) {
    Language.EN -> listOf(
        DemoGroup("Fruits", listOf("APPLE", "PEACH", "GRAPE", "LEMON")),
        DemoGroup("Colors", listOf("RED", "BLUE", "PINK", "GRAY")),
        DemoGroup("Planets", listOf("MARS", "VENUS", "SATURN", "MERCURY")),
        DemoGroup("Sports", listOf("GOLF", "RUGBY", "TENNIS", "BOXING")),
    )
    Language.TR -> listOf(
        DemoGroup("Meyveler", listOf("ELMA", "MUZ", "ÜZÜM", "KİRAZ")),
        DemoGroup("Renkler", listOf("KIRMIZI", "MAVİ", "PEMBE", "GRİ")),
        DemoGroup("Gezegenler", listOf("MARS", "VENÜS", "SATÜRN", "MERKÜR")),
        DemoGroup("Sporlar", listOf("TENİS", "GOLF", "BOKS", "YÜZME")),
    )
}

/** How the sixteen words lie at the start: (group, word of the group), row by row. Group 0 is the one that gets found. */
private val Layout = listOf(
    1 to 0, 0 to 0, 2 to 0, 3 to 0,
    0 to 1, 3 to 1, 1 to 1, 2 to 1,
    2 to 2, 1 to 2, 0 to 2, 3 to 2,
    3 to 3, 0 to 3, 2 to 3, 1 to 3,
)

private const val COLUMNS = 4

/**
 * The picture of a step. [t] is the time of the loop: the hand picks words and what that does is shown; then the picture fades and
 * starts over. Step 0 picks the four words of a group; step 1 picks the last of four and the group flies to the top and shows its
 * topic; step 2 picks a fourth word that does not belong, so the four shake and turn red.
 */
@Composable
internal fun TopicGroupsDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val groupColors = topicGroupColors(colors)
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    val language = LocalHowToWordLanguage.current
    val groups = groupsOf(language)
    val bold = TextStyle(fontWeight = FontWeight.Bold)
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.07f
        val gap = size.width * 0.025f
        val tileW = (size.width - 2 * pad - gap * (COLUMNS - 1)) / COLUMNS
        val tileH = minOf(tileW * 0.72f, (size.height - 2 * pad - gap * 3) / 4f)
        val boardH = tileH * 4 + gap * 3
        val top = (size.height - boardH) / 2f
        fun slotOrigin(slot: Int) = Offset(pad + (slot % COLUMNS) * (tileW + gap), top + (slot / COLUMNS) * (tileH + gap))
        val corner = CornerRadius(tileH * 0.2f)

        fun indexOf(group: Int, word: Int) = Layout.indexOf(group to word)

        // How picked each tile is (0 to 1), when the group is found (the time its flight starts), and when the four are rejected.
        val picked = FloatArray(16)
        var solveAt = -1f
        var rejectAt = -1f
        val taps = mutableListOf<DemoTap>()
        fun pick(slot: Int, at: Float) {
            picked[slot] = demoEase(demoSegment(t, at, at + 0.05f))
            taps += DemoTap(at, slotOrigin(slot) + Offset(tileW / 2f, tileH / 2f))
        }
        when (step) {
            0 -> for (w in 0 until 4) pick(indexOf(0, w), 0.1f + 0.19f * w)
            1 -> {
                for (w in 0 until 3) picked[indexOf(0, w)] = 1f
                pick(indexOf(0, 3), 0.08f)
                solveAt = 0.22f
            }
            else -> {
                for (w in 0 until 3) picked[indexOf(1, w)] = 1f
                pick(indexOf(3, 0), 0.1f)
                rejectAt = 0.26f
            }
        }

        // Where each tile is: after the group is found, its words are in the first row and the others follow.
        val others = (0 until 16).filter { Layout[it].first != 0 }
        val flight = demoEase(demoSegment(t, solveAt, solveAt + 0.2f))
        fun slotAfterSolve(slot: Int): Int = if (Layout[slot].first == 0) Layout[slot].second else 4 + others.indexOf(slot)
        val bannerAlpha = if (solveAt < 0f) 0f else demoEase(demoSegment(t, solveAt + 0.24f, solveAt + 0.34f))
        val reject = if (rejectAt < 0f) 0f else demoSegment(t, rejectAt, rejectAt + 0.3f)
        val rejected = reject in 0.001f..0.999f

        // The tiles that fly are drawn last, so they pass over the others.
        val order = (0 until 16).sortedBy { if (solveAt >= 0f && Layout[it].first == 0) 1 else 0 }
        for (slot in order) {
            val (group, word) = Layout[slot]
            val from = slotOrigin(slot)
            val to = if (solveAt >= 0f) slotOrigin(slotAfterSolve(slot)) else from
            var at = Offset(from.x + (to.x - from.x) * flight, from.y + (to.y - from.y) * flight)
            val isRejected = rejected && picked[slot] > 0.5f
            if (isRejected) at += Offset(sin(reject * 6.2832f * 3f) * tileW * 0.08f, 0f)
            val found = solveAt >= 0f && group == 0
            val base = colors.bgRaised
            val light = colors.textPrimary
            val fill = when {
                found -> lerp(lerp(base, light, picked[slot]), groupColors[0], flight)
                isRejected -> colors.error
                else -> lerp(base, light, picked[slot])
            }
            val ink = when {
                found -> colors.bgPage
                isRejected -> colors.textPrimary
                else -> lerp(colors.textPrimary, colors.bgPage, picked[slot])
            }
            drawRoundRect(fill.copy(alpha = fill.alpha * fade), at, Size(tileW, tileH), corner)
            val text = groups[group].words[word]
            val fontPx = measurer.fitFontPx(text, bold, this, tileW - 8.dp.toPx(), tileH * 0.3f)
            val inkAlpha = if (found) 1f - bannerAlpha else 1f
            drawCenteredText(measurer, text, at + Offset(tileW / 2f, tileH / 2f), fontPx, ink.copy(alpha = ink.alpha * fade * inkAlpha))
        }

        // The topic over the found group's row.
        if (bannerAlpha > 0f) {
            val origin = slotOrigin(0)
            val width = tileW * COLUMNS + gap * (COLUMNS - 1)
            val a = bannerAlpha * fade
            drawRoundRect(groupColors[0].copy(alpha = a), origin, Size(width, tileH), corner)
            val title = language.uppercase(groups[0].title)
            val line = groups[0].words.joinToString(", ")
            val center = origin + Offset(width / 2f, tileH / 2f)
            drawCenteredText(measurer, title, center - Offset(0f, tileH * 0.17f), measurer.fitFontPx(title, bold, this, width - 16.dp.toPx(), tileH * 0.3f), colors.bgPage.copy(alpha = a))
            drawCenteredText(measurer, line, center + Offset(0f, tileH * 0.2f), measurer.fitFontPx(line, bold, this, width - 16.dp.toPx(), tileH * 0.22f), colors.bgPage.copy(alpha = a), bold = false)
        }

        val (hand, press) = demoHand(t, taps, Offset(size.width, size.height))
        drawHandCursor(handPainter, hand, tileH * 1.1f, press, alpha = fade)
    }
}
