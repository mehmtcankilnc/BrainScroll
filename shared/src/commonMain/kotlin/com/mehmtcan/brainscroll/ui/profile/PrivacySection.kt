package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.privacy_help
import brainscroll.shared.generated.resources.privacy_info
import brainscroll.shared.generated.resources.privacy_text
import brainscroll.shared.generated.resources.privacy_title
import brainscroll.shared.generated.resources.privacy_what
import com.mehmtcan.brainscroll.ui.components.AppSwitch
import com.mehmtcan.brainscroll.ui.components.LineIcon
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.textSoft
import org.jetbrains.compose.resources.stringResource

/** Whether anonymous usage counts and crash reports are sent, and the callback to change it. */
data class PrivacyUi(val enabled: Boolean, val onChange: (Boolean) -> Unit)

/**
 * The switch for anonymous usage counts and crash reports, as the content of the privacy card: a chart icon, "Share usage data",
 * a few words under it and a green "What is shared?" that slides the long explanation open under the line. On by default, one tap
 * to turn off.
 */
@Composable
fun PrivacyRow(privacy: PrivacyUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var info by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.privacy_title)
    val moreDescription = stringResource(Res.string.privacy_info)

    Column(modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.heightIn(min = 64.dp),
        ) {
            LineIcon(LineIcon.Bars, colors.textPrimary, size = 24.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(title, style = type.heading.copy(color = colors.textPrimary))
                BasicText(stringResource(Res.string.privacy_help), style = type.caption.copy(color = colors.textSoft))
                Box(
                    modifier = Modifier
                        .testTag("privacyInfoButton")
                        .heightIn(min = 32.dp)
                        .semantics { contentDescription = moreDescription }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) { info = !info },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicText(stringResource(Res.string.privacy_what), style = type.caption.copy(color = colors.accent))
                }
            }
            AppSwitch(privacy.enabled, privacy.onChange, title, Modifier.testTag("privacySwitch"))
        }
        AnimatedVisibility(info, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            BasicText(
                text = stringResource(Res.string.privacy_text),
                style = type.body.copy(color = colors.textSoft),
                modifier = Modifier.padding(start = 24.dp + Spacing.md, bottom = Spacing.sm).testTag("privacyInfo"),
            )
        }
    }
}
