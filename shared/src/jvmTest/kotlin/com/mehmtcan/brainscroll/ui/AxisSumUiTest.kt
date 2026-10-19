package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.axissum.AxisSumSolver
import com.mehmtcan.brainscroll.ui.axissum.AxisSumCellValue
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The axis sum page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class AxisSumUiTest {

    private val games = listOf(GameKind.Word, GameKind.AxisSum)

    private fun ComposeUiTest.openBoard(haptics: RecordingHaptics? = null, reduceMotion: Boolean? = null) {
        start(haptics = haptics, reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("axisSumCell").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.cells(): List<SemanticsNodeInteraction> = onAllNodesWithTag("axisSumCell").let { all -> (0 until all.fetchSemanticsNodes().size).map { all[it] } }

    private fun ComposeUiTest.values() = onAllNodesWithTag("axisSumCell").fetchSemanticsNodes().map { it.config[AxisSumCellValue] }

    private fun ComposeUiTest.targets(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes()
        .map { it.config.getOrNull(SemanticsProperties.Text)?.joinToString("")?.toInt() ?: error("no number on a target") }

    private fun ComposeUiTest.tap(cell: Int) {
        cells()[cell].performClick()
        mainClock.advanceTimeBy(500)
        waitForIdle()
    }

    /** The cells to erase, worked out from the numbers and the sums on the screen. */
    private fun ComposeUiTest.toErase(): List<Int> {
        val digits = values().map { kotlin.math.abs(it) }
        val size = targets("axisRowTarget").size
        val way = assertNotNull(AxisSumSolver.solutions(size, digits, targets("axisRowTarget"), targets("axisColumnTarget"), limit = 1).firstOrNull())
        return way.indices.filter { !way[it] }
    }

    private fun ComposeUiTest.won() = hasAnyText("Bütün toplamlar tamam!", "Every sum is right!")

    @Test
    fun theBoardShowsTheNumbersTheSumsAndTheCounters() = runComposeUiTest {
        openBoard()
        snap("320_axissum_start")
        val size = targets("axisRowTarget").size
        assertEquals(size * size, cells().size)
        assertEquals(size, targets("axisColumnTarget").size)
        assertTrue(hasAnyText("Eksen Toplamı", "Axis Sum"))
        assertTrue(hasAnyText("Dokunuş 0", "Taps 0"))
        assertTrue(values().all { it > 0 }, "nothing is erased at the start")
    }

    @Test
    fun aTapErasesANumberAndAnotherBringsItBack() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openBoard(haptics)
        val before = values()
        haptics.kinds.clear()
        tap(0)
        assertEquals(-before[0], values()[0])
        assertEquals(listOf(HapticKind.Light), haptics.kinds)
        assertTrue(hasAnyText("Dokunuş 1", "Taps 1"))
        tap(0)
        assertEquals(before, values())
    }

    @Test
    fun restartBringsEveryNumberBack() = runComposeUiTest {
        openBoard()
        val before = values()
        tap(0)
        tap(1)
        onNodeWithTag("axisSumRestart").performClick()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertEquals(before, values())
    }

    @Test
    fun erasingTheRightNumbersWinsAndShowsTheTapsAndTheTime() = runComposeUiTest {
        openBoard()
        val cells = toErase()
        cells.forEach { tap(it) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("321_axissum_solved")
        assertTrue(won())
        assertTrue(hasAnyText("${cells.size} dokunuş", "${cells.size} taps"))
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun theWinBuzzesOnlyAfterTheNumbersHaveTurnedGreen() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openBoard(haptics)
        val cells = toErase()
        cells.dropLast(1).forEach { tap(it) }
        haptics.kinds.clear()
        mainClock.autoAdvance = false
        cells()[cells.last()].performClick()
        mainClock.advanceTimeBy(100)
        assertTrue(HapticKind.Success !in haptics.kinds, "no success buzz yet: ${haptics.kinds}")
        mainClock.advanceTimeBy(1_000)
        assertTrue(HapticKind.Success in haptics.kinds, "the success buzz comes after: ${haptics.kinds}")
    }

    @Test
    fun withReducedMotionTheWinShowsQuickly() = runComposeUiTest {
        openBoard(reduceMotion = true)
        val cells = toErase()
        cells.forEach { tap(it) }
        mainClock.advanceTimeBy(600)
        assertTrue(won())
    }

    @Test
    fun theBoardFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.AxisSum), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("322_axissum_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("axisSumCell", "axisRowTarget", "axisColumnTarget", "axisSumRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }
}
