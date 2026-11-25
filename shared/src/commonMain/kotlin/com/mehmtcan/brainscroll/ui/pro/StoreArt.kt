package com.mehmtcan.brainscroll.ui.pro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.daily.FlameIcon
import com.mehmtcan.brainscroll.ui.home.BrainMascot
import com.mehmtcan.brainscroll.ui.home.MascotFace
import com.mehmtcan.brainscroll.ui.home.lerp
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * The little pictures of the store, drawn in the style of the mascot: flat fills, a darker tone for the details, a soft shine.
 * Colors come from the theme tokens, so nothing here has a color of its own.
 */

/** A soft round glow of [color], brightest in the middle and gone at the edge. It sits behind a picture to lift it off the page. */
@Composable
fun SoftGlow(color: Color, size: Dp, modifier: Modifier = Modifier, strength: Float = 0.32f) {
    Canvas(modifier.size(size)) {
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = strength), color.copy(alpha = 0f)), center = center, radius = this.size.minDimension / 2f))
    }
}

/** A four-pointed sparkle. */
@Composable
fun Sparkle(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val c = center
        val r = this.size.minDimension / 2f
        val path = Path().apply {
            moveTo(c.x, c.y - r)
            quadraticTo(c.x, c.y, c.x + r, c.y)
            quadraticTo(c.x, c.y, c.x, c.y + r)
            quadraticTo(c.x, c.y, c.x - r, c.y)
            quadraticTo(c.x, c.y, c.x, c.y - r)
            close()
        }
        drawPath(path, color)
    }
}

/**
 * A hint, as a light bulb: a glass of the pending yellow with a shine and a little filament, a grey base, and three short rays
 * on top. Filled, like the mascot, instead of the thin outline of the guide button.
 */
@Composable
fun HintBulb(size: Dp, modifier: Modifier = Modifier, rays: Boolean = true) {
    val colors = BrainScrollTheme.colors
    val body = colors.pendingFill
    val light = lerp(body, Color.White, 0.55f)
    val dark = lerp(body, Color.Black, 0.45f)
    val base = colors.borderStrong
    val baseDark = lerp(base, Color.Black, 0.35f)
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun p(x: Float, y: Float) = Offset(x * w, y * h)
        val glass = Brush.radialGradient(listOf(light, body), center = p(0.42f, 0.28f), radius = 0.5f * w)
        // The rays first, so the glass covers their roots.
        if (rays) {
            val ray = 0.045f * w
            for ((angle, from, to) in listOf(Triple(-90f, 0.43f, 0.52f), Triple(-48f, 0.43f, 0.52f), Triple(-132f, 0.43f, 0.52f))) {
                val rad = angle * 3.14159f / 180f
                val cx = 0.5f
                val cy = 0.42f
                drawLine(
                    body,
                    p(cx + kotlin.math.cos(rad) * from, cy + kotlin.math.sin(rad) * from),
                    p(cx + kotlin.math.cos(rad) * to, cy + kotlin.math.sin(rad) * to),
                    strokeWidth = ray, cap = StrokeCap.Round,
                )
            }
        }
        // The glass: a ball that narrows into the neck.
        drawCircle(glass, 0.33f * w, p(0.5f, 0.42f))
        drawPath(
            Path().apply {
                moveTo(p(0.30f, 0.56f).x, p(0.30f, 0.56f).y)
                lineTo(p(0.38f, 0.80f).x, p(0.38f, 0.80f).y)
                lineTo(p(0.62f, 0.80f).x, p(0.62f, 0.80f).y)
                lineTo(p(0.70f, 0.56f).x, p(0.70f, 0.56f).y)
                close()
            },
            glass,
        )
        // The filament.
        drawPath(
            Path().apply {
                moveTo(p(0.43f, 0.72f).x, p(0.43f, 0.72f).y)
                lineTo(p(0.43f, 0.50f).x, p(0.43f, 0.50f).y)
                quadraticTo(p(0.47f, 0.40f).x, p(0.47f, 0.40f).y, p(0.5f, 0.50f).x, p(0.5f, 0.50f).y)
                quadraticTo(p(0.53f, 0.60f).x, p(0.53f, 0.60f).y, p(0.57f, 0.50f).x, p(0.57f, 0.50f).y)
                lineTo(p(0.57f, 0.72f).x, p(0.57f, 0.72f).y)
            },
            dark.copy(alpha = 0.75f),
            style = Stroke(width = 0.04f * w, cap = StrokeCap.Round),
        )
        // The shine on the upper left of the glass.
        drawArc(
            Color.White.copy(alpha = 0.7f), startAngle = 200f, sweepAngle = 55f, useCenter = false,
            topLeft = p(0.26f, 0.18f), size = Size(0.46f * w, 0.46f * h), style = Stroke(width = 0.055f * w, cap = StrokeCap.Round),
        )
        // The base: a wide ring and a narrow one under it.
        drawRoundRect(base, p(0.35f, 0.79f), Size(0.30f * w, 0.09f * h), CornerRadius(0.04f * w))
        drawRoundRect(baseDark, p(0.40f, 0.89f), Size(0.20f * w, 0.07f * h), CornerRadius(0.035f * w))
    }
}

