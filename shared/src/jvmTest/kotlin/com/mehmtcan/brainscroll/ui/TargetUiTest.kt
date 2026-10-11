package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.target.Move
import com.mehmtcan.brainscroll.game.target.Op
import com.mehmtcan.brainscroll.game.target.TargetSolver
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.target.TargetTokenId
import com.mehmtcan.brainscroll.ui.target.TargetTokenValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The target number page in the feed, driven through the real screen. The puzzle is random, so the tests read the numbers
 * the way a screen reader does and solve from that.
 */
@OptIn(ExperimentalTestApi::class)
class TargetUiTest {

    private val games = listOf(GameKind.Word, GameKind.Target)

    /** Starts the app and swipes from the first page (a word puzzle) to the second one (the target number game). */
    private fun ComposeUiTest.openTarget(cloud: TestCloud = TestCloud(), haptics: RecordingHaptics? = null) {
        start(cloud, haptics = haptics, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("targetNumber").fetchSemanticsNodes().size == 4 }
    }

    /** The (id, value) of every number on the table, in the order they are shown. */
    private fun ComposeUiTest.numbers(): List<Pair<Int, Int>> =
        onAllNodesWithTag("targetNumber").fetchSemanticsNodes().map { it.config[TargetTokenId] to it.config[TargetTokenValue] }

    private fun ComposeUiTest.tapNumber(id: Int) {
        val index = numbers().indexOfFirst { it.first == id }
        onAllNodesWithTag("targetNumber")[index].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.tapOp(op: Op) {
        onNodeWithTag("targetOp" + op.name).performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.play(move: Move) {
        tapNumber(move.first)
        tapOp(move.op)
        tapNumber(move.second)
    }

    /** The target, read from the description of its card. */
    private fun ComposeUiTest.target(): Int {
        val description = onNodeWithTag("targetGoal").fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.ContentDescription)!!.joinToString()
        return description.filter { it.isDigit() }.toInt()
    }

    private fun ComposeUiTest.won() = hasAnyText("Hedefe ulaştın!", "Target reached!")

    @Test
    fun theSecondPageShowsFourNumbersAndATargetThatIsNotAmongThem() = runComposeUiTest {
        openTarget()
        snap("150_target_start")
        val values = numbers().map { it.second }
        assertEquals(4, values.size)
        assertTrue(values.all { it in 1..9 })
        assertTrue(target() in 10..99)
        assertTrue(target() !in values)
        assertTrue(hasAnyText("Hedef sayı", "Target number"))
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
    }

    @Test
    fun aNumberAnOperationAndAnotherNumberReplaceTheTwoByTheResult() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTarget(haptics = haptics)
        val before = numbers()
        haptics.kinds.clear()
        tapNumber(before[0].first)
        tapOp(Op.Add)
        snap("151_target_picked")
        tapNumber(before[1].first)
        snap("152_target_combined")
        val after = numbers()
        assertEquals(3, after.size)
        assertTrue(before[0].second + before[1].second in after.map { it.second })
        assertTrue(after.none { it.first == before[0].first || it.first == before[1].first })
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
        assertTrue(hasAnyText("${before[0].second} + ${before[1].second} = ${before[0].second + before[1].second}"))
        assertEquals(listOf(HapticKind.Light, HapticKind.Light, HapticKind.Medium), haptics.kinds)
    }

    @Test
    fun aMoveThatIsNotAllowedDoesNothingAndBuzzesAWarning() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTarget(haptics = haptics)
        val before = numbers()
        // A subtraction from the smaller number or a division with a rest; with four numbers one of these pairs exists.
        val pair = before.flatMap { a -> before.map { b -> a to b } }.first { (a, b) -> a.first != b.first && Op.Sub.apply(a.second, b.second) == null }
        haptics.kinds.clear()
        tapNumber(pair.first.first)
        tapOp(Op.Sub)
        tapNumber(pair.second.first)
        assertEquals(before, numbers())
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
        assertEquals(listOf(HapticKind.Light, HapticKind.Light, HapticKind.Warning), haptics.kinds)
    }

    @Test
    fun undoAndRestartTakeTheNumbersBackAndKeepTheMoves() = runComposeUiTest {
        openTarget()
        val before = numbers()
        play(Move(before[0].first, Op.Add, before[1].first))
        play(Move(numbers()[0].first, Op.Mul, numbers()[1].first))
        assertEquals(2, numbers().size)
        onNodeWithTag("targetUndo").performClick()
        waitForIdle()
        assertEquals(3, numbers().size)
        onNodeWithTag("targetRestart").performClick()
        waitForIdle()
        assertEquals(before, numbers())
        assertTrue(hasAnyText("Hamle 2", "Moves 2"))
    }

    @Test
    fun reachingTheTargetShowsTheResultBuzzesOnceAndRecordsAWin() = runComposeUiTest {
        val cloud = TestCloud()
        val haptics = RecordingHaptics()
        openTarget(cloud, haptics)
        val solution = TargetSolver.solve(numbers().map { it.second }, target())!!
        haptics.kinds.clear()
        for (move in solution) play(move)
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("153_target_won")
        assertTrue(won())
        assertEquals(1, haptics.kinds.count { it == HapticKind.Success }, "winning is one success")
        waitUntil(timeoutMillis = 15_000) { cloud.telemetry.events.any { it.event == TelemetryEvent.EndlessFinished } }
        val event = cloud.telemetry.events.first { it.event == TelemetryEvent.EndlessFinished }
        assertEquals("target", event.props["game"])
        assertEquals("WON", event.props["outcome"])
        assertEquals(solution.size.toString(), event.props["moves"])
    }
}
