package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.account_anonymous
import brainscroll.shared.generated.resources.account_apple
import brainscroll.shared.generated.resources.account_google
import brainscroll.shared.generated.resources.account_offline
import brainscroll.shared.generated.resources.account_title
import brainscroll.shared.generated.resources.backup_done
import brainscroll.shared.generated.resources.backup_pending
import brainscroll.shared.generated.resources.sign_in_apple
import brainscroll.shared.generated.resources.sign_in_google
import brainscroll.shared.generated.resources.sign_out
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.ui.components.AppButton
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
)

/** Who you are signed in as, whether the progress is backed up, and the buttons to sign in or out. */
@Composable
fun AccountSection(account: AccountUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

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

            is AccountState.SignedIn -> {
                when (state.kind) {
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
                        AppButton(stringResource(Res.string.sign_out), account.onSignOut)
                    }
                }
                BasicText(
                    text = if (account.backupPending == 0) stringResource(Res.string.backup_done)
                    else stringResource(Res.string.backup_pending, account.backupPending),
                    style = type.caption.copy(color = colors.textSecondary),
                )
            }
        }
    }
}
