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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_letter
import brainscroll.shared.generated.resources.a11y_state_absent
import brainscroll.shared.generated.resources.a11y_state_correct
import brainscroll.shared.generated.resources.a11y_state_present
import brainscroll.shared.generated.resources.a11y_state_typed
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import org.jetbrains.compose.resources.stringResource
import com.mehmtcan.brainscroll.ui.theme.Radius

/**
 * Tile states of a word-puzzle grid. Shapes differ, not only colors (docs/design.md section 9).
 * [Empty] and [Filled] are the typing states, the other three are guess results.
 */
enum class TileState { Empty, Filled, Correct, Pending, Absent }

@Composable
fun LetterTile(
    letter: String,
    state: TileState,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val description = tileDescription(letter, state)

    val fill: Color
    val content: Color
    val border: Color
    val borderWidth: Float
    when (state) {
        TileState.Empty -> {
            fill = Color.Transparent
            content = colors.textPrimary
            border = colors.borderSubtle
            borderWidth = 2f
        }
        TileState.Filled -> {
            fill = Color.Transparent
            content = colors.textPrimary
            border = colors.borderStrong
            borderWidth = 2f
        }
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
            .size(size)
            .background(fill, Radius.tile)
            .border((if (borderWidth > 0f) borderWidth + extraBorder else 0f).dp, border, Radius.tile)
            .then(if (description != null) Modifier.semantics(mergeDescendants = true) { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = letter,
            style = BrainScrollTheme.typography.tile.copy(color = content),
        )
    }
}

/**
 * What a screen reader says for a tile, for example "Letter A, correct place" (docs/design.md section 9).
 * An empty tile says nothing, so a grid of 30 tiles does not read out 30 times.
 */
@Composable
private fun tileDescription(letter: String, state: TileState): String? {
    if (state == TileState.Empty || letter.isEmpty()) return null
    val status = when (state) {
        TileState.Correct -> stringResource(Res.string.a11y_state_correct)
        TileState.Pending -> stringResource(Res.string.a11y_state_present)
        TileState.Absent -> stringResource(Res.string.a11y_state_absent)
        else -> stringResource(Res.string.a11y_state_typed)
    }
    return stringResource(Res.string.a11y_letter, letter) + ", " + status
}
