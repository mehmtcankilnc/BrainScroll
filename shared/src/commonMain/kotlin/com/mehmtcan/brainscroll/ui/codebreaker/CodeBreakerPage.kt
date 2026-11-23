package com.mehmtcan.brainscroll.ui.codebreaker

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import brainscroll.shared.generated.resources.a11y_codebreaker_absent
import brainscroll.shared.generated.resources.a11y_codebreaker_exact
import brainscroll.shared.generated.resources.a11y_codebreaker_present
import com.mehmtcan.brainscroll.game.codebreaker.CodeMark
import com.mehmtcan.brainscroll.game.codebreaker.CodePuzzle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_codebreaker_board
import brainscroll.shared.generated.resources.a11y_codebreaker_check
import brainscroll.shared.generated.resources.a11y_codebreaker_empty
import brainscroll.shared.generated.resources.a11y_codebreaker_guess
import brainscroll.shared.generated.resources.a11y_codebreaker_pick
import brainscroll.shared.generated.resources.a11y_codebreaker_slot
import brainscroll.shared.generated.resources.a11y_key_backspace
import brainscroll.shared.generated.resources.cb_symbol_0
import brainscroll.shared.generated.resources.cb_symbol_1
import brainscroll.shared.generated.resources.cb_symbol_2
import brainscroll.shared.generated.resources.cb_symbol_3
import brainscroll.shared.generated.resources.cb_symbol_4
import brainscroll.shared.generated.resources.cb_symbol_5
import brainscroll.shared.generated.resources.cb_symbol_6
import brainscroll.shared.generated.resources.cb_symbol_7
import brainscroll.shared.generated.resources.codebreaker_hint
import brainscroll.shared.generated.resources.codebreaker_result
import brainscroll.shared.generated.resources.codebreaker_try
import brainscroll.shared.generated.resources.codebreaker_won
import brainscroll.shared.generated.resources.result_stat_guesses
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.codebreaker.CodeResult
import com.mehmtcan.brainscroll.game.codebreaker.CodeSnapshot
import com.mehmtcan.brainscroll.game.codebreaker.CodeStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The shapes laid on the colours: a screen reader cannot tell a colour by looking, a player who cannot tell colours apart can tell a shape. */
private const val Symbols = "▲●■◆★✚♥☾"

internal fun codeSymbol(color: Int): Char = Symbols[color.mod(Symbols.length)]

/** The eight colours, each far from the others (the first ones are used first, so the easy boards have the most different five). */
internal val CodeColors = listOf(
    Color(0xFFE5484D), Color(0xFFF5A524), Color(0xFF2FB36B), Color(0xFF2F7FE0),
    Color(0xFF8E4EC6), Color(0xFF12A5A5), Color(0xFFE0568F), Color(0xFF9A6B3F),
)

private val SymbolNames: List<StringResource> = listOf(
    Res.string.cb_symbol_0, Res.string.cb_symbol_1, Res.string.cb_symbol_2, Res.string.cb_symbol_3,
    Res.string.cb_symbol_4, Res.string.cb_symbol_5, Res.string.cb_symbol_6, Res.string.cb_symbol_7,
)

/** At least this high; a result card that needs more (big text) makes it higher and the list smaller. */
private val BottomAreaHeight = 152.dp
private val KeyHeight = 44.dp
private val KeyGap = 6.dp
private val MaxPeg = 48.dp

/** A peg of a try, ring included: five of them and the number fit the narrowest phone. */
private val PegSize = 46.dp

/** What is chosen so far for the next try, the tries so far and the code, for tests. */
internal val CodeInputKey = SemanticsPropertyKey<List<Int>>("CodeInput")
private var SemanticsPropertyReceiver.inputColors by CodeInputKey
internal val CodeTriesKey = SemanticsPropertyKey<Int>("CodeTries")
private var SemanticsPropertyReceiver.triesCount by CodeTriesKey
internal val CodeSecretKey = SemanticsPropertyKey<List<Int>>("CodeSecret")
private var SemanticsPropertyReceiver.secretColors by CodeSecretKey

