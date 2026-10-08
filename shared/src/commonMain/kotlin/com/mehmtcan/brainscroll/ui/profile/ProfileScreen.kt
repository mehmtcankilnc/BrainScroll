package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import brainscroll.shared.generated.resources.profile_settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.daily_freeze
import brainscroll.shared.generated.resources.favorite_daily_tag
import brainscroll.shared.generated.resources.favorite_lost
import brainscroll.shared.generated.resources.favorite_won
import brainscroll.shared.generated.resources.favorites_empty
import brainscroll.shared.generated.resources.favorites_title
import brainscroll.shared.generated.resources.guess_distribution
import brainscroll.shared.generated.resources.stat_best_streak
import brainscroll.shared.generated.resources.stat_best_time
import brainscroll.shared.generated.resources.stat_day_streak
import brainscroll.shared.generated.resources.stat_played
import brainscroll.shared.generated.resources.stat_streak
import brainscroll.shared.generated.resources.stat_win_rate
import brainscroll.shared.generated.resources.favorite_memory_result
import brainscroll.shared.generated.resources.favorite_memory_tag
import brainscroll.shared.generated.resources.stat_fewest_moves
import brainscroll.shared.generated.resources.stats_daily
import brainscroll.shared.generated.resources.stats_memory
import brainscroll.shared.generated.resources.stats_tubes
import brainscroll.shared.generated.resources.favorite_tubes_result
import brainscroll.shared.generated.resources.favorite_tubes_tag
import brainscroll.shared.generated.resources.stats_filter_all
import brainscroll.shared.generated.resources.stats_streak_note
import brainscroll.shared.generated.resources.stats_endless
import brainscroll.shared.generated.resources.stats_title
import brainscroll.shared.generated.resources.tab_ranks
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.feed.ProfileState
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** Where the profile can lead. Each is a page of its own with a back button (docs/decisions.md, Sadeleştirme). */
enum class ProfileTarget { Stats, Favorites, Settings, Ranks }

/**
 * The profile tab: one summary and a short list. The statistics, the favorites and the settings are pages of their
 * own, so this screen stays short.
 */
@Composable
fun ProfileScreen(profile: ProfileState, onOpen: (ProfileTarget) -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    // The endless numbers already include memory match, so only the daily puzzle is added.
    val played = profile.stats.played + profile.daily.played
    val won = profile.stats.won + profile.daily.won
    val winRate = if (played == 0) 0f else won.toFloat() / played

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Spacer(Modifier.height(Spacing.md))
        // The one surface on this screen: three numbers that sum the player up.
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(vertical = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SummaryNumber(played.toString(), stringResource(Res.string.stat_played))
            SummaryNumber("${(winRate * 100).roundToInt()}%", stringResource(Res.string.stat_win_rate))
            SummaryNumber(profile.dayStreak.best.toString(), stringResource(Res.string.stat_best_streak))
        }
        Column {
            ProfileRow(stringResource(Res.string.stats_title)) { onOpen(ProfileTarget.Stats) }
            ProfileRow(stringResource(Res.string.favorites_title), profile.favorites.size.takeIf { it > 0 }?.toString()) { onOpen(ProfileTarget.Favorites) }
            ProfileRow(stringResource(Res.string.tab_ranks)) { onOpen(ProfileTarget.Ranks) }
            ProfileRow(stringResource(Res.string.profile_settings), last = true) { onOpen(ProfileTarget.Settings) }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun SummaryNumber(value: String, label: String) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(value, style = type.display.copy(color = colors.textPrimary), maxLines = 1, softWrap = false)
        BasicText(label, style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center), maxLines = 2)
    }
}

