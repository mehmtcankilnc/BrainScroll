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
import brainscroll.shared.generated.resources.privacy_off
import brainscroll.shared.generated.resources.privacy_on
import brainscroll.shared.generated.resources.privacy_text
import brainscroll.shared.generated.resources.privacy_title
import brainscroll.shared.generated.resources.privacy_turn_off
import brainscroll.shared.generated.resources.privacy_turn_on
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Whether anonymous usage counts and crash reports are sent, and the callback to change it. */
data class PrivacyUi(val enabled: Boolean, val onChange: (Boolean) -> Unit)

/** The switch for anonymous usage counts and crash reports. On by default, one tap to turn off. */
@Composable
fun PrivacySection(privacy: PrivacyUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        BasicText(stringResource(Res.string.privacy_title), style = type.heading.copy(color = colors.textPrimary))
        BasicText(stringResource(Res.string.privacy_text), style = type.body.copy(color = colors.textSecondary))
        BasicText(
            text = stringResource(if (privacy.enabled) Res.string.privacy_on else Res.string.privacy_off),
            style = type.label.copy(color = if (privacy.enabled) colors.correctFill else colors.textSecondary),
        )
        AppButton(
            text = stringResource(if (privacy.enabled) Res.string.privacy_turn_off else Res.string.privacy_turn_on),
            onClick = { privacy.onChange(!privacy.enabled) },
        )
        // TEMP-CRASH-BUTTON: only for testing that a crash report arrives (docs/faz8-test-listesi.md 6d). REMOVE after the test.
        AppButton(text = "TEST: crash the app", onClick = { error("Test crash from the TEMP crash button") })
    }
}
