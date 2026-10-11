package com.mehmtcan.brainscroll.ui.pipes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_pipes_connected
import brainscroll.shared.generated.resources.a11y_pipes_dir_e
import brainscroll.shared.generated.resources.a11y_pipes_dir_n
import brainscroll.shared.generated.resources.a11y_pipes_dir_s
import brainscroll.shared.generated.resources.a11y_pipes_dir_w
import brainscroll.shared.generated.resources.a11y_pipes_empty
import brainscroll.shared.generated.resources.a11y_pipes_kind_corner
import brainscroll.shared.generated.resources.a11y_pipes_kind_cross
import brainscroll.shared.generated.resources.a11y_pipes_kind_end
import brainscroll.shared.generated.resources.a11y_pipes_kind_straight
import brainscroll.shared.generated.resources.a11y_pipes_kind_tee
import brainscroll.shared.generated.resources.a11y_pipes_moves
import brainscroll.shared.generated.resources.a11y_pipes_open_end
import brainscroll.shared.generated.resources.a11y_pipes_par
import brainscroll.shared.generated.resources.a11y_pipes_restart
import brainscroll.shared.generated.resources.a11y_pipes_tile
import brainscroll.shared.generated.resources.game_pipes
import brainscroll.shared.generated.resources.pipes_hint
import brainscroll.shared.generated.resources.pipes_moves
import brainscroll.shared.generated.resources.pipes_par
import brainscroll.shared.generated.resources.pipes_result
import brainscroll.shared.generated.resources.pipes_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator
import com.mehmtcan.brainscroll.game.pipes.PipesResult
import com.mehmtcan.brainscroll.game.pipes.PipesSnapshot
import com.mehmtcan.brainscroll.game.pipes.PipesStatus
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
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val TileGap = 3.dp
private val MaxTile = 64.dp

/** What a tile shows, for tests: its open sides and the sides that are open ends. */
internal val PipesTileMask = SemanticsPropertyKey<Int>("PipesTileMask")
internal val PipesTileOpenEnds = SemanticsPropertyKey<Int>("PipesTileOpenEnds")

/** The angle the tile is drawn at right now, in degrees (a quarter turn is 90), for tests of the turn. */
internal val PipesTileAngle = SemanticsPropertyKey<Float>("PipesTileAngle")
private var SemanticsPropertyReceiver.tileMask by PipesTileMask
private var SemanticsPropertyReceiver.tileOpenEnds by PipesTileOpenEnds
private var SemanticsPropertyReceiver.tileAngle by PipesTileAngle

