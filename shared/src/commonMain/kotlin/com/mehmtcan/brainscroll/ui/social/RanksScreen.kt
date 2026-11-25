@file:OptIn(ExperimentalLayoutApi::class)

package com.mehmtcan.brainscroll.ui.social

import brainscroll.shared.generated.resources.friends_add_title
import brainscroll.shared.generated.resources.friends_copied_short
import brainscroll.shared.generated.resources.friends_copy_code
import brainscroll.shared.generated.resources.friends_empty_title
import brainscroll.shared.generated.resources.friends_my_code_caption
import com.mehmtcan.brainscroll.ui.components.LineIcon
import com.mehmtcan.brainscroll.ui.components.PillSegmented
import com.mehmtcan.brainscroll.ui.components.PuzzlePair
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import brainscroll.shared.generated.resources.ranks_next_puzzle
import brainscroll.shared.generated.resources.ranks_your_place
import brainscroll.shared.generated.resources.daily_streak_label
import brainscroll.shared.generated.resources.ranks_your_streak
import com.mehmtcan.brainscroll.daily.formatCountdown
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.components.FilterChip
import com.mehmtcan.brainscroll.ui.motion.Motion
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.graphicsLayer
import com.mehmtcan.brainscroll.ui.home.GameThumbnail
import androidx.compose.foundation.horizontalScroll
import brainscroll.shared.generated.resources.ranks_today_game
import com.mehmtcan.brainscroll.ui.theme.world
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import com.mehmtcan.brainscroll.ui.components.PlayerAvatar
import com.mehmtcan.brainscroll.ui.components.RankRow
import com.mehmtcan.brainscroll.ui.daily.FlameIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextDecoration
import brainscroll.shared.generated.resources.report_player
import brainscroll.shared.generated.resources.howto_done
import brainscroll.shared.generated.resources.ranks_filters
import com.mehmtcan.brainscroll.ui.components.ForwardChevron
import com.mehmtcan.brainscroll.ui.components.ModalSheet
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.daily_retry
import brainscroll.shared.generated.resources.filter_everyone
import brainscroll.shared.generated.resources.filter_kind
import brainscroll.shared.generated.resources.filter_language
import brainscroll.shared.generated.resources.filter_period
import brainscroll.shared.generated.resources.filter_who
import brainscroll.shared.generated.resources.filter_friends
import brainscroll.shared.generated.resources.friends_accept
import brainscroll.shared.generated.resources.friends_add
import brainscroll.shared.generated.resources.friends_add_code
import brainscroll.shared.generated.resources.friends_add_name
import brainscroll.shared.generated.resources.friends_ask
import brainscroll.shared.generated.resources.friends_cancel
import brainscroll.shared.generated.resources.friends_code_hint
import brainscroll.shared.generated.resources.friends_copied
import brainscroll.shared.generated.resources.friends_copy
import brainscroll.shared.generated.resources.friends_decline
import brainscroll.shared.generated.resources.friends_invite_message
import brainscroll.shared.generated.resources.friends_list
import brainscroll.shared.generated.resources.friends_my_code
import brainscroll.shared.generated.resources.friends_name_hint
import brainscroll.shared.generated.resources.friends_none
import brainscroll.shared.generated.resources.friends_remove
import brainscroll.shared.generated.resources.friends_requests
import brainscroll.shared.generated.resources.friends_sent
import brainscroll.shared.generated.resources.gate_offline
import brainscroll.shared.generated.resources.gate_sign_in
import brainscroll.shared.generated.resources.gate_username_hint
import brainscroll.shared.generated.resources.gate_username_save
import brainscroll.shared.generated.resources.gate_username_text
import brainscroll.shared.generated.resources.gate_username_title
import brainscroll.shared.generated.resources.kind_current
import brainscroll.shared.generated.resources.kind_longest
import brainscroll.shared.generated.resources.ranks_empty_body
import brainscroll.shared.generated.resources.ranks_empty_friends_body
import brainscroll.shared.generated.resources.ranks_empty_friends_title
import brainscroll.shared.generated.resources.ranks_empty_title
import brainscroll.shared.generated.resources.ranks_failed
import brainscroll.shared.generated.resources.ranks_friends
import brainscroll.shared.generated.resources.ranks_guesses
import brainscroll.shared.generated.resources.ranks_loading
import brainscroll.shared.generated.resources.ranks_speed
import brainscroll.shared.generated.resources.ranks_speed_caption
import brainscroll.shared.generated.resources.ranks_stale
import brainscroll.shared.generated.resources.ranks_streak_caption
import brainscroll.shared.generated.resources.ranks_streaks
import brainscroll.shared.generated.resources.ranks_moves
import brainscroll.shared.generated.resources.ranks_stars
import brainscroll.shared.generated.resources.filter_game
import brainscroll.shared.generated.resources.ranks_you
import brainscroll.shared.generated.resources.scope_all_time
import brainscroll.shared.generated.resources.scope_today
import brainscroll.shared.generated.resources.sign_in_apple
import brainscroll.shared.generated.resources.sign_in_google
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.social.Board
import com.mehmtcan.brainscroll.social.DailyRow
import com.mehmtcan.brainscroll.social.DailyScope
import com.mehmtcan.brainscroll.social.RankKind
import com.mehmtcan.brainscroll.social.rankKind
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.LEVELLED_GAMES
import brainscroll.shared.generated.resources.difficulty_easy
import brainscroll.shared.generated.resources.difficulty_medium
import brainscroll.shared.generated.resources.difficulty_hard
import brainscroll.shared.generated.resources.ranks_caption_time
import brainscroll.shared.generated.resources.ranks_caption_rounds
import brainscroll.shared.generated.resources.ranks_caption_guesses
import brainscroll.shared.generated.resources.ranks_caption_stars
import brainscroll.shared.generated.resources.ranks_rounds
import com.mehmtcan.brainscroll.social.StreakKind
import com.mehmtcan.brainscroll.social.StreakRow
import com.mehmtcan.brainscroll.social.inviteLink
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.AppleSignInButton
import com.mehmtcan.brainscroll.ui.components.GoogleSignInButton
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.SegmentedControl
import com.mehmtcan.brainscroll.ui.components.TextAction
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.line.LineScoring
import com.mehmtcan.brainscroll.ui.home.displayName
import com.mehmtcan.brainscroll.ui.home.nativeName
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.textSoft
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** What the ranks tab needs from the account, as plain callbacks. */
data class RanksSignIn(
    val canSignInWithApple: Boolean,
    val onSignInWithGoogle: () -> Unit,
    val onSignInWithApple: () -> Unit,
)

