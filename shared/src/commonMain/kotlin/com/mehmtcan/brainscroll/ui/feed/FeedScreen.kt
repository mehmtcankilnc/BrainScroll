package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.howto.HintSheet
import com.mehmtcan.brainscroll.ui.components.AppOutlinedButton
import brainscroll.shared.generated.resources.hint_button
import brainscroll.shared.generated.resources.guide_none
import brainscroll.shared.generated.resources.guide_button
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.notice_not_in_list
import brainscroll.shared.generated.resources.notice_too_short
import com.mehmtcan.brainscroll.game.wordle.FeedSnapshot
import com.mehmtcan.brainscroll.game.wordle.GuessError
import com.mehmtcan.brainscroll.ui.components.NoticePill
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.memory.MemorySnapshot
import com.mehmtcan.brainscroll.game.wordle.RoundSnapshot
import com.mehmtcan.brainscroll.ui.memory.MemoryPage
import com.mehmtcan.brainscroll.ui.howto.HowToPlaySheet
import com.mehmtcan.brainscroll.ui.motion.CelebrateWin
import com.mehmtcan.brainscroll.ui.home.displayName
import com.mehmtcan.brainscroll.ui.streak.StreakInfoSheet
import com.mehmtcan.brainscroll.ui.streak.StreakKind
import com.mehmtcan.brainscroll.ui.tubes.TubesPage
import com.mehmtcan.brainscroll.ui.mines.MinesPage
import com.mehmtcan.brainscroll.game.mines.MinesSnapshot
import com.mehmtcan.brainscroll.game.lights.LightsSnapshot
import com.mehmtcan.brainscroll.game.pipes.PipesSnapshot
import com.mehmtcan.brainscroll.game.slide.SlideSnapshot
import com.mehmtcan.brainscroll.ui.pipes.PipesPage
import com.mehmtcan.brainscroll.game.sudoku.SudokuSnapshot
import com.mehmtcan.brainscroll.game.target.TargetSnapshot
import com.mehmtcan.brainscroll.ui.target.TargetPage
import com.mehmtcan.brainscroll.ui.slide.SlidePage
import com.mehmtcan.brainscroll.ui.lights.LightsPage
import com.mehmtcan.brainscroll.ui.sudoku.SudokuPage
import com.mehmtcan.brainscroll.game.tubes.TubesSnapshot
import com.mehmtcan.brainscroll.ui.wordle.WordlePage
import kotlinx.coroutines.delay
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val NOTICE_MILLIS = 1500L
private val NOTICE_TOP_OFFSET = 44.dp

/** A short message under the HUD. [id] makes two identical messages in a row count as different. */
private data class Notice(val text: StringResource, val id: Int)

@Composable
fun FeedScreen(viewModel: FeedViewModel, onHome: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            FeedUiState.Loading -> Unit
            is FeedUiState.Ready -> FeedContent(s.feed, s.favoriteIds, viewModel, onHome)
        }
    }
}

