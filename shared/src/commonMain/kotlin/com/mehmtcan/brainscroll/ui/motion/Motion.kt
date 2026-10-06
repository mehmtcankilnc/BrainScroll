package com.mehmtcan.brainscroll.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.game.wordle.WordleStatus

/** The motion tokens of docs/design.md section 6. Durations in milliseconds. */
object Motion {
    const val FAST = 100
    const val BASE = 200
    const val SLOW = 400
    const val FLIP = 500
    const val POP = 250
    const val FLIP_STAGGER = 100

    /** `cubic-bezier(0.2, 0, 0, 1)` */
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** `cubic-bezier(0.34, 1.56, 0.64, 1)`: goes a little past the target and settles back. */
    val Overshoot = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
}

/** True when the player asked the system for less motion: flips, pops and confetti become instant or disappear. */
val LocalReduceMotion = compositionLocalOf { false }

/** Reads the system setting (Android: animations removed, iOS: Reduce Motion). Desktop has none. */
@Composable
expect fun rememberReduceMotion(): Boolean

/**
 * True only for a puzzle that was being played when this composable first saw it and has ended since, so a
 * finished puzzle that is merely shown again (restored, scrolled back to) does not celebrate a second time.
 * The result card, the haptics and the confetti all key off it.
 */
@Composable
fun rememberJustFinished(status: WordleStatus): Boolean {
    val sawPlaying = remember { booleanArrayOf(status == WordleStatus.Playing) }
    if (status == WordleStatus.Playing) sawPlaying[0] = true
    return status != WordleStatus.Playing && sawPlaying[0]
}

/** A short scale "pop" with overshoot each time [value] goes up (a longer streak). Nothing when motion is reduced. */
@Composable
fun Modifier.popOnIncrease(value: Int): Modifier {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    var last by remember { mutableIntStateOf(value) }
    LaunchedEffect(value) {
        if (value > last && !reduce) {
            scale.snapTo(1f)
            scale.animateTo(1.25f, tween(Motion.FAST, easing = Motion.Standard))
            scale.animateTo(1f, tween(Motion.POP, easing = Motion.Overshoot))
        }
        last = value
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** A letter settling into its tile: starts a little small and overshoots to full size. Only when it appears while shown. */
@Composable
fun Modifier.popOnAppear(appeared: Boolean): Modifier {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    var was by remember { mutableStateOf(appeared) }
    LaunchedEffect(appeared) {
        if (appeared && !was && !reduce) {
            scale.snapTo(0.8f)
            scale.animateTo(1f, tween(Motion.POP, easing = Motion.Overshoot))
        }
        was = appeared
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** The result card arriving: fades in while rising a few dp, over `motion.slow`. Instant when [animate] is false. */
@Composable
fun Modifier.slideFadeIn(animate: Boolean): Modifier {
    val reduce = LocalReduceMotion.current
    val run = animate && !reduce
    val progress = remember { Animatable(if (run) 0f else 1f) }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        if (run) progress.animateTo(1f, tween(Motion.SLOW, easing = Motion.Standard))
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * with(density) { 16.dp.toPx() }
    }
}
