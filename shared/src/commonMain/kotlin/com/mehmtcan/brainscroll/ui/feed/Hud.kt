package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import brainscroll.shared.generated.resources.tab_home
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.howto.HowToIconButton
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_answer_streak
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.ChevronButton
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * The bar above the feed: the way back, the name of the game on the page and the "?" that opens its rules, and on the
 * right the number of puzzles solved in this feed (tap for what it means). With word puzzles in more than one language, [badge] says which one
 * the page is in.
 */
@Composable
fun Hud(
    title: String,
    badge: String?,
    /** Puzzles solved without the guide in this feed. */
    solvedCount: Int,
    /** Back to the home page. */
    onHome: () -> Unit,
    onHowToPlay: () -> Unit,
    onStreak: () -> Unit,
    modifier: Modifier = Modifier,
    /** Asks the guide for the next move; null when the puzzle is over and there is nothing to ask. */
    onGuide: (() -> Unit)? = null,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(
        modifier = modifier.fillMaxWidth().height(48.dp).padding(start = Spacing.sm, end = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChevronButton(stringResource(Res.string.tab_home), colors.textPrimary, onHome, touch = 44.dp)
        // The title (with the language under it when there is one) and the "?" take what the right side leaves, so the
        // light bulb and the win streak are never pushed off the screen; a long title is cut short instead.
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f, fill = false).padding(start = Spacing.xs)) {
                BasicText(
                    text = title,
                    style = type.heading.copy(color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }.testTag("feedTitle"),
                )
                if (badge != null) {
                    BasicText(
                        text = badge,
                        style = type.caption.copy(color = colors.textSecondary),
                        modifier = Modifier.testTag("languageBadge"),
                    )
                }
            }
            HowToIconButton(onHowToPlay)
        }
        // The light bulb is in the bar, next to the win streak, so it is never over the puzzle.
        if (onGuide != null) GuideIconButton(onGuide)
        val celebration = rememberStreakCelebration(solvedCount)
        Box(Modifier.testTag("winStreak")) {
            Box(Modifier.graphicsLayer { scaleX = celebration.scale; scaleY = celebration.scale }) {
                Chip(
                    text = celebration.shown.toString(),
                    textColor = colors.streakAnswer,
                    description = stringResource(Res.string.a11y_answer_streak, solvedCount),
                    onClick = onStreak,
                ) { BoltIcon(colors.streakAnswer) }
            }
            // The "+1" that rises over the number before it changes.
            if (celebration.plusAlpha > 0f) {
                BasicText(
                    text = "+1",
                    style = type.title.copy(color = colors.streakAnswer),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = celebration.plusLift)
                        .graphicsLayer { alpha = celebration.plusAlpha },
                )
            }
        }
    }
}

/** What the win streak chip shows while its number goes up: the number on show, the "+1" over it and the pop of the chip. */
private class StreakCelebration(val shown: Int, val plusAlpha: Float, val plusLift: Dp, val scale: Float)

/**
 * When the number of solved puzzles goes up (a puzzle solved without the guide) the number does not just change: a "+1" rises over the
 * old number, then the number turns into the new one with a pop. Anything else (a first value, a reset, reduced motion)
 * shows the new number at once.
 */
@Composable
private fun rememberStreakCelebration(value: Int): StreakCelebration {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableIntStateOf(value) }
    val plus = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(value) {
        if (value <= shown || reduce) {
            shown = value
            return@LaunchedEffect
        }
        plus.snapTo(0f)
        plus.animateTo(0.6f, tween(350, easing = Motion.Standard))
        shown = value
        coroutineScope {
            launch { plus.animateTo(1f, tween(300, easing = Motion.Standard)) }
            scale.snapTo(1f)
            scale.animateTo(1.3f, tween(Motion.FAST, easing = Motion.Standard))
            scale.animateTo(1f, tween(Motion.POP, easing = Motion.Overshoot))
        }
        plus.snapTo(0f)
    }
    val p = plus.value
    val alpha = if (p <= 0.6f) p / 0.6f else (1f - p) / 0.4f
    return StreakCelebration(shown, alpha.coerceIn(0f, 1f), (-22f * p).dp, scale.value)
}

/** Small lightning bolt for the answer streak, drawn as a path so it follows the token color. */
@Composable
internal fun BoltIcon(color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0.60f * w, 0f)
            lineTo(0.15f * w, 0.55f * h)
            lineTo(0.47f * w, 0.55f * h)
            lineTo(0.38f * w, h)
            lineTo(0.85f * w, 0.42f * h)
            lineTo(0.53f * w, 0.42f * h)
            close()
        }
        drawPath(path, color)
    }
}
