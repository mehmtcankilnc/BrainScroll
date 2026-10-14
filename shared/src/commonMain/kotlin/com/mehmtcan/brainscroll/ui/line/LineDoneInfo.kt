package com.mehmtcan.brainscroll.ui.line

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.daily_next
import com.mehmtcan.brainscroll.daily.formatCountdown
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.line.lineStars
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** What is known of today's solved board: the stars, the time, and how long until the next one. */
@OptIn(ExperimentalTime::class)
@Composable
fun LineDoneInfo(result: FinishedRound, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var nowMs by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = Clock.System.now().toEpochMilliseconds()
            delay(1000)
        }
    }
    Column(modifier = modifier.testTag("lineDone"), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            LineStars(result.lineStars, size = 24.dp)
            BasicText(formatDuration(result.durationMs ?: 0L), style = type.body.copy(color = colors.textPrimary))
        }
        BasicText(
            stringResource(Res.string.daily_next, formatCountdown(IstanbulDay.millisUntilNextDay(nowMs))),
            style = type.caption.copy(color = colors.textSecondary),
        )
    }
}
