@file:OptIn(ExperimentalLayoutApi::class)

package com.mehmtcan.brainscroll.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.daily_retry
import brainscroll.shared.generated.resources.filter_everyone
import brainscroll.shared.generated.resources.filter_friends
import brainscroll.shared.generated.resources.friends_accept
import brainscroll.shared.generated.resources.friends_add
import brainscroll.shared.generated.resources.friends_add_code
import brainscroll.shared.generated.resources.friends_add_name
import brainscroll.shared.generated.resources.friends_ask
import brainscroll.shared.generated.resources.friends_cancel
import brainscroll.shared.generated.resources.friends_code_hint
import brainscroll.shared.generated.resources.friends_copied
import brainscroll.shared.generated.resources.friends_copy
import brainscroll.shared.generated.resources.friends_decline
import brainscroll.shared.generated.resources.friends_invite_message
import brainscroll.shared.generated.resources.friends_list
import brainscroll.shared.generated.resources.friends_my_code
import brainscroll.shared.generated.resources.friends_name_hint
import brainscroll.shared.generated.resources.friends_none
import brainscroll.shared.generated.resources.friends_remove
import brainscroll.shared.generated.resources.friends_requests
import brainscroll.shared.generated.resources.friends_sent
import brainscroll.shared.generated.resources.gate_offline
import brainscroll.shared.generated.resources.gate_sign_in
import brainscroll.shared.generated.resources.gate_username_hint
import brainscroll.shared.generated.resources.gate_username_save
import brainscroll.shared.generated.resources.gate_username_text
import brainscroll.shared.generated.resources.gate_username_title
import brainscroll.shared.generated.resources.kind_current
import brainscroll.shared.generated.resources.kind_longest
import brainscroll.shared.generated.resources.ranks_empty
import brainscroll.shared.generated.resources.ranks_empty_friends
import brainscroll.shared.generated.resources.ranks_failed
import brainscroll.shared.generated.resources.ranks_friends
import brainscroll.shared.generated.resources.ranks_guesses
import brainscroll.shared.generated.resources.ranks_loading
import brainscroll.shared.generated.resources.ranks_speed
import brainscroll.shared.generated.resources.ranks_speed_caption
import brainscroll.shared.generated.resources.ranks_stale
import brainscroll.shared.generated.resources.ranks_streak_caption
import brainscroll.shared.generated.resources.ranks_streaks
import brainscroll.shared.generated.resources.ranks_you
import brainscroll.shared.generated.resources.scope_all_time
import brainscroll.shared.generated.resources.scope_today
import brainscroll.shared.generated.resources.sign_in_apple
import brainscroll.shared.generated.resources.sign_in_google
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.social.Board
import com.mehmtcan.brainscroll.social.DailyRow
import com.mehmtcan.brainscroll.social.DailyScope
import com.mehmtcan.brainscroll.social.StreakKind
import com.mehmtcan.brainscroll.social.StreakRow
import com.mehmtcan.brainscroll.social.inviteLink
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** What the ranks tab needs from the account, as plain callbacks. */
data class RanksSignIn(
    val canSignInWithApple: Boolean,
    val onSignInWithGoogle: () -> Unit,
    val onSignInWithApple: () -> Unit,
)

/** The ranks tab: speed and streak tables, and the friends. */
@Composable
fun RanksScreen(viewModel: SocialViewModel, signIn: RanksSignIn, modifier: Modifier = Modifier) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.onShown() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { SectionChips(ui, viewModel) }

        // Who may do what: anonymous players only look, friends need a username.
        when (ui.access) {
            Access.Offline -> item { Card { Body(stringResource(Res.string.gate_offline)) } }
            Access.Anonymous -> item { SignInCard(signIn) }
            Access.NeedsUsername -> item { UsernameCard(viewModel) }
            Access.Checking, Access.Ready -> Unit
        }

        when (ui.section) {
            Section.Speed -> speedSection(ui, viewModel)
            Section.Streak -> streakSection(ui, viewModel)
            Section.Friends -> friendsSection(ui, viewModel)
        }
    }
}

// --- Chips ---

@Composable
private fun SectionChips(ui: SocialUiState, viewModel: SocialViewModel) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Choice(stringResource(Res.string.ranks_speed), ui.section == Section.Speed) { viewModel.selectSection(Section.Speed) }
        Choice(stringResource(Res.string.ranks_streaks), ui.section == Section.Streak) { viewModel.selectSection(Section.Streak) }
        Choice(stringResource(Res.string.ranks_friends), ui.section == Section.Friends) { viewModel.selectSection(Section.Friends) }
    }
}

@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    Chip(
        text = text,
        textColor = if (selected) colors.textPrimary else colors.textSecondary,
        raised = selected,
        selected = selected,
        onClick = onClick,
    )
}

