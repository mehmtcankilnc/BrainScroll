package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The celebration (docs/design.md section 6): confetti for a won daily puzzle, never twice, never with reduced motion. */
@OptIn(ExperimentalTestApi::class)
class MotionUiTest {

    private fun ComposeUiTest.startDaily(cloud: TestCloud, reduceMotion: Boolean) {
        start(cloud, reduceMotion = reduceMotion)
        openTab("Günlük", "Daily")
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") }
        clickAny("Başla", "Start")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("ENTER", "GİR") }
    }

    private fun ComposeUiTest.confettiCount() = onAllNodesWithTag("confetti").fetchSemanticsNodes().size

    private fun ComposeUiTest.answer() = if (hasAnyText("Günün bulmacası")) "KİTAP" else "CRANE"

    @Test
    fun winningTheDailyPuzzleThrowsConfettiThatFallsAway() = runComposeUiTest {
        startDaily(TestCloud(), reduceMotion = false)
        mainClock.autoAdvance = false
        typeWord(answer())
        // The answer comes from the fake server on the next frames; let it arrive, then watch the burst.
        mainClock.advanceTimeBy(600)
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        assertEquals(1, confettiCount(), "confetti is on screen")
    }

    @Test
    fun reducedMotionHasNoConfetti() = runComposeUiTest {
        startDaily(TestCloud(), reduceMotion = true)
        typeWord(answer())
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()
        assertEquals(0, confettiCount())
        assertTrue(hasAnyText("Süre", "Time"), "the result still shows")
    }

    @Test
    fun aLostPuzzleHasNoConfetti() = runComposeUiTest {
        startDaily(TestCloud(), reduceMotion = false)
        val wrong = if (hasAnyText("Günün bulmacası")) "KIRIK" else "SLATE"
        repeat(6) { typeWord(wrong); waitForIdle() }
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()
        assertEquals(0, confettiCount())
    }

    @Test
    fun aPuzzleFinishedEarlierDoesNotCelebrateAgain() = runComposeUiTest {
        val cloud = TestCloud()
        cloud.daily.prefill(Language.TR, listOf("KİTAP"))
        cloud.daily.prefill(Language.EN, listOf("CRANE"))
        start(cloud, reduceMotion = false)
        openTab("Günlük", "Daily")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()
        assertEquals(0, confettiCount())
    }
}
