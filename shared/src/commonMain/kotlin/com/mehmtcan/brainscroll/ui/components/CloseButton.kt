package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_close
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import org.jetbrains.compose.resources.stringResource

/** A small "x" that closes the panel it sits on. The drawing is 14 dp, the touch area 48 dp. */
@Composable
fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier, tag: String = "closeButton") {
    val color = BrainScrollTheme.colors.textSecondary
    val description = stringResource(Res.string.a11y_close)
    Box(
        modifier = modifier
            .testTag(tag)
            .size(48.dp)
            .semantics { contentDescription = description }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(14.dp)) {
            val stroke = 2.dp.toPx()
            drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), stroke, StrokeCap.Round)
            drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
        }
    }
}
