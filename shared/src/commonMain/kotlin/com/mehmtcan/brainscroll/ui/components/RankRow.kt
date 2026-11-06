package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.daily.FlameIcon
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

/**
 * One line of a table, on the ranks page and on the home page: the place, the avatar, the name and, at the right, the main
 * number with a smaller one under it. Every row is a small card; mine is tinted light green (the color of "correct") and edged.
 * [displayName] is the name as it should read ("carol (you)" for me). [flame] puts the streak flame beside the main number.
 */
@Composable
fun RankRow(
    rank: Int,
    name: String,
    displayName: String,
    isMe: Boolean,
    primary: String,
    secondary: String? = null,
    flame: Boolean = false,
    /** When set, this many stars are drawn in place of the main text (the One Line table). */
    stars: Int? = null,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .background(colors.bgSurface, Radius.control)
            .then(
                if (isMe) Modifier.background(colors.correctFill.copy(alpha = 0.18f), Radius.control).border(1.5.dp, colors.correctFill.copy(alpha = 0.6f), Radius.control)
                else Modifier,
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        BasicText(rank.toString(), modifier = Modifier.width(28.dp), style = type.label.copy(color = colors.textSecondary, textAlign = TextAlign.Center))
        PlayerAvatar(name, 36.dp)
        BasicText(
            text = displayName,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = type.body.copy(color = colors.textPrimary, fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal),
        )
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (stars != null) StarCount(stars) else BasicText(primary, style = type.heading.copy(color = colors.textPrimary))
                if (flame) FlameIcon(colors.streakDay)
            }
            secondary?.let { BasicText(it, style = type.caption.copy(color = colors.textSecondary)) }
        }
    }
}
