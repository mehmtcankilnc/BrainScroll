package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.rotate
import com.mehmtcan.brainscroll.ui.motion.Motion
import androidx.compose.foundation.horizontalScroll
import com.mehmtcan.brainscroll.ui.components.AppTextButton
import com.mehmtcan.brainscroll.ui.home.displayName
import brainscroll.shared.generated.resources.favorites_all
import brainscroll.shared.generated.resources.favorites_record
import brainscroll.shared.generated.resources.favorites_replay
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import brainscroll.shared.generated.resources.a11y_answer_streak
import brainscroll.shared.generated.resources.a11y_flow_record
import brainscroll.shared.generated.resources.a11y_day_streak
import brainscroll.shared.generated.resources.account_apple
import brainscroll.shared.generated.resources.account_google
import brainscroll.shared.generated.resources.account_guest
import brainscroll.shared.generated.resources.account_guest_hint
import brainscroll.shared.generated.resources.account_offline
import brainscroll.shared.generated.resources.account_offline_short
import brainscroll.shared.generated.resources.backup_done
import brainscroll.shared.generated.resources.backup_pending
import brainscroll.shared.generated.resources.home_streaks_answer
import brainscroll.shared.generated.resources.home_streaks_day
import brainscroll.shared.generated.resources.profile_favorites_hint
import brainscroll.shared.generated.resources.profile_ranks_hint
import brainscroll.shared.generated.resources.profile_settings_hint
import brainscroll.shared.generated.resources.profile_stats_hint
import brainscroll.shared.generated.resources.stat_won
import brainscroll.shared.generated.resources.streak_best
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.ui.components.ForwardChevron
import com.mehmtcan.brainscroll.ui.daily.FlameIcon
import com.mehmtcan.brainscroll.ui.feed.BoltIcon
import com.mehmtcan.brainscroll.ui.streak.StreakInfoSheet
import com.mehmtcan.brainscroll.ui.streak.StreakKind
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
import brainscroll.shared.generated.resources.stats_mines
import brainscroll.shared.generated.resources.stats_sudoku
import brainscroll.shared.generated.resources.stats_lights
import brainscroll.shared.generated.resources.stats_slide
import brainscroll.shared.generated.resources.stats_target
import brainscroll.shared.generated.resources.stat_three_stars
import brainscroll.shared.generated.resources.stats_line
import brainscroll.shared.generated.resources.stats_pipes
import brainscroll.shared.generated.resources.favorite_line_result
import brainscroll.shared.generated.resources.favorite_line_tag
import brainscroll.shared.generated.resources.favorite_pipes_result
import brainscroll.shared.generated.resources.favorite_pipes_tag
import brainscroll.shared.generated.resources.favorite_target_result
import brainscroll.shared.generated.resources.favorite_target_tag
import brainscroll.shared.generated.resources.favorite_slide_result
import brainscroll.shared.generated.resources.favorite_slide_tag
import brainscroll.shared.generated.resources.favorite_lights_result
import brainscroll.shared.generated.resources.favorite_lights_tag
import brainscroll.shared.generated.resources.favorite_sudoku_result
import brainscroll.shared.generated.resources.favorite_sudoku_tag
import brainscroll.shared.generated.resources.favorite_mines_result
import brainscroll.shared.generated.resources.favorite_mines_tag
import brainscroll.shared.generated.resources.favorite_tubes_result
import brainscroll.shared.generated.resources.favorite_tubes_tag
import brainscroll.shared.generated.resources.stats_filter_all
import brainscroll.shared.generated.resources.stats_streak_note
import brainscroll.shared.generated.resources.stats_endless
import brainscroll.shared.generated.resources.stats_title
import brainscroll.shared.generated.resources.tab_ranks
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.line.lineStars
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
 * The profile tab, top to bottom: who you are and whether your progress is backed up, the two streaks (tap one to see
 * what it means), three numbers that sum you up, and a short list that leads to the pages: statistics, favorites,
 * ranks and settings. Each line of the list says in a few words what is behind it.
 */
