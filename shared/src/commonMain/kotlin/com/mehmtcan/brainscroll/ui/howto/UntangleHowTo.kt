package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_untangle_start
import brainscroll.shared.generated.resources.howto_untangle_tech
import brainscroll.shared.generated.resources.howto_card_untangle_1
import brainscroll.shared.generated.resources.howto_card_untangle_2
import brainscroll.shared.generated.resources.howto_card_untangle_3
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.untangle.UNTANGLE_BOARD
import com.mehmtcan.brainscroll.game.untangle.UntangleRules
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.ui.untangle.UntanglePaint
import com.mehmtcan.brainscroll.ui.untangle.drawUntangle

/** The three steps of the untangle card: two lines cross, a dot is dragged, no line crosses another. */
internal val UntangleHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_untangle_1, { t -> UntangleDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_untangle_2, { t -> UntangleDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_untangle_3, { t -> UntangleDemo(step = 2, t = t) }),
        HowToStep(Res.string.howto_untangle_tech, { t -> UntangleDemo(step = 3, t = t) }, cycleMillis = 4800, title = Res.string.howto_title_solve),
        HowToStep(Res.string.howto_untangle_start, { t -> UntangleDemo(step = 4, t = t) }, cycleMillis = 4800, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

// Four dots joined in a ring: the two diagonal lines of the tangle cross. Dragging dot 0 below dot 3 puts them right.
private val Edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)
private val Tangled = listOf(200, 200, 800, 800, 800, 200, 200, 800)
private val MovedDot = 0
private val Target = 200 to 950

@Composable
internal fun UntangleDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    val paint = UntanglePaint(GameKind.Untangle.world, colors.error, colors.bgRaised, colors.textPrimary, colors.pendingFill)
    val wonPaint = UntanglePaint(colors.correctFill, colors.error, colors.bgRaised, colors.textPrimary, colors.pendingFill)
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val side = minOf(size.width, size.height) * 0.9f
        val origin = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        val unit = side / UNTANGLE_BOARD
        val radius = side * 0.035f
        val line = side * 0.011f
        // Step 0: the tangle as it is. Step 1: the hand drags dot 0 to its new place. Step 2: it is there and the lines are right.
        // Steps 3 and 4 drag another dot: the top right one to the bottom (how to solve), the bottom left one up into the corner (first step).
        val drags = step == 1 || step == 3 || step == 4
        val moved = when (step) { 3 -> 2; 4 -> 3; else -> MovedDot }
        val target = when (step) { 3 -> 800 to 950; 4 -> 200 to 50; else -> Target }
        val progress = if (drags) demoEase(demoSegment(t, 0.2f, 0.65f)) else if (step == 2) 1f else 0f
        val positions = Tangled.toMutableList().also {
            it[2 * moved] = (Tangled[2 * moved] + (target.first - Tangled[2 * moved]) * progress).toInt()
            it[2 * moved + 1] = (Tangled[2 * moved + 1] + (target.second - Tangled[2 * moved + 1]) * progress).toInt()
        }
        val crossing = UntangleRules.crossingEdges(positions, Edges)
        val solved = (step == 2 && t > 0.3f) || (step >= 3 && t > 0.7f)
        drawUntangle(positions, Edges, crossing, origin, side, if (solved) wonPaint else paint, radius, line, lifted = if (drags && progress in 0.01f..0.99f) moved else -1, won = solved, alpha = fade)
        if (drags) {
            val press = demoSegment(t, 0.1f, 0.2f)
            val tip = Offset(origin.x + positions[2 * moved] * unit, origin.y + positions[2 * moved + 1] * unit)
            drawHandCursor(handPainter, tip, side * 0.12f, press.let { if (it in 0.01f..0.99f) it else 0f }, alpha = fade)
        }
    }
}
