package com.mehmtcan.brainscroll.ui.pro

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_done
import brainscroll.shared.generated.resources.pro_benefit_badge
import brainscroll.shared.generated.resources.pro_benefit_freeze
import brainscroll.shared.generated.resources.pro_benefit_hints
import brainscroll.shared.generated.resources.pro_benefit_look
import brainscroll.shared.generated.resources.pro_buy
import brainscroll.shared.generated.resources.pro_cancelled
import brainscroll.shared.generated.resources.pro_failed
import brainscroll.shared.generated.resources.pro_pending
import brainscroll.shared.generated.resources.pro_price_caption
import brainscroll.shared.generated.resources.pro_price_unavailable
import brainscroll.shared.generated.resources.pro_privacy
import brainscroll.shared.generated.resources.pro_restore
import brainscroll.shared.generated.resources.pro_restore_failed
import brainscroll.shared.generated.resources.pro_restore_none
import brainscroll.shared.generated.resources.pro_restored
import brainscroll.shared.generated.resources.pro_tagline
import brainscroll.shared.generated.resources.pro_thanks
import brainscroll.shared.generated.resources.pro_title
import brainscroll.shared.generated.resources.pro_working
import brainscroll.shared.generated.resources.rescue_body
import brainscroll.shared.generated.resources.rescue_done
import brainscroll.shared.generated.resources.rescue_later
import brainscroll.shared.generated.resources.rescue_rule
import brainscroll.shared.generated.resources.rescue_title
import com.mehmtcan.brainscroll.stats.Rescue
import com.mehmtcan.brainscroll.telemetry.Telemetry
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.CloseButton
import com.mehmtcan.brainscroll.ui.components.SlidePage
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.sound.LocalSoundEffects
import com.mehmtcan.brainscroll.ui.sound.rememberSoundHaptics
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The purchase screen of BrainScroll Pro (docs/pro-plan.md section 7). A full page that slides up from the bottom and slides
 * back down when it closes: the close button is there from the first frame, the back button closes it too, and nothing in it
 * hurries anyone. The buy button stays at the bottom while the text above it scrolls.
 * The price is what the store says for the player's country. With a [rescue] the same screen opens with the
 * streak that Pro would bring back (section 7.3).
 */
