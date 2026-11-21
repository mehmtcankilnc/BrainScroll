package com.mehmtcan.brainscroll.ui.wordle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.components.LanguageFlagButton
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.game_word_puzzle
import brainscroll.shared.generated.resources.key_enter
import brainscroll.shared.generated.resources.result_lost
import brainscroll.shared.generated.resources.result_won
import com.mehmtcan.brainscroll.game.wordle.RoundSnapshot
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import org.jetbrains.compose.resources.stringResource

/** The flag of a word puzzle: the language it shows, what a screen reader says, and what a tap does. */
class LanguageSwitch(val language: Language, val description: String, val onSwitch: () -> Unit)

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
    /** The language flag in the top right corner; null when the page has none. */
    languageSwitch: LanguageSwitch? = null,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    // The last row flips tile by tile: the result, the buzz and the confetti wait until it has turned over.
    val status = rememberSettledStatus(round.status, (Motion.FLIP + 4 * Motion.FLIP_STAGGER).toLong())
    OutcomeHaptics(status, round.id)
    // Only a puzzle that ends while it is shown gets the arrival animation; a finished one appears at once.
    val justFinished = rememberJustFinished(status)

    // When the language is switched on this page, the puzzle is made again: it shrinks away and the new one grows in.
    val reduce = LocalReduceMotion.current
    val renew = remember { Animatable(1f) }
    var shownId by remember { mutableStateOf(round.id) }
    LaunchedEffect(round.id) {
        if (shownId == round.id) return@LaunchedEffect
        shownId = round.id
        if (reduce) {
            renew.snapTo(0.3f)
            renew.animateTo(1f, tween(Motion.FAST))
        } else {
            renew.snapTo(0f)
            renew.animateTo(1f, tween(Motion.SLOW, easing = Motion.Overshoot))
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
            if (languageSwitch != null) LanguageFlagButton(languageSwitch.language, languageSwitch.description, languageSwitch.onSwitch)
        }

        val renewing = renew.value
        WordleGrid(
            round,
            modifier = Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                val scale = 0.85f + 0.15f * renewing
                scaleX = scale
                scaleY = scale
                alpha = renewing.coerceIn(0f, 1f)
            },
        )

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status != WordleStatus.Playing) {
                ResultCard(round, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
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
private fun ResultCard(round: RoundSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val won = round.status == WordleStatus.Won

    Box(
        modifier = modifier
            .fillMaxWidth()
            .resultSurface(colors.bgSurface, GameKind.Word.world, Radius.card)
            .padding(Spacing.lg),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            WordResultSummary(won, round.rows.size, round.answer, durationMs = null)
            BasicText(
                text = swipeHintText(),
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
