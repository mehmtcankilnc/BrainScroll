package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

/** A short message in a raised pill, used for hints and results ("Not in word list", "Signed in"). */
@Composable
fun NoticePill(text: String, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    BasicText(
        text = text,
        style = BrainScrollTheme.typography.label.copy(color = colors.textPrimary),
        modifier = modifier
            .padding(top = Spacing.xs)
            .background(colors.bgRaised, Radius.pill)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}
