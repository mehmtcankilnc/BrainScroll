package com.mehmtcan.brainscroll.ui.profile

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.semantics.selected
import brainscroll.shared.generated.resources.confirm_cancel
import brainscroll.shared.generated.resources.favorites_none_match
import brainscroll.shared.generated.resources.favorites_only_records
import brainscroll.shared.generated.resources.favorites_pill_daily
import brainscroll.shared.generated.resources.favorites_remove_confirm
import brainscroll.shared.generated.resources.favorites_remove_text
import brainscroll.shared.generated.resources.favorites_remove_title
import brainscroll.shared.generated.resources.favorites_sort_fastest
import brainscroll.shared.generated.resources.favorites_sort_oldest
import brainscroll.shared.generated.resources.favorites_sort_recent
import brainscroll.shared.generated.resources.favorites_sort_title
import brainscroll.shared.generated.resources.howto_done
import brainscroll.shared.generated.resources.month_short_1
import brainscroll.shared.generated.resources.month_short_2
import brainscroll.shared.generated.resources.month_short_3
import brainscroll.shared.generated.resources.month_short_4
import brainscroll.shared.generated.resources.month_short_5
import brainscroll.shared.generated.resources.month_short_6
import brainscroll.shared.generated.resources.month_short_7
import brainscroll.shared.generated.resources.month_short_8
import brainscroll.shared.generated.resources.month_short_9
import brainscroll.shared.generated.resources.month_short_10
import brainscroll.shared.generated.resources.month_short_11
import brainscroll.shared.generated.resources.month_short_12
import com.mehmtcan.brainscroll.ui.components.AppSwitch
import com.mehmtcan.brainscroll.ui.components.ConfirmDialog
import com.mehmtcan.brainscroll.ui.components.FilterChip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import org.jetbrains.compose.resources.StringResource
import brainscroll.shared.generated.resources.settings_preferences
import brainscroll.shared.generated.resources.settings_look
import brainscroll.shared.generated.resources.settings_account
import brainscroll.shared.generated.resources.settings_privacy
import androidx.compose.ui.draw.clip
import brainscroll.shared.generated.resources.stats_all_games
import brainscroll.shared.generated.resources.stats_not_played
import brainscroll.shared.generated.resources.stats_played_count
import brainscroll.shared.generated.resources.stats_your_games
import com.mehmtcan.brainscroll.ui.components.PillSegmented
import com.mehmtcan.brainscroll.ui.home.GameThumbnail
import androidx.compose.foundation.border
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import brainscroll.shared.generated.resources.profile_edit
import brainscroll.shared.generated.resources.profile_feed_run_hint
import brainscroll.shared.generated.resources.profile_guest_subtitle
import brainscroll.shared.generated.resources.profile_guest_title
import brainscroll.shared.generated.resources.profile_keep_text
import brainscroll.shared.generated.resources.profile_keep_title
import brainscroll.shared.generated.resources.profile_section_activity
import brainscroll.shared.generated.resources.profile_section_social
import brainscroll.shared.generated.resources.profile_sign_in
import brainscroll.shared.generated.resources.sign_in_apple
import brainscroll.shared.generated.resources.sign_in_google
import brainscroll.shared.generated.resources.tab_profile
import brainscroll.shared.generated.resources.unit_day
import brainscroll.shared.generated.resources.unit_days
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.AppleSignInButton
import com.mehmtcan.brainscroll.ui.components.GoogleSignInButton
import com.mehmtcan.brainscroll.ui.components.LineIcon
import com.mehmtcan.brainscroll.ui.components.ModalSheet
import com.mehmtcan.brainscroll.ui.components.PlayerAvatar
import com.mehmtcan.brainscroll.ui.social.RanksSignIn
import com.mehmtcan.brainscroll.ui.theme.textSoft
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
import androidx.compose.runtime.CompositionLocalProvider
import com.mehmtcan.brainscroll.telemetry.NoTelemetry
import com.mehmtcan.brainscroll.telemetry.Telemetry
import com.mehmtcan.brainscroll.ui.pro.PaywallSource
import com.mehmtcan.brainscroll.ui.pro.StoreButton
import com.mehmtcan.brainscroll.ui.theme.Accent
import com.mehmtcan.brainscroll.ui.theme.LocalBrainScrollColors
import com.mehmtcan.brainscroll.ui.theme.withAccent
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
import brainscroll.shared.generated.resources.stats_matchsticks
import brainscroll.shared.generated.resources.stats_lasers
import brainscroll.shared.generated.resources.stats_untangle
import brainscroll.shared.generated.resources.stats_codebreaker
import brainscroll.shared.generated.resources.stats_fences
import brainscroll.shared.generated.resources.stats_mirror
import brainscroll.shared.generated.resources.stats_crosssum
import brainscroll.shared.generated.resources.stats_oddone
import brainscroll.shared.generated.resources.stats_greaterless
import brainscroll.shared.generated.resources.stats_lamps
import brainscroll.shared.generated.resources.stats_lonenumber
import brainscroll.shared.generated.resources.stats_zerosones
import brainscroll.shared.generated.resources.stats_rectangles
import brainscroll.shared.generated.resources.stats_colorseq
import brainscroll.shared.generated.resources.stats_cubefold
import brainscroll.shared.generated.resources.stats_flashgrid
import brainscroll.shared.generated.resources.stat_best_rounds
import brainscroll.shared.generated.resources.favorite_flashgrid_result
import brainscroll.shared.generated.resources.favorite_colorseq_result
import brainscroll.shared.generated.resources.favorite_colorseq_tag
import brainscroll.shared.generated.resources.favorite_cubefold_result
import brainscroll.shared.generated.resources.favorite_cubefold_tag
import brainscroll.shared.generated.resources.favorite_flashgrid_tag
import brainscroll.shared.generated.resources.favorite_lonenumber_result
import brainscroll.shared.generated.resources.favorite_fences_result
import brainscroll.shared.generated.resources.favorite_mirror_result
import brainscroll.shared.generated.resources.favorite_mirror_tag
import brainscroll.shared.generated.resources.favorite_oddone_result
import brainscroll.shared.generated.resources.favorite_crosssum_result
import brainscroll.shared.generated.resources.favorite_crosssum_tag
import brainscroll.shared.generated.resources.favorite_oddone_tag
import brainscroll.shared.generated.resources.favorite_fences_tag
import brainscroll.shared.generated.resources.favorite_greaterless_result
import brainscroll.shared.generated.resources.favorite_greaterless_tag
import brainscroll.shared.generated.resources.favorite_lamps_result
import brainscroll.shared.generated.resources.favorite_lamps_tag
import brainscroll.shared.generated.resources.favorite_lonenumber_tag
import brainscroll.shared.generated.resources.favorite_zerosones_result
import brainscroll.shared.generated.resources.favorite_zerosones_tag
import brainscroll.shared.generated.resources.favorite_rectangles_result
import brainscroll.shared.generated.resources.favorite_rectangles_tag
import brainscroll.shared.generated.resources.favorite_codebreaker_result
import brainscroll.shared.generated.resources.favorite_codebreaker_tag
import brainscroll.shared.generated.resources.stats_fruitmath
import brainscroll.shared.generated.resources.favorite_fruitmath_result
import brainscroll.shared.generated.resources.favorite_fruitmath_tag
import brainscroll.shared.generated.resources.favorite_untangle_result
import brainscroll.shared.generated.resources.favorite_untangle_tag
import brainscroll.shared.generated.resources.favorite_lasers_result
import brainscroll.shared.generated.resources.favorite_lasers_tag
import brainscroll.shared.generated.resources.favorite_matchsticks_result
import brainscroll.shared.generated.resources.favorite_matchsticks_tag
import brainscroll.shared.generated.resources.daily_freeze
import brainscroll.shared.generated.resources.favorite_daily_tag
import brainscroll.shared.generated.resources.favorite_lost
import brainscroll.shared.generated.resources.favorite_won
import brainscroll.shared.generated.resources.favorites_empty
import brainscroll.shared.generated.resources.favorites_title
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
import brainscroll.shared.generated.resources.stats_hanoi
import brainscroll.shared.generated.resources.stats_queens
import brainscroll.shared.generated.resources.stats_tents
import brainscroll.shared.generated.resources.stats_arrows
import brainscroll.shared.generated.resources.stats_axissum
import brainscroll.shared.generated.resources.stats_ballslide
import brainscroll.shared.generated.resources.stats_colorflow
import brainscroll.shared.generated.resources.stats_topicgroups
import brainscroll.shared.generated.resources.ranks_friends
import brainscroll.shared.generated.resources.profile_friends_hint
import brainscroll.shared.generated.resources.favorite_topicgroups_result
import brainscroll.shared.generated.resources.favorite_topicgroups_tag
import brainscroll.shared.generated.resources.favorite_colorflow_result
import brainscroll.shared.generated.resources.favorite_colorflow_tag
import brainscroll.shared.generated.resources.favorite_ballslide_result
import brainscroll.shared.generated.resources.favorite_ballslide_tag
import brainscroll.shared.generated.resources.favorite_axissum_result
import brainscroll.shared.generated.resources.favorite_axissum_tag
import brainscroll.shared.generated.resources.stats_nonogram
import brainscroll.shared.generated.resources.favorite_nonogram_result
import brainscroll.shared.generated.resources.favorite_nonogram_tag
import brainscroll.shared.generated.resources.stat_flawless
import brainscroll.shared.generated.resources.favorite_arrows_result
import brainscroll.shared.generated.resources.favorite_arrows_tag
import brainscroll.shared.generated.resources.stats_blockout
import brainscroll.shared.generated.resources.favorite_blockout_result
import brainscroll.shared.generated.resources.favorite_blockout_tag
import brainscroll.shared.generated.resources.stats_jigsaw
import brainscroll.shared.generated.resources.favorite_jigsaw_result
import brainscroll.shared.generated.resources.favorite_jigsaw_tag
import brainscroll.shared.generated.resources.favorite_tents_result
import brainscroll.shared.generated.resources.favorite_tents_tag
import brainscroll.shared.generated.resources.favorite_queens_result
import brainscroll.shared.generated.resources.favorite_queens_tag
import brainscroll.shared.generated.resources.stats_line
import brainscroll.shared.generated.resources.stats_pipes
import brainscroll.shared.generated.resources.favorite_hanoi_result
import brainscroll.shared.generated.resources.favorite_hanoi_tag
import brainscroll.shared.generated.resources.favorite_line_result
import brainscroll.shared.generated.resources.favorite_line_stars
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
import brainscroll.shared.generated.resources.stats_endless
import brainscroll.shared.generated.resources.stats_title
import brainscroll.shared.generated.resources.tab_ranks
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.ui.components.SegmentedControl
import com.mehmtcan.brainscroll.ui.home.difficultyName
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
enum class ProfileTarget { Stats, Favorites, Settings, Ranks, Friends }

