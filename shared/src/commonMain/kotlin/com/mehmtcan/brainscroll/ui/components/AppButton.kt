package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius

/**
 * The app's button (docs/design.md section 7). Secondary: a surface. Primary: filled green, and at most one
 * per screen. At least 48 dp tall so it is easy to hit.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val colors = BrainScrollTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(if (primary) colors.correctFill else colors.bgSurface, Radius.control)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = BrainScrollTheme.typography.label.copy(color = if (primary) colors.correctOn else colors.textPrimary),
        )
    }
}

/**
 * A button that is only an outline in the green of the app, for the second action of a card (the daily puzzle). At least
 * 48 dp tall. It is quieter than the filled [AppButton], which stays the one main button of a screen.
 */
@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .border(1.5.dp, colors.correctFill, Radius.control)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = text, style = BrainScrollTheme.typography.label.copy(color = colors.correctFill))
    }
}
