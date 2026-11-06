package com.mehmtcan.brainscroll.ui.components

import com.mehmtcan.brainscroll.ui.theme.textSoft
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

/**
 * One option of a filter: a pill, the chosen one filled and edged in its color. With a [tint] (a game's color) it carries a small
 * dot of that color and is tinted with it when chosen, so the games read as the places they are. [onClick] null makes a chip that
 * only shows what is fixed. The pill is 40 dp tall and its touch area 48 dp.
 */
@Composable
fun FilterChip(text: String, selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, tint: Color? = null, solid: Boolean = false) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val accent = tint ?: colors.textPrimary
    // [solid]: the chosen pill is filled with the green of the app, with dark text.
    val fill by animateColorAsState(
        if (selected && solid) colors.accent else if (selected) accent.copy(alpha = if (tint != null) 0.24f else 0.14f) else colors.bgSurface,
        tween(Motion.BASE),
    )
    val edge by animateColorAsState(
        if (selected && solid) colors.accent else if (selected) accent.copy(alpha = 0.9f) else colors.borderSubtle,
        tween(Motion.BASE),
    )
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { this.selected = selected }
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton, onClick = onClick)
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .background(fill, Radius.pill)
                .border(1.5.dp, edge, Radius.pill)
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (tint != null) Box(Modifier.size(8.dp).background(tint, CircleShape))
            BasicText(
                text = text,
                maxLines = 1,
                style = type.label.copy(color = if (selected && solid) colors.accentOn else if (selected) colors.textPrimary else colors.textSoft),
            )
        }
    }
}