/**
 * The profile tab, top to bottom: the title, who you are (an avatar, the name and a way to edit it; for a guest, a line that
 * asks to sign in), the two streaks with a few words each (tap one to see what it means), three numbers that sum you up, and
 * two groups of lines that lead to the pages: "Your activity" (statistics, favorites, leaderboards) and "Social & settings"
 * (friends, settings). Each line says in a few words what is behind it.
 */
@Composable
fun ProfileScreen(
    profile: ProfileState,
    account: AccountState,
    onOpen: (ProfileTarget) -> Unit,
    modifier: Modifier = Modifier,
    /** The name on the tables, once the player has one. */
    username: String? = null,
    /** Opens the purchase screen from the Pro card. */
    onPro: (PaywallSource) -> Unit = {},
    telemetry: Telemetry = NoTelemetry,
    /**
     * The sign-in choices. The "Sign in" button of a guest opens them in a sheet right on this page where there are two (Google and
     * Apple, on iOS); where there is only Google (Android) it starts the Google sign-in at once. Null: it opens the settings.
     */
    signIn: RanksSignIn? = null,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    // The endless numbers already include memory match, so only the daily puzzle is added.
    val played = profile.stats.played + profile.daily.played
    val won = profile.stats.won + profile.daily.won
    var streakInfo by remember { mutableStateOf<StreakKind?>(null) }
    var signInSheet by remember { mutableStateOf(false) }
    val named = (account as? AccountState.SignedIn)?.let { it.kind != AccountState.SignedIn.Kind.Anonymous } == true
    // Signing in worked: the sheet has done its job.
    LaunchedEffect(named) { if (named) signInSheet = false }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = stringResource(Res.string.tab_profile),
                    style = type.display.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                StoreButton()
            }
            ProfileHeader(account, username)
            if (!named) SignInCard(offline = account !is AccountState.SignedIn) {
                when {
                    // Where Apple is offered too (iOS) there is a choice to make: the sheet. Where only Google is, go straight to it.
                    signIn == null -> onOpen(ProfileTarget.Settings)
                    signIn.canSignInWithApple -> signInSheet = true
                    else -> signIn.onSignInWithGoogle()
                }
            }

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

            // Before the first game there is no rate to speak of: a dash, not "0%".
            val rate = if (played == 0) "–" else "${(won.toFloat() / played * 100).roundToInt()}%"
            Row(
                modifier = Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(vertical = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummaryNumber(played.toString(), stringResource(Res.string.stat_played), Modifier.weight(1f))
                SummaryDivider()
                SummaryNumber(rate, stringResource(Res.string.stat_win_rate), Modifier.weight(1f))
                SummaryDivider()
                SummaryNumber(won.toString(), stringResource(Res.string.stat_won), Modifier.weight(1f))
            }

            ProfileGroup(stringResource(Res.string.profile_section_activity)) {
                ProfileRow(LineIcon.Bars, stringResource(Res.string.stats_title), stringResource(Res.string.profile_stats_hint)) { onOpen(ProfileTarget.Stats) }
                ProfileRow(
                    LineIcon.Heart,
                    stringResource(Res.string.favorites_title),
                    stringResource(Res.string.profile_favorites_hint),
                    value = profile.favorites.size.takeIf { it > 0 }?.toString(),
                ) { onOpen(ProfileTarget.Favorites) }
                ProfileRow(LineIcon.Trophy, stringResource(Res.string.tab_ranks), stringResource(Res.string.profile_ranks_hint), last = true) { onOpen(ProfileTarget.Ranks) }
            }
            ProfileGroup(stringResource(Res.string.profile_section_social)) {
                ProfileRow(LineIcon.People, stringResource(Res.string.ranks_friends), stringResource(Res.string.profile_friends_hint)) { onOpen(ProfileTarget.Friends) }
                ProfileRow(LineIcon.Gear, stringResource(Res.string.profile_settings), stringResource(Res.string.profile_settings_hint), last = true) { onOpen(ProfileTarget.Settings) }
            }
            ProCard(onPro, telemetry)
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
        if (signInSheet && signIn != null) SignInSheet(signIn, onDismiss = { signInSheet = false })
    }
}

