package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.daily.DailyException
import com.mehmtcan.brainscroll.daily.FakeDailyApi
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The daily puzzle tab, driven through the real screen with a fake server. */
@OptIn(ExperimentalTestApi::class)
class DailyUiTest {

    /** Scrolls the profile list until one of the texts is on screen. */
    private fun ComposeUiTest.scrollProfileTo(vararg texts: String) {
        onNode(hasScrollAction()).performScrollToNode(texts.map { hasText(it, substring = true) }.reduce { a, b -> a or b })
        waitForIdle()
    }

    private fun ComposeUiTest.openDaily() {
        openTab("Günlük", "Daily")
        waitForIdle()
    }

    /** Daily tab, in the language the fake server was set up for, started. */
    private fun ComposeUiTest.startDaily(cloud: TestCloud) {
        start(cloud)
        openDaily()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") }
        clickAny("Başla", "Start")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("ENTER", "GİR") }
    }

    /** The screen language follows the device, so the puzzle language does too. Pick the one in use. */
    private fun ComposeUiTest.puzzleLanguage() = if (hasAnyText("Günün bulmacası")) Language.TR else Language.EN

    @Test
    fun theTabOpensOnAStartScreenAndTheClockIsNotRunning() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        openDaily()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") }
        snap("13_daily_intro")

        assertTrue(hasAnyText("Günün bulmacası", "Today's puzzle"))
        assertTrue(cloud.daily.calls.none { it == "start" }, "opening the tab must not start the clock")
        assertFalse(hasAnyText("ENTER", "GİR"), "no keyboard before Start")
    }

    @Test
    fun pressingStartStartsTheAttemptAndShowsTheKeyboardAndATimer() = runComposeUiTest {
        val cloud = TestCloud()
        startDaily(cloud)
        snap("14_daily_playing")
        assertEquals(1, cloud.daily.calls.count { it == "start" })
        assertTrue(hasAnyText("Süre 00:", "Time 00:"))
    }

    @Test
    fun aGuessIsSentAndShownColoredByTheServer() = runComposeUiTest {
        val cloud = TestCloud()
        startDaily(cloud)
        val language = puzzleLanguage()
        val word = if (language == Language.TR) "KİTAP" else "SLATE"
        typeWord(word)
        waitUntil(timeoutMillis = 5_000) { cloud.daily.calls.any { it.startsWith("guess:") } }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("15_daily_after_guess")
        assertTrue(cloud.daily.calls.any { it.startsWith("guess:") })
    }

    @Test
    fun aTypoIsCaughtOnThePhoneAndNothingIsSent() = runComposeUiTest {
        val cloud = TestCloud()
        startDaily(cloud)
        "ZZZZZ".forEach { key(it) }
        enter()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Kelime listesinde yok", "Not in word list") }
        assertTrue(cloud.daily.calls.none { it.startsWith("guess:") })
    }

    @Test
    fun winningShowsTheResultTheTimeAndTheCountdown() = runComposeUiTest {
        val cloud = TestCloud()
        startDaily(cloud)
        cloud.daily.clock += 83_000 // the attempt took 1:23 on the server's clock
        val answer = if (puzzleLanguage() == Language.TR) "KİTAP" else "CRANE"
        typeWord(answer)
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("16_daily_won")

        assertTrue(hasAnyText("Süre 01:23", "Time 01:23"))
        assertFalse(hasAnyText("ENTER", "GİR"), "the keyboard is replaced by the result")
    }

    @Test
    fun theWinCountsForTheDayStreakInTheHeader() = runComposeUiTest {
        val cloud = TestCloud()
        startDaily(cloud)
        typeWord(if (puzzleLanguage() == Language.TR) "KİTAP" else "CRANE")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()
        // The flame chip shows the streak: one day.
        assertTrue(onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun aSolvedDailyPuzzleShowsUpInTheProfileWithItsTimeAndAsAFavorite() = runComposeUiTest {
        val cloud = TestCloud()
        startDaily(cloud)
        cloud.daily.clock += 83_000 // took 1:23 on the server's clock
        typeWord(if (puzzleLanguage() == Language.TR) "KİTAP" else "CRANE")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()

        onAllNodesWithTag("favoriteHeart")[0].performClick() // heart the daily result
        waitForIdle()

        openTab("Profil", "Profile")
        waitForIdle()
        snap("18_profile_with_daily")

        // The profile is a long list: only what is on screen exists, so scroll to each thing before looking for it.
        scrollProfileTo("En iyi süre: 01:23", "Best time: 01:23")
        assertTrue(hasAnyText("En iyi süre: 01:23", "Best time: 01:23"), "the best daily time is shown")
        scrollProfileTo("Günlük bulmaca · 01:23", "Daily puzzle · 01:23")
        assertTrue(hasAnyText("Günlük bulmaca · 01:23", "Daily puzzle · 01:23"), "the favorite row is marked as daily")
        assertFalse(hasAnyText("Henüz favori yok", "No favorites yet"))
    }

    @Test
    fun offlineShowsAMessageAndTryAgainBringsTheScreenBack() = runComposeUiTest {
        val cloud = TestCloud()
        cloud.daily.failure = DailyException.Offline()
        start(cloud)
        openDaily()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("internet bağlantısı gerekir", "needs an internet connection") }
        snap("17_daily_offline")

        cloud.daily.failure = null
        clickAny("Tekrar dene", "Try again")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") }
    }

    @Test
    fun theOtherLanguageIsItsOwnPuzzle() = runComposeUiTest {
        val cloud = TestCloud()
        startDaily(cloud)
        val here = puzzleLanguage()
        val other = if (here == Language.TR) "EN" else "TR"
        clickAny(other)
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") } // the other language was not started
        assertFalse(hasAnyText("ENTER", "GİR"))
    }

    @Test
    fun anAttemptAlreadyInProgressIsPickedUp() = runComposeUiTest {
        val cloud = TestCloud(daily = FakeDailyApi().also { it.prefill(Language.EN, listOf("SLATE"));  it.prefill(Language.TR, listOf("KIRIK")) })
        start(cloud)
        openDaily()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("ENTER", "GİR") } // straight into the puzzle, no Start button
        assertFalse(hasAnyText("Başla", "Start"))
        assertEquals(0, cloud.daily.calls.count { it == "start" })
    }
}
