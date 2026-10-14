package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.game.line.LineRound
import com.mehmtcan.brainscroll.game.line.LineSchedule
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.line.LineFilled
import com.mehmtcan.brainscroll.ui.line.LinePage
import com.mehmtcan.brainscroll.ui.line.LineReached
import com.mehmtcan.brainscroll.ui.line.LocalLineShare
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The one-line page on its own (the daily screen around it is tested with the daily puzzle), driven like a finger would. */
@OptIn(ExperimentalTestApi::class)
class LineUiTest {

    /** A Wednesday: a 5x5 board. */
    private val puzzle = LineSchedule.puzzleFor(20740)
    private val line = puzzle.solution
    private val n = puzzle.size
    private var clock = 1_000L
    private var favorite = false

    private class FakeClipboard : ClipboardManager {
        var copied: String? = null
        override fun setText(text: AnnotatedString) { copied = text.text }
        override fun getText(): AnnotatedString? = copied?.let(::AnnotatedString)
    }

    private fun ComposeUiTest.show(haptics: RecordingHaptics = RecordingHaptics(), share: String? = null, clipboard: ClipboardManager? = null): LineRound {
        val round = LineRound(0, "daily-test", puzzle, Language.EN) { clock }
        setContent {
            CompositionLocalProvider(
                LocalHaptics provides haptics,
                LocalLineShare provides share,
                LocalClipboardManager provides (clipboard ?: LocalClipboardManager.current),
            ) {
                BrainScrollTheme {
                    Box(Modifier.requiredSize(390.dp, 740.dp)) {
                        var snapshot by remember { mutableStateOf(round.snapshot()) }
                        LinePage(
                            page = snapshot,
                            onDrag = { cell -> round.drag(cell).also { snapshot = round.snapshot() } },
                            onTap = { cell -> round.tap(cell).also { snapshot = round.snapshot() } },
                            onUndo = { round.undo().also { snapshot = round.snapshot() } },
                            onRestart = { round.restart().also { snapshot = round.snapshot() } },
                            onHint = { round.showHint().also { snapshot = round.snapshot() } },
                            isFavorite = favorite,
                            onToggleFavorite = { favorite = !favorite },
                        )
                    }
                }
            }
        }
        waitForIdle()
        return round
    }

    private fun ComposeUiTest.filled() = onNodeWithTag("lineBoard").fetchSemanticsNode().config[LineFilled]
    private fun ComposeUiTest.reached() = onNodeWithTag("lineBoard").fetchSemanticsNode().config[LineReached]

    private fun ComposeUiTest.tapCell(cell: Int) {
        onAllNodesWithTag("lineCell")[cell].performClick()
        waitForIdle()
    }

    /** The middle of [cell] in the coordinates of the board. */
    private fun ComposeUiTest.center(cell: Int): Offset {
        val size = onNodeWithTag("lineBoard").fetchSemanticsNode().size
        return Offset((cell % n + 0.5f) * size.width / n, (cell / n + 0.5f) * size.height / n)
    }

    /** A finger that goes down on the first cell, over the others in turn, and up on the last. */
    private fun ComposeUiTest.drag(cells: List<Int>, lift: Boolean = true) {
        val points = cells.map { center(it) }
        onNodeWithTag("lineBoard").performTouchInput {
            down(points.first())
            for (point in points.drop(1)) moveTo(point)
            if (lift) up()
        }
        waitForIdle()
    }

    private fun ComposeUiTest.solved() = hasAnyText("Çözüldü!", "Solved!")

    @Test
    fun theBoardStartsEmptyWithOneButtonPerSquare() = runComposeUiTest {
        show()
        snap("170_line_start")
        assertEquals(n * n, onAllNodesWithTag("lineCell").fetchSemanticsNodes().size)
        assertEquals(0, filled())
        assertTrue(hasAnyText("0 / 25 kare", "0 / 25 squares"))
        assertTrue(hasAnyText("Geri alma 0", "Undos 0"))
    }

