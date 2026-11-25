package com.mehmtcan.brainscroll.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.tour_done
import brainscroll.shared.generated.resources.tour_next
import brainscroll.shared.generated.resources.tour_skip
import brainscroll.shared.generated.resources.tour_step
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.AppTextButton
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.textSoft
import org.jetbrains.compose.resources.stringResource

/** Whether this is a real start of the app (the opening animation and the first-run tour). Tests leave it off so nothing covers the screen. */
val LocalIntro = compositionLocalOf { false }

/** One stop of the tour: the part of the page it points at (null while it is not measured yet) and what it says. */
class TourStop(val target: Rect?, val title: String, val text: String)

/**
 * A short tour for a first start: the page goes dim, one part of it stays bright, a card beside it says what it is. A skip is
 * always there. [step] counts from 0.
 */
@Composable
fun TourOverlay(stops: List<TourStop>, step: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val stop = stops[step]
    val last = step == stops.lastIndex
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxSize().testTag("tour").pointerInput(Unit) { detectTapGestures { } }, // a tap on the dim part does nothing
    ) {
        val height = with(density) { maxHeight.toPx() }
        val target = stop.target
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.74f))
            if (target != null) {
                val pad = 6.dp.toPx()
                val topLeft = Offset(target.left - pad, target.top - pad)
                val size = Size(target.width + 2 * pad, target.height + 2 * pad)
                val corner = CornerRadius(20.dp.toPx())
                drawRoundRect(Color.Black, topLeft, size, corner, blendMode = BlendMode.Clear)
                drawRoundRect(colors.accent, topLeft, size, corner, style = Stroke(2.dp.toPx()))
            }
        }
        // The card goes under the bright part when that is in the upper half of the screen, over it otherwise.
        val below = target == null || target.center.y < height / 2f
        val gap = with(density) { 16.dp.toPx() }
        val card = Modifier
            .align(if (below) Alignment.TopCenter else Alignment.BottomCenter)
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = if (below && target != null) with(density) { (target.bottom + gap).toDp() } else Spacing.xl,
                bottom = if (!below && target != null) with(density) { (height - target.top + gap).toDp() } else Spacing.xl,
            )
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .border(1.dp, colors.borderStrong, Radius.card)
            .padding(Spacing.lg)
        Column(card, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicText(stop.title, style = type.title.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
                BasicText(stringResource(Res.string.tour_step, step + 1, stops.size), style = type.caption.copy(color = colors.textSoft), maxLines = 1)
            }
            BasicText(stop.text, style = type.body.copy(color = colors.textSecondary))
            Row(Modifier.fillMaxWidth().padding(top = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                if (!last) AppTextButton(stringResource(Res.string.tour_skip), onSkip, Modifier.testTag("tourSkip"))
                Spacer(Modifier.weight(1f))
                AppButton(stringResource(if (last) Res.string.tour_done else Res.string.tour_next), onNext, Modifier.width(140.dp).testTag("tourNext"), primary = true)
            }
        }
    }
}
