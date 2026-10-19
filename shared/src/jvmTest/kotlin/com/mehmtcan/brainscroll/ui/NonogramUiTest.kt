package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.nonogram.NonogramMark
import com.mehmtcan.brainscroll.game.nonogram.NonogramSolver
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.nonogram.NonogramCellValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The mini nonogram page in the feed, driven through the real screen. The picture is random, so the tests read the numbers the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class NonogramUiTest {

    private val games = listOf(GameKind.Word, GameKind.Nonogram)
    private val size = 8

    private fun ComposeUiTest.openNonogram(haptics: RecordingHaptics? = null, reduceMotion: Boolean? = null) {
        start(haptics = haptics, reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("nonogramCell").fetchSemanticsNodes().size == size * size }
    }

    private fun ComposeUiTest.cells(): List<Int> = onAllNodesWithTag("nonogramCell").fetchSemanticsNodes().map { it.config[NonogramCellValue] }

    private fun ComposeUiTest.clues(tag: String): List<List<Int>> = onAllNodesWithTag(tag).fetchSemanticsNodes().map { node ->
        val text = node.config[SemanticsProperties.ContentDescription].joinToString().substringAfter(": ")
        text.split(' ').mapNotNull { it.toIntOrNull() }.filter { it > 0 }
    }

    /** The cells of the picture, worked out from the numbers on the screen. */
    private fun ComposeUiTest.picture(): List<Int> =
        NonogramSolver.deduce(size, clues("nonogramRowClue"), clues("nonogramColumnClue")).first
            .filter { it.second == NonogramSolver.FILLED }.map { it.first }.sorted()

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("nonogramCell")[cell].performClick()
        mainClock.advanceTimeBy(600)
        waitForIdle()
    }

    private fun ComposeUiTest.description(cell: Int) =
        onAllNodesWithTag("nonogramCell").fetchSemanticsNodes()[cell].config[SemanticsProperties.ContentDescription].joinToString()

    private fun ComposeUiTest.won() = hasAnyText("Bu bir ", "It is a ", "Resim tamam!", "Picture complete!")

    /** Drags a finger from the centre of one cell to the centre of another. */
    private fun ComposeUiTest.drag(from: Int, to: Int) {
        val nodes = onAllNodesWithTag("nonogramCell").fetchSemanticsNodes()
        val board = onNodeWithTag("nonogramBoard").fetchSemanticsNode().boundsInRoot
        val a = nodes[from].boundsInRoot.center - board.topLeft
        val b = nodes[to].boundsInRoot.center - board.topLeft
        onNodeWithTag("nonogramBoard").performTouchInput { swipe(Offset(a.x, a.y), Offset(b.x, b.y), durationMillis = 300) }
        mainClock.advanceTimeBy(600)
        waitForIdle()
    }

    @Test
    fun theBoardIsEightByEightWithTheNumbersOfItsLines() = runComposeUiTest {
        openNonogram()
        snap("320_nonogram_start")
        assertEquals(size * size, cells().size)
        assertTrue(cells().all { it == NonogramMark.EMPTY })
        assertEquals(size, clues("nonogramRowClue").size)
        assertEquals(size, clues("nonogramColumnClue").size)
        assertEquals(clues("nonogramRowClue").sumOf { it.sum() }, clues("nonogramColumnClue").sumOf { it.sum() })
        assertTrue(hasAnyText("Mini Nonogram"))
        assertTrue(hasAnyText("Dolu 0", "Filled 0"))
    }

    @Test
    fun aTapFillsAndAnotherTapClearsAndTheCounterFollows() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openNonogram(haptics)
        val cell = picture().first()
        haptics.kinds.clear()
        tap(cell)
        assertEquals(NonogramMark.FILLED, cells()[cell])
        assertTrue(hasAnyText("Dolu 1", "Filled 1"))
        tap(cell)
        assertEquals(NonogramMark.EMPTY, cells()[cell])
        assertEquals(listOf(HapticKind.Light, HapticKind.Light), haptics.kinds)
    }

    @Test
    fun theCrossModeMarksCellsAndKeepsTheCounter() = runComposeUiTest {
        openNonogram()
        onNodeWithTag("nonogramMode").performClick()
        waitForIdle()
        tap(0)
        assertEquals(NonogramMark.CROSS, cells()[0])
        assertTrue(hasAnyText("Dolu 0", "Filled 0"), "a cross is not a filled cell")
        tap(0)
        assertEquals(NonogramMark.EMPTY, cells()[0])
    }

    @Test
    fun aDragFillsTheWholeStretchAsOneAction() = runComposeUiTest {
        openNonogram()
        drag(8, 12)
        val marks = cells()
        assertEquals(List(5) { NonogramMark.FILLED }, marks.subList(8, 13))
        assertEquals(5, marks.count { it == NonogramMark.FILLED })
        assertTrue(hasAnyText("Dolu 5", "Filled 5"))
    }

    @Test
    fun aDragKeepsToTheRowOrColumnItStartedAlong() = runComposeUiTest {
        openNonogram()
        // From (row 1, column 1) to (row 4, column 2): the finger moved more along the column, so the stroke is the column.
        drag(9, 4 * 8 + 2)
        val marks = cells()
        assertEquals(listOf(9, 17, 25, 33), marks.indices.filter { marks[it] == NonogramMark.FILLED })
    }

    @Test
    fun aDragStartingOnAFilledCellClearsTheStretch() = runComposeUiTest {
        openNonogram()
        drag(0, 3)
        drag(0, 3)
        assertTrue(cells().all { it == NonogramMark.EMPTY })
    }

    @Test
    fun aScreenReaderHearsRowColumnAndWhatIsInTheCellAndTheNumbers() = runComposeUiTest {
        openNonogram()
        val text = description(10)
        assertTrue(text.contains("2") && text.contains("3"), text)
        assertTrue(text.contains("boş") || text.contains("empty"), text)
        tap(10)
        assertTrue(description(10).contains("dolu") || description(10).contains("filled"), description(10))
        val clue = onAllNodesWithTag("nonogramRowClue").fetchSemanticsNodes().first().config[SemanticsProperties.ContentDescription].joinToString()
        assertTrue(clue.contains("1"), clue)
    }

    @Test
    fun restartClearsEveryMark() = runComposeUiTest {
        openNonogram()
        tap(picture().first())
        tap(0); tap(0)
        onNodeWithTag("nonogramMode").performClick()
        tap(63)
        onNodeWithTag("nonogramRestart").performClick()
        waitForIdle()
        assertTrue(cells().all { it == NonogramMark.EMPTY })
    }

    @Test
    fun theWinBuzzesOnlyWhenThePictureHasTakenItsColors() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openNonogram(haptics)
        val cells = picture()
        cells.dropLast(1).forEach { tap(it) }
        haptics.kinds.clear()
        mainClock.autoAdvance = false
        onAllNodesWithTag("nonogramCell")[cells.last()].performClick()
        mainClock.advanceTimeBy(100)
        assertTrue(HapticKind.Success !in haptics.kinds, "no success buzz while the picture is still changing color: ${haptics.kinds}")
        mainClock.advanceTimeBy(1_200)
        assertTrue(HapticKind.Success in haptics.kinds, "the success buzz comes once it has settled: ${haptics.kinds}")
    }

    @Test
    fun withReducedMotionTheWinShowsAtOnce() = runComposeUiTest {
        openNonogram(reduceMotion = true)
        val cells = picture()
        cells.dropLast(1).forEach { tap(it) }
        mainClock.autoAdvance = false
        onAllNodesWithTag("nonogramCell")[cells.last()].performClick()
        mainClock.advanceTimeBy(100)
        assertTrue(won())
    }

    @Test
    fun fillingThePictureWinsAndShowsItsNameAndTheTime() = runComposeUiTest {
        openNonogram()
        for (cell in picture()) tap(cell)
        snap("321_nonogram_solved")
        assertTrue(won())
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }
}
