package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.sound_title
import com.mehmtcan.brainscroll.ui.components.AppSwitch
import com.mehmtcan.brainscroll.ui.components.LineIcon
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** The switch for the sound effects of the games, as a line of the preferences card: a speaker, the name, the switch. On by default. */
@Composable
fun SoundRow(sound: PrivacyUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val title = stringResource(Res.string.sound_title)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        LineIcon(LineIcon.Speaker, colors.textPrimary, size = 24.dp)
        BasicText(title, style = type.heading.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
        AppSwitch(sound.enabled, sound.onChange, title, Modifier.testTag("soundSwitch"))
    }
}
