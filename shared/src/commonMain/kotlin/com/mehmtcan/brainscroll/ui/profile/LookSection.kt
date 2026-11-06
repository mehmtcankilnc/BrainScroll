package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import brainscroll.shared.generated.resources.look_pro_hint
import brainscroll.shared.generated.resources.pro_badge
import com.mehmtcan.brainscroll.ui.theme.on
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.accent_blue
import brainscroll.shared.generated.resources.accent_green
import brainscroll.shared.generated.resources.accent_orange
import brainscroll.shared.generated.resources.accent_yellow
import brainscroll.shared.generated.resources.look_accent
import brainscroll.shared.generated.resources.look_amoled
import brainscroll.shared.generated.resources.look_title
import brainscroll.shared.generated.resources.look_use_pro
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.AppSwitch
import com.mehmtcan.brainscroll.ui.theme.Accent
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Look
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.fill
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** A small padlock. */
@Composable
private fun LockIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        // The shackle, then the body over its feet.
        drawArc(color, 180f, 180f, false, Offset(0.25f * w, 0.05f * h), Size(0.5f * w, 0.7f * h), style = Stroke(stroke))
        drawRoundRect(color, Offset(0.12f * w, 0.42f * h), Size(0.76f * w, 0.52f * h), CornerRadius(2.dp.toPx()))
    }
}

/** The look settings: the pick, whether Pro is on, and what the controls do. */
data class LookUi(val look: Look, val pro: Boolean, val onAccent: (Accent) -> Unit, val onAmoled: (Boolean) -> Unit, val onPro: () -> Unit)

private const val PREVIEW_MILLIS = 2_000L

private fun Accent.label(): StringResource = when (this) {
    Accent.Green -> Res.string.accent_green
    Accent.Blue -> Res.string.accent_blue
    Accent.Orange -> Res.string.accent_orange
    Accent.Yellow -> Res.string.accent_yellow
}

/**
 * The accent color of the app's controls and the pure black page (docs/pro-plan.md section 8.5). Pro picks them. Without
 * Pro a tap on a color shows it for two seconds and offers "Use with Pro" under the colors; nothing opens by itself. The
 * colors of right and wrong in the games are not part of this.
 */
@Composable
fun LookSection(ui: LookUi, onPreview: (Accent?) -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var previewTick by remember { mutableStateOf(0) }
    var offerPro by remember { mutableStateOf(false) }
    // Each preview ends by itself after two seconds; the offer stays.
    LaunchedEffect(previewTick) {
        if (previewTick > 0) {
            delay(PREVIEW_MILLIS)
            onPreview(null)
        }
    }
    val shownAccent = if (ui.pro) ui.look.accent else Accent.Green
    val title = stringResource(Res.string.look_amoled)
    Column(
        modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            BasicText(stringResource(Res.string.look_title), style = type.heading.copy(color = colors.textPrimary))
            // Without Pro the section is a preview, and it says so at the top.
            if (!ui.pro) {
                BasicText(
                    text = stringResource(Res.string.pro_badge),
                    style = type.caption.copy(color = colors.accentOn),
                    modifier = Modifier.background(colors.accent, Radius.pill).padding(horizontal = Spacing.sm, vertical = 2.dp),
                )
            }
        }
        if (!ui.pro) BasicText(stringResource(Res.string.look_pro_hint), style = type.label.copy(color = colors.textSecondary))
        BasicText(stringResource(Res.string.look_accent), style = type.label.copy(color = colors.textSecondary))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Accent.entries.forEach { accent ->
                val name = stringResource(accent.label())
                val chosen = accent == shownAccent
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = name; role = Role.RadioButton; selected = chosen }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (ui.pro) {
                                ui.onAccent(accent)
                            } else if (accent != Accent.Green) {
                                offerPro = true
                                onPreview(accent)
                                previewTick += 1
                            }
                        }
                        .testTag("accent_${accent.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    val locked = !ui.pro && accent != Accent.Green
                    Box(
                        Modifier
                            .size(32.dp)
                            .graphicsLayer { alpha = if (locked) 0.55f else 1f }
                            .background(accent.fill, CircleShape)
                            // The chosen one is shown by a ring and not by color alone.
                            .then(if (chosen) Modifier.border(3.dp, colors.textPrimary, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        // A padlock on what needs Pro, so a color that only previews does not look usable.
                        if (locked) LockIcon(accent.on)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BasicText(title, style = type.body.copy(color = if (ui.pro) colors.textPrimary else colors.textSecondary), modifier = Modifier.weight(1f))
            if (!ui.pro) LockIcon(colors.textSecondary)
            AppSwitch(
                checked = ui.pro && ui.look.amoled,
                onCheckedChange = { if (ui.pro) ui.onAmoled(it) else offerPro = true },
                description = title,
                modifier = Modifier.graphicsLayer { alpha = if (ui.pro) 1f else 0.5f }.testTag("amoledSwitch"),
            )
        }
        if (!ui.pro && offerPro) {
            AppButton(stringResource(Res.string.look_use_pro), onClick = ui.onPro, primary = true, modifier = Modifier.testTag("lookUsePro"))
        }
    }
}
