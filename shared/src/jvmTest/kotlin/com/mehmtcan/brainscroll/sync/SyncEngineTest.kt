package com.mehmtcan.brainscroll.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.data.FavoriteRecord
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.stats.Streaks
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SyncEngineTest {

    private val cloud = FakeCloud()

    private fun newDevice(): GameRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BrainScrollDatabase.Schema.create(driver)
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun result(id: String, at: Long = 1_000L, outcome: Outcome = Outcome.WON, mode: Mode = Mode.ENDLESS) =
        FinishedRound(id, mode, Language.EN, "CRANE", listOf("SLATE", "CRANE"), outcome, wasSkipped = false, finishedAt = at)

    private fun GameRepository.sync(user: String = "user-a"): SyncResult {
        cloud.currentUser = user
        return runBlocking { SyncEngine(this@sync, cloud, batchSize = 3).sync(user) }
    }

    @Test
    fun resultsFinishedOfflineAreUploadedLater() {
        val device = newDevice()
        device.finish(result("r1"))
        device.finish(result("r2"))
        assertEquals(2, device.pendingCount())

        val outcome = device.sync()

        assertIs<SyncResult.Success>(outcome)
        assertEquals(0, device.pendingCount())
        assertEquals(setOf("r1", "r2"), cloud.resultsOf("user-a").keys)
    }

    @Test
    fun uploadsAreSplitIntoBatches() {
        val device = newDevice()
        repeat(7) { device.finish(result("r$it", at = it.toLong())) }
        device.sync()
        assertEquals(7, cloud.resultsOf("user-a").size)
        assertTrue(cloud.uploadCalls >= 3, "7 results with a batch size of 3 need at least 3 uploads")
    }

    @Test
    fun syncingTwiceDoesNotDuplicateAnything() {
        val device = newDevice()
        device.finish(result("r1"))
        device.sync()
        device.sync()
        assertEquals(1, cloud.resultsOf("user-a").size)
        assertEquals(0, device.pendingCount())
    }

    @Test
    fun anOfflineSyncKeepsTheQueueAndTheNextOneUploadsEverything() {
        val device = newDevice()
        device.finish(result("r1"))
        device.finish(result("r2"))

        cloud.failTransientCalls = 1
        val first = device.sync()
        assertIs<SyncResult.Failure>(first)
        assertEquals(2, device.pendingCount()) // nothing was lost

        val second = device.sync()
        assertIs<SyncResult.Success>(second)
        assertEquals(0, device.pendingCount())
        assertEquals(2, cloud.resultsOf("user-a").size)
    }

    @Test
    fun aFailureInTheMiddleContinuesWhereItStopped() {
        val device = newDevice()
        repeat(6) { device.finish(result("r$it", at = it.toLong())) }
        // The first batch (3 items) works, then the connection drops.
        cloud.failTransientCalls = 0
        val flaky = object : CloudApi by cloud {
            var calls = 0
            override suspend fun uploadResults(results: List<FinishedRound>) {
                if (++calls == 2) throw CloudException.Transient("dropped")
                cloud.uploadResults(results)
            }
        }
        val first = runBlocking { SyncEngine(device, flaky, batchSize = 3).sync("user-a") }
        assertIs<SyncResult.Failure>(first)
        assertEquals(3, device.pendingCount())
        assertEquals(3, cloud.resultsOf("user-a").size)

        device.sync()
        assertEquals(6, cloud.resultsOf("user-a").size)
        assertEquals(0, device.pendingCount())
    }

    @Test
    fun aRefusedResultIsDroppedAndTheOthersStillArrive() {
        val device = newDevice()
        device.finish(result("good1", at = 1))
        device.finish(result("bad", at = 2))
        device.finish(result("good2", at = 3))
        cloud.rejectedResultIds += "bad"

        val outcome = device.sync()

        assertIs<SyncResult.Success>(outcome)
        assertEquals(setOf("good1", "good2"), cloud.resultsOf("user-a").keys)
        assertEquals(0, device.pendingCount()) // the refused one is not retried forever
    }

    @Test
    fun aFavoriteToggledSeveralTimesIsUploadedOnceInItsLatestState() {
        val device = newDevice()
        device.finish(result("r1"))
        device.setFavorite("r1", true, now = 10)
        device.setFavorite("r1", false, now = 20)
        device.setFavorite("r1", true, now = 30)
        assertEquals(2, device.pendingCount()) // one result, one favorite

        device.sync()

        assertEquals(FavoriteRecord("r1", true, 30), cloud.favoritesOf("user-a")["r1"])
    }

    @Test
    fun signingInAsAnotherAccountOffersTheWholeHistoryToThatAccount() {
        val device = newDevice()
        device.finish(result("r1"))
        device.finish(result("r2"))
        device.setFavorite("r1", true, 5)
        device.sync("anonymous-user") // the first account, before linking
        assertEquals(2, cloud.resultsOf("anonymous-user").size)

        // The player signs in with Google and lands on an existing account that has other data.
        cloud.results["google-user"] = mutableMapOf("old" to result("old", at = 1))
        device.sync("google-user")

        assertEquals(setOf("old", "r1", "r2"), cloud.resultsOf("google-user").keys) // merged, nothing lost
        assertEquals(true, cloud.favoritesOf("google-user")["r1"]?.isFavorite)
        // The device also learned about the old result, so its stats now include it.
        assertEquals(3, device.history().size)
    }

    @Test
    fun syncingAgainForTheSameAccountDoesNotQueueEverythingAgain() {
        val device = newDevice()
        device.finish(result("r1"))
        device.sync()
        device.sync()
        assertEquals(0, device.pendingCount())
    }

    @Test
    fun aNewDeviceRestoresTheHistoryFromTheCloud() {
        val phone = newDevice()
        phone.finish(result("r1", at = 1))
        phone.finish(result("r2", at = 2))
        phone.finish(result("r3", at = 3, outcome = Outcome.LOST))
        phone.setFavorite("r2", true, 50)
        phone.sync()

        val tablet = newDevice()
        val outcome = tablet.sync()

        assertIs<SyncResult.Success>(outcome)
        assertEquals(3, outcome.downloadedResults)
        assertEquals(listOf("r1", "r2", "r3"), tablet.history().map { it.id })
        assertTrue(tablet.isFavorite("r2"))
        assertEquals(0, tablet.pendingCount()) // what came from the cloud is not sent back
        assertEquals(0, Streaks.current(tablet.history())) // the last result was a loss, so the streak restores correctly
    }

    @Test
    fun theNewestFavoriteWinsBetweenDevices() {
        val phone = newDevice()
        phone.finish(result("r1"))
        phone.setFavorite("r1", true, now = 100)
        phone.sync()

        val tablet = newDevice()
        tablet.sync() // gets the favorite (t=100)
        tablet.setFavorite("r1", false, now = 200) // later, the heart is removed on the tablet
        tablet.sync()

        phone.sync() // the phone learns about it
        assertEquals(false, phone.isFavorite("r1"))
        assertEquals(false, cloud.favoritesOf("user-a")["r1"]?.isFavorite)
    }

    @Test
    fun anOlderCloudFavoriteDoesNotOverwriteANewerLocalOne() {
        val device = newDevice()
        device.finish(result("r1"))
        cloud.favorites["user-a"] = mutableMapOf("r1" to FavoriteRecord("r1", false, 100))
        device.setFavorite("r1", true, now = 500) // newer, still waiting in the queue

        device.sync()

        assertTrue(device.isFavorite("r1"))
        assertEquals(true, cloud.favoritesOf("user-a")["r1"]?.isFavorite)
    }

    @Test
    fun dailyResultsAreNeverUploadedByTheClient() {
        val device = newDevice()
        device.finish(result("daily1", mode = Mode.DAILY)) // would arrive from the server in phase 6
        device.finish(result("endless1"))
        device.sync("account-1")
        device.sync("account-2") // switching accounts queues everything again

        assertTrue(cloud.resultsOf("account-1").keys == setOf("endless1"))
        assertTrue(cloud.resultsOf("account-2").keys == setOf("endless1"))
    }
}
