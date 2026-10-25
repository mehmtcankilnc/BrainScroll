package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.pro_active_hint
import brainscroll.shared.generated.resources.pro_active_title
import brainscroll.shared.generated.resources.pro_badge
import brainscroll.shared.generated.resources.pro_card_hint
import brainscroll.shared.generated.resources.pro_restore_failed
import brainscroll.shared.generated.resources.pro_restore_none
import brainscroll.shared.generated.resources.pro_restored
import brainscroll.shared.generated.resources.pro_title
import com.mehmtcan.brainscroll.telemetry.Telemetry
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.pro.LocalPro
import com.mehmtcan.brainscroll.ui.pro.PaywallSource
import com.mehmtcan.brainscroll.ui.pro.RestoreResult
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The BrainScroll Pro card of the profile. Free: a quiet card that opens the purchase screen (nothing opens by itself).
 * Pro: a thank-you with the supporter badge, and "Restore purchase" stays (there is no subscription to manage). Not drawn
 * where there is nothing to buy (desktop, no store keys).
 */
@Composable
fun ProCard(onOpen: (PaywallSource) -> Unit, telemetry: Telemetry, modifier: Modifier = Modifier) {
    val pro = LocalPro.current
    if (!pro.available) return
    val isPro by pro.isPro.collectAsStateWithLifecycle()
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<StringResource?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .then(
                if (isPro) Modifier
                else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) { onOpen(PaywallSource.Profile) },
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .testTag("proCard"),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            BasicText(
                text = stringResource(if (isPro) Res.string.pro_active_title else Res.string.pro_title),
                style = type.heading.copy(color = colors.textPrimary),
                modifier = Modifier.weight(1f),
            )
            // The supporter badge: only for someone who is Pro.
            if (isPro) {
                BasicText(
                    text = stringResource(Res.string.pro_badge),
                    style = type.caption.copy(color = colors.accentOn),
                    modifier = Modifier.background(colors.accent, Radius.pill).padding(horizontal = Spacing.sm, vertical = 2.dp).testTag("proBadge"),
                )
            }
        }
        if (!isPro) {
            BasicText(stringResource(Res.string.pro_card_hint), style = type.label.copy(color = colors.textSecondary))
        } else {
            BasicText(
                text = stringResource(Res.string.pro_active_hint),
                style = type.label.copy(color = colors.textSecondary),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                        if (!busy) {
                            busy = true
                            scope.launch {
                                message = when (pro.restore()) {
                                    RestoreResult.Restored -> {
                                        telemetry.event(TelemetryEvent.PurchaseRestored, mapOf("source" to "profile"))
                                        Res.string.pro_restored
                                    }
                                    RestoreResult.NothingToRestore -> Res.string.pro_restore_none
                                    RestoreResult.Failed -> Res.string.pro_restore_failed
                                }
                                busy = false
                            }
                        }
                    }
                    .testTag("proRestoreProfile"),
            )
            message?.let { BasicText(stringResource(it), style = type.caption.copy(color = colors.textSecondary)) }
        }
    }
}
