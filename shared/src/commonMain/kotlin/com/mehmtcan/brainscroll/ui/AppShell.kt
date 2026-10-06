package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.notice_sign_in_failed
import brainscroll.shared.generated.resources.notice_account_deleted
import brainscroll.shared.generated.resources.notice_delete_failed
import brainscroll.shared.generated.resources.notice_signed_in
import brainscroll.shared.generated.resources.tab_ranks
import brainscroll.shared.generated.resources.notice_username_saved
import brainscroll.shared.generated.resources.notice_username_invalid
import brainscroll.shared.generated.resources.notice_username_not_allowed
import brainscroll.shared.generated.resources.notice_username_taken
import brainscroll.shared.generated.resources.notice_friend_added
import brainscroll.shared.generated.resources.notice_request_sent
import brainscroll.shared.generated.resources.notice_now_friends
import brainscroll.shared.generated.resources.notice_code_not_found
import brainscroll.shared.generated.resources.notice_player_not_found
import brainscroll.shared.generated.resources.notice_is_self
import brainscroll.shared.generated.resources.notice_social_offline
import brainscroll.shared.generated.resources.notice_social_unavailable
import brainscroll.shared.generated.resources.notice_invite_sign_in
import brainscroll.shared.generated.resources.notice_invite_username
import com.mehmtcan.brainscroll.ui.social.RanksScreen
import com.mehmtcan.brainscroll.ui.social.RanksSignIn
import com.mehmtcan.brainscroll.ui.social.SocialEvent
import com.mehmtcan.brainscroll.ui.social.SocialViewModel
import brainscroll.shared.generated.resources.tab_daily
import brainscroll.shared.generated.resources.tab_feed
import brainscroll.shared.generated.resources.tab_profile
import com.mehmtcan.brainscroll.account.AccountEvent
import com.mehmtcan.brainscroll.ui.components.NoticePill
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.daily.DailyScreen
import com.mehmtcan.brainscroll.ui.daily.DailyViewModel
import com.mehmtcan.brainscroll.ui.feed.FeedScreen
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.profile.AccountUi
import com.mehmtcan.brainscroll.ui.profile.PrivacyUi
import com.mehmtcan.brainscroll.ui.profile.ProfileScreen
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class Tab(val label: StringResource) {
    Feed(Res.string.tab_feed),
    Daily(Res.string.tab_daily),
    Ranks(Res.string.tab_ranks),
    Profile(Res.string.tab_profile),
}

/** The screen frame: the current tab on top and the bottom tab bar (docs/design.md section 7). */
@Composable
fun AppShell(viewModel: FeedViewModel, dailyViewModel: DailyViewModel, socialViewModel: SocialViewModel) {
    val colors = BrainScrollTheme.colors
    var tab by remember { mutableStateOf(Tab.Feed) }
    // Which tabs are used (only the name of the tab, nothing about the player).
    LaunchedEffect(tab) { viewModel.telemetry.event(TelemetryEvent.TabViewed, mapOf("tab" to tab.name.lowercase())) }

    // Sign-in results are shown in a short message above whatever tab is open.
    // Friends and username messages too ([name] fills a %1$s in the text).
    var accountNotice by remember { mutableStateOf<ShellNotice?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.accountEvents.collect { event ->
            accountNotice = when (event) {
                AccountEvent.SignedIn -> ShellNotice(Res.string.notice_signed_in)
                is AccountEvent.SignInFailed -> ShellNotice(Res.string.notice_sign_in_failed, detail = event.detail)
                AccountEvent.AccountDeleted -> ShellNotice(Res.string.notice_account_deleted)
                AccountEvent.DeleteFailed -> ShellNotice(Res.string.notice_delete_failed)
            }
            // A failure stays a little longer so the small print can be read.
            delay(if (event is AccountEvent.SignInFailed) ACCOUNT_NOTICE_FAILED_MILLIS else ACCOUNT_NOTICE_MILLIS)
            accountNotice = null
        }
    }
    LaunchedEffect(socialViewModel) {
        socialViewModel.events.collect { event ->
            // An invite link opens the friends part, where the player sees what happened to it.
            if (event.kind in INVITE_EVENTS) tab = Tab.Ranks
            accountNotice = ShellNotice(noticeFor(event.kind), name = event.name)
            delay(ACCOUNT_NOTICE_MILLIS)
            accountNotice = null
        }
    }

    // Only the content is pushed away from the status bar. The tab bar below handles the bottom edge itself,
    // so its background can reach the screen edges and sit under the home indicator.
    Column(modifier = Modifier.fillMaxSize().background(colors.bgPage)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            when (tab) {
                Tab.Feed -> FeedScreen(viewModel)
                Tab.Daily -> DailyScreen(dailyViewModel)
                Tab.Ranks -> RanksScreen(
                    viewModel = socialViewModel,
                    signIn = RanksSignIn(
                        canSignInWithApple = viewModel.canSignInWithApple,
                        onSignInWithGoogle = viewModel::signInWithGoogle,
                        onSignInWithApple = viewModel::signInWithApple,
                    ),
                )
                Tab.Profile -> {
                    // A daily result or a favorite may have changed while another tab was open.
                    LaunchedEffect(Unit) { viewModel.refreshProfile() }
                    val profile by viewModel.profile.collectAsStateWithLifecycle()
                    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
                    val sharing by viewModel.telemetry.enabled.collectAsStateWithLifecycle()
                    ProfileScreen(
                        profile = profile,
                        account = AccountUi(
                            state = accountState,
                            canSignInWithApple = viewModel.canSignInWithApple,
                            backupPending = profile.backupPending,
                            onSignInWithGoogle = viewModel::signInWithGoogle,
                            onSignInWithApple = viewModel::signInWithApple,
                            onSignOut = viewModel::signOut,
                            onDeleteAccount = viewModel::deleteAccount,
                        ),
                        privacy = PrivacyUi(enabled = sharing, onChange = viewModel.telemetry::setEnabled),
                    )
                }
            }
            accountNotice?.let { notice ->
                val text = if (notice.name != null) stringResource(notice.text, notice.name) else stringResource(notice.text)
                NoticePill(text, Modifier.align(Alignment.TopCenter), notice.detail)
            }
        }
        TabBar(selected = tab, onSelect = { tab = it })
    }
}