/** The ranks tab: the speed and streak tables. The friends have a page of their own, in the profile. */
@Composable
fun RanksScreen(viewModel: SocialViewModel, signIn: RanksSignIn, modifier: Modifier = Modifier) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.onShown()
        // The friends page leaves its section open; the tables start from the speed table again.
        if (ui.section == Section.Friends) viewModel.selectSection(Section.Speed)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item { SectionTabs(ui, viewModel) }
        // The choices for the open table are always in view, each with its name above it.
        if (ui.section != Section.Friends) item { FilterPanel(ui, viewModel) }
        gateItems(ui, viewModel, signIn)

        when (ui.section) {
            Section.Speed -> speedSection(ui, viewModel)
            Section.Streak -> streakSection(ui, viewModel)
            Section.Friends -> Unit
        }
        if (ui.section != Section.Friends) item { ReportPlayerLink() }
    }
}

/** The friends page of the profile: my invite code, adding a friend, the requests and the list. */
@Composable
fun FriendsScreen(viewModel: SocialViewModel, signIn: RanksSignIn, modifier: Modifier = Modifier) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.onShown()
        viewModel.selectSection(Section.Friends)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        // More air between the invite card, the add-a-friend part and the list than the tables have.
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        gateItems(ui, viewModel, signIn)
        friendsSection(ui, viewModel)
        if (ui.access == Access.Ready) item { ReportPlayerLink() }
    }
}

/**
 * Other players see a username, so there is a plain way to report one that is not fine (App Store guideline 1.2): it opens a mail
 * to the support address, which the player can finish with the name. The server also refuses bad words when a name is chosen.
 */