/**
 * Who is playing, the same for a guest and a signed-in player: an avatar on the left, the name and a line under it. A player with a
 * name gets the avatar made of the first letter of the name and a button to edit the profile at the right edge; a guest gets a
 * plain person and a line about the progress.
 */
@Composable
private fun ProfileHeader(account: AccountState, username: String?) {
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
    val name = if (named) username ?: signedIn?.email ?: provider else stringResource(Res.string.profile_guest_title)
    Row(
        modifier = Modifier.testTag("accountCard").fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (named) {
            PlayerAvatar(name, 64.dp)
        } else {
            Box(Modifier.size(64.dp).background(colors.bgRaised, CircleShape), contentAlignment = Alignment.Center) {
                LineIcon(LineIcon.Person, colors.textSoft, size = 32.dp)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(name, style = type.title.copy(color = colors.textPrimary), maxLines = 2, overflow = TextOverflow.Ellipsis)
            BasicText(
                text = if (named) provider else stringResource(Res.string.profile_guest_subtitle),
                style = type.body.copy(color = colors.textSoft),
            )
        }
    }
}

/**
 * The guest's card: what signing in gives (the games are kept) and the button that does it. The button opens the sign-in right
 * here; offline there is nothing to press, only the words.
 */
@Composable
private fun SignInCard(offline: Boolean, onSignIn: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(
        modifier = Modifier
            .testTag("signInCard")
            .fillMaxWidth()
            .background(colors.bgSurface, Radius.card)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        LineIcon(LineIcon.Cloud, colors.textPrimary, size = 28.dp)
        Box(Modifier.width(1.dp).height(40.dp).background(colors.borderSubtle))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(stringResource(Res.string.profile_keep_title), style = type.heading.copy(color = colors.textPrimary))
            BasicText(
                text = stringResource(if (offline) Res.string.account_offline else Res.string.profile_keep_text),
                style = type.caption.copy(color = colors.textSoft),
            )
        }
        if (!offline) {
            Box(
                modifier = Modifier
                    .testTag("profileSignIn")
                    .heightIn(min = 48.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onSignIn),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.heightIn(min = 36.dp).border(1.5.dp, colors.accent, Radius.pill).padding(horizontal = Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(stringResource(Res.string.profile_sign_in), style = type.label.copy(color = colors.accent))
                }
            }
        }
    }
}

/** The sign-in, in a sheet over the profile: Google, and Apple where the device has it. */
@Composable
private fun SignInSheet(signIn: RanksSignIn, onDismiss: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    ModalSheet(onDismiss = onDismiss, tag = "signInSheet") { close ->
        BasicText(stringResource(Res.string.profile_keep_title), style = type.title.copy(color = colors.textPrimary))
        BasicText(stringResource(Res.string.profile_keep_text), style = type.body.copy(color = colors.textSoft))
        // Apple first and filled, Google under it and a step quieter: the order and weight players know from other apps.
        if (signIn.canSignInWithApple) AppleSignInButton(stringResource(Res.string.sign_in_apple), { close(); signIn.onSignInWithApple() }, Modifier.testTag("signInApple"))
        GoogleSignInButton(stringResource(Res.string.sign_in_google), { close(); signIn.onSignInWithGoogle() }, Modifier.testTag("signInGoogle"))
    }
}

/**
 * One streak: its icon and name, the number now (with its unit) and a few words under it: the best so far for the day streak, what
 * the number counts for the feed record. The whole tile opens the explanation.
 */
@Composable
private fun StreakTile(kind: StreakKind, title: String, current: Int, best: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val accent = if (kind == StreakKind.Daily) colors.streakDay else colors.streakAnswer
    val description = stringResource(if (kind == StreakKind.Daily) Res.string.a11y_day_streak else Res.string.a11y_flow_record, current)
    val unit = stringResource(if (current == 1) Res.string.unit_day else Res.string.unit_days)
    val note = if (best != null) {
        stringResource(Res.string.streak_best, best) + " " + stringResource(if (best == 1) Res.string.unit_day else Res.string.unit_days)
    } else {
        stringResource(Res.string.profile_feed_run_hint)
    }
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
            BasicText(title, style = type.caption.copy(color = colors.textPrimary), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            BasicText(current.toString(), style = type.display.copy(color = accent))
            if (kind == StreakKind.Daily) {
                BasicText(unit, style = type.body.copy(color = colors.textSoft), modifier = Modifier.padding(bottom = 6.dp))
            }
        }
        BasicText(note, style = type.caption.copy(color = colors.textSoft))
    }
}

@Composable
private fun SummaryNumber(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(value, style = type.display.copy(color = colors.textPrimary), maxLines = 1, softWrap = false)
        BasicText(label, style = type.caption.copy(color = colors.textSoft, textAlign = TextAlign.Center), maxLines = 2)
    }
}

@Composable
private fun SummaryDivider() {
    Box(Modifier.width(1.dp).height(40.dp).background(BrainScrollTheme.colors.borderSubtle))
}

/** A small label over a card that holds a few lines of the list. */
@Composable
private fun ProfileGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = BrainScrollTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        GroupLabel(label)
        Column(Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card)) { content() }
    }
}

/** The small spaced-out label over a group of a page. */
@Composable
private fun GroupLabel(label: String) {
    BasicText(
        text = label,
        style = BrainScrollTheme.typography.caption.copy(color = BrainScrollTheme.colors.textSoft, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(horizontal = Spacing.xs),
    )
}

/** A line of a group: an icon, a name with a few words under it, an optional small value and a chevron. A hairline below, except the last. */
@Composable
private fun ProfileRow(icon: LineIcon, title: String, hint: String? = null, value: String? = null, last: Boolean = false, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            LineIcon(icon, colors.textPrimary, size = 24.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(title, style = type.heading.copy(color = colors.textPrimary))
                if (hint != null) BasicText(hint, style = type.caption.copy(color = colors.textSoft))
            }
            if (value != null) BasicText(value, style = type.label.copy(color = colors.textSoft))
            ForwardChevron(colors.textSoft)
        }
        if (!last) Box(Modifier.fillMaxWidth().padding(start = Spacing.lg + 24.dp + Spacing.md).height(1.dp).background(colors.borderSubtle))
    }
}

