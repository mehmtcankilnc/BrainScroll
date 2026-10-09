package com.mehmtcan.brainscroll.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_switch_off
import brainscroll.shared.generated.resources.a11y_switch_on
import brainscroll.shared.generated.resources.game_memory
import brainscroll.shared.generated.resources.game_lights
import brainscroll.shared.generated.resources.game_mines
import brainscroll.shared.generated.resources.game_slide
import brainscroll.shared.generated.resources.game_sudoku
import brainscroll.shared.generated.resources.game_tubes
import brainscroll.shared.generated.resources.game_word_puzzle
import brainscroll.shared.generated.resources.personalize_done
import brainscroll.shared.generated.resources.personalize_games
import brainscroll.shared.generated.resources.personalize_language
import brainscroll.shared.generated.resources.personalize_last_game
import brainscroll.shared.generated.resources.personalize_note
import brainscroll.shared.generated.resources.personalize_subtitle
import brainscroll.shared.generated.resources.personalize_title
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.feed.FeedUiState
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * The feed settings (docs/decisions.md, Ana Sayfa): which games appear and the language of the word puzzles.
 * A panel over the current tab; the scrim behind it closes it. Changes are saved at once and apply to the puzzles
 * that come next.
 */
@Composable
fun PersonalizeSheet(viewModel: FeedViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val enabled by viewModel.enabledGames.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = (state as? FeedUiState.Ready)?.feed?.language

    Box(modifier = modifier.fillMaxSize()) {
        // The scrim: tapping outside the panel closes it.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.bgSurface, Radius.card)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            BasicText(stringResource(Res.string.personalize_title), style = type.title.copy(color = colors.textPrimary))
            BasicText(stringResource(Res.string.personalize_subtitle), style = type.body.copy(color = colors.textSecondary))

            BasicText(stringResource(Res.string.personalize_games), style = type.heading.copy(color = colors.textPrimary))
            viewModel.availableGames.forEach { game ->
                val on = game in enabled
                // The last game that is on cannot be switched off: the feed would be empty.
                val locked = on && enabled.size == 1
                GameSwitch(
                    label = when (game) {
                        GameKind.Word -> stringResource(Res.string.game_word_puzzle)
                        GameKind.Memory -> stringResource(Res.string.game_memory)
                        GameKind.Tubes -> stringResource(Res.string.game_tubes)
                        GameKind.Mines -> stringResource(Res.string.game_mines)
                        GameKind.Sudoku -> stringResource(Res.string.game_sudoku)
                        GameKind.Lights -> stringResource(Res.string.game_lights)
                        GameKind.Slide -> stringResource(Res.string.game_slide)
                    },
                    on = on,
                    locked = locked,
                    onToggle = { viewModel.setGameEnabled(game, !on) },
                )
            }
            if (enabled.size == 1) {
                BasicText(stringResource(Res.string.personalize_last_game), style = type.caption.copy(color = colors.textSecondary))
            }

            BasicText(stringResource(Res.string.personalize_language), style = type.heading.copy(color = colors.textPrimary))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Language.entries.forEach { option ->
                    Chip(
                        text = option.name,
                        textColor = if (option == language) colors.textPrimary else colors.textSecondary,
                        raised = option == language,
                        selected = option == language,
                        onClick = { viewModel.setWordLanguage(option) },
                    )
                }
            }

            BasicText(stringResource(Res.string.personalize_note), style = type.caption.copy(color = colors.textSecondary))
            AppButton(text = stringResource(Res.string.personalize_done), onClick = onClose, primary = true)
        }
    }
}

/** A row with a name and a switch. The whole row is the touch target (at least 48 dp tall). */
@Composable
private fun GameSwitch(label: String, on: Boolean, locked: Boolean, onToggle: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val stateText = if (on) stringResource(Res.string.a11y_switch_on) else stringResource(Res.string.a11y_switch_off)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                stateDescription = stateText
                contentDescription = label
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !locked,
                onClick = onToggle,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = type.body.copy(color = if (locked) colors.textSecondary else colors.textPrimary),
            modifier = Modifier.weight(1f),
        )
        // The track and the dot: the position of the dot says on or off, not only the color.
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .background(if (on) colors.correctFill else colors.bgRaised, CircleShape)
                .padding(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .offset(x = if (on) 20.dp else 0.dp)
                    .background(if (on) colors.correctOn else colors.textSecondary, CircleShape),
            )
        }
    }
}
