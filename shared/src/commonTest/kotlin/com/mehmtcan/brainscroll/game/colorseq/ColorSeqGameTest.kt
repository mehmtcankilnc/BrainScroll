package com.mehmtcan.brainscroll.game.colorseq

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.ColorSeqGuide
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorSeqGameTest {
    private val puzzle = ColorSeqPuzzle(4, 1, 5, 600, 12345)

    private fun wrongButton(expected: Int) = (0 until 4).first { it != expected }

    @Test
    fun theSequenceOnlyDependsOnTheSeedAndNeverRepeatsOneButtonThreeTimes() {
        assertEquals(5, puzzle.rounds)
        assertEquals(5, puzzle.sequence.size)
        assertEquals(puzzle.sequence, ColorSeqPuzzle(4, 1, 5, 600, 12345).sequence, "the same run plays the same lights")
        assertTrue((0 until 20).any { ColorSeqPuzzle(4, 1, 5, 600, it).sequence != puzzle.sequence }, "other seeds play other lights")
        for (seed in 0 until 300) for (buttons in 3..6) {
            val s = ColorSeqPuzzle(buttons, 1, 15, 400, seed).sequence
            assertTrue(s.all { it in 0 until buttons })
            assertTrue((2 until s.size).none { s[it] == s[it - 1] && s[it] == s[it - 2] }, "seed $seed has one button three times in a row")
        }
    }

    @Test
    fun everyRoundIsTheBeginningOfTheSequenceWithOneLightMore() {
        for (round in 0 until puzzle.rounds) {
            assertEquals(1 + round, puzzle.steps(round).size)
            assertEquals(puzzle.sequence.take(1 + round), puzzle.steps(round))
        }
    }

    @Test
    fun theRunIsSavedAndReadBackInAFewCharacters() {
        for (difficulty in Difficulty.entries) for (seed in 0 until 50) {
            val p = ColorSeqGenerator.generate(seed, difficulty)
            val text = p.encode()
            assertTrue(text.length <= 16, text)
            assertEquals(p, ColorSeqPuzzle.decode(text))
        }
        assertNull(ColorSeqPuzzle.decode(""))
        assertNull(ColorSeqPuzzle.decode("zzzzz"))
        assertNull(ColorSeqPuzzle.decode("4 9 x"))
    }

    @Test
    fun aRunThatCannotBeIsRefused() {
        fun refused(block: () -> ColorSeqPuzzle) = runCatching(block).isFailure
        assertTrue(refused { ColorSeqPuzzle(2, 1, 5, 600, 0) })
        assertTrue(refused { ColorSeqPuzzle(7, 1, 5, 600, 0) })
        assertTrue(refused { ColorSeqPuzzle(4, 6, 5, 600, 0) })
        assertTrue(refused { ColorSeqPuzzle(4, 1, 16, 600, 0) })
        assertTrue(refused { ColorSeqPuzzle(4, 1, 5, 610, 0) })
        assertTrue(refused { ColorSeqPuzzle(4, 1, 5, 600, -1) })
    }

    @Test
    fun theLevelsMakeTheRunsTheyAreMeantFor() {
        val easy = ColorSeqGenerator.generate(1, Difficulty.Easy)
        val medium = ColorSeqGenerator.generate(1, Difficulty.Medium)
        val hard = ColorSeqGenerator.generate(1, Difficulty.Hard)
        assertEquals(listOf(4, 4, 6), listOf(easy.buttons, medium.buttons, hard.buttons))
        assertEquals(listOf(6, 9, 12), listOf(easy.targetLength, medium.targetLength, hard.targetLength))
        assertTrue(easy.stepMs > medium.stepMs && medium.stepMs > hard.stepMs, "the lights go faster")
        assertEquals(1, easy.startLength, "the first round has one light")
        assertEquals(hard, ColorSeqGenerator.generate(1, Difficulty.Hard))
    }

    @Test
    fun repeatingEveryLightOfARoundClearsItAndTheNextHasOneMore() {
        val game = ColorSeqGame(puzzle).also { it.start() }
        assertEquals(ColorSeqResult.RoundCleared, game.tap(puzzle.steps(0)[0]))
        assertEquals(1, game.state.roundsCleared)
        assertEquals(puzzle.steps(1), game.state.sequence)
        val second = puzzle.steps(1)
        assertEquals(ColorSeqResult.Right, game.tap(second[0]))
        assertEquals(1, game.state.entered)
        assertEquals(ColorSeqResult.RoundCleared, game.tap(second[1]))
        assertEquals(0, game.state.entered)
        assertFalse(game.isFinished)
    }

    @Test
    fun aWrongButtonEndsTheRunAndKeepsWhatWasDue() {
        val game = ColorSeqGame(puzzle).also { it.start() }
        game.tap(puzzle.steps(0)[0])
        val due = puzzle.steps(1)[0]
        val wrong = wrongButton(due)
        assertEquals(ColorSeqResult.Missed, game.tap(wrong))
        assertTrue(game.isFinished)
        assertEquals(ColorSeqStatus.Lost, game.state.status)
        assertEquals(wrong, game.state.missed)
        assertEquals(due, game.state.expected)
        assertEquals(ColorSeqResult.Ignored, game.tap(due))
    }

    @Test
    fun theRightButtonInTheWrongPlaceIsWrong() {
        // The second light of round 1 is not the first one's button unless the sequence repeats it; use the one that differs.
        val game = ColorSeqGame(puzzle).also { it.start() }
        game.tap(puzzle.steps(0)[0])
        val second = puzzle.steps(1)
        game.tap(second[0])
        val wrong = wrongButton(second[1])
        assertEquals(ColorSeqResult.Missed, game.tap(wrong))
        assertEquals(1, game.state.roundsCleared)
    }

    @Test
    fun clearingTheLastRoundWins() {
        val game = ColorSeqGame(puzzle).also { it.start() }
        var last = ColorSeqResult.Ignored
        for (round in 0 until puzzle.rounds) for (button in puzzle.steps(round)) last = game.tap(button)
        assertEquals(ColorSeqResult.Perfect, last)
        assertEquals(ColorSeqStatus.Won, game.state.status)
        assertEquals(puzzle.rounds, game.state.roundsCleared)
        assertEquals(ColorSeqResult.Ignored, game.tap(0))
    }

    @Test
    fun aButtonOffTheBoardIsIgnored() {
        val game = ColorSeqGame(puzzle).also { it.start() }
        assertEquals(ColorSeqResult.Ignored, game.tap(-1))
        assertEquals(ColorSeqResult.Ignored, game.tap(4))
        assertFalse(game.isFinished)
    }

    @Test
    fun nothingCanBeTouchedBeforeStartIsPressed() {
        val game = ColorSeqGame(puzzle)
        assertFalse(game.state.started)
        assertEquals(ColorSeqResult.Ignored, game.tap(puzzle.steps(0)[0]))
        assertEquals(ColorSeqResult.Ignored, game.tap(wrongButton(puzzle.steps(0)[0])), "a wrong button does not count either")
        assertFalse(game.isFinished)
        assertTrue(game.start())
        assertFalse(game.start(), "started once")
        assertEquals(1, game.state.showToken, "the lights are played when start is pressed")
    }

    @Test
    fun clearingARoundPlaysTheNextOneAndARightButtonPlaysNothing() {
        val game = ColorSeqGame(puzzle)
        game.start()
        val token = game.state.showToken
        game.tap(puzzle.steps(0)[0])
        assertEquals(token + 1, game.state.showToken, "a cleared round plays the next one")
        game.tap(puzzle.steps(1)[0])
        assertEquals(token + 1, game.state.showToken, "a right button in the middle of a round plays nothing")
    }

    @Test
    fun aSavedRunCanOnlyBeOneThatWasNotWon() {
        val game = ColorSeqGame(puzzle)
        assertFalse(game.restore(-1, true))
        assertFalse(game.restore(puzzle.rounds, true))
        assertFalse(game.restore(2, false), "rounds cleared with no start")
        assertTrue(game.restore(2, true))
        assertEquals(2, game.state.roundsCleared)
        assertTrue(game.state.started)
        assertEquals(puzzle.steps(2), game.state.sequence)
        assertTrue(ColorSeqGame(puzzle).restore(0, false), "a run that was not started")
    }

    @Test
    fun startingOverGivesANewSequenceAndNeedsStartAgain() {
        val game = ColorSeqGame(puzzle)
        assertFalse(game.restart(game.freshPuzzle()), "nothing started yet")
        game.start()
        game.tap(puzzle.steps(0)[0])
        assertEquals(1, game.state.roundsCleared)
        val fresh = game.freshPuzzle()
        assertEquals(puzzle.buttons, fresh.buttons)
        assertEquals(puzzle.stepMs, fresh.stepMs)
        assertEquals(puzzle.targetLength, fresh.targetLength)
        assertNotEquals(puzzle.seed, fresh.seed)
        assertTrue(game.restart(fresh))
        assertEquals(fresh, game.puzzle)
        assertEquals(0, game.state.roundsCleared)
        assertFalse(game.state.started)
        assertEquals(ColorSeqResult.Ignored, game.tap(fresh.steps(0)[0]))
        assertTrue(game.start())
    }

    @Test
    fun aRunThatIsOverCannotBeStartedOver() {
        val game = ColorSeqGame(puzzle)
        game.start()
        game.tap(wrongButton(puzzle.steps(0)[0]))
        assertFalse(game.restart(game.freshPuzzle()))
        assertFalse(game.start())
    }

    // --- the guide ---

    @Test
    fun theGuidePlaysTheLightsOfTheRoundAgainOnlyOnceTheRunIsStarted() {
        val game = ColorSeqGame(puzzle)
        assertNull(ColorSeqGuide.step(game.state), "nothing to replay before start")
        game.start()
        game.tap(puzzle.steps(0)[0])
        val step = ColorSeqGuide.step(game.state)!!
        assertEquals(GuideReason.SeqReplay, step.reason)
        assertEquals(puzzle.steps(1), step.cells)
        game.tap(wrongButton(puzzle.steps(1)[0]))
        assertNull(ColorSeqGuide.step(game.state))
    }

    // --- the round ---

    private fun round(clock: () -> Long = { 0L }) = ColorSeqRound(0, "c1", puzzle, Language.EN, clock).also { it.start() }

    @Test
    fun theRoundSavesAndRestoresItsRoundsAndTime() {
        var now = 0L
        val round = round { now }
        round.onShown()
        round.tap(puzzle.steps(0)[0])
        now = 5_000
        val stored = round.toStored()
        assertEquals(listOf("r1", "s1"), stored.guesses)
        assertEquals(puzzle.encode(), stored.answer)
        assertEquals(GameKind.ColorSeq, stored.game)
        assertEquals("5000", stored.input)
        val back = ColorSeqRound.restore(0, stored, { now })!!
        assertEquals(1, back.snapshot().state.roundsCleared)
        assertTrue(back.snapshot().state.started)
        assertFalse(back.isUntouched)
    }

    @Test
    fun aRunLostOrWonIsAResultWithItsRounds() {
        val lost = round()
        lost.tap(puzzle.steps(0)[0])
        assertEquals(ColorSeqResult.Missed, lost.tap(wrongButton(puzzle.steps(1)[0])))
        assertTrue(lost.isFinished)
        val lostRow = lost.toFinished(1L)
        assertEquals(Outcome.LOST, lostRow.outcome)
        assertEquals(Mode.ENDLESS, lostRow.mode)
        assertEquals(listOf("1"), lostRow.guesses)
        assertEquals(1, lostRow.moveCount)

        val won = round()
        for (r in 0 until puzzle.rounds) puzzle.steps(r).forEach { won.tap(it) }
        val wonRow = won.toFinished(1L)
        assertEquals(Outcome.WON, wonRow.outcome)
        assertEquals(puzzle.rounds, wonRow.moveCount)
        assertTrue(won.snapshot().isWon)
        assertFalse(lost.snapshot().isWon)
    }

    @Test
    fun aSavedGameThatCannotBeReadStartsNew() {
        val stored = round().toStored()
        assertNull(ColorSeqRound.restore(0, stored.copy(answer = "!!"), { 0L }))
        assertNull(ColorSeqRound.restore(0, stored.copy(guesses = listOf("x1")), { 0L }))
        assertNull(ColorSeqRound.restore(0, stored.copy(guesses = listOf("r${puzzle.rounds}", "s1")), { 0L }), "a won run would not have been saved")
        assertNull(ColorSeqRound.restore(0, stored.copy(guesses = listOf("r2")), { 0L }), "rounds cleared with no start")
    }

    @Test
    fun theTimeIsThePageTimeAndStopsWhenTheRunIsOver() {
        var now = 0L
        val round = round { now }
        round.onShown()
        now = 7_000
        assertEquals(7_000L, round.snapshot().durationMs, "looking at the lights counts")
        round.onHidden()
        now = 99_000
        assertEquals(7_000L, round.snapshot().durationMs, "away from the page the clock stands")
        round.onShown()
        now = 100_000
        round.tap(wrongButton(puzzle.steps(0)[0]))
        now = 200_000
        assertEquals(8_000L, round.toFinished(1L).durationMs, "the clock stopped at the wrong button")
    }

    @Test
    fun startingOverKeepsTheRunAsAnotherOneInTheSavedRound() {
        val round = round()
        val before = round.toStored().answer
        round.tap(puzzle.steps(0)[0])
        assertTrue(round.restart())
        val stored = round.toStored()
        assertNotEquals(before, stored.answer, "the new sequence is the saved one")
        assertEquals(listOf("r0"), stored.guesses, "not started: no s1")
        val back = ColorSeqRound.restore(0, stored, { 0L })!!
        assertFalse(back.snapshot().state.started)
    }

    @Test
    fun aRoundThatWasNeverTouchedIsUntouched() {
        val round = ColorSeqRound(0, "c8", puzzle, Language.EN)
        assertTrue(round.isUntouched)
        round.tap(puzzle.steps(0)[0])
        assertTrue(round.isUntouched, "a tap before start does nothing")
        assertTrue(round.start())
        assertFalse(round.isUntouched)
    }
}