/**
 * The picture of a hint pack: the mascot with the hint stuck in its head like an idea, one bulb for a small pack and three, fanned
 * out, for a big one. It does not react to taps: the whole card is the button.
 */
@Composable
fun PackArt(hints: Int, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    Box(modifier.size(width = 112.dp, height = 104.dp), contentAlignment = Alignment.BottomCenter) {
        SoftGlow(colors.pendingFill, 112.dp, Modifier.align(Alignment.Center))
        if (hints >= 50) {
            // The bulbs sit in the top of the head and lean outward.
            HintBulb(34.dp, Modifier.align(Alignment.TopCenter).offset(x = (-30).dp, y = 22.dp).graphicsLayer { rotationZ = -30f }, rays = false)
            HintBulb(34.dp, Modifier.align(Alignment.TopCenter).offset(x = 30.dp, y = 22.dp).graphicsLayer { rotationZ = 30f }, rays = false)
            HintBulb(46.dp, Modifier.align(Alignment.TopCenter).offset(y = 8.dp))
        } else {
            HintBulb(48.dp, Modifier.align(Alignment.TopCenter).offset(y = 10.dp))
        }
        BrainMascot(face = MascotFace.Idea, modifier = Modifier.size(68.dp), interactive = false)
        Sparkle(colors.pendingFill, 11.dp, Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp))
        if (hints >= 50) Sparkle(lerp(colors.pendingFill, Color.White, 0.5f), 8.dp, Modifier.align(Alignment.TopStart).offset(x = 6.dp, y = 14.dp))
    }
}

private fun easeOut(x: Float): Float = 1f - (1f - x) * (1f - x)

/** Eases to 1 with a small overshoot on the way, like something popping into place. */
private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val y = x - 1f
    return 1f + c3 * y * y * y + c1 * y * y
}

/**
 * The picture of the streak offer, and a short story told with the mascot's faces. The mascot starts sad beside a dim ember (the
 * streak is out); the ember catches and the flame grows with a pop while its eyes go wide; then it breaks into a joyful face,
 * hops, sparkles burst around it, and the days of the streak count up in the badge. After that it floats a little and the
 * sparkles twinkle. The story plays once, about three seconds after the page has slid in. With reduced motion the page shows the
 * happy end at once and nothing moves.
 */
@Composable
fun StreakArt(days: Int, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(reduce) {
        if (reduce) {
            progress.snapTo(1f)
        } else {
            delay(450)
            progress.animateTo(1f, tween(3000, easing = LinearEasing))
        }
    }
    val idle = rememberInfiniteTransition(label = "idle")
    val bob by idle.animateFloat(0f, if (reduce) 0f else 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val flicker by idle.animateFloat(0f, if (reduce) 0f else 1f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flicker")
    val twinkle by idle.animateFloat(0.55f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "twinkle")

    val p = progress.value
    fun seg(a: Float, b: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)
    val ignite = seg(0.20f, 0.42f)
    val flame = easeOutBack(ignite)
    val face = lerp(lerp(MascotFace.Sad, MascotFace.Surprised, easeOut(seg(0.20f, 0.38f))), MascotFace.Joy, easeOut(seg(0.46f, 0.62f)))
    // It sinks a little while sad, rises when the flame catches, and hops once when it is happy.
    val rise = easeOut(seg(0.20f, 0.45f))
    val hop = kotlin.math.sin(kotlin.math.PI.toFloat() * seg(0.56f, 0.76f))
    val counted = (days * easeOut(seg(0.42f, 0.86f))).toInt()
    val badge = easeOutBack(seg(0.42f, 0.56f))
    val settled = seg(0.70f, 1f)

    Box(modifier.size(224.dp), contentAlignment = Alignment.Center) {
        SoftGlow(colors.streakDay, 224.dp, strength = 0.30f * ignite)
        // The flame behind: a dim ember at first, then the mascot stands in its heart.
        FlameIcon(
            color = lerp(colors.textDisabled, colors.streakDay, easeOut(seg(0.20f, 0.40f))),
            iconSize = 178.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-4).dp)
                .graphicsLayer {
                    val s = (0.30f + 0.70f * flame) * (1f + 0.025f * flicker * ignite)
                    scaleX = s
                    scaleY = s * (1f + 0.04f * flicker * ignite)
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
        )
        BrainMascot(
            face = face,
            modifier = Modifier
                .size(112.dp)
                .offset(y = 18.dp)
                .graphicsLayer {
                    translationY = (1f - rise) * 12.dp.toPx() - hop * 22.dp.toPx() + (0.5f - bob) * 10.dp.toPx() * settled
                },
            interactive = false,
            // A rim in the color of the page, so the green head does not melt into the orange flame behind it.
            outline = colors.bgPage,
        )
        Sparkle(
            colors.pendingFill, 16.dp,
            Modifier.align(Alignment.TopEnd).offset(x = (-26).dp, y = 22.dp).graphicsLayer { val s = easeOutBack(seg(0.62f, 0.78f)) * twinkle; scaleX = s; scaleY = s },
        )
        Sparkle(
            lerp(colors.streakDay, Color.White, 0.6f), 11.dp,
            Modifier.align(Alignment.TopStart).offset(x = 30.dp, y = 40.dp).graphicsLayer { val s = easeOutBack(seg(0.68f, 0.84f)) * twinkle; scaleX = s; scaleY = s },
        )
        Sparkle(
            lerp(colors.streakDay, Color.White, 0.5f), 9.dp,
            Modifier.align(Alignment.CenterStart).offset(x = 10.dp, y = 22.dp).graphicsLayer { val s = easeOutBack(seg(0.74f, 0.90f)) * twinkle; scaleX = s; scaleY = s },
        )
        // The streak that comes back, in the colors of the streak everywhere else in the app. It pops in and counts up.
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp, y = (-6).dp)
                .graphicsLayer { scaleX = badge; scaleY = badge }
                .background(colors.bgSurface, Radius.pill)
                .border(1.5.dp, colors.streakDay, Radius.pill)
                .padding(horizontal = Spacing.md, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlameIcon(colors.streakDay, 22.dp)
            BasicText(counted.toString(), style = BrainScrollTheme.typography.heading.copy(color = colors.textPrimary))
        }
    }
}