    @Test
    fun theLineBeginsOnNumberOneAndGrowsWithTaps() = runComposeUiTest {
        val haptics = RecordingHaptics()
        show(haptics)
        val notOne = (0 until n * n).first { it != line[0] }
        tapCell(notOne)
        assertEquals(0, filled(), "the line can only begin on number 1")
        tapCell(line[0])
        assertEquals(1, filled())
        assertEquals(1, reached())
        tapCell(line[2])
        assertEquals(1, filled(), "a square that is not next to the end does nothing")
        tapCell(line[1])
        tapCell(line[2])
        assertEquals(3, filled())
        assertTrue(haptics.kinds.all { it == HapticKind.Light }, "only the squares that went on buzz: ${haptics.kinds}")
        assertEquals(3, haptics.kinds.size)
        assertTrue(hasAnyText("3 / 25 kare", "3 / 25 squares"))
    }

    @Test
    fun aFingerDrawsTheWholeLineAndTheBoardIsSolved() = runComposeUiTest {
        val haptics = RecordingHaptics()
        val round = show(haptics)
        drag(line.toList())
        snap("171_line_solved")
        assertEquals(n * n, filled())
        assertTrue(round.isFinished)
        assertTrue(solved())
        assertEquals(0, onAllNodesWithTag("lineCell").fetchSemanticsNodes().size, "a solved board takes no more touches")
        assertTrue(hasAnyText("3 yıldızdan 3", "3 of 3 stars") || onAllNodesWithTag("lineStars").fetchSemanticsNodes().isNotEmpty())
        assertTrue(HapticKind.Light in haptics.kinds)
    }

    @Test
    fun movingTheFingerBackOverTheLineTakesSquaresOffForFree() = runComposeUiTest {
        val round = show()
        drag(line.take(5), lift = false)
        assertEquals(5, filled())
        onNodeWithTag("lineBoard").performTouchInput {
            moveTo(center(line[3]))
            moveTo(center(line[2]))
            up()
        }
        waitForIdle()
        assertEquals(3, filled())
        assertEquals(0, round.snapshot().state.undos, "going back while drawing is not an undo")
    }

    @Test
    fun aTapOnAnEarlierSquareCutsTheLineAndCountsAsAnUndo() = runComposeUiTest {
        val round = show()
        drag(line.take(6))
        tapCell(line[2])
        assertEquals(3, filled())
        assertEquals(1, round.snapshot().state.undos)
        assertTrue(hasAnyText("Geri alma 1", "Undos 1"))
    }

    @Test
    fun theFingerDoesNotSkipOrFlickerWhenItCutsACorner() = runComposeUiTest {
        show()
        drag(line.take(2), lift = false)
        // Near the corner where four squares meet: nothing counts there while moving.
        onNodeWithTag("lineBoard").performTouchInput {
            val size = this.visibleSize
            val corner = Offset(2f * size.width / n, 2f * size.height / n)
            moveTo(corner)
            up()
        }
        waitForIdle()
        assertEquals(2, filled())
    }

    @Test
    fun aNumberOutOfOrderIsRefusedWithAWordAndABuzz() = runComposeUiTest {
        val haptics = RecordingHaptics()
        show(haptics)
        // Walk the solution until the end of the line sits next to a number that is not the next one.
        var wrong: Pair<Int, Int>? = null // the length of the line to draw and the square to touch
        val numberOf = puzzle.numbers
        for (k in 0 until n * n - 1) {
            val reached = line.take(k + 1).count { numberOf[it] > 0 }
            val head = line[k]
            val next = (0 until n * n).firstOrNull { c ->
                numberOf[c] > reached + 1 && c !in line.take(k + 1) &&
                    kotlin.math.abs(c / n - head / n) + kotlin.math.abs(c % n - head % n) == 1
            }
            if (next != null) { wrong = (k + 1) to next; break }
        }
        val (length, cell) = checkNotNull(wrong) { "this board has no number next to the line out of order" }
        drag(line.take(length))
        haptics.kinds.clear()
        tapCell(cell)
        assertEquals(length, filled(), "the number was not entered")
        assertTrue(hasAnyText("Önce", "Pass number"))
        assertEquals(listOf(HapticKind.Warning), haptics.kinds)
        // The next square that works takes the word away again.
        tapCell(line[length])
        assertTrue(hasAnyText("Önce", "Pass number").not())
    }

