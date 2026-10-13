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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
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
 * Points at a thing with a thick white ring that runs along its own edge, on the inside: the outer edge of the ring is the
 * edge of the thing, with the same corners, so nothing is left between the two and nothing lies over the things around it.
 * The ring breathes (its strength rises and falls); it does not move or change the size of the thing, and it stays at full
 * strength when motion is reduced. Without [on] the modifier does nothing.
 */
@Composable
fun Modifier.guideHighlight(on: Boolean, shape: Shape): Modifier {
    if (!on) return this
    val ring = BrainScrollTheme.colors.textPrimary
    val reduce = LocalReduceMotion.current
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
    )
    val beat = if (reduce) 1f else pulse
    return this.testTag("guideMark").drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        val edge = Path().apply { addOutline(outline) }
        // A stroke twice as wide as the ring, along the edge, cut to the inside of the edge: what is left is a band of
        // the ring's width whose outer side has exactly the corners of the thing.
        clipPath(edge) {
            drawOutline(outline, ring.copy(alpha = 0.55f + 0.45f * beat), style = Stroke(width = 2 * RING_WIDTH.toPx()))
        }
    }
}

private val RING_WIDTH = 3.5.dp

/**
 * Wraps one cell so the guide can ring it without the cell knowing. The wrapper is always there, so the cell keeps its
 * state (an animation in progress) when the ring appears or goes.
 */
@Composable
fun GuideMark(on: Boolean, shape: Shape, content: @Composable () -> Unit) {
    Box(Modifier.guideHighlight(on, shape)) { content() }
}
