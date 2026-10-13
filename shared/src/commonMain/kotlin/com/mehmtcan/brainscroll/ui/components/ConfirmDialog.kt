package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing

/**
 * A question with two answers, over the whole screen: the scrim behind it, "Cancel" or the system back button close it
 * and nothing else happens. Only for things that cannot be taken back.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    /** The confirming button is red, for what cannot be taken back. */
    danger: Boolean = false,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Overlay {
    BackHandler(enabled = true, onBack = onCancel)

    Box(modifier = modifier.fillMaxSize().testTag("confirmDialog")) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Spacing.lg)
                .fillMaxWidth()
                .background(colors.bgSurface, Radius.card)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            BasicText(title, style = type.title.copy(color = colors.textPrimary), modifier = Modifier.semantics { heading() })
            BasicText(message, style = type.body.copy(color = colors.textSecondary))
            AppButton(confirmText, onConfirm, modifier = Modifier.testTag("confirmYes"), primary = !danger, danger = danger)
            AppOutlinedButton(cancelText, onCancel, modifier = Modifier.testTag("confirmNo"))
        }
    }
    }
}
