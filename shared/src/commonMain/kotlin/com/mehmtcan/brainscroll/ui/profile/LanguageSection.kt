package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.app_language_device
import brainscroll.shared.generated.resources.app_language_text
import brainscroll.shared.generated.resources.app_language_title
import com.mehmtcan.brainscroll.ui.components.ForwardChevron
import com.mehmtcan.brainscroll.ui.i18n.AppLanguage
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** The language of the app's texts the player chose (null: follow the device) and the callback to change it. */
data class LanguageUi(val selected: AppLanguage?, val onSelect: (AppLanguage?) -> Unit)

/**
 * The app language as an accordion: the closed field shows the current choice, and opening it slides the list of
 * "Device language" and every language of [AppLanguage] out underneath while the arrow turns. A new language shows up
 * here by itself.
 */
@Composable
fun LanguageSection(language: LanguageUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var open by remember { mutableStateOf(false) }
    val device = stringResource(Res.string.app_language_device)
    val options: List<Pair<AppLanguage?, String>> =
        listOf<Pair<AppLanguage?, String>>(null to device) + AppLanguage.entries.map { it to it.nativeName }
    val current = options.first { it.first == language.selected }.second
    val arrow by animateFloatAsState(if (open) -90f else 90f, tween(Motion.BASE, easing = Motion.Standard))

    Column(
        modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        BasicText(stringResource(Res.string.app_language_title), style = type.heading.copy(color = colors.textPrimary))
        BasicText(stringResource(Res.string.app_language_text), style = type.body.copy(color = colors.textSecondary))
        Column(Modifier.fillMaxWidth().background(colors.bgPage, Radius.control).border(1.dp, colors.borderSubtle, Radius.control)) {
            Row(
                modifier = Modifier
                    .testTag("appLanguageField")
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.DropdownList) { open = !open }
                    .padding(horizontal = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(current, style = type.label.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
                ForwardChevron(colors.textSecondary, Modifier.rotate(arrow))
            }
            AnimatedVisibility(
                visible = open,
                enter = expandVertically(tween(Motion.BASE, easing = Motion.Standard)) + fadeIn(tween(Motion.BASE)),
                exit = shrinkVertically(tween(Motion.BASE, easing = Motion.Standard)) + fadeOut(tween(Motion.FAST)),
            ) {
                Column {
                    options.forEach { (option, name) ->
                        val chosen = option == language.selected
                        Row(
                            modifier = Modifier
                                .testTag("appLanguage_" + (option?.name ?: "DEVICE"))
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .semantics { selected = chosen }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) {
                                    open = false
                                    if (!chosen) language.onSelect(option)
                                }
                                .padding(horizontal = Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicText(name, style = type.body.copy(color = if (chosen) colors.correctFill else colors.textPrimary), modifier = Modifier.weight(1f))
                            if (chosen) BasicText("✓", style = type.label.copy(color = colors.correctFill))
                        }
                    }
                }
            }
        }
    }
}
