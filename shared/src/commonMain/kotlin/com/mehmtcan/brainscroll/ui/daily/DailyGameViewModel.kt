package com.mehmtcan.brainscroll.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmtcan.brainscroll.daily.DailyApi
import com.mehmtcan.brainscroll.daily.DailyException
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.daily.dailyGameId
import com.mehmtcan.brainscroll.game.hanoi.HanoiResult
import com.mehmtcan.brainscroll.game.queens.QueensResult
import com.mehmtcan.brainscroll.game.crossword.CrosswordPuzzle
import com.mehmtcan.brainscroll.game.crossword.CrosswordResult
import com.mehmtcan.brainscroll.game.crossword.CrosswordRound
import com.mehmtcan.brainscroll.game.crossword.CrosswordSchedule
import com.mehmtcan.brainscroll.game.crossword.CrosswordScoring
import com.mehmtcan.brainscroll.game.crossword.CrosswordSnapshot
import com.mehmtcan.brainscroll.game.blockout.BlockOutResult
import com.mehmtcan.brainscroll.game.tents.TentsResult
import com.mehmtcan.brainscroll.game.lights.LightsResult
import com.mehmtcan.brainscroll.game.line.LineResult
import com.mehmtcan.brainscroll.game.memory.FlipResult
import com.mehmtcan.brainscroll.game.mines.MinesResult
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.pipes.PipesResult
import com.mehmtcan.brainscroll.game.slide.SlideResult
import com.mehmtcan.brainscroll.game.sudoku.SudokuResult
import com.mehmtcan.brainscroll.game.target.Op
import com.mehmtcan.brainscroll.game.target.TargetResult
import com.mehmtcan.brainscroll.game.tubes.PourResult
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FeedStore
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.stats.DayStreak
import com.mehmtcan.brainscroll.stats.DayStreaks
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.feed.FeedActions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Where the daily puzzle of a game other than the word puzzle stands. */
enum class DailyGamePhase {
    /** Not started, or started and not finished. The board is on screen. */
    Playing,

    /** Done today. */
    Finished,
}

/**
 * What the daily screen and the home card show for the daily puzzle when today's game is not the word puzzle.
 * [page] is the live board, null when the puzzle was finished before the app was started this time: then only the
 * [result] is known, a finished board is not kept.
 */
data class DailyGameUi(
    val day: Long,
    val game: GameKind,
    val phase: DailyGamePhase,
    val page: PageSnapshot?,
    val result: FinishedRound?,
    val dayStreak: DayStreak,
    val favoriteIds: Set<String>,
)

/**
 * The daily puzzle of the days when the game is not the word puzzle ([gameOf] says which game a day has). The board is
 * built from the day number, so everyone gets the same one, and the time is measured here; nothing is secret, so no
 * server is needed to play, even offline. When a puzzle ends, its result is kept on the device, counts for the day
 * streak, and is sent to the server for the speed tables as soon as there is a connection.
 *
 * It runs the puzzle on a small [EndlessFeed] of one page, so every game plays exactly as in the feed, and it
 * implements [FeedActions] so the same screens of the games are used.
 */