@Composable
private fun ReportPlayerLink() {
    val uri = LocalUriHandler.current
    val colors = BrainScrollTheme.colors
    val turkish = Locale.current.language == "tr"
    BasicText(
        text = stringResource(Res.string.report_player),
        style = BrainScrollTheme.typography.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center, textDecoration = TextDecoration.Underline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                uri.openUri("mailto:support@playbrainscroll.com?subject=" + if (turkish) "Oyuncu%20bildirimi" else "Player%20report")
            }
            .padding(vertical = Spacing.md)
            .testTag("reportPlayer"),
    )
}

/** Who may do what: anonymous players only look, friends need a username. */
private fun LazyListScope.gateItems(ui: SocialUiState, viewModel: SocialViewModel, signIn: RanksSignIn) {
    when (ui.access) {
        Access.Offline -> item { Card { Body(stringResource(Res.string.gate_offline)) } }
        Access.Anonymous -> item { SignInCard(signIn) }
        Access.NeedsUsername -> item { UsernameCard(viewModel) }
        Access.Checking, Access.Ready -> Unit
    }
}

// --- Choices ---

/** Which table: one control across the width. */
@Composable
private fun SectionTabs(ui: SocialUiState, viewModel: SocialViewModel) {
    val sections = listOf(Section.Speed, Section.Streak)
    SegmentedControl(
        options = listOf(
            stringResource(Res.string.ranks_speed),
            stringResource(Res.string.ranks_streaks),
        ),
        selectedIndex = sections.indexOf(ui.section).coerceAtLeast(0),
        onSelect = { viewModel.selectSection(sections[it]) },
    )
}

/**
 * The choices of the open table, always on the page, in three layers: the games as a strip of round badges (the chosen one big
 * and in its color, the others small and dim) with its name and what is shown under it; one line of chips for the rest
 * (period, language, whose results); and for today's table a box with the time to the next puzzle and my place.
 * Today's table is of today's game only, so tapping another badge there moves to the table of all time with that game.
 */
@Composable
private fun FilterPanel(ui: SocialUiState, viewModel: SocialViewModel) {
    val description = stringResource(Res.string.ranks_filters)
    Column(
        modifier = Modifier.fillMaxWidth().testTag("ranksFilters").semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (ui.section == Section.Speed) {
            GameBadges(ui, viewModel)
            GameHeader(ui)
        }
        if (ui.section == Section.Streak) StreakHero()
        ChipRow(ui, viewModel)
        if (ui.section == Section.Speed && ui.scope == DailyScope.Today) PlaceBox(ui)
        if (ui.section == Section.Streak) StreakBox(ui)
        Caption(
            stringResource(
                if (ui.section != Section.Speed) Res.string.ranks_streak_caption
                else when (ui.game.rankKind()) {
                    RankKind.Time -> Res.string.ranks_caption_time
                    RankKind.Rounds -> Res.string.ranks_caption_rounds
                    RankKind.Guesses -> Res.string.ranks_caption_guesses
                    RankKind.Stars -> Res.string.ranks_caption_stars
                },
            ),
        )
    }
}

/** The games in a row of badges: the picture of the game on a tile with its name under it, the chosen one edged in its color. */
@Composable
private fun GameBadges(ui: SocialUiState, viewModel: SocialViewModel) {
    val shown = if (ui.scope == DailyScope.Today) ui.todayGame else ui.game
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        // Today's game first: it is where the page starts.
        (listOf(ui.todayGame) + GameKind.entries.filter { it != ui.todayGame }).forEach { game ->
            GameBadge(game, ui.language, selected = shown == game) {
                if (!(ui.scope == DailyScope.Today && game == ui.todayGame)) {
                    viewModel.selectGame(game)
                    if (ui.scope == DailyScope.Today) viewModel.selectScope(DailyScope.AllTime)
                }
            }
        }
    }
}

