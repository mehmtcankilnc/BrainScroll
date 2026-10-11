package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

/**
 * A row of choices that share the width, one of them chosen: the chosen one is lifted. Reads as "pick one of these"
 * at a glance, unlike a loose row of chips. Each choice is at least 48 dp tall to touch.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.pill)
            .background(colors.bgSurface)
            .border(1.dp, colors.borderSubtle, Radius.pill)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val chosen = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(Radius.pill)
                    .background(if (chosen) colors.bgRaised else colors.bgSurface)
                    .semantics { selected = chosen }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) { onSelect(index) }
                    .padding(horizontal = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = label,
                    maxLines = 1,
                    style = type.label.copy(
                        color = if (chosen) colors.textPrimary else colors.textSecondary,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}
