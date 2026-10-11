package com.mehmtcan.brainscroll.ui.mines

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val ExplosionMillis = 850
private const val SparkCount = 26

private class Spark(val angle: Float, val speed: Float, val size: Float, val colorIndex: Int)

/**
 * The blast when a mine is opened: a flash, a shock ring that spreads over the board, and sparks that fly out and fade.
 * It is drawn over the board around the cell that blew up ([centerX], [centerY] from the top left of the board), once
 * when [play] turns true. Nothing is drawn when the player asked for reduced motion, and it never takes touches.
 * The sparks come from a fixed seed so a screenshot test is stable.
 */
@Composable
fun MineExplosion(play: Boolean, centerX: Dp, centerY: Dp, cell: Dp, modifier: Modifier = Modifier) {
    if (LocalReduceMotion.current) return
    val colors = BrainScrollTheme.colors
    val palette = listOf(colors.error, colors.streakDay, colors.pendingFill, colors.textPrimary)
    val sparks = remember {
        val random = Random(11)
        List(SparkCount) {
            Spark(
                angle = random.nextFloat() * 2f * PI.toFloat(),
                speed = 1.6f + random.nextFloat() * 3.4f,
                size = 0.08f + random.nextFloat() * 0.12f,
                colorIndex = random.nextInt(palette.size),
            )
        }
    }
    val progress = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(play) {
        if (play) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(ExplosionMillis, easing = LinearEasing))
        }
    }

    Canvas(modifier.fillMaxSize().testTag("mineExplosion")) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val center = Offset(centerX.toPx(), centerY.toPx())
        val unit = cell.toPx()
        val eased = FastOutSlowInEasing.transform(t)

        // The flash: a bright disc that blooms quickly and is gone by a third of the way.
        if (t < 0.35f) {
            val f = t / 0.35f
            drawCircle(colors.streakDay.copy(alpha = 0.85f * (1f - f)), radius = unit * (0.5f + 1.1f * f), center = center)
            drawCircle(colors.textPrimary.copy(alpha = 0.9f * (1f - f)), radius = unit * 0.55f * (1f - f), center = center)
        }
        // The shock ring: spreads over the board, thinner and fainter as it goes.
        drawCircle(
            color = colors.error.copy(alpha = 0.8f * (1f - t)),
            radius = unit * (0.6f + 5.5f * eased),
            center = center,
            style = Stroke(width = unit * 0.28f * (1f - t) + 1f),
        )
        // The sparks.
        for (spark in sparks) {
            val distance = spark.speed * eased * unit * 1.6f
            val position = Offset(center.x + cos(spark.angle) * distance, center.y + sin(spark.angle) * distance)
            drawCircle(
                color = palette[spark.colorIndex].copy(alpha = (1f - t) * 0.95f),
                radius = unit * spark.size * (1f - 0.6f * t),
                center = position,
            )
        }
    }
}

/** A short sideways shake that dies down, for the board when a mine goes off. 0 when [play] is false or motion is reduced. */
@Composable
fun rememberBlastShake(play: Boolean): Animatable<Float, *> {
    val shake = remember { Animatable(1f) }
    val reduce = LocalReduceMotion.current
    LaunchedEffect(play) {
        if (play && !reduce) {
            shake.snapTo(0f)
            shake.animateTo(1f, tween(480, easing = LinearEasing))
        }
    }
    return shake
}
