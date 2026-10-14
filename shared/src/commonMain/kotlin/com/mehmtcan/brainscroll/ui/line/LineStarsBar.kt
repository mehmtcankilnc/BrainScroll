package com.mehmtcan.brainscroll.ui.line

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_line_stars
import brainscroll.shared.generated.resources.a11y_line_stars_info
import brainscroll.shared.generated.resources.howto_done
import brainscroll.shared.generated.resources.line_stars_info_counts
import brainscroll.shared.generated.resources.line_stars_info_hint
import brainscroll.shared.generated.resources.line_stars_info_intro
import brainscroll.shared.generated.resources.line_stars_info_rank
import brainscroll.shared.generated.resources.line_stars_info_title
import brainscroll.shared.generated.resources.line_stars_rule_0
import brainscroll.shared.generated.resources.line_stars_rule_1
import brainscroll.shared.generated.resources.line_stars_rule_2
import com.mehmtcan.brainscroll.game.line.LineScoring
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.ModalSheet
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** How long a lost star takes to fall away. */
internal const val STAR_LOSS_MILLIS = 600

/** How far each star of the bar is filled right now (1 = whole, 0 = gone), for tests of the animation. */
internal val LineStarProgress = SemanticsPropertyKey<List<Float>>("LineStarProgress")
private var SemanticsPropertyReceiver.lineStarProgress by LineStarProgress

/**
 * The stars the board is worth right now: three at the start, and one goes whenever the undos or a hint cost it. A lost
 * star tips over, spins and fades as it falls, leaving the outline behind (a snap with reduced motion). The round "i"
 * next to them opens [LineStarsInfoSheet].
 */
@Composable
internal fun LineStarsBar(stars: Int, onInfo: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val reduce = LocalReduceMotion.current
    // A new board starts at its value (no animation); only a change while the board is on screen animates.
    val progress = List(LineScoring.MAX_STARS) { index ->
        animateFloatAsState(
            targetValue = if (index < stars) 1f else 0f,
            animationSpec = if (reduce) snap() else tween(STAR_LOSS_MILLIS, easing = Motion.Standard),
            label = "lineStar$index",
        ).value
    }
    val description = stringResource(Res.string.a11y_line_stars, stars)
    val infoDescription = stringResource(Res.string.a11y_line_stars_info)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier = Modifier.testTag("lineStarsLive").semantics(mergeDescendants = true) {
                contentDescription = description
                lineStarProgress = progress
            },
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            progress.forEach { p ->
                Canvas(Modifier.size(30.dp)) {
                    drawStar(colors.borderStrong, filled = false)
                    if (p > 0.001f) {
                        val s = size.minDimension
                        withTransform({
                            translate(top = (1f - p) * s * 0.35f)
                            rotate((1f - p) * 140f, pivot = Offset(s / 2, s / 2))
                            scale(p, p, pivot = Offset(s / 2, s / 2))
                        }) { drawStar(colors.streakDay.copy(alpha = p), filled = true) }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .testTag("lineStarsInfo")
                .size(40.dp)
                .semantics { contentDescription = infoDescription }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onInfo),
            contentAlignment = Alignment.Center,
        ) {
            val ring = colors.textSecondary
            Canvas(Modifier.size(20.dp)) {
                val s = size.minDimension
                drawCircle(ring, radius = s * 0.46f, style = Stroke(width = s * 0.09f))
                drawCircle(ring, radius = s * 0.06f, center = Offset(s / 2, s * 0.30f))
                drawLine(ring, Offset(s / 2, s * 0.45f), Offset(s / 2, s * 0.74f), strokeWidth = s * 0.10f, cap = StrokeCap.Round)
            }
        }
    }
}

/** How stars are earned, as a panel over the page in the same style as the rules of a game. */
@Composable
internal fun LineStarsInfoSheet(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    ModalSheet(onDismiss = onClose, modifier = modifier, tag = "lineStarsSheet") { close ->
        BasicText(
            stringResource(Res.string.line_stars_info_title),
            style = type.title.copy(color = colors.textPrimary),
            modifier = Modifier.semantics { heading() },
        )
        BasicText(stringResource(Res.string.line_stars_info_intro), style = type.body.copy(color = colors.textPrimary))
        listOf(
            LineScoring.MAX_STARS to Res.string.line_stars_rule_0,
            LineScoring.MAX_STARS - 1 to Res.string.line_stars_rule_1,
            LineScoring.MAX_STARS - 2 to Res.string.line_stars_rule_2,
        ).forEach { (earned, rule) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                LineStars(earned, size = 22.dp)
                BasicText(stringResource(rule), style = type.body.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
            }
        }
        BasicText(stringResource(Res.string.line_stars_info_hint), style = type.body.copy(color = colors.textPrimary))
        BasicText(stringResource(Res.string.line_stars_info_counts), style = type.body.copy(color = colors.textSecondary))
        BasicText(stringResource(Res.string.line_stars_info_rank), style = type.body.copy(color = colors.textSecondary))
        AppButton(text = stringResource(Res.string.howto_done), onClick = close, primary = true, modifier = Modifier.testTag("lineStarsSheetDone"))
    }
}