private class ShellNotice(val text: StringResource, val name: String? = null, val detail: String? = null)

private val INVITE_EVENTS = setOf(
    SocialEvent.Kind.InviteNeedsSignIn,
    SocialEvent.Kind.InviteNeedsUsername,
    SocialEvent.Kind.FriendAdded,
    SocialEvent.Kind.CodeNotFound,
)

private fun noticeFor(kind: SocialEvent.Kind): StringResource = when (kind) {
    SocialEvent.Kind.UsernameSaved -> Res.string.notice_username_saved
    SocialEvent.Kind.UsernameInvalid -> Res.string.notice_username_invalid
    SocialEvent.Kind.UsernameNotAllowed -> Res.string.notice_username_not_allowed
    SocialEvent.Kind.UsernameTaken -> Res.string.notice_username_taken
    SocialEvent.Kind.FriendAdded -> Res.string.notice_friend_added
    SocialEvent.Kind.RequestSent -> Res.string.notice_request_sent
    SocialEvent.Kind.NowFriends -> Res.string.notice_now_friends
    SocialEvent.Kind.CodeNotFound -> Res.string.notice_code_not_found
    SocialEvent.Kind.PlayerNotFound -> Res.string.notice_player_not_found
    SocialEvent.Kind.IsSelf -> Res.string.notice_is_self
    SocialEvent.Kind.Offline -> Res.string.notice_social_offline
    SocialEvent.Kind.Unavailable -> Res.string.notice_social_unavailable
    SocialEvent.Kind.InviteNeedsSignIn -> Res.string.notice_invite_sign_in
    SocialEvent.Kind.InviteNeedsUsername -> Res.string.notice_invite_username
}

private const val ACCOUNT_NOTICE_MILLIS = 2_500L
private const val ACCOUNT_NOTICE_FAILED_MILLIS = 6_000L

/**
 * Full-width bar. The background is drawn first and covers the whole width and the bottom safe area
 * (home indicator); only afterwards is the content padded, so the labels stay in the safe area.
 */
@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val colors = BrainScrollTheme.colors
    val haptics = LocalHaptics.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
    ) {
        // Thin line on top, so the bar reads as its own surface even where the tones are close.
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
        Row(modifier = Modifier.fillMaxWidth().height(TAB_BAR_HEIGHT)) {
            Tab.entries.forEach { tab ->
                val active = tab == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .semantics { this.selected = active }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                if (tab != selected) haptics.perform(HapticKind.Light)
                                onSelect(tab)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // The bar has a fixed height: its text grows with the system setting, but stops at 1.3 times.
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(TAB_MAX_FONT_SCALE))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BasicText(
                            maxLines = 1,
                            softWrap = false,
                            text = stringResource(tab.label),
                            style = BrainScrollTheme.typography.label.copy(
                                color = if (active) colors.textPrimary else colors.textSecondary,
                            ),
                        )
                        // A short bar under the active label: the selected tab is not shown by color alone.
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(3.dp)
                                .background(if (active) colors.correctFill else Color.Transparent, Radius.pill),
                        )
                    }
                    }
                }
            }
        }
    }
}

private val TAB_BAR_HEIGHT = 56.dp
private const val TAB_MAX_FONT_SCALE = 1.3f