    @Test
    fun undoTakesTheLastSquareOffAndRestartClearsTheLine() = runComposeUiTest {
        val round = show()
        drag(line.take(4))
        onNodeWithTag("lineUndo").performClick()
        waitForIdle()
        assertEquals(3, filled())
        onNodeWithTag("lineRestart").performClick()
        waitForIdle()
        assertEquals(0, filled())
        assertEquals(2, round.snapshot().state.undos)
        assertTrue(hasAnyText("Geri alma 2", "Undos 2"))
    }

    @Test
    fun aHintShowsTheNextSquaresCountsAndGoesAwayWithTheNextMove() = runComposeUiTest {
        val round = show()
        drag(line.take(2))
        onNodeWithTag("lineHint").performClick()
        waitForIdle()
        assertEquals(1, round.snapshot().state.hintsUsed)
        assertEquals(line.drop(2).take(3), round.snapshot().hint)
        assertTrue(hasAnyText("İpucu 1", "Hints 1"))
        tapCell(line[2])
        assertTrue(round.snapshot().hint.isEmpty())
    }

    @Test
    fun aHintOnANumberedSquareIsShownAsARingAroundTheNumber() = runComposeUiTest {
        val round = show()
        drag(line.take(1))
        val target = line[1]
        assertTrue(puzzle.numbers[target] > 0, "this board has a number right after number 1")
        val side = onNodeWithTag("lineBoard").fetchSemanticsNode().size.width.toFloat()
        val cell = side / n
        val at = center(target) + Offset(cell * 0.38f, 0f)
        val before = onNodeWithTag("lineBoard").captureToImage().toPixelMap()[at.x.toInt(), at.y.toInt()]
        onNodeWithTag("lineHint").performClick()
        waitForIdle()
        assertTrue(target in round.snapshot().hint)
        val after = onNodeWithTag("lineBoard").captureToImage().toPixelMap()[at.x.toInt(), at.y.toInt()]
        assertNotEquals(before, after, "the numbered square carries the hint mark too")
        snap("173_line_hint_on_number")
    }

    @Test
    fun whatIsDrawnFollowsTheLine() = runComposeUiTest {
        show()
        val plain = (0 until n * n).first { puzzle.numbers[it] == 0 && it != line[1] }
        val onLine = line.drop(1).first { puzzle.numbers[it] == 0 }
        val before = onNodeWithTag("lineBoard").captureToImage().toPixelMap()
        val at = center(onLine)
        val pixelBefore = before[at.x.toInt() + 12, at.y.toInt()]
        drag(line.take(line.indexOf(onLine) + 1))
        val after = onNodeWithTag("lineBoard").captureToImage().toPixelMap()
        assertNotEquals(pixelBefore, after[at.x.toInt() + 12, at.y.toInt()], "the line is drawn over the square")
        val other = center(plain)
        if (plain !in line.take(line.indexOf(onLine) + 1)) {
            assertEquals(before[other.x.toInt(), other.y.toInt()], after[other.x.toInt(), other.y.toInt()], "a square off the line is untouched")
        }
    }

    @Test
    fun theResultOffersToCopyTheShareLineAndSaysSo() = runComposeUiTest {
        val clipboard = FakeClipboard()
        show(share = "One Line #4 3 stars 00:48", clipboard = clipboard)
        drag(line.toList())
        snap("172_line_share")
        assertTrue(onAllNodesWithTag("lineShare").fetchSemanticsNodes().isNotEmpty(), "a solved board with a share line has the button")
        assertEquals(null, clipboard.copied)
        onNodeWithTag("lineShare").performClick()
        waitForIdle()
        assertEquals("One Line #4 3 stars 00:48", clipboard.copied)
        assertTrue(hasAnyText("Kopyalandı", "Copied"))
    }

    @Test
    fun withoutAShareLineThereIsNoShareButton() = runComposeUiTest {
        show()
        drag(line.toList())
        assertEquals(0, onAllNodesWithTag("lineShare").fetchSemanticsNodes().size)
    }

    @Test
    fun theHeartOnTheResultTogglesTheFavorite() = runComposeUiTest {
        show()
        drag(line.toList())
        onNodeWithTag("favoriteHeart").performClick()
        waitForIdle()
        assertTrue(favorite)
    }
}