@Composable
fun ProfileScreen(
    profile: ProfileState,
    account: AccountState,
    onOpen: (ProfileTarget) -> Unit,
    modifier: Modifier = Modifier,
    /** The name on the tables, once the player has one. */
    username: String? = null,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    // The endless numbers already include memory match, so only the daily puzzle is added.
    val played = profile.stats.played + profile.daily.played
    val won = profile.stats.won + profile.daily.won
    val winRate = if (played == 0) 0f else won.toFloat() / played
    var streakInfo by remember { mutableStateOf<StreakKind?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.md))
            AccountCard(account, username) { onOpen(ProfileTarget.Settings) }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                StreakTile(
                    kind = StreakKind.Daily,
                    title = stringResource(Res.string.home_streaks_day),
                    current = profile.dayStreak.current,
                    best = profile.dayStreak.best,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { streakInfo = StreakKind.Daily },
                )
                // The most puzzles solved in one feed: the number itself starts from zero with every feed, so only the record is here.
                StreakTile(
                    kind = StreakKind.Win,
                    title = stringResource(Res.string.home_streaks_answer),
                    current = profile.bestFlow,
                    best = null,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { streakInfo = StreakKind.Win },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(vertical = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SummaryNumber(played.toString(), stringResource(Res.string.stat_played))
                SummaryNumber("${(winRate * 100).roundToInt()}%", stringResource(Res.string.stat_win_rate))
                SummaryNumber(won.toString(), stringResource(Res.string.stat_won))
            }

            Column {
                ProfileRow(stringResource(Res.string.stats_title), stringResource(Res.string.profile_stats_hint)) { onOpen(ProfileTarget.Stats) }
                ProfileRow(
                    stringResource(Res.string.favorites_title),
                    stringResource(Res.string.profile_favorites_hint),
                    value = profile.favorites.size.takeIf { it > 0 }?.toString(),
                ) { onOpen(ProfileTarget.Favorites) }
                ProfileRow(stringResource(Res.string.tab_ranks), stringResource(Res.string.profile_ranks_hint)) { onOpen(ProfileTarget.Ranks) }
                ProfileRow(stringResource(Res.string.profile_settings), stringResource(Res.string.profile_settings_hint), last = true) { onOpen(ProfileTarget.Settings) }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
        when (streakInfo) {
            StreakKind.Daily -> StreakInfoSheet(
                StreakKind.Daily,
                current = profile.dayStreak.current,
                best = profile.dayStreak.best,
                freezes = profile.dayStreak.freezesAvailable,
                onClose = { streakInfo = null },
            )
            StreakKind.Win -> StreakInfoSheet(StreakKind.Win, current = null, best = profile.bestFlow, onClose = { streakInfo = null })
            null -> Unit
        }
    }
}

/**
 * Who is playing: a round mark with a letter, the name (or what kind of player this is) and how the account was made.
 * With a Google or Apple account it only tells you who you are, since the settings have their own line below. A guest
 * or an offline player gets a row that leads to the sign-in.
 */
@Composable
private fun AccountCard(account: AccountState, username: String?, onSignIn: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val signedIn = account as? AccountState.SignedIn
    val named = signedIn != null && signedIn.kind != AccountState.SignedIn.Kind.Anonymous
    val provider = when {
        signedIn == null -> stringResource(Res.string.account_offline_short)
        signedIn.kind == AccountState.SignedIn.Kind.Google -> stringResource(Res.string.account_google)
        signedIn.kind == AccountState.SignedIn.Kind.Apple -> stringResource(Res.string.account_apple)
        else -> stringResource(Res.string.account_guest)
    }
    val title = if (named) username ?: provider else provider
    val detail = when {
        signedIn == null -> stringResource(Res.string.account_offline)
        !named -> stringResource(Res.string.account_guest_hint)
        username != null -> provider
        else -> null
    }
    val initial = ((if (named) username else null) ?: signedIn?.email)?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Row(
        modifier = Modifier
            .testTag("accountCard")
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .then(
                if (named) Modifier
                else Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onSignIn),
            )
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            Modifier.size(44.dp).background(if (named) colors.correctFill else colors.bgRaised, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(initial, style = type.heading.copy(color = if (named) colors.correctOn else colors.textSecondary))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(title, style = type.heading.copy(color = colors.textPrimary), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (detail != null) BasicText(detail, style = type.caption.copy(color = colors.textSecondary))
        }
        if (!named) ForwardChevron(colors.textSecondary)
    }
}

