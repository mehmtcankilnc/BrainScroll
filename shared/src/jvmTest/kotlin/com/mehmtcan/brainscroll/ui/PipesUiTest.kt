package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.pipes.PipesTileAngle
import com.mehmtcan.brainscroll.ui.pipes.PipesTileMask
import com.mehmtcan.brainscroll.ui.pipes.PipesTileOpenEnds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The pipes page in the feed, driven through the real screen. The board is random, so the tests read the tiles the way a
 * screen reader does and solve from that.
 */
@OptIn(ExperimentalTestApi::class)
class PipesUiTest {

    private val games = listOf(GameKind.Word, GameKind.Pipes)

    /** Starts the app and swipes from the first page (a word puzzle) to the second one (pipes). */
    private fun ComposeUiTest.openPipes(cloud: TestCloud = TestCloud(), haptics: RecordingHaptics? = null) {
        start(cloud, haptics = haptics, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("pipeTile").fetchSemanticsNodes().size == 25 }
    }

    /** The open sides of every tile. */
    private fun ComposeUiTest.tiles(): List<Int> =
        onAllNodesWithTag("pipeTile").fetchSemanticsNodes().map { it.config[PipesTileMask] }

    private fun ComposeUiTest.openEnds(): Int =
        onAllNodesWithTag("pipeTile").fetchSemanticsNodes().sumOf { it.config[PipesTileOpenEnds].countOneBits() }

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("pipeTile")[cell].performClick()
        mainClock.advanceTimeBy(300)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Tüm borular bağlandı!", "All pipes connected!")

    @Test
    fun theSecondPageIsABoardOfTwentyFiveTilesWithOpenEnds() = runComposeUiTest {
        openPipes()
        snap("160_pipes_start")
        assertEquals(25, tiles().size)
        assertEquals(24, tiles().sumOf { it.countOneBits() } / 2, "a tree: one pipe less than cells")
        assertTrue(openEnds() > 0)
        assertTrue(hasAnyText("Boru bağla", "Pipes"))
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
        assertTrue(onAllNodesWithTag("pipeTile").fetchSemanticsNodes().size == 25)
    }

    @Test
    fun aTapTurnsTheTileAQuarterClockwiseAndCountsAMove() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openPipes(haptics = haptics)
        val before = tiles()
        haptics.kinds.clear()
        tap(7)
        snap("161_pipes_turned")
        val after = tiles()
        assertEquals(PipesGenerator.rotate(before[7]), after[7])
        for (cell in before.indices) if (cell != 7) assertEquals(before[cell], after[cell])
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
        assertEquals(listOf(HapticKind.Light), haptics.kinds)
    }

    private fun ComposeUiTest.angle(cell: Int): Float =
        onAllNodesWithTag("pipeTile").fetchSemanticsNodes()[cell].config[PipesTileAngle]

    @Test
    fun aTurnGlidesFromTheOldPositionAndNeverShowsTheNewOneFirst() = runComposeUiTest {
        openPipes()
        mainClock.autoAdvance = false
        val before = angle(7)
        onAllNodesWithTag("pipeTile")[7].performClick()
        // The first frame of the turn is still the old position, then the angle glides up to a quarter and stops there.
        mainClock.advanceTimeBy(16)
        val first = angle(7)
        assertTrue(first >= before && first < before + 90f - 1f, "the first frame is near the old position: $first")
        mainClock.advanceTimeBy(60)
        val middle = angle(7)
        assertTrue(middle > first && middle < before + 90f, "in the middle of the turn: $middle")
        mainClock.advanceTimeBy(400)
        assertEquals(before + 90f, angle(7), "the turn ends on the quarter")
    }

    @Test
    fun aRestartDoesNotSpinTheTiles() = runComposeUiTest {
        openPipes()
        mainClock.autoAdvance = false
        onAllNodesWithTag("pipeTile")[7].performClick()
        mainClock.advanceTimeBy(500)
        val turned = angle(7)
        onNodeWithTag("pipesRestart").performClick()
        mainClock.advanceTimeBy(16)
        assertEquals(turned, angle(7), "the angle stays: the tile shows its first pipes without turning")
        mainClock.advanceTimeBy(500)
        assertEquals(turned, angle(7))
    }

    @Test
    fun restartPutsTheTilesBackAndKeepsTheMoves() = runComposeUiTest {
        openPipes()
        val before = tiles()
        tap(0)
        tap(6)
        onNodeWithTag("pipesRestart").performClick()
        mainClock.advanceTimeBy(300)
        waitForIdle()
        assertEquals(before, tiles())
        assertTrue(hasAnyText("Hamle 2", "Moves 2"))
    }

    @Test
    fun connectingEveryPipeShowsTheResultBuzzesOnceAndRecordsAWin() = runComposeUiTest {
        val cloud = TestCloud()
        val haptics = RecordingHaptics()
        openPipes(cloud, haptics)
        val turns = PipesGenerator.solve(tiles().toIntArray(), 5)!!
        haptics.kinds.clear()
        for (cell in turns.indices) repeat(turns[cell]) { tap(cell) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("162_pipes_won")
        assertTrue(won())
        assertEquals(0, openEnds())
        assertEquals(1, haptics.kinds.count { it == HapticKind.Success }, "winning is one success")
        waitUntil(timeoutMillis = 15_000) { cloud.telemetry.events.any { it.event == TelemetryEvent.EndlessFinished } }
        val event = cloud.telemetry.events.first { it.event == TelemetryEvent.EndlessFinished }
        assertEquals("pipes", event.props["game"])
        assertEquals("WON", event.props["outcome"])
        assertEquals(turns.sum().toString(), event.props["moves"])
    }
}