/** Lays shape [color] (0 to 7) on the point [c], [s] big from the middle to its edge, drawn so that it sits exactly in the middle. */
private fun DrawScope.drawCodeSymbol(color: Int, c: Offset, s: Float, ink: Color) {
    fun poly(vararg p: Pair<Float, Float>) = Path().apply {
        moveTo(c.x + p[0].first * s, c.y + p[0].second * s)
        for (i in 1 until p.size) lineTo(c.x + p[i].first * s, c.y + p[i].second * s)
        close()
    }
    when (color.mod(8)) {
        // The triangle is drawn so that its middle of mass, not its box, sits on the middle of the peg.
        0 -> drawPath(poly(0f to -0.95f, 0.95f to 0.65f, -0.95f to 0.65f), ink)
        1 -> drawCircle(ink, s * 0.85f, c)
        2 -> drawRect(ink, Offset(c.x - s * 0.78f, c.y - s * 0.78f), Size(s * 1.56f, s * 1.56f))
        3 -> drawPath(poly(0f to -1f, 0.85f to 0f, 0f to 1f, -0.85f to 0f), ink)
        4 -> {
            val points = (0 until 10).map { i ->
                val r = if (i % 2 == 0) 1.02f else 0.42f
                val a = -PI / 2 + i * PI / 5
                (cos(a) * r).toFloat() to (sin(a) * r + 0.05f).toFloat()
            }
            drawPath(poly(*points.toTypedArray()), ink)
        }
        5 -> {
            drawRect(ink, Offset(c.x - s * 0.95f, c.y - s * 0.3f), Size(s * 1.9f, s * 0.6f))
            drawRect(ink, Offset(c.x - s * 0.3f, c.y - s * 0.95f), Size(s * 0.6f, s * 1.9f))
        }
        6 -> drawPath(
            Path().apply {
                moveTo(c.x, c.y + s * 0.95f)
                cubicTo(c.x - s * 1.5f, c.y - s * 0.05f, c.x - s * 0.95f, c.y - s * 1.05f, c.x, c.y - s * 0.4f)
                cubicTo(c.x + s * 0.95f, c.y - s * 1.05f, c.x + s * 1.5f, c.y - s * 0.05f, c.x, c.y + s * 0.95f)
                close()
            },
            ink,
        )
        else -> {
            val moon = Path.combine(
                PathOperation.Difference,
                Path().apply { addOval(Rect(Offset(c.x - s * 0.9f, c.y - s * 0.9f), Size(s * 1.8f, s * 1.8f))) },
                Path().apply { addOval(Rect(Offset(c.x - s * 0.9f + s * 0.75f, c.y - s * 0.9f - s * 0.1f), Size(s * 1.6f, s * 1.6f))) },
            )
            // The crescent is lopsided: move it a little to the left so that it looks centred.
            translate(left = -s * 0.2f) { drawPath(moon, ink) }
        }
    }
}

/**
 * A colour with its shape on it (the shape is drawn, not typed, so it is exactly in the middle), or an empty place (a ring). A [mark] puts
 * a ring around the peg, the way a letter of the word puzzle is coloured: a solid green one for the right colour in the right place, a
 * dashed green one for the right colour in the wrong place, a grey one for a colour the code has no more of. The pattern (solid or dashed)
 * and the green against the grey tell them apart, so the colour of the ring alone is not what carries it. [size] is the whole thing,
 * ring included. With [ringHidden] the peg is as small as a scored one but its ring is not drawn yet (the card of the rules shows the
 * rings arriving).
 */
@Composable
internal fun Peg(color: Int?, size: Dp, modifier: Modifier = Modifier, mark: CodeMark? = null, ringHidden: Boolean = false) {
    val colors = BrainScrollTheme.colors
    val border = colors.borderStrong.copy(alpha = 0.6f)
    val green = colors.correctFill
    val grey = colors.textSecondary.copy(alpha = 0.75f)
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val inner = r * (if (mark == null) 1f else 0.8f)
        if (color == null) {
            drawCircle(border, inner - 1.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        } else {
            drawCircle(CodeColors[color.mod(CodeColors.size)], inner, center)
            drawCodeSymbol(color, center, inner * 0.5f, Color.White)
        }
        val ring = 3.dp.toPx()
        when (if (ringHidden) null else mark) {
            CodeMark.Exact -> drawCircle(green, r - ring / 2f, center, style = Stroke(ring))
            CodeMark.Absent -> drawCircle(grey, r - ring / 2f, center, style = Stroke(ring))
            CodeMark.Present -> {
                val circumference = (2 * PI * (r - ring / 2f)).toFloat()
                val dash = circumference / 12f
                drawCircle(
                    green, r - ring / 2f, center,
                    style = Stroke(ring, cap = StrokeCap.Butt, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash * 0.62f, dash * 0.38f))),
                )
            }
            else -> Unit
        }
    }
}

