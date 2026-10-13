package com.mehmtcan.brainscroll.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import kotlin.math.abs
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

/**
 * A short message in a card like the ones of the profile (same corners and surface) with a green outline, so it is seen:
 * hints and results ("Not in word list", "Signed in"). [detail] is an optional second line in small print, for example
 * an error code. It comes in with a short rise and fade (not when motion is reduced). With [onDismiss] it has an x and
 * can also be swiped away (sideways or down).
 */
@Composable
fun NoticePill(text: String, modifier: Modifier = Modifier, detail: String? = null, onDismiss: (() -> Unit)? = null) {
    val colors = BrainScrollTheme.colors
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { if (progress.value < 1f) progress.animateTo(1f, tween(Motion.BASE, easing = Motion.Standard)) }
    val density = LocalDensity.current
    // Where the finger has dragged the card; it follows the finger and either goes away or springs back.
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val swipe = if (onDismiss == null) Modifier else Modifier.pointerInput(onDismiss) {
        val away = 72.dp.toPx()
        val down = 36.dp.toPx()
        detectDragGestures(
            onDragEnd = {
                if (abs(dragX) > away || dragY > down) onDismiss()
                dragX = 0f
                dragY = 0f
            },
            onDragCancel = {
                dragX = 0f
                dragY = 0f
            },
        ) { change, drag ->
            change.consume()
            dragX += drag.x
            dragY = (dragY + drag.y).coerceAtLeast(0f)
        }
    }
    Row(
        // A screen reader reads a message when it appears, without the player having to look for it.
        modifier = modifier
            .semantics(mergeDescendants = false) { liveRegion = LiveRegionMode.Polite }
            .graphicsLayer {
                alpha = progress.value * (1f - (abs(dragX) / 600f).coerceAtMost(0.6f))
                translationX = dragX
                translationY = (1f - progress.value) * with(density) { 12.dp.toPx() } + dragY
            }
            .then(swipe)
            .background(colors.bgSurface, Radius.card)
            .border(1.5.dp, colors.correctFill, Radius.card)
            .padding(start = Spacing.lg, end = if (onDismiss != null) 0.dp else Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(vertical = Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(text = text, style = BrainScrollTheme.typography.heading.copy(color = colors.textPrimary, textAlign = TextAlign.Center))
            if (detail != null) {
                BasicText(text = detail, style = BrainScrollTheme.typography.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center))
            }
        }
        if (onDismiss != null) CloseButton(onDismiss, tag = "noticeClose")
    }
}
