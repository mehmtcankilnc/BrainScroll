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
 * A burst of confetti from the top of the screen, once. docs/design.md section 6: only for a won daily puzzle
 * and for streak milestones. It draws nothing when the player asked for reduced motion, and never takes touches.
 * The pieces are the same every time (a fixed seed), so a screenshot test is stable.
 */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    if (LocalReduceMotion.current) return

    val colors = BrainScrollTheme.colors
    val palette = listOf(colors.correctFill, colors.pendingFill, colors.streakDay, colors.streakAnswer, colors.error)
    val pieces = remember {
        val random = Random(7)
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

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(DurationMillis, easing = LinearEasing)) }

    Canvas(modifier = modifier.fillMaxSize().testTag("confetti")) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val seconds = t * DurationMillis / 1000f
        // Gentle gravity, and a fade over the last quarter so the pieces do not vanish suddenly.
        val gravity = 1.1f
        val alpha = if (t > 0.75f) (1f - t) / 0.25f else 1f
        for (piece in pieces) {
            val x = (piece.x + piece.vx * seconds) * size.width
            val y = (piece.y + piece.vy * seconds + 0.5f * gravity * seconds * seconds) * size.height
            val side = piece.size * size.width
            rotate(degrees = piece.spin * seconds, pivot = Offset(x, y)) {
                drawRect(
                    color = palette[piece.colorIndex].copy(alpha = alpha),
                    topLeft = Offset(x - side / 2, y - side / 2),
                    size = Size(side, side * 0.6f),
                )
            }
        }
    }
}
