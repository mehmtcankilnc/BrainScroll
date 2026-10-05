package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.skip_label
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** The 40 dp bar above the feed (docs/design.md section 7): streak chip, skip chip, language switch. */
@Composable
fun Hud(
    answerStreak: Int,
    skipsLeft: Int,
    language: Language,
    /** Null while the language is locked (the feed has been started). */
    onToggleLanguage: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().height(40.dp).padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Chip(text = answerStreak.toString(), textColor = colors.streakAnswer) { BoltIcon(colors.streakAnswer) }
        Chip(text = stringResource(Res.string.skip_label, skipsLeft), textColor = colors.textSecondary)
        Spacer(Modifier.weight(1f))
        Chip(
            text = language.name,
            textColor = if (onToggleLanguage != null) colors.textPrimary else colors.textDisabled,
            onClick = onToggleLanguage,
        )
    }
}

@Composable
private fun Chip(
    text: String,
    textColor: Color,
    onClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = BrainScrollTheme.colors
    val base = Modifier.background(colors.bgSurface, Radius.pill)
    val clickable = if (onClick == null) base else
        base.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

    Row(
        modifier = clickable.padding(horizontal = Spacing.md, vertical = Spacing.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        icon?.invoke()
        BasicText(text = text, style = BrainScrollTheme.typography.label.copy(color = textColor))
    }
}

/** Small lightning bolt for the answer streak, drawn as a path so it follows the token color. */
@Composable
private fun BoltIcon(color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0.60f * w, 0f)
            lineTo(0.15f * w, 0.55f * h)
            lineTo(0.47f * w, 0.55f * h)
            lineTo(0.38f * w, h)
            lineTo(0.85f * w, 0.42f * h)
            lineTo(0.53f * w, 0.42f * h)
            close()
        }
        drawPath(path, color)
    }
}
