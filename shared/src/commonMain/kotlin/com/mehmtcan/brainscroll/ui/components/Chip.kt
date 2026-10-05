package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

/**
 * A small pill with a label and an optional icon (docs/design.md section 7). It is clickable when [onClick] is
 * given. [raised] is the "selected" look: a lighter surface.
 */
@Composable
fun Chip(
    text: String,
    textColor: Color,
    onClick: (() -> Unit)? = null,
    raised: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = BrainScrollTheme.colors
    val base = Modifier.background(if (raised) colors.bgRaised else colors.bgSurface, Radius.pill)
    val clickable = if (onClick == null) base else
        base.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

    Row(
        modifier = clickable.padding(horizontal = Spacing.md, vertical = Spacing.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        icon?.invoke()
        BasicText(text = text, style = BrainScrollTheme.typography.label.copy(color = textColor))
    }
}
