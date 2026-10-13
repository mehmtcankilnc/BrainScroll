package com.mehmtcan.brainscroll.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius

/**
 * A small on/off switch: the knob slides and the track changes color. The track is 48 x 28 dp, so it is also the touch
 * area. When it is on the knob is on the right, so the state is not shown by color alone.
 */
@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, description: String, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val track by animateColorAsState(if (checked) colors.correctFill else colors.bgRaised, tween(Motion.BASE, easing = Motion.Standard))
    val knobOffset by animateDpAsState(if (checked) 22.dp else 2.dp, tween(Motion.BASE, easing = Motion.Standard))
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .semantics { contentDescription = description }
            .background(track, Radius.pill)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(24.dp)
                .background(if (checked) colors.correctOn else colors.textSecondary, CircleShape),
        )
    }
}
