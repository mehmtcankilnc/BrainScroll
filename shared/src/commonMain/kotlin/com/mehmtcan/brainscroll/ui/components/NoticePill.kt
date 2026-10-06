package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

/**
 * A short message in a raised pill, used for hints and results ("Not in word list", "Signed in").
 * [detail] is an optional second line in small print, for example an error code.
 */
@Composable
fun NoticePill(text: String, modifier: Modifier = Modifier, detail: String? = null) {
    val colors = BrainScrollTheme.colors
    Column(
        // A screen reader reads a message when it appears, without the player having to look for it.
        modifier = modifier
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(top = Spacing.xs)
            .background(colors.bgRaised, Radius.card)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(text = text, style = BrainScrollTheme.typography.label.copy(color = colors.textPrimary, textAlign = TextAlign.Center))
        if (detail != null) {
            BasicText(text = detail, style = BrainScrollTheme.typography.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center))
        }
    }
}
