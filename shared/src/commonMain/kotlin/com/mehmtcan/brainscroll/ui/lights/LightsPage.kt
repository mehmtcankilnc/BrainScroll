package com.mehmtcan.brainscroll.ui.lights

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import brainscroll.shared.generated.resources.a11y_lights_cell_off
import brainscroll.shared.generated.resources.a11y_lights_cell_on
import brainscroll.shared.generated.resources.a11y_lights_moves
import brainscroll.shared.generated.resources.a11y_lights_par
import brainscroll.shared.generated.resources.a11y_lights_restart
import brainscroll.shared.generated.resources.game_lights
import brainscroll.shared.generated.resources.lights_hint
import brainscroll.shared.generated.resources.lights_moves
import brainscroll.shared.generated.resources.lights_par
import brainscroll.shared.generated.resources.lights_result
import brainscroll.shared.generated.resources.lights_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.lights.LightsResult
import com.mehmtcan.brainscroll.game.lights.LightsSnapshot
import com.mehmtcan.brainscroll.game.lights.LightsStatus
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
private val CellGap = 8.dp
private val MaxCell = 68.dp

/** What a light shows, for tests: 1 on, 0 off. */
internal val LightValue = SemanticsPropertyKey<Int>("LightValue")
private var SemanticsPropertyReceiver.lightValue by LightValue

/** One full-screen page of the feed: the title, the move counters, the board of lights, and the restart button or the result. */
@Composable
fun LightsPage(
    page: LightsSnapshot,
    onTap: (Int) -> LightsResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val won = state.status == LightsStatus.Won

    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 250)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.lights_moves, state.moves),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_lights_moves, state.moves),
            )
            Chip(
                text = stringResource(Res.string.lights_par, state.par),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_lights_par, state.par),
            )
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cellSize: Dp = minOf(MaxCell, (maxWidth - CellGap * (state.size - 1)) / state.size, (maxHeight - CellGap * (state.size - 1)) / state.size)
            Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                for (row in 0 until state.size) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                        for (column in 0 until state.size) {
                            val index = row * state.size + column
                            GuideMark(isGuided(index), Radius.card) {
                                Light(
                                    row = row + 1,
                                    column = column + 1,
                                    on = state.lit[index],
                                    playing = !won,
                                    size = cellSize,
                                    onClick = { if (onTap(index) != LightsResult.Ignored) haptics.perform(HapticKind.Light) },
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                LightsResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.lights_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_lights_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "lightsRestart",
                    ) { drawRestart(iconColor) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/**
 * One light. On it is a filled yellow tile with a bright bulb; off it is a dark tile with a dim ring, so the two are told
 * apart by the shape in the middle as well as the color.
 */
@Composable
private fun Light(row: Int, column: Int, on: Boolean, playing: Boolean, size: Dp, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.card
    val duration = if (LocalReduceMotion.current) 0 else Motion.BASE
    val fill by animateColorAsState(if (on) colors.pendingFill else colors.bgSurface, tween(duration, easing = Motion.Standard))
    val description = stringResource(if (on) Res.string.a11y_lights_cell_on else Res.string.a11y_lights_cell_off, row, column)

    Box(
        modifier = Modifier
            .size(size)
            .testTag("lightCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                lightValue = if (on) 1 else 0
                role = Role.Button
            }
            .background(fill, shape)
            .border((1.5f + extraBorder).dp, if (on) colors.pendingFill else colors.borderStrong.copy(alpha = 0.5f), shape)
            .then(
                if (playing) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (on) {
            Box(Modifier.size(size * 0.42f).background(colors.pendingOn, CircleShape))
        } else {
            Box(Modifier.size(size * 0.3f).border(2.dp, colors.borderStrong.copy(alpha = 0.4f), CircleShape))
        }
    }
}

@Composable
private fun LightsResultCard(page: LightsSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.lights_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.lights_result, page.state.moves, formatDuration(page.durationMs)),
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