/**
 * The numbers. At the top the three that sum you up and, when games have levels, the level the numbers below are for (no "all":
 * the best time of an easy board does not belong next to a hard one's, so one level is always chosen). Then "Your games": one
 * card for each game with its own picture, its name and how often it was played; tapping a card opens its numbers. The games that
 * were played come first, the ones that were not follow in the usual order, marked "not played yet". Everything is derived from
 * the saved results.
 */
@Composable
fun StatsPage(
    profile: ProfileState,
    modifier: Modifier = Modifier,
    /** True once a game has levels: the per-game numbers are then for one level at a time. */
    showLevels: Boolean = false,
    onLevel: (Difficulty) -> Unit = {},
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val daily = profile.daily
    // The endless numbers already include memory match, so only the daily puzzle is added.
    val played = profile.stats.played + profile.daily.played
    val won = profile.stats.won + profile.daily.won
    // Before the first game there is no rate to speak of: a dash, not "0%".
    val rate = if (played == 0) "–" else "${(won.toFloat() / played * 100).roundToInt()}%"

    val playedLabel = stringResource(Res.string.stat_played)
    val winLabel = stringResource(Res.string.stat_win_rate)
    val bestLabel = stringResource(Res.string.stat_best_time)
    val movesLabel = stringResource(Res.string.stat_fewest_moves)
    val flawlessLabel = stringResource(Res.string.stat_flawless)
    fun pct(rate: Float) = "${(rate * 100).roundToInt()}%"
    fun time(ms: Long?) = ms?.let(::formatDuration) ?: "-"
    val levelName = if (showLevels) difficultyName(profile.statsLevel) else null

    fun entry(kind: GameKind, title: String, tag: String, played: Int, vararg tiles: Pair<String, String>) =
        StatEntry(kind, title, tag, played, levelName) { TileGrid(tiles.toList()) }
    // A game of which every result is a win has no rate: how many, the fastest and the fewest moves.
    fun moves(kind: GameKind, title: String, tag: String, played: Int, bestTimeMs: Long?, fewestMoves: Int?) =
        entry(kind, title, tag, played, time(bestTimeMs) to bestLabel, (fewestMoves?.toString() ?: "-") to movesLabel)
    fun timed(kind: GameKind, title: String, tag: String, played: Int, bestTimeMs: Long?) =
        entry(kind, title, tag, played, time(bestTimeMs) to bestLabel)

    // The daily puzzle always comes first: its streak counts days, not answers, and it has a time.
    val dailyEntry = StatEntry(GameKind.Line, stringResource(Res.string.stats_daily), "statsDaily", daily.played, null) {
        StatTiles(
            played = daily.played,
            winRate = daily.winRate,
            streak = profile.dayStreak.current,
            best = profile.dayStreak.best,
            streakLabel = stringResource(Res.string.stat_day_streak),
        )
        daily.bestTimeMs?.let { best ->
            BasicText(
                text = stringResource(Res.string.stat_best_time) + ": " + formatDuration(best),
                style = type.body.copy(color = colors.textPrimary),
            )
        }
        if (profile.dayStreak.freezesAvailable > 0) {
            BasicText(
                text = stringResource(Res.string.daily_freeze, profile.dayStreak.freezesAvailable),
                style = type.caption.copy(color = colors.textSoft),
            )
        }
    }
    val games = listOf(
        entry(GameKind.Word, stringResource(Res.string.stats_endless), "statsWord", profile.endless.played,
            pct(profile.endless.winRate) to winLabel),
        entry(GameKind.Memory, stringResource(Res.string.stats_memory), "statsMemory", profile.memory.played,
            pct(profile.memory.winRate) to winLabel,
            time(profile.memory.bestTimeMs) to bestLabel, (profile.memory.fewestMoves?.toString() ?: "-") to movesLabel),
        moves(GameKind.Tubes, stringResource(Res.string.stats_tubes), "statsTubes", profile.tubes.played, profile.tubes.bestTimeMs, profile.tubes.fewestMoves),
        moves(GameKind.Lights, stringResource(Res.string.stats_lights), "statsLights", profile.lights.played, profile.lights.bestTimeMs, profile.lights.fewestMoves),
        moves(GameKind.Slide, stringResource(Res.string.stats_slide), "statsSlide", profile.slide.played, profile.slide.bestTimeMs, profile.slide.fewestMoves),
        moves(GameKind.Target, stringResource(Res.string.stats_target), "statsTarget", profile.target.played, profile.target.bestTimeMs, profile.target.fewestMoves),
        moves(GameKind.Hanoi, stringResource(Res.string.stats_hanoi), "statsHanoi", profile.hanoi.played, profile.hanoi.bestTimeMs, profile.hanoi.fewestMoves),
        timed(GameKind.Jigsaw, stringResource(Res.string.stats_jigsaw), "statsJigsaw", profile.jigsaw.played, profile.jigsaw.bestTimeMs),
        timed(GameKind.Tents, stringResource(Res.string.stats_tents), "statsTents", profile.tents.played, profile.tents.bestTimeMs),
        entry(GameKind.Arrows, stringResource(Res.string.stats_arrows), "statsArrows", profile.arrows.played,
            time(profile.arrows.bestTimeMs) to bestLabel, profile.arrows.flawless.toString() to flawlessLabel),
        entry(GameKind.TopicGroups, stringResource(Res.string.stats_topicgroups), "statsTopicGroups", profile.topicGroups.played,
            time(profile.topicGroups.bestTimeMs) to bestLabel, profile.topicGroups.flawless.toString() to flawlessLabel),
        timed(GameKind.AxisSum, stringResource(Res.string.stats_axissum), "statsAxisSum", profile.axisSum.played, profile.axisSum.bestTimeMs),
        timed(GameKind.BallSlide, stringResource(Res.string.stats_ballslide), "statsBallSlide", profile.ballSlide.played, profile.ballSlide.bestTimeMs),
        timed(GameKind.ColorFlow, stringResource(Res.string.stats_colorflow), "statsColorFlow", profile.colorFlow.played, profile.colorFlow.bestTimeMs),
        entry(GameKind.FruitMath, stringResource(Res.string.stats_fruitmath), "statsFruitMath", profile.fruitMath.played,
            time(profile.fruitMath.bestTimeMs) to bestLabel, profile.fruitMath.flawless.toString() to flawlessLabel),
        entry(GameKind.CubeFold, stringResource(Res.string.stats_cubefold), "statsCubeFold", profile.cubeFold.played,
            (profile.cubeFold.bestRounds?.toString() ?: "-") to stringResource(Res.string.stat_best_rounds)),
        entry(GameKind.ColorSeq, stringResource(Res.string.stats_colorseq), "statsColorSeq", profile.colorSeq.played,
            (profile.colorSeq.bestRounds?.toString() ?: "-") to stringResource(Res.string.stat_best_rounds)),
        entry(GameKind.FlashGrid, stringResource(Res.string.stats_flashgrid), "statsFlashGrid", profile.flashGrid.played,
            (profile.flashGrid.bestRounds?.toString() ?: "-") to stringResource(Res.string.stat_best_rounds)),
        timed(GameKind.Rectangles, stringResource(Res.string.stats_rectangles), "statsRectangles", profile.rectangles.played, profile.rectangles.bestTimeMs),
        timed(GameKind.LoneNumber, stringResource(Res.string.stats_lonenumber), "statsLoneNumber", profile.loneNumber.played, profile.loneNumber.bestTimeMs),
        timed(GameKind.ZerosOnes, stringResource(Res.string.stats_zerosones), "statsZerosOnes", profile.zerosOnes.played, profile.zerosOnes.bestTimeMs),
        timed(GameKind.Lamps, stringResource(Res.string.stats_lamps), "statsLamps", profile.lamps.played, profile.lamps.bestTimeMs),
        timed(GameKind.GreaterLess, stringResource(Res.string.stats_greaterless), "statsGreaterLess", profile.greaterLess.played, profile.greaterLess.bestTimeMs),
        timed(GameKind.Fences, stringResource(Res.string.stats_fences), "statsFences", profile.fences.played, profile.fences.bestTimeMs),
        timed(GameKind.Mirror, stringResource(Res.string.stats_mirror), "statsMirror", profile.mirror.played, profile.mirror.bestTimeMs),
        timed(GameKind.CrossSum, stringResource(Res.string.stats_crosssum), "statsCrossSum", profile.crossSum.played, profile.crossSum.bestTimeMs),
        entry(GameKind.OddOne, stringResource(Res.string.stats_oddone), "statsOddOne", profile.oddOne.played,
            (profile.oddOne.bestRounds?.toString() ?: "-") to stringResource(Res.string.stat_best_rounds)),
        moves(GameKind.CodeBreaker, stringResource(Res.string.stats_codebreaker), "statsCodeBreaker", profile.codeBreaker.played, profile.codeBreaker.bestTimeMs, profile.codeBreaker.fewestMoves),
        timed(GameKind.Untangle, stringResource(Res.string.stats_untangle), "statsUntangle", profile.untangle.played, profile.untangle.bestTimeMs),
        timed(GameKind.Lasers, stringResource(Res.string.stats_lasers), "statsLasers", profile.lasers.played, profile.lasers.bestTimeMs),
        timed(GameKind.Matchsticks, stringResource(Res.string.stats_matchsticks), "statsMatchsticks", profile.matchsticks.played, profile.matchsticks.bestTimeMs),
        timed(GameKind.Nonogram, stringResource(Res.string.stats_nonogram), "statsNonogram", profile.nonogram.played, profile.nonogram.bestTimeMs),
        moves(GameKind.BlockOut, stringResource(Res.string.stats_blockout), "statsBlockOut", profile.blockOut.played, profile.blockOut.bestTimeMs, profile.blockOut.fewestMoves),
        moves(GameKind.Queens, stringResource(Res.string.stats_queens), "statsQueens", profile.queens.played, profile.queens.bestTimeMs, profile.queens.fewestMoves),
        moves(GameKind.Pipes, stringResource(Res.string.stats_pipes), "statsPipes", profile.pipes.played, profile.pipes.bestTimeMs, profile.pipes.fewestMoves),
        // Minesweeper: played, win rate and the fastest win.
        entry(GameKind.Mines, stringResource(Res.string.stats_mines), "statsMines", profile.mines.played,
            pct(profile.mines.winRate) to winLabel, time(profile.mines.bestTimeMs) to bestLabel),
        // A sudoku cannot be lost any more, so it has no win rate: played and the fastest win.
        timed(GameKind.Sudoku, stringResource(Res.string.stats_sudoku), "statsSudoku", profile.sudoku.played, profile.sudoku.bestTimeMs),
    )
    // The ones that were played first (the most played at the top); the rest keep their usual order (the sort is stable).
    val ordered = listOf(dailyEntry) + games.sortedByDescending { it.played }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            BasicText(
                text = stringResource(Res.string.stats_all_games),
                style = type.caption.copy(color = colors.textSoft, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(horizontal = Spacing.xs),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(vertical = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummaryNumber(played.toString(), playedLabel, Modifier.weight(1f))
                SummaryDivider()
                SummaryNumber(rate, winLabel, Modifier.weight(1f))
                SummaryDivider()
                SummaryNumber(won.toString(), stringResource(Res.string.stat_won), Modifier.weight(1f))
            }
        }

        // The numbers of each game below are for one level: the best time of an easy board does not belong next to a hard one's.
        if (showLevels) {
            item {
                PillSegmented(
                    options = Difficulty.entries.map { difficultyName(it) },
                    selectedIndex = Difficulty.entries.indexOf(profile.statsLevel),
                    onSelect = { onLevel(Difficulty.entries[it]) },
                    modifier = Modifier.testTag("statsLevel"),
                )
            }
        }

        item {
            BasicText(
                text = stringResource(Res.string.stats_your_games),
                style = type.title.copy(color = colors.textPrimary),
                modifier = Modifier.padding(top = Spacing.sm).semantics { heading() },
            )
        }
        items(ordered, key = { it.tag }) { entry -> StatGroup(entry) }
        item { Spacer(Modifier.height(Spacing.xl)) }
    }
}