@Composable
private fun GameBadge(game: GameKind, wordLanguage: Language, selected: Boolean, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val world = game.world
    val name = game.displayName()
    val ring by animateColorAsState(if (selected) world else colors.borderSubtle, tween(Motion.BASE))
    val shape = Radius.card
    Column(
        modifier = Modifier
            .testTag("rankGameBadge")
            // As wide as the picture, or as the name when that is longer: a name is never cut.
            .widthIn(min = BADGE_SIZE)
            .semantics(mergeDescendants = true) {
                contentDescription = name
                this.selected = selected
                role = Role.RadioButton
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(BADGE_SIZE)
                .clip(shape)
                .background(colors.bgSurface)
                .then(if (selected) Modifier.background(world.copy(alpha = 0.25f)) else Modifier)
                .border(if (selected) 3.dp else 1.5.dp, ring, shape),
            contentAlignment = Alignment.Center,
        ) {
            // The picture a little smaller than the tile: it sits clear of the rounded corners and the ring, nothing is cut.
            GameThumbnail(game, wordLanguage, Modifier.requiredSize(BADGE_SIZE * 0.78f).graphicsLayer { alpha = if (selected) 1f else 0.8f })
        }
        // The name is always written: the chosen one in the main color and bold, with a bar of the game's color under it.
        BasicText(
            text = name,
            maxLines = 1,
            softWrap = false,
            style = type.caption.copy(
                color = if (selected) colors.textPrimary else colors.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
        )
        Box(Modifier.width(24.dp).height(3.dp).background(if (selected) world else Color.Transparent, Radius.pill))
    }
}

/** One line: the game on show and what is shown of it ("Word puzzle · Today · Türkçe"). */
@Composable
private fun GameHeader(ui: SocialUiState) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val game = if (ui.scope == DailyScope.Today) ui.todayGame else ui.game
    val summary = listOfNotNull(
        stringResource(if (ui.scope == DailyScope.Today) Res.string.scope_today else Res.string.scope_all_time),
        if (game == GameKind.Word) ui.language.nativeName() else null,
    ).joinToString(" · ")
    BasicText(
        text = "${game.displayName()} · $summary",
        modifier = Modifier.fillMaxWidth(),
        style = type.label.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
    )
}

/** One line of chips that scrolls sideways; the groups are told apart by a thin bar. */
@Composable
private fun ChipRow(ui: SocialUiState, viewModel: SocialViewModel) {
    val colors = BrainScrollTheme.colors
    @Composable
    fun Divider() = Box(Modifier.width(1.dp).height(24.dp).background(colors.borderSubtle))
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ui.section == Section.Speed) {
            FilterChip(stringResource(Res.string.scope_today), ui.scope == DailyScope.Today, { viewModel.selectScope(DailyScope.Today) })
            FilterChip(stringResource(Res.string.scope_all_time), ui.scope == DailyScope.AllTime, { viewModel.selectScope(DailyScope.AllTime) })
            if (ui.game == GameKind.Word) {
                Divider()
                Language.entries.forEach { language ->
                    FilterChip(language.nativeName(), ui.language == language, { viewModel.selectLanguage(language) })
                }
            }
            // Every game with levels has one table per level (the daily one-line puzzle has none).
            if (ui.game in LEVELLED_GAMES && ui.game != GameKind.Line) {
                Divider()
                Difficulty.entries.forEach { level ->
                    FilterChip(
                        stringResource(
                            when (level) {
                                Difficulty.Easy -> Res.string.difficulty_easy
                                Difficulty.Medium -> Res.string.difficulty_medium
                                Difficulty.Hard -> Res.string.difficulty_hard
                            },
                        ),
                        ui.difficulty == level,
                        { viewModel.selectDifficulty(level) },
                    )
                }
            }
        } else {
            FilterChip(stringResource(Res.string.kind_current), ui.kind == StreakKind.Current, { viewModel.selectKind(StreakKind.Current) })
            FilterChip(stringResource(Res.string.kind_longest), ui.kind == StreakKind.Longest, { viewModel.selectKind(StreakKind.Longest) })
        }
        if (ui.access == Access.Ready) {
            Divider()
            FilterChip(stringResource(Res.string.filter_everyone), !ui.friendsOnly, { viewModel.selectFriendsOnly(false) })
            FilterChip(stringResource(Res.string.filter_friends), ui.friendsOnly, { viewModel.selectFriendsOnly(true) })
        }
    }
}

