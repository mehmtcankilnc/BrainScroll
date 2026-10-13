package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.sudoku.SudokuCellValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The sudoku page in the feed, driven through the real screen. The board is random, so the tests read the cells the way
 * a screen reader does and solve from that, with the same logic solver the board generator uses.
 */
@OptIn(ExperimentalTestApi::class)
class SudokuUiTest {

    private val games = listOf(GameKind.Word, GameKind.Sudoku)

    /** Starts the app and swipes from the first page (a word puzzle) to the second one (sudoku). */
    private fun ComposeUiTest.openSudoku(cloud: TestCloud = TestCloud(), haptics: RecordingHaptics? = null) {
        start(cloud, haptics = haptics, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { cellCount() == 81 }
    }

    private fun ComposeUiTest.cellCount() = onAllNodesWithTag("sudokuCell").fetchSemanticsNodes().size

    /** What every cell shows: 0 empty, 1..9 a digit, 100+d a wrong digit, 200+mask notes. */
    private fun ComposeUiTest.values(): List<Int> =
        onAllNodesWithTag("sudokuCell").fetchSemanticsNodes().map { it.config[SudokuCellValue] }

    private fun ComposeUiTest.tapCell(cell: Int) {
        onAllNodesWithTag("sudokuCell")[cell].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.key(digit: Int) {
        onNodeWithTag("sudokuKey$digit").performClick()
        waitForIdle()
    }

    /** The solution, found from the digits on show (the givens are the only digits before the first move). */
    private fun ComposeUiTest.solution(): IntArray =
        SudokuGenerator.solveBySingles(values().map { if (it in 1..9) it else 0 }.toIntArray())!!

    private fun ComposeUiTest.won() = hasAnyText("Sudoku çözüldü!", "Sudoku solved!")

    private fun ComposeUiTest.lost() = hasAnyText("Çok fazla hata", "Too many mistakes")

    private fun ComposeUiTest.place(cell: Int, digit: Int) {
        tapCell(cell)
        key(digit)
    }

    @Test
    fun theSecondPageIsABoardOfEightyOneCellsWithTheGivensFilledIn() = runComposeUiTest {
        openSudoku()
        snap("120_sudoku_start")
        assertEquals(81, cellCount())
        assertEquals(SudokuGenerator.DEFAULT_CLUES, values().count { it in 1..9 })
        assertTrue(hasAnyText("Sudoku"))
        assertTrue(hasAnyText("Hata: 0", "Mistakes: 0"))
        for (digit in 1..9) onNodeWithTag("sudokuKey$digit").assertExists()
    }

    @Test
    fun aRightDigitStaysAndAWrongOneShowsRedCountsAMistakeAndBuzzes() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openSudoku(haptics = haptics)
        val solution = solution()
        val empty = values().indices.filter { values()[it] == 0 }
        haptics.kinds.clear()

        place(empty[0], solution[empty[0]])
        assertEquals(solution[empty[0]], values()[empty[0]])
        assertEquals(listOf(HapticKind.Medium), haptics.kinds, "a right digit is a medium tap")

        haptics.kinds.clear()
        val wrong = if (solution[empty[1]] == 1) 2 else 1
        place(empty[1], wrong)
        snap("121_sudoku_mistake")
        assertEquals(100 + wrong, values()[empty[1]])
        assertTrue(hasAnyText("Hata: 1", "Mistakes: 1"))
        assertEquals(listOf(HapticKind.Warning), haptics.kinds)

        // A wrong digit can be erased and the cell tried again.
        onNodeWithTag("sudokuErase").performClick()
        waitForIdle()
        assertEquals(0, values()[empty[1]])
        key(solution[empty[1]])
        assertEquals(solution[empty[1]], values()[empty[1]])
    }

    @Test
    fun aGivenCellCanBeSelectedButNotChanged() = runComposeUiTest {
        openSudoku()
        val given = values().indexOfFirst { it in 1..9 }
        val before = values()
        place(given, if (before[given] == 1) 2 else 1)
        assertEquals(before, values())
        assertTrue(hasAnyText("Hata: 0", "Mistakes: 0"))
    }

    @Test
    fun inNotesModeTheDigitsAreWrittenAsNotesAndAPlacedDigitClearsThemAround() = runComposeUiTest {
        openSudoku()
        val solution = solution()
        val empty = values().indices.filter { values()[it] == 0 }
        val cell = empty[0]
        val peer = SudokuGenerator.peers(cell).first { values()[it] == 0 }

        onNodeWithTag("sudokuNotes").performClick()
        waitForIdle()
        tapCell(peer)
        key(solution[cell])
        key(((solution[cell]) % 9) + 1)
        snap("122_sudoku_notes")
        assertEquals(200 + (1 shl (solution[cell] - 1)) + (1 shl (solution[cell] % 9)), values()[peer])

        onNodeWithTag("sudokuNotes").performClick()
        waitForIdle()
        place(cell, solution[cell])
        assertEquals(200 + (1 shl (solution[cell] % 9)), values()[peer], "the placed digit is gone from the notes around")
    }

    @Test
    fun completingARowPlaysAWaveAcrossIt() = runComposeUiTest {
        openSudoku()
        val solution = solution()
        val row = (0 until 9).map { it }
        val empty = row.filter { values()[it] == 0 }
        for (cell in empty.dropLast(1)) place(cell, solution[cell])
        mainClock.autoAdvance = false
        place(empty.last(), solution[empty.last()])
        mainClock.advanceTimeBy(250)
        snap("126_sudoku_wave")
        mainClock.autoAdvance = true
        waitForIdle()
        assertTrue(row.all { values()[it] == solution[it] })
    }

    @Test
    fun mistakesAreCountedWithoutALimitAndTheBoardGoesOn() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openSudoku(haptics = haptics)
        val solution = solution()
        val empty = values().indices.filter { values()[it] == 0 }
        haptics.kinds.clear()
        for (cell in empty.take(4)) place(cell, if (solution[cell] == 1) 2 else 1)
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("123_sudoku_many_mistakes")
        assertTrue(hasAnyText("Hata: 4", "Mistakes: 4"), "the number goes past three")
        assertTrue(HapticKind.Warning in haptics.kinds)
        assertTrue(HapticKind.Success !in haptics.kinds)
        onAllNodesWithTag("sudokuKey1").assertCountEquals(1) // the pad is still there: the game is not over
    }

    @Test
    fun solvingTheBoardShowsTheResultBuzzesOnceAndRecordsAWin() = runComposeUiTest {
        val cloud = TestCloud()
        val haptics = RecordingHaptics()
        openSudoku(cloud, haptics)
        val solution = solution()
        val empty = values().indices.filter { values()[it] == 0 }
        haptics.kinds.clear()
        for (cell in empty) place(cell, solution[cell])
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("124_sudoku_won")
        assertTrue(won())
        assertEquals(1, haptics.kinds.count { it == HapticKind.Success }, "winning is one success")
        waitUntil(timeoutMillis = 15_000) { cloud.telemetry.events.any { it.event == TelemetryEvent.EndlessFinished } }
        val event = cloud.telemetry.events.first { it.event == TelemetryEvent.EndlessFinished }
        assertEquals("sudoku", event.props["game"])
        assertEquals("WON", event.props["outcome"])
    }
}