/** One streak: its icon and name, the number now and the best so far. The whole tile opens the explanation. */
@Composable
private fun StreakTile(kind: StreakKind, title: String, current: Int, best: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val accent = if (kind == StreakKind.Daily) colors.streakDay else colors.streakAnswer
    val description = stringResource(if (kind == StreakKind.Daily) Res.string.a11y_day_streak else Res.string.a11y_flow_record, current)
    Column(
        modifier = modifier
            .testTag(if (kind == StreakKind.Daily) "dayStreakTile" else "winStreakTile")
            .background(colors.bgSurface, Radius.card)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (kind == StreakKind.Daily) FlameIcon(accent) else BoltIcon(accent)
            BasicText(title, style = type.caption.copy(color = colors.textSecondary), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        BasicText(current.toString(), style = type.display.copy(color = accent))
        if (best != null) BasicText(stringResource(Res.string.streak_best, best), style = type.caption.copy(color = colors.textSecondary))
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

/** A line of the list: a name with a few words under it, an optional small value and a chevron. A hairline below. */
@Composable
private fun ProfileRow(title: String, hint: String? = null, value: String? = null, last: Boolean = false, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(title, style = type.heading.copy(color = colors.textPrimary))
                if (hint != null) BasicText(hint, style = type.caption.copy(color = colors.textSecondary))
            }
            if (value != null) BasicText(value, style = type.label.copy(color = colors.textSecondary))
            ForwardChevron(colors.textSecondary)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
    }
}

/**
 * The numbers. At the top the three that sum you up; under them one line per game with how often it was played. A line
 * opens (slides open) to that game's own numbers, so the page is short until you ask for more. Everything is derived from
 * the saved results.
 */
@Composable
fun StatsPage(profile: ProfileState, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    // Null shows both languages together.
    var dailyLanguage by remember { mutableStateOf<Language?>(null) }
    val daily = dailyLanguage?.let { profile.dailyByLanguage[it] } ?: profile.daily
    // The endless numbers already include memory match, so only the daily puzzle is added.
    val played = profile.stats.played + profile.daily.played
    val won = profile.stats.won + profile.daily.won
    val winRate = if (played == 0) 0f else won.toFloat() / played

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(vertical = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SummaryNumber(played.toString(), stringResource(Res.string.stat_played))
                SummaryNumber("${(winRate * 100).roundToInt()}%", stringResource(Res.string.stat_win_rate))
                SummaryNumber(won.toString(), stringResource(Res.string.stat_won))
            }
        }
        item { Spacer(Modifier.height(Spacing.xs)) }

        // The daily puzzle: its streak counts days, not answers, and it has a time. The numbers can be shown for
        // both languages together or for one (there are two puzzles a day, one per language).
        item {
            StatGroup(stringResource(Res.string.stats_daily), profile.daily.played, "statsDaily") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    DailyFilterChip(stringResource(Res.string.stats_filter_all), dailyLanguage == null) { dailyLanguage = null }
                    Language.entries.forEach { language ->
                        DailyFilterChip(language.name, dailyLanguage == language) { dailyLanguage = language }
                    }
                }
                StatTiles(
                    played = daily.played,
                    winRate = daily.winRate,
                    streak = profile.dayStreak.current,
                    best = profile.dayStreak.best,
                    streakLabel = stringResource(Res.string.stat_day_streak),
                )
                if (dailyLanguage != null) {
                    BasicText(stringResource(Res.string.stats_streak_note), style = type.caption.copy(color = colors.textSecondary))
                }
                daily.bestTimeMs?.let { best ->
                    BasicText(
                        text = stringResource(Res.string.stat_best_time) + ": " + formatDuration(best),
                        style = type.body.copy(color = colors.textPrimary),
                    )
                }
                if (profile.dayStreak.freezesAvailable > 0) {
                    BasicText(
                        text = stringResource(Res.string.daily_freeze, profile.dayStreak.freezesAvailable),
                        style = type.caption.copy(color = colors.textSecondary),
                    )
                }
                GuessDistribution(daily.guessDistribution)
            }
        }

        // The endless feed: word puzzles, with how many guesses they took.
        item {
            StatGroup(stringResource(Res.string.stats_endless), profile.stats.played, "statsWord") {
                TileGrid(
                    listOf(
                        profile.stats.played.toString() to stringResource(Res.string.stat_played),
                        "${(profile.stats.winRate * 100).roundToInt()}%" to stringResource(Res.string.stat_win_rate),
                    ),
                )
                GuessDistribution(profile.stats.guessDistribution)
            }
        }
        // Memory match has no guesses: results, the fastest win and the fewest moves.
        item {
            StatGroup(stringResource(Res.string.stats_memory), profile.memory.played, "statsMemory") {
                TileGrid(
                    listOf(
                        profile.memory.played.toString() to stringResource(Res.string.stat_played),
                        "${(profile.memory.winRate * 100).roundToInt()}%" to stringResource(Res.string.stat_win_rate),
                        (profile.memory.bestTimeMs?.let(::formatDuration) ?: "-") to stringResource(Res.string.stat_best_time),
                        (profile.memory.fewestMoves?.toString() ?: "-") to stringResource(Res.string.stat_fewest_moves),
                    ),
                )
            }
        }
        // Every game of these is a win, so there is no win rate, only the fastest game and the fewest moves.
        item { MovesGroup(stringResource(Res.string.stats_tubes), profile.tubes.played, profile.tubes.bestTimeMs, profile.tubes.fewestMoves, "statsTubes") }
        item { MovesGroup(stringResource(Res.string.stats_lights), profile.lights.played, profile.lights.bestTimeMs, profile.lights.fewestMoves, "statsLights") }
        item { MovesGroup(stringResource(Res.string.stats_slide), profile.slide.played, profile.slide.bestTimeMs, profile.slide.fewestMoves, "statsSlide") }
        item { MovesGroup(stringResource(Res.string.stats_target), profile.target.played, profile.target.bestTimeMs, profile.target.fewestMoves, "statsTarget") }
        // One Line is only the daily puzzle: played, the fastest board and the boards that earned three stars.
        item {
            StatGroup(stringResource(Res.string.stats_line), profile.line.played, "statsLine") {
                TileGrid(
                    listOf(
                        profile.line.played.toString() to stringResource(Res.string.stat_played),
                        (profile.line.bestTimeMs?.let(::formatDuration) ?: "-") to stringResource(Res.string.stat_best_time),
                        profile.line.threeStars.toString() to stringResource(Res.string.stat_three_stars),
                    ),
                )
            }
        }
        item { MovesGroup(stringResource(Res.string.stats_pipes), profile.pipes.played, profile.pipes.bestTimeMs, profile.pipes.fewestMoves, "statsPipes") }
        // Minesweeper: played, win rate and the fastest win.
        item { WinRateGroup(stringResource(Res.string.stats_mines), profile.mines.played, profile.mines.winRate, profile.mines.bestTimeMs, "statsMines") }
        // A sudoku cannot be lost any more, so it has no win rate: played and the fastest win.
        item {
            StatGroup(stringResource(Res.string.stats_sudoku), profile.sudoku.played, "statsSudoku") {
                TileGrid(
                    listOf(
                        profile.sudoku.played.toString() to stringResource(Res.string.stat_played),
                        (profile.sudoku.bestTimeMs?.let(::formatDuration) ?: "-") to stringResource(Res.string.stat_best_time),
                    ),
                )
            }
        }
        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