/** One game of the statistics page: its picture and name, how often it was played, a note, and the numbers [content] shows when opened. */
private class StatEntry(
    val kind: GameKind,
    val title: String,
    val tag: String,
    val played: Int,
    /** The level the numbers are for, when games have levels (shown under the name). */
    val note: String?,
    val content: @Composable ColumnScope.() -> Unit,
)

/**
 * One game on the statistics page, a card of its own: the picture of the game, its name, "12 played" (or "not played yet") and
 * the level, always in view; tapping the card slides open the numbers under a hairline and turns the arrow.
 */
@Composable
private fun StatGroup(entry: StatEntry) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var open by remember { mutableStateOf(false) }
    val arrow by animateFloatAsState(if (open) -90f else 90f, tween(Motion.BASE, easing = Motion.Standard))
    Column(Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).testTag(entry.tag)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) { open = !open }
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            GameThumbnail(
                entry.kind,
                Language.entries.first(),
                Modifier.size(52.dp).clip(Radius.control).border(1.dp, colors.borderSubtle, Radius.control),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(entry.title, style = type.heading.copy(color = colors.textPrimary))
                BasicText(
                    text = if (entry.played > 0) stringResource(Res.string.stats_played_count, entry.played) else stringResource(Res.string.stats_not_played),
                    style = type.caption.copy(color = colors.textSoft),
                )
                entry.note?.let { BasicText(it, style = type.caption.copy(color = colors.textSoft)) }
            }
            ForwardChevron(colors.textSoft, Modifier.rotate(arrow))
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(Motion.BASE, easing = Motion.Standard)) + fadeIn(tween(Motion.BASE)),
            exit = shrinkVertically(tween(Motion.BASE, easing = Motion.Standard)) + fadeOut(tween(Motion.FAST)),
        ) {
            Column(Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
                entry.content(this)
            }
        }
    }
}

/** How the list of favorites is ordered. The list the player sees starts with the ones added last. */
private enum class FavoriteSort { Recent, Oldest, Fastest }