/** Two numbers over today's table: the time to the next puzzle (it ticks) and my place on the table, when I am on it. */
@OptIn(ExperimentalTime::class)
@Composable
private fun PlaceBox(ui: SocialUiState) {
    var nowMs by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMs = Clock.System.now().toEpochMilliseconds()
        }
    }
    val board = ui.speed
    val place = board?.rows?.firstOrNull { it.isMe }?.rank ?: board?.me?.rank
    StatBox(
        leftLabel = stringResource(Res.string.ranks_next_puzzle),
        leftValue = formatCountdown(IstanbulDay.millisUntilNextDay(nowMs)),
        rightLabel = stringResource(Res.string.ranks_your_place),
        rightValue = place?.let { "#$it" } ?: "–",
    )
}

/** The streak table's two numbers: my streak and my place on the table (a dash when I am not on it). */
@Composable
private fun StreakBox(ui: SocialUiState) {
    val board = ui.streak
    val me = board?.rows?.firstOrNull { it.isMe } ?: board?.me
    StatBox(
        leftLabel = stringResource(Res.string.ranks_your_streak),
        leftValue = me?.value?.toString() ?: "–",
        rightLabel = stringResource(Res.string.ranks_your_place),
        rightValue = me?.rank?.let { "#$it" } ?: "–",
    )
}

/** Two labeled numbers in a box, side by side with a thin bar between them. */
@Composable
private fun StatBox(leftLabel: String, leftValue: String, rightLabel: String, rightValue: String) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    @Composable
    fun Cell(label: String, value: String, modifier: Modifier) =
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(label, style = type.caption.copy(color = colors.textSecondary))
            BasicText(value, style = type.title.copy(color = colors.textPrimary))
        }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .border(1.dp, colors.borderSubtle, Radius.card)
            .padding(vertical = Spacing.sm),
    ) {
        Cell(leftLabel, leftValue, Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(40.dp).background(colors.borderSubtle).align(Alignment.CenterVertically))
        Cell(rightLabel, rightValue, Modifier.weight(1f))
    }
}

/** The top of the streak tab: a big flame in the streak's color and the name of the streak. */
@Composable
private fun StreakHero() {
    val colors = BrainScrollTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().testTag("streakHero"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        FlameIcon(colors.streakDay, iconSize = 64.dp)
        BasicText(stringResource(Res.string.daily_streak_label), style = BrainScrollTheme.typography.title.copy(color = colors.textPrimary))
    }
}

private val BADGE_SIZE = 84.dp

// --- Speed and streak tables ---

/** One line of a table, whatever the table: who, which place, the main number and a smaller one under it. */
private class BoardEntry(val rank: Int, val name: String, val isMe: Boolean, val primary: String, val secondary: String? = null, val flame: Boolean = false, val stars: Int? = null)

private fun LazyListScope.speedSection(ui: SocialUiState, viewModel: SocialViewModel) {
    boardItems(ui, ui.speed, viewModel) { row ->
        BoardEntry(
            rank = row.rank,
            name = row.username,
            isMe = row.isMe,
            // The big number is what the table is ranked by, the small one what breaks a tie.
            primary = when (ui.game.rankKind()) {
                RankKind.Time -> formatDuration(row.durationMs)
                RankKind.Rounds -> stringResource(Res.string.ranks_rounds, row.guesses)
                RankKind.Guesses -> stringResource(Res.string.ranks_guesses, row.guesses)
                RankKind.Stars -> ""
            },
            secondary = when (ui.game.rankKind()) {
                RankKind.Time -> if (row.guesses > 0) stringResource(Res.string.ranks_moves, row.guesses) else null
                else -> formatDuration(row.durationMs)
            },
            stars = if (ui.game.rankKind() == RankKind.Stars) row.guesses.coerceIn(0, LineScoring.MAX_STARS) else null,
        )
    }
}

private fun LazyListScope.streakSection(ui: SocialUiState, viewModel: SocialViewModel) {
    boardItems(ui, ui.streak, viewModel) { row ->
        BoardEntry(rank = row.rank, name = row.username, isMe = row.isMe, primary = row.value.toString(), flame = true)
    }
}