@Composable
private fun MovesGroup(title: String, played: Int, bestTimeMs: Long?, fewestMoves: Int?, tag: String) {
    StatGroup(title, played, tag) {
        TileGrid(
            listOf(
                played.toString() to stringResource(Res.string.stat_played),
                (bestTimeMs?.let(::formatDuration) ?: "-") to stringResource(Res.string.stat_best_time),
                (fewestMoves?.toString() ?: "-") to stringResource(Res.string.stat_fewest_moves),
            ),
        )
    }
}

@Composable
private fun WinRateGroup(title: String, played: Int, winRate: Float, bestTimeMs: Long?, tag: String) {
    StatGroup(title, played, tag) {
        TileGrid(
            listOf(
                played.toString() to stringResource(Res.string.stat_played),
                "${(winRate * 100).roundToInt()}%" to stringResource(Res.string.stat_win_rate),
                (bestTimeMs?.let(::formatDuration) ?: "-") to stringResource(Res.string.stat_best_time),
            ),
        )
    }
}

/**
 * One game on the statistics page: its name and how often it was played, always in view; tapping the line slides open
 * the numbers under it ([content]) and turns the arrow.
 */
@Composable
private fun StatGroup(title: String, played: Int, tag: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var open by remember { mutableStateOf(false) }
    val arrow by animateFloatAsState(if (open) -90f else 90f, tween(Motion.BASE, easing = Motion.Standard))
    Column(Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).testTag(tag)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) { open = !open }
                .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BasicText(title, style = type.heading.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
            BasicText(played.toString(), style = type.label.copy(color = colors.textSecondary))
            ForwardChevron(colors.textSecondary, Modifier.rotate(arrow))
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(Motion.BASE, easing = Motion.Standard)) + fadeIn(tween(Motion.BASE)),
            exit = shrinkVertically(tween(Motion.BASE, easing = Motion.Standard)) + fadeOut(tween(Motion.FAST)),
        ) {
            Column(Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                content()
            }
        }
    }
}

/**
 * The finished puzzles the player marked with the heart. A row of chips filters them by game, a puzzle that holds the
 * best time of its game says so, and "Play again" starts a new feed with that very puzzle as its first page.
 */