/** A line of the list: a name, an optional small value and a chevron. No surface, a hairline below. */
@Composable
private fun ProfileRow(title: String, value: String? = null, last: Boolean = false, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BasicText(title, style = type.heading.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
            if (value != null) BasicText(value, style = type.label.copy(color = colors.textSecondary))
            Canvas(Modifier.size(12.dp)) {
                val path = Path().apply {
                    moveTo(0.3f * size.width, 0.1f * size.height)
                    lineTo(0.75f * size.width, 0.5f * size.height)
                    lineTo(0.3f * size.width, 0.9f * size.height)
                }
                drawPath(path, colors.textSecondary, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
    }
}

/** The numbers: the endless feed, memory match and the daily puzzle. Everything is derived from the saved results. */
@Composable
fun StatsPage(profile: ProfileState, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    // Null shows both languages together.
    var dailyLanguage by remember { mutableStateOf<Language?>(null) }
    val daily = dailyLanguage?.let { profile.dailyByLanguage[it] } ?: profile.daily

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { BasicText(stringResource(Res.string.stats_title), style = type.title.copy(color = colors.textPrimary)) }

        // The endless feed.
        item { BasicText(stringResource(Res.string.stats_endless), style = type.heading.copy(color = colors.textPrimary)) }
        item {
            StatTiles(
                played = profile.stats.played,
                winRate = profile.stats.winRate,
                streak = profile.stats.currentStreak,
                best = profile.stats.bestStreak,
                streakLabel = stringResource(Res.string.stat_streak),
            )
        }
        item { GuessDistribution(profile.stats.guessDistribution) }
        item { Spacer(Modifier.height(Spacing.sm)) }

        // Memory match: it has no guesses, so its numbers are the results, the fastest win and the fewest moves.
        item { BasicText(stringResource(Res.string.stats_memory), style = type.heading.copy(color = colors.textPrimary)) }
        item {
            TileGrid(
                listOf(
                    profile.memory.played.toString() to stringResource(Res.string.stat_played),
                    "${(profile.memory.winRate * 100).roundToInt()}%" to stringResource(Res.string.stat_win_rate),
                    (profile.memory.bestTimeMs?.let(::formatDuration) ?: "-") to stringResource(Res.string.stat_best_time),
                    (profile.memory.fewestMoves?.toString() ?: "-") to stringResource(Res.string.stat_fewest_moves),
                ),
            )
        }
        item { Spacer(Modifier.height(Spacing.sm)) }

        // Color tubes: every game is a win, so there is no win rate, only the fastest game and the fewest moves.
        item { BasicText(stringResource(Res.string.stats_tubes), style = type.heading.copy(color = colors.textPrimary)) }
        item {
            TileGrid(
                listOf(
                    profile.tubes.played.toString() to stringResource(Res.string.stat_played),
                    (profile.tubes.bestTimeMs?.let(::formatDuration) ?: "-") to stringResource(Res.string.stat_best_time),
                    (profile.tubes.fewestMoves?.toString() ?: "-") to stringResource(Res.string.stat_fewest_moves),
                ),
            )
        }
        item { Spacer(Modifier.height(Spacing.sm)) }

        // The daily puzzle: its streak counts days, not answers, and it has a time. The numbers can be shown for
        // both languages together or for one (there are two puzzles a day, one per language).
        item { BasicText(stringResource(Res.string.stats_daily), style = type.heading.copy(color = colors.textPrimary)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DailyFilterChip(stringResource(Res.string.stats_filter_all), dailyLanguage == null) { dailyLanguage = null }
                Language.entries.forEach { language ->
                    DailyFilterChip(language.name, dailyLanguage == language) { dailyLanguage = language }
                }
            }
        }
        item {
            StatTiles(
                played = daily.played,
                winRate = daily.winRate,
                streak = profile.dayStreak.current,
                best = profile.dayStreak.best,
                streakLabel = stringResource(Res.string.stat_day_streak),
            )
        }
        if (dailyLanguage != null) {
            item {
                BasicText(
                    text = stringResource(Res.string.stats_streak_note),
                    style = type.caption.copy(color = colors.textSecondary),
                )
            }
        }
        daily.bestTimeMs?.let { best ->
            item {
                BasicText(
                    text = stringResource(Res.string.stat_best_time) + ": " + formatDuration(best),
                    style = type.body.copy(color = colors.textPrimary),
                )
            }
        }
        if (profile.dayStreak.freezesAvailable > 0) {
            item {
                BasicText(
                    text = stringResource(Res.string.daily_freeze, profile.dayStreak.freezesAvailable),
                    style = type.caption.copy(color = colors.textSecondary),
                )
            }
        }
        item { GuessDistribution(daily.guessDistribution) }
        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

/** The finished puzzles the player marked with the heart. */
@Composable
fun FavoritesPage(profile: ProfileState, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { BasicText(stringResource(Res.string.favorites_title), style = type.title.copy(color = colors.textPrimary)) }
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

/** The account and the privacy choice. */
@Composable
fun SettingsPage(account: AccountUi, privacy: PrivacyUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { BasicText(stringResource(Res.string.profile_settings), style = type.title.copy(color = colors.textPrimary)) }
        item { AccountSection(account) }
        item { PrivacySection(privacy) }
        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun StatTiles(played: Int, winRate: Float, streak: Int, best: Int, streakLabel: String) {
    TileGrid(
        listOf(
            played.toString() to stringResource(Res.string.stat_played),
            "${(winRate * 100).roundToInt()}%" to stringResource(Res.string.stat_win_rate),
            streak.toString() to streakLabel,
            best.toString() to stringResource(Res.string.stat_best_streak),
        ),
    )
}

/** Value and label pairs as tiles: four in a row, two by two when the player chose big text. */
@Composable
private fun TileGrid(tiles: List<Pair<String, String>>) {
    // Four tiles side by side do not fit when the player chose big text: then two rows of two.
    val perRow = if (LocalDensity.current.fontScale > LARGE_TEXT_SCALE) 2 else 4
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        tiles.chunked(perRow).forEach { row ->
            // IntrinsicSize.Max makes the tiles as tall as the tallest one, even if one uses a smaller number size.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                row.forEach { (value, label) -> StatTile(value, label, Modifier.weight(1f)) }
            }
        }
    }
}

/** Above this system font scale the layouts switch to roomier arrangements. */
private const val LARGE_TEXT_SCALE = 1.3f

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(
        modifier = modifier.fillMaxHeight().padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // "100%" does not fit the tile at the big size and would break into two lines: longer values use a smaller size.
        val valueStyle = if (value.length <= 3) type.display else type.title
        BasicText(value, style = valueStyle.copy(color = colors.textPrimary), maxLines = 1, softWrap = false)
        // The label wraps (up to two lines) so big text is never cut off.
        BasicText(label, style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center), maxLines = 2, modifier = Modifier.padding(horizontal = Spacing.xs))
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
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Memory and tubes results have no word (their board is a list of numbers), so the game's name is shown instead.
        BasicText(
            text = when (round.game) {
                GameKind.Word -> round.answer
                GameKind.Memory -> stringResource(Res.string.favorite_memory_tag)
                GameKind.Tubes -> stringResource(Res.string.favorite_tubes_tag)
            },
            style = type.heading.copy(color = colors.textPrimary),
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            BasicText(
                text = when {
                    round.outcome != Outcome.WON -> stringResource(Res.string.favorite_lost)
                    round.game == GameKind.Memory -> stringResource(Res.string.favorite_memory_result, round.moveCount, formatDuration(round.durationMs ?: 0L))
                    round.game == GameKind.Tubes -> stringResource(Res.string.favorite_tubes_result, round.moveCount, formatDuration(round.durationMs ?: 0L))
                    else -> stringResource(Res.string.favorite_won, round.guesses.size)
                },
                style = type.label.copy(color = if (round.outcome == Outcome.WON) colors.correctFill else colors.textSecondary),
            )
            val date = IstanbulDay.format(round.dayIndex)
            val detail = if (round.mode == Mode.DAILY) {
                listOfNotNull(stringResource(Res.string.favorite_daily_tag), round.durationMs?.let(::formatDuration), date).joinToString(" · ")
            } else {
                date
            }
            BasicText(detail, style = type.caption.copy(color = colors.textSecondary))
        }
    }
}

@Composable
private fun DailyFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    Chip(
        text = text,
        textColor = if (selected) colors.textPrimary else colors.textSecondary,
        raised = selected,
        selected = selected,
        onClick = onClick,
    )
}
