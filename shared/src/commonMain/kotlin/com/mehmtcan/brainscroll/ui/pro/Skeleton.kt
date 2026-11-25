package com.mehmtcan.brainscroll.ui.pro

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/**
 * A placeholder for something that is still loading: a quiet block of the surface color with a soft band of light that crosses it
 * now and then. With reduced motion it is just the block. The shape and size are the real thing's, so nothing jumps when it arrives.
 */
@Composable
internal fun Modifier.skeleton(shape: Shape): Modifier {
    val base = BrainScrollTheme.colors.bgRaised
    val light = lerp(base, Color.White, 0.12f)
    val progress = if (LocalReduceMotion.current) {
        null
    } else {
        rememberInfiniteTransition(label = "skeleton").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
            label = "skeleton",
        )
    }
    return this.clip(shape).drawBehind {
        drawRect(base)
        val p = progress?.value ?: return@drawBehind
        val band = size.width * 0.7f
        val x = -band + (size.width + band) * p
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, light, Color.Transparent), startX = x, endX = x + band))
    }
}
