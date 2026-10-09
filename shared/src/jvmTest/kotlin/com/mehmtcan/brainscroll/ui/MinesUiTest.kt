package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.mines.MinesGame
import com.mehmtcan.brainscroll.game.mines.MinesSolver
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.mines.CellValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The minesweeper page in the feed, driven through the real screen. The board is random, so the tests read the cells
 * the way a screen reader does and play from that, with the same logic solver the board generator uses.
 */
@OptIn(ExperimentalTestApi::class)
class MinesUiTest {

    private val games = listOf(GameKind.Word, GameKind.Mines)
    private val cells = MinesGame.DEFAULT_ROWS * MinesGame.DEFAULT_COLS

    /** Starts the app and swipes from the first page (a word puzzle) to the second one (minesweeper). */
    private fun ComposeUiTest.openMines(cloud: TestCloud = TestCloud(), haptics: RecordingHaptics? = null) {
        start(cloud, haptics = haptics, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { cellCount() == cells }
    }

    private fun ComposeUiTest.cellCount() = onAllNodesWithTag("mineCell").fetchSemanticsNodes().size

    /** What every cell shows: -1 closed, -2 flagged, 0..8 open, -3 and -4 mines after a loss. */
    private fun ComposeUiTest.values(): List<Int> =
        onAllNodesWithTag("mineCell").fetchSemanticsNodes().map { it.config[CellValue] }

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("mineCell")[cell].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.longPress(cell: Int) {
        onAllNodesWithTag("mineCell")[cell].performTouchInput { longClick() }
        waitForIdle()
    }

    private fun ComposeUiTest.doubleTap(cell: Int) {
        onAllNodesWithTag("mineCell")[cell].performTouchInput { doubleClick() }
        waitForIdle()
    }

    /** The cells that logic proves safe right now (closed, and certain). */
    private fun ComposeUiTest.safeCells(): List<Int> {
        val numbers = values().map { if (it >= 0) it else -1 }.toIntArray()
        return MinesSolver.deduce(MinesGame.DEFAULT_ROWS, MinesGame.DEFAULT_COLS, numbers).safe.filter { values()[it] == -1 }
    }

    private fun ComposeUiTest.solve() {
        tap(27)
        var guard = 0
        while (values().any { it == -1 || it == -2 } && !cleared() && guard++ < 80) {
            val safe = safeCells()
            check(safe.isNotEmpty()) { "the board needed a guess: ${values()}" }
            safe.forEach { tap(it) }
        }
    }

    private fun ComposeUiTest.cleared() = hasAnyText("Tahta temizlendi!", "Board cleared!")

    private fun ComposeUiTest.lost() = hasAnyText("Mayına bastın", "You hit a mine")

    @Test
    fun theSecondPageIsABoardOfSixtyFourClosedCellsAndTenMines() = runComposeUiTest {
        openMines()
        snap("100_mines_start")
        assertEquals(64, cellCount())
        assertTrue(values().all { it == -1 })
        assertTrue(hasAnyText("Mayın tarlası", "Minesweeper"))
        assertTrue(hasAnyText("Mayın 10", "Mines 10"))
    }

    @Test
    fun theFirstTapIsSafeAndOpensAnArea() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openMines(haptics = haptics)
        haptics.kinds.clear()
        tap(27)
        snap("101_mines_opened")
        val shown = values()
        assertEquals(0, shown[27], "the first cell has no mine around it")
        assertTrue(shown.count { it >= 0 } > 1, "so an area opened")
        assertEquals(listOf(HapticKind.Light), haptics.kinds, "an open is a light tap")
    }

    @Test
    fun aLongPressFlagsAndAnotherOneTakesTheFlagAway() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openMines(haptics = haptics)
        haptics.kinds.clear()
        longPress(5)
        assertEquals(-2, values()[5])
        assertTrue(hasAnyText("Mayın 9", "Mines 9"))
        assertEquals(listOf(HapticKind.Medium), haptics.kinds, "a flag is a medium tap")
        tap(5)
        assertEquals(-2, values()[5], "a tap does not open a flagged cell")
        longPress(5)
        assertEquals(-1, values()[5])
        assertTrue(hasAnyText("Mayın 10", "Mines 10"))
    }

    @Test
    fun inFlagModeATapFlagsAndALongPressOpens() = runComposeUiTest {
        openMines()
        onNodeWithTag("minesFlagMode").performClick()
        waitForIdle()
        snap("102_mines_flag_mode")
        tap(5)
        assertEquals(-2, values()[5], "a tap flags in flag mode")
        longPress(27)
        assertTrue(values()[27] >= 0, "and a long press opens")
        tap(5)
        assertEquals(-1, values()[5], "a tap takes the flag away")
    }

    @Test
    fun aDoubleTapOnANumberOpensTheCellsAroundItWhenTheFlagsMatch() = runComposeUiTest {
        openMines()
        tap(27)
        var guard = 0
        while (guard++ < 60) {
            val numbers = values().map { if (it >= 0) it else -1 }.toIntArray()
            val deduction = MinesSolver.deduce(8, 8, numbers)
            val candidate = numbers.indices.firstOrNull { cell ->
                val around = MinesGame.neighbors(cell, 8, 8)
                numbers[cell] > 0 &&
                    around.count { it in deduction.mines } == numbers[cell] &&
                    around.any { it in deduction.safe && values()[it] == -1 }
            }
            if (candidate != null) {
                val around = MinesGame.neighbors(candidate, 8, 8)
                around.filter { it in deduction.mines && values()[it] == -1 }.forEach { longPress(it) }
                snap("103_mines_before_chord")
                doubleTap(candidate)
                snap("104_mines_after_chord")
                assertTrue(around.none { values()[it] == -1 }, "everything around the number is open or flagged")
                return@runComposeUiTest
            }
            val next = safeCells().firstOrNull() ?: error("no chord found and nothing safe to open")
            tap(next)
        }
        error("no number to chord on")
    }

    @Test
    fun openingAMineLosesShowsTheMinesAndBuzzesAWarning() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openMines(haptics = haptics)
        tap(27)
        haptics.kinds.clear()
        // Every closed cell in turn: a wrong guess comes quickly (no logic is used on purpose).
        for (cell in 0 until cells) {
            if (values()[cell] == -1) tap(cell)
            if (lost()) break
        }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("105_mines_lost")
        assertTrue(lost())
        assertTrue(values().any { it == -4 }, "the mine that was opened is marked")
        assertTrue(values().count { it == -3 || it == -4 } == 10, "and all ten mines are shown")
        assertTrue(HapticKind.Warning in haptics.kinds)
        assertTrue(HapticKind.Success !in haptics.kinds)
    }

    @Test
    fun clearingTheBoardShowsTheResultBuzzesOnceAndRecordsAWin() = runComposeUiTest {
        val cloud = TestCloud()
        val haptics = RecordingHaptics()
        openMines(cloud, haptics)
        haptics.kinds.clear()
        solve()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("106_mines_won")
        assertTrue(cleared())
        assertEquals(1, haptics.kinds.count { it == HapticKind.Success }, "winning is one success")
        waitUntil(timeoutMillis = 15_000) { cloud.telemetry.events.any { it.event == TelemetryEvent.EndlessFinished } }
        val event = cloud.telemetry.events.first { it.event == TelemetryEvent.EndlessFinished }
        assertEquals("mines", event.props["game"])
        assertEquals("WON", event.props["outcome"])
    }
}
