package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.game.guide.GuideStep
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The step the guide gave for the page on screen, or null. The game pages read it to point at their cells. */
val LocalGuide = compositionLocalOf<GuideStep?> { null }

/** True if the guide points at [index] (a cell, a card, a tube, ...) of the page. */
@Composable
fun isGuided(index: Int): Boolean = LocalGuide.current?.cells?.contains(index) == true

/**
 * A pulsing ring around a thing the guide points at, in the color of the text so it shows on every board and in both
 * themes. The ring does not move when motion is reduced. Without [on] the modifier does nothing.
 */
@Composable
fun Modifier.guideHighlight(on: Boolean, shape: Shape): Modifier {
    if (!on) return this
    val color = BrainScrollTheme.colors.textPrimary
    val reduce = LocalReduceMotion.current
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
    )
    val alpha = if (reduce) 1f else pulse
    return this.testTag("guideMark").drawWithContent {
        drawContent()
        val width = 3.dp.toPx()
        inset(width / 2) {
            drawOutline(shape.createOutline(size, layoutDirection, this), color.copy(alpha = alpha), style = Stroke(width))
        }
    }
}

/**
 * Wraps one cell so the guide can ring it without the cell knowing. The wrapper is always there, so the cell keeps its
 * state (an animation in progress) when the ring appears or goes.
 */
@Composable
fun GuideMark(on: Boolean, shape: Shape, content: @Composable () -> Unit) {
    Box(Modifier.guideHighlight(on, shape)) { content() }
}
