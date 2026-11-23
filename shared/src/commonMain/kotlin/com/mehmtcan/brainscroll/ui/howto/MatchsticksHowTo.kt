package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_matchsticks_start
import brainscroll.shared.generated.resources.howto_matchsticks_tech
import brainscroll.shared.generated.resources.howto_card_matchsticks_1
import brainscroll.shared.generated.resources.howto_card_matchsticks_2
import brainscroll.shared.generated.resources.howto_card_matchsticks_3
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.matchsticks.MatchGenerator
import com.mehmtcan.brainscroll.game.matchsticks.MatchRules
import com.mehmtcan.brainscroll.ui.matchsticks.MatchHead
import com.mehmtcan.brainscroll.ui.matchsticks.MatchPaint
import com.mehmtcan.brainscroll.ui.matchsticks.WoodStick
import com.mehmtcan.brainscroll.ui.matchsticks.drawMatchBoard
import com.mehmtcan.brainscroll.ui.matchsticks.drawMatchStick
import com.mehmtcan.brainscroll.ui.matchsticks.matchSlotLine
import com.mehmtcan.brainscroll.ui.matchsticks.matchWidthUnits
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the matchstick card: tap the stick, tap the empty place, the sum is right. */
internal val MatchsticksHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_matchsticks_1, { t -> MatchsticksDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_matchsticks_2, { t -> MatchsticksDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_matchsticks_3, { t -> MatchsticksDemo(step = 2, t = t) }),
        HowToStep(Res.string.howto_matchsticks_tech, { t -> MatchsticksDemo(step = 3, t = t) }, cycleMillis = 6400, title = Res.string.howto_title_solve),
        HowToStep(Res.string.howto_matchsticks_start, { t -> MatchsticksDemo(step = 4, t = t) }, cycleMillis = 4800, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

/** A fixed wrong sum and its one way to put it right, so the demo always shows the same thing. */
private val Demo by lazy { MatchGenerator.generate(4, Difficulty.Medium) }

@Composable
internal fun MatchsticksDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    val board = Demo
    val cells = board.puzzle.cells
    val fixed = MatchRules.apply(cells, board.solution)
    val paint = MatchPaint(
        stick = WoodStick,
        head = MatchHead,
        fixed = WoodStick.copy(alpha = 0.45f),
        held = colors.streakAnswer,
        heldHead = Color(0xFFFF9F43),
        ghost = colors.textSecondary.copy(alpha = 0.8f),
    )
    val wonPaint = MatchPaint(colors.correctFill, colors.correctFill.copy(alpha = 0.6f), colors.correctFill.copy(alpha = 0.45f), paint.held, paint.heldHead, paint.ghost)
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val totalUnits = matchWidthUnits(cells.size) + 16f
        val unit = minOf(size.width / totalUnits, size.height / 120f) * 0.92f
        val origin = Offset((size.width - totalUnits * unit) / 2f + 8f * unit, (size.height - 108f * unit) / 2f + 6f * unit)
        val from = matchSlotLine(board.solution.from)
        val to = matchSlotLine(board.solution.to)
        fun middle(line: FloatArray, p: Float = 0f) = Offset(
            origin.x + ((line[0] + line[2]) / 2f) * unit,
            origin.y + ((line[1] + line[3]) / 2f) * unit + p,
        )
        when (step) {
            0 -> {
                // The hand comes to the stick and presses it: the stick turns yellow and the empty places show.
                val reach = demoEase(demoSegment(t, 0.1f, 0.35f))
                val press = demoSegment(t, 0.35f, 0.5f)
                val lifted = if (press > 0.3f) board.solution.from else null
                drawMatchBoard(cells, origin, unit, paint, lifted = lifted, showGhosts = lifted != null, alpha = fade)
                val start = Offset(size.width * 0.92f, size.height * 0.95f)
                val tip = middle(from)
                val hand = Offset(start.x + (tip.x - start.x) * reach, start.y + (tip.y - start.y) * reach)
                drawHandCursor(handPainter, hand, size.width * 0.2f, press.let { if (it in 0.01f..0.99f) it else 0f }, alpha = fade)
            }
            1 -> {
                // The stick is up; the hand takes it to the empty place and lets go: the stick flies there.
                val travel = demoEase(demoSegment(t, 0.15f, 0.5f))
                val press = demoSegment(t, 0.5f, 0.65f)
                val land = demoEase(demoSegment(t, 0.55f, 0.8f))
                if (land == 0f) {
                    drawMatchBoard(cells, origin, unit, paint, lifted = board.solution.from, showGhosts = true, alpha = fade)
                } else {
                    drawMatchBoard(fixed, origin, unit, paint, hidden = setOf(board.solution.to), alpha = fade)
                    val line = FloatArray(4) { from[it] + (to[it] - from[it]) * land }
                    drawMatchStick(line, origin, unit, paint.held.copy(alpha = fade), paint.heldHead.copy(alpha = fade))
                }
                val a = middle(from)
                val b = middle(to)
                val hand = Offset(a.x + (b.x - a.x) * travel, a.y + (b.y - a.y) * travel)
                drawHandCursor(handPainter, hand, size.width * 0.2f, press.let { if (it in 0.01f..0.99f) it else 0f }, alpha = fade)
            }
            // The whole move in one picture: take the stick, carry it to the empty place, the sum is right and the sticks turn green.
            3 -> {
                val reach = demoEase(demoSegment(t, 0.04f, 0.2f))
                val travel = demoEase(demoSegment(t, 0.4f, 0.64f))
                val land = demoEase(demoSegment(t, 0.66f, 0.8f))
                val green = demoEase(demoSegment(t, 0.86f, 0.96f))
                val a = middle(from)
                val b = middle(to)
                val start = Offset(size.width * 0.92f, size.height * 0.95f)
                if (t < 0.28f) {
                    drawMatchBoard(cells, origin, unit, paint, alpha = fade)
                } else if (land == 0f) {
                    drawMatchBoard(cells, origin, unit, paint, lifted = board.solution.from, showGhosts = true, alpha = fade)
                } else {
                    drawMatchBoard(fixed, origin, unit, if (green > 0.5f) wonPaint else paint, hidden = if (land < 1f) setOf(board.solution.to) else emptySet(), alpha = fade)
                    if (land < 1f) {
                        val line = FloatArray(4) { from[it] + (to[it] - from[it]) * land }
                        drawMatchStick(line, origin, unit, paint.held.copy(alpha = fade), paint.heldHead.copy(alpha = fade))
                    }
                }
                val hand = if (t < 0.4f) Offset(start.x + (a.x - start.x) * reach, start.y + (a.y - start.y) * reach) else Offset(a.x + (b.x - a.x) * travel, a.y + (b.y - a.y) * travel)
                val press = if (t in 0.26f..0.4f || t in 0.64f..0.7f) 1f else 0f
                drawHandCursor(handPainter, hand, size.width * 0.2f, press, alpha = fade)
            }
            // The first move: the stick that has to go is lifted and the places that can take it show; the hand shows where it goes.
            4 -> {
                val travel = demoEase(demoSegment(t, 0.3f, 0.75f))
                drawMatchBoard(cells, origin, unit, paint, lifted = board.solution.from, showGhosts = true, alpha = fade)
                val a = middle(from)
                val b = middle(to)
                val hand = Offset(a.x + (b.x - a.x) * travel, a.y + (b.y - a.y) * travel)
                drawHandCursor(handPainter, hand, size.width * 0.2f, if (t in 0.1f..0.3f) 1f else 0f, alpha = fade)
            }
            else -> {
                // The sum is right: the sticks turn green.
                val green = demoEase(demoSegment(t, 0.2f, 0.45f))
                drawMatchBoard(fixed, origin, unit, if (green > 0.5f) wonPaint else paint, alpha = fade)
            }
        }
    }
}
