package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.blockout.BlockMove
import com.mehmtcan.brainscroll.game.blockout.BlockOutSolver
import com.mehmtcan.brainscroll.game.blockout.PieceShape
import com.mehmtcan.brainscroll.ui.blockout.BlockOutPieceInfo
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The block out page in the feed, driven through the real screen. The board is random, so the tests read the blocks the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class BlockOutUiTest {

    private val games = listOf(GameKind.Word, GameKind.BlockOut)
    private val size = 5

    private fun ComposeUiTest.openBlockOut(haptics: RecordingHaptics? = null, reduceMotion: Boolean? = null) {
        start(haptics = haptics, reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("blockoutPiece").fetchSemanticsNodes().isNotEmpty() }
    }

    /** Every block as [id, place, length, lies, line, target]. */
    private fun ComposeUiTest.pieces(): List<List<Int>> = onAllNodesWithTag("blockoutPiece").fetchSemanticsNodes().map { it.config[BlockOutPieceInfo] }.sortedBy { it[0] }

    private fun ComposeUiTest.shapes() = pieces().map { PieceShape(it[3] == 1, it[4], it[2], it[5] == 1) }

    private fun ComposeUiTest.way(): List<BlockMove> =
        BlockOutSolver.shortest(size, shapes(), IntArray(pieces().size) { pieces()[it][1] })!!

    private fun ComposeUiTest.piece(id: Int): SemanticsNodeInteraction = onAllNodesWithTag("blockoutPiece")[pieces().indexOfFirst { it[0] == id }]

    private fun ComposeUiTest.settle() {
        mainClock.advanceTimeBy(500)
        waitForIdle()
    }

    /** Moves a block the way a finger does without dragging: tap it, then tap the cell it should go to. */
    private fun ComposeUiTest.tapMove(move: BlockMove) {
        val info = pieces().first { it[0] == move.piece }
        val horizontal = info[3] == 1
        // The cell the near end of the block goes to.
        val at = if (move.to > info[1]) move.to + info[2] - 1 else move.to
        val row = if (horizontal) info[4] else at
        val column = if (horizontal) at else info[4]
        piece(move.piece).performClick()
        waitForIdle()
        onAllNodesWithTag("blockoutCell")[row * size + column].performClick()
        settle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Yol açıldı!", "The way is clear!")

    @Test
    fun theBoardIsFiveByFiveWithTheTargetInTheMiddleRowAndTheCountersOnTop() = runComposeUiTest {
        openBlockOut()
        snap("300_blockout_start")
        assertEquals(size * size, onAllNodesWithTag("blockoutCell").fetchSemanticsNodes().size)
        val target = pieces().single { it[5] == 1 }
        assertEquals(2, target[4], "the target lies in the middle row")
        assertEquals(1, target[3])
        assertTrue(hasAnyText("Blok Çıkışı", "Block Out"))
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
        assertTrue(Regex("(En az|Fewest) \\d+").let { re -> hasAnyText(*(8..30).flatMap { listOf("En az $it", "Fewest $it") }.toTypedArray()) })
    }

    @Test
    fun tappingABlockAndThenACellSlidesItAndCountsOneMove() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openBlockOut(haptics)
        val move = way().first()
        haptics.kinds.clear()
        tapMove(move)
        assertEquals(move.to, pieces().first { it[0] == move.piece }[1])
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
        assertTrue(HapticKind.Medium in haptics.kinds)
        snap("301_blockout_moved")
    }

    @Test
    fun draggingABlockAlongItsLengthSlidesItToTheNearestCellAndCountsOneMove() = runComposeUiTest {
        openBlockOut()
        val move = way().first()
        val info = pieces().first { it[0] == move.piece }
        val horizontal = info[3] == 1
        val cells = move.to - info[1]
        val node = piece(move.piece)
        // The block is the width of a cell plus a gap per step; a drag of that many cells, from its middle.
        val step = node.fetchSemanticsNode().boundsInRoot.let { b -> (if (horizontal) b.width else b.height) / info[2] }
        node.performTouchInput {
            val from = center
            val to = if (horizontal) Offset(from.x + cells * step, from.y) else Offset(from.x, from.y + cells * step)
            swipe(from, to, durationMillis = 300)
        }
        settle()
        assertEquals(move.to, pieces().first { it[0] == move.piece }[1], "a drag of several cells is one move")
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
    }

    @Test
    fun aBlockCannotBeDraggedThroughAnotherOne() = runComposeUiTest {
        openBlockOut()
        // Find a block that has another block right beside it along its line, and drag it far into that block.
        val shapes = shapes()
        val pos = IntArray(shapes.size) { pieces()[it][1] }
        val blocked = shapes.indices.first { i -> BlockOutSolver.range(size, shapes, pos, i).let { it.last < size - shapes[i].length || it.first > 0 } }
        val before = pieces()[blocked][1]
        val info = pieces()[blocked]
        val horizontal = info[3] == 1
        val range = BlockOutSolver.range(size, shapes, pos, blocked)
        val towardsFar = if (range.last < size - shapes[blocked].length) 1 else -1
        val node = piece(blocked)
        val step = node.fetchSemanticsNode().boundsInRoot.let { b -> (if (horizontal) b.width else b.height) / info[2] }
        node.performTouchInput {
            val from = center
            val distance = 4 * step * towardsFar
            swipe(from, if (horizontal) Offset(from.x + distance, from.y) else Offset(from.x, from.y + distance), durationMillis = 300)
        }
        settle()
        val after = pieces()[blocked][1]
        assertTrue(after in range, "it stays within what it can reach: $after in $range")
        if (after != before) assertTrue(hasAnyText("Hamle 1", "Moves 1")) else assertTrue(hasAnyText("Hamle 0", "Moves 0"))
    }

    @Test
    fun tappingACellThatTheBlockCannotReachBuzzesAndMovesNothing() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openBlockOut(haptics)
        val first = pieces().first { it[5] != 1 }
        piece(first[0]).performClick()
        waitForIdle()
        // An empty cell that is not in the block's line (a cell under another block would take the tap instead).
        val covered = pieces().flatMap { p -> (0 until p[2]).map { k -> if (p[3] == 1) p[4] * size + p[1] + k else (p[1] + k) * size + p[4] } }.toSet()
        val horizontal = first[3] == 1
        val outside = (0 until size * size).first { cell ->
            cell !in covered && (if (horizontal) cell / size != first[4] else cell % size != first[4])
        }
        val row = outside / size
        val column = outside % size
        haptics.kinds.clear()
        onAllNodesWithTag("blockoutCell")[row * size + column].performClick()
        waitForIdle()
        assertEquals(HapticKind.Warning, haptics.kinds.last())
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
    }

    @Test
    fun aScreenReaderHearsEachBlockAndCanSlideItWithActions() = runComposeUiTest {
        openBlockOut()
        val target = pieces().single { it[5] == 1 }
        val text = piece(target[0]).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()
        assertTrue(text.contains("turuncu", ignoreCase = true) || text.contains("orange", ignoreCase = true), text)
        // Slide one block one cell with the custom action of a screen reader.
        val shapes = shapes()
        val pos = IntArray(shapes.size) { pieces()[it][1] }
        val mover = shapes.indices.first { BlockOutSolver.range(size, shapes, pos, it).let { r -> r.last > r.first } }
        val range = BlockOutSolver.range(size, shapes, pos, mover)
        val actions = piece(mover).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(2, actions.size)
        val forward = pos[mover] < range.last
        val action = if (forward) actions[1] else actions[0]
        runOnUiThread { action.action() }
        settle()
        assertEquals(pos[mover] + if (forward) 1 else -1, pieces()[mover][1])
        assertTrue(hasAnyText("Hamle 1", "Moves 1"))
    }

    @Test
    fun restartPutsTheBlocksBackAndKeepsTheMoves() = runComposeUiTest {
        openBlockOut()
        val start = pieces()
        tapMove(way().first())
        assertTrue(pieces() != start)
        onNodeWithTag("blockoutRestart").performClick()
        settle()
        assertEquals(start, pieces())
        assertTrue(hasAnyText("Hamle 1", "Moves 1"), "the move stays counted")
    }

    @Test
    fun theWinBuzzesOnlyWhenTheTargetHasSlidOut() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openBlockOut(haptics)
        val way = way()
        way.dropLast(1).forEach { tapMove(it) }
        val last = way.last()
        val info = pieces().first { it[0] == last.piece }
        piece(last.piece).performClick()
        waitForIdle()
        haptics.kinds.clear()
        mainClock.autoAdvance = false
        onAllNodesWithTag("blockoutCell")[info[4] * size + (last.to + info[2] - 1)].performClick()
        mainClock.advanceTimeBy(60)
        assertTrue(HapticKind.Success !in haptics.kinds, "no success buzz while the target is still sliding: ${haptics.kinds}")
        mainClock.advanceTimeBy(1_200)
        assertTrue(HapticKind.Success in haptics.kinds, "the success buzz comes once it has slid out: ${haptics.kinds}")
    }

    @Test
    fun withReducedMotionABlockJumpsAtOnce() = runComposeUiTest {
        openBlockOut(reduceMotion = true)
        val move = way().first()
        val info = pieces().first { it[0] == move.piece }
        piece(move.piece).performClick()
        waitForIdle()
        val at = if (move.to > info[1]) move.to + info[2] - 1 else move.to
        val horizontal = info[3] == 1
        mainClock.autoAdvance = false
        onAllNodesWithTag("blockoutCell")[(if (horizontal) info[4] else at) * size + (if (horizontal) at else info[4])].performClick()
        mainClock.advanceTimeBy(32)
        val bounds = piece(move.piece).fetchSemanticsNode().boundsInRoot
        val boardBounds = onNodeWithTag("blockoutBoard").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.left >= boardBounds.left && bounds.right <= boardBounds.right, "it is on the board")
        assertEquals(move.to, pieces().first { it[0] == move.piece }[1])
    }

    @Test
    fun theShortestWayWinsAndShowsTheResult() = runComposeUiTest {
        openBlockOut()
        for (move in way()) tapMove(move)
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("302_blockout_solved")
        assertTrue(won())
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }
}
