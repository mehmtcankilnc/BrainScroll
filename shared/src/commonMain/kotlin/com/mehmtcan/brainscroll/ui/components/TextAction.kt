package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

/** A text field with a button next to it. */
@Composable
fun TextAction(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    actionLabel: String,
    onAction: () -> Unit,
    /** True inside a card (the field is darker than the card), false on the page (the field is a surface). */
    onSurface: Boolean = true,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(if (onSurface) colors.bgPage else colors.bgSurface, Radius.control)
                .padding(horizontal = Spacing.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = type.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.textPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) BasicText(hint, style = type.body.copy(color = colors.textDisabled), maxLines = 1)
                    inner()
                },
            )
        }
        Box(modifier = Modifier.width(96.dp)) { AppButton(actionLabel, onAction, primary = true) }
    }
}