/** One full-screen page of the feed: the title, the move counters, the board of pipes, and the restart button or the result. */
@Composable
fun PipesPage(
    page: PipesSnapshot,
    onTurn: (Int) -> PipesResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val won = state.status == PipesStatus.Won

    val status = if (won) WordleStatus.Won else WordleStatus.Playing
    OutcomeHaptics(status)
    val justFinished = rememberJustFinished(status)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.pipes_moves, state.moves),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_pipes_moves, state.moves),
            )
            Chip(
                text = stringResource(Res.string.pipes_par, state.par),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_pipes_par, state.par),
            )
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val tile: Dp = minOf(MaxTile, (maxWidth - TileGap * (state.size - 1)) / state.size, (maxHeight - TileGap * (state.size - 1)) / state.size)
            val labels = PipesLabels(
                kinds = listOf(
                    stringResource(Res.string.a11y_pipes_kind_end),
                    stringResource(Res.string.a11y_pipes_kind_straight),
                    stringResource(Res.string.a11y_pipes_kind_corner),
                    stringResource(Res.string.a11y_pipes_kind_tee),
                    stringResource(Res.string.a11y_pipes_kind_cross),
                ),
                directions = listOf(
                    stringResource(Res.string.a11y_pipes_dir_n),
                    stringResource(Res.string.a11y_pipes_dir_e),
                    stringResource(Res.string.a11y_pipes_dir_s),
                    stringResource(Res.string.a11y_pipes_dir_w),
                ),
                connected = stringResource(Res.string.a11y_pipes_connected),
                openEnd = stringResource(Res.string.a11y_pipes_open_end),
            )
            Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
                for (row in 0 until state.size) {
                    Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                        for (column in 0 until state.size) {
                            val index = row * state.size + column
                            GuideMark(isGuided(index), Radius.tile) {
                                PipeTile(
                                    row = row + 1,
                                    column = column + 1,
                                    mask = state.tiles[index],
                                    matched = state.matched[index],
                                    labels = labels,
                                    playing = !won,
                                    size = tile,
                                    onClick = { if (onTurn(index) != PipesResult.Ignored) haptics.perform(HapticKind.Light) },
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (won) {
                PipesResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.pipes_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_pipes_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "pipesRestart",
                    ) { drawRestart(iconColor) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** How long a quarter turn takes. */
private const val TURN_MILLIS = 150

/**
 * What a tile remembers to turn smoothly: the pipes in the position they are drawn in ([base]) and how many quarter
 * turns the tile has been turned since ([quarterTurns], which only goes up). The pipes on show are [base] turned
 * [quarterTurns] times. [update] is called with the tile of the moment; it is safe to call again with the same tile.
 */
private class TileTurn(mask: Int) {
    var base = mask
        private set
    var quarterTurns = 0
        private set
    private var seen = mask

    fun update(mask: Int) {
        if (mask == seen) return
        if (mask == PipesGenerator.rotate(seen)) {
            quarterTurns++
        } else {
            // Not a turn (a restart): the tile now shows these pipes at the angle it has, so the starting position moves.
            base = backBy(mask, quarterTurns)
        }
        seen = mask
    }

    /** Sides (of the tile as it is on show) in the starting position, to draw them under the angle. */
    fun baseSides(sides: Int): Int = backBy(sides, quarterTurns)

    private fun backBy(tile: Int, turns: Int): Int {
        var result = tile
        repeat((4 - turns % 4) % 4) { result = PipesGenerator.rotate(result) }
        return result
    }
}

/** The words a screen reader says for a tile, read once for the whole board. */
private class PipesLabels(val kinds: List<String>, val directions: List<String>, val connected: String, val openEnd: String)

internal val Sides = listOf(PipesGenerator.NORTH, PipesGenerator.EAST, PipesGenerator.SOUTH, PipesGenerator.WEST)

/** One tile. It turns a quarter clockwise when it is tapped; a restart changes it without a turn. */
@Composable
private fun PipeTile(row: Int, column: Int, mask: Int, matched: Int, labels: PipesLabels, playing: Boolean, size: Dp, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val reduce = LocalReduceMotion.current
    val openEnds = mask and matched.inv()

    // The turn. The pipes are always drawn in a fixed starting position and the whole tile is turned by an angle that
    // glides to the next quarter. So the first frame of a turn still shows the old position: the new one is never seen
    // before the turn starts. A change that is not a tap's turn (a restart) moves the starting position instead, so the
    // angle stays where it is and the tile simply shows the new pipes.
    val turn = remember { TileTurn(mask) }
    turn.update(mask)
    val angle by animateFloatAsState(
        targetValue = turn.quarterTurns * 90f,
        animationSpec = if (reduce) snap() else tween(TURN_MILLIS, easing = Motion.Standard),
    )

    val sides = Sides.indices.filter { mask and Sides[it] != 0 }
    val kind = when (sides.size) {
        0 -> -1
        1 -> 0
        2 -> if (sides[1] - sides[0] == 2) 1 else 2
        3 -> 3
        else -> 4
    }
    val description = if (kind < 0) {
        stringResource(Res.string.a11y_pipes_empty, row, column)
    } else {
        stringResource(
            Res.string.a11y_pipes_tile,
            row, column, labels.kinds[kind], sides.joinToString(", ") { labels.directions[it] },
            if (openEnds == 0) labels.connected else labels.openEnd,
        )
    }

    Box(
        modifier = Modifier
            .size(size)
            .testTag("pipeTile")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                tileMask = mask
                tileOpenEnds = openEnds
                tileAngle = angle
                role = Role.Button
            }
            .background(colors.bgSurface, Radius.tile)
            .border((1f + extraBorder).dp, colors.borderStrong.copy(alpha = 0.3f), Radius.tile)
            .then(
                if (playing) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size).graphicsLayer { rotationZ = angle }) {
            drawPipes(turn.base, turn.baseSides(matched), colors.correctFill, colors.borderStrong, colors.textSecondary)
        }
    }
}

/**
 * The pipes of a tile: a short arm for every open side, with a hub in the middle. An arm that meets its neighbor is
 * [joined] and runs to the edge. An open end stops short and has a small ring at its tip, so it can be told from a joined
 * arm without the color.
 */
internal fun DrawScope.drawPipes(mask: Int, matched: Int, joined: Color, open: Color, hub: Color) {
    val s = size.minDimension
    val center = Offset(s / 2, s / 2)
    val width = s * 0.16f
    val directions = listOf(Offset(0f, -1f), Offset(1f, 0f), Offset(0f, 1f), Offset(-1f, 0f))
    Sides.forEachIndexed { index, side ->
        if (mask and side == 0) return@forEachIndexed
        val direction = directions[index]
        if (matched and side != 0) {
            drawLine(joined, center, center + direction * (s / 2), strokeWidth = width, cap = StrokeCap.Round)
        } else {
            drawLine(open, center, center + direction * (s * 0.30f), strokeWidth = width, cap = StrokeCap.Round)
            drawCircle(open, radius = s * 0.07f, center = center + direction * (s * 0.42f), style = Stroke(width = s * 0.04f))
        }
    }
    drawCircle(hub, radius = s * 0.09f, center = center)
}

@Composable
private fun PipesResultCard(page: PipesSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.pipes_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.pipes_result, page.state.moves, formatDuration(page.durationMs)),
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
