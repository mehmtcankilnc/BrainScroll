package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.hanoi.HanoiSolver
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator
import com.mehmtcan.brainscroll.ui.pipes.PipesTileMask
import com.mehmtcan.brainscroll.game.tubes.TubesGame
import com.mehmtcan.brainscroll.game.tubes.solutionOf
import com.mehmtcan.brainscroll.ui.hanoi.HanoiTowerDisks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The win of a puzzle (the result card and the confetti) only comes once the last move has finished moving, in the games
 * where the last move is an animation, and a move never starts with a frame of how it ends.
 */
@OptIn(ExperimentalTestApi::class)
class WinSettleUiTest {

    private fun ComposeUiTest.confetti() = onAllNodesWithTag("confetti").fetchSemanticsNodes().isNotEmpty()

    // --- Tower of Hanoi ---------------------------------------------------------------------------------------------

    private fun ComposeUiTest.openHanoi() {
        start(games = listOf(GameKind.Word, GameKind.Hanoi))
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("hanoiTower").fetchSemanticsNodes().size == 3 }
    }

    private fun ComposeUiTest.hanoiTowers(): List<List<Int>> =
        onAllNodesWithTag("hanoiTower").fetchSemanticsNodes().map { it.config[HanoiTowerDisks] }

    private fun ComposeUiTest.hanoiPegs(): IntArray {
        val result = IntArray(hanoiTowers().sumOf { it.size })
        hanoiTowers().forEachIndexed { tower, disks -> disks.forEach { result[it - 1] = tower } }
        return result
    }

    private fun ComposeUiTest.hanoiGoal(): Int = (0 until 3).first { tower ->
        val description = onAllNodesWithTag("hanoiTower").fetchSemanticsNodes()[tower].config[SemanticsProperties.ContentDescription].joinToString()
        description.contains("goal", ignoreCase = true) || description.contains("hedef", ignoreCase = true)
    }

    private fun ComposeUiTest.hanoiTap(tower: Int) {
        onAllNodesWithTag("hanoiTower")[tower].performClick()
        mainClock.advanceTimeBy(700)
        waitForIdle()
    }

    @Test
    fun hanoiTheLastDiskLandsBeforeTheResultAndTheConfettiCome() = runComposeUiTest {
        openHanoi()
        val way = HanoiSolver.shortest(hanoiPegs(), hanoiGoal())
        way.dropLast(1).forEach { hanoiTap(it.from); hanoiTap(it.to) }
        val last = way.last()
        hanoiTap(last.from)
        mainClock.autoAdvance = false
        onAllNodesWithTag("hanoiTower")[last.to].performClick()
        mainClock.advanceTimeBy(150)
        assertTrue(onAllNodesWithTag("hanoiFlyingDisk", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty(), "the last disk is still on its way")
        assertTrue(!confetti(), "no confetti while the last disk moves")
        assertTrue(!hasAnyText("Tüm diskler taşındı!", "All disks moved!"), "no result while the last disk moves")
        mainClock.advanceTimeBy(1_200)
        assertTrue(hasAnyText("Tüm diskler taşındı!", "All disks moved!"), "the result comes once it has landed")
        assertTrue(confetti(), "and then the confetti")
    }

    @Test
    fun hanoiAMoveNeverStartsWithAFrameOfHowItEnds() = runComposeUiTest {
        openHanoi()
        val way = HanoiSolver.shortest(hanoiPegs(), hanoiGoal())
        hanoiTap(way[0].from)
        hanoiTap(way[0].to)
        // The second move: its very first frame has the disk where it comes from, not where it goes.
        hanoiTap(way[1].from)
        mainClock.autoAdvance = false
        onAllNodesWithTag("hanoiTower")[way[1].to].performClick()
        mainClock.advanceTimeBy(16)
        val disk = onAllNodesWithTag("hanoiFlyingDisk", useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot.center.x
        val from = onAllNodesWithTag("hanoiTower").fetchSemanticsNodes()[way[1].from].boundsInRoot.center.x
        val to = onAllNodesWithTag("hanoiTower").fetchSemanticsNodes()[way[1].to].boundsInRoot.center.x
        assertTrue(kotlin.math.abs(disk - from) < kotlin.math.abs(disk - to), "the first frame of the move is at the start: disk $disk, from $from, to $to")
    }

    // --- Color tubes ------------------------------------------------------------------------------------------------

    private fun ComposeUiTest.tubeCount() = onAllNodesWithTag("tube").fetchSemanticsNodes().size

    private fun ComposeUiTest.tubeNames(): List<List<String>> = (0 until tubeCount()).map { tube ->
        val text = onAllNodesWithTag("tube")[tube].fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        if (text.contains(": ")) text.substringAfter(": ").split(", ") else emptyList()
    }

    private fun ComposeUiTest.tubeBoard(): List<List<Int>> {
        val names = tubeNames()
        val numbers = names.flatten().distinct().withIndex().associate { it.value to it.index }
        return names.map { tube -> tube.map { numbers.getValue(it) } }
    }

    private fun ComposeUiTest.tubeTap(tube: Int) {
        onAllNodesWithTag("tube")[tube].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.pour(from: Int, to: Int) {
        tubeTap(from)
        tubeTap(to)
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
    }

    @Test
    fun tubesTheLastPourEndsBeforeTheResultAndTheConfettiCome() = runComposeUiTest {
        start(games = listOf(GameKind.Word, GameKind.Tubes))
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { tubeCount() == TubesGame.COLORS + TubesGame.EMPTY_TUBES }
        val steps = solutionOf(tubeBoard())
        steps.dropLast(1).forEach { (from, to) -> pour(from, to) }
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        val (from, to) = steps.last()
        tubeTap(from)
        mainClock.autoAdvance = false
        onAllNodesWithTag("tube")[to].performClick()
        mainClock.advanceTimeBy(400)
        assertTrue(!confetti(), "no confetti while the last pour is still being shown")
        assertTrue(!hasAnyText("Tüm renkler ayrıldı!", "All colors sorted!"), "no result card while the last pour is being shown")
        mainClock.advanceTimeBy(1_800)
        assertTrue(hasAnyText("Tüm renkler ayrıldı!", "All colors sorted!"), "the result comes after it")
        assertTrue(confetti(), "and the confetti")
    }

    // --- Pipes: the last tile turns for a moment before the board is done ---------------------------------------------

    private fun ComposeUiTest.pipeTiles(): List<Int> =
        onAllNodesWithTag("pipeTile").fetchSemanticsNodes().map { it.config[PipesTileMask] }

    @Test
    fun pipesTheLastTurnEndsBeforeTheResultAndTheConfettiCome() = runComposeUiTest {
        start(games = listOf(GameKind.Word, GameKind.Pipes))
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("pipeTile").fetchSemanticsNodes().size == 25 }
        val turns = PipesGenerator.solve(pipeTiles().toIntArray(), 5)!!
        val taps = turns.indices.flatMap { cell -> List(turns[cell]) { cell } }
        taps.dropLast(1).forEach { cell ->
            onAllNodesWithTag("pipeTile")[cell].performClick()
            mainClock.advanceTimeBy(300)
            waitForIdle()
        }
        mainClock.autoAdvance = false
        onAllNodesWithTag("pipeTile")[taps.last()].performClick()
        mainClock.advanceTimeBy(60)
        assertTrue(!confetti(), "no confetti while the last tile is still turning")
        assertTrue(!hasAnyText("Tüm borular bağlandı!", "All pipes connected!"), "no result while the last tile is still turning")
        mainClock.advanceTimeBy(1_000)
        assertTrue(hasAnyText("Tüm borular bağlandı!", "All pipes connected!"), "the result comes once it has turned")
        assertTrue(confetti(), "and then the confetti")
    }
}
