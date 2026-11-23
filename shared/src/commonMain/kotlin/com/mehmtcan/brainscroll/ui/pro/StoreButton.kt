package com.mehmtcan.brainscroll.ui.pro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_store
import com.mehmtcan.brainscroll.ui.components.LineIcon
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * The store icon in the top right of Home, the feed and Profile (docs/pro-plan.md section 7.4). It opens the purchase
 * screen. It is a quiet shortcut: no dot, no badge. It is not there where nothing can be bought (the store is off, the
 * desktop app) and not for a Pro player, who has nothing left to buy.
 * With [wiggle] the icon shakes once, a moment after the page opens, to be noticed; never again after that, and not at all
 * with reduced motion. The feed passes false: nothing should move beside a puzzle.
 */
@Composable
fun StoreButton(modifier: Modifier = Modifier, wiggle: Boolean = true) {
    val pro = LocalPro.current
    val isPro by pro.isPro.collectAsStateWithLifecycle()
    if (!pro.available || isPro) return
    val openPaywall = LocalOpenPaywall.current
    val label = stringResource(Res.string.a11y_store)
    val reduce = LocalReduceMotion.current
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!wiggle || reduce) return@LaunchedEffect
        delay(500)
        for (target in floatArrayOf(-14f, 14f, -9f, 9f, 0f)) angle.animateTo(target, tween(110, easing = Motion.Standard))
    }
    Box(
        modifier = modifier
            .testTag("storeButton")
            .size(48.dp)
            .semantics { contentDescription = label }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                openPaywall(PaywallSource.StoreIcon)
            },
        contentAlignment = Alignment.Center,
    ) {
        // No background: the shop front is drawn in the main action color of the app.
        LineIcon(LineIcon.Shop, BrainScrollTheme.colors.accent, modifier = Modifier.graphicsLayer { rotationZ = angle.value }, size = 28.dp)
    }
}
