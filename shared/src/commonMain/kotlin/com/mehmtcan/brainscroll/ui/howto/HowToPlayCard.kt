package com.mehmtcan.brainscroll.ui.howto

import brainscroll.shared.generated.resources.howto_next
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import brainscroll.shared.generated.resources.hand_cursor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_howto_next
import brainscroll.shared.generated.resources.a11y_howto_previous
import brainscroll.shared.generated.resources.a11y_howto_step
import brainscroll.shared.generated.resources.howto_card_title
import brainscroll.shared.generated.resources.howto_ok
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.Overlay
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * One step of a How to Play card: a sentence or two, and the small picture over it. The picture is drawn for the time [t] of its
 * loop (0 up to 1, then it starts over): it shows the first frame of the step at 0, so it never starts empty. With reduced
 * motion [t] stays at [REDUCED_DEMO_TIME], a frame in which the step has already happened.
 */
class HowToStep(val caption: StringResource, val demo: @Composable (t: Float) -> Unit, val cycleMillis: Int = 3200)

/** The three steps of a game. */
class HowToCardContent(val steps: List<HowToStep>)

/** The card is round at all four corners, as much as the top of a sheet is. */
private val CardShape = RoundedCornerShape(20.dp)

/** The frame of a loop that is shown when motion is reduced: the thing has happened, the hand is still there. */
const val REDUCED_DEMO_TIME = 0.62f

/**
 * The How to Play card: a card in the middle of the screen over the dark page, with the name of the card on top, the picture of
 * the step with a hand that shows what to do, the sentence, the dots of the steps and the buttons: back, and Next (OK on the last step). Swiping
 * the card, the back and Next buttons change the step; OK (the last step), the dark area and the system back button close it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HowToPlayCard(content: HowToCardContent, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val reduce = LocalReduceMotion.current
    val steps = content.steps
    var step by remember { mutableIntStateOf(0) }
    val close by rememberUpdatedState(onClose)
    BackHandler(enabled = true) { close() }

    // The overlay is drawn where the window keeps its layers, not where the card is called, so what the caller provided (the
    // language of the word pictures) is read here and handed on inside.
    val wordLanguage = LocalHowToWordLanguage.current
    Overlay {
      androidx.compose.runtime.CompositionLocalProvider(LocalHowToWordLanguage provides wordLanguage) {
        Box(modifier = modifier.fillMaxSize().testTag("howToCard")) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
            )
            BoxWithConstraints(Modifier.align(Alignment.Center).padding(Spacing.lg)) {
                val width = minOf(maxWidth, 380.dp)
                // The picture is a square as wide as the card, but never taller than a share of the screen (a short screen with big text).
                val demoSide = minOf(width - Spacing.lg * 2, this@BoxWithConstraints.maxHeight.let { (it + Spacing.lg * 2) * 0.42f })
                Column(
                    modifier = Modifier
                        .width(width)
                        .background(colors.bgSurface, CardShape)
                        .border(1.dp, colors.borderSubtle, CardShape)
                        // Taps on the card stay on the card (they must not reach the dark area, which closes it).
                        .pointerInput(Unit) { detectTapGestures { } }
                        .pointerInput(steps.size) {
                            var drag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { drag = 0f },
                                onDragEnd = {
                                    if (drag < -60f && step < steps.lastIndex) step++
                                    if (drag > 60f && step > 0) step--
                                },
                            ) { _, amount -> drag += amount }
                        }
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    BasicText(
                        text = stringResource(Res.string.howto_card_title).uppercase(),
                        style = type.title.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
                        modifier = Modifier.semantics { heading() },
                    )
                    Crossfade(targetState = step, animationSpec = tween(if (reduce) 0 else Motion.BASE), label = "howToStep") { index ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            // The picture: it plays over and over; a new step starts it again from its first frame.
                            val t = rememberDemoTime(steps[index].cycleMillis)
                            Box(
                                Modifier
                                    .size(demoSide)
                                    .clip(Radius.card)
                                    .background(colors.bgPage)
                                    .testTag("howToDemo"),
                            ) { steps[index].demo(t) }
                            Box(Modifier.heightIn(min = 72.dp), contentAlignment = Alignment.Center) {
                                BasicText(
                                    text = stringResource(steps[index].caption),
                                    style = type.body.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("howToCaption"),
                                )
                            }
                        }
                    }
                    Dots(count = steps.size, selected = step)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        StepButton(left = true, enabled = step > 0, description = stringResource(Res.string.a11y_howto_previous), tag = "howToPrevious") { step-- }
                        // One main button: "Next" on every step but the last, where it says "OK" and closes the card.
                        val last = step >= steps.lastIndex
                        Box(Modifier.weight(1f)) {
                            AppButton(
                                text = stringResource(if (last) Res.string.howto_ok else Res.string.howto_next),
                                onClick = { if (last) close() else step++ },
                                primary = true,
                                modifier = Modifier.testTag("howToOk"),
                            )
                        }
                    }
                }
            }
        }
      }
    }
}

/** The dots of the steps: the one you are on is filled with the color of the game's highlight. */
@Composable
private fun Dots(count: Int, selected: Int) {
    val colors = BrainScrollTheme.colors
    val description = stringResource(Res.string.a11y_howto_step, selected + 1, count)
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = description }.testTag("howToDots"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) { i ->
            Box(Modifier.size(10.dp).background(if (i == selected) colors.pendingFill else colors.bgRaised, CircleShape))
        }
    }
}