/** The status line, the rows, and my own row when I am below the top. */
private fun <R> LazyListScope.boardItems(
    ui: SocialUiState,
    board: Board<R>?,
    viewModel: SocialViewModel,
    entry: @Composable (R) -> BoardEntry,
) {
    item {
        when {
            board == null && ui.failed -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Body(stringResource(Res.string.ranks_failed))
                AppButton(stringResource(Res.string.daily_retry), viewModel::refresh)
            }
            board == null -> Body(stringResource(Res.string.ranks_loading))
            ui.stale -> Caption(stringResource(Res.string.ranks_stale))
        }
    }
    if (board == null) return
    if (board.rows.isEmpty()) {
        item { EmptyBoard(ui) }
        return
    }
    items(board.rows) { row -> BoardLine(entry(row)) }
    val me = board.me
    if (me != null && board.rows.none { it == me }) {
        item { Caption("…") }
        item { BoardLine(entry(me)) }
    }
}

/**
 * A table that nobody is on yet: not a dry line but an invitation. The picture of the game (for a speed table), a short title and a line
 * that says the first place is free, so the player wants to be the one who takes it.
 */
@Composable
private fun EmptyBoard(ui: SocialUiState) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val world = colors.accent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ranksEmpty")
            .background(colors.bgSurface, Radius.card)
            .border(1.dp, world.copy(alpha = 0.35f), Radius.card)
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // The same sign for every table: an empty podium (dashed bars, the middle one the tallest, a star over it).
        val ink = colors.textSecondary
        Canvas(Modifier.requiredSize(64.dp)) {
            val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
            val stroke = Stroke(width = 2.dp.toPx(), pathEffect = dash)
            val barW = size.width * 0.26f
            val gap = size.width * 0.05f
            val x0 = (size.width - (barW * 3 + gap * 2)) / 2f
            val base = size.height * 0.95f
            val heights = floatArrayOf(0.42f, 0.62f, 0.3f)
            for (i in 0 until 3) {
                val h = size.height * heights[i] * 1.1f
                drawRoundRect(
                    ink,
                    topLeft = androidx.compose.ui.geometry.Offset(x0 + i * (barW + gap), base - h),
                    size = androidx.compose.ui.geometry.Size(barW, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = stroke,
                )
            }
        }
        BasicText(
            text = stringResource(if (ui.friendsOnly) Res.string.ranks_empty_friends_title else Res.string.ranks_empty_title),
            style = type.heading.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
        )
        BasicText(
            text = stringResource(if (ui.friendsOnly) Res.string.ranks_empty_friends_body else Res.string.ranks_empty_body),
            style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun displayName(entry: BoardEntry) =
    if (entry.isMe) "${entry.name} (${stringResource(Res.string.ranks_you)})" else entry.name

@Composable
private fun BoardLine(entry: BoardEntry) {
    RankRow(
        rank = entry.rank,
        name = entry.name,
        displayName = displayName(entry),
        isMe = entry.isMe,
        primary = entry.primary,
        secondary = entry.secondary,
        flame = entry.flame,
        stars = entry.stars,
    )
}

// --- Gates ---

@Composable
private fun SignInCard(signIn: RanksSignIn) {
    Card {
        Body(stringResource(Res.string.gate_sign_in))
        if (signIn.canSignInWithApple) AppleSignInButton(stringResource(Res.string.sign_in_apple), signIn.onSignInWithApple)
        GoogleSignInButton(stringResource(Res.string.sign_in_google), signIn.onSignInWithGoogle)
    }
}

@Composable
private fun UsernameCard(viewModel: SocialViewModel) {
    var name by remember { mutableStateOf("") }
    Card {
        Heading(stringResource(Res.string.gate_username_title))
        Caption(stringResource(Res.string.gate_username_text))
        TextAction(
            value = name,
            onChange = { name = it.take(MAX_NAME) },
            hint = stringResource(Res.string.gate_username_hint),
            actionLabel = stringResource(Res.string.gate_username_save),
            onAction = { viewModel.saveUsername(name) },
        )
    }
}

// --- Friends ---

private fun LazyListScope.friendsSection(ui: SocialUiState, viewModel: SocialViewModel) {
    if (ui.access != Access.Ready) {
        // The gate card above says what to do first; nothing else to show here.
        return
    }
    item { InviteCard(ui) }
    item { AddFriend(viewModel) }

    val friends = ui.friends
    if (friends == null) {
        item { if (ui.failed) Body(stringResource(Res.string.ranks_failed)) else Body(stringResource(Res.string.ranks_loading)) }
        return
    }
    if (ui.stale) item { Caption(stringResource(Res.string.ranks_stale)) }

    if (friends.incoming.isNotEmpty()) {
        item { Heading(stringResource(Res.string.friends_requests)) }
        items(friends.incoming) { name ->
            PersonLine(name) {
                SmallAction(stringResource(Res.string.friends_accept), primary = true) { viewModel.respond(name, true) }
                SmallAction(stringResource(Res.string.friends_decline)) { viewModel.respond(name, false) }
            }
        }
    }
    // "Your friends", with how many on the right (no number for nobody: the empty picture says it).
    item {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Heading(stringResource(Res.string.friends_list)) }
            if (friends.friends.isNotEmpty()) CountPill(friends.friends.size)
        }
    }
    if (friends.friends.isEmpty()) item { NoFriendsYet() }
    items(friends.friends) { name ->
        PersonLine(name) { SmallAction(stringResource(Res.string.friends_remove)) { viewModel.removeFriend(name) } }
    }
    if (friends.outgoing.isNotEmpty()) {
        item { Heading(stringResource(Res.string.friends_sent)) }
        items(friends.outgoing) { name ->
            PersonLine(name) { SmallAction(stringResource(Res.string.friends_cancel)) { viewModel.cancelRequest(name) } }
        }
    }
}

