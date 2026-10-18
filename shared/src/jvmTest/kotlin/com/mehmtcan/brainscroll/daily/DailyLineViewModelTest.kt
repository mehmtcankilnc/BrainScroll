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
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The daily puzzle of the app, One Line: the board is the one of the day, the same for everyone, saved as it goes, counted for
 * the day streak, drawn again as a solved board the next time the app opens, and sent to the server when there is a connection.
 * Its stars come from the squares taken back and the hints.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyLineViewModelTest {

    private val api = FakeDailyApi()
    private val signedIn = MutableStateFlow(true)
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

    /** A board is made (or drawn again) off the main thread: wait until the page of today's board is there. */
    private fun TestScope.awaitBoard(vm: DailyGameViewModel): LineSnapshot {
        val end = System.currentTimeMillis() + 10_000
        while ((vm.ui.value.page as? LineSnapshot)?.puzzle?.encode() != puzzle().encode() && System.currentTimeMillis() < end) {
            advanceUntilIdle()
            Thread.sleep(10)
        }
        return assertIs(vm.ui.value.page)
    }

    private fun DailyGameViewModel.board(): LineSnapshot = assertIs(ui.value.page)

    private fun puzzle() = LineSchedule.puzzleFor(day)

    private fun solution() = puzzle().solution.toList()

    private fun DailyGameViewModel.drag(cells: List<Int>) = cells.forEach { dragLine(0, it) }

    @Test
    fun theBoardIsTheOneOfTheDayAndEveryoneGetsTheSame() = runTest {
        val a = viewModel()
        val b = viewModel()
        assertEquals(puzzle().encode(), awaitBoard(a).puzzle.encode())
        assertEquals(puzzle().encode(), awaitBoard(b).puzzle.encode())
        assertEquals(GameKind.Line, a.ui.value.game)
        assertEquals(DailyGamePhase.Playing, a.ui.value.phase)
    }

    @Test
    fun aBoardThatWasStartedComesBackAtOnce() = runTest {
        val repository = repository()
        val first = viewModel(repository)
        awaitBoard(first)
        first.drag(solution().take(3))
        val saved = first.board().state

        val second = viewModel(repository)
        assertEquals(saved, second.board().state, "read from storage")
        assertEquals(DailyGamePhase.Playing, second.ui.value.phase)
        assertTrue(repository.unfinishedRounds().isEmpty(), "the feed does not restore the puzzle of the day")
    }

    @Test
    fun solvingKeepsADailyResultWithItsScoreCodeAndCountsForTheDayStreak() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        awaitBoard(vm)
        vm.drag(solution())
        advanceUntilIdle()

        val ui = vm.ui.value
        assertEquals(DailyGamePhase.Finished, ui.phase)
        val result = repository.history().single()
        assertEquals(dailyGameId(day, GameKind.Line), result.id)
        assertEquals(Mode.DAILY, result.mode)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(day, result.dayIndex)
        assertEquals(listOf("1"), result.guesses, "the score code: 1 and no undo, no hint")
        assertEquals(1, ui.dayStreak.current)
        assertEquals(3, vm.board().stars)
    }

    @Test
    fun squaresTakenBackAndHintsTakeStarsOffAndAreInTheScoreCode() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        awaitBoard(vm)
        val cells = solution()
        vm.drag(cells.take(2))
        assertTrue(vm.undoLine(0))
        assertEquals(2, vm.board().stars)
        vm.drag(cells.drop(1))
        advanceUntilIdle()
        assertEquals(listOf("2"), repository.history().single().guesses, "1 + one undo")
        assertEquals(2, vm.board().stars)

        // A hint costs a star as well.
        val other = repository()
        val hinted = viewModel(other)
        awaitBoard(hinted)
        assertNotNull(hinted.hintLine(0))
        hinted.drag(cells)
        advanceUntilIdle()
        assertEquals(listOf("101"), other.history().single().guesses, "1 + 100 for the hint")
        assertEquals(2, hinted.board().stars)
    }

    @Test
    fun theResultIsSentToTheServerOnceWithTheScoreCodeAsItsMoves() = runTest {
        val vm = viewModel()
        awaitBoard(vm)
        vm.drag(solution())
        advanceUntilIdle()
        assertEquals(1, api.gameResults.size)
        assertTrue(api.gameResults.single().startsWith("line:WON:1:"), api.gameResults.single())
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, api.gameResults.size, "asking again does not send it again")
    }

    @Test
    fun aSolvedBoardComesBackDrawnAsSolvedWithTheStarsAndTimeOfTheResult() = runTest {
        val repository = repository()
        val first = viewModel(repository)
        awaitBoard(first)
        val cells = solution()
        first.drag(cells.take(2))
        first.undoLine(0)
        first.drag(cells.drop(1))
        advanceUntilIdle()
        val result = repository.history().single()

        val again = viewModel(repository)
        assertEquals(DailyGamePhase.Finished, again.ui.value.phase)
        assertEquals(Outcome.WON, again.ui.value.result?.outcome)
        val page = awaitBoard(again)
        assertTrue(page.isFinished)
        assertEquals(1, page.state.undos)
        assertEquals(2, page.stars)
        assertEquals(result.durationMs, page.durationMs)
        assertTrue(repository.unfinishedDailyRound(result.id) == null)
    }

    @Test
    fun aNewDayStartsANewBoard() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        awaitBoard(vm)
        vm.drag(solution())
        advanceUntilIdle()
        assertEquals(DailyGamePhase.Finished, vm.ui.value.phase)

        day += 1
        clock = IstanbulDay.startOfDay(day) + 3_600_000L
        vm.refresh()
        assertEquals(DailyGamePhase.Playing, vm.ui.value.phase)
        assertEquals(day, vm.ui.value.day)
        val board = awaitBoard(vm)
        assertTrue(board.state.path.isEmpty())
    }
}