/** Everyone or friends only. Friends only exists for players who can have friends. */
@Composable
private fun ScopeChips(ui: SocialUiState, viewModel: SocialViewModel) {
    if (ui.access != Access.Ready) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Choice(stringResource(Res.string.filter_everyone), !ui.friendsOnly) { viewModel.selectFriendsOnly(false) }
        Choice(stringResource(Res.string.filter_friends), ui.friendsOnly) { viewModel.selectFriendsOnly(true) }
    }
}

// --- Speed and streak tables ---

private fun LazyListScope.speedSection(ui: SocialUiState, viewModel: SocialViewModel) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Language.entries.forEach { language ->
                    Choice(language.name, ui.language == language) { viewModel.selectLanguage(language) }
                }
                Spacer(Modifier.width(Spacing.sm))
                Choice(stringResource(Res.string.scope_today), ui.scope == DailyScope.Today) { viewModel.selectScope(DailyScope.Today) }
                Choice(stringResource(Res.string.scope_all_time), ui.scope == DailyScope.AllTime) { viewModel.selectScope(DailyScope.AllTime) }
            }
            ScopeChips(ui, viewModel)
            Caption(stringResource(Res.string.ranks_speed_caption))
        }
    }
    boardItems(ui, ui.speed, viewModel) { row ->
        BoardLine(
            rank = row.rank,
            name = row.username,
            isMe = row.isMe,
            value = "${stringResource(Res.string.ranks_guesses, row.guesses)} · ${formatDuration(row.durationMs)}",
        )
    }
}

private fun LazyListScope.streakSection(ui: SocialUiState, viewModel: SocialViewModel) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Choice(stringResource(Res.string.kind_current), ui.kind == StreakKind.Current) { viewModel.selectKind(StreakKind.Current) }
                Choice(stringResource(Res.string.kind_longest), ui.kind == StreakKind.Longest) { viewModel.selectKind(StreakKind.Longest) }
            }
            ScopeChips(ui, viewModel)
            Caption(stringResource(Res.string.ranks_streak_caption))
        }
    }
    boardItems(ui, ui.streak, viewModel) { row ->
        BoardLine(rank = row.rank, name = row.username, isMe = row.isMe, value = row.value.toString())
    }
}

/** The status line, the top rows, and my own row when I am below the top. */
private fun <R> LazyListScope.boardItems(
    ui: SocialUiState,
    board: Board<R>?,
    viewModel: SocialViewModel,
    line: @Composable (R) -> Unit,
) {
    item {
        when {
            board == null && ui.failed -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Body(stringResource(Res.string.ranks_failed))
                AppButton(stringResource(Res.string.daily_retry), viewModel::refresh)
            }
            board == null -> Body(stringResource(Res.string.ranks_loading))
            ui.stale -> Caption(stringResource(Res.string.ranks_stale))
        }
    }
    if (board == null) return
    if (board.rows.isEmpty()) {
        item { Body(stringResource(if (ui.friendsOnly) Res.string.ranks_empty_friends else Res.string.ranks_empty)) }
        return
    }
    items(board.rows) { row -> line(row) }
    val me = board.me
    if (me != null && board.rows.none { it == me }) {
        item { Caption("…") }
        item { line(me) }
    }
}

@Composable
private fun BoardLine(rank: Int, name: String, isMe: Boolean, value: String) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .background(if (isMe) colors.bgRaised else colors.bgSurface, Radius.control)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        BasicText(rank.toString(), modifier = Modifier.width(32.dp), style = type.label.copy(color = colors.textSecondary))
        BasicText(
            text = if (isMe) "$name (${stringResource(Res.string.ranks_you)})" else name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = type.body.copy(color = colors.textPrimary, fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal),
        )
        BasicText(value, style = type.label.copy(color = colors.textPrimary))
    }
}

// --- Gates ---

@Composable
private fun SignInCard(signIn: RanksSignIn) {
    Card {
        Body(stringResource(Res.string.gate_sign_in))
        AppButton(stringResource(Res.string.sign_in_google), signIn.onSignInWithGoogle, primary = true)
        if (signIn.canSignInWithApple) AppButton(stringResource(Res.string.sign_in_apple), signIn.onSignInWithApple)
    }
}

@Composable
private fun UsernameCard(viewModel: SocialViewModel) {
    var name by remember { mutableStateOf("") }
    Card {
        Heading(stringResource(Res.string.gate_username_title))
        Caption(stringResource(Res.string.gate_username_text))
        TextAction(
            value = name,
            onChange = { name = it.take(MAX_NAME) },
            hint = stringResource(Res.string.gate_username_hint),
            actionLabel = stringResource(Res.string.gate_username_save),
            onAction = { viewModel.saveUsername(name) },
        )
    }
}

// --- Friends ---

