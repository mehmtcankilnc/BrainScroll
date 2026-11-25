package com.mehmtcan.brainscroll.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EasyBody = Color(0xFF7ED957)
private val EasyDark = Color(0xFF4FA82E)
private val MediumBody = Color(0xFFFFC046)
private val MediumDark = Color(0xFFD9922B)
private val HardBody = Color(0xFFFF6B5B)
private val HardDark = Color(0xFFC63C2E)

/** The main color of the mascot at [t] (0 easy, 1 medium, 2 hard): the color of the slider follows it. */
fun mascotTint(t: Float): Color = if (t <= 1f) lerp(EasyBody, MediumBody, t.coerceIn(0f, 1f)) else lerp(MediumBody, HardBody, (t - 1f).coerceIn(0f, 1f))

private fun mascotDark(t: Float): Color = if (t <= 1f) lerp(EasyDark, MediumDark, t.coerceIn(0f, 1f)) else lerp(MediumDark, HardDark, (t - 1f).coerceIn(0f, 1f))

private fun mix(a: Float, b: Float, c: Float, t: Float): Float = if (t <= 1f) a + (b - a) * t.coerceIn(0f, 1f) else b + (c - b) * (t - 1f).coerceIn(0f, 1f)

/** The face of the difficulty slider at [t] (0 easy, 1 medium, 2 hard): the same numbers the mascot always used. */
fun faceForDifficulty(t: Float): MascotFace {
    val d = t.coerceIn(0f, 2f)
    return MascotFace(
        lid = mix(0f, 0.22f, 0.42f, d),
        pupil = mix(4f, 3.8f, 2.8f, d),
        browInner = mix(50f, 51.5f, 57f, d),
        browOuter = mix(54f, 52.5f, 48f, d),
        browLift = mix(-5f, -3f, 3f, d),
        cheek = (1f - d / 1.2f).coerceIn(0f, 1f),
        mouthEnd = mix(77f, 80f, 82f, d),
        mouthControl = mix(88f, 80f, 70f, d),
        mouthTilt = mix(0f, 3f, 0f, d),
        sweat = ((d - 1.3f) / 0.7f).coerceIn(0f, 1f),
    )
}

/**
 * The brain character of the app icon, whose face goes from easy to hard as [t] goes from 0 to 2 (any value in between gives
 * the face in between, so dragging the slider morphs it live): a smile, a smirk, a frown; raised brows, flat brows, angry
 * brows; wide eyes, half closed eyes; the cheeks fade and a drop of sweat shows. The eyes look toward [lookX] (-1 to 1) and
 * down. A tap makes it blink and hop, unless it is not [interactive] (it sits inside something that is a button itself).
 */
@Composable
fun BrainMascot(t: Float, modifier: Modifier = Modifier, lookX: Float = 0f, interactive: Boolean = true) {
    BrainMascot(face = faceForDifficulty(t), modifier = modifier, lookX = lookX, interactive = interactive, tint = t)
}

/**
 * The same character with any [face] ([MascotFace.Happy], [MascotFace.Sad], a blend of two with [lerp], ...). [tint] is the color
 * of the body on the difficulty scale (0 green, 1 yellow, 2 red); a mood keeps the green. With an [outline] the head gets a thin
 * rim of that color, which keeps it apart from a background of a similar strength (the flame).
 */
@Composable
fun BrainMascot(face: MascotFace, modifier: Modifier = Modifier, lookX: Float = 0f, interactive: Boolean = true, tint: Float = 0f, outline: Color? = null) {
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val blink = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    // An idle blink now and then (not with reduced motion).
    LaunchedEffect(reduce) {
        if (reduce) return@LaunchedEffect
        while (true) {
            delay(3200)
            blink.animateTo(1f, tween(70))
            blink.animateTo(0f, tween(110))
        }
    }
    Canvas(
        modifier
            .testTag("brainMascot")
            .clearAndSetSemantics { }
            .then(
                if (!interactive) Modifier else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    scope.launch {
                        blink.animateTo(1f, tween(60))
                        blink.animateTo(0f, tween(100))
                    }
                    if (!reduce) {
                        scope.launch {
                            hop.snapTo(0f)
                            hop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
                            hop.snapTo(0f)
                        }
                    }
                },
            ),
    ) {
        val u = size.minDimension / 120f
        // The hop: squash, then stretch up and settle, around the bottom of the head.
        val h = hop.value
        val squash = if (h <= 0f) 0f else kotlin.math.sin(h * 3.14159f * 2f) * (1f - h) * 0.12f
        scale(1f + squash, 1f - squash, pivot = Offset(size.width / 2f, size.height * 0.9f)) {
            drawBrain(u, face, tint.coerceIn(0f, 2f), lookX.coerceIn(-1f, 1f), blink.value, outline)
        }
    }
}

