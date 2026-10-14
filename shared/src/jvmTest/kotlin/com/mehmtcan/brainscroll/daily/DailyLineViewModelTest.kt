package com.mehmtcan.brainscroll.daily

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.prepareDatabase
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.daily.dailyGameId
import com.mehmtcan.brainscroll.game.line.LineSchedule
import com.mehmtcan.brainscroll.game.line.LineSnapshot
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The daily puzzle of the app, One Line: the board is made from the day number in the background, the same for everyone,
 * saved as it goes, counted for the day streak, drawn again as a solved board the next time the app opens, and sent to
 * the server when there is a connection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyLineViewModelTest {

    private val api = FakeDailyApi()
    private val signedIn = MutableStateFlow(true)

    /** A Wednesday, so a 5x5 board. */
    private var day = 20_740L
    private var clock = IstanbulDay.startOfDay(day) + 10 * 3_600_000L

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun repository(): GameRepository {
        val file = File.createTempFile("daily-line-test", ".db").also { it.deleteOnExit() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { prepareDatabase(it) }
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun viewModel(repository: GameRepository = repository()) = DailyGameViewModel(
        repository = repository,
        api = api,
        signedIn = signedIn,
        now = { clock += 1_000L; clock },
        gameOf = { GameKind.Line },
        language = { Language.EN },
    )

    /** The board is made on another thread: let the test's own thread take turns until the page is there. */
    private fun TestScope.awaitPage(vm: DailyGameViewModel) {
        var tries = 0
        while (vm.ui.value.page == null && tries++ < 600) {
            advanceUntilIdle()
            Thread.sleep(10)
        }
        assertNotNull(vm.ui.value.page, "the board was never made")
    }

    private fun DailyGameViewModel.line(): LineSnapshot = assertIs(ui.value.page)

    private fun DailyGameViewModel.solve() {
        for (cell in LineSchedule.puzzleFor(day).solution) dragLine(0, cell)
    }

    @Test
    fun theBoardIsTheOneOfTheDayAndEveryoneGetsTheSame() = runTest {
        val a = viewModel()
        val b = viewModel()
        awaitPage(a)
        awaitPage(b)
        val expected = LineSchedule.puzzleFor(day)
        assertContentEquals(expected.checkpoints, a.line().puzzle.checkpoints)
        assertContentEquals(expected.checkpoints, b.line().puzzle.checkpoints)
        assertEquals(GameKind.Line, a.ui.value.game)
        assertEquals(DailyGamePhase.Playing, a.ui.value.phase)
    }

    @Test
    fun whileTheBoardIsBeingMadeThereIsNoPageAndNoResult() = runTest {
        val vm = viewModel()
        assertNull(vm.ui.value.page)
        assertNull(vm.ui.value.result)
        assertEquals(DailyGamePhase.Playing, vm.ui.value.phase)
    }

    @Test
    fun aBoardThatWasStartedComesBackAtOnceWithoutBeingMadeAgain() = runTest {
        val repository = repository()
        val first = viewModel(repository)
        awaitPage(first)
        val line = LineSchedule.puzzleFor(day).solution
        first.dragLine(0, line[0])
        first.dragLine(0, line[1])
        first.dragLine(0, line[2])
        val saved = first.line().state

        val second = viewModel(repository)
        assertEquals(saved, second.line().state, "read from storage, no waiting")
        assertEquals(DailyGamePhase.Playing, second.ui.value.phase)
        assertTrue(repository.unfinishedRounds().isEmpty(), "the feed does not restore the puzzle of the day")
    }

    @Test
    fun solvingKeepsADailyResultWithItsStarsAndCountsForTheDayStreak() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        awaitPage(vm)
        vm.solve()
        advanceUntilIdle()

        val ui = vm.ui.value
        assertEquals(DailyGamePhase.Finished, ui.phase)
        val result = repository.history().single()
        assertEquals(dailyGameId(day, GameKind.Line), result.id)
        assertEquals(Mode.DAILY, result.mode)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(day, result.dayIndex)
        assertEquals(listOf("1"), result.guesses)
        assertEquals(1, ui.dayStreak.current)
        assertEquals(3, vm.line().stars)
    }

    @Test
    fun aHintedBoardStillCountsForTheStreakButTakesAStarOff() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        awaitPage(vm)
        val line = LineSchedule.puzzleFor(day).solution
        vm.dragLine(0, line[0])
        assertNotNull(vm.hintLine(0))
        for (cell in line.drop(1)) vm.dragLine(0, cell)
        advanceUntilIdle()

        assertEquals(1, vm.ui.value.dayStreak.current)
        assertEquals(listOf("101"), repository.history().single().guesses)
        assertEquals(2, vm.line().stars)
    }

    @Test
    fun theResultIsSentToTheServerOnce() = runTest {
        val vm = viewModel()
        awaitPage(vm)
        vm.solve()
        advanceUntilIdle()
        assertEquals(1, api.gameResults.size)
        assertTrue(api.gameResults.single().startsWith("line:WON:"), api.gameResults.single())
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, api.gameResults.size, "asking again does not send it again")
    }

    @Test
    fun aSolvedBoardComesBackDrawnAsSolvedWithTheStarsAndTimeOfTheResult() = runTest {
        val repository = repository()
        val first = viewModel(repository)
        awaitPage(first)
        first.solve()
        advanceUntilIdle()
        val result = repository.history().single()

        val again = viewModel(repository)
        assertEquals(DailyGamePhase.Finished, again.ui.value.phase)
        assertEquals(Outcome.WON, again.ui.value.result?.outcome)
        awaitPage(again)
        val page = again.line()
        assertTrue(page.isFinished)
        assertEquals(LineSchedule.puzzleFor(day).cells, page.state.path.size)
        assertEquals(3, page.stars)
        assertEquals(result.durationMs, page.durationMs)
        assertTrue(repository.unfinishedDailyRound(result.id) == null)
    }

    @Test
    fun aNewDayStartsANewBoard() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        awaitPage(vm)
        vm.solve()
        advanceUntilIdle()
        assertEquals(DailyGamePhase.Finished, vm.ui.value.phase)

        day += 3 // a Saturday: 7x7
        clock = IstanbulDay.startOfDay(day) + 3_600_000L
        vm.refresh()
        assertEquals(DailyGamePhase.Playing, vm.ui.value.phase)
        assertEquals(day, vm.ui.value.day)
        awaitPage(vm)
        assertEquals(7, vm.line().puzzle.size)
        assertEquals(0, vm.line().state.path.size)
    }
}
