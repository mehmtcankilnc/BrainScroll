package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.tubes.TubesGame
import com.mehmtcan.brainscroll.game.tubes.solutionOf
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.tubes.DrawnUnits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The color tubes page in the feed, driven through the real screen. The board is random, so the tests read the
 * tubes the way a screen reader does (each tube names its colors from the bottom) and play from that.
 */
@OptIn(ExperimentalTestApi::class)
class TubesUiTest {

    private val games = listOf(GameKind.Word, GameKind.Tubes)

    /** Starts the app and swipes from the first page (a word puzzle) to the second one (tubes). */
    private fun ComposeUiTest.openTubes(cloud: TestCloud = TestCloud(), haptics: RecordingHaptics? = null) {
        start(cloud, haptics = haptics, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { tubeCount() == TubesGame.COLORS + TubesGame.EMPTY_TUBES }
    }

    private fun ComposeUiTest.tubeCount() = onAllNodesWithTag("tube").fetchSemanticsNodes().size

    private fun ComposeUiTest.description(tube: Int): String =
        onAllNodesWithTag("tube")[tube].fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()

    /** The colors of every tube from the bottom, as names: "Tube 3, from the bottom: Star, Heart" gives [Star, Heart]. */
    private fun ComposeUiTest.names(): List<List<String>> = (0 until tubeCount()).map { tube ->
        val text = description(tube)
        if (text.contains(": ")) text.substringAfter(": ").split(", ") else emptyList()
    }

    /** The same boards with numbers instead of names, so the solver can read them. */
    private fun ComposeUiTest.board(): List<List<Int>> {
        val names = names()
        val numbers = names.flatten().distinct().withIndex().associate { it.value to it.index }
        return names.map { tube -> tube.map { numbers.getValue(it) } }
    }

    private fun ComposeUiTest.tap(tube: Int) {
        onAllNodesWithTag("tube")[tube].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.pour(from: Int, to: Int) {
        tap(from)
        tap(to)
        // The pour is shown for a second; the board only has its new colors once that is over.
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
    }

    private fun ComposeUiTest.solve() {
        solutionOf(board()).forEach { (from, to) -> pour(from, to) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    private fun ComposeUiTest.solved() = hasAnyText("Tüm renkler ayrıldı!", "All colors sorted!")

    @Test
    fun theSecondPageIsABoardOfSevenTubesFiveFullAndTwoEmpty() = runComposeUiTest {
        openTubes()
        snap("90_tubes_start")
        assertEquals(7, tubeCount())
        val tubes = names()
        assertEquals(TubesGame.COLORS, tubes.count { it.size == TubesGame.DEFAULT_CAPACITY })
        assertEquals(TubesGame.EMPTY_TUBES, tubes.count { it.isEmpty() })
        assertTrue(hasAnyText("Renk tüpleri", "Color tubes"))
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
    }

    @Test
    fun tappingATubeLiftsItAndTappingAnotherPoursIntoIt() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTubes(haptics = haptics)
        val (from, to) = solutionOf(board()).first()
        val before = names()
        haptics.kinds.clear()

        tap(from)
        mainClock.advanceTimeBy(300)
        snap("91_tubes_selected")
        assertTrue(description(from).let { it.contains("seçili") || it.contains("selected") }, description(from))
        assertEquals(listOf(HapticKind.Light), haptics.kinds, "choosing a tube is a light tap")

        // Stop the clock by hand to look in the middle of the pour.
        mainClock.autoAdvance = false
        tap(to)
        mainClock.advanceTimeBy(500)
        snap("92_tubes_pouring")
        // (The tube being poured is drawn on top, so it comes last in the list: compare without the order.)
        assertEquals(before.map { it.toString() }.sorted(), names().map { it.toString() }.sorted(), "while the pour is shown the tubes still look as they were")
        mainClock.autoAdvance = true
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("94_tubes_poured")
        assertEquals(HapticKind.Medium, haptics.kinds.last(), "a pour is a medium tap")
        assertTrue(names() != before, "the colors moved")
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
    }

    @Test
    fun tappingTheChosenTubeAgainPutsItBack() = runComposeUiTest {
        openTubes()
        val full = names().indexOfFirst { it.isNotEmpty() }
        tap(full)
        tap(full)
        assertTrue(!description(full).let { it.contains("seçili") || it.contains("selected") })
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
    }

    @Test
    fun undoTakesAPourBackButTheMoveStaysCountedAndRestartStartsOver() = runComposeUiTest {
        openTubes()
        val start = names()
        val (from, to) = solutionOf(board()).first()
        pour(from, to)
        assertTrue(names() != start)

        onNodeWithTag("tubesUndo").performClick()
        waitForIdle()
        assertEquals(start, names())
        assertTrue(hasAnyText("Hamle 1", "Moves 1"), "undo does not give the move back")

        pour(from, to)
        onNodeWithTag("tubesRestart").performClick()
        waitForIdle()
        assertEquals(start, names())
        assertTrue(hasAnyText("Hamle 2", "Moves 2"), "restart keeps the moves too")
    }

    @Test
    fun solvingTheBoardShowsTheResultBuzzesOnceAndRecordsAWin() = runComposeUiTest {
        val cloud = TestCloud()
        val haptics = RecordingHaptics()
        openTubes(cloud, haptics)
        haptics.kinds.clear()
        solve()
        snap("93_tubes_won")
        assertTrue(solved())
        assertEquals(1, haptics.kinds.count { it == HapticKind.Success }, "winning is one success")
        waitUntil(timeoutMillis = 15_000) { cloud.telemetry.events.any { it.event == TelemetryEvent.EndlessFinished } }
        val event = cloud.telemetry.events.first { it.event == TelemetryEvent.EndlessFinished }
        assertEquals("tubes", event.props["game"])
        assertEquals("WON", event.props["outcome"])
    }

    private fun ComposeUiTest.sortedNames() = names().map { it.toString() }.sorted()

    @Test
    fun aSecondPourDoesNotShowItsResultForAnInstantBeforeItStarts() = runComposeUiTest {
        openTubes()
        val first = solutionOf(board()).first()
        pour(first.first, first.second)

        // The second pour, with the clock stopped one frame after the tap: the tubes must still look as they did.
        val second = solutionOf(board()).first()
        val before = sortedNames()
        mainClock.autoAdvance = false
        tap(second.first)
        tap(second.second)
        mainClock.advanceTimeBy(16)
        assertEquals(before, sortedNames(), "the first frame of a pour shows the board as it was")
        // The description comes from the contents, the picture from the drawn amounts: both must still be the old board.
        val contents = names().map { it.size.toFloat() }
        val drawn = onAllNodesWithTag("tube").fetchSemanticsNodes().map { it.config[DrawnUnits] }
        assertEquals<List<Float>>(contents, drawn, "the liquid drawn in every tube is what it was before the pour")
        mainClock.autoAdvance = true
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        assertTrue(sortedNames() != before, "and then the colors have moved")
    }

    @Test
    fun finishingATubeThrowsALittleConfettiThatThenGoesAway() = runComposeUiTest {
        openTubes()
        val steps = solutionOf(board())
        steps.dropLast(1).forEach { (from, to) -> pour(from, to) }
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        val (from, to) = steps.last()
        mainClock.autoAdvance = false
        tap(from)
        tap(to)
        mainClock.advanceTimeBy(1_300) // the pour is over (1 s) and the confetti is in the air
        snap("95_tubes_confetti")
        assertEquals(true, onAllNodesWithTag("tubeConfetti").fetchSemanticsNodes().isNotEmpty())
        mainClock.autoAdvance = true
        mainClock.advanceTimeBy(2_500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("tubeConfetti").fetchSemanticsNodes().isEmpty(), "it is over after a second or so")
        assertTrue(solved())
    }
}