@Composable
private fun FeedContent(feed: FeedSnapshot, favoriteIds: Set<String>, viewModel: FeedViewModel, onHome: () -> Unit) {
    // Start on the page the player was on (it matters when coming back from another tab).
    val startPage = viewModel.page.coerceIn(0, feed.pages.size - 1)
    val pagerState = rememberPagerState(initialPage = startPage) { feed.pages.size }
    var notice by remember { mutableStateOf<Notice?>(null) }
    // The game whose rules are on show ("How to play?"), if any.
    var howTo by remember { mutableStateOf<GameKind?>(null) }
    var streakInfo by remember { mutableStateOf(false) }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val bestWinStreak = maxOf(profile.stats.bestStreak, feed.answerStreak)
    var noticeCounter by remember { mutableIntStateOf(0) }
    fun show(text: StringResource) {
        notice = Notice(text, noticeCounter++)
    }

    // The feed scrolls freely: a puzzle that is left unfinished is simply saved where it is.
    LaunchedEffect(pagerState) {
        viewModel.onPageSettled(pagerState.settledPage)
        snapshotFlow { pagerState.settledPage }.collect { viewModel.onPageSettled(it) }
    }

    val page = pagerState.currentPage
    val current = feed.pages[page]
    // The step of the guide counts while the page looks as it did when it was given; any move makes it old.
    val guideState by viewModel.guide.collectAsStateWithLifecycle()
    val guide = guideState?.takeIf { it.page == current }?.step
    var hintFor by remember { mutableStateOf<GameKind?>(null) }
    LaunchedEffect(viewModel) { viewModel.guideNothing.collect { show(Res.string.guide_none) } }
    val currentWord = current as? RoundSnapshot

    // Rejected guesses show a message. The tick makes the same error show again. (Only word puzzles have guesses.)
    val haptics = LocalHaptics.current
    LaunchedEffect(currentWord?.index, currentWord?.errorTick) {
        val word = currentWord ?: return@LaunchedEffect
        if (word.errorTick == 0) return@LaunchedEffect
        haptics.perform(HapticKind.Warning)
        when (word.error) {
            GuessError.WrongLength -> show(Res.string.notice_too_short)
            GuessError.NotInDictionary, GuessError.InvalidLetters -> show(Res.string.notice_not_in_list)
            else -> Unit
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MILLIS)
            notice = null
        }
    }

    // A physical keyboard (desktop) works too. It always acts on the page that is currently shown.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // The rules panel goes over the whole screen, the bar at the top included, so the scrim darkens and blocks all of it.
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    // With the rules on show, typing must not reach the puzzle below.
                    if (howTo != null || event.type != KeyEventType.KeyDown || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
                        return@onPreviewKeyEvent false
                    }
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> { viewModel.submit(pagerState.currentPage); true }
                        Key.Backspace -> { viewModel.backspace(pagerState.currentPage); true }
                        else -> {
                            val ch = event.utf16CodePoint.toChar()
                            if (ch.isLetter()) { viewModel.type(pagerState.currentPage, ch); true } else false
                        }
                    }
                },
        ) {
            Hud(
                title = current.kind.displayName(),
                badge = if (currentWord != null && feed.wordLanguages.size > 1) currentWord.language.name else null,
                answerStreak = feed.answerStreak,
                onHome = onHome,
                onHowToPlay = { howTo = current.kind },
                onStreak = { streakInfo = true },
            )
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { it },
                ) { index ->
                    GamePage(feed.pages[index], index, viewModel, favoriteIds, guide = guide.takeIf { index == page })
                }
                // Sits under the page title so it never covers it.
                guide?.let { GuideBanner(it, Modifier.align(Alignment.TopCenter).padding(top = Spacing.xs)) }
                notice?.let { NoticePill(stringResource(it.text), Modifier.align(Alignment.TopCenter).padding(top = NOTICE_TOP_OFFSET)) }
            }
            // Help for a puzzle that is not over: a general tip (free) and the guide (the puzzle becomes neutral).
            if (!current.isFinished) {
                HelpRow(onHint = { hintFor = current.kind }, onGuide = { viewModel.requestGuide(page) })
            }
        }
        CelebrateWin(current)
        hintFor?.let { HintSheet(it, onClose = { hintFor = null }) }
        howTo?.let { HowToPlaySheet(it, onClose = { howTo = null }) }
        if (streakInfo) StreakInfoSheet(StreakKind.Win, current = feed.answerStreak, best = bestWinStreak, onClose = { streakInfo = false })
    }
}

/** Two quiet buttons under the puzzle. The guide is the one that marks the puzzle, so it says what it does when used. */
@Composable
private fun HelpRow(onHint: () -> Unit, onGuide: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        AppOutlinedButton(stringResource(Res.string.hint_button), onHint, Modifier.weight(1f).testTag("hintButton"))
        AppOutlinedButton(stringResource(Res.string.guide_button), onGuide, Modifier.weight(1f).testTag("guideButton"))
    }
}