/**
 * The finished puzzles the player marked with the heart, one card each: the picture of the game, its name with the level and
 * "daily" beside it, the result and the date, the heart (tap, and after a question the puzzle leaves the list) and "Play
 * again", which starts a new feed with that very puzzle as its first page. A row of pills filters by game (the chosen one is
 * filled green); under it the way the list is ordered, and a button that opens the ordering and the filter for records.
 */
@Composable
fun FavoritesPage(
    profile: ProfileState,
    onReplay: (FinishedRound) -> Unit = {},
    modifier: Modifier = Modifier,
    /** The heart of a card was confirmed: the puzzle is not a favorite any more. */
    onRemove: (FinishedRound) -> Unit = {},
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    var filter by remember { mutableStateOf<GameKind?>(null) }
    var sort by remember { mutableStateOf(FavoriteSort.Recent) }
    var recordsOnly by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<FinishedRound?>(null) }
    val present = GameKind.entries.filter { game -> profile.favorites.any { it.game == game } }
    // A filter whose games were all un-hearted is gone: show everything again.
    val active = filter?.takeIf { it in present }
    val ofGame = if (active == null) profile.favorites else profile.favorites.filter { it.game == active }
    val filtered = if (recordsOnly) ofGame.filter { holdsRecord(profile, it) } else ofGame
    val shown = when (sort) {
        FavoriteSort.Recent -> filtered
        FavoriteSort.Oldest -> filtered.reversed()
        FavoriteSort.Fastest -> filtered.sortedBy { it.durationMs ?: Long.MAX_VALUE }
    }
    val sortName = stringResource(sort.label())

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (profile.favorites.isEmpty()) {
            item {
                BasicText(
                    stringResource(Res.string.favorites_empty),
                    style = type.body.copy(color = colors.textSoft),
                )
            }
        } else {
            if (present.size > 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        FilterChip(stringResource(Res.string.favorites_all), active == null, { filter = null }, solid = true)
                        present.forEach { game -> FilterChip(game.displayName(), active == game, { filter = game }, solid = true) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = if (recordsOnly) sortName + " · " + stringResource(Res.string.favorites_only_records) else sortName,
                        style = type.body.copy(color = colors.textSoft),
                        modifier = Modifier.weight(1f).testTag("favoritesSortLabel"),
                    )
                    Box(
                        modifier = Modifier
                            .testTag("favoritesSortButton")
                            .size(48.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) { sheet = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        val description = stringResource(Res.string.favorites_sort_title)
                        LineIcon(LineIcon.Sort, colors.textPrimary, Modifier.semantics { contentDescription = description }, size = 24.dp)
                    }
                }
            }
            items(shown, key = { it.id }) { round ->
                FavoriteCard(round, record = holdsRecord(profile, round), onReplay = { onReplay(round) }, onRemove = { removing = round })
            }
            if (shown.isEmpty()) {
                item { BasicText(stringResource(Res.string.favorites_none_match), style = type.body.copy(color = colors.textSoft)) }
            }
        }
        item { Spacer(Modifier.height(Spacing.xl)) }
    }

    removing?.let { round ->
        ConfirmDialog(
            title = stringResource(Res.string.favorites_remove_title),
            message = stringResource(Res.string.favorites_remove_text),
            confirmText = stringResource(Res.string.favorites_remove_confirm),
            cancelText = stringResource(Res.string.confirm_cancel),
            onConfirm = { removing = null; onRemove(round) },
            onCancel = { removing = null },
        )
    }
    if (sheet) {
        ModalSheet(onDismiss = { sheet = false }, tag = "favoritesSortSheet") { close ->
            BasicText(stringResource(Res.string.favorites_sort_title), style = type.title.copy(color = colors.textPrimary))
            FavoriteSort.entries.forEach { option ->
                val chosen = option == sort
                Row(
                    modifier = Modifier
                        .testTag("favoritesSort_" + option.name)
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics { selected = chosen }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) { sort = option },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(stringResource(option.label()), style = type.body.copy(color = if (chosen) colors.correctFill else colors.textPrimary), modifier = Modifier.weight(1f))
                    if (chosen) BasicText("✓", style = type.label.copy(color = colors.correctFill))
                }
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                val title = stringResource(Res.string.favorites_only_records)
                BasicText(title, style = type.body.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
                AppSwitch(recordsOnly, { recordsOnly = it }, title, Modifier.testTag("favoritesRecordsOnly"))
            }
            AppButton(stringResource(Res.string.howto_done), close, primary = true)
        }
    }
}

private fun FavoriteSort.label(): StringResource = when (this) {
    FavoriteSort.Recent -> Res.string.favorites_sort_recent
    FavoriteSort.Oldest -> Res.string.favorites_sort_oldest
    FavoriteSort.Fastest -> Res.string.favorites_sort_fastest
}

/** True when this won, unassisted puzzle is the fastest of its game (the daily word puzzle has its own best time). */
private fun holdsRecord(profile: ProfileState, round: FinishedRound): Boolean {
    val time = round.durationMs ?: return false
    if (round.outcome != Outcome.WON || round.assisted) return false
    // The best times on the profile are for one level: a puzzle of another level is not compared with them.
    if (!(round.mode == Mode.DAILY && round.game == GameKind.Word) && round.difficulty != profile.statsLevel) return false
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
        GameKind.Hanoi -> profile.hanoi.bestTimeMs
        GameKind.Queens -> profile.queens.bestTimeMs
        GameKind.Tents -> profile.tents.bestTimeMs
        GameKind.Jigsaw -> profile.jigsaw.bestTimeMs
        GameKind.BlockOut -> profile.blockOut.bestTimeMs
        GameKind.Arrows -> profile.arrows.bestTimeMs
        GameKind.Nonogram -> profile.nonogram.bestTimeMs
        GameKind.AxisSum -> profile.axisSum.bestTimeMs
        GameKind.BallSlide -> profile.ballSlide.bestTimeMs
        GameKind.ColorFlow -> profile.colorFlow.bestTimeMs
        GameKind.TopicGroups -> profile.topicGroups.bestTimeMs
        GameKind.Matchsticks -> profile.matchsticks.bestTimeMs
        GameKind.Lasers -> profile.lasers.bestTimeMs
        GameKind.Untangle -> profile.untangle.bestTimeMs
        GameKind.FruitMath -> profile.fruitMath.bestTimeMs
        GameKind.CodeBreaker -> profile.codeBreaker.bestTimeMs
        GameKind.Rectangles -> profile.rectangles.bestTimeMs
        GameKind.FlashGrid -> null
        GameKind.ColorSeq -> null
        GameKind.CubeFold -> null
        GameKind.LoneNumber -> profile.loneNumber.bestTimeMs
        GameKind.ZerosOnes -> profile.zerosOnes.bestTimeMs
        GameKind.Lamps -> profile.lamps.bestTimeMs
        GameKind.GreaterLess -> profile.greaterLess.bestTimeMs
        GameKind.Fences -> profile.fences.bestTimeMs
        GameKind.Mirror -> profile.mirror.bestTimeMs
        GameKind.OddOne -> null
        GameKind.CrossSum -> profile.crossSum.bestTimeMs
        GameKind.Line -> profile.line.bestTimeMs
    }
    return best != null && time <= best
}