/**
 * A small crown in the yellow of the app, with a band and a gem on each point. With a [tint] it is one flat color instead (for a
 * crown on a pill of its own yellow).
 */
@Composable
fun Crown(modifier: Modifier = Modifier, width: Dp = 44.dp, tint: Color? = null) {
    val colors = BrainScrollTheme.colors
    val gold = colors.pendingFill
    val shade = lerp(gold, Color.Black, 0.3f)
    Canvas(modifier.size(width = width, height = width * (32f / 44f))) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            moveTo(0.04f * w, 0.88f * h)
            lineTo(0.04f * w, 0.28f * h)
            lineTo(0.28f * w, 0.56f * h)
            lineTo(0.50f * w, 0.06f * h)
            lineTo(0.72f * w, 0.56f * h)
            lineTo(0.96f * w, 0.28f * h)
            lineTo(0.96f * w, 0.88f * h)
            close()
        }
        drawPath(body, if (tint != null) Brush.verticalGradient(listOf(tint, tint)) else Brush.verticalGradient(listOf(lerp(gold, Color.White, 0.35f), gold)))
        drawRoundRect(tint?.copy(alpha = 0.55f) ?: shade, Offset(0.04f * w, 0.70f * h), Size(0.92f * w, 0.18f * h), CornerRadius(0.05f * w))
        for ((x, y) in listOf(0.04f to 0.28f, 0.50f to 0.06f, 0.96f to 0.28f)) drawCircle(if (tint != null) gold else Color.White, 0.06f * w, Offset(x * w, y * h))
    }
}

/**
 * The mascot of the Pro card: wearing a crown, tilted a little, on a glow, and every few seconds it turns once around its vertical axis, like a coin. The
 * mascot is the same one as everywhere; only the crown is new.
 */
@Composable
fun ProMascot(modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val reduce = LocalReduceMotion.current
    // Now and then it turns once around its vertical axis (crown and all). Not with reduced motion.
    val spin = remember { Animatable(0f) }
    LaunchedEffect(reduce) {
        if (reduce) return@LaunchedEffect
        delay(2500)
        while (true) {
            spin.snapTo(0f)
            spin.animateTo(360f, tween(750, easing = FastOutSlowInEasing))
            delay(5200)
        }
    }
    Box(modifier.size(120.dp), contentAlignment = Alignment.Center) {
        SoftGlow(colors.accent, 120.dp, strength = 0.30f)
        Box(Modifier.size(104.dp).graphicsLayer {
                rotationZ = -7f
                // A turn around the vertical axis, like a coin spinning: seen from the front it narrows, flips, and widens again.
                rotationY = spin.value
                cameraDistance = 14f * density
            }, contentAlignment = Alignment.TopCenter) {
            BrainMascot(t = 0f, modifier = Modifier.size(104.dp).offset(y = 6.dp))
            Crown(Modifier.offset(x = 5.dp, y = (-1).dp).graphicsLayer { rotationZ = 9f })
        }
        Sparkle(colors.pendingFill, 11.dp, Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 8.dp))
    }
}
