package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_ballslide_1
import brainscroll.shared.generated.resources.howto_card_ballslide_2
import brainscroll.shared.generated.resources.howto_card_ballslide_3
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.ballslide.BallDir
import com.mehmtcan.brainscroll.game.ballslide.BallSlideGenerator
import com.mehmtcan.brainscroll.game.ballslide.BallSlidePuzzle
import com.mehmtcan.brainscroll.game.ballslide.BallSlideSolver
import com.mehmtcan.brainscroll.ui.ballslide.ballSlideWallColor
import com.mehmtcan.brainscroll.ui.ballslide.drawWall
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the ball slide card: swipe and the ball slides to a wall, it paints what it passes, paint every cell. */
internal val BallSlideHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_ballslide_1, { t -> BallSlideDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_ballslide_2, { t -> BallSlideDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_ballslide_3, { t -> BallSlideDemo(step = 2, t = t) }, cycleMillis = 6400),
    ),
)

// A real easy board and the shortest way that paints all of it: the picture plays the first slides of it.
private val Board by lazy { BallSlideGenerator.generate(seed = 3, difficulty = Difficulty.Easy).puzzle }
private val Way: List<Int> by lazy { BallSlideSolver.shortest(Board, Board.start, 1L shl Board.start).orEmpty() }

@Composable
internal fun BallSlideDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    val puzzle: BallSlidePuzzle = Board
    val n = puzzle.size
    val moves = when (step) {
        0 -> Way.take(1)
        1 -> Way.take(2)
        else -> Way
    }
    // When each slide starts, and how long it takes: the slides follow each other with a pause for the hand.
    val gap = if (step == 2) 0.17f else 0.3f
    val starts = moves.indices.map { 0.18f + it * gap }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val side = minOf(size.width, size.height) * 0.84f
        val cell = side / n
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        fun center(c: Int) = Offset(left + (c % n + 0.5f) * cell, top + (c / n + 0.5f) * cell)
        val inset = cell * 0.03f
        val corner = CornerRadius(cell * 0.18f)

        // The slides: where the ball is before each, the cells it passes, and how far each has gone at time t.
        var ball = puzzle.start
        val painted = BooleanArray(n * n).also { it[puzzle.start] = true }
        var ballAt = center(ball)
        val keyframes = ArrayList<Pair<Float, Offset>>()
        keyframes += 0f to Offset(size.width, size.height)
        var pressAt = -1f
        moves.forEachIndexed { k, dir ->
            val path = puzzle.slide(ball, dir)
            val duration = (path.size * 0.045f).coerceIn(0.08f, 0.2f)
            val start = starts[k]
            // The hand: on the ball just before the slide, then it swipes the way the ball goes.
            val from = center(ball)
            val swipeEnd = Offset(from.x + BallDir.dColumn[dir] * cell * 1.3f, from.y + BallDir.dRow[dir] * cell * 1.3f)
            keyframes += (start - 0.07f) to from
            keyframes += (start + 0.07f) to swipeEnd
            if (t >= start - 0.01f && t <= start + 0.06f) pressAt = demoSegment(t, start - 0.01f, start + 0.06f)
            if (t >= start + duration) {
                path.forEach { painted[it] = true }
                ball = path.last()
                ballAt = center(ball)
            } else if (t >= start) {
                val p = demoEase(demoSegment(t, start, start + duration)) * path.size
                val whole = p.toInt().coerceIn(0, path.size - 1)
                val a = if (whole == 0) center(ball) else center(path[whole - 1])
                val b = center(path[whole])
                val f = p - whole
                ballAt = Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
                for (i in 0 until whole) painted[path[i]] = true
            }
        }

        for (c in 0 until n * n) {
            val o = Offset(left + (c % n) * cell + inset, top + (c / n) * cell + inset)
            val box = Size(cell - 2 * inset, cell - 2 * inset)
            if (puzzle.walls[c]) {
                drawWall(o, box.width, ballSlideWallColor(colors), colors.bgPage.copy(alpha = 0.6f), corner, fade)
            } else {
                drawRoundRect(colors.bgSurface.copy(alpha = fade), o, box, corner)
                if (painted[c]) drawRoundRect(colors.correctFill.copy(alpha = 0.16f * fade), o, box, corner)
                drawRoundRect(colors.borderStrong.copy(alpha = 0.35f * fade), o, box, corner, style = Stroke(width = 1.5.dp.toPx()))
            }
        }
        drawCircle(colors.pendingFill.copy(alpha = fade), cell * 0.3f, ballAt)
        drawCircle(Color.White.copy(alpha = 0.55f * fade), cell * 0.09f, Offset(ballAt.x - cell * 0.1f, ballAt.y - cell * 0.1f))

        // The hand follows its keyframes (a straight way from one to the next, slowing at both ends).
        var hand = keyframes.first().second
        for (i in 1 until keyframes.size) {
            val (t0, p0) = keyframes[i - 1]
            val (t1, p1) = keyframes[i]
            if (t >= t0) {
                val f = demoEase(demoSegment(t, t0, t1))
                hand = Offset(p0.x + (p1.x - p0.x) * f, p0.y + (p1.y - p0.y) * f)
            }
        }
        drawHandCursor(handPainter, hand, cell * 0.95f, if (pressAt >= 0f) pressAt else 0f, alpha = fade)
    }
}
