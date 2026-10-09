package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.lights.LightValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Lights Out page in the feed, driven through the real screen. The board is random, so the tests read the lights
 * the way a screen reader does. Solving uses the fact that a 5x5 board is a linear puzzle over two states: the set of
 * cells to tap is found here by trying every combination of the first row and chasing the lights down.
 */
@OptIn(ExperimentalTestApi::class)
class LightsUiTest {

    private val games = listOf(GameKind.Word, GameKind.Lights)

    /** Starts the app and swipes from the first page (a word puzzle) to the second one (Lights Out). */
    private fun ComposeUiTest.openLights(cloud: TestCloud = TestCloud(), haptics: RecordingHaptics? = null) {
        start(cloud, haptics = haptics, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("lightCell").fetchSemanticsNodes().size == 25 }
    }

    private fun ComposeUiTest.lit(): List<Boolean> =
        onAllNodesWithTag("lightCell").fetchSemanticsNodes().map { it.config[LightValue] == 1 }

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("lightCell")[cell].performClick()
        waitForIdle()
    }

    /** A set of cells whose taps put out every light (the first row picks, the rest follow by chasing). */
    private fun solution(start: List<Boolean>): List<Int> {
        for (mask in 0 until 32) {
            val board = start.toBooleanArray()
            val taps = mutableListOf<Int>()
            fun press(cell: Int) {
                taps += cell
                val r = cell / 5
                val c = cell % 5
                board[cell] = !board[cell]
                if (r > 0) board[cell - 5] = !board[cell - 5]
                if (r < 4) board[cell + 5] = !board[cell + 5]
                if (c > 0) board[cell - 1] = !board[cell - 1]
                if (c < 4) board[cell + 1] = !board[cell + 1]
            }
            for (c in 0 until 5) if (mask and (1 shl c) != 0) press(c)
            for (r in 1 until 5) for (c in 0 until 5) if (board[(r - 1) * 5 + c]) press(r * 5 + c)
            if (board.none { it }) return taps
        }
        error("no solution")
    }

    private fun ComposeUiTest.won() = hasAnyText("Tüm ışıklar söndü!", "All lights out!")

    @Test
    fun theSecondPageIsABoardOfTwentyFiveLightsWithSomeOn() = runComposeUiTest {
        openLights()
        snap("130_lights_start")
        assertEquals(25, onAllNodesWithTag("lightCell").fetchSemanticsNodes().size)
        assertTrue(lit().count { it } >= 4)
        assertTrue(hasAnyText("Işıkları söndür", "Lights out"))
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
        assertTrue(hasAnyText("En çok 7", "Within 7"))
    }

    @Test
    fun aTapSwitchesTheLightAndItsNeighborsAndCountsAMove() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openLights(haptics = haptics)
        val before = lit()
        haptics.kinds.clear()
        tap(12)
        snap("131_lights_tapped")
        val after = lit()
        assertEquals(setOf(12, 7, 17, 11, 13), after.indices.filter { after[it] != before[it] }.toSet())
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
        assertEquals(listOf(HapticKind.Light), haptics.kinds)
    }

    @Test
    fun restartPutsTheLightsBackAndKeepsTheMoves() = runComposeUiTest {
        openLights()
        val before = lit()
        tap(0)
        tap(6)
        onNodeWithTag("lightsRestart").performClick()
        waitForIdle()
        assertEquals(before, lit())
        assertTrue(hasAnyText("Hamle 2", "Moves 2"))
    }

    @Test
    fun puttingOutTheLastLightShowsTheResultBuzzesOnceAndRecordsAWin() = runComposeUiTest {
        val cloud = TestCloud()
        val haptics = RecordingHaptics()
        openLights(cloud, haptics)
        val taps = solution(lit())
        haptics.kinds.clear()
        for (cell in taps) tap(cell)
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("132_lights_won")
        assertTrue(won())
        assertTrue(lit().none { it })
        assertEquals(1, haptics.kinds.count { it == HapticKind.Success }, "winning is one success")
        waitUntil(timeoutMillis = 15_000) { cloud.telemetry.events.any { it.event == TelemetryEvent.EndlessFinished } }
        val event = cloud.telemetry.events.first { it.event == TelemetryEvent.EndlessFinished }
        assertEquals("lights", event.props["game"])
        assertEquals("WON", event.props["outcome"])
        assertEquals(taps.size.toString(), event.props["moves"])
    }
}