/**
 * One full-screen page of the feed: the tries so far (the list scrolls, the newest is kept in view) ending in the row being filled, and
 * under it the colours to pick from and the delete and check keys (the result takes their place when the code is cracked). The tries have
 * no limit.
 */
@Composable
fun CodeBreakerPage(
    page: CodeSnapshot,
    onSubmit: (List<Int>) -> CodeResult,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val reduce = LocalReduceMotion.current
    val state = page.state
    val puzzle = state.puzzle
    val length = puzzle.length
    val won = state.status == CodeStatus.Won

    // The result and the buzz wait a moment so the last guess can be seen with its rings.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    val input = remember(page.id) { mutableStateListOf<Int>() }

    // The newest try stays in view: the list goes to its end when a guess is added.
    // It also starts there, with no animation: a page that is built again (the player came back to it) must not begin at the top of a
    // long list, because then a swipe up to the next game would first scroll the list through all of its tries.
    val rows = state.guesses.size + 1
    val lastItem = rows + (if (won) 0 else 1) - 1
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = lastItem)
    var listShown by remember(page.id) { mutableStateOf(false) }
    LaunchedEffect(state.guesses.size, won) {
        if (reduce || !listShown) listState.scrollToItem(lastItem) else listState.animateScrollToItem(lastItem)
        listShown = true
    }

    val names = SymbolNames.map { stringResource(it) }
    val emptyText = stringResource(Res.string.a11y_codebreaker_empty)
    val boardDescription = stringResource(Res.string.a11y_codebreaker_board, state.tries)
    val hint = stringResource(Res.string.codebreaker_hint)

    fun check() {
        if (won || input.size != length) return
        val result = onSubmit(input.toList())
        if (result != CodeResult.Ignored) input.clear()
        if (result == CodeResult.Guessed) haptics.perform(HapticKind.Medium)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(text = stringResource(Res.string.codebreaker_try, state.tries + (if (won) 0 else 1)), textColor = colors.textSecondary)
        Spacer(Modifier.height(Spacing.sm))

        LazyColumn(
            state = listState,
            // A list that fits is not a scroll area: it must not take the swipe that goes to the next game (only a long one scrolls).
            userScrollEnabled = listState.canScrollForward || listState.canScrollBackward,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("codeBoard")
                .semantics {
                    contentDescription = boardDescription
                    triesCount = state.tries
                    inputColors = input.toList()
                    secretColors = puzzle.secret
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "hint") {
                BasicText(hint, style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center), modifier = Modifier.padding(horizontal = Spacing.md))
            }
            itemsIndexed(state.guesses, key = { i, _ -> "g$i" }) { i, guess ->
                val number = state.tries - state.guesses.size + i + 1
                val markNames = mapOf(
                    CodeMark.Exact to stringResource(Res.string.a11y_codebreaker_exact),
                    CodeMark.Present to stringResource(Res.string.a11y_codebreaker_present),
                    CodeMark.Absent to stringResource(Res.string.a11y_codebreaker_absent),
                )
                val spoken = stringResource(
                    Res.string.a11y_codebreaker_guess, number,
                    guess.colors.indices.joinToString("; ") { k -> names[guess.colors[k]] + ", " + markNames.getValue(guess.feedback.marks[k]) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md).clearAndSetSemantics {
                        contentDescription = spoken
                        // The newest try is read out when it appears, so a screen reader hears what it scored.
                        if (i == state.guesses.lastIndex) liveRegion = LiveRegionMode.Polite
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BasicText(number.toString(), modifier = Modifier.size(24.dp), style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.End))
                    guess.colors.forEachIndexed { k, c -> Peg(c, PegSize, mark = guess.feedback.marks[k]) }
                }
            }
            if (!won) {
                item(key = "current") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BasicText((state.tries + 1).toString(), modifier = Modifier.size(24.dp), style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.End))
                        for (i in 0 until length) {
                            val color = input.getOrNull(i)
                            val label = stringResource(Res.string.a11y_codebreaker_slot, i + 1, color?.let { names[it] } ?: emptyText)
                            Box(
                                modifier = Modifier
                                    .testTag("codeSlot$i")
                                    .clip(CircleShape)
                                    .clickable(enabled = color != null, role = Role.Button) {
                                        haptics.perform(HapticKind.Light)
                                        input.removeAt(i)
                                    }
                                    .semantics { contentDescription = label },
                            ) { Peg(color, PegSize) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                CodeBreakerResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                val backspace = stringResource(Res.string.a11y_key_backspace)
                val checkLabel = stringResource(Res.string.a11y_codebreaker_check)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KeyGap), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Up to six colours in a row; eight go in two rows of four, so every colour stays big enough to hit.
                    val perRow = if (puzzle.colors > 6) (puzzle.colors + 1) / 2 else puzzle.colors
                    for (rowColors in (0 until puzzle.colors).chunked(perRow)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                            for (c in rowColors) {
                                val taken = !puzzle.repeats && c in input
                                val enabled = !taken && input.size < length
                                val label = stringResource(Res.string.a11y_codebreaker_pick, c + 1, names[c])
                                Box(
                                    modifier = Modifier
                                        .testTag("codeColor$c")
                                        .alpha(if (enabled) 1f else 0.3f)
                                        .clip(CircleShape)
                                        .clickable(enabled = enabled, role = Role.Button) {
                                            haptics.perform(HapticKind.Light)
                                            input.add(c)
                                        }
                                        .semantics { contentDescription = label },
                                ) { Peg(c, MaxPeg) }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(KeyGap), modifier = Modifier.fillMaxWidth()) {
                        Key("⌫", backspace, "codeBackspace", 2f, colors.error, colors.textPrimary, enabled = input.isNotEmpty()) {
                            input.removeAt(input.lastIndex)
                        }
                        Key("✓", checkLabel, "codeCheck", 3f, colors.correctFill, colors.correctOn, enabled = input.size == length) { check() }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun RowScope.Key(label: String, description: String, tag: String, weight: Float, fill: Color, content: Color, enabled: Boolean, onClick: () -> Unit) {
    val haptics = LocalHaptics.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .weight(weight)
            .height(KeyHeight)
            .testTag(tag)
            .alpha(if (enabled) 1f else 0.45f)
            .background(fill, shape)
            .clickable(enabled = enabled, role = Role.Button) {
                haptics.perform(HapticKind.Light)
                onClick()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = BrainScrollTheme.typography.label.copy(color = content, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun CodeBreakerResultCard(page: CodeSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("codeResultCard").resultSurface(colors.bgSurface, GameKind.CodeBreaker.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.codebreaker_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.codebreaker_result, page.state.tries, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(page.state.tries.toString(), stringResource(Res.string.result_stat_guesses), GameKind.CodeBreaker.world),
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.CodeBreaker.world),
                ),
            )
            BasicText(
                text = swipeHintText(),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** A small picture for the thumbnails: rows of coloured pegs, some with their rings. */
@Composable
internal fun CodeBreakerThumbArt(side: Dp) {
    val peg = side * 0.2f
    val secret = listOf(0, 3, 2, 1)
    Column(Modifier.size(side * 0.9f), verticalArrangement = Arrangement.spacedBy(side * 0.04f, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
        for (row in listOf(listOf(2, 0, 4, 3), listOf(0, 3, 1, 5), listOf(0, 3, 2, 1))) {
            val marks = CodePuzzle.feedback(secret, row).marks
            Row(horizontalArrangement = Arrangement.spacedBy(side * 0.03f), verticalAlignment = Alignment.CenterVertically) {
                row.forEachIndexed { i, c -> Peg(c, peg, mark = marks[i]) }
            }
        }
    }
}
