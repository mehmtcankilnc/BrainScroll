package com.mehmtcan.brainscroll.daily

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.prepareDatabase
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.crossword.CrosswordSchedule
import com.mehmtcan.brainscroll.game.crossword.CrosswordSnapshot
import com.mehmtcan.brainscroll.game.daily.dailyGameId
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
 * The daily puzzle of the app, the crossword: the board is the one of the day (and language), the same for everyone, saved as it
 * goes, counted for the day streak, drawn again as a solved board the next time the app opens, and sent to the server when there
 * is a connection. Its stars come from the mistakes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyCrosswordViewModelTest {

    private val api = FakeDailyApi()
    private val signedIn = MutableStateFlow(true)
    private var day = 20_740L
    private var clock = IstanbulDay.startOfDay(day) + 10 * 3_600_000L

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun repository(): GameRepository {
        val file = File.createTempFile("daily-crossword-test", ".db").also { it.deleteOnExit() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { prepareDatabase(it) }
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun viewModel(repository: GameRepository = repository(), language: Language = Language.EN) = DailyGameViewModel(
        repository = repository,
        api = api,
        signedIn = signedIn,
        now = { clock += 1_000L; clock },
        gameOf = { GameKind.Crossword },
        language = { language },
    )

    private fun DailyGameViewModel.board(): CrosswordSnapshot = assertIs(ui.value.page)

    private fun puzzle(language: Language = Language.EN) = CrosswordSchedule.puzzleFor(day, language)

    private fun DailyGameViewModel.letterCells() = board().state.isClue.indices.filter { !board().state.isClue[it] }

    private fun DailyGameViewModel.solve() {
        val squares = puzzle().squares
        for (cell in letterCells()) placeLetter(0, cell, squares[cell])
    }

    private fun wrongLetter(right: Char) = Language.EN.alphabet.first { it != right }

    @Test
    fun theBoardIsTheOneOfTheDayAndLanguageAndEveryoneGetsTheSame() = runTest {
        for (language in Language.entries) {
            val a = viewModel(language = language)
            val b = viewModel(language = language)
            val expected = puzzle(language)
            assertEquals(expected.clues.toSet(), a.board().state.clues.map { it.text }.toSet())
            assertEquals(expected.clues.toSet(), b.board().state.clues.map { it.text }.toSet())
            assertEquals(language, a.board().language)
            assertEquals(GameKind.Crossword, a.ui.value.game)
            assertEquals(DailyGamePhase.Playing, a.ui.value.phase)
        }
    }

    @Test
    fun aBoardThatWasStartedComesBackAtOnce() = runTest {
        val repository = repository()
        val first = viewModel(repository)
        val squares = puzzle().squares
        val cells = first.letterCells()
        first.placeLetter(0, cells[0], squares[cells[0]])
        first.placeLetter(0, cells[1], wrongLetter(squares[cells[1]]))
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
        vm.solve()
        advanceUntilIdle()

        val ui = vm.ui.value
        assertEquals(DailyGamePhase.Finished, ui.phase)
        val result = repository.history().single()
        assertEquals(dailyGameId(day, GameKind.Crossword), result.id)
        assertEquals(Mode.DAILY, result.mode)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(day, result.dayIndex)
        assertEquals(listOf("1"), result.guesses, "the score code: 1 and no mistakes")
        assertEquals(puzzle().encode(), result.answer, "the board is named, so the solved board can be drawn again")
        assertEquals(1, ui.dayStreak.current)
        assertEquals(3, vm.board().stars)
    }

    @Test
    fun theMistakesTakeStarsOffAndAreInTheScoreCode() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        val squares = puzzle().squares
        val cells = vm.letterCells()
        repeat(11) {
            vm.placeLetter(0, cells[0], wrongLetter(squares[cells[0]]))
            vm.eraseLetter(0, cells[0])
        }
        vm.solve()
        advanceUntilIdle()

        assertEquals(listOf("12"), repository.history().single().guesses)
        assertEquals(1, vm.board().stars)
        assertEquals(1, vm.ui.value.dayStreak.current)
    }

    @Test
    fun theResultIsSentToTheServerOnceWithTheScoreCodeAsItsMoves() = runTest {
        val vm = viewModel()
        vm.solve()
        advanceUntilIdle()
        assertEquals(1, api.gameResults.size)
        assertTrue(api.gameResults.single().startsWith("crossword:WON:1:"), api.gameResults.single())
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, api.gameResults.size, "asking again does not send it again")
    }

    @Test
    fun aSolvedBoardComesBackDrawnAsSolvedWithTheStarsAndTimeOfTheResult() = runTest {
        val repository = repository()
        val first = viewModel(repository)
        val squares = puzzle().squares
        val cells = first.letterCells()
        first.placeLetter(0, cells[0], wrongLetter(squares[cells[0]]))
        first.eraseLetter(0, cells[0])
        first.solve()
        advanceUntilIdle()
        val result = repository.history().single()

        val again = viewModel(repository)
        assertEquals(DailyGamePhase.Finished, again.ui.value.phase)
        assertEquals(Outcome.WON, again.ui.value.result?.outcome)
        val page = again.board()
        assertTrue(page.isFinished)
        assertEquals(1, page.state.mistakes)
        assertEquals(3, page.stars)
        assertEquals(result.durationMs, page.durationMs)
        assertNotNull(page.state.letters.firstOrNull { it != ' ' })
        assertTrue(repository.unfinishedDailyRound(result.id) == null)
    }

    @Test
    fun aNewDayStartsANewBoard() = runTest {
        val repository = repository()
        val vm = viewModel(repository)
        val before = vm.board().state.clues.map { it.text }.toSet()
        vm.solve()
        advanceUntilIdle()
        assertEquals(DailyGamePhase.Finished, vm.ui.value.phase)

        day += 1
        clock = IstanbulDay.startOfDay(day) + 3_600_000L
        vm.refresh()
        assertEquals(DailyGamePhase.Playing, vm.ui.value.phase)
        assertEquals(day, vm.ui.value.day)
        assertTrue(vm.board().state.letters.all { it == ' ' })
        assertTrue(vm.board().state.clues.map { it.text }.toSet() != before)
    }
}
