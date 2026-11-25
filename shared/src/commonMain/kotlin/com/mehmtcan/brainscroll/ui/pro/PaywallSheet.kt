package com.mehmtcan.brainscroll.ui.pro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.guide_left
import brainscroll.shared.generated.resources.guide_left_bonus
import brainscroll.shared.generated.resources.howto_done
import brainscroll.shared.generated.resources.pack_added
import brainscroll.shared.generated.resources.pack_adding
import brainscroll.shared.generated.resources.pack_best
import brainscroll.shared.generated.resources.pack_item_a11y
import brainscroll.shared.generated.resources.pack_processing
import brainscroll.shared.generated.resources.pack_title
import brainscroll.shared.generated.resources.pack_unit
import brainscroll.shared.generated.resources.pro_cta
import brainscroll.shared.generated.resources.pro_cancelled
import brainscroll.shared.generated.resources.pro_failed
import brainscroll.shared.generated.resources.pro_one_time
import brainscroll.shared.generated.resources.pro_pending
import brainscroll.shared.generated.resources.pro_price_caption
import brainscroll.shared.generated.resources.pro_price_unavailable
import brainscroll.shared.generated.resources.pro_privacy
import brainscroll.shared.generated.resources.pro_restore
import brainscroll.shared.generated.resources.pro_restore_failed
import brainscroll.shared.generated.resources.pro_restore_none
import brainscroll.shared.generated.resources.pro_restored
import brainscroll.shared.generated.resources.pro_terms
import brainscroll.shared.generated.resources.pro_thanks
import brainscroll.shared.generated.resources.pro_title
import brainscroll.shared.generated.resources.pro_working
import brainscroll.shared.generated.resources.rescue_cta
import brainscroll.shared.generated.resources.rescue_done
import brainscroll.shared.generated.resources.rescue_headline
import brainscroll.shared.generated.resources.rescue_later
import brainscroll.shared.generated.resources.rescue_price
import brainscroll.shared.generated.resources.rescue_support
import brainscroll.shared.generated.resources.store_pro_hints
import brainscroll.shared.generated.resources.store_pro_streak
import brainscroll.shared.generated.resources.store_title
import com.mehmtcan.brainscroll.game.guide.QuotaState
import com.mehmtcan.brainscroll.stats.Rescue
import com.mehmtcan.brainscroll.telemetry.Telemetry
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.CloseButton
import com.mehmtcan.brainscroll.ui.components.SlidePage
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.home.BrainMascot
import com.mehmtcan.brainscroll.ui.home.MascotFace
import com.mehmtcan.brainscroll.ui.sound.LocalSoundEffects
import com.mehmtcan.brainscroll.ui.sound.rememberSoundHaptics
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The purchase screens of BrainScroll Pro and the hint packs (docs/pro-plan.md sections 7 and 11). A full page that slides up from
 * the bottom and slides back down when it closes: the close button is there from the first frame, the back button closes it
 * too, and nothing in it hurries anyone.
 *
 * Two pages share the same buying logic:
 *  - the store (no [rescue]): a header with the bought hints, the Pro card, and the hint packs as cards in a grid;
 *  - the streak offer (with a [rescue], section 7.3): the mascot, the streak that would come back, and one price with one button.
 *
 * Prices are what the store says for the player's country. The restore, terms and privacy links are on both.
 */