/**
 * The settings, in groups with a small label over each: "Preferences" (the app language and the sound), "Appearance" when there
 * is something to choose, "Account" (who you are, and signing in or out) and "Privacy" (sharing usage data).
 */
@Composable
fun SettingsPage(account: AccountUi, privacy: PrivacyUi, language: LanguageUi, sound: PrivacyUi, modifier: Modifier = Modifier, look: LookUi? = null) {
    val colors = BrainScrollTheme.colors
    // Tapping a locked accent color shows it on the whole page for a moment.
    var preview by remember { mutableStateOf<Accent?>(null) }
    CompositionLocalProvider(LocalBrainScrollColors provides (preview?.let { colors.withAccent(it) } ?: colors)) {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item {
                ProfileGroup(stringResource(Res.string.settings_preferences)) {
                    LanguageRow(language)
                    Box(Modifier.fillMaxWidth().padding(start = Spacing.lg + 24.dp + Spacing.md).height(1.dp).background(colors.borderSubtle))
                    SoundRow(sound)
                }
            }
            if (look != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        GroupLabel(stringResource(Res.string.settings_look))
                        LookSection(look, onPreview = { preview = it })
                    }
                }
            }
            item { ProfileGroup(stringResource(Res.string.settings_account)) { AccountSection(account) } }
            item { ProfileGroup(stringResource(Res.string.settings_privacy)) { PrivacyRow(privacy) } }
            item { Spacer(Modifier.height(Spacing.xl)) }
        }
    }
}

@Composable
private fun StatTiles(played: Int, winRate: Float, streak: Int, best: Int, streakLabel: String) {
    TileGrid(
        listOf(
            "${(winRate * 100).roundToInt()}%" to stringResource(Res.string.stat_win_rate),
            streak.toString() to streakLabel,
            best.toString() to stringResource(Res.string.stat_best_streak),
        ),
    )
}

/** Value and label pairs in a row with thin bars between them: four in a row, two by two when the player chose big text. */
@Composable
private fun TileGrid(tiles: List<Pair<String, String>>) {
    // Four numbers side by side do not fit when the player chose big text: then two rows of two.
    val perRow = if (LocalDensity.current.fontScale > LARGE_TEXT_SCALE) 2 else 4
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        tiles.chunked(perRow).forEach { row ->
            // IntrinsicSize.Max makes the cells as tall as the tallest one, so the bars between them are as tall as the row.
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max), verticalAlignment = Alignment.CenterVertically) {
                row.forEachIndexed { index, (value, label) ->
                    if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = Spacing.sm).background(BrainScrollTheme.colors.borderSubtle))
                    StatTile(value, label, Modifier.weight(1f))
                }
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
        BasicText(label, style = type.caption.copy(color = colors.textSoft, textAlign = TextAlign.Center), maxLines = 2, modifier = Modifier.padding(horizontal = Spacing.xs))
    }
}

/**
 * One favorite as a card: the picture of its game on the left; the name (the word, for a word puzzle) with the level and "daily" as
 * small pills beside it, the result and the date in the middle; at the right the heart at the top and "Play again" at the bottom.
 */
@Composable
private fun FavoriteCard(round: FinishedRound, record: Boolean, onReplay: () -> Unit, onRemove: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(colors.bgSurface, Radius.card)
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        GameThumbnail(round.game, round.language, Modifier.size(64.dp).clip(Radius.control).border(1.dp, colors.borderSubtle, Radius.control))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BasicText(
                    text = favoriteTag(round),
                    style = type.heading.copy(color = colors.textPrimary),
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (round.difficulty != Difficulty.Medium) Pill(difficultyName(round.difficulty))
                if (round.mode == Mode.DAILY) Pill(stringResource(Res.string.favorites_pill_daily))
            }
            if (round.game == GameKind.Word) {
                BasicText(GameKind.Word.displayName(), style = type.caption.copy(color = colors.textSoft))
            }
            BasicText(favoriteResult(round), style = type.body.copy(color = colors.textSoft))
            BasicText(favoriteDate(round.dayIndex), style = type.caption.copy(color = colors.textSoft))
            if (record) {
                BasicText(stringResource(Res.string.favorites_record), style = type.label.copy(color = colors.pendingFill), modifier = Modifier.testTag("favoriteRecord"))
            }
        }
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
            HeartButton(filled = true, onClick = onRemove, tint = colors.accent, modifier = Modifier.offset(x = 12.dp, y = (-12).dp))
            AppTextButton(stringResource(Res.string.favorites_replay), onReplay, Modifier.testTag("favoriteReplay"))
        }
    }
}

/** A small rounded tag beside a name: the level, or "daily". */
@Composable
private fun Pill(text: String) {
    val colors = BrainScrollTheme.colors
    BasicText(
        text = text,
        style = BrainScrollTheme.typography.caption.copy(color = colors.textSoft),
        maxLines = 1,
        modifier = Modifier
            .background(colors.bgRaised, Radius.pill)
            .border(1.dp, colors.borderSubtle, Radius.pill)
            .padding(horizontal = Spacing.sm, vertical = 2.dp),
    )
}

