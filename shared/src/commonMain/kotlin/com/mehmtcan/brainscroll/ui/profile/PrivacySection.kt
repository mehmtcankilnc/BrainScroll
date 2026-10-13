package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import brainscroll.shared.generated.resources.privacy_info
import brainscroll.shared.generated.resources.privacy_text
import brainscroll.shared.generated.resources.privacy_title
import com.mehmtcan.brainscroll.ui.components.AppSwitch
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Whether anonymous usage counts and crash reports are sent, and the callback to change it. */
data class PrivacyUi(val enabled: Boolean, val onChange: (Boolean) -> Unit)

/**
 * The switch for anonymous usage counts and crash reports. On by default, one tap to turn off. The long explanation
 * stays out of the way behind the small "?": it slides open under the line when it is tapped.
 */
@Composable
fun PrivacySection(privacy: PrivacyUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var info by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.privacy_title)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            BasicText(title, style = type.body.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
            InfoButton(open = info, onClick = { info = !info })
            AppSwitch(privacy.enabled, privacy.onChange, title, Modifier.testTag("privacySwitch"))
        }
        AnimatedVisibility(info, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            BasicText(
                text = stringResource(Res.string.privacy_text),
                style = type.body.copy(color = colors.textSecondary),
                modifier = Modifier.padding(bottom = Spacing.sm).testTag("privacyInfo"),
            )
        }
    }
}

/** A small circle with a question mark; the touch area is 48 dp. */
@Composable
private fun InfoButton(open: Boolean, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val color = if (open) colors.textPrimary else colors.textSecondary
    val description = stringResource(Res.string.privacy_info)
    Box(
        modifier = Modifier
            .testTag("privacyInfoButton")
            .size(48.dp)
            .semantics { contentDescription = description }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(20.dp).border(1.5.dp, color, CircleShape), contentAlignment = Alignment.Center) {
            BasicText("?", style = BrainScrollTheme.typography.caption.copy(color = color))
        }
    }
}
