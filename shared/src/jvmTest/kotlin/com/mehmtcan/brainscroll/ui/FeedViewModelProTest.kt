package com.mehmtcan.brainscroll.ui

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.prepareDatabase
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.stats.Rescue
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.pro.FakeStoreBilling
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Pro rules that live in the view model: the free guide steps and the streak rescue offer. */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelProTest {
    private val now = 1_800_000_000_000L
    private val today = IstanbulDay.dayIndex(now)
    private val billing = FakeStoreBilling()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun repository(): GameRepository {
        val file = File.createTempFile("pro-test", ".db").also { it.deleteOnExit() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { prepareDatabase(it) }
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun TestScope.viewModel(repository: GameRepository, store: FakeStoreBilling? = billing): FeedViewModel =
        FeedViewModel(
            repository = repository,
            deviceLanguage = Language.EN,
            now = { now },
            cloudServices = TestCloud().factory,
            games = listOf(GameKind.Word),
            billing = store,
            buildInBackground = false,
        ).also { advanceUntilIdle() }

    /** Daily puzzles finished on each of [days] (Istanbul day indexes). */
    private fun GameRepository.finishDaily(days: List<Long>) = days.forEach { day ->
        finish(
            FinishedRound(
                id = "daily-$day", mode = Mode.DAILY, language = Language.EN, answer = "CRANE", guesses = listOf("CRANE"),
                outcome = Outcome.WON, finishedAt = IstanbulDay.startOfDay(day) + 1_000L, puzzleDay = day,
            ),
        )
    }

    /** Fourteen days in a row that ended three days ago (two days missed, today still open): Pro's two freezes bridge the gap, the free rule cannot. */
    private val brokenStreak get() = (today - 16..today - 3).toList()

    @Test
    fun theFreePlayerStartsWithThreeStepsAndTheDesktopHasNoLimit() = runTest {
        assertEquals(3, viewModel(repository()).quota.value.freeLeft)
        assertTrue(viewModel(repository(), store = null).quota.value.unlimited, "nothing to buy, so no wall")
    }

    @Test
    fun noRescueWhereNothingCanBeBought() = runTest {
        val repo = repository().also { it.finishDaily(brokenStreak) }
        assertNull(viewModel(repo, store = null).rescueOffer())
    }

    @Test
    fun aBrokenStreakIsOfferedOnceAndOnlyOnceForThatBreak() = runTest {
        val repo = repository().also { it.finishDaily(brokenStreak) }
        val vm = viewModel(repo)
        assertEquals(Rescue(freeStreak = 0, proStreak = 14), vm.rescueOffer())
        // Asking is not showing.
        assertEquals(Rescue(0, 14), vm.rescueOffer())
        vm.markRescueShown()
        assertNull(vm.rescueOffer(), "closed once, not again for this break")
        // Still the same after a restart: it is kept on the device.
        assertNull(viewModel(repo).rescueOffer())
    }

    @Test
    fun noRescueWhenTheStreakIsAlive() = runTest {
        val repo = repository().also { it.finishDaily((today - 8..today - 1).toList()) }
        assertNull(viewModel(repo).rescueOffer())
    }

    @Test
    fun noRescueForAProPlayer() = runTest {
        billing.refreshAnswer = true
        val repo = repository().also { it.finishDaily(brokenStreak) }
        val vm = viewModel(repo)
        assertTrue(vm.pro.isPro.value)
        assertNull(vm.rescueOffer())
    }

    @Test
    fun theStreakOfTheProfileFollowsPro() = runTest {
        val repo = repository().also { it.finishDaily(brokenStreak) }
        val vm = viewModel(repo)
        vm.refreshProfile()
        assertEquals(0, vm.profile.value.dayStreak.current, "free rule: broken")
        billing.updates.emit(true) // the purchase went through
        advanceUntilIdle()
        assertEquals(14, vm.profile.value.dayStreak.current, "Pro rule counts the whole history again")
        billing.updates.emit(false) // refunded
        advanceUntilIdle()
        assertEquals(0, vm.profile.value.dayStreak.current)
    }

    @Test
    fun theAccountIsHandedToTheStore() = runTest {
        viewModel(repository())
        assertEquals(listOf("test-user"), billing.identified)
    }
}