/** The square button with an arrow to the left or right. Dimmed and not tappable when there is no step that way. */
@Composable
private fun StepButton(left: Boolean, enabled: Boolean, description: String, tag: String, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    Box(
        modifier = Modifier
            .testTag(tag)
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(Radius.card)
            .background(colors.bgRaised)
            .semantics {
                contentDescription = description
                if (!enabled) disabled()
            }
            .then(
                if (enabled) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val color = colors.textPrimary
        Canvas(Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val dir = if (left) -1f else 1f
            val mid = w / 2f
            val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val path = Path().apply {
                moveTo(mid - dir * w * 0.1f - dir * w * 0.2f, h * 0.2f)
                lineTo(mid + dir * w * 0.3f, h * 0.5f)
                lineTo(mid - dir * w * 0.1f - dir * w * 0.2f, h * 0.8f)
            }
            drawPath(path, color, style = stroke)
        }
    }
}

/**
 * The time of the loop of a picture, 0 up to 1 and then again, [cycleMillis] for one round. Under reduced motion it stays at
 * [REDUCED_DEMO_TIME] and nothing moves.
 */
@Composable
fun rememberDemoTime(cycleMillis: Int): Float {
    if (LocalReduceMotion.current) return REDUCED_DEMO_TIME
    val transition = rememberInfiniteTransition(label = "howToDemo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(cycleMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "howToTime",
    )
    return t
}

/** 0 before [from], 1 after [to], and in between how far [t] is. */
fun demoSegment(t: Float, from: Float, to: Float): Float = if (to <= from) (if (t >= to) 1f else 0f) else ((t - from) / (to - from)).coerceIn(0f, 1f)

/** Starts slowly and ends slowly. */
fun demoEase(x: Float): Float = x * x * (3f - 2f * x)

/** The picture comes up from a dim first frame (never empty) and fades out at the end of its loop, so the start over is not a jump. */
fun demoFade(t: Float): Float = minOf(0.35f + 0.65f * (t / 0.05f).coerceIn(0f, 1f), ((1f - t) / 0.06f).coerceIn(0f, 1f))

/** The language of the word puzzle the card is opened from: the picture of the word card uses its letters and its keyboard. */
val LocalHowToWordLanguage = androidx.compose.runtime.compositionLocalOf { com.mehmtcan.brainscroll.game.wordle.Language.EN }

/** One tap of the hand in a picture: the time of the loop it lands at and the place it taps. */
class DemoTap(val at: Float, val pos: Offset)

/** Where the hand is at time [t] and how far its tap has gone (0 to 1, see [drawHandCursor]): it comes to each tap in turn. */
fun demoHand(t: Float, taps: List<DemoTap>, start: Offset, travel: Float = 0.07f, pressLength: Float = 0.06f): Pair<Offset, Float> {
    var from = start
    var pos = start
    var press = 0f
    for (tap in taps) {
        if (t < tap.at - travel) break
        val arrive = demoEase(demoSegment(t, tap.at - travel, tap.at))
        pos = Offset(from.x + (tap.pos.x - from.x) * arrive, from.y + (tap.pos.y - from.y) * arrive)
        press = demoSegment(t, tap.at, tap.at + pressLength)
        from = tap.pos
    }
    return pos to press
}

/** The size of something that pops in at [at]: 0 before, then it grows past its size and settles (the pop of a tent or a queen). */
fun demoPop(t: Float, at: Float, length: Float = 0.07f): Float = if (t < at) 0f else Motion.Overshoot.transform(demoSegment(t, at, at + length))

/** The picture of the hand (drawable/hand_cursor.xml): a pointing hand with the fingertip at [HAND_TIP_X], [HAND_TIP_Y] of it. */
@Composable
fun rememberHandPainter(): Painter = painterResource(Res.drawable.hand_cursor)

private const val HAND_TIP_X = 0.2f
private const val HAND_TIP_Y = 0.02f

/**
 * The hand that shows where to tap: the picture of a pointing hand whose fingertip is on [tip]. [press] (0 to 1) pushes it
 * down a little, as a tap does, and draws a ring that spreads from the fingertip. [unit] is about the size of a board cell;
 * the hand is a bit bigger than that.
 */
fun DrawScope.drawHandCursor(painter: Painter, tip: Offset, unit: Float, press: Float, alpha: Float = 1f) {
    if (press > 0f && press < 1f) {
        drawCircle(Color.White.copy(alpha = 0.7f * (1f - press) * alpha), radius = unit * (0.15f + 0.55f * press), center = tip, style = Stroke(width = unit * 0.06f))
    }
    val push = if (press in 0.0001f..0.9999f) 1f - 0.08f * kotlin.math.sin(press * 3.1416f) else 1f
    val side = unit * 1.5f
    scale(push, push, pivot = tip) {
        translate(tip.x - HAND_TIP_X * side, tip.y - HAND_TIP_Y * side) {
            with(painter) { draw(Size(side, side), alpha) }
        }
    }
}
