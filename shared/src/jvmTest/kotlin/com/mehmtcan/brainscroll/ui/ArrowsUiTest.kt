package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The arrow escape page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen
 * reader does: one button per arrow (on its head), saying whether it can leave.
 */
@OptIn(ExperimentalTestApi::class)
class ArrowsUiTest {

    private val games = listOf(GameKind.Word, GameKind.Arrows)

    private fun ComposeUiTest.openArrows(haptics: RecordingHaptics? = null, reduceMotion: Boolean? = null) {
        start(haptics = haptics, reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("arrowsCell").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.arrows(): List<SemanticsNodeInteraction> = onAllNodesWithTag("arrowsCell").let { all ->
        (0 until all.fetchSemanticsNodes().size).map { all[it] }
    }

    private fun ComposeUiTest.arrowCount() = onAllNodesWithTag("arrowsCell").fetchSemanticsNodes().size

    private fun SemanticsNodeInteraction.text() = fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()

    private fun String.canLeave() = contains("çıkabilir") || contains("can leave")

    private fun ComposeUiTest.tapFree() {
        arrows().first { it.text().canLeave() }.performClick()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    private fun ComposeUiTest.tapBlocked() {
        arrows().first { !it.text().canLeave() }.performClick()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Tüm oklar kaçtı!", "All arrows escaped!")

    @Test
    fun theBoardShowsTheArrowsTheMistakesAndTheTitle() = runComposeUiTest {
        openArrows()
        snap("310_arrows_start")
        val count = arrowCount()
        assertTrue(count in 5..11, "$count arrows")
        assertTrue(hasAnyText("Ok Kaçışı", "Arrow Escape"))
        assertTrue(hasAnyText("Kalan $count", "Left $count"))
        assertTrue(hasAnyText("Hata 0", "Mistakes 0"))
    }

    @Test
    fun anArrowWithAClearWayLeavesAndTheCounterFollows() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openArrows(haptics)
        val count = arrowCount()
        haptics.kinds.clear()
        tapFree()
        assertEquals(count - 1, arrowCount())
        assertTrue(hasAnyText("Kalan ${count - 1}", "Left ${count - 1}"))
        assertEquals(listOf(HapticKind.Light), haptics.kinds)
    }

    @Test
    fun anArrowInTheWayIsAMistakeWithABuzzAndStaysPut() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openArrows(haptics)
        val count = arrowCount()
        haptics.kinds.clear()
        tapBlocked()
        assertEquals(count, arrowCount())
        assertTrue(hasAnyText("Hata 1", "Mistakes 1"))
        assertEquals(HapticKind.Warning, haptics.kinds.last())
    }

    @Test
    fun aScreenReaderHearsDirectionRowColumnAndWhetherItCanLeave() = runComposeUiTest {
        openArrows()
        val texts = arrows().map { it.text() }
        assertTrue(texts.any { it.canLeave() }, "some arrow can leave: $texts")
        assertTrue(texts.any { it.contains("engelli") || it.contains("blocked") }, "some arrow is blocked: $texts")
        assertTrue(texts.all { Regex("\\d").containsMatchIn(it) }, "each says its row and column: $texts")
    }

    @Test
    fun restartPutsEveryArrowBack() = runComposeUiTest {
        openArrows()
        val count = arrowCount()
        repeat(2) { tapFree() }
        assertEquals(count - 2, arrowCount())
        onNodeWithTag("arrowsRestart").performClick()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertEquals(count, arrowCount())
    }

    @Test
    fun theWinBuzzesOnlyWhenTheLastArrowHasLeft() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openArrows(haptics)
        repeat(arrowCount() - 1) { tapFree() }
        haptics.kinds.clear()
        mainClock.autoAdvance = false
        arrows().single().performClick()
        mainClock.advanceTimeBy(100)
        assertTrue(HapticKind.Success !in haptics.kinds, "no success buzz while the last arrow is still leaving: ${haptics.kinds}")
        mainClock.advanceTimeBy(1_200)
        assertTrue(HapticKind.Success in haptics.kinds, "the success buzz comes once it has left: ${haptics.kinds}")
    }

    @Test
    fun withReducedMotionTheWinShowsQuickly() = runComposeUiTest {
        openArrows(reduceMotion = true)
        repeat(arrowCount() - 1) { tapFree() }
        mainClock.autoAdvance = false
        arrows().single().performClick()
        mainClock.advanceTimeBy(1_000)
        assertTrue(won())
    }

    @Test
    fun clearingEveryArrowWinsAndShowsTheTimeAndMistakes() = runComposeUiTest {
        openArrows()
        tapBlocked()
        repeat(arrowCount()) { tapFree() }
        snap("311_arrows_solved")
        assertTrue(won())
        assertTrue(hasAnyText("1 hata", "1 mistakes"))
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }
}
