package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.foundation.Canvas
import brainscroll.shared.generated.resources.tab_home
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.howto.HowToIconButton
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_answer_streak
import com.mehmtcan.brainscroll.ui.motion.popOnIncrease
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.ChevronButton
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * The bar above the feed: the way back, the name of the game on the page and the "?" that opens its rules, and on the
 * right the win streak (tap for what it means). With word puzzles in more than one language, [badge] says which one
 * the page is in.
 */
@Composable
fun Hud(
    title: String,
    badge: String?,
    answerStreak: Int,
    /** Back to the home page. */
    onHome: () -> Unit,
    onHowToPlay: () -> Unit,
    onStreak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(
        modifier = modifier.fillMaxWidth().height(48.dp).padding(start = Spacing.sm, end = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChevronButton(stringResource(Res.string.tab_home), colors.textPrimary, onHome, touch = 48.dp)
        BasicText(
            text = title,
            style = type.heading.copy(color = colors.textPrimary),
            maxLines = 1,
            modifier = Modifier.padding(start = Spacing.xs).semantics { heading() }.testTag("feedTitle"),
        )
        if (badge != null) {
            BasicText(
                text = badge,
                style = type.label.copy(color = colors.textSecondary),
                modifier = Modifier.padding(start = Spacing.sm).testTag("languageBadge"),
            )
        }
        HowToIconButton(onHowToPlay)
        Spacer(Modifier.weight(1f))
        Box(Modifier.popOnIncrease(answerStreak).testTag("winStreak")) {
            Chip(
                text = answerStreak.toString(),
                textColor = colors.streakAnswer,
                description = stringResource(Res.string.a11y_answer_streak, answerStreak),
                onClick = onStreak,
            ) { BoltIcon(colors.streakAnswer) }
        }
    }
}

/** Small lightning bolt for the answer streak, drawn as a path so it follows the token color. */
@Composable
internal fun BoltIcon(color: Color) {
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
