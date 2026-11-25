package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import com.mehmtcan.brainscroll.ui.sound.LocalSoundEffects
import com.mehmtcan.brainscroll.ui.sound.rememberSoundHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalWinSignal
import com.mehmtcan.brainscroll.ui.motion.WinSignal
import com.mehmtcan.brainscroll.ui.motion.rememberWinPulse
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.ui.theme.worldGlow
import com.mehmtcan.brainscroll.ui.theme.nextWorldPeek
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import com.mehmtcan.brainscroll.ui.theme.Spacing
import brainscroll.shared.generated.resources.guide_needs_connection
import brainscroll.shared.generated.resources.guide_none
import brainscroll.shared.generated.resources.feed_puzzle_number
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_word_language
import brainscroll.shared.generated.resources.notice_hard_mode
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.home.nativeName
import com.mehmtcan.brainscroll.ui.wordle.LanguageSwitch
import brainscroll.shared.generated.resources.notice_not_in_list
import brainscroll.shared.generated.resources.notice_too_short
import com.mehmtcan.brainscroll.game.wordle.FeedSnapshot
import com.mehmtcan.brainscroll.game.wordle.GuessError
import com.mehmtcan.brainscroll.ui.components.NoticePill
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.memory.MemorySnapshot
import com.mehmtcan.brainscroll.game.wordle.RoundSnapshot
import com.mehmtcan.brainscroll.ui.memory.MemoryPage
import com.mehmtcan.brainscroll.ui.howto.HowToPlaySheet
import com.mehmtcan.brainscroll.ui.motion.CelebrateWin
import com.mehmtcan.brainscroll.ui.home.difficultyName
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
            FeedUiState.Loading -> GeneratingPage(kind = null)
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
    // The pages say when their win is on show; the confetti waits for it.
    val winSignal = remember { WinSignal() }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val bestFlow = maxOf(profile.bestFlow, feed.solvedCount)
    val quota by viewModel.quota.collectAsStateWithLifecycle()
    // A new day gives the free guide steps back, also when the app stayed open over midnight.
    LaunchedEffect(pagerState.currentPage) { viewModel.refreshQuota() }
    var noticeCounter by remember { mutableIntStateOf(0) }
    fun show(text: StringResource) {
        notice = Notice(text, noticeCounter++)
    }
    // The flag of the word puzzles: the language they come in. A tap switches to the other language.
    val wordLanguage = feed.wordLanguages.first()
    val otherLanguage = Language.entries.first { it != wordLanguage }
    val flagDescription = stringResource(Res.string.a11y_word_language, wordLanguage.nativeName(), otherLanguage.nativeName())
    val languageSwitch = remember(wordLanguage, flagDescription) {
        LanguageSwitch(wordLanguage, flagDescription) { viewModel.setWordLanguage(otherLanguage) }
    }

    // The clock of the page that is on show runs while the feed is on the screen and the app is in front.
    LifecycleResumeEffect(viewModel) {
        viewModel.onFeedResumed()
        onPauseOrDispose { viewModel.onFeedPaused() }
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
    val guide = guideState?.takeIf { it.page.withoutClock() == current.withoutClock() }?.step
    LaunchedEffect(viewModel) { viewModel.guideNothing.collect { show(Res.string.guide_none) } }
    LaunchedEffect(viewModel) { viewModel.guideNeedsConnection.collect { show(Res.string.guide_needs_connection) } }
    val currentWord = current as? RoundSnapshot

    // Rejected guesses show a message. The tick makes the same error show again. (Only word puzzles have guesses.)
    val haptics = rememberSoundHaptics(LocalHaptics.current, LocalSoundEffects.current)
    LaunchedEffect(currentWord?.index, currentWord?.errorTick) {
        val word = currentWord ?: return@LaunchedEffect
        if (word.errorTick == 0) return@LaunchedEffect
        haptics.perform(HapticKind.Warning)
        when (word.error) {
            GuessError.WrongLength -> show(Res.string.notice_too_short)
            GuessError.NotInDictionary, GuessError.InvalidLetters -> show(Res.string.notice_not_in_list)
            GuessError.HardMode -> show(Res.string.notice_hard_mode)
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
    // The glow of the game on show: it changes color as the feed moves from one game to the next.
    val world by animateColorAsState(
        targetValue = current.kind.world,
        animationSpec = if (LocalReduceMotion.current) snap() else tween(500),
        label = "world",
    )
    // Once this puzzle is done, the light of the next game comes up from the bottom edge: a hint of what the next swipe brings.
    val nextKind = feed.pages.getOrNull(page + 1)?.kind
    val peekWorld by animateColorAsState(
        targetValue = (nextKind ?: current.kind).world,
        animationSpec = if (LocalReduceMotion.current) snap() else tween(300),
        label = "peekWorld",
    )
    val peekAlpha by animateFloatAsState(
        targetValue = if (current.isFinished && nextKind != null) 1f else 0f,
        animationSpec = if (LocalReduceMotion.current) snap() else tween(700),
        label = "peekAlpha",
    )
    Box(Modifier.fillMaxSize().worldGlow(world).nextWorldPeek(peekWorld) { peekAlpha }) {
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
                badge = listOfNotNull(
                    feed.difficulties.getOrNull(page)?.takeIf { it != Difficulty.Medium && current.kind in viewModel.levelledGames }?.let { difficultyName(it) },
                ).joinToString(" · ").ifEmpty { null },
                solvedCount = feed.solvedCount,
                onHome = onHome,
                onHowToPlay = { howTo = current.kind },
                onStreak = { streakInfo = true },
                onGuide = if (current.isFinished) null else ({ viewModel.requestGuide(page) }),
                quota = quota,
                position = stringResource(Res.string.feed_puzzle_number, page + 1),
            )
            // Is a finger down on the feed? Watched without taking part, so the lock after a winning swipe ends when it is lifted.
            var fingerDown by remember { mutableStateOf(false) }
            LaunchedEffect(winSignal.scrollLocked, fingerDown) {
                if (winSignal.scrollLocked && !fingerDown) winSignal.scrollLocked = false
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxSize().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            fingerDown = event.changes.any { it.pressed }
                        }
                    }
                },
            ) {
                VerticalPager(
                    state = pagerState,
                    userScrollEnabled = !winSignal.scrollLocked,
                    modifier = Modifier
                        .fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { it },
                ) { index ->
                    CompositionLocalProvider(LocalWinSignal provides winSignal) {
                        val item = feed.pages[index]
                        val pulse = rememberWinPulse(item.id, item.isFinished, winSignal)
                        Box(
                            Modifier.fillMaxSize().graphicsLayer {
                                scaleX = pulse
                                scaleY = pulse
                                // The swell is around the board, where the burst starts.
                                transformOrigin = TransformOrigin(0.5f, 0.42f)
                            },
                        ) {
                            GamePage(item, index, viewModel, favoriteIds, guide = guide.takeIf { index == page }, languageSwitch = languageSwitch)
                        }
                    }
                }
                notice?.let { NoticePill(stringResource(it.text), Modifier.align(Alignment.TopCenter).padding(top = NOTICE_TOP_OFFSET), onDismiss = { notice = null }) }
            }
        }
        // What the guide says takes the place of the top bar for as long as it is shown. It is over the bar and not in the
        // layout, so the board keeps its size and its place, and it is never over the board (or the thing it points at).
        run {
            // Keeps the last step while the sentence fades away.
            val shown = remember { mutableStateOf(guide) }
            if (guide != null) shown.value = guide
            val alpha by animateFloatAsState(if (guide != null) 1f else 0f)
            if (alpha > 0f) {
                shown.value?.let { step ->
                    GuideBanner(
                        step = step,
                        onClose = viewModel::clearGuide,
                        modifier = Modifier.align(Alignment.TopCenter).graphicsLayer { this.alpha = alpha },
                        onRestart = restartOf(viewModel, current.kind)?.let { restart -> { restart(page) } },
                    )
                }
            }
        }
        CelebrateWin(current, winSignal)
        howTo?.let { HowToPlaySheet(it, onClose = { howTo = null }, wordLanguage = currentWord?.language ?: feed.wordLanguages.first()) }
        if (streakInfo) StreakInfoSheet(StreakKind.Win, current = feed.solvedCount, best = bestFlow, onClose = { streakInfo = false })
    }
}

/** How a puzzle goes back to its start, for the games where the guide can find it has no way left. */
private fun restartOf(actions: FeedActions, kind: GameKind): ((Int) -> Unit)? = when (kind) {
    GameKind.Tubes -> { page -> actions.restart(page) }
    GameKind.Target -> { page -> actions.restartNumbers(page) }
    else -> null
}
