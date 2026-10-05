package com.mehmtcan.brainscroll.ui.wordle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.game_word_puzzle
import brainscroll.shared.generated.resources.key_enter
import brainscroll.shared.generated.resources.result_lost
import brainscroll.shared.generated.resources.result_won
import brainscroll.shared.generated.resources.swipe_hint
import com.mehmtcan.brainscroll.game.wordle.RoundSnapshot
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the keyboard (3 keys of 48 dp plus gaps), reused by the result card so the grid does not jump. */
private val BottomAreaHeight = 152.dp

/** One full-screen page of the feed: title, grid, and either the keyboard or the result. */
@Composable
fun WordlePage(
    round: RoundSnapshot,
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        BasicText(
            text = stringResource(Res.string.game_word_puzzle),
            style = type.heading.copy(color = colors.textPrimary),
        )
        Spacer(Modifier.height(Spacing.md))

        WordleGrid(round, modifier = Modifier.weight(1f).fillMaxWidth())

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (round.isFinished) {
                ResultCard(round, isFavorite, onToggleFavorite)
            } else {
                WordleKeyboard(
                    language = round.language,
                    letterStates = round.letterStates,
                    enterLabel = stringResource(Res.string.key_enter),
                    onLetter = onLetter,
                    onBackspace = onBackspace,
                    onEnter = onEnter,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun ResultCard(round: RoundSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val won = round.status == WordleStatus.Won

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .padding(Spacing.lg),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BasicText(
                text = if (won) stringResource(Res.string.result_won, round.rows.size)
                else stringResource(Res.string.result_lost, round.answer.orEmpty()),
                style = type.title.copy(color = if (won) colors.correctFill else colors.textPrimary, textAlign = TextAlign.Center),
            )
            BasicText(
                text = "↑  " + stringResource(Res.string.swipe_hint),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(
            filled = isFavorite,
            onClick = onToggleFavorite,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}