/** A small rounded number beside a heading. */
@Composable
private fun CountPill(count: Int) {
    val colors = BrainScrollTheme.colors
    BasicText(
        text = count.toString(),
        style = BrainScrollTheme.typography.label.copy(color = colors.textPrimary),
        modifier = Modifier
            .testTag("friendsCount")
            .background(colors.bgSurface, Radius.pill)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
    )
}

/** What the list says when there is nobody: a little picture of two pieces that fit, and what to do. */
@Composable
private fun NoFriendsYet() {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg).testTag("noFriends"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        PuzzlePair()
        BasicText(
            text = stringResource(Res.string.friends_empty_title),
            style = type.title.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
        )
        BasicText(
            text = stringResource(Res.string.friends_none),
            style = type.body.copy(color = colors.textSoft, textAlign = TextAlign.Center),
        )
    }
}

/**
 * One way to add a friend at a time, chosen with the same sliding pill as the levels of the statistics: by their code, or by asking
 * their username. One field and one button, which waits (dim) until something is typed.
 */
@Composable
private fun AddFriend(viewModel: SocialViewModel) {
    var byCode by remember { mutableStateOf(true) }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Heading(stringResource(Res.string.friends_add_title))
        PillSegmented(
            options = listOf(stringResource(Res.string.friends_add_code), stringResource(Res.string.friends_add_name)),
            selectedIndex = if (byCode) 0 else 1,
            onSelect = { byCode = it == 0 },
        )
        if (byCode) {
            TextAction(
                value = code,
                onChange = { code = it.take(MAX_CODE) },
                hint = stringResource(Res.string.friends_code_hint),
                actionLabel = stringResource(Res.string.friends_add),
                onAction = { viewModel.addFriendByCode(code); code = "" },
                onSurface = false,
                actionEnabled = code.isNotBlank(),
            )
        } else {
            TextAction(
                value = name,
                onChange = { name = it.take(MAX_NAME) },
                hint = stringResource(Res.string.friends_name_hint),
                actionLabel = stringResource(Res.string.friends_ask),
                onAction = { viewModel.sendRequest(name); name = "" },
                onSurface = false,
                actionEnabled = name.isNotBlank(),
            )
        }
    }
}