@Composable
fun FavoritesPage(profile: ProfileState, onReplay: (FinishedRound) -> Unit = {}, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var filter by remember { mutableStateOf<GameKind?>(null) }
    val present = GameKind.entries.filter { game -> profile.favorites.any { it.game == game } }
    // A filter whose games were all un-hearted is gone: show everything again.
    val active = filter?.takeIf { it in present }
    val shown = if (active == null) profile.favorites else profile.favorites.filter { it.game == active }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (profile.favorites.isEmpty()) {
            item {
                BasicText(
                    stringResource(Res.string.favorites_empty),
                    style = type.body.copy(color = colors.textSecondary),
                )
            }
        } else {
            if (present.size > 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        FilterChip(stringResource(Res.string.favorites_all), active == null) { filter = null }
                        present.forEach { game -> FilterChip(game.displayName(), active == game) { filter = game } }
                    }
                }
            }
            items(shown, key = { it.id }) { round ->
                FavoriteRow(round, record = holdsRecord(profile, round), onReplay = { onReplay(round) })
            }
        }
        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

/** True when this won, unassisted puzzle is the fastest of its game (the daily word puzzle has its own best time). */
private fun holdsRecord(profile: ProfileState, round: FinishedRound): Boolean {
    val time = round.durationMs ?: return false
    if (round.outcome != Outcome.WON || round.assisted) return false
    val best = if (round.mode == Mode.DAILY && round.game == GameKind.Word) profile.daily.bestTimeMs else when (round.game) {
        GameKind.Word -> null
        GameKind.Memory -> profile.memory.bestTimeMs
        GameKind.Tubes -> profile.tubes.bestTimeMs
        GameKind.Mines -> profile.mines.bestTimeMs
        GameKind.Sudoku -> profile.sudoku.bestTimeMs
        GameKind.Lights -> profile.lights.bestTimeMs
        GameKind.Slide -> profile.slide.bestTimeMs
        GameKind.Target -> profile.target.bestTimeMs
        GameKind.Pipes -> profile.pipes.bestTimeMs
        GameKind.Line -> profile.line.bestTimeMs
    }
    return best != null && time <= best
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    Chip(
        text = text,
        textColor = if (selected) colors.textPrimary else colors.textSecondary,
        raised = selected,
        selected = selected,
        onClick = onClick,
    )
}

/** The account and the privacy choice. */
@Composable
fun SettingsPage(account: AccountUi, privacy: PrivacyUi, language: LanguageUi, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { LanguageSection(language) }
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
private fun FavoriteRow(round: FinishedRound, record: Boolean, onReplay: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(Modifier.fillMaxWidth()) {
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
                GameKind.Mines -> stringResource(Res.string.favorite_mines_tag)
                GameKind.Sudoku -> stringResource(Res.string.favorite_sudoku_tag)
                GameKind.Lights -> stringResource(Res.string.favorite_lights_tag)
                GameKind.Slide -> stringResource(Res.string.favorite_slide_tag)
                GameKind.Target -> stringResource(Res.string.favorite_target_tag)
                GameKind.Pipes -> stringResource(Res.string.favorite_pipes_tag)
                GameKind.Line -> stringResource(Res.string.favorite_line_tag)
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
                    round.game == GameKind.Mines -> stringResource(Res.string.favorite_mines_result, formatDuration(round.durationMs ?: 0L))
                    round.game == GameKind.Sudoku -> stringResource(Res.string.favorite_sudoku_result, formatDuration(round.durationMs ?: 0L))
                    round.game == GameKind.Lights -> stringResource(Res.string.favorite_lights_result, round.moveCount, formatDuration(round.durationMs ?: 0L))
                    round.game == GameKind.Slide -> stringResource(Res.string.favorite_slide_result, round.moveCount, formatDuration(round.durationMs ?: 0L))
                    round.game == GameKind.Target -> stringResource(Res.string.favorite_target_result, round.moveCount, formatDuration(round.durationMs ?: 0L))
                    round.game == GameKind.Pipes -> stringResource(Res.string.favorite_pipes_result, round.moveCount, formatDuration(round.durationMs ?: 0L))
                    round.game == GameKind.Line -> stringResource(Res.string.favorite_line_result, round.lineStars, formatDuration(round.durationMs ?: 0L))
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (record) {
                BasicText(stringResource(Res.string.favorites_record), style = type.label.copy(color = colors.pendingFill), modifier = Modifier.testTag("favoriteRecord"))
            }
            Spacer(Modifier.weight(1f))
            AppTextButton(stringResource(Res.string.favorites_replay), onReplay, Modifier.testTag("favoriteReplay"))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
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
