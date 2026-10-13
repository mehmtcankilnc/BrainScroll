package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.account_anonymous
import brainscroll.shared.generated.resources.account_apple
import brainscroll.shared.generated.resources.account_google
import brainscroll.shared.generated.resources.account_offline
import brainscroll.shared.generated.resources.account_title
import brainscroll.shared.generated.resources.confirm_cancel
import brainscroll.shared.generated.resources.delete_account
import brainscroll.shared.generated.resources.delete_cancel
import brainscroll.shared.generated.resources.delete_confirm
import brainscroll.shared.generated.resources.delete_text
import brainscroll.shared.generated.resources.delete_title
import brainscroll.shared.generated.resources.gate_username_hint
import brainscroll.shared.generated.resources.sign_in_apple
import brainscroll.shared.generated.resources.sign_in_google
import brainscroll.shared.generated.resources.sign_out
import brainscroll.shared.generated.resources.signout_confirm_text
import brainscroll.shared.generated.resources.signout_confirm_title
import brainscroll.shared.generated.resources.username_change
import brainscroll.shared.generated.resources.username_save
import brainscroll.shared.generated.resources.username_title
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.ConfirmDialog
import com.mehmtcan.brainscroll.ui.components.TextAction
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** What the account section needs to draw itself and to react. Plain data and callbacks, so it is easy to test. */
data class AccountUi(
    val state: AccountState,
    val canSignInWithApple: Boolean,
    /** Results and favorites that are not in the cloud yet. */
    val backupPending: Int,
    val onSignInWithGoogle: () -> Unit,
    val onSignInWithApple: () -> Unit,
    val onSignOut: () -> Unit,
    val onDeleteAccount: () -> Unit,
    /** The name on the tables and in friend lists. Null until the server has given or loaded one. */
    val username: String? = null,
    val onChangeUsername: (String) -> Unit = {},
)

/** What the player was asked to confirm. */
private enum class Confirming { SignOut, Delete }

/**
 * Who you are signed in as, the username (which can be changed here), and the buttons to sign in or out. Signing out is
 * the main button; deleting the account is red. Each asks "are you sure?" first, because the second cannot be undone.
 */
@Composable
fun AccountSection(account: AccountUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var confirming by remember { mutableStateOf<Confirming?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        BasicText(stringResource(Res.string.account_title), style = type.heading.copy(color = colors.textPrimary))

        when (val state = account.state) {
            AccountState.Loading, AccountState.NoSession ->
                BasicText(stringResource(Res.string.account_offline), style = type.body.copy(color = colors.textSecondary))

            is AccountState.SignedIn -> when (state.kind) {
                AccountState.SignedIn.Kind.Anonymous -> {
                    BasicText(stringResource(Res.string.account_anonymous), style = type.body.copy(color = colors.textSecondary))
                    AppButton(stringResource(Res.string.sign_in_google), account.onSignInWithGoogle, primary = true)
                    if (account.canSignInWithApple) {
                        AppButton(stringResource(Res.string.sign_in_apple), account.onSignInWithApple)
                    }
                }
                AccountState.SignedIn.Kind.Google, AccountState.SignedIn.Kind.Apple -> {
                    val provider = if (state.kind == AccountState.SignedIn.Kind.Google) Res.string.account_google else Res.string.account_apple
                    BasicText(stringResource(provider), style = type.body.copy(color = colors.textPrimary))
                    state.email?.let { BasicText(it, style = type.caption.copy(color = colors.textSecondary)) }
                    account.username?.let { UsernameRow(it, account.onChangeUsername) }
                    AppButton(
                        stringResource(Res.string.sign_out),
                        { confirming = Confirming.SignOut },
                        Modifier.padding(top = Spacing.xs).testTag("signOut"),
                        primary = true,
                    )
                    AppButton(stringResource(Res.string.delete_account), { confirming = Confirming.Delete }, Modifier.testTag("deleteAccount"), danger = true)
                }
            }
        }
    }

    when (confirming) {
        Confirming.SignOut -> ConfirmDialog(
            title = stringResource(Res.string.signout_confirm_title),
            message = stringResource(Res.string.signout_confirm_text),
            confirmText = stringResource(Res.string.sign_out),
            cancelText = stringResource(Res.string.confirm_cancel),
            onConfirm = { confirming = null; account.onSignOut() },
            onCancel = { confirming = null },
        )
        Confirming.Delete -> ConfirmDialog(
            title = stringResource(Res.string.delete_title),
            message = stringResource(Res.string.delete_text),
            confirmText = stringResource(Res.string.delete_confirm),
            cancelText = stringResource(Res.string.delete_cancel),
            onConfirm = { confirming = null; account.onDeleteAccount() },
            onCancel = { confirming = null },
            danger = true,
        )
        null -> Unit
    }
}

/** The username with a "Change" action that opens a field under it. */
@Composable
private fun UsernameRow(username: String, onSave: (String) -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var editing by remember { mutableStateOf(false) }
    var draft by remember(username) { mutableStateOf(username) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Column(Modifier.weight(1f)) {
                BasicText(stringResource(Res.string.username_title), style = type.caption.copy(color = colors.textSecondary))
                BasicText(username, style = type.heading.copy(color = colors.textPrimary), maxLines = 1, modifier = Modifier.testTag("usernameValue"))
            }
            Box(
                modifier = Modifier
                    .testTag("usernameChange")
                    .heightIn(min = 48.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                        draft = username
                        editing = !editing
                    }
                    .padding(horizontal = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(stringResource(Res.string.username_change), style = type.label.copy(color = colors.correctFill))
            }
        }
        AnimatedVisibility(editing, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            TextAction(
                value = draft,
                onChange = { draft = it.take(MAX_NAME) },
                hint = stringResource(Res.string.gate_username_hint),
                actionLabel = stringResource(Res.string.username_save),
                onAction = { onSave(draft); editing = false },
            )
        }
    }
}

private const val MAX_NAME = 16
