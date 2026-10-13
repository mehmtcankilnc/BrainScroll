package com.mehmtcan.brainscroll.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

/**
 * A row of choices that share the width, one of them chosen: plain text on a hairline, the chosen one in the main color
 * with a short green bar under it, the same way the tab bar shows where you are. Each choice is at least 48 dp tall.
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val chosen = index == selectedIndex
                val textColor by animateColorAsState(if (chosen) colors.textPrimary else colors.textSecondary, tween(Motion.BASE))
                val barColor by animateColorAsState(if (chosen) colors.correctFill else colors.bgPage.copy(alpha = 0f), tween(Motion.BASE))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { selected = chosen }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) { onSelect(index) }
                        .padding(horizontal = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 45.dp), contentAlignment = Alignment.Center) {
                        BasicText(
                            text = label,
                            maxLines = 1,
                            style = type.label.copy(color = textColor, textAlign = TextAlign.Center),
                        )
                    }
                    Box(Modifier.width(24.dp).height(3.dp).background(barColor, Radius.pill))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
    }
}
