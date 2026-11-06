package com.mehmtcan.brainscroll.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.textSoft

/**
 * A few choices in one dark pill that share its width; the chosen one is a filled green pill inside it that slides to the next
 * choice when another is tapped (at once with reduced motion). For a choice that changes what a whole page shows (the level of
 * the statistics). Each choice has a 40 dp tall touch area inside the 48 dp pill.
 */
@Composable
fun PillSegmented(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val reduce = LocalReduceMotion.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.pill)
            .border(1.dp, colors.borderSubtle, Radius.pill)
            .padding(4.dp),
    ) {
        val segment = maxWidth / options.size
        // The green pill is one shape under the texts, and it moves: the texts only change color.
        val x by animateDpAsState(
            targetValue = segment * selectedIndex,
            animationSpec = if (reduce) snap() else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "pillX",
        )
        Box(
            Modifier
                .offset(x = x)
                .size(width = segment, height = 40.dp)
                .background(colors.accent, Radius.pill),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            options.forEachIndexed { index, text ->
                val chosen = index == selectedIndex
                val ink by animateColorAsState(if (chosen) colors.accentOn else colors.textSoft, if (reduce) snap() else tween(Motion.BASE))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .semantics { selected = chosen }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(text, maxLines = 1, style = type.label.copy(color = ink, textAlign = TextAlign.Center))
                }
            }
        }
    }
}
