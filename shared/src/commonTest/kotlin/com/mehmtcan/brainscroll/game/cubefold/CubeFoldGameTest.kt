package com.mehmtcan.brainscroll.game.cubefold

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.CubeGuide
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CubeFoldGameTest {
    private val puzzle = CubeFoldPuzzle(4, 5, 4242)

    /** The cross: top (0,1), left (1,0), middle (1,1), right (1,2), bottom (2,1), end (3,1), with the symbols 1, 3, 0, 2, 4, 5. */
    private val cross = listOf(
        NetCell(0, 1, 1), NetCell(1, 0, 3), NetCell(1, 1, 0), NetCell(1, 2, 2), NetCell(2, 1, 4), NetCell(3, 1, 5),
    )

    private fun canon(vararg view: Int) = CubeFolding.canonical(view.toList())

    @Test
    fun aCrossFoldsToACubeAndAStripOfSixDoesNot() {
        val faces = assertNotNull(CubeFolding.fold(cross.map { it.row to it.column }))
        assertEquals(6, faces.toSet().size)
        assertNull(CubeFolding.fold((0 until 6).map { 0 to it }), "six in a row would wrap round and meet itself")
        assertNull(CubeFolding.fold(listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1, 2 to 0, 2 to 1)), "a 2 by 3 block has faces twice")
        assertNull(CubeFolding.fold(listOf(0 to 0, 0 to 1, 0 to 3, 1 to 0, 1 to 1, 1 to 2)), "a net that is not in one piece")
    }

    @Test
    fun theCornersOfTheCrossAreWhatAPersonSeesWhenTheCubeIsFoldedAwayFromThem() {
        // Looking at the middle square as the front, the top square folds to the top and the right square to the right.
        val (corners, partner) = CubeFolding.corners(cross)
        val real = corners.map(CubeFolding::canonical).toSet()
        assertEquals(8, real.size)
        assertTrue(canon(1, 0, 2) in real, "top, front, right in the order top, left, right")
        assertFalse(canon(1, 2, 0) in real, "the same three faces the wrong way round are not a corner")
        // Opposite faces of the cross: top and bottom, front and end, left and right.
        assertEquals(4, partner[1]); assertEquals(5, partner[0]); assertEquals(3, partner[2]); assertEquals(2, partner[3]); assertEquals(1, partner[4]); assertEquals(0, partner[5])
        // No corner has two faces that are opposite.
        for (view in corners) assertTrue(view.none { partner[it] in view })
    }

    @Test
    fun everyQuestionHasExactlyOneRightCubeAndNoWrongCubeIsARealCorner() {
        for (options in 3..4) for (seed in 0 until 600) {
            val random = Random(seed)
            val q = CubeFolding.question(random, options)
            assertEquals(options, q.options.size)
            val (corners, partner) = CubeFolding.corners(q.net)
            val real = corners.map(CubeFolding::canonical).toSet()
            assertEquals(1, q.options.count { CubeFolding.canonical(it) in real }, "seed $seed: one real corner among the options")
            assertTrue(CubeFolding.canonical(q.options[q.correct]) in real)
            assertEquals(options, q.options.map(CubeFolding::canonical).toSet().size, "seed $seed: no two options are the same cube")
            assertEquals(partner, q.partner)
            q.options.forEachIndexed { i, view ->
                assertEquals(3, view.toSet().size)
                assertTrue(view.all { it in 0..5 })
                val hasOpposite = view.any { partner[it] in view }
                when (q.whys[i]) {
                    CubeWhy.Right -> assertEquals(q.correct, i)
                    CubeWhy.Opposite -> assertTrue(hasOpposite, "an opposite option shows opposite faces")
                    CubeWhy.Mirror -> assertTrue(!hasOpposite && view.toSet() in corners.map { it.toSet() }, "a mirror option is the faces of a corner")
                }
            }
            assertTrue(q.whys.contains(CubeWhy.Mirror) && q.whys.contains(CubeWhy.Opposite), "seed $seed: both kinds of wrong cube")
            // The net is six different squares in one piece, with no 2 by 2 block.
            assertEquals(6, q.net.map { it.symbol }.toSet().size)
            assertTrue(q.net.maxOf { it.row } < 5 && q.net.maxOf { it.column } < 5)
        }
    }

    @Test
    fun theRightCubeIsNotAlwaysInTheSamePlace() {
        for (options in 3..4) {
            val places = (0 until 400).map { CubeFolding.question(Random(it), options).correct }.toSet()
            assertEquals((0 until options).toSet(), places)
        }
    }

    @Test
    fun theQuestionsOnlyDependOnTheSeedAndTheRound() {
        for (round in 0 until puzzle.rounds) {
            val a = puzzle.question(round)
            val b = CubeFoldPuzzle(4, 5, 4242).question(round)
            assertEquals(a.net, b.net)
            assertEquals(a.options, b.options)
            assertEquals(a.correct, b.correct)
        }
        val nets = (0 until puzzle.rounds).map { puzzle.question(it).net }.toSet()
        assertTrue(nets.size > 1, "the questions of a run differ")
    }

    @Test
    fun theRunIsSavedAndReadBackInAFewCharacters() {
        for (difficulty in Difficulty.entries) for (seed in 0 until 50) {
            val p = CubeFoldGenerator.generate(seed, difficulty)
            val text = p.encode()
            assertTrue(text.length <= 14, text)
            assertEquals(p, CubeFoldPuzzle.decode(text))
        }
        assertNull(CubeFoldPuzzle.decode(""))
        assertNull(CubeFoldPuzzle.decode("zz"))
        assertNull(CubeFoldPuzzle.decode("9 x x"))
    }

    @Test
    fun aRunThatCannotBeIsRefused() {
        fun refused(block: () -> CubeFoldPuzzle) = runCatching(block).isFailure
        assertTrue(refused { CubeFoldPuzzle(2, 5, 0) })
        assertTrue(refused { CubeFoldPuzzle(5, 5, 0) })
        assertTrue(refused { CubeFoldPuzzle(4, 0, 0) })
        assertTrue(refused { CubeFoldPuzzle(4, 16, 0) })
        assertTrue(refused { CubeFoldPuzzle(4, 5, -1) })
    }

    @Test
    fun theLevelsMakeTheRunsTheyAreMeantFor() {
        val easy = CubeFoldGenerator.generate(1, Difficulty.Easy)
        val medium = CubeFoldGenerator.generate(1, Difficulty.Medium)
        val hard = CubeFoldGenerator.generate(1, Difficulty.Hard)
        assertEquals(listOf(3, 4, 4), listOf(easy.options, medium.options, hard.options))
        assertEquals(listOf(6, 8, 10), listOf(easy.rounds, medium.rounds, hard.rounds))
        assertEquals(hard, CubeFoldGenerator.generate(1, Difficulty.Hard))
    }

    private fun rightOf(game: CubeFoldGame) = game.state.question.correct

    private fun wrongOf(game: CubeFoldGame) = game.state.question.whys.indexOfFirst { it != CubeWhy.Right }

    @Test
    fun theRightCubeBringsTheNextQuestionAndTheLastOneWins() {
        val game = CubeFoldGame(puzzle)
        for (round in 0 until puzzle.rounds - 1) {
            assertEquals(CubeResult.Correct, game.pick(rightOf(game)))
            assertEquals(round + 1, game.state.cleared)
        }
        assertEquals(CubeResult.Perfect, game.pick(rightOf(game)))
        assertEquals(CubeStatus.Won, game.state.status)
        assertEquals(puzzle.rounds, game.state.cleared)
        assertEquals(CubeResult.Ignored, game.pick(0))
    }

    @Test
    fun aWrongCubeEndsTheRunAndKeepsTheReveal() {
        val game = CubeFoldGame(puzzle)
        game.pick(rightOf(game))
        val question = game.state.question
        val wrong = wrongOf(game)
        assertEquals(CubeResult.Wrong, game.pick(wrong))
        assertTrue(game.isFinished)
        assertEquals(CubeStatus.Lost, game.state.status)
        assertEquals(1, game.state.cleared)
        assertEquals(wrong, game.state.last!!.picked)
        assertFalse(game.state.last!!.correct)
        assertEquals(question.options, game.state.last!!.question.options)
        assertEquals(CubeResult.Ignored, game.pick(question.correct))
    }

    @Test
    fun anOptionThatIsNotThereIsIgnored() {
        val game = CubeFoldGame(puzzle)
        assertEquals(CubeResult.Ignored, game.pick(-1))
        assertEquals(CubeResult.Ignored, game.pick(4))
        assertFalse(game.isFinished)
    }

    @Test
    fun aSavedRunCanOnlyBeOneThatWasNotWon() {
        val game = CubeFoldGame(puzzle)
        assertFalse(game.restore(-1))
        assertFalse(game.restore(puzzle.rounds))
        assertTrue(game.restore(3))
        assertEquals(3, game.state.cleared)
        assertEquals(puzzle.question(3).options, game.state.question.options)
    }

    // --- the guide ---

    @Test
    fun theGuideRulesOutAWrongCubeWithItsReasonAndTheSquaresThatShowIt() {
        for (round in 0 until puzzle.rounds) {
            val game = CubeFoldGame(puzzle).also { it.restore(round) }
            val question = game.state.question
            val step = assertNotNull(CubeGuide.step(game.state))
            val option = step.cells.single()
            assertTrue(question.whys[option] != CubeWhy.Right, "the guide never points at the right cube")
            assertEquals(if (question.whys[option] == CubeWhy.Opposite) GuideReason.CubeOpposite else GuideReason.CubeMirror, step.reason)
            val expected = if (question.whys[option] == CubeWhy.Opposite) 2 else 3
            assertEquals(expected, step.evidence.size)
            assertTrue(step.evidence.all { it - CubeGuide.NET_BASE in question.net.indices })
        }
        val game = CubeFoldGame(puzzle)
        game.pick(wrongOf(game))
        assertNull(CubeGuide.step(game.state))
    }

    // --- the round ---

    private fun round(clock: () -> Long = { 0L }) = CubeFoldRound(0, "q1", puzzle, Language.EN, clock)

    @Test
    fun theRoundSavesAndRestoresItsQuestionsAndTime() {
        var now = 0L
        val round = round { now }
        round.onShown()
        round.pick(round.snapshot().state.question.correct)
        now = 5_000
        val stored = round.toStored()
        assertEquals(listOf("r1"), stored.guesses)
        assertEquals(puzzle.encode(), stored.answer)
        assertEquals(GameKind.CubeFold, stored.game)
        assertEquals("5000", stored.input)
        val back = CubeFoldRound.restore(0, stored, { now })!!
        assertEquals(1, back.snapshot().state.cleared)
        assertFalse(back.isUntouched)
    }

    @Test
    fun aRunLostOrWonIsAResultWithItsQuestions() {
        val lost = round()
        lost.pick(lost.snapshot().state.question.correct)
        val wrong = lost.snapshot().state.question.whys.indexOfFirst { it != CubeWhy.Right }
        assertEquals(CubeResult.Wrong, lost.pick(wrong))
        val lostRow = lost.toFinished(1L)
        assertEquals(Outcome.LOST, lostRow.outcome)
        assertEquals(Mode.ENDLESS, lostRow.mode)
        assertEquals(1, lostRow.moveCount)

        val won = round()
        repeat(puzzle.rounds) { won.pick(won.snapshot().state.question.correct) }
        val wonRow = won.toFinished(1L)
        assertEquals(Outcome.WON, wonRow.outcome)
        assertEquals(puzzle.rounds, wonRow.moveCount)
        assertTrue(won.snapshot().isWon)
        assertFalse(lost.snapshot().isWon)
    }

    @Test
    fun aSavedGameThatCannotBeReadStartsNew() {
        val stored = round().toStored()
        assertNull(CubeFoldRound.restore(0, stored.copy(answer = "!!"), { 0L }))
        assertNull(CubeFoldRound.restore(0, stored.copy(guesses = listOf("x1")), { 0L }))
        assertNull(CubeFoldRound.restore(0, stored.copy(guesses = listOf("r${puzzle.rounds}")), { 0L }), "a won run would not have been saved")
    }

    @Test
    fun theTimeIsThePageTimeAndStopsWhenTheRunIsOver() {
        var now = 0L
        val round = round { now }
        round.onShown()
        now = 7_000
        assertEquals(7_000L, round.snapshot().durationMs)
        round.onHidden()
        now = 99_000
        assertEquals(7_000L, round.snapshot().durationMs, "away from the page the clock stands")
        round.onShown()
        now = 100_000
        round.pick(round.snapshot().state.question.whys.indexOfFirst { it != CubeWhy.Right })
        now = 200_000
        assertEquals(8_000L, round.toFinished(1L).durationMs)
    }

    @Test
    fun aRoundThatWasNeverTouchedIsUntouched() {
        val round = round()
        assertTrue(round.isUntouched)
        round.pick(round.snapshot().state.question.correct)
        assertFalse(round.isUntouched)
    }
}
