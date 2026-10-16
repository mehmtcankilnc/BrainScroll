package com.mehmtcan.brainscroll.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.back
import brainscroll.shared.generated.resources.confirm_cancel
import brainscroll.shared.generated.resources.editor_language
import brainscroll.shared.generated.resources.new_feed_confirm
import brainscroll.shared.generated.resources.new_feed_message
import brainscroll.shared.generated.resources.new_feed_title
import com.mehmtcan.brainscroll.ui.components.ConfirmDialog
import com.mehmtcan.brainscroll.ui.feed.FeedUiState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import brainscroll.shared.generated.resources.editor_language_hint
import brainscroll.shared.generated.resources.editor_selected
import brainscroll.shared.generated.resources.editor_subtitle
import brainscroll.shared.generated.resources.editor_title
import brainscroll.shared.generated.resources.home_start_feed
import brainscroll.shared.generated.resources.personalize_last_game
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.ChevronButton
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** What the player sees for a language: its own name, the same in every app language. */
fun Language.nativeName(): String = when (this) {
    Language.TR -> "Türkçe"
    Language.EN -> "English"
}

/**
 * The page that builds the feed (docs/decisions.md "Ana Sayfa yenilemesi"): every game as a card with its picture, the
 * ones in the feed marked, the language of the word games, and the button that starts the feed. A change applies at
 * once, to the puzzles that are still to come. The last game that is on can not be switched off.
 */
@Composable
fun FeedEditorScreen(feedViewModel: FeedViewModel, onBack: () -> Unit, onStartFeed: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val enabled by feedViewModel.enabledGames.collectAsStateWithLifecycle()
    val wordLanguages by feedViewModel.wordLanguages.collectAsStateWithLifecycle()
    val wordLanguage = wordLanguages.first()

    val feedState by feedViewModel.state.collectAsStateWithLifecycle()
    val hasUnfinished = (feedState as? FeedUiState.Ready)?.feed?.hasUnfinished == true
    var confirmNewFeed by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // The header stays in place: the back button, the title and the count on one line, the line about it below.
        Column(Modifier.fillMaxWidth().padding(start = Spacing.sm, end = Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                ChevronButton(
                    description = stringResource(Res.string.back),
                    color = colors.textPrimary,
                    onClick = onBack,
                    touch = 48.dp,
                )
                BasicText(
                    text = stringResource(Res.string.editor_title),
                    style = type.title.copy(color = colors.textPrimary),
                    modifier = Modifier.weight(1f),
                )
                BasicText(
                    text = stringResource(Res.string.editor_selected, enabled.size),
                    style = type.label.copy(color = colors.correctFill),
                    modifier = Modifier.padding(start = Spacing.sm).testTag("editorSelectedCount"),
                )
            }
            BasicText(
                text = stringResource(Res.string.editor_subtitle),
                style = type.body.copy(color = colors.textSecondary),
                modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.md),
            )
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Two games to a row.
            feedViewModel.availableGames.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { game ->
                        val on = game in enabled
                        GameCard(
                            game = game,
                            wordLanguage = wordLanguage,
                            on = on,
                            // The last game that is on can not be switched off: the feed would be empty.
                            onClick = if (on && enabled.size == 1) null else ({ feedViewModel.setGameEnabled(game, !on) }),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            if (GameKind.Word in enabled || GameKind.Crossword in enabled) {
                Column(Modifier.fillMaxWidth().padding(top = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    BasicText(stringResource(Res.string.editor_language), style = type.body.copy(color = colors.textPrimary))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Language.entries.forEach { option ->
                            val chosen = option in wordLanguages
                            WordLanguageChip(
                                language = option,
                                chosen = chosen,
                                // The last language that is on stays: there would be no word puzzles.
                                onClick = if (chosen && wordLanguages.size == 1) null else ({ feedViewModel.setWordLanguageEnabled(option, !chosen) }),
                            )
                        }
                    }
                    BasicText(stringResource(Res.string.editor_language_hint), style = type.caption.copy(color = colors.textSecondary))
                }
            }
            if (enabled.size == 1) {
                BasicText(stringResource(Res.string.personalize_last_game), style = type.caption.copy(color = colors.textSecondary))
            }
            Spacer(Modifier.height(Spacing.sm))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
            AppButton(
                stringResource(Res.string.home_start_feed),
                { if (hasUnfinished) confirmNewFeed = true else onStartFeed() },
                modifier = Modifier.testTag("startFeed"),
                primary = true,
            )
        }
    }
    if (confirmNewFeed) {
        ConfirmDialog(
            title = stringResource(Res.string.new_feed_title),
            message = stringResource(Res.string.new_feed_message),
            confirmText = stringResource(Res.string.new_feed_confirm),
            cancelText = stringResource(Res.string.confirm_cancel),
            onConfirm = { confirmNewFeed = false; onStartFeed() },
            onCancel = { confirmNewFeed = false },
        )
    }
    }
}

