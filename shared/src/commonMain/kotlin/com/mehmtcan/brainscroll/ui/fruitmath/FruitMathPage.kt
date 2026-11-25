package com.mehmtcan.brainscroll.ui.fruitmath

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import brainscroll.shared.generated.resources.a11y_fruitmath_board
import brainscroll.shared.generated.resources.a11y_fruitmath_divided
import brainscroll.shared.generated.resources.a11y_fruitmath_times
import brainscroll.shared.generated.resources.a11y_fruitmath_plus
import brainscroll.shared.generated.resources.a11y_fruitmath_equals
import brainscroll.shared.generated.resources.a11y_fruitmath_power
import brainscroll.shared.generated.resources.a11y_fruitmath_squared
import brainscroll.shared.generated.resources.a11y_key_backspace
import brainscroll.shared.generated.resources.a11y_key_enter
import brainscroll.shared.generated.resources.fruitmath_result
import brainscroll.shared.generated.resources.fruitmath_won
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.fruitmath.FruitLine
import com.mehmtcan.brainscroll.game.fruitmath.FruitResult
import com.mehmtcan.brainscroll.game.fruitmath.FruitSnapshot
import com.mehmtcan.brainscroll.game.fruitmath.FruitStatus
import com.mehmtcan.brainscroll.game.fruitmath.FruitToken
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.feed.LocalGuide
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.theme.GuideColor
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** The text of fruit [index] (a number from 0 to 7). */
private val Emoji = listOf("🍎", "🍌", "🍇", "🍒", "🍍", "🥝", "🍋", "🍉")

internal fun fruitEmoji(index: Int): String = Emoji[index.mod(Emoji.size)]

/** Height reserved for the bottom area (the keypad, or the result in its place), the same as the other games, so the board does not jump between them. */
/* At least this high; a result card that needs more (big text) makes it higher and the board smaller. */
private val BottomAreaHeight = 152.dp
private val MaxBoard = 420.dp
private val KeyHeight = 44.dp
private val KeyGap = 6.dp

/** How far (in dp) the board goes to one side and the other, one after the other, and how long each move takes. */
private val SHAKE_STEPS = listOf(-12f, 10f, -8f, 6f, -3f, 0f)
private const val SHAKE_STEP_MILLIS = 55

/** How many digits an answer can have. */
private const val MAX_DIGITS = 3

/** What is typed so far, the wrong answers given and the answer of the last line, for tests. */
internal val FruitInputKey = SemanticsPropertyKey<String>("FruitInput")
private var SemanticsPropertyReceiver.inputText by FruitInputKey
internal val FruitMistakesKey = SemanticsPropertyKey<Int>("FruitMistakes")
private var SemanticsPropertyReceiver.mistakeCount by FruitMistakesKey
internal val FruitAnswerKey = SemanticsPropertyKey<Int>("FruitAnswer")
private var SemanticsPropertyReceiver.answerValue by FruitAnswerKey
/** How far (in dp) the board is off to the side while it shakes. */
internal val FruitShakeKey = SemanticsPropertyKey<Float>("FruitShake")
private var SemanticsPropertyReceiver.shakeOffset by FruitShakeKey

/** The biggest width the pieces of a line take, in the unit of its text size (a fruit is a little wider than it is high). */
private fun lineUnits(line: FruitLine): Float {
    var units = 0f
    for (token in line.tokens) {
        units += when (token) {
            is FruitToken.Fruit -> 1.3f + (if (token.squared) 0.6f else 0f)
            is FruitToken.Op -> 0.95f
            is FruitToken.Power -> 1.3f + 0.9f
        }
    }
    return units
}

/** The sizes of a board's text, in dp: one size for the fruits of every line (the one the longest line needs), and a size for the numbers. */
internal class FruitSizes(val symbol: Float, val number: Float, val numberWidth: Dp)

/**
 * The sizes for [lines] in rows [rowHeight] high on a card [panelWidth] wide. Every line gets the same size of fruit, the biggest that the
 * longest line fits in, so the last line is no smaller than the others; the numbers have a size of their own (they never shrink with the
 * fruits) and room for three digits.
 */
internal fun fruitSizes(lines: List<FruitLine>, rowHeight: Dp, panelWidth: Dp): FruitSizes {
    val number = rowHeight.value * 0.55f
    val numberWidth = (number * 2.0f).dp
    val room = panelWidth - numberWidth - rowHeight * 0.45f
    val fit = lines.minOf { room.value / lineUnits(it) }
    return FruitSizes(minOf(rowHeight.value * 0.6f, fit).coerceAtLeast(10f), number, numberWidth)
}

