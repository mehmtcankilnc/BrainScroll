package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius

/** The three guess states of a word-puzzle tile. Shapes differ, not only colors (docs/design.md section 9). */
enum class TileState { Correct, Pending, Absent }

@Composable
fun LetterTile(
    letter: String,
    state: TileState,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors

    val fill: Color
    val content: Color
    val border: Color
    val borderWidth: Float
    when (state) {
        TileState.Correct -> {
            fill = colors.correctFill
            content = colors.correctOn
            border = colors.correctFill
            borderWidth = 0f
        }
        TileState.Pending -> {
            fill = Color.Transparent
            content = colors.pendingFill
            border = colors.pendingFill
            borderWidth = 2.5f
        }
        TileState.Absent -> {
            fill = colors.absentFill
            content = colors.absentOn
            border = colors.absentFill
            borderWidth = 0f
        }
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .background(fill, Radius.tile)
            .border(borderWidth.dp, border, Radius.tile),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = letter,
            style = BrainScrollTheme.typography.tile.copy(color = content),
        )
    }
}
