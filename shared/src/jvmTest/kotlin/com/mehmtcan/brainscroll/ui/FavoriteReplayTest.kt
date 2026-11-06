package com.mehmtcan.brainscroll.ui

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.prepareDatabase
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.line.LineGenerator
import com.mehmtcan.brainscroll.game.line.LineRound
import com.mehmtcan.brainscroll.game.sudoku.SudokuRound
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.ui.feed.FeedUiState
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.line.LineReplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** "Play again" on a favorite: the very puzzle comes back, at its own level, and One Line does not go through the feed. */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteReplayTest {
    private val now = 1_800_000_000_000L

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun repository(): GameRepository {
        val file = File.createTempFile("replay-test", ".db").also { it.deleteOnExit() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { prepareDatabase(it) }
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun favorite(game: GameKind, answer: String, difficulty: Difficulty) = FinishedRound(
        id = "fav", mode = Mode.ENDLESS, language = Language.EN, answer = answer, guesses = listOf("1"),
        outcome = Outcome.WON, finishedAt = now, durationMs = 61_000L, game = game, difficulty = difficulty,
    )

    @Test
    fun aHardFavoriteIsPlayedAgainAsTheSameBoardAtTheHardLevel() = runTest {
        val sudoku = SudokuRound(0, "x", seed = 12345, language = Language.EN, now = { now }, clues = 30).toStored().answer
        val vm = FeedViewModel(
            repository = repository(),
            deviceLanguage = Language.EN,
            now = { now },
            cloudServices = TestCloud().factory,
            games = listOf(GameKind.Sudoku),
        ).also { advanceUntilIdle() }

        vm.playAgain(favorite(GameKind.Sudoku, sudoku, Difficulty.Hard))
        advanceUntilIdle()

        val feed = (vm.state.value as FeedUiState.Ready).feed
        assertEquals(GameKind.Sudoku, feed.pages.first().kind)
        assertEquals(Difficulty.Hard, feed.difficulties.first(), "it was a hard board: it is played, and counted, as one")
    }

    @Test
    fun oneLineIsPlayedAgainOnItsOwnAsTheSameBoardAndItsResultIsKept() = runTest {
        val repository = repository()
        val puzzle = LineGenerator.generate(7L, size = 5, checkpoints = 4)
        val answer = LineRound(0, "x", puzzle, Language.EN).toStored().answer
        var finished = 0
        val replay = LineReplay(repository, now = { now }, onFinished = { finished++ })

        replay.start(favorite(GameKind.Line, answer, Difficulty.Medium))
        val page = assertNotNull(replay.page)
        assertEquals(puzzle.solution.toList(), page.puzzle.solution.toList(), "the favorite's board, not a new one")
        assertEquals(false, page.isFinished)

        for (cell in puzzle.solution) replay.drag(cell)
        assertTrue(replay.page!!.isFinished, "the board can be solved")
        assertEquals(1, finished)
        assertTrue(repository.history().any { it.game == GameKind.Line && it.id == replay.page!!.id }, "its result is a result of its own")
    }

    @Test
    fun aReplayedWinIsKeptButDoesNotRaiseTheSolvedCount() = runTest {
        val puzzle = LineGenerator.generate(11L, size = 5, checkpoints = 4)
        val answer = LineRound(0, "x", puzzle, Language.EN).toStored().answer
        val finished = mutableListOf<FinishedRound>()
        val store = object : com.mehmtcan.brainscroll.game.wordle.FeedStore {
            override fun saveProgress(round: com.mehmtcan.brainscroll.game.wordle.StoredRound) = Unit
            override fun deleteProgress(id: String) = Unit
            override fun finish(result: FinishedRound) { finished += result }
        }
        val copy = com.mehmtcan.brainscroll.game.wordle.StoredRound("copy", Language.EN, answer, emptyList(), "", 0, GameKind.Line)
        val feed = com.mehmtcan.brainscroll.game.wordle.EndlessFeed(
            wordLists = emptyMap(), startLanguage = Language.EN, store = store, restored = listOf(copy), now = { now }, games = listOf(GameKind.Line),
        ).also { it.ensureSize(1) }
        feed.markAsReplay("copy")

        for (cell in puzzle.solution) feed.dragLine(0, cell)

        assertEquals(1, finished.size, "the result is kept")
        assertEquals(0, feed.snapshot().solvedCount, "but the number of puzzles solved in a row stays where it was")
    }
}
