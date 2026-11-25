package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.graphics.graphicsLayer
import brainscroll.shared.generated.resources.guide_button
import brainscroll.shared.generated.resources.guide_left
import brainscroll.shared.generated.resources.guide_left_both
import brainscroll.shared.generated.resources.guide_left_bonus
import brainscroll.shared.generated.resources.guide_left_none
import com.mehmtcan.brainscroll.game.guide.QuotaState
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import org.jetbrains.compose.resources.stringResource

/**
 * The light bulb in the bar next to the win streak: it asks the guide for the next move ("Guide me"). It is only an icon,
 * and in the bar, so it is never over the puzzle; the sentence of the guide shows under the board.
 */
@Composable
fun GuideIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, quota: QuotaState? = null) {
    val colors = BrainScrollTheme.colors
    // With a quota the bulb says how many free steps are left, and goes dim at none (still a button: it opens the purchase screen).
    val showQuota = quota != null && !quota.unlimited
    val free = quota?.freeLeft ?: 0
    val bought = quota?.bonusLeft ?: 0
    // The badge counts the free steps and the bought hints together: both are one more step the bulb can give.
    val left = free + bought
    val description = when {
        showQuota && left == 0 -> stringResource(Res.string.guide_left_none)
        showQuota && bought == 0 -> stringResource(Res.string.guide_left, free)
        showQuota && free == 0 -> stringResource(Res.string.guide_left_bonus, bought)
        showQuota -> stringResource(Res.string.guide_left_both, free, bought)
        else -> stringResource(Res.string.guide_button)
    }
    Box(
        modifier = modifier
            .testTag("guideButton")
            .size(44.dp)
            .semantics { contentDescription = description }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(32.dp).graphicsLayer { alpha = if (showQuota && left == 0) 0.45f else 1f }.background(colors.bgSurface, CircleShape), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val stroke = 1.8.dp.toPx()
                val w = size.width
                val h = size.height
                val line = Stroke(width = stroke, cap = StrokeCap.Round)
                // The bulb, and under it the base.
                drawArc(colors.pendingFill, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(0.17f * w, 0.02f * h), size = Size(0.66f * w, 0.66f * h), style = line)
                drawLine(colors.pendingFill, Offset(0.36f * w, 0.82f * h), Offset(0.64f * w, 0.82f * h), stroke, StrokeCap.Round)
                drawLine(colors.pendingFill, Offset(0.41f * w, 0.97f * h), Offset(0.59f * w, 0.97f * h), stroke, StrokeCap.Round)
            }
        }
        if (showQuota) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(if (left == 0) colors.bgRaised else colors.pendingFill, CircleShape)
                    .testTag("guideBadge"),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(left.toString(), style = BrainScrollTheme.typography.caption.copy(color = if (left == 0) colors.textSecondary else colors.pendingOn))
            }
        }
    }
}
