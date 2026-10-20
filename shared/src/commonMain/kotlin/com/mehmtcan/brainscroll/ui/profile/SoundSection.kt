package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.sound_title
import com.mehmtcan.brainscroll.ui.components.AppSwitch
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** The switch for the sound effects of the games. On by default. */
@Composable
fun SoundSection(sound: PrivacyUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val title = stringResource(Res.string.sound_title)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BasicText(title, style = type.body.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
            AppSwitch(sound.enabled, sound.onChange, title, Modifier.testTag("soundSwitch"))
        }
    }
}
