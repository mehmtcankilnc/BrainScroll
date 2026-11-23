package com.mehmtcan.brainscroll.game.oddone

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.GameLevels
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OddGameTest {

    @Test
    fun everyQuestionHasFourNumbersThatFollowOneRuleAndOneThatDoesNot() {
        for (level in 1..3) for (seed in 0 until 40) {
            val puzzle = OddPuzzle(level, 12, seed)
            for (round in 0 until puzzle.rounds) {
                val q = puzzle.question(round)
                assertEquals(5, q.numbers.size)
                assertEquals(5, q.numbers.toSet().size, "five different numbers: $q")
                assertTrue(q.rule.level <= level, "the rule belongs to the level: $q")
                assertTrue(q.numbers.indices.all { (it == q.oddIndex) != q.rule.holds(q.numbers[it]) }, "only the odd one breaks the rule: $q")
                assertTrue(q.numbers.all { it in 1..intArrayOf(30, 60, 99)[level - 1] })
            }
        }
    }

    @Test
    fun noOtherRuleSinglesOutADifferentNumber() {
        for (level in 1..3) for (seed in 0 until 60) {
            val puzzle = OddPuzzle(level, 12, seed)
            for (round in 0 until puzzle.rounds) {
                val q = puzzle.question(round)
                for (other in OddRule.entries) {
                    val breaking = q.numbers.filter { !other.holds(it) }
                    assertFalse(breaking.size == 1 && breaking[0] != q.odd, "$other points at ${breaking.firstOrNull()} in $q")
                }
            }
        }
    }

    @Test
    fun theSameRunGivesTheSameQuestionsAndOtherSeedsGiveOthers() {
        val a = OddPuzzle(2, 10, 7)
        assertEquals((0 until 10).map { a.question(it) }, (0 until 10).map { OddPuzzle(2, 10, 7).question(it) })
        val firsts = (0 until 30).map { OddPuzzle(2, 10, it).question(0) }.toSet()
        assertTrue(firsts.size >= 25, "${firsts.size} different questions in 30 seeds")
    }

    @Test
    fun theLevelsAddRulesAndBiggerNumbers() {
        val easy = (0 until 80).map { OddPuzzle(1, 8, it).question(0).rule }.toSet()
        assertTrue(easy.all { it.level == 1 })
        val hard = (0 until 200).map { OddPuzzle(3, 12, it).question(0).rule }.toSet()
        assertTrue(hard.any { it.level == 3 } && hard.any { it.level == 1 })
        assertEquals(setOf(8, 10, 12), Difficulty.entries.map { GameLevels.oddOne(it).rounds }.toSet())
    }

    @Test
    fun aRunIsSavedWithFewCharactersAndReadBack() {
        for (level in 1..3) {
            val puzzle = OddPuzzle(level, 12, Int.MAX_VALUE)
            assertTrue(puzzle.encode().length <= 32)
            assertEquals(puzzle, OddPuzzle.decode(puzzle.encode()))
        }
        assertNull(OddPuzzle.decode(""))
        assertNull(OddPuzzle.decode("9zz"))
        assertNull(OddPuzzle.decode("2!!"))
    }

    private fun game(rounds: Int = 3) = OddGame(OddPuzzle(2, rounds, 5))

    @Test
    fun aRightPickBringsTheNextQuestionAndTheLastOneWins() {
        val g = game(3)
        assertEquals(OddResult.Correct, g.pick(g.state.question.oddIndex))
        assertEquals(1, g.state.cleared)
        assertEquals(OddResult.Correct, g.pick(g.state.question.oddIndex))
        assertEquals(OddResult.Perfect, g.pick(g.state.question.oddIndex))
        assertEquals(OddStatus.Won, g.state.status)
        assertEquals(OddResult.Ignored, g.pick(0), "a won run is closed")
    }

    @Test
    fun aWrongPickEndsTheRunWithTheRoundsClearedSoFar() {
        val g = game(3)
        g.pick(g.state.question.oddIndex)
        val q = g.state.question
        val wrong = (0 until 5).first { it != q.oddIndex }
        assertEquals(OddResult.Wrong, g.pick(wrong))
        assertEquals(OddStatus.Lost, g.state.status)
        assertEquals(1, g.state.cleared)
        assertEquals(q, g.state.question, "the run ended on this question")
        assertEquals(OddReveal(q, wrong, false), g.state.last)
    }

    @Test
    fun aPickThatIsNotOnTheBoardIsIgnored() {
        val g = game()
        assertEquals(OddResult.Ignored, g.pick(-1))
        assertEquals(OddResult.Ignored, g.pick(5))
        assertEquals(0, g.state.cleared)
    }

    @Test
    fun restoreRejectsWhatCannotBeARun() {
        val g = game(3)
        assertFalse(g.restore(-1))
        assertFalse(g.restore(3), "that would have been won")
        assertTrue(g.restore(2))
        assertEquals(2, g.state.cleared)
        assertNull(g.state.last)
    }

    private fun round(puzzle: OddPuzzle, clock: () -> Long = { 0L }) = OddRound(0, "o1", puzzle, Language.EN, clock)

    @Test
    fun aRoundIsSavedAndRebuiltAsTheyLeftIt() {
        var time = 1_000L
        val puzzle = OddPuzzle(2, 6, 11)
        val r = round(puzzle) { time }
        r.onShown()
        r.pick(r.snapshot().state.question.oddIndex)
        r.pick(r.snapshot().state.question.oddIndex)
        time += 9_000
        r.onHidden()
        val stored = r.toStored()
        assertEquals(GameKind.OddOne, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        val back = assertNotNull(OddRound.restore(4, stored) { time })
        assertEquals(4, back.index)
        assertEquals(2, back.snapshot().state.cleared)
        assertEquals(r.snapshot().state.question, back.snapshot().state.question)
        assertEquals(9_000, back.snapshot().durationMs)
        assertFalse(back.isUntouched)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round(OddPuzzle(1, 8, 3)).also { it.pick(0) }.toStored()
        assertNull(OddRound.restore(0, stored.copy(answer = "")) { 0 })
        assertNull(OddRound.restore(0, stored.copy(guesses = listOf("x1"))) { 0 })
        assertNull(OddRound.restore(0, stored.copy(guesses = listOf("r9"))) { 0 }, "more rounds than the run has")
        assertNull(OddRound.restore(0, stored.copy(guesses = listOf("r-1"))) { 0 })
    }

    @Test
    fun theTimeIsThePageTimeAndStopsWhenTheRunIsOver() {
        var time = 0L
        val puzzle = OddPuzzle(1, 3, 21)
        val r = round(puzzle) { time }
        r.onShown()
        time += 20_000
        repeat(3) { time += 2_000; r.pick(r.snapshot().state.question.oddIndex) }
        assertTrue(r.isFinished)
        time += 600_000
        assertEquals(26_000L, r.snapshot().durationMs, "looking at the question counts, the clock stops at the end")
        val finished = r.toFinished(9)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.OddOne, finished.game)
        assertEquals(3, finished.moveCount)
    }

    @Test
    fun aLostRunIsALossWithItsRoundsAsTheResult() {
        val r = round(OddPuzzle(1, 8, 4))
        r.pick(r.snapshot().state.question.oddIndex)
        r.pick((0 until 5).first { it != r.snapshot().state.question.oddIndex })
        assertTrue(r.isFinished)
        val finished = r.toFinished(1)
        assertEquals(Outcome.LOST, finished.outcome)
        assertEquals(1, finished.moveCount)
    }
}
