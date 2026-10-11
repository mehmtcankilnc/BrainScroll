package com.mehmtcan.brainscroll.daily

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.prepareDatabase
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.daily.dailyGameId
import com.mehmtcan.brainscroll.game.mines.MinesGame
import com.mehmtcan.brainscroll.game.mines.MinesSnapshot
import com.mehmtcan.brainscroll.game.mines.MinesSolver
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.daily.DailyGamePhase
import com.mehmtcan.brainscroll.ui.daily.DailyGameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The daily puzzle of the days when the game is not the word puzzle, played on the device: the same board for everyone,
 * saved as it goes, counted for the day streak, and sent to the server when there is a connection. Minesweeper stands in
 * for every game: the page, the saving and the sending do not depend on which one it is.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyGameViewModelTest {

    private val api = FakeDailyApi()
    private val signedIn = MutableStateFlow(true)
    private var day = 20_733L
    private var clock = IstanbulDay.startOfDay(day) + 10 * 3_600_000L

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun repository(): GameRepository {
        val file = File.createTempFile("daily-game-test", ".db").also { it.deleteOnExit() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { prepareDatabase(it) }
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun viewModel(repository: GameRepository = repository()) = DailyGameViewModel(
        repository = repository,
        api = api,
        signedIn = signedIn,
        // Every look at the clock is a second later, so the puzzle takes some time.
        now = { clock += 1_000L; clock },
        gameOf = { GameKind.Mines },
        language = { Language.EN },
    )

    private fun DailyGameViewModel.board(): List<Int> = (ui.value.page as MinesSnapshot).state.numbers

    /** Plays minesweeper with logic alone, like the page's own tests: the boards need no guess. */
    private fun DailyGameViewModel.solve() {
        openCell(0, 27)
        var guard = 0
        while (ui.value.phase == DailyGamePhase.Playing && guard++ < 80) {
            val numbers = board()
            val safe = MinesSolver.deduce(MinesGame.DEFAULT_ROWS, MinesGame.DEFAULT_COLS, numbers.toIntArray()).safe.filter { numbers[it] == -1 }
            check(safe.isNotEmpty()) { "the board needed a guess" }
            safe.forEach { openCell(0, it) }
        }
    }

    @Test
    fun everyoneGetsTheSameBoardOnTheSameDay() = runTest {
        val a = viewModel()
        val b = viewModel()
        a.openCell(0, 27)
        b.openCell(0, 27)
        assertEquals(a.board(), b.board())
        assertEquals(GameKind.Mines, a.ui.value.game)
    }

    @Test
    fun anotherDayIsAnotherBoard() = runTest {
        val a = viewModel()
        a.openCell(0, 27)
        val first = a.board()
        day += 1
        clock = IstanbulDay.startOfDay(day) + 10 * 3_600_000L
        val b = viewModel()
        b.openCell(0, 27)
        assertNotEquals(first, b.board())
    }

    @Test
    fun theBoardIsSavedAndComesBackAfterARestart() = runTest {
        val repository = repository()
        val first = viewModel(repository)
        first.openCell(0, 27)
        first.flagCell(0, 0)
        val saved = first.board()

        val second = viewModel(repository)
        assertEquals(saved, second.board())
        assertEquals(DailyGamePhase.Playing, second.ui.value.phase)
    }

    @Test
    fun theUnfinishedPuzzleIsNotInTheFeed() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        vm.openCell(0, 27)
        assertTrue(repository.unfinishedRounds().isEmpty(), "the feed does not restore the puzzle of the day")
        assertEquals(1, repository.unfinishedDailyRound(dailyGameId(day, GameKind.Mines))?.let { 1 })
    }

    @Test
    fun finishingKeepsADailyResultAndCountsForTheDayStreak() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        vm.solve()
        advanceUntilIdle()

        val ui = vm.ui.value
        assertEquals(DailyGamePhase.Finished, ui.phase)
        val result = repository.history().single()
        assertEquals(dailyGameId(day, GameKind.Mines), result.id)
        assertEquals(Mode.DAILY, result.mode)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(day, result.dayIndex)
        assertEquals(1, ui.dayStreak.current)
        assertTrue(repository.unfinishedDailyRound(result.id) == null, "a finished puzzle is no longer in progress")
    }

    @Test
    fun theResultIsSentToTheServerOnce() = runTest {
        val vm = viewModel()
        vm.solve()
        advanceUntilIdle()
        assertEquals(1, api.gameResults.size)
        assertTrue(api.gameResults.single().startsWith("mines:WON:"))
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, api.gameResults.size, "asking again does not send it again")
    }

    @Test
    fun withoutAConnectionTheResultWaitsAndIsSentLater() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        api.failure = DailyException.Offline()
        vm.solve()
        advanceUntilIdle()
        assertTrue(api.gameResults.isEmpty())
        assertEquals(DailyGamePhase.Finished, vm.ui.value.phase, "the puzzle itself is done")

        api.failure = null
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, api.gameResults.size)
    }

    @Test
    fun withoutASessionTheResultStaysOnTheDeviceUntilThereIsOne() = runTest {
        signedIn.value = false
        val vm = viewModel()
        vm.solve()
        advanceUntilIdle()
        assertTrue(api.gameResults.isEmpty())

        signedIn.value = true
        advanceUntilIdle()
        assertEquals(1, api.gameResults.size)
    }

    @Test
    fun aFinishedPuzzleDoesNotComeBackAsABoardAfterARestart() = runTest {
        val repository = repository()
        viewModel(repository).solve()
        advanceUntilIdle()

        val again = viewModel(repository)
        assertEquals(DailyGamePhase.Finished, again.ui.value.phase)
        assertNull(again.ui.value.page, "only the result is known, a finished board is not kept")
        assertEquals(Outcome.WON, again.ui.value.result?.outcome)
    }

    @Test
    fun aNewDayStartsANewPuzzle() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        vm.solve()
        advanceUntilIdle()
        assertEquals(DailyGamePhase.Finished, vm.ui.value.phase)

        day += 1
        clock = IstanbulDay.startOfDay(day) + 3_600_000L
        vm.refresh()
        assertEquals(DailyGamePhase.Playing, vm.ui.value.phase)
        assertEquals(day, vm.ui.value.day)
    }
}
