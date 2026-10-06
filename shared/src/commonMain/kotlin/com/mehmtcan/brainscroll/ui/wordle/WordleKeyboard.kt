package com.mehmtcan.brainscroll.ui.wordle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_key_backspace
import brainscroll.shared.generated.resources.a11y_key_enter
import brainscroll.shared.generated.resources.a11y_letter
import brainscroll.shared.generated.resources.a11y_state_absent
import brainscroll.shared.generated.resources.a11y_state_correct
import brainscroll.shared.generated.resources.a11y_state_present
import com.mehmtcan.brainscroll.game.wordle.Language
import org.jetbrains.compose.resources.stringResource
import com.mehmtcan.brainscroll.game.wordle.LetterResult
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius

/**
 * Keyboard rows per language (docs/design.md section 7). Turkish has no Q, W, X and adds `Ğ Ü Ş İ Ö Ç`.
 * The two special keys sit at the ends of the last row.
 */
internal fun keyboardRows(language: Language): List<String> = when (language) {
    Language.EN -> listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    Language.TR -> listOf("ERTYUIOPĞÜ", "ASDFGHJKLŞİ", "ZCVBNMÖÇ")
}

private val KeyHeight = 48.dp // minimum touch target, docs/design.md section 4
private val KeyGap = 4.dp

@Composable
fun WordleKeyboard(
    language: Language,
    letterStates: Map<Char, LetterResult>,
    enterLabel: String,
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(language) { keyboardRows(language) }
    val enterDescription = stringResource(Res.string.a11y_key_enter)
    val backspaceDescription = stringResource(Res.string.a11y_key_backspace)
    // Every key gets the same width: shorter rows are padded with empty space on both sides.
    // The last row also holds the two wider special keys (1.5 units each).
    val unitsPerRow = rows.mapIndexed { i, letters -> letters.length + if (i == rows.lastIndex) 3f else 0f }
    val widest = unitsPerRow.max()

    Column(
        modifier = modifier
            // Dragging on the keys must not turn the feed's vertical pager (docs/design.md section 8):
            // consuming vertical drags here keeps them away from the pager that sits above us.
            .pointerInput(Unit) { detectVerticalDragGestures { change, _ -> change.consume() } },
        verticalArrangement = Arrangement.spacedBy(KeyGap),
    ) {
        rows.forEachIndexed { rowIndex, letters ->
            Row(
                modifier = Modifier.padding(horizontal = KeyGap),
                horizontalArrangement = Arrangement.spacedBy(KeyGap, Alignment.CenterHorizontally),
            ) {
                val isLast = rowIndex == rows.lastIndex
                val pad = (widest - unitsPerRow[rowIndex]) / 2f
                if (pad > 0f) Spacer(Modifier.weight(pad))
                if (isLast) Key(label = enterLabel, description = enterDescription, weight = 1.5f, onClick = onEnter, small = true)
                letters.forEach { letter ->
                    Key(
                        label = letter.toString(),
                        description = letterKeyDescription(letter.toString(), letterStates[letter]),
                        result = letterStates[letter],
                        onClick = { onLetter(letter) },
                    )
                }
                if (isLast) Key(label = "←", description = backspaceDescription, weight = 1.5f, onClick = onBackspace)
                if (pad > 0f) Spacer(Modifier.weight(pad))
            }
        }
    }
}

@Composable
private fun RowScope.Key(
    label: String,
    description: String,
    onClick: () -> Unit,
    result: LetterResult? = null,
    weight: Float = 1f,
    small: Boolean = false,
) {
    val colors = BrainScrollTheme.colors
    val haptics = LocalHaptics.current

    val fill: Color
    val content: Color
    val borderColor: Color
    when (result) {
        LetterResult.Correct -> { fill = colors.correctFill; content = colors.correctOn; borderColor = colors.correctFill }
        LetterResult.Present -> { fill = colors.bgSurface; content = colors.pendingFill; borderColor = colors.pendingFill }
        LetterResult.Absent -> { fill = colors.bgSurface; content = colors.textDisabled; borderColor = colors.bgSurface }
        null -> { fill = colors.bgSurface; content = colors.textPrimary; borderColor = colors.bgSurface }
    }

    Box(
        modifier = Modifier
            .weight(weight)
            .height(KeyHeight)
            .background(fill, Radius.control)
            .border(2.dp, borderColor, Radius.control)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.perform(HapticKind.Light)
                    onClick()
                },
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val style = if (small) BrainScrollTheme.typography.caption else BrainScrollTheme.typography.label
        BasicText(text = label, style = style.copy(color = content))
    }
}

/** "Letter A" for a key not tried yet, "Letter A, not in the word" once the keyboard knows more. */
@Composable
private fun letterKeyDescription(letter: String, result: LetterResult?): String {
    val name = stringResource(Res.string.a11y_letter, letter)
    return when (result) {
        LetterResult.Correct -> name + ", " + stringResource(Res.string.a11y_state_correct)
        LetterResult.Present -> name + ", " + stringResource(Res.string.a11y_state_present)
        LetterResult.Absent -> name + ", " + stringResource(Res.string.a11y_state_absent)
        null -> name
    }
}