/** One line of the board: the fruits and the signs between them, an equals sign and a number (or a question mark). */
@Composable
internal fun FruitRow(line: FruitLine, number: String, numberColor: Color, sizes: FruitSizes, rowHeight: Dp, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    // The board is drawn to fit its card: its text does not grow with the size of text the player chose (a screen that is already full would break).
    val density = LocalDensity.current
    fun fixed(size: Float) = with(density) { size.dp.toSp() }
    val size = sizes.symbol
    val spoken = fruitLineText(line, number)
    Row(modifier = modifier.fillMaxWidth().height(rowHeight).clearAndSetSemantics { contentDescription = spoken }, verticalAlignment = Alignment.CenterVertically) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((size * 0.1f).dp)) {
            for (token in line.tokens) when (token) {
                is FruitToken.Fruit -> {
                    BasicText(fruitEmoji(token.index), style = TextStyle(fontSize = fixed(size)))
                    if (token.squared) BasicText("²", style = TextStyle(fontSize = fixed(size * 0.75f), fontWeight = FontWeight.Bold, color = colors.textPrimary))
                }
                is FruitToken.Op -> BasicText(token.symbol.toString(), style = TextStyle(fontSize = fixed(size * 0.8f), fontWeight = FontWeight.Bold, color = colors.textSecondary))
                // A power: the base as big as any fruit and the exponent small, up and to the right of it.
                is FruitToken.Power -> Row(verticalAlignment = Alignment.Top) {
                    BasicText(fruitEmoji(token.base), style = TextStyle(fontSize = fixed(size)))
                    BasicText(fruitEmoji(token.exponent), modifier = Modifier.offset(y = (-size * 0.18f).dp), style = TextStyle(fontSize = fixed(size * 0.62f)))
                }
            }
        }
        BasicText("=", style = TextStyle(fontSize = fixed(sizes.number * 0.75f), fontWeight = FontWeight.Bold, color = colors.textSecondary))
        Box(modifier = Modifier.width(sizes.numberWidth), contentAlignment = Alignment.CenterEnd) {
            BasicText(number, style = TextStyle(fontSize = fixed(sizes.number), fontWeight = FontWeight.Black, color = numberColor, textAlign = TextAlign.End), maxLines = 1)
        }
    }
}

/**
 * One full-screen page of the feed: the lines of the board in a card, with what is typed in place of the question mark, and under it a
 * keypad (the result takes its place when the answer is right). A wrong answer flashes red and is cleared; the player may try again.
 */
