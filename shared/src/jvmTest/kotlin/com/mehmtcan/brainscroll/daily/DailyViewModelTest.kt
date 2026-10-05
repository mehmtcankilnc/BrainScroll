package com.mehmtcan.brainscroll.daily

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.prepareDatabase
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.LetterResult
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.WordList
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.daily.DailyEvent
import com.mehmtcan.brainscroll.ui.daily.DailyPhase
import com.mehmtcan.brainscroll.ui.daily.DailyViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DailyViewModelTest {

    private val api = FakeDailyApi()
    private val signedIn = MutableStateFlow(true)

    private val lists = mapOf(
        Language.EN to WordList(Language.EN, 5, listOf("crane", "slate", "ghost", "pound", "apple", "brick"), listOf("crane")),
        Language.TR to WordList(Language.TR, 5, listOf("kitap", "kırık", "çiçek"), listOf("kitap")),
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun repository(): GameRepository {
        val file = File.createTempFile("daily-test", ".db").also { it.deleteOnExit() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { prepareDatabase(it) }
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun viewModel(
        repository: GameRepository = repository(),
        language: Language = Language.EN,
    ) = DailyViewModel(api, repository, signedIn, language, now = { api.clock }, loadWords = { lists.getValue(it) })

    private fun DailyViewModel.typeWord(word: String) = word.forEach { type(it) }

    /** Types a word and presses Enter, then lets the (instant) fake server answer. */
    private fun TestScope.play(vm: DailyViewModel, word: String) {
        vm.typeWord(word)
        vm.submit()
        advanceUntilIdle()
    }

    private fun TestScope.started(vm: DailyViewModel = viewModel()): DailyViewModel {
        advanceUntilIdle()
        vm.start()
        advanceUntilIdle()
        return vm
    }

    @Test
    fun opensOnTheStartScreenWithoutStartingTheClock() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(DailyPhase.Intro, vm.ui.value.phase)
        assertEquals(listOf("state"), api.calls) // only looked, did not start
        assertNull(vm.ui.value.round)
    }

    @Test
    fun startingMovesToPlaying() = runTest {
        val vm = started()
        assertEquals(DailyPhase.Playing, vm.ui.value.phase)
        assertEquals(6, vm.ui.value.round!!.maxAttempts)
        assertNull(vm.ui.value.round!!.answer) // never shown while playing
    }

    @Test
    fun withoutAnAccountTheScreenSaysItIsOffline() = runTest {
        signedIn.value = false
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(DailyPhase.Offline, vm.ui.value.phase)
        assertTrue(api.calls.isEmpty())

        signedIn.value = true // the account arrives
        advanceUntilIdle()
        assertEquals(DailyPhase.Intro, vm.ui.value.phase)
    }

    @Test
    fun offlineAtOpeningShowsOfflineAndRetryRecovers() = runTest {
        api.failure = DailyException.Offline()
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(DailyPhase.Offline, vm.ui.value.phase)

        api.failure = null
        vm.refresh()
        advanceUntilIdle()
        assertEquals(DailyPhase.Intro, vm.ui.value.phase)
    }

    @Test
    fun aServerErrorShowsUnavailable() = runTest {
        api.failure = DailyException.Unavailable()
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(DailyPhase.Unavailable, vm.ui.value.phase)
    }

    @Test
    fun typingBuildsUppercaseLettersUpToFiveAndBackspaceRemoves() = runTest {
        val vm = started()
        vm.typeWord("cranes1 ")
        assertEquals("CRANE", vm.ui.value.round!!.input)
        vm.backspace()
        assertEquals("CRAN", vm.ui.value.round!!.input)
    }

    @Test
    fun typingDoesNothingBeforeTheAttemptIsStarted() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.typeWord("crane")
        assertNull(vm.ui.value.round)
    }

    @Test
    fun aShortWordIsRefusedOnThePhoneWithoutAnyRequest() = runTest {
        val vm = started()
        val events = collectEvents(vm)
        vm.typeWord("cra")
        vm.submit()
        advanceUntilIdle()
        assertEquals(listOf(DailyEvent.TooShort), events)
        assertEquals(listOf("state", "start"), api.calls) // nothing was sent
        assertEquals(1, vm.ui.value.round!!.errorTick)
        assertEquals("CRA", vm.ui.value.round!!.input) // the letters stay so they can be fixed
    }

    @Test
    fun aWordNotInTheListIsRefusedOnThePhoneAndCostsNothing() = runTest {
        val vm = started()
        val events = collectEvents(vm)
        vm.typeWord("zzzzz")
        vm.submit()
        advanceUntilIdle()
        assertEquals(listOf(DailyEvent.NotInList), events)
        assertTrue(api.calls.none { it.startsWith("guess") })
    }

    @Test
    fun aGuessIsColoredByTheServerAndTheInputIsCleared() = runTest {
        val vm = started()
        play(vm, "slate") // answer is CRANE
        val round = vm.ui.value.round!!
        assertEquals(1, round.rows.size)
        assertEquals("SLATE", round.rows[0].word)
        assertEquals(
            listOf(LetterResult.Absent, LetterResult.Absent, LetterResult.Correct, LetterResult.Absent, LetterResult.Correct),
            round.rows[0].results,
        )
        assertEquals("", round.input)
        assertEquals(LetterResult.Correct, round.letterStates['A'])
        assertEquals(DailyPhase.Playing, vm.ui.value.phase)
    }

    @Test
    fun theRowIsMarkedPendingWhileTheServerIsAnswering() = runTest {
        val vm = started()
        val gate = CompletableDeferred<Unit>()
        api.gate = gate
        vm.typeWord("slate")
        vm.submit()
        advanceUntilIdle()

        assertEquals("SLATE", vm.ui.value.round!!.pending)
        vm.typeWord("x") // typing is blocked meanwhile
        assertEquals("SLATE", vm.ui.value.round!!.input)

        gate.complete(Unit)
        advanceUntilIdle()
        assertNull(vm.ui.value.round!!.pending)
        assertEquals(1, vm.ui.value.round!!.rows.size)
    }

    @Test
    fun goingOfflineDuringAGuessKeepsTheLettersAndSaysSo() = runTest {
        val vm = started()
        val events = collectEvents(vm)
        api.failure = DailyException.Offline()
        vm.typeWord("slate")
        vm.submit()
        advanceUntilIdle()

        assertEquals(listOf(DailyEvent.Offline), events)
        assertEquals("SLATE", vm.ui.value.round!!.input)
        assertNull(vm.ui.value.round!!.pending)
        assertEquals(DailyPhase.Playing, vm.ui.value.phase)

        api.failure = null // the connection is back: pressing Enter again works
        vm.submit()
        advanceUntilIdle()
        assertEquals(1, vm.ui.value.round!!.rows.size)
    }

    @Test
    fun winningFinishesWithTheTimeAndRecordsTheResultOnThePhone() = runTest {
        val repo = repository()
        api.clock = 5_000L
        val vm = started(viewModel(repo))
        api.clock = 5_000L + 83_000L // the player took 1:23
        play(vm, "crane")

        assertEquals(DailyPhase.Finished, vm.ui.value.phase)
        assertEquals(83_000L, vm.ui.value.state!!.durationMs)
        assertEquals("CRANE", vm.ui.value.round!!.answer)

        val saved = repo.history().single()
        assertEquals(Mode.DAILY, saved.mode)
        assertEquals(Outcome.WON, saved.outcome)
        assertEquals(83_000L, saved.durationMs)
        assertEquals(20_733L, saved.dayIndex) // the puzzle's day, not the day of the fake clock
        assertEquals(0, repo.pendingCount()) // the server already has it, nothing to upload
        assertEquals(1, vm.ui.value.dayStreak.current)
    }

    @Test
    fun theRecordedResultCountsForThePuzzleDayEvenAfterMidnight() = runTest {
        val repo = repository()
        api.dayIndex = 20_733
        api.clock = IstanbulDay.startOfDay(20_734) + 5_000 // already the next day when the attempt ends
        val vm = started(viewModel(repo))
        play(vm, "crane")
        assertEquals(20_733L, repo.history().single().dayIndex)
    }

    @Test
    fun sixWrongGuessesLoseAndRevealTheAnswer() = runTest {
        val vm = started()
        for (word in listOf("slate", "ghost", "pound", "apple", "brick", "slate")) play(vm, word)
        assertEquals(DailyPhase.Finished, vm.ui.value.phase)
        assertEquals(DailyStatus.Lost, vm.ui.value.state!!.status)
        assertEquals("CRANE", vm.ui.value.round!!.answer)
        // A loss still finishes the day: it counts for the day streak (docs/decisions.md).
        assertEquals(1, vm.ui.value.dayStreak.current)
    }

    @Test
    fun anAttemptThatWasAlreadyRunningIsResumedWithItsRows() = runTest {
        api.prefill(Language.EN, listOf("SLATE", "GHOST"))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(DailyPhase.Playing, vm.ui.value.phase)
        assertEquals(listOf("SLATE", "GHOST"), vm.ui.value.round!!.rows.map { it.word })
    }

    @Test
    fun anAttemptFinishedOnAnotherPhoneShowsTheResultAndIsRecordedHere() = runTest {
        api.prefill(Language.EN, listOf("CRANE"))
        val repo = repository()
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertEquals(DailyPhase.Finished, vm.ui.value.phase)
        assertEquals(1, repo.history().size)
        vm.refresh() // opening it again does not record a second copy
        advanceUntilIdle()
        assertEquals(1, repo.history().size)
    }

    @Test
    fun theTwoLanguagesAreIndependentPuzzles() = runTest {
        val vm = started()
        play(vm, "slate")
        assertEquals(1, vm.ui.value.round!!.rows.size)

        vm.selectLanguage(Language.TR)
        advanceUntilIdle()
        assertEquals(Language.TR, vm.ui.value.language)
        assertEquals(DailyPhase.Intro, vm.ui.value.phase) // Turkish was not started

        vm.selectLanguage(Language.EN)
        advanceUntilIdle()
        assertEquals(1, vm.ui.value.round!!.rows.size) // English is where we left it
    }

    @Test
    fun finishingBothLanguagesOnTheSameDayCountsAsOneDayInTheStreak() = runTest {
        val vm = started()
        play(vm, "crane")
        vm.selectLanguage(Language.TR)
        advanceUntilIdle()
        vm.start()
        advanceUntilIdle()
        play(vm, "kitap")
        assertEquals(1, vm.ui.value.dayStreak.current)
    }

    @Test
    fun theStreakContinuesFromYesterday() = runTest {
        val repo = repository()
        repo.finish(
            FinishedRound(
                id = "yesterday", mode = Mode.DAILY, language = Language.EN, answer = "SLATE", guesses = listOf("SLATE"),
                outcome = Outcome.WON, wasSkipped = false, finishedAt = IstanbulDay.startOfDay(20_732) + 1_000,
                durationMs = 5_000,
            ),
        )
        api.clock = IstanbulDay.startOfDay(20_733) + 1_000
        val vm = started(viewModel(repo))
        assertEquals(1, vm.ui.value.dayStreak.current) // yesterday counts, today is still open
        play(vm, "crane")
        assertEquals(2, vm.ui.value.dayStreak.current)
    }

    @Test
    fun turkishLowercaseTypingMatchesTheUppercaseRules() = runTest {
        val vm = viewModel(language = Language.TR)
        advanceUntilIdle()
        vm.start()
        advanceUntilIdle()
        vm.typeWord("kitap") // lowercase i must become İ (the answer is KİTAP)
        assertEquals("KİTAP", vm.ui.value.round!!.input)
        vm.submit()
        advanceUntilIdle()
        assertEquals(DailyPhase.Finished, vm.ui.value.phase)
    }

    @Test
    fun theAnswerIsNeverPartOfTheScreenStateWhilePlaying() = runTest {
        val vm = started()
        play(vm, "slate")
        assertFalse(vm.ui.value.toString().contains("CRANE"), "the screen state must not leak the answer")
        assertNotNull(vm.ui.value.state)
    }

    /** Collects the one-off events of a ViewModel into a list for the rest of the test. */
    private fun TestScope.collectEvents(vm: DailyViewModel): MutableList<DailyEvent> {
        val events = mutableListOf<DailyEvent>()
        // Unconfined: the event is recorded the moment it is emitted, no scheduling to wait for.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }
        return events
    }
}
