package com.mehmtcan.brainscroll.ui.memory

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_memory_mistakes
import brainscroll.shared.generated.resources.a11y_memory_moves
import brainscroll.shared.generated.resources.game_memory
import brainscroll.shared.generated.resources.memory_hint
import brainscroll.shared.generated.resources.memory_lost
import brainscroll.shared.generated.resources.memory_mistakes
import brainscroll.shared.generated.resources.memory_moves
import brainscroll.shared.generated.resources.memory_result
import brainscroll.shared.generated.resources.memory_won
import brainscroll.shared.generated.resources.swipe_hint
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.memory.FlipResult
import com.mehmtcan.brainscroll.game.memory.MemorySnapshot
import com.mehmtcan.brainscroll.game.memory.MemoryStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.howto.HowToPlayButton
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the word puzzle's keyboard, so the board does not jump between games. */
private val BottomAreaHeight = 152.dp
private val CardGap = 8.dp
private val MaxCard = 88.dp
private const val COLUMNS = 4

/** How long two wrong cards stay face up before they turn back, unless the next tap hides them sooner. */
private const val MISMATCH_MILLIS = 900L

/** One full-screen page of the feed: the title, the counters, the board of cards, and the hint or the result. */
@Composable
fun MemoryPage(
    page: MemorySnapshot,
    /** Turns over a card and says what happened, so this page can buzz the right way. */
    onFlip: (Int) -> FlipResult,
    onCloseMismatch: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onHowToPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state

    // The same buzz and arrival rules as the word puzzle: only a board that ends while it is shown celebrates.
    val wordleStatus = when (state.status) {
        MemoryStatus.Playing -> WordleStatus.Playing
        MemoryStatus.Won -> WordleStatus.Won
        MemoryStatus.Lost -> WordleStatus.Lost
    }
    OutcomeHaptics(wordleStatus)
    val justFinished = rememberJustFinished(wordleStatus)

    // Two wrong cards are looked at for a moment and then turn back by themselves.
    LaunchedEffect(state.mismatchShown, state.moves) {
        if (state.mismatchShown && state.status == MemoryStatus.Playing) {
            delay(MISMATCH_MILLIS)
            onCloseMismatch()
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        BasicText(stringResource(Res.string.game_memory), style = type.heading.copy(color = colors.textPrimary))
        HowToPlayButton(onHowToPlay)
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.memory_moves, state.moves),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_memory_moves, state.moves),
            )
            Chip(
                text = stringResource(Res.string.memory_mistakes, state.mistakes, state.maxMistakes),
                // Close to the limit the counter turns red, and says so with its words too.
                textColor = if (state.mistakes >= state.maxMistakes - 2) colors.error else colors.textSecondary,
                description = stringResource(Res.string.a11y_memory_mistakes, state.mistakes, state.maxMistakes),
            )
        }
        Spacer(Modifier.height(Spacing.md))

        Board(page, onFlip = { card ->
            when (onFlip(card)) {
                FlipResult.Opened -> haptics.perform(HapticKind.Light)
                FlipResult.Match -> haptics.perform(HapticKind.Medium)
                FlipResult.Mismatch -> haptics.perform(HapticKind.Warning)
                FlipResult.Ignored -> Unit
            }
        }, modifier = Modifier.weight(1f).fillMaxWidth())

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (page.isFinished) {
                MemoryResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                BasicText(
                    text = stringResource(Res.string.memory_hint),
                    style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** 4 columns of equal cards, 8 dp apart (docs/design.md section 4), as large as the room allows. */
@Composable
private fun Board(page: MemorySnapshot, onFlip: (Int) -> Unit, modifier: Modifier) {
    val state = page.state
    val rows = state.layout.size / COLUMNS
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val byWidth = (maxWidth - CardGap * (COLUMNS - 1)) / COLUMNS
        val byHeight = (maxHeight - CardGap * (rows - 1)) / rows
        val cardSize: Dp = minOf(MaxCard, byWidth, byHeight)

        Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
            for (row in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(CardGap)) {
                    for (column in 0 until COLUMNS) {
                        val index = row * COLUMNS + column
                        MemoryCard(
                            position = index + 1,
                            symbol = state.layout[index],
                            face = when {
                                index in state.matched -> CardFace.Matched
                                index in state.faceUp -> CardFace.Up
                                // A lost game shows the cards that were left, so the player sees where they were.
                                state.status == MemoryStatus.Lost -> CardFace.Up
                                else -> CardFace.Down
                            },
                            size = cardSize,
                            onClick = { onFlip(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryResultCard(page: MemorySnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val won = page.state.status == MemoryStatus.Won

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(if (won) Res.string.memory_won else Res.string.memory_lost),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = if (won) colors.correctFill else colors.textPrimary, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.memory_result, page.state.moves, formatDuration(page.durationMs)),
                style = type.body.copy(color = colors.textPrimary),
            )
            BasicText(
                text = "↑  " + stringResource(Res.string.swipe_hint),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}
