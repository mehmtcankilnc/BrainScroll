package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/**
 * A round button with only an icon, large enough to hit easily (the circle is 56 dp, more than the 48 dp minimum).
 * [draw] paints the icon in a square of 28 dp. [selected] is for a button that is a switch: it gets the yellow outline.
 */
@Composable
fun RoundIconButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean? = null,
    tag: String? = null,
    draw: DrawScope.() -> Unit,
) {
    val colors = BrainScrollTheme.colors
    Box(
        modifier = modifier
            .size(56.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(if (selected == true) colors.bgRaised else colors.bgSurface)
            .border(if (selected == true) 2.5.dp else 1.5.dp, if (selected == true) colors.pendingFill else colors.borderStrong.copy(alpha = 0.6f), CircleShape)
            .semantics {
                contentDescription = description
                if (selected != null) this.selected = selected
                if (!enabled) disabled()
            }
            .then(
                if (enabled) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(28.dp)) { draw() }
    }
}
