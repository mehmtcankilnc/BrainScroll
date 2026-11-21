package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_lasers_1
import brainscroll.shared.generated.resources.howto_card_lasers_2
import brainscroll.shared.generated.resources.howto_card_lasers_3
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.lasers.LaserRules
import com.mehmtcan.brainscroll.ui.lasers.LaserPaint
import com.mehmtcan.brainscroll.ui.lasers.LaserTargetColor
import com.mehmtcan.brainscroll.ui.lasers.drawLaserBoard
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world

/** The three steps of the laser mirrors card: the beam misses a target, a tap turns the mirror, every target is lit. */
internal val LasersHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_lasers_1, { t -> LasersDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_lasers_2, { t -> LasersDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_lasers_3, { t -> LasersDemo(step = 2, t = t) }),
    ),
)

private const val N = 4
private const val ROW = 1
private const val MIRROR_CELL = 6
private val Targets = setOf(2, 5)

/** The mirrors of the demo: one mirror, as it stands wrong (a `\` sends the beam down) and after the tap (a `/` sends it up). */
private fun mirrors(slash: Boolean) = List(N * N) { if (it == MIRROR_CELL) (if (slash) LaserRules.SLASH else LaserRules.BACKSLASH) else 0 }

@Composable
internal fun LasersDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    val paint = LaserPaint(
        tile = colors.bgSurface,
        mirrorTile = colors.bgRaised,
        edge = colors.borderStrong.copy(alpha = 0.35f),
        mirror = Color(0xFFA8DCFF),
        beam = GameKind.Lasers.world,
        beamCore = Color(0xFFFFE9EC),
        target = LaserTargetColor,
        emitter = GameKind.Lasers.world,
        guide = colors.pendingFill,
    )
    val wonPaint = LaserPaint(paint.tile, paint.mirrorTile, paint.edge, paint.mirror, colors.correctFill, Color(0xFFEFFFE6), paint.target, paint.emitter, paint.guide)
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val cell = minOf(size.width / (N + 1.7f), size.height / (N + 0.7f)) * 0.9f
        val origin = Offset((size.width - (N + 1.7f) * cell) / 2f + 0.85f * cell, (size.height - (N + 0.7f) * cell) / 2f + 0.35f * cell)
        fun center(c: Int) = Offset(origin.x + (c % N + 0.5f) * cell, origin.y + (c / N + 0.5f) * cell)
        val wrong = mirrors(slash = false)
        val right = mirrors(slash = true)
        when (step) {
            0 -> drawLaserBoard(N, ROW, wrong, Targets, LaserRules.trace(N, ROW, wrong), origin, cell, paint, alpha = fade)
            1 -> {
                // The hand comes to the mirror and taps it: the mirror turns and the beam runs its new way.
                val reach = demoEase(demoSegment(t, 0.1f, 0.4f))
                val press = demoSegment(t, 0.4f, 0.55f)
                val turned = t >= 0.5f
                val mirrorsNow = if (turned) right else wrong
                val run = if (turned) demoEase(demoSegment(t, 0.5f, 0.8f)) else 1f
                drawLaserBoard(N, ROW, mirrorsNow, Targets, LaserRules.trace(N, ROW, mirrorsNow), origin, cell, paint, beamProgress = run, alpha = fade)
                val start = Offset(size.width * 0.9f, size.height * 0.95f)
                val tip = center(MIRROR_CELL)
                val hand = Offset(start.x + (tip.x - start.x) * reach, start.y + (tip.y - start.y) * reach)
                drawHandCursor(handPainter, hand, cell * 0.95f, press.let { if (it in 0.01f..0.99f) it else 0f }, alpha = fade)
            }
            else -> {
                val glow = demoEase(demoSegment(t, 0.15f, 0.4f))
                drawLaserBoard(N, ROW, right, Targets, LaserRules.trace(N, ROW, right), origin, cell, if (glow > 0.5f) wonPaint else paint, alpha = fade)
            }
        }
    }
}
