package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuLayout
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.sudoku.SudokuCellValue
import com.mehmtcan.brainscroll.ui.sudoku.SudokuRegions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The jigsaw sudoku page in the feed. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class JigsawUiTest {

    private val games = listOf(GameKind.Word, GameKind.Jigsaw)

    private fun ComposeUiTest.openJigsaw(haptics: RecordingHaptics? = null) {
        start(haptics = haptics, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("sudokuCell").fetchSemanticsNodes().size == 81 }
    }

    private fun ComposeUiTest.values(): List<Int> = onAllNodesWithTag("sudokuCell").fetchSemanticsNodes().map { it.config[SudokuCellValue] }

    private fun ComposeUiTest.regions(): List<Int> = onNodeWithTag("sudokuBoard").fetchSemanticsNode().config[SudokuRegions]

    private fun ComposeUiTest.tapCell(cell: Int) {
        onAllNodesWithTag("sudokuCell")[cell].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.key(digit: Int) {
        onNodeWithTag("sudokuKey$digit").performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.solution(): IntArray {
        val givens = values().map { if (it in 1..9) it else 0 }.toIntArray()
        return SudokuGenerator.solveBySingles(givens, SudokuLayout.of(regions()))!!
    }

    @Test
    fun theBoardHasNineRegionsOfNineCellsAndNoClassicBoxes() = runComposeUiTest {
        openJigsaw()
        snap("300_jigsaw_start")
        val regions = regions()
        for (region in 0 until 9) assertEquals(9, regions.count { it == region })
        assertTrue(!SudokuLayout.of(regions).isStandard)
        assertTrue(hasAnyText("Jigsaw Sudoku"))
        assertTrue(hasAnyText("Hata: 0", "Mistakes: 0"))
    }

    @Test
    fun aRightDigitSticksAndAWrongOneShowsInRedAndCountsAMistake() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openJigsaw(haptics)
        val solution = solution()
        val cell = values().indexOf(0)
        haptics.kinds.clear()
        tapCell(cell)
        key((1..9).first { it != solution[cell] })
        assertTrue(values()[cell] > 100, "a wrong digit is marked")
        assertTrue(hasAnyText("Hata: 1", "Mistakes: 1"))
        assertEquals(HapticKind.Warning, haptics.kinds.last())
        onNodeWithTag("sudokuErase").performClick()
        waitForIdle()
        assertEquals(0, values()[cell])
        key(solution[cell])
        assertEquals(solution[cell], values()[cell])
        snap("301_jigsaw_placed")
    }

    @Test
    fun fillingTheBoardWins() = runComposeUiTest {
        openJigsaw()
        val solution = solution()
        for (cell in values().indices) {
            if (values()[cell] != 0) continue
            tapCell(cell)
            key(solution[cell])
        }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("302_jigsaw_solved")
        assertTrue(hasAnyText("Sudoku çözüldü!", "Sudoku solved!"))
    }
}