/**
 * My invite code: a link icon and the name, the code big and green with a square button beside it that copies the code, a line
 * about what it is for, and a button that copies the whole invite message. Each icon turns into a tick for three seconds when it
 * has copied, and the message button says "Copied!".
 */
@Composable
private fun InviteCard(ui: SocialUiState) {
    val code = ui.inviteCode ?: return
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val clipboard = LocalClipboardManager.current
    val message = stringResource(Res.string.friends_invite_message, code, inviteLink(code))
    var codeCopied by remember { mutableStateOf(false) }
    var messageCopied by remember { mutableStateOf(false) }
    LaunchedEffect(codeCopied) { if (codeCopied) { delay(COPIED_MILLIS); codeCopied = false } }
    LaunchedEffect(messageCopied) { if (messageCopied) { delay(COPIED_MILLIS); messageCopied = false } }
    val copyCodeDescription = stringResource(Res.string.friends_copy_code)

    Column(
        modifier = Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            LineIcon(LineIcon.Link, colors.accent, size = 24.dp)
            BasicText(stringResource(Res.string.friends_my_code), style = type.heading.copy(color = colors.textPrimary))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            BasicText(
                text = code,
                modifier = Modifier.weight(1f).testTag("inviteCode"),
                maxLines = 1,
                style = type.display.copy(color = colors.correctFill, letterSpacing = 4.sp),
            )
            Box(
                modifier = Modifier
                    .testTag("copyCode")
                    .size(48.dp)
                    .background(colors.bgRaised, Radius.control)
                    .semantics { contentDescription = copyCodeDescription }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                        clipboard.setText(AnnotatedString(code))
                        codeCopied = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                LineIcon(if (codeCopied) LineIcon.Check else LineIcon.Copy, if (codeCopied) colors.accent else colors.textPrimary, size = 24.dp)
            }
        }
        BasicText(stringResource(Res.string.friends_my_code_caption), style = type.body.copy(color = colors.textSoft))
        Row(
            modifier = Modifier
                .testTag("copyInvite")
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .border(1.5.dp, colors.borderStrong, Radius.control)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                    clipboard.setText(AnnotatedString(message))
                    messageCopied = true
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        ) {
            LineIcon(if (messageCopied) LineIcon.Check else LineIcon.Message, if (messageCopied) colors.accent else colors.textPrimary, size = 24.dp)
            BasicText(
                text = stringResource(if (messageCopied) Res.string.friends_copied_short else Res.string.friends_copy),
                style = type.label.copy(color = colors.textPrimary),
            )
        }
    }
}

/** How long a copy button shows its tick before it goes back. */
private const val COPIED_MILLIS = 3_000L

/** One person: their avatar and name, and what can be done with them (a card, like a row of a leaderboard). */
@Composable
private fun PersonLine(name: String, actions: @Composable () -> Unit) {
    val colors = BrainScrollTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.control)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PlayerAvatar(name, 36.dp)
        BasicText(
            text = name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = BrainScrollTheme.typography.body.copy(color = colors.textPrimary),
        )
        actions()
    }
}

@Composable
private fun SmallAction(text: String, primary: Boolean = false, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    // The pill is 36 dp, the touch area 48 dp.
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 36.dp)
                .background(if (primary) colors.correctFill else colors.bgRaised, Radius.pill)
                .padding(horizontal = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text, style = BrainScrollTheme.typography.label.copy(color = if (primary) colors.correctOn else colors.textPrimary))
        }
    }
}

// --- Small building blocks ---

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrainScrollTheme.colors.bgSurface, Radius.card)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) { content() }
}

@Composable
private fun Heading(text: String) =
    BasicText(text, style = BrainScrollTheme.typography.heading.copy(color = BrainScrollTheme.colors.textPrimary))

@Composable
private fun Body(text: String) =
    BasicText(text, style = BrainScrollTheme.typography.body.copy(color = BrainScrollTheme.colors.textSecondary))

@Composable
private fun Caption(text: String) =
    BasicText(text, style = BrainScrollTheme.typography.caption.copy(color = BrainScrollTheme.colors.textSecondary))

private const val MAX_NAME = 16
private const val MAX_CODE = 8
