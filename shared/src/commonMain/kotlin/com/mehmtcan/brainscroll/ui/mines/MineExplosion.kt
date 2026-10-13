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

private const val SparkCount = 12

private class Spark(val angle: Float, val speed: Float, val size: Float, val colorIndex: Int)

/** How long one small blast lasts. */
private const val BurstMillis = 380

/** The room between two blasts of the chain, in milliseconds: shorter when there are many mines, so it never drags. */
private fun stepMillis(mines: Int): Int = (900 / maxOf(mines, 1)).coerceIn(35, 90)

/** One mine in the chain: the cell it sits in and when (milliseconds after the first blast) it goes off. */
class MineBlast(val cell: Int, val delayMillis: Int)

/**
 * The order the mines go off in after one was opened: the one that was opened first, then the next closest, and so on
 * outwards, a short moment apart. [cols] gives the distance on the board.
 */
fun mineChain(mines: Set<Int>, start: Int, cols: Int): List<MineBlast> {
    if (mines.isEmpty()) return emptyList()
    fun distance(cell: Int): Int {
        val dr = cell / cols - start / cols
        val dc = cell % cols - start % cols
        return dr * dr + dc * dc
    }
    val step = stepMillis(mines.size)
    return mines.sortedWith(compareBy({ distance(it) }, { it })).mapIndexed { i, cell -> MineBlast(cell, i * step) }
}

/** Milliseconds the chain needs from the first blast until the last one has faded. */
fun mineChainMillis(chain: List<MineBlast>): Int = (chain.lastOrNull()?.delayMillis ?: 0) + BurstMillis

/**
 * The clock of the chain, in milliseconds since the first blast. It runs when [play] turns true (a mine was just opened),
 * and stays at "everything is over" otherwise (a finished page that is shown again) or when motion is reduced.
 */
@Composable
fun rememberChainClock(play: Boolean, totalMillis: Int): Float {
    val reduce = LocalReduceMotion.current
    val clock = remember(play) { Animatable(if (play && !reduce) 0f else Float.MAX_VALUE) }
    LaunchedEffect(play) {
        if (play && !reduce) clock.animateTo(totalMillis.toFloat(), tween(totalMillis, easing = LinearEasing))
    }
    return clock.value
}

/**
 * The blasts of the chain: for each mine a small flash, a ring that spreads a little and a few sparks that fade, over
 * [BurstMillis]. Drawn over the board; [centers] gives the centre of each cell from the top left of the board. The sparks
 * come from a fixed seed so a screenshot test is stable. Nothing is drawn when [clock] says it is over, and it never takes touches.
 */
@Composable
fun MineChainBlasts(chain: List<MineBlast>, clock: Float, centers: (Int) -> Pair<Dp, Dp>, cell: Dp, modifier: Modifier = Modifier) {
    if (chain.isEmpty() || clock >= mineChainMillis(chain)) return
    val colors = BrainScrollTheme.colors
    val palette = listOf(colors.error, colors.streakDay, colors.pendingFill, colors.textPrimary)
    val sparks = remember {
        val random = Random(11)
        List(SparkCount) {
            Spark(
                angle = random.nextFloat() * 2f * PI.toFloat(),
                speed = 0.7f + random.nextFloat() * 0.7f,
                size = 0.07f + random.nextFloat() * 0.08f,
                colorIndex = random.nextInt(palette.size),
            )
        }
    }
    Canvas(modifier.fillMaxSize().testTag("mineExplosion")) {
        val unit = cell.toPx()
        for ((i, blast) in chain.withIndex()) {
            val age = clock - blast.delayMillis
            if (age < 0f || age >= BurstMillis) continue
            val t = age / BurstMillis
            val eased = FastOutSlowInEasing.transform(t)
            val (cx, cy) = centers(blast.cell)
            val center = Offset(cx.toPx(), cy.toPx())
            // The flash: a disc that blooms a little and is gone by half of the way.
            if (t < 0.5f) {
                val f = t / 0.5f
                drawCircle(colors.streakDay.copy(alpha = 0.8f * (1f - f)), radius = unit * (0.35f + 0.5f * f), center = center)
            }
            // The ring: spreads a little more than the cell, thinner and fainter as it goes.
            drawCircle(
                color = colors.error.copy(alpha = 0.7f * (1f - t)),
                radius = unit * (0.4f + 0.9f * eased),
                center = center,
                style = Stroke(width = unit * 0.12f * (1f - t) + 1f),
            )
            // A few sparks, each blast turned a little differently.
            for (k in 0 until SparkCount step 3) {
                val spark = sparks[(k + i) % SparkCount]
                val distance = spark.speed * eased * unit
                val position = Offset(center.x + cos(spark.angle + i) * distance, center.y + sin(spark.angle + i) * distance)
                drawCircle(
                    color = palette[spark.colorIndex].copy(alpha = (1f - t) * 0.9f),
                    radius = unit * spark.size * (1f - 0.6f * t),
                    center = position,
                )
            }
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
