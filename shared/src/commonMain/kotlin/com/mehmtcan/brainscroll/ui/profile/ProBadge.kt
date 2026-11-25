package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.pro_badge
import brainscroll.shared.generated.resources.profile_get_pro
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.pro.Crown
import com.mehmtcan.brainscroll.ui.pro.SoftGlow
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** 0 to 1 in the first second of every four, then rests at 1: the sweep of light over a Pro pill. With reduced motion it is parked. */
@Composable
private fun rememberShine(): State<Float> {
    val reduce = LocalReduceMotion.current
    if (reduce) return remember { mutableStateOf(1f) }
    return rememberInfiniteTransition(label = "shine").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4400
                0f at 0
                1f at 1100 using FastOutSlowInEasing
                1f at 4400
            },
            RepeatMode.Restart,
        ),
        label = "shine",
    )
}

/** A soft diagonal band of light that crosses the pill from left to right while [progress] goes from 0 to 1. */
private fun Modifier.shine(progress: () -> Float): Modifier = drawWithContent {
    drawContent()
    val p = progress()
    if (p > 0.001f && p < 0.999f) {
        val band = size.height * 1.6f
        val x = -band + (size.width + band * 2f) * p
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                start = Offset(x, 0f),
                end = Offset(x + band, size.height),
            ),
        )
    }
}

/**
 * Pro, under the name on the profile. For a Pro player a golden pill with a crown and the word PRO; for the others a green pill
 * with a crown and "Get Pro" that opens the store. Both have a sweep of light across them now and then (not with reduced motion).
 */
@Composable
fun ProPill(isPro: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val shine = rememberShine()
    val gold = colors.pendingFill
    val fill = if (isPro) Brush.horizontalGradient(listOf(lerp(gold, Color.White, 0.45f), gold, lerp(gold, Color.Black, 0.12f))) else Brush.horizontalGradient(listOf(colors.accent, colors.accent))
    val ink = if (isPro) colors.pendingOn else colors.accentOn
    Row(
        modifier = modifier
            .clip(Radius.pill)
            .background(fill)
            .then(if (isPro) Modifier else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onOpen))
            .shine { shine.value }
            .padding(start = Spacing.sm, end = Spacing.md, top = 6.dp, bottom = 6.dp)
            .testTag(if (isPro) "proBadge" else "proCard"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crown(width = 20.dp, tint = ink)
        BasicText(
            text = stringResource(if (isPro) Res.string.pro_badge else Res.string.profile_get_pro),
            style = type.label.copy(color = ink),
        )
    }
}

/**
 * The avatar of a Pro player: a golden ring around it, a glow behind it, and a crown tilted on its top left corner. [content] is the
 * avatar itself, 64 dp. The ring and the crown are drawn outside its box, so the layout is the same as without them.
 */
@Composable
fun ProAvatarFrame(content: @Composable () -> Unit) {
    val gold = BrainScrollTheme.colors.pendingFill
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        SoftGlow(gold, 104.dp, strength = 0.28f)
        content()
        Box(
            Modifier
                .size(72.dp)
                .border(
                    2.5.dp,
                    Brush.sweepGradient(listOf(lerp(gold, Color.White, 0.5f), gold, lerp(gold, Color.Black, 0.2f), gold, lerp(gold, Color.White, 0.5f))),
                    CircleShape,
                ),
        )
        Crown(
            width = 30.dp,
            modifier = Modifier.align(Alignment.TopStart).offset(x = (-9).dp, y = (-17).dp).graphicsLayer { rotationZ = -22f },
        )
    }
}
