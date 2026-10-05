package com.mehmtcan.brainscroll.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Time is virtual here: `advanceTimeBy(5_000)` returns at once, it only moves the test clock. */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncCoordinatorTest {

    private val cloud = FakeCloud()
    private val user = MutableStateFlow<String?>(null)
    private var syncedCount = 0

    private fun device(): GameRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BrainScrollDatabase.Schema.create(driver)
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun result(id: String) =
        FinishedRound(id, Mode.ENDLESS, Language.EN, "CRANE", listOf("CRANE"), Outcome.WON, false, 1_000L)

    private fun TestScope.coordinator(repo: GameRepository): SyncCoordinator {
        // The fake cloud acts for whoever is signed in right now.
        val engine = SyncEngine(repo, object : CloudApi by cloud {
            override suspend fun uploadResults(results: List<FinishedRound>) {
                cloud.currentUser = user.value ?: error("signed out")
                cloud.uploadResults(results)
            }
            override suspend fun downloadResults(): List<FinishedRound> {
                cloud.currentUser = user.value ?: error("signed out")
                return cloud.downloadResults()
            }
        })
        return SyncCoordinator(backgroundScope, engine, user, onSynced = { syncedCount++ }).also { it.start() }
    }

    @Test
    fun syncsRightAfterSigningIn() = runTest {
        val repo = device().apply { finish(result("r1")) }
        coordinator(repo)

        runCurrent()
        assertEquals(0, cloud.resultsOf("a").size) // not signed in yet: nothing happens
        user.value = "a"
        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(setOf("r1"), cloud.resultsOf("a").keys)
        assertEquals(1, syncedCount)
    }

    @Test
    fun aLocalChangeIsSyncedShortlyAfterwards() = runTest {
        val repo = device()
        val sync = coordinator(repo)
        user.value = "a"
        advanceTimeBy(2_000); runCurrent()

        repo.finish(result("r2"))
        sync.request()
        advanceTimeBy(1_000); runCurrent()
        assertEquals(0, cloud.resultsOf("a").size) // still waiting out the short delay
        advanceTimeBy(1_000); runCurrent()
        assertEquals(setOf("r2"), cloud.resultsOf("a").keys)
    }

    @Test
    fun aBurstOfChangesIsOneSync() = runTest {
        val repo = device()
        val sync = coordinator(repo)
        user.value = "a"
        advanceTimeBy(2_000); runCurrent()
        val syncsBefore = syncedCount

        repeat(5) { i -> repo.finish(result("b$i")); sync.request() }
        advanceTimeBy(2_000); runCurrent()

        assertEquals(5, cloud.resultsOf("a").size)
        assertEquals(syncsBefore + 1, syncedCount)
    }

    @Test
    fun aFailedSyncIsRetriedWithGrowingWaits() = runTest {
        val repo = device().apply { finish(result("r1")) }
        coordinator(repo)
        cloud.failTransientCalls = 2 // the first two attempts fail

        // Timeline: sign-in at 0 s, each attempt starts 1.5 s after it was requested (the short delay for bursts).
        user.value = "a"
        advanceTimeBy(1_600); runCurrent() // 1.5 s: first attempt fails, then it waits 5 s
        assertEquals(0, cloud.resultsOf("a").size)
        advanceTimeBy(5_000 + 1_500); runCurrent() // 8 s: second attempt fails, then it waits 15 s
        assertEquals(0, cloud.resultsOf("a").size)
        advanceTimeBy(15_000 + 1_600); runCurrent() // 24.5 s: third attempt works
        assertEquals(setOf("r1"), cloud.resultsOf("a").keys)
        assertEquals(1, syncedCount)
    }

    @Test
    fun theLoopStartsOverForANewAccount() = runTest {
        val repo = device().apply { finish(result("r1")) }
        coordinator(repo)
        user.value = "a"
        advanceTimeBy(2_000); runCurrent()

        user.value = "b" // signed in as somebody else: this device's history is offered to that account
        advanceTimeBy(2_000); runCurrent()

        assertEquals(setOf("r1"), cloud.resultsOf("a").keys)
        assertEquals(setOf("r1"), cloud.resultsOf("b").keys)
    }

    @Test
    fun signingOutStopsSyncing() = runTest {
        val repo = device()
        val sync = coordinator(repo)
        user.value = "a"
        advanceTimeBy(2_000); runCurrent()

        user.value = null
        repo.finish(result("late"))
        sync.request()
        advanceTimeBy(10_000); runCurrent()

        assertEquals(0, cloud.resultsOf("a").size)
    }
}
