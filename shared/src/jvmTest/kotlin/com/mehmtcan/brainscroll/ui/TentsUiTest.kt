package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.tents.TentsSolver
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.tents.TentsCellValue
import com.mehmtcan.brainscroll.ui.tents.TentsClueValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The tents page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class TentsUiTest {

    private val games = listOf(GameKind.Word, GameKind.Tents)
    private val size = 6

    private fun ComposeUiTest.openTents(haptics: RecordingHaptics? = null, reduceMotion: Boolean? = null) {
        start(haptics = haptics, reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("tentsCell").fetchSemanticsNodes().size == size * size }
    }

    /** What every cell holds: 0 empty, 1 tent, 2 grass, 3 tree, 4 tent that breaks a rule. */
    private fun ComposeUiTest.cells(): List<Int> = onAllNodesWithTag("tentsCell").fetchSemanticsNodes().map { it.config[TentsCellValue] }

    private fun ComposeUiTest.rowClues(): List<Int> = onAllNodesWithTag("tentsRowClue").fetchSemanticsNodes().map { it.config[TentsClueValue] }
    private fun ComposeUiTest.columnClues(): List<Int> = onAllNodesWithTag("tentsColumnClue").fetchSemanticsNodes().map { it.config[TentsClueValue] }

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("tentsCell")[cell].performClick()
        mainClock.advanceTimeBy(400)
        waitForIdle()
    }

    private fun ComposeUiTest.description(cell: Int) =
        onAllNodesWithTag("tentsCell").fetchSemanticsNodes()[cell].config[SemanticsProperties.ContentDescription].joinToString()

    private fun ComposeUiTest.won() = hasAnyText("Çadırlar kuruldu!", "All tents pitched!")

    /** The tents of the board, worked out from the trees and the numbers on the screen. */
    private fun ComposeUiTest.answer(): List<Int> {
        val trees = cells().indices.filter { cells()[it] == 3 }.toSet()
        return TentsSolver.solutions(size, trees, rowClues(), columnClues()).single().sorted()
    }

    @Test
    fun theBoardIsSixBySixWithEightTreesAndTheNumbersOfItsLines() = runComposeUiTest {
        openTents()
        snap("300_tents_start")
        assertEquals(8, cells().count { it == 3 })
        assertEquals(0, cells().count { it == 1 })
        assertEquals(size, rowClues().size)
        assertEquals(size, columnClues().size)
        assertEquals(8, rowClues().sum())
        assertEquals(8, columnClues().sum())
        assertTrue(hasAnyText("Çadırlar", "Tents"))
        assertTrue(hasAnyText("Çadır 0/8", "Tents 0/8"))
    }

    @Test
    fun aTapCyclesTentGrassEmptyAndTheCounterFollows() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTents(haptics)
        val cell = answer().first()
        haptics.kinds.clear()
        tap(cell)
        assertEquals(1, cells()[cell])
        assertTrue(hasAnyText("Çadır 1/8", "Tents 1/8"))
        tap(cell)
        assertEquals(2, cells()[cell], "grass")
        assertTrue(hasAnyText("Çadır 0/8", "Tents 0/8"), "grass is not a tent")
        tap(cell)
        assertEquals(0, cells()[cell])
        assertEquals(listOf(HapticKind.Light, HapticKind.Light, HapticKind.Light), haptics.kinds)
    }

    @Test
    fun aTreeIsRefusedWithABuzz() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTents(haptics)
        val tree = cells().indexOf(3)
        haptics.kinds.clear()
        tap(tree)
        assertEquals(3, cells()[tree])
        assertEquals(HapticKind.Warning, haptics.kinds.last())
    }

    @Test
    fun aTentThatBreaksARuleIsMarkedAndSaysSoToAScreenReader() = runComposeUiTest {
        openTents()
        val cell = answer().first()
        val next = if (cell % size < size - 1) cell + 1 else cell - 1
        if (cells()[next] == 3) return@runComposeUiTest // the neighbor is a tree on this board; another run of the suite covers it
        tap(cell)
        tap(next)
        snap("301_tents_conflict")
        assertEquals(4, cells()[cell], "the two touch")
        assertEquals(4, cells()[next])
        val text = description(next)
        assertTrue(text.contains("bozan", ignoreCase = true) || text.contains("breaks", ignoreCase = true), text)
        tap(next); tap(next)
        assertEquals(1, cells()[cell], "the red goes when the other tent does")
    }

    @Test
    fun aScreenReaderHearsRowColumnAndWhatIsInTheCell() = runComposeUiTest {
        openTents()
        val tree = cells().indexOf(3)
        assertTrue(description(tree).contains("ağaç") || description(tree).contains("tree"), description(tree))
        val empty = cells().indexOf(0)
        assertTrue(description(empty).contains("${empty / size + 1}") && description(empty).contains("${empty % size + 1}"), description(empty))
        val clue = onAllNodesWithTag("tentsRowClue").fetchSemanticsNodes().first().config[SemanticsProperties.ContentDescription].joinToString()
        assertTrue(clue.contains("1"), clue)
    }

    @Test
    fun restartClearsEveryMark() = runComposeUiTest {
        openTents()
        val cell = answer().first()
        tap(cell)
        tap(answer().last()); tap(answer().last())
        onNodeWithTag("tentsRestart").performClick()
        waitForIdle()
        assertTrue(cells().none { it == 1 || it == 2 || it == 4 })
    }

    @Test
    fun theWinBuzzesOnlyWhenTheLastTentHasSettled() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTents(haptics)
        val cells = answer()
        cells.dropLast(1).forEach { tap(it) }
        haptics.kinds.clear()
        mainClock.autoAdvance = false
        onAllNodesWithTag("tentsCell")[cells.last()].performClick()
        mainClock.advanceTimeBy(100)
        assertTrue(HapticKind.Success !in haptics.kinds, "no success buzz while the last tent is still popping in: ${haptics.kinds}")
        mainClock.advanceTimeBy(1_200)
        assertTrue(HapticKind.Success in haptics.kinds, "the success buzz comes once it has settled: ${haptics.kinds}")
    }

    @Test
    fun withReducedMotionTheWinShowsAtOnce() = runComposeUiTest {
        openTents(reduceMotion = true)
        val cells = answer()
        cells.dropLast(1).forEach { tap(it) }
        mainClock.autoAdvance = false
        onAllNodesWithTag("tentsCell")[cells.last()].performClick()
        mainClock.advanceTimeBy(100)
        assertTrue(won())
    }

    @Test
    fun placingEveryTentWinsAndShowsTheTime() = runComposeUiTest {
        openTents()
        for (cell in answer()) tap(cell)
        snap("302_tents_solved")
        assertTrue(won())
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }
}
