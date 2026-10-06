package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
    /** What a screen reader says instead of [text], for chips that only show a number. */
    description: String? = null,
    /** For chips that are one choice of several: whether this one is the chosen one. Null for plain chips. */
    selected: Boolean? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = BrainScrollTheme.colors
    val semantics = Modifier.semantics(mergeDescendants = true) {
        if (description != null) contentDescription = description
        if (selected != null) this.selected = selected
        if (onClick != null) role = if (selected != null) Role.RadioButton else Role.Button
    }
    val pill: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .background(if (raised) colors.bgRaised else colors.bgSurface, Radius.pill)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            icon?.invoke()
            BasicText(text = text, style = BrainScrollTheme.typography.label.copy(color = textColor))
        }
    }

    if (onClick == null) {
        Box(modifier = semantics) { pill() }
    } else {
        // The pill is small, the touch area is at least 48 dp tall (docs/design.md section 4).
        Box(
            modifier = semantics
                .heightIn(min = 48.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { pill() }
    }
}
