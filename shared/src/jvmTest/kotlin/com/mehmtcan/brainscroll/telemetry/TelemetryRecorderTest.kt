package com.mehmtcan.brainscroll.telemetry

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.prepareDatabase
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryRecorderTest {

    private val api = FakeTelemetryApi()
    private val signedIn = MutableStateFlow(true)
    private val info = PlatformInfo("android", "1.0 (7)")

    private fun repository(): GameRepository {
        val file = File.createTempFile("telemetry-test", ".db").also { it.deleteOnExit() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { prepareDatabase(it) }
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun TestScope.recorder(repository: GameRepository = repository()): TelemetryRecorder =
        TelemetryRecorder(repository, api, info, backgroundScope, signedIn, flushDelayMillis = 1_000).also {
            it.start()
            testScheduler.runCurrent() // lets the collector of the session start
        }

    /** Lets all waiting work run, including delays. (advanceUntilIdle does not run work of the background scope.) */
    private fun TestScope.settle() {
        advanceTimeBy(10_000)
        testScheduler.runCurrent()
    }

    @Test
    fun eventsGoOutTogetherAfterAShortWait() = runTest {
        val recorder = recorder()
        settle()
        recorder.event(TelemetryEvent.AppOpen)
        recorder.event(TelemetryEvent.TabViewed, mapOf("tab" to "daily"))
        advanceTimeBy(500)
        assertTrue(api.events.isEmpty(), "still waiting for more events to go with it")
        advanceTimeBy(1_000)
        assertEquals(listOf(TelemetryEvent.AppOpen, TelemetryEvent.TabViewed), api.names())
        assertEquals(listOf(2), api.batchSizes, "one request, not two")
        assertEquals(mapOf("tab" to "daily"), api.events[1].props)
        assertEquals(info, api.infos.single())
    }

    @Test
    fun withoutASessionEventsWaitAndGoWhenThereIsOne() = runTest {
        signedIn.value = false
        val recorder = recorder()
        settle()
        recorder.event(TelemetryEvent.AppOpen)
        advanceTimeBy(5_000)
        assertTrue(api.events.isEmpty())
        signedIn.value = true
        settle()
        assertEquals(listOf(TelemetryEvent.AppOpen), api.names())
        signedIn.value = true
    }

    @Test
    fun aBatchHoldsAtMostFiftyEvents() = runTest {
        val recorder = recorder()
        settle()
        repeat(120) { recorder.event(TelemetryEvent.TabViewed) }
        settle()
        assertEquals(listOf(50, 50, 20), api.batchSizes)
    }

    @Test
    fun whenSendingFailsTheEventsAreKeptAndGoLater() = runTest {
        api.failure = IOException("offline")
        val recorder = recorder()
        settle()
        recorder.event(TelemetryEvent.AppOpen)
        settle()
        assertTrue(api.events.isEmpty())

        api.failure = null
        recorder.event(TelemetryEvent.TabViewed)
        settle()
        assertEquals(listOf(TelemetryEvent.AppOpen, TelemetryEvent.TabViewed), api.names(), "the old one went along, in order")
    }

    @Test
    fun aPhoneThatStaysOfflineKeepsOnlyTheNewestTwoHundred() = runTest {
        signedIn.value = false
        val recorder = recorder()
        settle()
        repeat(260) { recorder.event(TelemetryEvent.TabViewed, mapOf("n" to it.toString())) }
        signedIn.value = true
        settle()
        assertEquals(200, api.events.size)
        assertEquals("60", api.events.first().props["n"], "the oldest were dropped")
        assertEquals("259", api.events.last().props["n"])
    }

    @Test
    fun turningItOffStopsEverythingForgetsWhatWaitedAndIsRemembered() = runTest {
        val repository = repository()
        signedIn.value = false
        val recorder = recorder(repository)
        settle()
        recorder.event(TelemetryEvent.AppOpen)
        recorder.recordCrash(CrashReport("K", "m", "s"))
        assertNotNull(repository.pendingCrash())

        recorder.setEnabled(false)
        assertFalse(recorder.enabled.value)
        assertNull(repository.pendingCrash(), "a waiting crash is forgotten too")
        recorder.event(TelemetryEvent.TabViewed)
        recorder.recordCrash(CrashReport("K", "m", "s"))
        assertNull(repository.pendingCrash(), "nothing new is kept")
        signedIn.value = true
        settle()
        assertTrue(api.events.isEmpty() && api.crashes.isEmpty(), "and nothing is sent")

        // The choice survives a restart.
        assertFalse(TelemetryRecorder(repository, api, info, backgroundScope, signedIn).enabled.value)
        recorder.setEnabled(true)
        assertTrue(TelemetryRecorder(repository, api, info, backgroundScope, signedIn).enabled.value)
    }

    @Test
    fun aCrashIsKeptOnTheDeviceAndSentAtTheNextStartOnce() = runTest {
        val repository = repository()
        // The app dies: the crash is written down, nothing can be sent.
        TelemetryRecorder(repository, api, info, backgroundScope, MutableStateFlow(false))
            .recordCrash(CrashReport("java.lang.IllegalStateException", "boom", "at a.B.c(B.kt:1)"))
        assertTrue(api.crashes.isEmpty())

        // Next start, with a session.
        recorder(repository)
        settle()
        assertEquals(CrashReport("java.lang.IllegalStateException", "boom", "at a.B.c(B.kt:1)"), api.crashes.single())
        assertNull(repository.pendingCrash(), "sent, so removed")

        recorder(repository)
        settle()
        assertEquals(1, api.crashes.size, "never sent twice")
    }

    @Test
    fun aCrashThatCouldNotBeSentIsKeptForTheNextStart() = runTest {
        val repository = repository()
        TelemetryRecorder(repository, api, info, backgroundScope, MutableStateFlow(false)).recordCrash(CrashReport("K", "m", "s"))
        api.failure = IOException("offline")
        recorder(repository)
        settle()
        assertNotNull(repository.pendingCrash())
        api.failure = null
        recorder(repository)
        settle()
        assertEquals(1, api.crashes.size)
    }

    @Test
    fun aCrashFromBeforeOptingOutIsNotSent() = runTest {
        val repository = repository()
        TelemetryRecorder(repository, api, info, backgroundScope, MutableStateFlow(false)).recordCrash(CrashReport("K", "m", "s"))
        repository.setTelemetryEnabled(false) // turned off in another way, for example by an older version
        recorder(repository)
        settle()
        assertTrue(api.crashes.isEmpty())
        assertNull(repository.pendingCrash())
    }

    @Test
    fun anUnreadableStoredCrashIsDropped() = runTest {
        val repository = repository()
        repository.putPendingCrash("not json")
        recorder(repository)
        settle()
        assertNull(repository.pendingCrash())
        assertTrue(api.crashes.isEmpty())
    }

    @Test
    fun aCrashReportIsCutToTheServersLimits() {
        val error = IllegalStateException("x".repeat(900))
        val report = crashReportOf(error)
        assertEquals("java.lang.IllegalStateException", report.kind)
        assertEquals(500, report.message.length)
        assertTrue(report.stack.length <= 8000)
        assertTrue(report.stack.contains("aCrashReportIsCutToTheServersLimits"), "the stack names where it happened")
    }
}
