package com.mehmtcan.brainscroll.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.launch

/** How dark the page gets behind a sheet or a dialog. */
private const val SCRIM_ALPHA = 0.6f

/**
 * Where the sheets and dialogs are drawn: one layer over the whole window, above the page and the tab bar, and (with the
 * app drawn behind the system bars) the status bar and the navigation bar too. [AppShell] owns one; a screen asks for it
 * through [Overlay].
 */
class OverlayHost {
    internal val layers = mutableStateMapOf<Any, @Composable () -> Unit>()
}

val LocalOverlayHost = staticCompositionLocalOf<OverlayHost?> { null }

/** The place where the layers of [host] are shown. Put it last in the root box so it is above everything. */
@Composable
fun OverlayLayers(host: OverlayHost) {
    host.layers.entries.toList().forEach { (id, layer) -> key(id) { layer() } }
}

/**
 * Shows [content] in the overlay layer of the window instead of where it is called, so it covers the whole screen. With no
 * host (a screen shown by itself) it is drawn in place.
 */
@Composable
fun Overlay(content: @Composable () -> Unit) {
    val host = LocalOverlayHost.current
    if (host == null) {
        content()
        return
    }
    val key = remember { Any() }
    val latest by rememberUpdatedState(content)
    DisposableEffect(host, key) {
        host.layers[key] = { latest() }
        onDispose { host.layers.remove(key) }
    }
}

/**
 * A panel that slides up from the bottom, with the page darkening behind it. Tapping the dark area, the system back button
 * or [content]'s `close` all slide it away and then call [onDismiss]. The sheet is as tall as its content.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "sheet",
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

        Box(modifier = modifier.fillMaxSize().testTag(tag)) {
            // The dark area: tapping it closes the sheet, and it keeps touches and swipes away from the page below.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = progress.value }
                    .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = (1f - progress.value) * size.height }
                    .background(colors.bgSurface, Radius.sheet)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) { content(close) }
        }
    }
}