/** The result line of a favorite: "5 guesses", "9 moves · 00:12", or that it was not solved. */
@Composable
private fun favoriteResult(round: FinishedRound): String {
    val time = formatDuration(round.durationMs ?: 0L)
    return when {
        round.outcome != Outcome.WON -> stringResource(Res.string.favorite_lost)
        round.game == GameKind.Memory -> stringResource(Res.string.favorite_memory_result, round.moveCount, time)
        round.game == GameKind.Tubes -> stringResource(Res.string.favorite_tubes_result, round.moveCount, time)
        round.game == GameKind.Mines -> stringResource(Res.string.favorite_mines_result, time)
        round.game == GameKind.Sudoku -> stringResource(Res.string.favorite_sudoku_result, time)
        round.game == GameKind.Lights -> stringResource(Res.string.favorite_lights_result, round.moveCount, time)
        round.game == GameKind.Slide -> stringResource(Res.string.favorite_slide_result, round.moveCount, time)
        round.game == GameKind.Target -> stringResource(Res.string.favorite_target_result, round.moveCount, time)
        round.game == GameKind.Pipes -> stringResource(Res.string.favorite_pipes_result, round.moveCount, time)
        round.game == GameKind.Hanoi -> stringResource(Res.string.favorite_hanoi_result, round.moveCount, time)
        round.game == GameKind.Queens -> stringResource(Res.string.favorite_queens_result, round.moveCount, time)
        round.game == GameKind.Tents -> stringResource(Res.string.favorite_tents_result, time)
        round.game == GameKind.Jigsaw -> stringResource(Res.string.favorite_jigsaw_result, time)
        round.game == GameKind.BlockOut -> stringResource(Res.string.favorite_blockout_result, round.moveCount, time)
        round.game == GameKind.Nonogram -> stringResource(Res.string.favorite_nonogram_result, time)
        round.game == GameKind.ColorFlow -> stringResource(Res.string.favorite_colorflow_result, round.moveCount, time)
        round.game == GameKind.FruitMath -> stringResource(Res.string.favorite_fruitmath_result, time)
        round.game == GameKind.FlashGrid -> stringResource(Res.string.favorite_flashgrid_result, round.moveCount)
        round.game == GameKind.ColorSeq -> stringResource(Res.string.favorite_colorseq_result, round.moveCount)
        round.game == GameKind.CubeFold -> stringResource(Res.string.favorite_cubefold_result, round.moveCount)
        round.game == GameKind.Rectangles -> stringResource(Res.string.favorite_rectangles_result, time)
        round.game == GameKind.LoneNumber -> stringResource(Res.string.favorite_lonenumber_result, time)
        round.game == GameKind.ZerosOnes -> stringResource(Res.string.favorite_zerosones_result, time)
        round.game == GameKind.Lamps -> stringResource(Res.string.favorite_lamps_result, time)
        round.game == GameKind.GreaterLess -> stringResource(Res.string.favorite_greaterless_result, time)
        round.game == GameKind.Fences -> stringResource(Res.string.favorite_fences_result, time)
        round.game == GameKind.Mirror -> stringResource(Res.string.favorite_mirror_result, time)
        round.game == GameKind.OddOne -> stringResource(Res.string.favorite_oddone_result, round.moveCount)
        round.game == GameKind.CrossSum -> stringResource(Res.string.favorite_crosssum_result, time)
        round.game == GameKind.CodeBreaker -> stringResource(Res.string.favorite_codebreaker_result, round.moveCount, time)
        round.game == GameKind.Untangle -> stringResource(Res.string.favorite_untangle_result, time)
        round.game == GameKind.Lasers -> stringResource(Res.string.favorite_lasers_result, time)
        round.game == GameKind.Matchsticks -> stringResource(Res.string.favorite_matchsticks_result, time)
        round.game == GameKind.BallSlide -> stringResource(Res.string.favorite_ballslide_result, round.moveCount, time)
        round.game == GameKind.AxisSum -> stringResource(Res.string.favorite_axissum_result, round.moveCount, time)
        round.game == GameKind.TopicGroups -> stringResource(Res.string.favorite_topicgroups_result, round.moveCount, time)
        round.game == GameKind.Arrows -> stringResource(Res.string.favorite_arrows_result, round.moveCount, time)
        round.game == GameKind.Line && round.mode == Mode.DAILY -> stringResource(Res.string.favorite_line_stars, round.lineStars, time)
        round.game == GameKind.Line -> stringResource(Res.string.favorite_line_result, round.moveCount, time)
        // A daily word puzzle is timed by the server: its time is said too.
        else -> stringResource(Res.string.favorite_won, round.guesses.size) + (round.durationMs?.takeIf { round.mode == Mode.DAILY }?.let { " · " + formatDuration(it) } ?: "")
    }
}

private val MonthNames = listOf(
    Res.string.month_short_1, Res.string.month_short_2, Res.string.month_short_3, Res.string.month_short_4,
    Res.string.month_short_5, Res.string.month_short_6, Res.string.month_short_7, Res.string.month_short_8,
    Res.string.month_short_9, Res.string.month_short_10, Res.string.month_short_11, Res.string.month_short_12,
)

/** The day as "20 Oct 2026" (or "20 Eki 2026"): the day, the short month in the app language, the year. */
@Composable
private fun favoriteDate(dayIndex: Long): String {
    val (year, month, day) = IstanbulDay.date(dayIndex)
    return "$day ${stringResource(MonthNames[month - 1])} $year"
}

/** The line a favorite starts with: the word of a word puzzle, the name of the game for the others. */
@Composable
private fun favoriteTag(round: FinishedRound): String = when (round.game) {
    GameKind.Word -> round.answer
    GameKind.Memory -> stringResource(Res.string.favorite_memory_tag)
    GameKind.Tubes -> stringResource(Res.string.favorite_tubes_tag)
    GameKind.Mines -> stringResource(Res.string.favorite_mines_tag)
    GameKind.Sudoku -> stringResource(Res.string.favorite_sudoku_tag)
    GameKind.Lights -> stringResource(Res.string.favorite_lights_tag)
    GameKind.Slide -> stringResource(Res.string.favorite_slide_tag)
    GameKind.Target -> stringResource(Res.string.favorite_target_tag)
    GameKind.Pipes -> stringResource(Res.string.favorite_pipes_tag)
    GameKind.Hanoi -> stringResource(Res.string.favorite_hanoi_tag)
    GameKind.Queens -> stringResource(Res.string.favorite_queens_tag)
    GameKind.Tents -> stringResource(Res.string.favorite_tents_tag)
    GameKind.Jigsaw -> stringResource(Res.string.favorite_jigsaw_tag)
    GameKind.BlockOut -> stringResource(Res.string.favorite_blockout_tag)
    GameKind.Arrows -> stringResource(Res.string.favorite_arrows_tag)
    GameKind.Nonogram -> stringResource(Res.string.favorite_nonogram_tag)
    GameKind.AxisSum -> stringResource(Res.string.favorite_axissum_tag)
    GameKind.BallSlide -> stringResource(Res.string.favorite_ballslide_tag)
    GameKind.ColorFlow -> stringResource(Res.string.favorite_colorflow_tag)
    GameKind.TopicGroups -> stringResource(Res.string.favorite_topicgroups_tag)
    GameKind.Matchsticks -> stringResource(Res.string.favorite_matchsticks_tag)
    GameKind.Lasers -> stringResource(Res.string.favorite_lasers_tag)
    GameKind.Untangle -> stringResource(Res.string.favorite_untangle_tag)
    GameKind.FruitMath -> stringResource(Res.string.favorite_fruitmath_tag)
    GameKind.CodeBreaker -> stringResource(Res.string.favorite_codebreaker_tag)
    GameKind.Rectangles -> stringResource(Res.string.favorite_rectangles_tag)
    GameKind.FlashGrid -> stringResource(Res.string.favorite_flashgrid_tag)
    GameKind.ColorSeq -> stringResource(Res.string.favorite_colorseq_tag)
    GameKind.CubeFold -> stringResource(Res.string.favorite_cubefold_tag)
    GameKind.LoneNumber -> stringResource(Res.string.favorite_lonenumber_tag)
    GameKind.ZerosOnes -> stringResource(Res.string.favorite_zerosones_tag)
    GameKind.Lamps -> stringResource(Res.string.favorite_lamps_tag)
    GameKind.GreaterLess -> stringResource(Res.string.favorite_greaterless_tag)
    GameKind.Fences -> stringResource(Res.string.favorite_fences_tag)
    GameKind.Mirror -> stringResource(Res.string.favorite_mirror_tag)
    GameKind.OddOne -> stringResource(Res.string.favorite_oddone_tag)
    GameKind.CrossSum -> stringResource(Res.string.favorite_crosssum_tag)
    GameKind.Line -> stringResource(Res.string.favorite_line_tag)
}
