package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.favorite_lost
import brainscroll.shared.generated.resources.favorite_won
import brainscroll.shared.generated.resources.favorites_empty
import brainscroll.shared.generated.resources.favorites_title
import brainscroll.shared.generated.resources.guess_distribution
import brainscroll.shared.generated.resources.stat_best_streak
import brainscroll.shared.generated.resources.stat_played
import brainscroll.shared.generated.resources.stat_streak
import brainscroll.shared.generated.resources.stat_win_rate
import brainscroll.shared.generated.resources.stats_title
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.stats.Stats
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.feed.ProfileState
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** Statistics and favorites (docs/plan.md phase 4). Everything shown is derived from the saved results. */
@Composable
fun ProfileScreen(profile: ProfileState, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { Spacer(Modifier.height(Spacing.md)) }
        item {
            BasicText(stringResource(Res.string.stats_title), style = type.title.copy(color = colors.textPrimary))
        }
        item { StatTiles(profile.stats) }
        item { Spacer(Modifier.height(Spacing.sm)) }
        item {
            BasicText(stringResource(Res.string.guess_distribution), style = type.heading.copy(color = colors.textPrimary))
        }
        item { GuessDistribution(profile.stats.guessDistribution) }
        item { Spacer(Modifier.height(Spacing.md)) }
        item {
            BasicText(stringResource(Res.string.favorites_title), style = type.title.copy(color = colors.textPrimary))
        }
        if (profile.favorites.isEmpty()) {
            item {
                BasicText(
                    stringResource(Res.string.favorites_empty),
                    style = type.body.copy(color = colors.textSecondary),
                )
            }
        } else {
            items(profile.favorites, key = { it.id }) { FavoriteRow(it) }
        }
        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun StatTiles(stats: Stats) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
        StatTile(stats.played.toString(), stringResource(Res.string.stat_played), Modifier.weight(1f))
        StatTile("${(stats.winRate * 100).roundToInt()}%", stringResource(Res.string.stat_win_rate), Modifier.weight(1f))
        StatTile(stats.currentStreak.toString(), stringResource(Res.string.stat_streak), Modifier.weight(1f))
        StatTile(stats.bestStreak.toString(), stringResource(Res.string.stat_best_streak), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(
        modifier = modifier.background(colors.bgSurface, Radius.control).padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(value, style = type.display.copy(color = colors.textPrimary))
        BasicText(label, style = type.caption.copy(color = colors.textSecondary))
    }
}

/** One horizontal bar per number of guesses. The longest bar fills the width, the rest are proportional. */
@Composable
private fun GuessDistribution(counts: List<Int>) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val most = counts.maxOrNull()?.coerceAtLeast(1) ?: 1

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        counts.forEachIndexed { i, count ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BasicText("${i + 1}", style = type.label.copy(color = colors.textSecondary), modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    // A bar for zero wins stays a thin stub so the row is still visible.
                    val fraction = if (count == 0) 0.02f else count.toFloat() / most
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(20.dp)
                            .background(if (count == 0) colors.bgSurface else colors.correctFill, Radius.tile),
                    )
                }
                BasicText("$count", style = type.label.copy(color = colors.textPrimary), modifier = Modifier.width(28.dp))
            }
        }
    }
}

@Composable
private fun FavoriteRow(round: FinishedRound) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.control)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        BasicText(round.answer, style = type.heading.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            BasicText(
                text = if (round.outcome == Outcome.WON) stringResource(Res.string.favorite_won, round.guesses.size)
                else stringResource(Res.string.favorite_lost),
                style = type.label.copy(color = if (round.outcome == Outcome.WON) colors.correctFill else colors.textSecondary),
            )
            BasicText(IstanbulDay.format(round.dayIndex), style = type.caption.copy(color = colors.textSecondary))
        }
    }
}
