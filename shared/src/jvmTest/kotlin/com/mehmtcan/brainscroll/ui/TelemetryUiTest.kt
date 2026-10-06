package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The anonymous counts as the app produces them, and the switch in the profile. */
@OptIn(ExperimentalTestApi::class)
class TelemetryUiTest {

    /** Events leave in a batch a few seconds after the last one: let that time pass on the test clock. */
    private fun ComposeUiTest.letEventsGoOut() {
        waitForIdle()
        waitUntil(timeoutMillis = 15_000) { true }
        mainClock.advanceTimeBy(4_000)
        waitForIdle()
    }

    private fun ComposeUiTest.waitForEvent(cloud: TestCloud, event: TelemetryEvent) =
        waitUntil(timeoutMillis = 15_000) { event in cloud.telemetry.names() }

    @Test
    fun opensAndTabViewsAreCountedWithoutAnyIdentity() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        openTab("Günlük", "Daily")
        waitForIdle()
        waitForEvent(cloud, TelemetryEvent.TabViewed)
        waitUntil(timeoutMillis = 15_000) { cloud.telemetry.events.any { it.props["tab"] == "daily" } }

        assertTrue(TelemetryEvent.AppOpen in cloud.telemetry.names())
        assertTrue(cloud.telemetry.events.any { it.event == TelemetryEvent.TabViewed && it.props["tab"] == "feed" })
        assertEquals("desktop", cloud.telemetry.infos.first().platform)
        // Whatever was sent holds no more than names and small values like the tab: nothing about the account.
        val everything = cloud.telemetry.events.joinToString { it.props.toString() + it.event.wireName }
        assertFalse(everything.contains("test-user"), "the user id must not appear anywhere")
    }

    @Test
    fun aDailyPuzzleIsCountedWhenStartedAndWhenFinished() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        openTab("Günlük", "Daily")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") }
        clickAny("Başla", "Start")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("ENTER", "GİR") }
        typeWord(if (hasAnyText("Günün bulmacası")) "KİTAP" else "CRANE")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForEvent(cloud, TelemetryEvent.DailyFinished)

        val started = cloud.telemetry.events.single { it.event == TelemetryEvent.DailyStarted }
        assertTrue(started.props["language"] in listOf("EN", "TR"))
        val finished = cloud.telemetry.events.single { it.event == TelemetryEvent.DailyFinished }
        assertEquals("WON", finished.props["outcome"])
        assertEquals("1", finished.props["guesses"])
    }

    @Test
    fun theSwitchInTheProfileStopsTheCounting() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        openTab("Profil", "Profile")
        waitForIdle()
        assertTrue(hasAnyText("Paylaşım açık", "Sharing is on"))
        snap("60_privacy_on")
        clickAny("Kapat", "Turn off")
        waitForIdle()
        assertTrue(hasAnyText("Paylaşım kapalı", "Sharing is off"))
        snap("61_privacy_off")

        val countBefore = cloud.telemetry.events.size
        openTab("Günlük", "Daily")
        waitForIdle()
        letEventsGoOut()
        assertEquals(countBefore, cloud.telemetry.events.size, "nothing is counted after turning it off")
        assertFalse(cloud.telemetry.events.any { it.props["tab"] == "daily" })

        // Turning it on again counts again.
        openTab("Profil", "Profile")
        waitForIdle()
        clickAny("Aç", "Turn on")
        waitForIdle()
        openTab("Sıralama", "Ranks")
        waitUntil(timeoutMillis = 15_000) { cloud.telemetry.events.any { it.props["tab"] == "ranks" } }
    }
}