/**
 * One game: its picture and, under it, the round mark and the name together in the middle of the line. A game that is in
 * the feed has a green outline and a green mark with a check; one that is not has a quiet outline, a dimmed picture and
 * an empty ring.
 */
@Composable
private fun GameCard(game: GameKind, wordLanguage: Language, on: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val name = game.displayName()
    Column(
        modifier = modifier
            .testTag("gameCard")
            .clip(Radius.card)
            .background(colors.bgSurface)
            .border(if (on) 1.5.dp else 1.dp, if (on) colors.correctFill else colors.borderSubtle, Radius.card)
            .semantics(mergeDescendants = true) {
                contentDescription = name
                selected = on
                role = Role.Checkbox
            }
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Checkbox, onClick = onClick)
                else Modifier,
            )
            .padding(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        GameThumbnail(game, wordLanguage, Modifier.fillMaxWidth().aspectRatio(1.25f).graphicsLayer { alpha = if (on) 1f else 0.5f })
        // The mark and the name are one group in the middle of the line, under the middle of the picture.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
        ) {
            CheckMark(on)
            BasicText(
                text = name,
                style = type.label.copy(color = if (on) colors.textPrimary else colors.textSecondary, lineHeight = 20.sp),
                maxLines = 1,
            )
        }
    }
}

private val CHECK_SIZE = 18.dp

/** The round mark in the corner of a card: a ring when the game is not in the feed, a green disc with a check when it is. */
@Composable
private fun CheckMark(on: Boolean, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    Box(
        modifier = modifier
            .size(CHECK_SIZE)
            .then(
                if (on) Modifier.background(colors.correctFill, CircleShape)
                else Modifier.border(1.5.dp, colors.borderStrong.copy(alpha = 0.7f), CircleShape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (on) {
            val color = colors.correctOn
            Canvas(Modifier.size(10.dp)) {
                val path = Path().apply {
                    moveTo(0.12f * size.width, 0.55f * size.height)
                    lineTo(0.4f * size.width, 0.8f * size.height)
                    lineTo(0.9f * size.width, 0.2f * size.height)
                }
                drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

/** A language of the word puzzles: filled green when it is in the feed. Several can be on at the same time. */
@Composable
private fun WordLanguageChip(language: Language, chosen: Boolean, onClick: (() -> Unit)?) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Box(
        modifier = Modifier
            .testTag("language" + language.name)
            .clip(Radius.pill)
            .background(if (chosen) colors.correctFill else colors.bgSurface)
            .border(1.dp, if (chosen) colors.correctFill else colors.borderSubtle, Radius.pill)
            .semantics { selected = chosen }
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Checkbox, onClick = onClick)
                else Modifier,
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm + 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(language.nativeName(), style = type.label.copy(color = if (chosen) colors.correctOn else colors.textSecondary))
    }
}
