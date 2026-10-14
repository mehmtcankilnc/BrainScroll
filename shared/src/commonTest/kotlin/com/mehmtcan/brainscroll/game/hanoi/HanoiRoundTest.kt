package com.mehmtcan.brainscroll.game.hanoi

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

class HanoiRoundTest {

    private var clock = 1_000L

    private val puzzle = HanoiGenerator.generate(4)

    private fun round() = HanoiRound(0, "h1", puzzle.start.toList(), puzzle.goal, Language.EN, { clock }, puzzle.par)

    /** Plays the shortest way from the position the round is in now. */
    private fun HanoiRound.finish(stepMillis: Long = 1_000) {
        for (step in HanoiSolver.shortest(snapshot().state.pegs.toIntArray(), puzzle.goal)) {
            clock += stepMillis
            move(step.from, step.to)
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstMove() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Hanoi, r.kind)
        assertEquals(puzzle.par, r.snapshot().state.par)
        val first = HanoiSolver.shortest(puzzle.start, puzzle.goal).first()
        r.move(first.from, first.to)
        assertTrue(!r.isUntouched)
    }

    @Test
    fun aRefusedMoveChangesNothingNotEvenTheClock() {
        val r = round()
        val way = HanoiSolver.shortest(puzzle.start, puzzle.goal)
        r.move(way[0].from, way[0].to)
        clock += 4_000
        assertEquals(HanoiResult.Ignored, r.move(0, 0))
        clock += 3_000
        r.move(way[1].from, way[1].to)
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(2, r.toStored().guesses.size)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        val way = HanoiSolver.shortest(puzzle.start, puzzle.goal)
        clock = 10_000
        r.move(way[0].from, way[0].to)
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.move(way[1].from, way[1].to)
        clock += 10 * 60_000
        r.move(way[2].from, way[2].to)
        assertEquals(2_500 + HanoiRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsPositionAndActions() {
        val r = round()
        val way = HanoiSolver.shortest(puzzle.start, puzzle.goal)
        r.move(way[0].from, way[0].to)
        r.move(way[1].from, way[1].to)
        r.restart()
        r.move(way[0].from, way[0].to)
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Hanoi, stored.game)
        assertEquals(puzzle.start.joinToString("") + ">" + puzzle.goal, stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertEquals(listOf("m${way[0].from}${way[0].to}", "m${way[1].from}${way[1].to}", "r", "m${way[0].from}${way[0].to}"), stored.guesses)

        val back = assertNotNull(HanoiRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(HanoiRound.restore(0, stored.copy(answer = "0120"), { clock }), "no goal")
        assertNull(HanoiRound.restore(0, stored.copy(answer = "01a0>2"), { clock }), "not a tower")
        assertNull(HanoiRound.restore(0, stored.copy(answer = "0>2"), { clock }), "one disk is not a puzzle")
        assertNull(HanoiRound.restore(0, stored.copy(answer = "0120>7"), { clock }), "no such goal tower")
        assertNull(HanoiRound.restore(0, stored.copy(guesses = listOf("x01")), { clock }))
        assertNull(HanoiRound.restore(0, stored.copy(guesses = listOf("m0")), { clock }))
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
        assertEquals(GameKind.Hanoi, finished.game)
        assertEquals(listOf(puzzle.par.toString()), finished.guesses)
        assertEquals(puzzle.par, finished.moveCount)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters")
        assertEquals((puzzle.par - 1) * 1_000L, finished.durationMs)
    }

    @Test
    fun theGuideSaysTheFirstMoveOfTheShortestWayFromWhereTheBoardIs() {
        val r = round()
        val step = assertNotNull(assertNotNull(r.guideTask())())
        val first = HanoiSolver.shortest(puzzle.start, puzzle.goal).first()
        assertEquals(GuideReason.HanoiMove, step.reason)
        assertEquals(listOf(first.from, first.to), step.cells)
        assertEquals(listOf(first.from, first.to), step.numbers)

        // A move away from the shortest way: the guide works from the new position.
        val wrong = HanoiSolver.legalMoves(puzzle.start).first { it != first }
        r.move(wrong.from, wrong.to)
        val again = assertNotNull(assertNotNull(r.guideTask())())
        val now = HanoiSolver.shortest(r.snapshot().state.pegs.toIntArray(), puzzle.goal).first()
        assertEquals(listOf(now.from, now.to), again.cells)
        r.finish()
        assertNull(r.guideTask())
    }
}
