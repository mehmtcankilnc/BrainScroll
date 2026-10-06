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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.notice_sign_in_failed
import brainscroll.shared.generated.resources.notice_signed_in
import brainscroll.shared.generated.resources.tab_daily
import brainscroll.shared.generated.resources.tab_feed
import brainscroll.shared.generated.resources.tab_profile
import com.mehmtcan.brainscroll.account.AccountEvent
import com.mehmtcan.brainscroll.ui.components.NoticePill
import com.mehmtcan.brainscroll.ui.daily.DailyScreen
import com.mehmtcan.brainscroll.ui.daily.DailyViewModel
import com.mehmtcan.brainscroll.ui.feed.FeedScreen
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.profile.AccountUi
import com.mehmtcan.brainscroll.ui.profile.ProfileScreen
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class Tab(val label: StringResource) {
    Feed(Res.string.tab_feed),
    Daily(Res.string.tab_daily),
    Profile(Res.string.tab_profile),
}

/** The screen frame: the current tab on top and the bottom tab bar (docs/design.md section 7). */
@Composable
fun AppShell(viewModel: FeedViewModel, dailyViewModel: DailyViewModel) {
    val colors = BrainScrollTheme.colors
    var tab by remember { mutableStateOf(Tab.Feed) }

    // Sign-in results are shown in a short message above whatever tab is open.
    var accountNotice by remember { mutableStateOf<Pair<StringResource, String?>?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.accountEvents.collect { event ->
            accountNotice = when (event) {
                AccountEvent.SignedIn -> Res.string.notice_signed_in to null
                is AccountEvent.SignInFailed -> Res.string.notice_sign_in_failed to event.detail
            }
            // A failure stays a little longer so the small print can be read.
            delay(if (event is AccountEvent.SignInFailed) ACCOUNT_NOTICE_FAILED_MILLIS else ACCOUNT_NOTICE_MILLIS)
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
                Tab.Profile -> {
                    // A daily result or a favorite may have changed while another tab was open.
                    LaunchedEffect(Unit) { viewModel.refreshProfile() }
                    val profile by viewModel.profile.collectAsStateWithLifecycle()
                    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
                    ProfileScreen(
                        profile = profile,
                        account = AccountUi(
                            state = accountState,
                            canSignInWithApple = viewModel.canSignInWithApple,
                            backupPending = profile.backupPending,
                            onSignInWithGoogle = viewModel::signInWithGoogle,
                            onSignInWithApple = viewModel::signInWithApple,
                            onSignOut = viewModel::signOut,
                        ),
                    )
                }
            }
            accountNotice?.let { (text, detail) -> NoticePill(stringResource(text), Modifier.align(Alignment.TopCenter), detail) }
        }
        TabBar(selected = tab, onSelect = { tab = it })
    }
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(tab) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BasicText(
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

private val TAB_BAR_HEIGHT = 56.dp
