package com.mehmtcan.brainscroll.ui.wordle

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.daily_game_lost
import brainscroll.shared.generated.resources.daily_time
import brainscroll.shared.generated.resources.daily_game_won
import brainscroll.shared.generated.resources.result_lost
import brainscroll.shared.generated.resources.result_stat_answer
import brainscroll.shared.generated.resources.result_stat_guesses
import brainscroll.shared.generated.resources.result_stat_time
import brainscroll.shared.generated.resources.result_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.world
import org.jetbrains.compose.resources.stringResource

/**
 * The inside of a word puzzle's result card, the same for the feed and the daily puzzle: how it went as the title, then the numbers
 * big (the guesses used and the time when there is one; when it was lost, the word instead of the guesses). A screen reader still gets
 * the sentence the card used to show ("Solved in 3", "The word was ...").
 */
@Composable
internal fun WordResultSummary(won: Boolean, guesses: Int, answer: String?, durationMs: Long?) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val outcome = if (won) stringResource(Res.string.result_won, guesses) else stringResource(Res.string.result_lost, answer.orEmpty())
    val time = durationMs?.let { stringResource(Res.string.daily_time, formatDuration(it)) }
    val summary = if (time != null) "$outcome. $time" else outcome
    BasicText(
        text = stringResource(if (won) Res.string.daily_game_won else Res.string.daily_game_lost),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = type.title.copy(color = if (won) colors.correctFill else colors.textPrimary, textAlign = TextAlign.Center),
    )
    val stats = buildList {
        if (won) {
            add(ResultStatData(guesses.toString(), stringResource(Res.string.result_stat_guesses), GameKind.Word.world))
        } else {
            add(ResultStatData(answer.orEmpty(), stringResource(Res.string.result_stat_answer), colors.textPrimary))
        }
        durationMs?.let { add(ResultStatData(formatDuration(it), stringResource(Res.string.result_stat_time), colors.streakAnswer)) }
    }
    ResultStats(summary, stats)
}