private fun DrawScope.drawBrain(u: Float, face: MascotFace, tint: Float, lookX: Float, blink: Float, outline: Color?) {
    val body = mascotTint(tint)
    val dark = mascotDark(tint)
    val ink = Color(0xFF10240A)
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    // The head: the same circles as the icon. With an outline the same circles, a little bigger, are drawn first in its color.
    val head = listOf(
        Triple(40f, 50f, 18f), Triple(34f, 70f, 16f), Triple(52f, 36f, 16f), Triple(80f, 50f, 18f), Triple(86f, 70f, 16f),
        Triple(68f, 36f, 16f), Triple(50f, 80f, 15f), Triple(70f, 80f, 15f), Triple(60f, 58f, 24f),
    )
    if (outline != null) for ((x, y, r) in head) drawCircle(outline, (r + 2.6f) * u, p(x, y))
    for ((x, y, r) in head) drawCircle(body, r * u, p(x, y))
    val stroke = Stroke(width = 3.5f * u, cap = StrokeCap.Round)
    // The grooves of the brain.
    for ((a, b, c, d) in listOf(
        listOf(40f, 72f, 46f, 67f), listOf(80f, 72f, 74f, 67f),
    )) {
        val path = Path().apply { moveTo(p(a, b).x, p(a, b).y); quadraticTo(p(c, d).x, p(c, d).y, p(c + (if (a < 60) 5f else -5f), b + 1f).x, p(c + (if (a < 60) 5f else -5f), b + 1f).y) }
        drawPath(path, dark, style = stroke)
    }
    drawPath(
        Path().apply {
            moveTo(p(60f, 34f).x, p(60f, 34f).y)
            cubicTo(p(55f, 42f).x, p(55f, 42f).y, p(63f, 48f).x, p(63f, 48f).y, p(60f, 54f).x, p(60f, 54f).y)
        },
        dark,
        style = Stroke(width = 4f * u, cap = StrokeCap.Round),
    )

    // Eyes: white, a lid that comes down, a pupil that looks toward the slider. With eyeArc they turn into happy arcs.
    val round = 1f - face.eyeArc
    val lid = (face.lid + blink * 0.9f).coerceIn(0f, 1f)
    for (eyeX in listOf(48f, 72f)) {
        val c = p(eyeX, 62f)
        if (round > 0f) {
            val eye = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, 8f * u)) }
            drawCircle(Color.White.copy(alpha = round), 8f * u, c)
            drawCircle(Color(0xFF121212).copy(alpha = round), face.pupil * u, p(eyeX + 1.5f + lookX * 2.2f, 63.5f + 1.2f))
            clipPath(eye) { drawRect(body, topLeft = Offset(c.x - 8f * u, c.y - 8f * u), size = androidx.compose.ui.geometry.Size(16f * u, 16f * u * lid)) }
        }
        if (face.eyeArc > 0f) {
            drawPath(
                Path().apply {
                    moveTo(p(eyeX - 6.5f, 64f).x, p(eyeX - 6.5f, 64f).y)
                    quadraticTo(p(eyeX, 53f).x, p(eyeX, 53f).y, p(eyeX + 6.5f, 64f).x, p(eyeX + 6.5f, 64f).y)
                },
                ink.copy(alpha = face.eyeArc), style = Stroke(width = 3.4f * u, cap = StrokeCap.Round),
            )
        }
    }

    // Brows: raised and round when happy, flat when medium, low on the inside (angry) when hard, high on the inside when sad.
    val inner = face.browInner
    val outer = face.browOuter
    val lift = face.browLift
    val browStroke = Stroke(width = 3.6f * u, cap = StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(p(38f, outer).x, p(38f, outer).y)
            quadraticTo(p(44f, (inner + outer) / 2 + lift).x, p(44f, (inner + outer) / 2 + lift).y, p(55f, inner).x, p(55f, inner).y)
        },
        dark, style = browStroke,
    )
    drawPath(
        Path().apply {
            moveTo(p(82f, outer).x, p(82f, outer).y)
            quadraticTo(p(76f, (inner + outer) / 2 + lift).x, p(76f, (inner + outer) / 2 + lift).y, p(65f, inner).x, p(65f, inner).y)
        },
        dark, style = browStroke,
    )

    // Cheeks: they fade out as it gets harder and glow when it is happy.
    val cheek = face.cheek.coerceAtLeast(0f)
    if (cheek > 0f) {
        val blush = Color(0xFFFF9F43).copy(alpha = (0.55f * cheek).coerceAtMost(0.9f))
        val radius = 5f + (cheek - 1f).coerceAtLeast(0f) * 2.5f
        drawCircle(blush, radius * u, p(40f, 73f))
        drawCircle(blush, radius * u, p(80f, 73f))
    }

    // The mouth: a smile, a smirk, a frown, a round "o", or an open smile. A closed mouth is one curved line. An open one is
    // a shape: the upper lip (the curve) and a rounder lower lip under it, filled dark, with a tongue when it is smiling wide.
    val left = 60f - face.mouthHalf
    val right = 60f + face.mouthHalf
    val rightEnd = face.mouthEnd - face.mouthTilt
    if (face.mouthOpen > 0.5f) {
        val depth = face.mouthOpen
        val bulge = face.mouthHalf * 0.45f
        val mouth = Path().apply {
            moveTo(p(left, face.mouthEnd).x, p(left, face.mouthEnd).y)
            quadraticTo(p(60f, face.mouthControl).x, p(60f, face.mouthControl).y, p(right, rightEnd).x, p(right, rightEnd).y)
            cubicTo(
                p(right + bulge, rightEnd + depth * 1.33f).x, p(right + bulge, rightEnd + depth * 1.33f).y,
                p(left - bulge, face.mouthEnd + depth * 1.33f).x, p(left - bulge, face.mouthEnd + depth * 1.33f).y,
                p(left, face.mouthEnd).x, p(left, face.mouthEnd).y,
            )
            close()
        }
        val alpha = (depth / 4f).coerceIn(0f, 1f)
        drawPath(mouth, ink.copy(alpha = alpha))
        if (face.tongue > 0f) {
            clipPath(mouth) {
                drawOval(
                    Color(0xFFFF7A8A).copy(alpha = face.tongue * alpha),
                    topLeft = p(60f - face.mouthHalf * 0.55f, face.mouthEnd + depth * 0.52f),
                    size = androidx.compose.ui.geometry.Size(face.mouthHalf * 1.1f * u, depth * 0.62f * u),
                )
            }
        }
        // A thin rim in the same ink rounds the corners of the shape.
        drawPath(mouth, ink.copy(alpha = alpha), style = Stroke(width = 1.8f * u, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
    drawPath(
        Path().apply {
            moveTo(p(left, face.mouthEnd).x, p(left, face.mouthEnd).y)
            quadraticTo(p(60f, face.mouthControl).x, p(60f, face.mouthControl).y, p(right, rightEnd).x, p(right, rightEnd).y)
        },
        ink.copy(alpha = (1f - face.mouthOpen / 4f).coerceIn(0f, 1f)),
        style = Stroke(width = 3.5f * u, cap = StrokeCap.Round),
    )

    // A drop of sweat on the hard or worried face, and a tear under the left eye of the sad one.
    fun drop(x: Float, y: Float, color: Color) = drawPath(
        Path().apply {
            moveTo(p(x, y - 7f).x, p(x, y - 7f).y)
            cubicTo(p(x + 5f, y).x, p(x + 5f, y).y, p(x + 4f, y + 6f).x, p(x + 4f, y + 6f).y, p(x, y + 6f).x, p(x, y + 6f).y)
            cubicTo(p(x - 4f, y + 6f).x, p(x - 4f, y + 6f).y, p(x - 5f, y).x, p(x - 5f, y).y, p(x, y - 7f).x, p(x, y - 7f).y)
        },
        color,
    )
    if (face.sweat > 0f) drop(96f, 42f + (1f - face.sweat) * -4f, Color(0xFF5BC0FF).copy(alpha = face.sweat))
    if (face.tear > 0f) drop(42f, 76f + (1f - face.tear) * -4f, Color(0xFF5BC0FF).copy(alpha = face.tear))
}
