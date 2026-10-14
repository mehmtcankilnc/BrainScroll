package com.mehmtcan.brainscroll.ui.slide

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.guideHighlight
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_slide_moves
import brainscroll.shared.generated.resources.a11y_slide_restart
import brainscroll.shared.generated.resources.a11y_slide_tile
import brainscroll.shared.generated.resources.a11y_slide_tile_home
import brainscroll.shared.generated.resources.game_slide
import brainscroll.shared.generated.resources.slide_hint
import brainscroll.shared.generated.resources.slide_moves
import brainscroll.shared.generated.resources.slide_result
import brainscroll.shared.generated.resources.slide_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.slide.SlideResult
import com.mehmtcan.brainscroll.game.slide.SlideSnapshot
import com.mehmtcan.brainscroll.game.slide.SlideState
import com.mehmtcan.brainscroll.game.slide.SlideStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val TileGap = 8.dp
private val MaxTile = 96.dp

/** The number on a tile and the cell it is in, for tests. */
internal val SlideTileNumber = SemanticsPropertyKey<Int>("SlideTileNumber")
internal val SlideTileCell = SemanticsPropertyKey<Int>("SlideTileCell")
private var SemanticsPropertyReceiver.tileNumber by SlideTileNumber
private var SemanticsPropertyReceiver.tileCell by SlideTileCell

/** One full-screen page of the feed: the title, the move counter, the tiles, and the restart button or the result. */
@Composable
fun SlidePage(
    page: SlideSnapshot,
    onSlide: (Int) -> SlideResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val won = state.status == SlideStatus.Won

    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 250)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Chip(
            text = stringResource(Res.string.slide_moves, state.moves),
            textColor = colors.textSecondary,
            description = stringResource(Res.string.a11y_slide_moves, state.moves),
        )
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val tile: Dp = minOf(
                MaxTile,
                (maxWidth - TileGap * (state.cols - 1)) / state.cols,
                (maxHeight - TileGap * (state.rows - 1)) / state.rows,
            )
            Board(
                state = state,
                tile = tile,
                playing = !won,
                onSlide = { cell -> if (onSlide(cell) != SlideResult.Ignored) haptics.perform(HapticKind.Light) },
            )
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                SlideResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.slide_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_slide_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "slideRestart",
                    ) { drawRestart(iconColor) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/**
 * The tiles, each placed by its own offset so that it glides to its new cell when it slides (the tiles are keyed by
 * their number, so a tile keeps its animation as its cell changes).
 */
@Composable
private fun Board(state: SlideState, tile: Dp, playing: Boolean, onSlide: (Int) -> Unit) {
    val width = tile * state.cols + TileGap * (state.cols - 1)
    val height = tile * state.rows + TileGap * (state.rows - 1)
    Box(Modifier.size(width, height)) {
        for (number in 1 until state.cols * state.rows) {
            key(number) {
                val cell = state.tiles.indexOf(number)
                Tile(
                    number = number,
                    cell = cell,
                    row = cell / state.cols,
                    column = cell % state.cols,
                    home = state.isHome(cell),
                    size = tile,
                    playing = playing,
                    guided = isGuided(cell),
                    onClick = { onSlide(cell) },
                )
            }
        }
    }
}

@Composable
private fun Tile(number: Int, cell: Int, row: Int, column: Int, home: Boolean, size: Dp, playing: Boolean, guided: Boolean, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val density = LocalDensity.current
    val duration = if (LocalReduceMotion.current) 0 else Motion.BASE
    val x by animateDpAsState((size + TileGap) * column, tween(duration, easing = Motion.Standard))
    val y by animateDpAsState((size + TileGap) * row, tween(duration, easing = Motion.Standard))
    val description = stringResource(if (home) Res.string.a11y_slide_tile_home else Res.string.a11y_slide_tile, number, row + 1, column + 1)

    Box(
        modifier = Modifier
            .offset(x, y)
            .size(size)
            .testTag("slideTile")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                tileNumber = number
                tileCell = cell
                role = Role.Button
            }
            .background(colors.bgSurface, Radius.card)
            .border((1.5f + extraBorder).dp, if (home) colors.correctFill.copy(alpha = 0.6f) else colors.borderStrong.copy(alpha = 0.5f), Radius.card)
            .guideHighlight(guided, Radius.card)
            .then(
                if (playing) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // The number follows the tile, not the text size setting: it has to fit its tile either way.
        BasicText(
            text = number.toString(),
            style = BrainScrollTheme.typography.display.copy(
                color = if (home) colors.correctFill else colors.textPrimary,
                fontSize = with(density) { (size * 0.42f).toSp() },
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun SlideResultCard(page: SlideSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.slide_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.slide_result, page.state.moves, formatDuration(page.durationMs)),
                style = type.body.copy(color = colors.textPrimary),
            )
            BasicText(
                text = swipeHintText(),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}
