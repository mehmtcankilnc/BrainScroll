package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.mehmtcan.brainscroll.ui.components.LineIcon
import com.mehmtcan.brainscroll.ui.i18n.AppLanguage
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.textSoft
import org.jetbrains.compose.resources.stringResource

/** The language of the app's texts the player chose (null: follow the device) and the callback to change it. */
data class LanguageUi(val selected: AppLanguage?, val onSelect: (AppLanguage?) -> Unit)

/**
 * The app language as a line of the preferences card: a globe, the name with a few words under it and, at the right, the current
 * choice with an arrow. Tapping the line slides the list of "Device language" and every language of [AppLanguage] out under it
 * while the arrow turns. A new language shows up here by itself.
 */
@Composable
fun LanguageRow(language: LanguageUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var open by remember { mutableStateOf(false) }
    val device = stringResource(Res.string.app_language_device)
    val options: List<Pair<AppLanguage?, String>> =
        listOf<Pair<AppLanguage?, String>>(null to device) + AppLanguage.entries.map { it to it.nativeName }
    val current = options.first { it.first == language.selected }.second
    val arrow by animateFloatAsState(if (open) -90f else 90f, tween(Motion.BASE, easing = Motion.Standard))

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .testTag("appLanguageField")
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.DropdownList) { open = !open }
                .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            LineIcon(LineIcon.Globe, colors.textPrimary, size = 24.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(stringResource(Res.string.app_language_title), style = type.heading.copy(color = colors.textPrimary))
                BasicText(stringResource(Res.string.app_language_text), style = type.caption.copy(color = colors.textSoft))
            }
            BasicText(current, style = type.label.copy(color = colors.textSoft))
            ForwardChevron(colors.textSoft, Modifier.rotate(arrow))
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(Motion.BASE, easing = Motion.Standard)) + fadeIn(tween(Motion.BASE)),
            exit = shrinkVertically(tween(Motion.BASE, easing = Motion.Standard)) + fadeOut(tween(Motion.FAST)),
        ) {
            Column(Modifier.padding(start = Spacing.lg + 24.dp + Spacing.md, end = Spacing.lg, bottom = Spacing.sm)) {
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
                            },
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
