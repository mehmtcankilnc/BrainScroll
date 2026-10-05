package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import brainscroll.shared.generated.resources.notice_finish_first
import brainscroll.shared.generated.resources.notice_not_in_list
import brainscroll.shared.generated.resources.notice_skipped
import brainscroll.shared.generated.resources.notice_too_short
import com.mehmtcan.brainscroll.game.wordle.FeedSnapshot
import com.mehmtcan.brainscroll.game.wordle.GuessError
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.components.NoticePill
import com.mehmtcan.brainscroll.ui.wordle.WordlePage
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val NOTICE_MILLIS = 1500L
private val NOTICE_TOP_OFFSET = 44.dp

/** A short message under the HUD. [id] makes two identical messages in a row count as different. */
private data class Notice(val text: StringResource, val id: Int)

@Composable
fun FeedScreen(viewModel: FeedViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            FeedUiState.Loading -> Unit
            is FeedUiState.Ready -> FeedContent(s.feed, s.favoriteIds, viewModel)
        }
    }
}

@Composable
private fun FeedContent(feed: FeedSnapshot, favoriteIds: Set<String>, viewModel: FeedViewModel) {
    // Start on the page the player was on (it matters when coming back from another tab).
    val startPage = viewModel.page.coerceIn(0, feed.rounds.size - 1)
    val pagerState = rememberPagerState(initialPage = startPage) { feed.rounds.size }
    var notice by remember { mutableStateOf<Notice?>(null) }
    var noticeCounter by remember { mutableIntStateOf(0) }
    fun show(text: StringResource) {
        notice = Notice(text, noticeCounter++)
    }

    // The effect below lives across recompositions, so it must read the newest snapshot, not the first one.
    val latestFeed by rememberUpdatedState(feed)

    // Leaving a puzzle forward: allowed if it is finished, already skipped, or the one skip is still available.
    LaunchedEffect(pagerState) {
        var last = pagerState.settledPage
        viewModel.onPageSettled(last)
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page > last) {
                val lastRound = latestFeed.rounds[last]
                val wasUnfinished = !lastRound.isFinished && !lastRound.skipped
                if (viewModel.leave(last)) {
                    if (wasUnfinished) show(Res.string.notice_skipped)
                    last = page
                    viewModel.onPageSettled(page)
                } else {
                    show(Res.string.notice_finish_first)
                    pagerState.animateScrollToPage(last)
                }
            } else {
                last = page
                viewModel.onPageSettled(page)
            }
        }
    }

    val page = pagerState.currentPage
    val current = feed.rounds[page]

    // Rejected guesses show a message. The tick makes the same error show again.
    LaunchedEffect(current.index, current.errorTick) {
        if (current.errorTick == 0) return@LaunchedEffect
        when (current.error) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
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
            answerStreak = feed.answerStreak,
            skipsLeft = feed.skipsLeft,
            language = feed.language,
            // Locked once the feed has been started (first letter typed or first skip).
            onToggleLanguage = if (feed.canChangeLanguage) {
                {
                    val next = if (feed.language == Language.EN) Language.TR else Language.EN
                    viewModel.setLanguage(next)
                }
            } else null,
        )
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            // Forward is blocked up front while this puzzle is unfinished and no skip is left.
            val leaveBlocked = !(current.isFinished || current.skipped || feed.skipsLeft > 0)
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .blockForwardScroll(blocked = leaveBlocked) { show(Res.string.notice_finish_first) },
                beyondViewportPageCount = 1,
                key = { it },
            ) { index ->
                WordlePage(
                    round = feed.rounds[index],
                    onLetter = { viewModel.type(index, it) },
                    onBackspace = { viewModel.backspace(index) },
                    onEnter = { viewModel.submit(index) },
                    isFavorite = feed.rounds[index].id in favoriteIds,
                    onToggleFavorite = { viewModel.toggleFavorite(feed.rounds[index].id) },
                )
            }
            // Sits under the page title so it never covers it.
            notice?.let { NoticePill(stringResource(it.text), Modifier.align(Alignment.TopCenter).padding(top = NOTICE_TOP_OFFSET)) }
        }
    }
}
