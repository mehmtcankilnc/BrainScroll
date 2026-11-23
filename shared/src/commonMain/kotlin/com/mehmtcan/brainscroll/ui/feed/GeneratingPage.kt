package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.feed_generating
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.world
import org.jetbrains.compose.resources.stringResource

/** The skeleton only shows after this long: a board that is there in a blink never flashes a placeholder. */
private const val SHOW_AFTER_MILLIS = 150L

private const val SKELETON_SIDE = 5
private val MaxSkeleton = 280.dp
private val SkeletonGap = 6.dp

/**
 * The page of a puzzle whose board is still being made (see `PendingRound`): a board-shaped skeleton in the colour of the game that
 * breathes, and a line saying the puzzle is being prepared. [kind] is null while the feed itself is loading. The page can be swiped
 * away like any other; when the board is there it takes this place.
 */
@Composable
fun GeneratingPage(kind: GameKind?, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val tint = kind?.world ?: colors.textSecondary
    val reduceMotion = LocalReduceMotion.current

    // Nothing for the first moments: a short wait is not worth showing.
    var due by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(SHOW_AFTER_MILLIS)
        due = true
    }
    val appear by animateFloatAsState(if (due) 1f else 0f, tween(if (reduceMotion) 0 else 220), label = "generatingAppear")
    val breath = if (reduceMotion) 0.5f else {
        val transition = rememberInfiniteTransition(label = "generatingBreath")
        val value by transition.animateFloat(0.25f, 0.75f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "breath")
        value
    }
    val text = stringResource(Res.string.feed_generating)

    Box(
        modifier = modifier.fillMaxSize().testTag("generatingPage").semantics {
            contentDescription = text
            liveRegion = LiveRegionMode.Polite
        },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg).alpha(appear),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side: Dp = minOf(MaxSkeleton, maxWidth)
                val cell = (side - SkeletonGap * (SKELETON_SIDE - 1)) / SKELETON_SIDE
                Column(verticalArrangement = Arrangement.spacedBy(SkeletonGap)) {
                    for (row in 0 until SKELETON_SIDE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(SkeletonGap)) {
                            for (column in 0 until SKELETON_SIDE) {
                                // A wave of light runs over the cells (the cells next to each other differ a little), so it reads as busy.
                                val shade = (breath + 0.08f * ((row + column) % 3)).coerceIn(0.15f, 0.85f)
                                Box(Modifier.size(cell).background(tint.copy(alpha = 0.22f * shade + 0.06f), Radius.tile))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            BasicText(
                text = text,
                style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
    }
}