class DailyGameViewModel(
    private val repository: GameRepository,
    private val api: DailyApi,
    /** True while there is an account session: without one the result stays on the device. */
    private val signedIn: StateFlow<Boolean>,
    private val now: () -> Long,
    private val gameOf: (Long) -> GameKind,
    /** The language the results are saved with. The game itself has none. */
    private val language: () -> Language,
    private val onLocalChange: () -> Unit = {},
) : ViewModel(), FeedActions {

    private var feed: EndlessFeed? = null
    private var feedId: String? = null

    /** The finished crossword of today, drawn again for a puzzle that was solved before the app was opened this time. */
    private var solved: CrosswordSnapshot? = null
    private val handled = mutableSetOf<String>()
    private val sending = mutableSetOf<String>()

    private val _ui = MutableStateFlow(snapshot())
    val ui: StateFlow<DailyGameUi> = _ui

    init {
        refresh()
        viewModelScope.launch { signedIn.collect { if (it) sendPending() } }
    }

    /** Looks again: a new day brings a new game and a new board. Cheap, called whenever a screen is shown. */
    fun refresh() {
        val day = IstanbulDay.dayIndex(now())
        val game = gameOf(day)
        val id = dailyGameId(day, game)
        if (game != GameKind.Word && id != feedId) {
            feedId = id
            solved = null
            feed = null
            val result = repository.history().firstOrNull { it.id == id }
            if (result != null) {
                if (game == GameKind.Crossword) showSolvedCrossword(id, result)
            } else {
                feed = buildFeed(day, game, id)
            }
        }
        publish()
        sendPending()
    }

    /** Draws today's crossword again, filled in, from the board that is named in the result. */
    private fun showSolvedCrossword(id: String, result: FinishedRound) {
        val puzzle = CrosswordPuzzle.decode(result.language, result.answer) ?: return
        val round = CrosswordRound(0, id, puzzle)
        val state = round.snapshot().state
        for (cell in state.isClue.indices) if (!state.isClue[cell]) round.place(cell, puzzle.squares[cell])
        val snapshot = round.snapshot()
        solved = snapshot.copy(
            durationMs = result.durationMs ?: 0L,
            state = snapshot.state.copy(mistakes = CrosswordScoring.mistakesOf(result.guesses.firstOrNull()?.toIntOrNull() ?: 1)),
        )
    }

    private fun buildFeed(day: Long, game: GameKind, id: String): EndlessFeed {
        val stored = repository.unfinishedDailyRound(id)
        val store = object : FeedStore {
            override fun saveProgress(round: StoredRound) = repository.saveProgress(round)
            override fun deleteProgress(id: String) = repository.deleteProgress(id)
            override fun finish(result: FinishedRound) {
                // A daily crossword keeps the score code (1 + the mistakes) as its only guess: that is what the server and the stars use.
                val guesses = if (game == GameKind.Crossword) {
                    listOf(CrosswordScoring.encode(result.guesses.getOrNull(1)?.toIntOrNull() ?: 0).toString())
                } else {
                    result.guesses
                }
                repository.finish(result.copy(mode = Mode.DAILY, puzzleDay = day, guesses = guesses))
            }
        }
        return EndlessFeed(
            wordLists = emptyMap(),
            startLanguage = language(),
            // The same board for everyone: the first (and only) one drawn from this day's numbers.
            random = Random(day * 1_000_003L + game.ordinal),
            store = store,
            restored = listOfNotNull(stored),
            now = now,
            newId = { id },
            games = listOf(game),
            crosswordBoard = { _, boardLanguage -> CrosswordSchedule.puzzleFor(day, boardLanguage) },
        ).also { it.ensureSize(1) }
    }

    // --- The result goes to the server ---

    /** Sends today's result if it is not sent yet and there is a session. Quiet when it cannot. */
    private fun sendPending() {
        val id = feedId ?: return
        if (!signedIn.value || repository.dailyResultSent(id)) return
        val result = repository.history().firstOrNull { it.id == id } ?: return
        val duration = result.durationMs ?: return
        // Two looks at the same moment must not send it twice.
        if (!sending.add(id)) return
        viewModelScope.launch {
            try {
                api.submitGameResult(result.game, result.outcome, result.moveCount.coerceAtLeast(1), duration, result.language)
                repository.markDailyResultSent(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DailyException) {
                // The server finds the result implausible: asking again would not change that.
                if (e is DailyException.InvalidGuess) repository.markDailyResultSent(id)
            } finally {
                sending.remove(id)
            }
        }
    }

    // --- Playing (FeedActions) ---

    /** Runs a move on the board and tells the screen. A puzzle that just ended is counted once. */
    private fun <T> act(default: T, move: (EndlessFeed) -> T): T {
        val f = feed ?: return default
        val result = move(f)
        val page = f.snapshot().pages.firstOrNull()
        val id = feedId
        if (page != null && page.isFinished && id != null && handled.add(id)) {
            publish()
            onLocalChange()
            sendPending()
        } else {
            publish()
        }
        return result
    }

    // The word puzzle has its own screen and flow; its actions do nothing here.
    override fun type(page: Int, letter: Char) = Unit
    override fun backspace(page: Int) = Unit
    override fun submit(page: Int) = Unit

    override fun flip(page: Int, card: Int): FlipResult = act(FlipResult.Ignored) { it.flip(0, card) }
    override fun closeMismatch(page: Int) = act(Unit) { it.closeMismatch(0) }
    override fun pour(page: Int, from: Int, to: Int): PourResult = act(PourResult.Ignored) { it.pour(0, from, to) }
    override fun undo(page: Int): Boolean = act(false) { it.undo(0) }
    override fun restart(page: Int): Boolean = act(false) { it.restart(0) }
    override fun openCell(page: Int, cell: Int): MinesResult = act(MinesResult.Ignored) { it.openCell(0, cell) }
    override fun flagCell(page: Int, cell: Int): Boolean = act(false) { it.flagCell(0, cell) }
    override fun chordCell(page: Int, cell: Int): MinesResult = act(MinesResult.Ignored) { it.chordCell(0, cell) }
    override fun tapLight(page: Int, cell: Int): LightsResult = act(LightsResult.Ignored) { it.tapLight(0, cell) }
    override fun restartLights(page: Int): Boolean = act(false) { it.restartLights(0) }
    override fun slideTile(page: Int, cell: Int): SlideResult = act(SlideResult.Ignored) { it.slideTile(0, cell) }
    override fun restartSlide(page: Int): Boolean = act(false) { it.restartSlide(0) }
    override fun combineNumbers(page: Int, firstId: Int, op: Op, secondId: Int): TargetResult =
        act(TargetResult.Ignored) { it.combineNumbers(0, firstId, op, secondId) }
    override fun previewNumbers(page: Int, firstId: Int, op: Op, secondId: Int): Int? = feed?.previewNumbers(0, firstId, op, secondId)
    override fun undoNumbers(page: Int): Boolean = act(false) { it.undoNumbers(0) }
    override fun restartNumbers(page: Int): Boolean = act(false) { it.restartNumbers(0) }
    override fun turnPipe(page: Int, cell: Int): PipesResult = act(PipesResult.Ignored) { it.turnPipe(0, cell) }
    override fun restartPipes(page: Int): Boolean = act(false) { it.restartPipes(0) }
    override fun moveDisk(page: Int, from: Int, to: Int): HanoiResult = act(HanoiResult.Ignored) { it.moveDisk(0, from, to) }
    override fun restartHanoi(page: Int): Boolean = act(false) { it.restartHanoi(0) }
    override fun toggleQueen(page: Int, cell: Int): QueensResult = act(QueensResult.Ignored) { it.toggleQueen(0, cell) }
    override fun restartQueens(page: Int): Boolean = act(false) { it.restartQueens(0) }
    override fun tapTent(page: Int, cell: Int): TentsResult = act(TentsResult.Ignored) { it.tapTent(0, cell) }
    override fun restartTents(page: Int): Boolean = act(false) { it.restartTents(0) }
    override fun slideBlock(page: Int, piece: Int, to: Int): BlockOutResult = act(BlockOutResult.Ignored) { it.slideBlock(0, piece, to) }
    override fun restartBlockOut(page: Int): Boolean = act(false) { it.restartBlockOut(0) }
    override fun placeLetter(page: Int, cell: Int, letter: Char): CrosswordResult = act(CrosswordResult.Ignored) { it.placeLetter(0, cell, letter) }
    override fun eraseLetter(page: Int, cell: Int): CrosswordResult = act(CrosswordResult.Ignored) { it.eraseLetter(0, cell) }
    override fun dragLine(page: Int, cell: Int): LineResult = act(LineResult.Ignored) { it.dragLine(0, cell) }
    override fun tapLine(page: Int, cell: Int): LineResult = act(LineResult.Ignored) { it.tapLine(0, cell) }
    override fun undoLine(page: Int): Boolean = act(false) { it.undoLine(0) }
    override fun restartLine(page: Int): Boolean = act(false) { it.restartLine(0) }
    override fun hintLine(page: Int): List<Int>? = act(null) { it.hintLine(0) }
    override fun placeDigit(page: Int, cell: Int, digit: Int): SudokuResult = act(SudokuResult.Ignored) { it.placeDigit(0, cell, digit) }
    override fun toggleNote(page: Int, cell: Int, digit: Int): SudokuResult = act(SudokuResult.Ignored) { it.toggleNote(0, cell, digit) }
    override fun eraseCell(page: Int, cell: Int): SudokuResult = act(SudokuResult.Ignored) { it.eraseCell(0, cell) }

    /** The heart on the result of today's puzzle. */
    override fun toggleFavorite(resultId: String) {
        repository.setFavorite(resultId, !repository.isFavorite(resultId), now())
        publish()
        onLocalChange()
    }

    private fun publish() {
        _ui.value = snapshot()
    }

    private fun snapshot(): DailyGameUi {
        val today = IstanbulDay.dayIndex(now())
        val game = gameOf(today)
        val id = dailyGameId(today, game)
        val history = repository.history()
        val result = history.firstOrNull { it.id == id }
        val page = feed?.snapshot()?.pages?.firstOrNull() ?: solved?.takeIf { result != null }
        return DailyGameUi(
            day = today,
            game = game,
            phase = if (result != null || page?.isFinished == true) DailyGamePhase.Finished else DailyGamePhase.Playing,
            page = page,
            result = result,
            dayStreak = DayStreaks.compute(history.filter { it.mode == Mode.DAILY }.map { it.dayIndex }, today),
            favoriteIds = repository.favoriteResults().map { it.id }.toSet(),
        )
    }
}
