package com.mehmtcan.brainscroll.ui.line

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.back
import brainscroll.shared.generated.resources.favorites_replay
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.line.LineResult
import com.mehmtcan.brainscroll.game.line.LineSnapshot
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FeedStore
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.ui.components.ChevronButton
import com.mehmtcan.brainscroll.ui.feed.LocalDailyPage
import com.mehmtcan.brainscroll.ui.feed.LocalReplayPage
import com.mehmtcan.brainscroll.ui.home.displayName
import com.mehmtcan.brainscroll.ui.motion.CelebrateWin
import com.mehmtcan.brainscroll.ui.motion.LocalWinSignal
import com.mehmtcan.brainscroll.ui.motion.WinSignal
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.textSoft
import kotlin.random.Random
import org.jetbrains.compose.resources.stringResource

/**
 * One Line played again from the favorites. The line game is not in the feed (it is the daily puzzle), so a favorite of it is not
 * replayed there: this holds one page of that very board, on a screen of its own. The board is the favorite's, the level too; the
 * moves are not saved while it is played (leaving it forgets it) and its result is a result of its own, like any practice game.
 */
class LineReplay(
    private val repository: GameRepository,
    private val now: () -> Long,
    /** Called when the board was solved: the profile and the cloud look again. */
    private val onFinished: () -> Unit = {},
) {
    private var feed: EndlessFeed? = null

    /** The board on show, null before [start]. */
    var page by mutableStateOf<LineSnapshot?>(null)
        private set

    var favoriteIds by mutableStateOf(emptySet<String>())
        private set

    /** Sets the favorite's board up again, untouched, with a fresh id. */
    fun start(round: FinishedRound) {
        val copy = StoredRound(
            id = Random.nextLong().toULong().toString(16),
            language = round.language,
            answer = round.answer,
            guesses = emptyList(),
            input = "",
            position = 0,
            game = GameKind.Line,
            difficulty = round.difficulty,
        )
        val store = object : FeedStore {
            override fun saveProgress(round: StoredRound) = Unit
            override fun deleteProgress(id: String) = Unit
            override fun finish(result: FinishedRound) {
                repository.finish(result)
                onFinished()
            }
        }
        feed = EndlessFeed(
            wordLists = emptyMap(),
            startLanguage = round.language,
            store = store,
            restored = listOf(copy),
            now = now,
            games = listOf(GameKind.Line),
        ).also { it.ensureSize(1) }
        publish()
    }

    private fun publish() {
        page = feed?.snapshot()?.pages?.firstOrNull() as? LineSnapshot
        favoriteIds = repository.favoriteResults().map { it.id }.toSet()
    }

    fun drag(cell: Int): LineResult = (feed?.dragLine(0, cell) ?: LineResult.Ignored).also { publish() }
    fun tap(cell: Int): LineResult = (feed?.tapLine(0, cell) ?: LineResult.Ignored).also { publish() }
    fun undo(): Boolean = (feed?.undoLine(0) ?: false).also { publish() }
    fun restart(): Boolean = (feed?.restartLine(0) ?: false).also { publish() }
    fun hint(): List<Int>? = feed?.hintLine(0).also { publish() }

    fun toggleFavorite(resultId: String) {
        repository.setFavorite(resultId, !repository.isFavorite(resultId), now())
        publish()
        onFinished()
    }
}

/** The screen of a [LineReplay]: a bar with the way back, the name of the game and "Play again", and the board under it. */
@Composable
fun LineReplayScreen(replay: LineReplay, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    // The page says when its win is on show; the confetti waits for it.
    val winSignal = remember { WinSignal() }
    val page = replay.page
    Box(modifier.fillMaxSize().testTag("lineReplay")) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = Spacing.sm, end = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChevronButton(stringResource(Res.string.back), colors.textPrimary, onBack, touch = 48.dp)
                Column(Modifier.padding(start = Spacing.xs)) {
                    BasicText(
                        text = GameKind.Line.displayName(),
                        style = type.heading.copy(color = colors.textPrimary),
                        maxLines = 1,
                        modifier = Modifier.semantics { heading() },
                    )
                    BasicText(stringResource(Res.string.favorites_replay), style = type.caption.copy(color = colors.textSoft), maxLines = 1)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (page != null) {
                    // The board gives stars like the daily one, and no next puzzle is promised under it.
                    CompositionLocalProvider(LocalDailyPage provides true, LocalReplayPage provides true, LocalWinSignal provides winSignal) {
                        LinePage(
                            page = page,
                            onDrag = replay::drag,
                            onTap = replay::tap,
                            onUndo = replay::undo,
                            onRestart = replay::restart,
                            onHint = replay::hint,
                            isFavorite = page.id in replay.favoriteIds,
                            onToggleFavorite = { replay.toggleFavorite(page.id) },
                        )
                    }
                }
            }
        }
        page?.let { CelebrateWin(it, winSignal) }
    }
}
