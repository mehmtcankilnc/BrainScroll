package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.queens.QueensSolver
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.queens.QueensCellValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The queens page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class QueensUiTest {

    private val games = listOf(GameKind.Word, GameKind.Queens)
    private val size = 5

    /** Starts the app and swipes from the first page (a word puzzle) to the second one (Queens). */
    private fun ComposeUiTest.openQueens(haptics: RecordingHaptics? = null, reduceMotion: Boolean? = null) {
        start(haptics = haptics, reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("queensCell").fetchSemanticsNodes().size == size * size }
    }

    /** What every cell holds: 0 empty, 1 queen, 2 locked queen, 3 attacked queen. */
    private fun ComposeUiTest.cells(): List<Int> = onAllNodesWithTag("queensCell").fetchSemanticsNodes().map { it.config[QueensCellValue] }

    /** The locked queens. Only right at the start (before anything attacks them), since an attacked queen shows as attacked. */
    private fun ComposeUiTest.locked(): Set<Int> = cells().indices.filter { cells()[it] == 2 }.toSet()

    private fun ComposeUiTest.tap(cell: Int) {
        onAllNodesWithTag("queensCell")[cell].performClick()
        mainClock.advanceTimeBy(400)
        waitForIdle()
    }

    private fun ComposeUiTest.description(cell: Int) =
        onAllNodesWithTag("queensCell").fetchSemanticsNodes()[cell].config[SemanticsProperties.ContentDescription].joinToString()

    private fun ComposeUiTest.won() = hasAnyText("Tüm vezirler yerinde!", "All queens in place!")

    /** The cells that finish the board from the locked queens at the start. */
    private fun ComposeUiTest.missing(): List<Int> {
        val lockedCells = locked()
        return QueensSolver.solutions(size, lockedCells).first().filter { it !in lockedCells }
    }

    @Test
    fun theBoardIsFiveByFiveWithTwoLockedQueens() = runComposeUiTest {
        openQueens()
        snap("300_queens_start")
        assertEquals(2, cells().count { it == 2 })
        assertEquals(0, cells().count { it == 1 })
        assertTrue(hasAnyText("Vezirler", "Queens"))
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
        assertTrue(hasAnyText("En az 3", "Fewest 3"))
    }

    @Test
    fun aTapPlacesAQueenAndAnotherTapTakesItAwayAndEachCountsAMove() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openQueens(haptics)
        val cell = missing().first()
        haptics.kinds.clear()
        tap(cell)
        assertTrue(cells()[cell] != 0, "a queen is there")
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
        tap(cell)
        assertEquals(0, cells()[cell])
        assertTrue(hasAnyText("Hamle 2", "Moves 2"))
        assertEquals(listOf(HapticKind.Light, HapticKind.Light), haptics.kinds)
    }

    @Test
    fun aLockedQueenIsRefusedWithABuzzAndCountsNothing() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openQueens(haptics)
        val lockedCell = locked().first()
        haptics.kinds.clear()
        tap(lockedCell)
        assertEquals(2, cells()[lockedCell])
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
        assertEquals(HapticKind.Warning, haptics.kinds.last())
    }

    @Test
    fun queensThatAttackEachOtherAreMarkedAndSaySoToAScreenReader() = runComposeUiTest {
        openQueens()
        val lockedCell = locked().first()
        val row = lockedCell / size
        val other = (0 until size).map { row * size + it }.first { it != lockedCell && cells()[it] == 0 }
        tap(other)
        assertEquals(3, cells()[other], "the new queen attacks the locked one in its row")
        assertEquals(3, cells()[lockedCell])
        snap("301_queens_attacked")
        val text = description(other)
        assertTrue(text.contains("saldır", ignoreCase = true) || text.contains("attacked", ignoreCase = true), text)
        tap(other)
        assertEquals(2, cells()[lockedCell], "the lock stays and the red goes")
    }

    @Test
    fun aScreenReaderHearsRowColumnAndWhatIsInTheCell() = runComposeUiTest {
        openQueens()
        val empty = cells().indexOf(0)
        val text = description(empty)
        assertTrue(text.contains("${empty / size + 1}") && text.contains("${empty % size + 1}"), text)
        val lockedCell = locked().first()
        assertTrue(hasWord(description(lockedCell), "kilitli", "locked"), description(lockedCell))
    }

    private fun hasWord(text: String, vararg words: String) = words.any { text.contains(it, ignoreCase = true) }

    @Test
    fun restartTakesAwayThePlayersQueensAndKeepsTheMoves() = runComposeUiTest {
        openQueens()
        val cell = missing().first()
        tap(cell)
        assertTrue(cells()[cell] != 0)
        onNodeWithTag("queensRestart").performClick()
        waitForIdle()
        assertEquals(0, cells()[cell])
        assertEquals(2, cells().count { it == 2 }, "the locked queens stay")
        assertTrue(hasAnyText("Hamle 1", "Moves 1"), "the move stays counted")
    }

    @Test
    fun theWinBuzzesOnlyWhenTheLastQueenHasSettled() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openQueens(haptics)
        val cells = missing()
        cells.dropLast(1).forEach { tap(it) }
        haptics.kinds.clear()
        mainClock.autoAdvance = false
        onAllNodesWithTag("queensCell")[cells.last()].performClick()
        mainClock.advanceTimeBy(100)
        assertTrue(HapticKind.Success !in haptics.kinds, "no success buzz while the last queen is still popping in: ${haptics.kinds}")
        mainClock.advanceTimeBy(1_200)
        assertTrue(HapticKind.Success in haptics.kinds, "the success buzz comes once it has settled: ${haptics.kinds}")
    }

    @Test
    fun withReducedMotionTheWinShowsAtOnce() = runComposeUiTest {
        openQueens(reduceMotion = true)
        val cells = missing()
        cells.dropLast(1).forEach { tap(it) }
        mainClock.autoAdvance = false
        onAllNodesWithTag("queensCell")[cells.last()].performClick()
        mainClock.advanceTimeBy(100)
        assertTrue(won())
    }

    @Test
    fun fillingTheBoardRightWinsAndShowsTheResult() = runComposeUiTest {
        openQueens()
        for (cell in missing()) tap(cell)
        snap("302_queens_solved")
        assertTrue(won())
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }
}
