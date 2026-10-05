package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.tab_feed
import brainscroll.shared.generated.resources.tab_profile
import com.mehmtcan.brainscroll.ui.feed.FeedScreen
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.profile.ProfileScreen
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class Tab(val label: StringResource) {
    Feed(Res.string.tab_feed),
    Profile(Res.string.tab_profile),
}

/** The screen frame: the current tab on top and the bottom tab bar (docs/design.md section 7). */
@Composable
fun AppShell(viewModel: FeedViewModel) {
    val colors = BrainScrollTheme.colors
    var tab by remember { mutableStateOf(Tab.Feed) }

    Column(modifier = Modifier.fillMaxSize().background(colors.bgPage).safeContentPadding()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                Tab.Feed -> FeedScreen(viewModel)
                Tab.Profile -> {
                    val profile by viewModel.profile.collectAsStateWithLifecycle()
                    ProfileScreen(profile)
                }
            }
        }
        TabBar(selected = tab, onSelect = { tab = it })
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val colors = BrainScrollTheme.colors
    Row(modifier = Modifier.fillMaxWidth().height(64.dp).background(colors.bgSurface)) {
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
                BasicText(
                    text = stringResource(tab.label),
                    style = BrainScrollTheme.typography.label.copy(
                        color = if (active) colors.textPrimary else colors.textSecondary,
                    ),
                )
            }
        }
    }
}
