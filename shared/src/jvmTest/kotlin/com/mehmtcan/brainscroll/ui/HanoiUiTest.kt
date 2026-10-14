package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.hanoi.HanoiSolver
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.hanoi.HanoiPicked
import com.mehmtcan.brainscroll.ui.hanoi.HanoiTowerDisks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The tower of Hanoi page in the feed, driven through the real screen. The board is random, so the tests read the towers the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class HanoiUiTest {

    private val games = listOf(GameKind.Word, GameKind.Hanoi)

    /** Starts the app and swipes from the first page (a word puzzle) to the second one (Hanoi). */
    private fun ComposeUiTest.openHanoi(haptics: RecordingHaptics? = null, reduceMotion: Boolean? = null) {
        start(haptics = haptics, reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("hanoiTower").fetchSemanticsNodes().size == 3 }
    }

    /** The disks of each tower from the bottom up, as numbers (the smallest is 1). */
    private fun ComposeUiTest.towers(): List<List<Int>> =
        onAllNodesWithTag("hanoiTower").fetchSemanticsNodes().map { it.config[HanoiTowerDisks] }

    /** For each disk from the smallest, its tower: the position the solver works from. */
    private fun ComposeUiTest.pegs(): IntArray {
        val result = IntArray(towers().sumOf { it.size })
        towers().forEachIndexed { tower, disks -> disks.forEach { result[it - 1] = tower } }
        return result
    }

    private fun ComposeUiTest.picked() = onNodeWithTag("hanoiBoard").fetchSemanticsNode().config[HanoiPicked]

    private fun ComposeUiTest.tap(tower: Int) {
        onAllNodesWithTag("hanoiTower")[tower].performClick()
        // Long enough for a disk to be carried across and land.
        mainClock.advanceTimeBy(700)
        waitForIdle()
    }

    private fun ComposeUiTest.flying() = onAllNodesWithTag("hanoiFlyingDisk", useUnmergedTree = true).fetchSemanticsNodes()

    private fun ComposeUiTest.won() = hasAnyText("Tüm diskler taşındı!", "All disks moved!")

    @Test
    fun theSecondPageIsThreeTowersWithFourDisks() = runComposeUiTest {
        openHanoi()
        snap("300_hanoi_start")
        assertEquals(4, towers().sumOf { it.size })
        for (tower in towers()) assertEquals(tower.sortedDescending(), tower, "a tower is in order of size, the biggest at the bottom")
        assertTrue(hasAnyText("Hanoi Kuleleri", "Tower of Hanoi"))
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
        assertTrue(hasAnyText("Hedef", "Goal"), "the goal tower is named")
        assertEquals(4, onAllNodesWithTag("hanoiDisk", useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun aTapPicksUpATowerAndAnotherTapMovesTheTopDiskAndCountsAMove() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openHanoi(haptics)
        val step = HanoiSolver.shortest(pegs(), goalOf()).first()
        val before = towers()
        haptics.kinds.clear()
        tap(step.from)
        assertEquals(step.from, picked())
        tap(step.to)
        snap("301_hanoi_moved")
        val after = towers()
        assertEquals(before[step.from].dropLast(1), after[step.from])
        assertEquals(before[step.to] + before[step.from].last(), after[step.to])
        assertEquals(-1, picked())
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
        assertEquals(listOf(HapticKind.Light, HapticKind.Medium), haptics.kinds)
    }

    /** The goal tower, read from the screen: its label is the only one that names it. */
    private fun ComposeUiTest.goalOf(): Int {
        val index = (0 until 3).first { tower ->
            val description = onAllNodesWithTag("hanoiTower").fetchSemanticsNodes()[tower].config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()
            description.contains("goal", ignoreCase = true) || description.contains("hedef", ignoreCase = true)
        }
        return index
    }

    @Test
    fun aBiggerDiskOnASmallerOneIsRefusedWithABuzzAndTheDiskStaysPicked() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openHanoi(haptics)
        val pegs = pegs()
        // Find two towers where the top of the first is bigger than the top of the second.
        val tops = towers().map { it.lastOrNull() }
        val pair = (0 until 3).flatMap { a -> (0 until 3).map { b -> a to b } }
            .firstOrNull { (a, b) -> a != b && tops[a] != null && tops[b] != null && tops[a]!! > tops[b]!! }
        if (pair == null) return@runComposeUiTest // every top is in order on this board: nothing to refuse
        haptics.kinds.clear()
        tap(pair.first)
        tap(pair.second)
        assertEquals(pegs.toList(), pegs().toList(), "nothing moved")
        assertEquals(pair.first, picked(), "the disk stays picked up")
        assertEquals(HapticKind.Warning, haptics.kinds.last())
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
    }

    @Test
    fun aMovedDiskIsCarriedOverTheTopOfTheTowersAndLandsOnItsPile() = runComposeUiTest {
        openHanoi()
        val step = HanoiSolver.shortest(pegs(), goalOf()).first()
        val before = towers()
        val total = before.sumOf { it.size }
        tap(step.from)
        mainClock.autoAdvance = false
        onAllNodesWithTag("hanoiTower")[step.to].performClick()
        mainClock.advanceTimeBy(210)
        // In the middle of the way the disk is in the air, at the top of the towers, and in neither pile.
        val air = flying()
        assertEquals(1, air.size, "one disk is on its way")
        val tower = onAllNodesWithTag("hanoiTower").fetchSemanticsNodes()[step.from].boundsInRoot
        assertTrue(air.single().boundsInRoot.top < tower.top + tower.height * 0.25f, "it travels near the top of the towers: ${air.single().boundsInRoot.top} against ${tower.top}")
        assertEquals(total - 1, towers().sumOf { it.size }, "the carried disk is in no pile")
        snap("303_hanoi_carried")
        mainClock.advanceTimeBy(600)
        assertEquals(0, flying().size, "it has landed")
        assertEquals(before[step.to] + before[step.from].last(), towers()[step.to])
        assertEquals(before[step.from].dropLast(1), towers()[step.from])
    }

    @Test
    fun withReducedMotionTheDiskJumpsAtOnce() = runComposeUiTest {
        openHanoi(reduceMotion = true)
        val step = HanoiSolver.shortest(pegs(), goalOf()).first()
        val before = towers()
        tap(step.from)
        mainClock.autoAdvance = false
        onAllNodesWithTag("hanoiTower")[step.to].performClick()
        mainClock.advanceTimeBy(48)
        assertEquals(0, flying().size, "no disk in the air")
        assertEquals(before[step.to] + before[step.from].last(), towers()[step.to])
    }

    @Test
    fun theTowersAreShortAndSizedForTheirDisks() = runComposeUiTest {
        openHanoi()
        val tower = onAllNodesWithTag("hanoiTower").fetchSemanticsNodes().first().boundsInRoot
        val disk = onAllNodesWithTag("hanoiDisk", useUnmergedTree = true).fetchSemanticsNodes().first().boundsInRoot
        // Four disks and a little room: the tower is not much more than seven disks high, whatever the screen's density.
        assertTrue(tower.height / disk.height < 8f, "the tower is about ${tower.height / disk.height} disks high")
        assertTrue(tower.height / tower.width < 2.2f, "and not a thin needle: ${tower.height / tower.width} times its width")
    }

    @Test
    fun theGoalTowerIsTintedGreenAndOnlyThePickedTowerHasAThickYellowBorder() = runComposeUiTest {
        openHanoi()
        val goal = goalOf()
        val other = (0 until 3).first { it != goal && towers()[it].isNotEmpty() }
        fun inside(tower: Int, image: androidx.compose.ui.graphics.ImageBitmap): androidx.compose.ui.graphics.Color {
            val b = onAllNodesWithTag("hanoiTower").fetchSemanticsNodes()[tower].boundsInRoot
            return image.toPixelMap()[(b.left + b.width * 0.18f).toInt(), (b.top + b.height * 0.4f).toInt()]
        }
        fun edge(tower: Int, image: androidx.compose.ui.graphics.ImageBitmap): androidx.compose.ui.graphics.Color {
            val b = onAllNodesWithTag("hanoiTower").fetchSemanticsNodes()[tower].boundsInRoot
            return image.toPixelMap()[(b.left + 2f).toInt(), (b.top + b.height * 0.5f).toInt()]
        }
        val plain = onRoot().captureToImage()
        val goalInside = inside(goal, plain)
        val otherInside = inside(other, plain)
        assertTrue(goalInside.green > goalInside.red + 0.03f && goalInside.green > otherInside.green + 0.02f, "the goal tower is tinted green: $goalInside against $otherInside")
        assertTrue(kotlin.math.abs(otherInside.red - otherInside.green) < 0.02f, "an ordinary tower is gray: $otherInside")

        tap(other)
        val picked = onRoot().captureToImage()
        val pickedEdge = edge(other, picked)
        val goalEdge = edge(goal, picked)
        assertTrue(pickedEdge.red > 0.8f && pickedEdge.green > 0.6f && pickedEdge.blue < 0.4f, "the picked tower has a yellow border: $pickedEdge")
        assertTrue(!(goalEdge.red > 0.6f && goalEdge.green > 0.6f && goalEdge.blue < 0.4f) && goalEdge.green - goalEdge.red < 0.1f, "the goal tower has no colored border: $goalEdge")
        snap("304_hanoi_picked_and_goal")
    }

    private fun ComposeUiTest.description(tower: Int): String =
        onAllNodesWithTag("hanoiTower").fetchSemanticsNodes()[tower].config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].joinToString()

    @Test
    fun aScreenReaderHearsTheDisksOfEveryTowerTheGoalAndWhichTowerIsPickedUp() = runComposeUiTest {
        openHanoi()
        val goal = goalOf()
        for (tower in 0 until 3) {
            val text = description(tower)
            assertTrue(text.contains(('A' + tower).toString()), "the letter of the tower: $text")
            val disks = towers()[tower]
            if (disks.isEmpty()) assertTrue(hasWord(text, "disk yok", "no disks"), "an empty tower says so: $text")
            else assertTrue(text.contains(disks.joinToString(", ")), "the disks from the bottom: $text")
            assertEquals(tower == goal, hasWord(text, "hedef kule", "goal tower"), "only the goal tower says it is: $text")
        }
        val from = (0 until 3).first { towers()[it].isNotEmpty() }
        assertTrue(!isSelected(from))
        tap(from)
        assertTrue(isSelected(from), "the picked tower is selected")
        assertTrue(hasWord(description(from), "seçildi", "picked up"), description(from))
        for (other in (0 until 3).filter { it != from }) assertTrue(!isSelected(other))
    }

    private fun hasWord(text: String, vararg words: String) = words.any { text.contains(it, ignoreCase = true) }

    private fun ComposeUiTest.isSelected(tower: Int): Boolean =
        onAllNodesWithTag("hanoiTower").fetchSemanticsNodes()[tower].config[androidx.compose.ui.semantics.SemanticsProperties.Selected]

    @Test
    fun theWinBuzzesOnlyWhenTheLastDiskHasLanded() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openHanoi(haptics)
        val way = HanoiSolver.shortest(pegs(), goalOf())
        way.dropLast(1).forEach { tap(it.from); tap(it.to) }
        tap(way.last().from)
        haptics.kinds.clear()
        mainClock.autoAdvance = false
        onAllNodesWithTag("hanoiTower")[way.last().to].performClick()
        mainClock.advanceTimeBy(150)
        assertTrue(HapticKind.Success !in haptics.kinds, "no success buzz while the last disk is on its way: ${haptics.kinds}")
        mainClock.advanceTimeBy(1_200)
        assertTrue(HapticKind.Success in haptics.kinds, "the success buzz comes once it has landed: ${haptics.kinds}")
    }

    @Test
    fun tappingThePickedTowerAgainPutsTheDiskDown() = runComposeUiTest {
        openHanoi()
        val from = (0 until 3).first { towers()[it].isNotEmpty() }
        tap(from)
        assertEquals(from, picked())
        tap(from)
        assertEquals(-1, picked())
    }

    @Test
    fun restartPutsTheDisksBackAndKeepsTheMoves() = runComposeUiTest {
        openHanoi()
        val start = towers()
        val step = HanoiSolver.shortest(pegs(), goalOf()).first()
        tap(step.from)
        tap(step.to)
        assertTrue(towers() != start)
        onNodeWithTag("hanoiRestart").performClick()
        waitForIdle()
        assertEquals(start, towers())
        assertTrue(hasAnyText("Hamle 1", "Moves 1"), "the move stays counted")
    }

    @Test
    fun theShortestWayWinsAndShowsTheResult() = runComposeUiTest {
        openHanoi()
        for (step in HanoiSolver.shortest(pegs(), goalOf())) {
            tap(step.from)
            tap(step.to)
        }
        snap("302_hanoi_solved")
        assertTrue(won())
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }
}
