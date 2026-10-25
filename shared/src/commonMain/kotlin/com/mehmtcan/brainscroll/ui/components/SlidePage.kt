package com.mehmtcan.brainscroll.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * A page over the whole window that slides up from the bottom edge and, when it closes, slides back down. Like [ModalSheet] it
 * is drawn in the overlay layer, closes with the system back button, and calls [onDismiss] once it is gone; unlike the sheet it
 * covers everything (no dark area to tap), keeps clear of the system bars, and has [content] lay out the whole screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SlidePage(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "page",
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    Overlay {
        val colors = BrainScrollTheme.colors
        val reduce = LocalReduceMotion.current
        val progress = remember { Animatable(if (reduce) 1f else 0f) }
        val scope = rememberCoroutineScope()
        var closing by remember { mutableStateOf(false) }
        val dismiss by rememberUpdatedState(onDismiss)
        LaunchedEffect(Unit) { if (progress.value < 1f) progress.animateTo(1f, tween(Motion.SLOW, easing = Motion.Standard)) }
        val close: () -> Unit = {
            if (!closing) {
                closing = true
                scope.launch {
                    if (!reduce) progress.animateTo(0f, tween(Motion.BASE, easing = Motion.Standard))
                    dismiss()
                }
            }
        }
        BackHandler(enabled = true, onBack = close)

        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag(tag)
                .graphicsLayer { translationY = (1f - progress.value) * size.height }
                .background(colors.bgPage)
                // The page is opaque and keeps touches away from what is under it.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) { content(close) }
    }
}
