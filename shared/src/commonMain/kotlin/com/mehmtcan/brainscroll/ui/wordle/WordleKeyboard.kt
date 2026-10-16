package com.mehmtcan.brainscroll.ui.wordle

import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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
 * Delete and Enter sit at the right end of the second and third row, one above the other.
 */
internal fun keyboardRows(language: Language): List<String> = when (language) {
    Language.EN -> listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    Language.TR -> listOf("ERTYUIOPĞÜ", "ASDFGHJKLŞİ", "ZCVBNMÖÇ")
}

private val KeyHeight = 48.dp // minimum touch target, docs/design.md section 4
private val KeyGap = 4.dp

/** The width of the special keys at the right, in letter keys. */
private const val SPECIAL_UNITS = 1.4f

/** Holding delete: the wait before it starts repeating, and the time between two deletions after that. */
private const val HOLD_DELAY_MILLIS = 400L
private const val HOLD_REPEAT_MILLIS = 70L

/** The width of Enter, in letter keys. */
private const val ENTER_UNITS = 3.4f

@Composable
fun WordleKeyboard(
    language: Language,
    letterStates: Map<Char, LetterResult>,
    enterLabel: String,
    onLetter: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
    /** What a screen reader says for the key at the end of the third row; null for the Enter of the word puzzle. */
    enterDescriptionText: String? = null,
) {
    val rows = remember(language) { keyboardRows(language) }
    val enterDescription = enterDescriptionText ?: stringResource(Res.string.a11y_key_enter)
    val backspaceDescription = stringResource(Res.string.a11y_key_backspace)
    // Every key gets the same width and height. Delete (red) ends the second row, a little wider than a letter. Enter (the
    // green of the main button) ends the third row and is as wide as it takes to make that row as long as the second, so
    // the third row has no empty space at its left. Its right edge is in line with delete, right under it. The first row
    // is centered.
    val colors = BrainScrollTheme.colors
    val widest = maxOf(rows[0].length.toFloat(), rows[1].length + SPECIAL_UNITS, rows[2].length + SPECIAL_UNITS)
    // Enter is the same width in every language; the letters of its row share what is left, so that row is as long as the others.
    val enterUnits = ENTER_UNITS
    val thirdRowWeight = (widest - enterUnits) / rows[2].length

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
                horizontalArrangement = Arrangement.spacedBy(KeyGap),
            ) {
                val free = widest - letters.length - SPECIAL_UNITS * (if (rowIndex == 1) 1 else 0)
                if (rowIndex == 0 && free > 0f) Spacer(Modifier.weight(free / 2f))
                letters.forEach { letter ->
                    Key(
                        label = letter.toString(),
                        description = letterKeyDescription(letter.toString(), letterStates[letter]),
                        result = letterStates[letter],
                        onClick = { onLetter(letter) },
                        weight = if (rowIndex == 2) thirdRowWeight else 1f,
                    )
                }
                when (rowIndex) {
                    0 -> if (free > 0f) Spacer(Modifier.weight(free / 2f))
                    1 -> Key(label = "←", description = backspaceDescription, onClick = onBackspace, weight = SPECIAL_UNITS, fill = colors.error, content = colors.textPrimary, tag = "keyBackspace", repeatOnHold = true)
                    else -> Key(label = enterLabel, description = enterDescription, onClick = onEnter, weight = enterUnits, small = true, fill = colors.correctFill, content = colors.correctOn, tag = "keyEnter")
                }
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
    /** A fill of its own, for Enter and delete; null takes the color of the letter's state. */
    fill: Color? = null,
    content: Color? = null,
    tag: String? = null,
    /** Keeps pressing while the key is held (delete): once at the touch, then again and again after a short wait. */
    repeatOnHold: Boolean = false,
) {
    val colors = BrainScrollTheme.colors
    val haptics = LocalHaptics.current
    val press by rememberUpdatedState(onClick)

    val fillColor: Color
    val contentColor: Color
    val borderColor: Color
    if (fill != null) {
        fillColor = fill; contentColor = content ?: colors.textPrimary; borderColor = fill
    } else when (result) {
        LetterResult.Correct -> { fillColor = colors.correctFill; contentColor = colors.correctOn; borderColor = colors.correctFill }
        LetterResult.Present -> { fillColor = colors.bgSurface; contentColor = colors.pendingFill; borderColor = colors.pendingFill }
        LetterResult.Absent -> { fillColor = colors.bgSurface; contentColor = colors.textDisabled; borderColor = colors.bgSurface }
        null -> { fillColor = colors.bgSurface; contentColor = colors.textPrimary; borderColor = colors.bgSurface }
    }

    Box(
        modifier = Modifier
            .weight(weight)
            .height(KeyHeight)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .background(fillColor, Radius.control)
            .border(2.dp, borderColor, Radius.control)
            .then(
                if (repeatOnHold) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                haptics.perform(HapticKind.Light)
                                press()
                                coroutineScope {
                                    val repeating = launch {
                                        delay(HOLD_DELAY_MILLIS)
                                        while (true) {
                                            press()
                                            delay(HOLD_REPEAT_MILLIS)
                                        }
                                    }
                                    tryAwaitRelease()
                                    repeating.cancel()
                                }
                            })
                        }
                        // A screen reader or a switch taps it once; there is no holding there.
                        .semantics {
                            role = Role.Button
                            onClick { press(); true }
                        }
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            haptics.perform(HapticKind.Light)
                            onClick()
                        },
                    )
                },
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val style = if (small) BrainScrollTheme.typography.caption else BrainScrollTheme.typography.label
        BasicText(text = label, style = style.copy(color = contentColor), maxLines = 1)
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
