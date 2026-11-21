package com.mehmtcan.brainscroll.ui.home

import androidx.compose.runtime.LaunchedEffect
import com.mehmtcan.brainscroll.ui.components.NoticePill
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_difficulty
import brainscroll.shared.generated.resources.a11y_editor_search
import brainscroll.shared.generated.resources.a11y_editor_search_close
import brainscroll.shared.generated.resources.back
import brainscroll.shared.generated.resources.editor_no_match
import brainscroll.shared.generated.resources.editor_search_hint
import brainscroll.shared.generated.resources.editor_select_all
import brainscroll.shared.generated.resources.confirm_cancel
import brainscroll.shared.generated.resources.new_feed_confirm
import brainscroll.shared.generated.resources.new_feed_message
import brainscroll.shared.generated.resources.new_feed_title
import com.mehmtcan.brainscroll.ui.components.ConfirmDialog
import com.mehmtcan.brainscroll.ui.feed.FeedUiState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import brainscroll.shared.generated.resources.editor_selected
import brainscroll.shared.generated.resources.editor_next
import brainscroll.shared.generated.resources.editor_difficulty_title
import brainscroll.shared.generated.resources.editor_difficulty_subtitle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.backhandler.BackHandler
import brainscroll.shared.generated.resources.editor_title
import brainscroll.shared.generated.resources.home_start_feed
import brainscroll.shared.generated.resources.personalize_last_game
import com.mehmtcan.brainscroll.game.Difficulty
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
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun FeedEditorScreen(feedViewModel: FeedViewModel, onBack: () -> Unit, onStartFeed: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val enabled by feedViewModel.enabledGames.collectAsStateWithLifecycle()
    val wordLanguages by feedViewModel.wordLanguages.collectAsStateWithLifecycle()
    val wordLanguage = wordLanguages.first()
    val difficulties by feedViewModel.difficulties.collectAsStateWithLifecycle()

    val feedState by feedViewModel.state.collectAsStateWithLifecycle()
    val hasUnfinished = (feedState as? FeedUiState.Ready)?.feed?.hasUnfinished == true
    var confirmNewFeed by remember { mutableStateOf(false) }
    // A short message over the button when the last game is tapped: it cannot be switched off. The counter makes a second tap show it again.
    var notice by remember { mutableStateOf<Int?>(null) }
    val haptics = LocalHaptics.current
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MILLIS)
            notice = null
        }
    }
    // The search field is always there. What is typed filters the cards by name and by what the game is about (see GameSearch.kt).
    var query by remember { mutableStateOf("") }
    val names = feedViewModel.availableGames.associateWith { it.displayName() }
    val shown = feedViewModel.availableGames.filter { gameMatchesQuery(it, names.getValue(it), query) }
    // Step 0 picks the games; step 1 picks the level of the whole feed. Without a game that has levels there is no step 1.
    var step by remember { mutableIntStateOf(0) }
    val hasLevels = enabled.any { it in feedViewModel.levelledGames }
    if (!hasLevels && step == 1) step = 0
    BackHandler(enabled = step == 1) { step = 0 }
    val level = remember(difficulties) { feedViewModel.feedDifficulty() }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // The header stays in place: the back button, the title and the count on one line, the line about it below.
        Column(Modifier.fillMaxWidth().padding(start = Spacing.sm, end = Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                ChevronButton(
                    description = stringResource(Res.string.back),
                    color = colors.textPrimary,
                    onClick = { if (step == 1) step = 0 else onBack() },
                    touch = 48.dp,
                )
                BasicText(
                    text = stringResource(if (step == 1) Res.string.editor_difficulty_title else Res.string.editor_title),
                    style = type.title.copy(color = colors.textPrimary),
                    modifier = Modifier.weight(1f),
                )
                if (step == 0) BasicText(
                    text = stringResource(Res.string.editor_selected, enabled.size),
                    style = type.label.copy(color = colors.accent),
                    modifier = Modifier.padding(start = Spacing.sm).testTag("editorSelectedCount"),
                )
            }
            if (step == 1) BasicText(
                text = stringResource(Res.string.editor_difficulty_subtitle),
                style = type.body.copy(color = colors.textSecondary),
                modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.sm),
            )
        }
        if (step == 0) {
            EditorTools(
                query = query,
                onQuery = { query = it },
                // "Select all" turns on every game that is shown (the ones the search found, if it is used). It can only turn on:
                // at least one game has to stay on, and there is no "none".
                canSelectAll = shown.any { it !in enabled },
                onSelectAll = {
                    haptics.perform(HapticKind.Medium)
                    feedViewModel.addGames(shown)
                },
                modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.lg),
            )
        }
        if (step == 1) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.lg), contentAlignment = Alignment.Center) {
                FeedDifficultyStep(current = level, onSelect = feedViewModel::setFeedDifficulty)
            }
        } else LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.lg).testTag("editorList"),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (shown.isEmpty()) {
                item(key = "none") {
                    BasicText(
                        text = stringResource(Res.string.editor_no_match),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl).testTag("editorNoMatch"),
                    )
                }
            }
            // Three games to a row. Only the rows that are on the screen are built: every card has a picture, and drawing all of
            // them at once made the page slow to open.
            itemsIndexed(shown.chunked(GAMES_PER_ROW), key = { _, row -> row.first().id }) { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { column, game ->
                        val on = game in enabled
                        GameCard(
                            game = game,
                            wordLanguage = wordLanguage,
                            on = on,
                            onClick = {
                                if (on && enabled.size == 1) {
                                    // The last game that is on can not be switched off: the feed would be empty. Say so.
                                    haptics.perform(HapticKind.Warning)
                                    notice = (notice ?: 0) + 1
                                } else {
                                    feedViewModel.setGameEnabled(game, !on)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // A short last row keeps the cards their width.
                    repeat(GAMES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            item(key = "end") { Spacer(Modifier.height(Spacing.sm)) }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
            if (step == 0 && hasLevels) {
                AppButton(
                    stringResource(Res.string.editor_next),
                    { step = 1 },
                    modifier = Modifier.testTag("editorNext"),
                    primary = true,
                )
            } else {
                AppButton(
                    stringResource(Res.string.home_start_feed),
                    { if (hasUnfinished) confirmNewFeed = true else onStartFeed() },
                    modifier = Modifier.testTag("startFeed"),
                    primary = true,
                )
            }
        }
    }
    if (notice != null) {
        NoticePill(
            text = stringResource(Res.string.personalize_last_game),
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = Spacing.lg, end = Spacing.lg, bottom = NOTICE_BOTTOM).testTag("editorNotice"),
            onDismiss = { notice = null },
        )
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
 * One game, small enough for three to a row: its picture with the round mark in its corner, and the name under it (up to two
 * lines, never cut). A game that is in the feed has a green outline and a green mark with a check; one that is not has a quiet
 * outline, a dimmed picture and an empty ring.
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
            .border(if (on) 1.5.dp else 1.dp, if (on) colors.accent else colors.borderSubtle, Radius.card)
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
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box {
            GameThumbnailImage(game, wordLanguage, Modifier.fillMaxWidth().aspectRatio(1.25f).graphicsLayer { alpha = if (on) 1f else 0.5f })
            CheckMark(on, Modifier.align(Alignment.TopEnd).padding(Spacing.xs))
        }
        BasicText(
            text = name,
            style = type.caption.copy(color = if (on) colors.textPrimary else colors.textSecondary, textAlign = TextAlign.Center),
            minLines = 2,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
        )
    }
}


/** How many games the editor puts in a row. */
private const val GAMES_PER_ROW = 3

/** How long the message about the last game stays, and how far over the bottom it floats (clear of the button). */
private const val NOTICE_MILLIS = 2_000L
private val NOTICE_BOTTOM = 88.dp

private val CHECK_SIZE = 18.dp

/** The round mark in the corner of a card: an empty ring when the game is not in the feed, a ring with a solid center when it is. */
@Composable
private fun CheckMark(on: Boolean, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    Box(
        modifier = modifier
            .size(CHECK_SIZE)
            .background(if (on) colors.accent.copy(alpha = 0.18f) else Color.Transparent, CircleShape)
            .border(1.5.dp, if (on) colors.accent else colors.borderStrong.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Box(Modifier.size(CHECK_SIZE / 2).background(colors.accent, CircleShape))
    }
}

/**
 * The row under the title: the search field (always open, a magnifier on its left; the cards are filtered as it is typed) and a "select
 * all" icon button beside it. Select all is dimmed when every game shown is already on; it never turns anything off.
 */
@Composable
private fun EditorTools(
    query: String,
    onQuery: (String) -> Unit,
    canSelectAll: Boolean,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val searchLabel = stringResource(Res.string.a11y_editor_search)
    val clearLabel = stringResource(Res.string.a11y_editor_search_close)
    val selectAll = stringResource(Res.string.editor_select_all)
    val hint = stringResource(Res.string.editor_search_hint)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(colors.bgSurface, Radius.control)
                .border(1.dp, colors.borderSubtle, Radius.control)
                .padding(start = Spacing.md),
        ) {
            Canvas(Modifier.size(20.dp)) {
                val ink = colors.textSecondary
                drawCircle(ink, size.width * 0.32f, androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.42f), style = Stroke(2.dp.toPx()))
                drawLine(ink, androidx.compose.ui.geometry.Offset(size.width * 0.66f, size.height * 0.66f), androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.92f), 2.dp.toPx(), StrokeCap.Round)
            }
            Box(Modifier.weight(1f).padding(horizontal = Spacing.sm), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = type.body.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.textPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().testTag("editorSearchField").semantics { contentDescription = searchLabel },
                    decorationBox = { inner ->
                        if (query.isEmpty()) BasicText(hint, style = type.body.copy(color = colors.textDisabled), maxLines = 1)
                        inner()
                    },
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("editorSearchClear")
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onQuery("") }
                        .semantics { contentDescription = clearLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(12.dp)) {
                        val ink = colors.textSecondary
                        drawLine(ink, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), 2.dp.toPx(), StrokeCap.Round)
                        drawLine(ink, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(0f, size.height), 2.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
        }
        // Select all: a square with a check in it, in the accent colour while it can still turn something on.
        val ink = if (canSelectAll) colors.accent else colors.textDisabled
        Box(
            modifier = Modifier
                .size(48.dp)
                .testTag("editorSelectAll")
                .clip(Radius.control)
                .border(1.dp, if (canSelectAll) colors.accent else colors.borderSubtle, Radius.control)
                .clickable(enabled = canSelectAll, role = Role.Button, onClick = onSelectAll)
                .semantics { contentDescription = selectAll; if (!canSelectAll) disabled() },
            contentAlignment = Alignment.Center,
        ) {
            // A filled rounded square with one check in it (the accent while it can still turn something on).
            val fill = if (canSelectAll) colors.accent else colors.borderSubtle
            val tick = if (canSelectAll) colors.accentOn else colors.textDisabled
            Box(Modifier.size(24.dp).background(fill, androidx.compose.foundation.shape.RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(14.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.1f, size.height * 0.55f)
                        lineTo(size.width * 0.4f, size.height * 0.82f)
                        lineTo(size.width * 0.92f, size.height * 0.2f)
                    }
                    drawPath(path, tick, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}