@Composable
fun PaywallSheet(source: PaywallSource, onClose: () -> Unit, telemetry: Telemetry, modifier: Modifier = Modifier, rescue: Rescue? = null) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val pro = LocalPro.current
    val isPro by pro.isPro.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptics = rememberSoundHaptics(LocalHaptics.current, LocalSoundEffects.current)
    val uri = LocalUriHandler.current

    var product by remember { mutableStateOf<ProProduct?>(null) }
    var priceFailed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<StringResource?>(null) }
    var celebrated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        telemetry.event(TelemetryEvent.PaywallShown, mapOf("source" to source.wireName))
        product = pro.product()
        priceFailed = product == null
    }
    // Pro turned on: by this purchase, a restore, or an approval that came later. The page changes at once.
    LaunchedEffect(isPro) {
        if (isPro && !celebrated) {
            celebrated = true
            haptics.perform(HapticKind.Success)
        }
    }

    val buy: () -> Unit = {
        if (!busy) {
            busy = true
            message = null
            scope.launch {
                if (product == null) {
                    product = pro.product()
                    priceFailed = product == null
                }
                if (product == null) {
                    message = Res.string.pro_price_unavailable
                } else {
                    telemetry.event(TelemetryEvent.PurchaseStarted, mapOf("source" to source.wireName))
                    when (val result = pro.purchase()) {
                        PurchaseResult.Success -> telemetry.event(TelemetryEvent.PurchaseSucceeded, mapOf("source" to source.wireName))
                        PurchaseResult.Cancelled -> {
                            telemetry.event(TelemetryEvent.PurchaseCancelled, mapOf("source" to source.wireName))
                            message = Res.string.pro_cancelled
                        }
                        PurchaseResult.Pending -> message = Res.string.pro_pending
                        is PurchaseResult.Failed -> {
                            telemetry.event(TelemetryEvent.PurchaseFailed, mapOf("source" to source.wireName, "reason" to result.reason.take(40)))
                            message = Res.string.pro_failed
                        }
                    }
                }
                busy = false
            }
        }
    }
    val restore: () -> Unit = {
        if (!busy) {
            busy = true
            message = null
            scope.launch {
                message = when (pro.restore()) {
                    RestoreResult.Restored -> {
                        telemetry.event(TelemetryEvent.PurchaseRestored, mapOf("source" to source.wireName))
                        Res.string.pro_restored
                    }
                    RestoreResult.NothingToRestore -> Res.string.pro_restore_none
                    RestoreResult.Failed -> Res.string.pro_restore_failed
                }
                busy = false
            }
        }
    }

    SlidePage(onDismiss = onClose, modifier = modifier, tag = "paywallSheet") { close ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = stringResource(if (rescue != null && !isPro) Res.string.rescue_title else Res.string.pro_title),
                style = type.title.copy(color = colors.textPrimary),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            CloseButton(close, tag = "paywallClose")
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (isPro) {
                BasicText(stringResource(Res.string.pro_thanks), style = type.heading.copy(color = colors.accent), modifier = Modifier.testTag("proThanks"))
                if (rescue != null) {
                    BasicText(stringResource(Res.string.rescue_done, rescue.proStreak), style = type.body.copy(color = colors.textPrimary))
                }
            } else {
                if (rescue != null) {
                    BasicText(stringResource(Res.string.rescue_body, rescue.proStreak), style = type.heading.copy(color = colors.textPrimary))
                    BasicText(stringResource(Res.string.rescue_rule), style = type.body.copy(color = colors.textSecondary))
                } else {
                    BasicText(stringResource(Res.string.pro_tagline), style = type.body.copy(color = colors.textSecondary))
                }
                listOf(Res.string.pro_benefit_hints, Res.string.pro_benefit_freeze, Res.string.pro_benefit_look, Res.string.pro_benefit_badge).forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 7.dp).size(8.dp).background(colors.accent, CircleShape))
                        BasicText(stringResource(line), style = type.body.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
                    }
                }
                // The price card. Everything in it reads as one thing to a screen reader.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgRaised, Radius.card)
                        .padding(Spacing.lg)
                        .semantics(mergeDescendants = true) {},
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText(
                        text = product?.priceText ?: if (priceFailed) "–" else "…",
                        style = type.display.copy(color = colors.accent, textAlign = TextAlign.Center),
                        modifier = Modifier.testTag("proPrice"),
                    )
                    BasicText(
                        text = stringResource(if (priceFailed) Res.string.pro_price_unavailable else Res.string.pro_price_caption),
                        style = type.label.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                }
            }
        }
        if (isPro) {
            AppButton(stringResource(Res.string.howto_done), onClick = close, primary = true)
        } else {
            AppButton(
                text = stringResource(if (busy) Res.string.pro_working else Res.string.pro_buy),
                onClick = buy,
                primary = true,
                modifier = Modifier.testTag("proBuy"),
            )
            // A plain way out that is as easy to see as the buy button, for the paywall that comes by itself.
            if (rescue != null) AppButton(stringResource(Res.string.rescue_later), onClick = close, modifier = Modifier.testTag("proLater"))
            message?.let { BasicText(stringResource(it), style = type.label.copy(color = colors.textSecondary), modifier = Modifier.testTag("proMessage")) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                LinkText(stringResource(Res.string.pro_restore), restore, Modifier.weight(1f).testTag("proRestore"))
                LinkText(
                    stringResource(Res.string.pro_privacy),
                    { uri.openUri(if (Locale.current.language == "tr") ProConfig.PRIVACY_URL_TR else ProConfig.PRIVACY_URL_EN) },
                    Modifier.testTag("proPrivacy"),
                )
            }
        }
    }
}

/** A line of text that is a button, at least 48 dp tall. */
@Composable
private fun LinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicText(text, style = BrainScrollTheme.typography.label.copy(color = BrainScrollTheme.colors.textSecondary))
    }
}
