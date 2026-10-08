package com.mehmtcan.brainscroll.ui.memory

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_card_down
import brainscroll.shared.generated.resources.a11y_card_matched
import brainscroll.shared.generated.resources.a11y_card_up
import brainscroll.shared.generated.resources.symbol_circle
import brainscroll.shared.generated.resources.symbol_diamond
import brainscroll.shared.generated.resources.symbol_heart
import brainscroll.shared.generated.resources.symbol_moon
import brainscroll.shared.generated.resources.symbol_plus
import brainscroll.shared.generated.resources.symbol_square
import brainscroll.shared.generated.resources.symbol_star
import brainscroll.shared.generated.resources.symbol_triangle
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.popOnAppear
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What a card shows (docs/design.md section 7). */
enum class CardFace { Down, Up, Matched }

/**
 * One memory card. Face down: a surface with a faint brain. Face up: the shape with a yellow ("pending") outline.
 * Matched: filled green with the shape in one dark color, so a match is told by fill and shape, not by color alone.
 * Turning over is a flip around the vertical axis, instant when the player asked for less motion.
 *
 * [position] is 1 based and only used for the screen reader ("Face-down card, position 7").
 */
@Composable
fun MemoryCard(
    position: Int,
    symbol: CardSymbol,
    face: CardFace,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val reduce = LocalReduceMotion.current
    val density = LocalDensity.current
    val extraBorder = if (LocalHighContrast.current) 1f else 0f

    // 0 = face down, 180 = face up. A card shown for the first time (a board restored from storage) starts where
    // it belongs and does not play the flip.
    val target = if (face == CardFace.Down) 0f else 180f
    val rotation = remember { Animatable(target) }
    LaunchedEffect(target, reduce) {
        if (reduce) rotation.snapTo(target) else rotation.animateTo(target, tween(Motion.BASE + 100, easing = Motion.Standard))
    }
    val showFront = rotation.value > 90f

    val description = cardDescription(position, symbol, face)
    Box(
        modifier = modifier
            .size(size)
            .testTag("memoryCard")
            .popOnAppear(face == CardFace.Matched)
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12f * density.density
            }
            .background(
                when {
                    !showFront -> colors.bgSurface
                    face == CardFace.Matched -> colors.correctFill
                    else -> colors.bgSurface
                },
                Radius.control,
            )
            .then(
                if (showFront && face == CardFace.Up) Modifier.border((2.5f + extraBorder).dp, colors.pendingFill, Radius.control)
                else if (!showFront) Modifier.border((1f + extraBorder).dp, colors.borderSubtle, Radius.control)
                else Modifier,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (showFront) {
            // The front is drawn mirrored back, because the whole card was turned by 180 degrees.
            Box(Modifier.fillMaxSize().padding(size / 5).graphicsLayer { rotationY = 180f }) {
                CardShape(
                    symbol = symbol,
                    color = if (face == CardFace.Matched) colors.correctOn else symbolColor(symbol),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            FaintBrain(Modifier.size(size / 2))
        }
    }
}

/** The mark on the back of a card: two soft lobes with a line between them, in a very quiet color. */
@Composable
private fun FaintBrain(modifier: Modifier) {
    val color = BrainScrollTheme.colors.textDisabled.copy(alpha = 0.45f)
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(color, radius = r * 0.52f, center = Offset(size.width / 2 - r * 0.42f, size.height / 2))
        drawCircle(color, radius = r * 0.52f, center = Offset(size.width / 2 + r * 0.42f, size.height / 2))
    }
}

@Composable
private fun cardDescription(position: Int, symbol: CardSymbol, face: CardFace): String {
    val name = stringResource(symbolName(symbol))
    return when (face) {
        CardFace.Down -> stringResource(Res.string.a11y_card_down, position)
        CardFace.Up -> stringResource(Res.string.a11y_card_up, name, position)
        CardFace.Matched -> stringResource(Res.string.a11y_card_matched, name, position)
    }
}

internal fun symbolName(symbol: CardSymbol): StringResource = when (symbol) {
    CardSymbol.Circle -> Res.string.symbol_circle
    CardSymbol.Triangle -> Res.string.symbol_triangle
    CardSymbol.Star -> Res.string.symbol_star
    CardSymbol.Heart -> Res.string.symbol_heart
    CardSymbol.Diamond -> Res.string.symbol_diamond
    CardSymbol.Square -> Res.string.symbol_square
    CardSymbol.Plus -> Res.string.symbol_plus
    CardSymbol.Moon -> Res.string.symbol_moon
}
