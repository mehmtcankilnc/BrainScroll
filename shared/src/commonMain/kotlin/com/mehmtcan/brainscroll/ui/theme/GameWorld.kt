package com.mehmtcan.brainscroll.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.game.GameKind

/**
 * The "world" of a game: the hue its page glows with behind the board. Every game gets its own, so scrolling the feed
 * feels like moving between places and not between identical gray screens. The meaning colors (correct, pending, error)
 * are untouched: the world is only the light around the board, never a state.
 */
internal val GameKind.world: Color
    get() = when (this) {
        // Not green: green is "correct" in this game, and a green room makes the empty tiles and the right letters blur together.
        GameKind.Word -> Color(0xFF7A63E8)
        GameKind.Memory -> Color(0xFFE0568F)
        GameKind.Tubes -> Color(0xFF2FA8C9)
        GameKind.Mines -> Color(0xFFD9694A)
        GameKind.Sudoku -> Color(0xFF5B7BE0)
        GameKind.Lights -> Color(0xFFE0B23A)
        GameKind.Slide -> Color(0xFF9A6BE0)
        GameKind.Target -> Color(0xFFE08A3A)
        GameKind.Pipes -> Color(0xFF3AA89A)
        GameKind.Hanoi -> Color(0xFFB0753F)
        GameKind.Queens -> Color(0xFFC45BD6)
        GameKind.Tents -> Color(0xFF6FA83A)
        GameKind.Jigsaw -> Color(0xFF4F8FE0)
        GameKind.BlockOut -> Color(0xFFD65B5B)
        GameKind.Arrows -> Color(0xFF3AA0E0)
        GameKind.Nonogram -> Color(0xFF8A7BE0)
        GameKind.AxisSum -> Color(0xFF3AC4A0)
        GameKind.BallSlide -> Color(0xFF5BC0E0)
        GameKind.ColorFlow -> Color(0xFFE05B9A)
        GameKind.TopicGroups -> Color(0xFFD0A03A)
        GameKind.Matchsticks -> Color(0xFFB7C93C)
        GameKind.Lasers -> Color(0xFFFF4F6D)
        GameKind.Untangle -> Color(0xFF5BD6C9)
        GameKind.FruitMath -> Color(0xFFFF8A4C)
        GameKind.CodeBreaker -> Color(0xFFB07CFF)
        GameKind.Rectangles -> Color(0xFF6FD672)
        GameKind.FlashGrid -> Color(0xFFFFD84F)
        GameKind.ColorSeq -> Color(0xFFB48CF2)
        GameKind.CubeFold -> Color(0xFFFF9F5A)
        GameKind.LoneNumber -> Color(0xFF8E9BFF)
        GameKind.ZerosOnes -> Color(0xFFFF9E80)
        GameKind.Lamps -> Color(0xFFF5C26B)
        GameKind.GreaterLess -> Color(0xFF7FD6C2)
        GameKind.Fences -> Color(0xFFC89F6A)
        GameKind.Mirror -> Color(0xFF6ACFD8)
        GameKind.OddOne -> Color(0xFFF28CB5)
        GameKind.CrossSum -> Color(0xFF9BD36A)
        GameKind.Line -> Color(0xFF4FB3F0)
    }

/**
 * Paints the glow of a [world] behind the content: one soft light behind the middle of the page, fading into the page
 * color before it reaches the top bar and the bottom. Cheap (one gradient), no layout effect.
 */
fun Modifier.worldGlow(world: Color): Modifier = drawBehind {
    // The light sits behind the board, not at the edges: the bar at the top and the result card at the bottom stay on a
    // nearly plain page, so their text and edges keep their contrast.
    drawRect(
        Brush.radialGradient(
            colors = listOf(world.copy(alpha = 0.26f), Color.Transparent),
            center = Offset(size.width / 2f, size.height * 0.42f),
            radius = size.height * 0.5f,
        ),
    )
}

/**
 * The next game's light coming up from the bottom edge, once the puzzle on show is done: a hint of what the next swipe
 * brings, so the feed pulls on. [alpha] is read while drawing, so it can animate without recomposing the page.
 */
fun Modifier.nextWorldPeek(world: Color, alpha: () -> Float): Modifier = drawBehind {
    val a = alpha()
    if (a <= 0f) return@drawBehind
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, world.copy(alpha = 0.34f * a)),
            startY = size.height * 0.72f,
            endY = size.height,
        ),
    )
}

/**
 * A surface with depth: a little lighter at the top than at the bottom, with a thin light edge on top. It is the one
 * step away from "flat": cards and boards that should feel like objects use it, everything else stays flat.
 */
fun Modifier.raised(color: Color, shape: CornerBasedShape): Modifier = this
    .background(
        Brush.verticalGradient(listOf(color.lighten(0.07f), color, color.darken(0.06f))),
        shape,
    )
    .border(
        1.dp,
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent)),
        shape,
    )

/**
 * The surface of a result card: the card color with a glow of the game's [world] coming up from its top, and an edge
 * in the same hue. It stands out from the page by its color and edge, not by being brighter.
 */
fun Modifier.resultSurface(base: Color, world: Color, shape: CornerBasedShape): Modifier = this
    // The card takes the height its content needs, even when the room it is given is shorter (a fixed bottom area): it
    // grows past it, centered, and never cuts its text off.
    .wrapContentHeight(Alignment.CenterVertically, unbounded = true)
    .background(base, shape)
    .background(Brush.verticalGradient(listOf(world.copy(alpha = 0.30f), world.copy(alpha = 0.04f))), shape)
    .border(1.5.dp, Brush.verticalGradient(listOf(world.copy(alpha = 0.85f), world.copy(alpha = 0.25f))), shape)

private fun Color.lighten(by: Float) = Color(
    red + (1f - red) * by, green + (1f - green) * by, blue + (1f - blue) * by, alpha,
)

private fun Color.darken(by: Float) = Color(red * (1f - by), green * (1f - by), blue * (1f - by), alpha)
