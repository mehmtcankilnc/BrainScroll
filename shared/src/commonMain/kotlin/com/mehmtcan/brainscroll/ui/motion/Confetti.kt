package com.mehmtcan.brainscroll.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.random.Random

private const val ParticleCount = 56
private const val DurationMillis = 2_200

/** One piece of confetti. Positions are fractions of the width and height, speeds are per second. */
private class Piece(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val spin: Float,
    val colorIndex: Int,
)

/**
 * A burst of confetti from the top of the screen, once. It celebrates a won puzzle (the daily one and every game in
 * the feed) and the streak milestones of the daily puzzle. It draws nothing when the player asked for reduced motion, and never takes touches.
 * The pieces are the same every time (a fixed seed), so a screenshot test is stable.
 */
@Composable
fun Confetti(modifier: Modifier = Modifier, world: Color? = null) {
    if (LocalReduceMotion.current) return

    // With a [world] the win is a burst: a ring of light spreads from behind the board, and the pieces fly out of the middle of
    // the screen in every direction (more of them, bigger, some round), then fall. Without one it is the plain shower from the top.
    val burst = world != null
    val colors = BrainScrollTheme.colors
    val palette = listOfNotNull(colors.correctFill, colors.pendingFill, colors.streakDay, colors.streakAnswer, colors.error, world)
    val pieces = remember {
        val random = Random(7)
        if (burst) {
            List(BurstCount) {
                val angle = random.nextFloat() * 2f * PI.toFloat()
                val speed = 0.35f + random.nextFloat() * 0.75f
                Piece(
                    x = 0.5f,
                    y = BurstOriginY,
                    // The screen is taller than wide, so the vertical speed is scaled down to the same distance.
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed * 0.55f - 0.12f,
                    size = 0.022f + random.nextFloat() * 0.022f,
                    spin = (random.nextFloat() - 0.5f) * 900f,
                    colorIndex = random.nextInt(palette.size),
                )
            }
        } else {
            List(ParticleCount) {
                Piece(
                    x = 0.1f + random.nextFloat() * 0.8f,
                    y = -0.05f - random.nextFloat() * 0.1f,
                    vx = (random.nextFloat() - 0.5f) * 0.5f,
                    vy = 0.1f + random.nextFloat() * 0.35f,
                    size = 0.016f + random.nextFloat() * 0.016f,
                    spin = (random.nextFloat() - 0.5f) * 720f,
                    colorIndex = random.nextInt(palette.size),
                )
            }
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(DurationMillis, easing = LinearEasing)) }

    Canvas(modifier = modifier.fillMaxSize().testTag("confetti")) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val seconds = t * DurationMillis / 1000f
        if (world != null && seconds < RingSeconds) {
            // The light: a ring that spreads out and thins while it fades, over a flash of the world's color that dies quickly.
            val r = (seconds / RingSeconds).let { 1f - (1f - it) * (1f - it) }
            val center = Offset(size.width / 2f, size.height * BurstOriginY)
            drawRect(world.copy(alpha = 0.22f * (1f - seconds / RingSeconds)))
            drawCircle(
                color = world.copy(alpha = 0.9f * (1f - r)),
                radius = size.width * 0.15f + size.width * 0.75f * r,
                center = center,
                style = Stroke(width = size.width * 0.05f * (1f - r) + 2.dp.toPx()),
            )
        }
        // Gentle gravity, and a fade over the last quarter so the pieces do not vanish suddenly.
        val gravity = 1.1f
        val alpha = if (t > 0.75f) (1f - t) / 0.25f else 1f
        for ((i, piece) in pieces.withIndex()) {
            val x = (piece.x + piece.vx * seconds) * size.width
            val y = (piece.y + piece.vy * seconds + 0.5f * gravity * seconds * seconds) * size.height
            val side = piece.size * size.width
            val color = palette[piece.colorIndex].copy(alpha = alpha)
            if (burst && i % 3 == 0) {
                drawCircle(color, radius = side / 2f, center = Offset(x, y))
            } else {
                rotate(degrees = piece.spin * seconds, pivot = Offset(x, y)) {
                    drawRect(color = color, topLeft = Offset(x - side / 2, y - side / 2), size = Size(side, side * 0.6f))
                }
            }
        }
    }
}

private const val BurstCount = 90
private const val BurstOriginY = 0.42f
private const val RingSeconds = 0.8f