private fun LazyListScope.friendsSection(ui: SocialUiState, viewModel: SocialViewModel) {
    if (ui.access != Access.Ready) {
        // The gate card above says what to do first; nothing else to show here.
        return
    }
    item { InviteCard(ui, viewModel) }
    item {
        var code by remember { mutableStateOf("") }
        Card {
            Heading(stringResource(Res.string.friends_add_code))
            TextAction(
                value = code,
                onChange = { code = it.take(MAX_CODE) },
                hint = stringResource(Res.string.friends_code_hint),
                actionLabel = stringResource(Res.string.friends_add),
                onAction = { viewModel.addFriendByCode(code); code = "" },
            )
        }
    }
    item {
        var name by remember { mutableStateOf("") }
        Card {
            Heading(stringResource(Res.string.friends_add_name))
            TextAction(
                value = name,
                onChange = { name = it.take(MAX_NAME) },
                hint = stringResource(Res.string.friends_name_hint),
                actionLabel = stringResource(Res.string.friends_ask),
                onAction = { viewModel.sendRequest(name); name = "" },
            )
        }
    }

    val friends = ui.friends
    if (friends == null) {
        item { if (ui.failed) Body(stringResource(Res.string.ranks_failed)) else Body(stringResource(Res.string.ranks_loading)) }
        return
    }
    if (ui.stale) item { Caption(stringResource(Res.string.ranks_stale)) }

    if (friends.incoming.isNotEmpty()) {
        item { Heading(stringResource(Res.string.friends_requests)) }
        items(friends.incoming) { name ->
            PersonLine(name) {
                SmallAction(stringResource(Res.string.friends_accept), primary = true) { viewModel.respond(name, true) }
                SmallAction(stringResource(Res.string.friends_decline)) { viewModel.respond(name, false) }
            }
        }
    }
    item { Heading(stringResource(Res.string.friends_list)) }
    if (friends.friends.isEmpty()) item { Body(stringResource(Res.string.friends_none)) }
    items(friends.friends) { name ->
        PersonLine(name) { SmallAction(stringResource(Res.string.friends_remove)) { viewModel.removeFriend(name) } }
    }
    if (friends.outgoing.isNotEmpty()) {
        item { Heading(stringResource(Res.string.friends_sent)) }
        items(friends.outgoing) { name ->
            PersonLine(name) { SmallAction(stringResource(Res.string.friends_cancel)) { viewModel.cancelRequest(name) } }
        }
    }
}

@Composable
private fun InviteCard(ui: SocialUiState, viewModel: SocialViewModel) {
    val code = ui.inviteCode ?: return
    val colors = BrainScrollTheme.colors
    val clipboard = LocalClipboardManager.current
    val message = stringResource(Res.string.friends_invite_message, code, inviteLink(code))
    var copied by remember { mutableStateOf(false) }
    Card {
        Heading(stringResource(Res.string.friends_my_code))
        BasicText(code, style = BrainScrollTheme.typography.display.copy(color = colors.correctFill, letterSpacing = 4.sp))
        AppButton(stringResource(Res.string.friends_copy), { clipboard.setText(AnnotatedString(message)); copied = true })
        if (copied) Caption(stringResource(Res.string.friends_copied))
    }
}

@Composable
private fun PersonLine(name: String, actions: @Composable () -> Unit) {
    val colors = BrainScrollTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.control)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        BasicText(
            text = name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = BrainScrollTheme.typography.body.copy(color = colors.textPrimary),
        )
        actions()
    }
}

@Composable
private fun SmallAction(text: String, primary: Boolean = false, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    // The pill is 36 dp, the touch area 48 dp.
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 36.dp)
                .background(if (primary) colors.correctFill else colors.bgRaised, Radius.pill)
                .padding(horizontal = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text, style = BrainScrollTheme.typography.label.copy(color = if (primary) colors.correctOn else colors.textPrimary))
        }
    }
}

// --- Small building blocks ---

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrainScrollTheme.colors.bgSurface, Radius.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) { content() }
}

@Composable
private fun Heading(text: String) =
    BasicText(text, style = BrainScrollTheme.typography.heading.copy(color = BrainScrollTheme.colors.textPrimary))

@Composable
private fun Body(text: String) =
    BasicText(text, style = BrainScrollTheme.typography.body.copy(color = BrainScrollTheme.colors.textSecondary))

@Composable
private fun Caption(text: String) =
    BasicText(text, style = BrainScrollTheme.typography.caption.copy(color = BrainScrollTheme.colors.textSecondary))

/** A text field with a button next to it. */
@Composable
private fun TextAction(value: String, onChange: (String) -> Unit, hint: String, actionLabel: String, onAction: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(colors.bgPage, Radius.control)
                .padding(horizontal = Spacing.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = type.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.textPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) BasicText(hint, style = type.body.copy(color = colors.textDisabled), maxLines = 1)
                    inner()
                },
            )
        }
        Box(modifier = Modifier.width(96.dp)) { AppButton(actionLabel, onAction, primary = true) }
    }
}

private const val MAX_NAME = 16
private const val MAX_CODE = 8
