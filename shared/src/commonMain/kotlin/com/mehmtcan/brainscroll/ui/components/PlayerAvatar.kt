package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world

/** The color of a player's avatar: one of the game worlds, the same for the same name every time. */
internal fun avatarColor(name: String): Color {
    val worlds = GameKind.entries.map { it.world }
    return worlds[(name.hashCode() and Int.MAX_VALUE) % worlds.size]
}

/**
 * A round badge with the first letter of a name, in a color that belongs to that name (the games' colors, so players look like
 * they come from different worlds). [ring] replaces the color of the edge, for a place on the podium or for "me".
 * It draws no description of its own: the name is always written next to it.
 */
@Composable
fun PlayerAvatar(name: String, size: Dp, modifier: Modifier = Modifier, ring: Color? = null, ringWidth: Dp = 1.5.dp) {
    val color = avatarColor(name)
    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = 0.25f), CircleShape)
            .border(ringWidth, ring ?: color.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?",
            style = BrainScrollTheme.typography.heading.copy(color = color, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold),
        )
    }
}
