package com.mehmtcan.brainscroll.game.blockout

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockOutRoundTest {

    private var clock = 1_000L

    private val board = BlockOutGenerator.generate(4)

    private fun round() = BlockOutRound(0, "b1", board.puzzle, Language.EN, { clock }, board.par)

    private val way get() = assertNotNull(BlockOutSolver.shortest(board.puzzle.size, board.puzzle.shapes, board.puzzle.start))

    private fun BlockOutRound.finish(stepMillis: Long = 1_000) {
        val pos = snapshot().state.pieces.map { it.pos }.toIntArray()
        for (move in BlockOutSolver.shortest(board.puzzle.size, board.puzzle.shapes, pos)!!) {
            clock += stepMillis
            move(move.piece, move.to)
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstMove() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.BlockOut, r.kind)
        assertEquals(board.par, r.snapshot().state.par)
        r.move(way[0].piece, way[0].to)
        assertTrue(!r.isUntouched)
    }

    @Test
    fun aRefusedMoveChangesNothingNotEvenTheClock() {
        val r = round()
        r.move(way[0].piece, way[0].to)
        clock += 4_000
        assertEquals(BlockOutResult.Ignored, r.move(0, 99))
        clock += 3_000
        r.move(way[1].piece, way[1].to)
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(2, r.toStored().guesses.size)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        clock = 10_000
        r.move(way[0].piece, way[0].to)
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.move(way[1].piece, way[1].to)
        clock += 10 * 60_000
        r.move(way[2].piece, way[2].to)
        assertEquals(2_500 + BlockOutRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsBoardAndActions() {
        val r = round()
        r.move(way[0].piece, way[0].to)
        r.move(way[1].piece, way[1].to)
        r.restart()
        r.move(way[0].piece, way[0].to)
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.BlockOut, stored.game)
        assertEquals(board.puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertEquals(listOf("m${way[0].piece}:${way[0].to}", "m${way[1].piece}:${way[1].to}", "r", "m${way[0].piece}:${way[0].to}"), stored.guesses)

        val back = assertNotNull(BlockOutRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(BlockOutRound.restore(0, stored.copy(answer = "XX"), { clock }))
        assertNull(BlockOutRound.restore(0, stored.copy(guesses = listOf("x1:2")), { clock }))
        assertNull(BlockOutRound.restore(0, stored.copy(guesses = listOf("m1")), { clock }))
        assertNull(BlockOutRound.restore(0, stored.copy(guesses = listOf("ma:2")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfMoves() {
        val r = round()
        clock = 5_000
        r.finish()
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.BlockOut, finished.game)
        assertEquals(listOf(board.par.toString()), finished.guesses)
        assertEquals(board.par, finished.moveCount)
        assertTrue(finished.answer.length <= 32)
        assertEquals((board.par - 1) * 1_000L, finished.durationMs)
    }

    @Test
    fun theGuideSaysTheFirstMoveOfTheShortestWayFromWhereTheBoardIs() {
        val r = round()
        val step = assertNotNull(assertNotNull(r.guideTask())())
        val first = way[0]
        val piece = r.snapshot().state.pieces[first.piece]
        assertEquals(GuideReason.BlockOutSlide, step.reason)
        assertEquals(listOf(first.piece), step.cells)
        assertEquals(kotlin.math.abs(first.to - piece.pos), step.numbers[1])
        val direction = step.numbers[0]
        assertTrue(if (piece.horizontal) direction in listOf(2, 3) else direction in listOf(0, 1))
        assertEquals(first.to < piece.pos, direction == 0 || direction == 2)

        // After a move that is not on the shortest way the guide works from the new position.
        r.finish()
        assertNull(r.guideTask())
    }
}