@Composable
fun FruitMathPage(
    page: FruitSnapshot,
    onSubmit: (Int) -> FruitResult,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val haptics = LocalHaptics.current
    val state = page.state
    val puzzle = state.puzzle
    val won = state.status == FruitStatus.Won

    // The result and the buzz wait a moment so the right answer can be seen where the question mark was.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    var input by remember(page.id) { mutableStateOf("") }
    var wrongKey by remember(page.id) { mutableIntStateOf(0) }
    var wrongFlash by remember(page.id) { mutableStateOf(false) }
    LaunchedEffect(wrongKey) {
        if (wrongKey > 0) {
            wrongFlash = true
            delay(550)
            wrongFlash = false
        }
    }

    // A wrong answer shakes the board a little, the way a head shakes "no" (not with reduced motion: the red number says it).
    val reduce = LocalReduceMotion.current
    val shake = remember(page.id) { Animatable(0f) }
    // Read here (not only where it is drawn) so that what a screen reader or a test sees follows the shake; it lasts a third of a second.
    val shakeNow = shake.value
    LaunchedEffect(wrongKey) {
        if (wrongKey > 0 && !reduce) {
            for (target in SHAKE_STEPS) shake.animateTo(target, tween(SHAKE_STEP_MILLIS, easing = LinearEasing))
        }
    }

    val guide = LocalGuide.current?.takeIf { it.reason == GuideReason.FruitValues }
    val pulse by rememberInfiniteTransition(label = "fruitGuide").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    fun enter() {
        val answer = input.toIntOrNull() ?: return
        when (onSubmit(answer)) {
            FruitResult.Wrong -> {
                haptics.perform(HapticKind.Warning)
                wrongKey++
                input = ""
            }
            FruitResult.Solved -> input = ""
            FruitResult.Ignored -> Unit
        }
    }

    val description = stringResource(Res.string.a11y_fruitmath_board)
    // Whatever does not fit its page is cut off, never drawn over the page next to it.
    Column(
        modifier = modifier.fillMaxSize().clipToBounds().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val rows = puzzle.fruitCount + 1
            val width = minOf(MaxBoard, maxWidth)
            val inner = 14.dp
            val rowHeight = minOf(60.dp, (maxHeight - inner * 2) / rows).coerceAtLeast(24.dp)
            val shape = RoundedCornerShape(20.dp)
            Column(
                modifier = Modifier
                    .width(width)
                    .graphicsLayer { translationX = shake.value.dp.toPx() }
                    .testTag("fruitBoard")
                    .semantics {
                        contentDescription = description
                        inputText = input
                        mistakeCount = state.mistakes
                        answerValue = puzzle.answer
                        shakeOffset = shakeNow
                    }
                    .background(colors.bgSurface, shape)
                    .border(BorderStroke(1.5.dp, colors.borderStrong.copy(alpha = 0.35f)), shape)
                    .padding(horizontal = inner, vertical = inner),
            ) {
                val sizes = fruitSizes(puzzle.lines + puzzle.last, rowHeight, width - inner * 2)
                for (line in puzzle.lines) FruitRow(line, line.total.toString(), colors.textPrimary, sizes, rowHeight)
                val shown = when {
                    won -> puzzle.answer.toString()
                    input.isEmpty() -> "?"
                    else -> input
                }
                val shownColor = when {
                    won -> colors.correctFill
                    wrongFlash -> colors.error
                    else -> colors.streakAnswer
                }
                // The guide rings the last line: the values it gave are for working that one out.
                val ring = if (guide != null && !won) Modifier.border(BorderStroke(2.5.dp, GuideColor.copy(alpha = 0.35f + 0.6f * pulse)), RoundedCornerShape(10.dp)) else Modifier
                FruitRow(puzzle.last, shown, shownColor, sizes, rowHeight, ring)
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                FruitResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                NumberKeypad(
                    onDigit = { digit -> if (!won && input.length < MAX_DIGITS) input = if (input == "0") digit.toString() else input + digit },
                    onBackspace = { if (!won) input = input.dropLast(1) },
                    onEnter = { if (!won) enter() },
                    canEnter = input.isNotEmpty() && !won,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** The keys 1 to 5, 6 to 0, and below them delete (red) and enter (the green of the main button), the way the word puzzle's keyboard looks. */
@Composable
private fun NumberKeypad(onDigit: (Int) -> Unit, onBackspace: () -> Unit, onEnter: () -> Unit, canEnter: Boolean, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val backspaceDescription = stringResource(Res.string.a11y_key_backspace)
    val enterDescription = stringResource(Res.string.a11y_key_enter)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KeyGap)) {
        for (digits in listOf(listOf(1, 2, 3, 4, 5), listOf(6, 7, 8, 9, 0))) {
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                for (d in digits) {
                    Key(d.toString(), d.toString(), "fruitKey$d", 1f, colors.bgRaised, colors.textPrimary) { onDigit(d) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
            Key("⌫", backspaceDescription, "fruitBackspace", 2f, colors.error, colors.textPrimary, onClick = onBackspace)
            Key("✓", enterDescription, "fruitEnter", 3f, colors.correctFill, colors.correctOn, enabled = canEnter, onClick = onEnter)
        }
    }
}

@Composable
private fun RowScope.Key(label: String, description: String, tag: String, weight: Float, fill: Color, content: Color, enabled: Boolean = true, onClick: () -> Unit) {
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
private fun FruitResultCard(page: FruitSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("fruitResultCard").resultSurface(colors.bgSurface, GameKind.FruitMath.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.fruitmath_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.fruitmath_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.FruitMath.world),
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

/** A line the way TalkBack says it: the fruits by name, the signs in words, then "equals" and the number (or the question). */
@Composable
internal fun fruitLineText(line: FruitLine, number: String): String {
    val plus = stringResource(Res.string.a11y_fruitmath_plus)
    val times = stringResource(Res.string.a11y_fruitmath_times)
    val divided = stringResource(Res.string.a11y_fruitmath_divided)
    val parts = line.tokens.map { token ->
        when (token) {
            is FruitToken.Fruit -> if (token.squared) stringResource(Res.string.a11y_fruitmath_squared, fruitEmoji(token.index)) else fruitEmoji(token.index)
            is FruitToken.Op -> when (token.symbol) {
                '+' -> plus
                '×' -> times
                '÷' -> divided
                else -> token.symbol.toString()
            }
            is FruitToken.Power -> stringResource(Res.string.a11y_fruitmath_power, fruitEmoji(token.base), fruitEmoji(token.exponent))
        }
    }
    return stringResource(Res.string.a11y_fruitmath_equals, parts.joinToString(" "), number)
}
