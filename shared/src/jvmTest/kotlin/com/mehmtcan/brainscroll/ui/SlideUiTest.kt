package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.slide.SlideSolver
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.slide.SlideTileCell
import com.mehmtcan.brainscroll.ui.slide.SlideTileNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The sliding puzzle page in the feed, driven through the real screen. The board is random, so the tests read the tiles
 * the way a screen reader does and solve from that.
 */
@OptIn(ExperimentalTestApi::class)
class SlideUiTest {

    private val games = listOf(GameKind.Word, GameKind.Slide)

    /** Starts the app and swipes from the first page (a word puzzle) to the second one (the sliding puzzle). */
    private fun ComposeUiTest.openSlide(cloud: TestCloud = TestCloud(), haptics: RecordingHaptics? = null) {
        start(cloud, haptics = haptics, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("slideTile").fetchSemanticsNodes().size == 11 }
    }

    /** The number in every cell (0 for the empty one), read from the tiles on screen. */
    private fun ComposeUiTest.board(): List<Int> {
        val tiles = IntArray(12)
        for (node in onAllNodesWithTag("slideTile").fetchSemanticsNodes()) {
            tiles[node.config[SlideTileCell]] = node.config[SlideTileNumber]
        }
        return tiles.toList()
    }

    private fun ComposeUiTest.tapCell(cell: Int) {
        val number = board()[cell]
        val index = onAllNodesWithTag("slideTile").fetchSemanticsNodes().indexOfFirst { it.config[SlideTileNumber] == number }
        onAllNodesWithTag("slideTile")[index].performClick()
        mainClock.advanceTimeBy(400)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Bulmaca çözüldü!", "Puzzle solved!")

    private fun List<Int>.nextToBlank(): Int {
        val blank = indexOf(0)
        return if (blank % 3 > 0) blank - 1 else blank + 1
    }

    @Test
    fun theSecondPageIsABoardOfElevenTilesAndAnEmptySpace() = runComposeUiTest {
        openSlide()
        snap("140_slide_start")
        assertEquals((0..11).toList(), board().sorted())
        assertTrue(hasAnyText("Kayan bulmaca", "Sliding puzzle"))
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
    }

    @Test
    fun aTapOnATileNextToTheSpaceSlidesItAndCountsAMove() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openSlide(haptics = haptics)
        val before = board()
        val cell = before.nextToBlank()
        haptics.kinds.clear()
        tapCell(cell)
        snap("141_slide_moved")
        val after = board()
        assertEquals(0, after[cell])
        assertEquals(before[cell], after[before.indexOf(0)])
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
        assertEquals(listOf(HapticKind.Light), haptics.kinds)
    }

    @Test
    fun aTapOnATileNotInLineWithTheSpaceDoesNothing() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openSlide(haptics = haptics)
        val before = board()
        val blank = before.indexOf(0)
        val off = (0 until 12).first { it / 3 != blank / 3 && it % 3 != blank % 3 }
        haptics.kinds.clear()
        tapCell(off)
        assertEquals(before, board())
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
        assertTrue(haptics.kinds.isEmpty())
    }

    @Test
    fun restartPutsTheTilesBackAndKeepsTheMoves() = runComposeUiTest {
        openSlide()
        val before = board()
        tapCell(before.nextToBlank())
        onNodeWithTag("slideRestart").performClick()
        mainClock.advanceTimeBy(400)
        waitForIdle()
        assertEquals(before, board())
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
    }

    @Test
    fun solvingThePuzzleShowsTheResultBuzzesOnceAndRecordsAWin() = runComposeUiTest {
        val cloud = TestCloud()
        val haptics = RecordingHaptics()
        openSlide(cloud, haptics)
        val taps = SlideSolver.solve(board(), 3, 4)
        haptics.kinds.clear()
        for (cell in taps) tapCell(cell)
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("142_slide_won")
        assertTrue(won())
        assertEquals((1..11).toList() + 0, board())
        assertEquals(1, haptics.kinds.count { it == HapticKind.Success }, "winning is one success")
        waitUntil(timeoutMillis = 15_000) { cloud.telemetry.events.any { it.event == TelemetryEvent.EndlessFinished } }
        val event = cloud.telemetry.events.first { it.event == TelemetryEvent.EndlessFinished }
        assertEquals("slide", event.props["game"])
        assertEquals("WON", event.props["outcome"])
        assertEquals(taps.size.toString(), event.props["moves"])
    }
}