@Composable
fun PaywallSheet(source: PaywallSource, onClose: () -> Unit, telemetry: Telemetry, modifier: Modifier = Modifier, rescue: Rescue? = null, quota: QuotaState? = null) {
    val pro = LocalPro.current
    val isPro by pro.isPro.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptics = rememberSoundHaptics(LocalHaptics.current, LocalSoundEffects.current)
    val uri = LocalUriHandler.current

    var product by remember { mutableStateOf<ProProduct?>(null) }
    var packs by remember { mutableStateOf<List<HintPack>>(emptyList()) }
    val bought by pro.hints.balance.collectAsStateWithLifecycle()
    var priceFailed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<StringResource?>(null) }
    var celebrated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        telemetry.event(TelemetryEvent.PaywallShown, mapOf("source" to source.wireName))
        product = pro.product()
        priceFailed = product == null
        if (rescue == null) {
            packs = pro.packs()
            pro.hints.refresh()
        }
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
    val buyPack: (HintPack) -> Unit = { pack ->
        if (!busy) {
            busy = true
            message = null
            scope.launch {
                val item = mapOf("source" to source.wireName, "item" to pack.id)
                pro.hints.refresh()
                val before = pro.hints.balance.value
                telemetry.event(TelemetryEvent.PurchaseStarted, item)
                when (val result = pro.buyPack(pack)) {
                    PurchaseResult.Success -> {
                        telemetry.event(TelemetryEvent.PurchaseSucceeded, item)
                        haptics.perform(HapticKind.Success)
                        // The store only took the payment: the server adds the hints a few seconds later (the webhook).
                        message = Res.string.pack_adding
                        message = if (pro.hints.awaitCredit(before)) Res.string.pack_added else Res.string.pack_processing
                    }
                    PurchaseResult.Cancelled -> {
                        telemetry.event(TelemetryEvent.PurchaseCancelled, item)
                        message = Res.string.pro_cancelled
                    }
                    PurchaseResult.Pending -> message = Res.string.pro_pending
                    is PurchaseResult.Failed -> {
                        telemetry.event(TelemetryEvent.PurchaseFailed, item + ("reason" to result.reason.take(40)))
                        message = Res.string.pro_failed
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

    val ui = PaywallUi(
        product = product,
        priceFailed = priceFailed,
        busy = busy,
        message = message,
        packs = packs,
        bought = bought,
        quota = quota,
        isPro = isPro,
        buy = buy,
        buyPack = buyPack,
        restore = restore,
        privacy = { uri.openUri(if (Locale.current.language == "tr") ProConfig.PRIVACY_URL_TR else ProConfig.PRIVACY_URL_EN) },
        terms = { uri.openUri(ProConfig.TERMS_URL) },
    )
    SlidePage(onDismiss = onClose, modifier = modifier, tag = "paywallSheet") { close ->
        if (rescue != null) RescuePage(ui, rescue, close) else StorePage(ui, close)
    }
}

/** Everything a purchase page shows or does, so the two pages stay plain drawing code. */
private class PaywallUi(
    val product: ProProduct?,
    val priceFailed: Boolean,
    val busy: Boolean,
    val message: StringResource?,
    val packs: List<HintPack>,
    val bought: Int,
    val quota: QuotaState?,
    val isPro: Boolean,
    val buy: () -> Unit,
    val buyPack: (HintPack) -> Unit,
    val restore: () -> Unit,
    val privacy: () -> Unit,
    val terms: () -> Unit,
) {
    val priceText: String get() = product?.priceText ?: if (priceFailed) "–" else "…"
}

// --- The store ---

@Composable
private fun ColumnScope.StorePage(ui: PaywallUi, close: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    // The bar: close on the left, the title in the middle, the hints bought so far on the right (only once there are some).
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        CloseButton(close, modifier = Modifier.align(Alignment.CenterStart), tag = "paywallClose")
        BasicText(
            stringResource(Res.string.store_title),
            style = type.title.copy(color = colors.textPrimary),
            modifier = Modifier.align(Alignment.Center).semantics { heading() },
        )
        if (ui.bought > 0) HintChip(ui.bought, Modifier.align(Alignment.CenterEnd))
    }
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (ui.isPro) {
            ThanksBlock(null)
        } else {
            ProCard(ui)
            if (ui.packs.isNotEmpty()) {
                BasicText(
                    stringResource(Res.string.pack_title),
                    style = type.heading.copy(color = colors.textPrimary),
                    modifier = Modifier.padding(top = Spacing.sm).semantics { heading() },
                )
                val free = ui.quota?.freeLeft ?: 0
                if (free > 0) BasicText(stringResource(Res.string.guide_left, free), style = type.label.copy(color = colors.textSecondary))
                ui.packs.chunked(2).forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        line.forEach { pack ->
                            PackCard(
                                pack,
                                best = ui.packs.size > 1 && pack == ui.packs.last(),
                                enabled = !ui.busy,
                                onClick = { ui.buyPack(pack) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (line.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
    if (ui.isPro) AppButton(stringResource(Res.string.howto_done), onClick = close, primary = true)
    Footer(ui)
}

/** The main offer of the store: the mascot, the two things Pro is for, the price and the buy button. */
@Composable
private fun ProCard(ui: PaywallUi) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    // The mascot stands over the top edge of the card instead of sitting inside it: the card starts below its head.
    Box(Modifier.fillMaxWidth().padding(top = 44.dp)) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .border(1.5.dp, colors.accent, Radius.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
            Spacer(Modifier.width(100.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                BasicText(stringResource(Res.string.pro_title), style = type.title.copy(color = colors.textPrimary))
                ProPoint(stringResource(Res.string.store_pro_hints))
                ProPoint(stringResource(Res.string.store_pro_streak))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            // The price and its caption read as one thing to a screen reader.
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                BasicText(ui.priceText, style = type.display.copy(color = colors.accent), modifier = Modifier.testTag("proPrice"))
                BasicText(
                    stringResource(if (ui.priceFailed) Res.string.pro_price_unavailable else Res.string.pro_price_caption),
                    style = type.label.copy(color = colors.textSecondary),
                )
            }
            AppButton(
                text = stringResource(if (ui.busy) Res.string.pro_working else Res.string.pro_cta),
                onClick = ui.buy,
                primary = true,
                modifier = Modifier.width(140.dp).testTag("proBuy"),
            )
        }
    }
    ProMascot(Modifier.align(Alignment.TopStart).offset(x = 4.dp, y = (-44).dp))
    }
}

@Composable
private fun ProPoint(text: String) {
    val colors = BrainScrollTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(colors.accent, CircleShape))
        BasicText(text, style = BrainScrollTheme.typography.body.copy(color = colors.textPrimary))
    }
}

/** One hint pack as a card: the bulbs grow with the pack, the count, and the price as a button. The whole card buys. */
@Composable
private fun PackCard(pack: HintPack, best: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val description = stringResource(Res.string.pack_item_a11y, pack.hints, pack.priceText)
    Column(
        modifier = modifier
            .heightIn(min = 208.dp)
            .background(colors.bgSurface, Radius.card)
            .border(0.5.dp, colors.borderSubtle, Radius.card)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(Spacing.md)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag("pack_${pack.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // A slot of the same height on every card, so the numbers line up whether a card has the tag or not.
        Box(Modifier.height(22.dp), contentAlignment = Alignment.Center) {
            if (best) {
                BasicText(
                    stringResource(Res.string.pack_best),
                    style = type.caption.copy(color = colors.pendingOn),
                    modifier = Modifier.background(colors.pendingFill, Radius.pill).padding(horizontal = Spacing.sm, vertical = 2.dp),
                )
            }
        }
        PackArt(pack.hints)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(pack.hints.toString(), style = type.display.copy(color = colors.textPrimary))
            BasicText(stringResource(Res.string.pack_unit), style = type.label.copy(color = colors.textSecondary))
        }
        BasicText(
            pack.priceText,
            style = type.label.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .background(colors.bgRaised, Radius.pill)
                .padding(vertical = 10.dp),
        )
    }
}

/** The hints bought so far, as a small pill in the bar of the store. */
@Composable
private fun HintChip(count: Int, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val description = stringResource(Res.string.guide_left_bonus, count)
    Row(
        modifier = modifier
            .height(36.dp)
            .background(colors.bgSurface, Radius.pill)
            .border(1.5.dp, colors.pendingFill, Radius.pill)
            .padding(horizontal = Spacing.md)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag("packBalance"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HintBulb(22.dp, rays = false)
        BasicText(count.toString(), style = BrainScrollTheme.typography.label.copy(color = colors.textPrimary))
    }
}

// --- The streak offer ---

/**
 * What a player sees when the streak broke (docs/pro-plan.md section 7.3): the mascot, the streak that would come back, what Pro
 * also gives, the one-time price, one main button and a plain way out. Nothing counts down and nothing says "last chance".
 */
@Composable
private fun ColumnScope.RescuePage(ui: PaywallUi, rescue: Rescue, close: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { CloseButton(close, tag = "paywallClose") }
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        if (ui.isPro) {
            ThanksBlock(rescue)
        } else {
            StreakArt(rescue.proStreak)
            BasicText(
                stringResource(Res.string.pro_one_time),
                style = type.label.copy(color = colors.pendingOn),
                modifier = Modifier.background(colors.pendingFill, Radius.pill).padding(horizontal = Spacing.lg, vertical = 6.dp),
            )
            BasicText(
                stringResource(Res.string.rescue_headline, rescue.proStreak),
                style = type.display.copy(color = colors.textPrimary, fontSize = 30.sp, lineHeight = 36.sp, textAlign = TextAlign.Center),
                modifier = Modifier.semantics { heading() },
            )
            BasicText(
                stringResource(Res.string.rescue_support),
                style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
            BasicText(
                if (ui.priceFailed) stringResource(Res.string.pro_price_unavailable) else stringResource(Res.string.rescue_price, ui.priceText),
                style = type.label.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
                modifier = Modifier.testTag("proPrice"),
            )
        }
    }
    if (ui.isPro) {
        AppButton(stringResource(Res.string.howto_done), onClick = close, primary = true)
    } else {
        AppButton(
            text = stringResource(if (ui.busy) Res.string.pro_working else Res.string.rescue_cta),
            onClick = ui.buy,
            primary = true,
            modifier = Modifier.testTag("proBuy"),
        )
        // A plain way out for the page that comes by itself: small, underlined, but always there and 48 dp tall to touch.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .heightIn(min = 48.dp)
                .padding(horizontal = Spacing.lg)
                .testTag("proLater")
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = close),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(Res.string.rescue_later),
                style = BrainScrollTheme.typography.label.copy(color = BrainScrollTheme.colors.textSecondary, textDecoration = TextDecoration.Underline),
            )
        }
    }
    Footer(ui)
}

// --- Both ---

/** Pro is on: the thanks, and for the streak offer the days that came back. */
@Composable
private fun ThanksBlock(rescue: Rescue?) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        BrainMascot(face = MascotFace.Joy, modifier = Modifier.size(140.dp))
        BasicText(
            stringResource(Res.string.pro_thanks),
            style = type.heading.copy(color = colors.accent, textAlign = TextAlign.Center),
            modifier = Modifier.testTag("proThanks"),
        )
        if (rescue != null) {
            BasicText(stringResource(Res.string.rescue_done, rescue.proStreak), style = type.body.copy(color = colors.textPrimary, textAlign = TextAlign.Center))
        }
    }
}

/** The message of the last action, then restore, terms and privacy: at the bottom of both pages, outside the scrolling part. */
@Composable
private fun Footer(ui: PaywallUi) {
    val type = BrainScrollTheme.typography
    // What went wrong is red (a cancelled or failed purchase, no price, nothing to restore); the rest (waiting, adding) is calm.
    // Always centered and always in this one place, above the links, on both pages.
    ui.message?.let {
        val failed = it in listOf(Res.string.pro_cancelled, Res.string.pro_failed, Res.string.pro_price_unavailable, Res.string.pro_restore_none, Res.string.pro_restore_failed)
        BasicText(
            stringResource(it),
            style = type.label.copy(color = if (failed) BrainScrollTheme.colors.error else BrainScrollTheme.colors.textSecondary, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().testTag("proMessage"),
        )
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        LinkText(stringResource(Res.string.pro_restore), ui.restore, Modifier.testTag("proRestore"))
        LinkText(stringResource(Res.string.pro_terms), ui.terms, Modifier.testTag("proTerms"))
        LinkText(stringResource(Res.string.pro_privacy), ui.privacy, Modifier.testTag("proPrivacy"))
    }
}

/** A line of text that is a button, at least 48 dp tall. */
@Composable
private fun LinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = BrainScrollTheme.typography.label.copy(color = BrainScrollTheme.colors.textSecondary, textAlign = TextAlign.Center))
    }
}
