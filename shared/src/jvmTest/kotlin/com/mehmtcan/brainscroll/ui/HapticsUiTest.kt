package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** When the phone buzzes (docs/design.md section 6), seen through a recording stand-in for the vibration motor. */
@OptIn(ExperimentalTestApi::class)
class HapticsUiTest {

    private fun androidx.compose.ui.test.ComposeUiTest.startDaily(cloud: TestCloud, haptics: RecordingHaptics) {
        start(cloud, haptics)
        openTab("Günlük", "Daily")
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") }
        clickAny("Başla", "Start")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("ENTER", "GİR") }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.answer() = if (hasAnyText("Günün bulmacası")) "KİTAP" else "CRANE"

    @Test
    fun everyKeyPressIsALightTap() = runComposeUiTest {
        val haptics = RecordingHaptics()
        startDaily(TestCloud(), haptics)
        haptics.kinds.clear()
        "CRA".forEach { key(it) }
        assertEquals(List(3) { HapticKind.Light }, haptics.kinds)
    }

    @Test
    fun switchingTabsIsALightTapButTappingTheOpenTabIsNot() = runComposeUiTest {
        val haptics = RecordingHaptics()
        start(TestCloud(), haptics)
        haptics.kinds.clear()
        openTab("Profil", "Profile")
        waitForIdle()
        assertEquals(listOf(HapticKind.Light), haptics.kinds)
        openTab("Profil", "Profile")
        waitForIdle()
        assertEquals(1, haptics.kinds.size, "the tab was already open")
    }

    @Test
    fun aRejectedGuessIsAWarning() = runComposeUiTest {
        val haptics = RecordingHaptics()
        startDaily(TestCloud(), haptics)
        "ZZZZZ".forEach { key(it) }
        haptics.kinds.clear()
        enter() // not in the word list
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Kelime listesinde yok", "Not in word list") }
        assertEquals(listOf(HapticKind.Light, HapticKind.Warning), haptics.kinds, "the Enter key tap, then the warning")
    }

    @Test
    fun winningTheDailyPuzzleIsASuccess() = runComposeUiTest {
        val cloud = TestCloud()
        val haptics = RecordingHaptics()
        startDaily(cloud, haptics)
        haptics.kinds.clear()
        typeWord(answer())
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()
        assertTrue(HapticKind.Success in haptics.kinds, haptics.kinds.toString())
        assertEquals(1, haptics.kinds.count { it == HapticKind.Success })
    }

    @Test
    fun losingIsAWarning() = runComposeUiTest {
        val haptics = RecordingHaptics()
        startDaily(TestCloud(), haptics)
        val wrong = if (hasAnyText("Günün bulmacası")) "KIRIK" else "SLATE"
        repeat(6) { typeWord(wrong); waitForIdle() }
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        assertTrue(HapticKind.Warning in haptics.kinds, haptics.kinds.toString())
        assertTrue(HapticKind.Success !in haptics.kinds)
    }

    @Test
    fun aPuzzleThatWasAlreadyFinishedStaysQuiet() = runComposeUiTest {
        val cloud = TestCloud()
        // Finished on another phone: the daily puzzle opens straight on the result.
        cloud.daily.prefill(Language.TR, listOf("KİTAP"))
        cloud.daily.prefill(Language.EN, listOf("CRANE"))
        val haptics = RecordingHaptics()
        start(cloud, haptics)
        haptics.kinds.clear()
        openTab("Günlük", "Daily")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()
        assertTrue(HapticKind.Success !in haptics.kinds, "opening a finished puzzle must not celebrate again: ${haptics.kinds}")
    }

    @Test
    fun theHeartIsALightTap() = runComposeUiTest {
        val cloud = TestCloud()
        val haptics = RecordingHaptics()
        startDaily(cloud, haptics)
        typeWord(answer())
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()
        haptics.kinds.clear()
        onAllNodesWithTag("favoriteHeart")[0].performClick()
        waitForIdle()
        assertEquals(listOf(HapticKind.Light), haptics.kinds)
    }
}
