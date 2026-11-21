package com.mehmtcan.brainscroll.game.codebreaker

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.CodeGuide
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CodeBreakerTest {
    private val puzzle = CodePuzzle(6, false, listOf(0, 3, 2, 1))

    @Test
    fun feedbackCountsRightPlaceAndWrongPlace() {
        assertEquals(1 to 2, puzzle.feedback(listOf(0, 2, 4, 3)).let { it.exact to it.misplaced })
        assertEquals(4 to 0, puzzle.feedback(listOf(0, 3, 2, 1)).let { it.exact to it.misplaced })
        assertEquals(0 to 4, puzzle.feedback(listOf(3, 0, 1, 2)).let { it.exact to it.misplaced })
        assertEquals(0 to 0, puzzle.feedback(listOf(4, 5, 4, 5)).let { it.exact to it.misplaced })
    }

    @Test
    fun everyPegGetsItsOwnMarkLikeTheLettersOfTheWordPuzzle() {
        val code = CodePuzzle(6, true, listOf(1, 1, 2, 3))
        // Guess 1 2 1 5: the first is right; the 2 is in the code elsewhere; the second 1 is the code's other 1; the 5 is not in it.
        assertEquals(listOf(CodeMark.Exact, CodeMark.Present, CodeMark.Present, CodeMark.Absent), code.feedback(listOf(1, 2, 1, 5)).marks)
        // Three reds against one red: only the one that is in its place scores, the others get nothing.
        val oneRed = CodePuzzle(6, true, listOf(0, 1, 2, 3))
        assertEquals(listOf(CodeMark.Exact, CodeMark.Absent, CodeMark.Absent, CodeMark.Absent), oneRed.feedback(listOf(0, 0, 0, 5)).marks)
        // The red is elsewhere: the first of the three reds takes it, the rest get nothing.
        assertEquals(listOf(CodeMark.Absent, CodeMark.Present, CodeMark.Absent, CodeMark.Absent), oneRed.feedback(listOf(5, 0, 0, 0)).marks.let { it })
        assertEquals(listOf(CodeMark.Exact, CodeMark.Exact, CodeMark.Exact, CodeMark.Exact), puzzle.feedback(puzzle.secret).marks)
    }

    @Test
    fun aColourIsCountedOnceHoweverOftenItIsGuessed() {
        // The code has one red; a guess of three reds scores one red only.
        val repeating = CodePuzzle(6, true, listOf(0, 1, 2, 3))
        assertEquals(1 to 0, repeating.feedback(listOf(0, 0, 0, 5)).let { it.exact to it.misplaced })
        assertEquals(0 to 1, repeating.feedback(listOf(5, 0, 0, 0)).let { it.exact to it.misplaced })
        // A code with two of a colour and a guess with one in the right place and another elsewhere.
        val twice = CodePuzzle(6, true, listOf(1, 1, 2, 3))
        assertEquals(1 to 2, twice.feedback(listOf(1, 2, 1, 5)).let { it.exact to it.misplaced })
    }

    @Test
    fun aBoardWithNoRepeatsRefusesAGuessWithAColourTwice() {
        assertFalse(puzzle.accepts(listOf(0, 0, 1, 2)))
        assertFalse(puzzle.accepts(listOf(0, 1, 2)))
        assertFalse(puzzle.accepts(listOf(0, 1, 2, 6)))
        assertTrue(puzzle.accepts(listOf(5, 4, 3, 2)))
        assertTrue(CodePuzzle(8, true, listOf(1, 1, 1, 1, 1)).accepts(listOf(7, 7, 7, 7, 7)))
    }

    @Test
    fun aBoardThatCannotBeIsRefused() {
        fun refused(block: () -> CodePuzzle) = runCatching(block).isFailure
        assertTrue(refused { CodePuzzle(6, false, listOf(1, 1, 2, 3)) })
        assertTrue(refused { CodePuzzle(4, false, listOf(0, 1, 2, 4)) })
        assertTrue(refused { CodePuzzle(9, true, listOf(0, 1, 2, 3)) })
        assertTrue(refused { CodePuzzle(6, true, listOf(0, 1)) })
    }

    @Test
    fun theBoardIsSavedAndReadBack() {
        assertEquals("6u0321", puzzle.encode())
        assertEquals(puzzle, CodePuzzle.decode(puzzle.encode()))
        val hard = CodePuzzle(8, true, listOf(7, 7, 0, 3, 3))
        assertEquals(hard, CodePuzzle.decode(hard.encode()))
        assertNull(CodePuzzle.decode(""))
        assertNull(CodePuzzle.decode("6x0321"))
        assertNull(CodePuzzle.decode("6u03"))
        assertNull(CodePuzzle.decode("6u0311"), "a colour twice on a board with no repeats")
    }

    @Test
    fun theGameCountsGuessesAndEndsOnTheCode() {
        val game = CodeGame(puzzle)
        assertEquals(CodeResult.Ignored, game.submit(listOf(0, 1)))
        assertEquals(CodeResult.Guessed, game.submit(listOf(5, 4, 3, 2)))
        assertEquals(CodeResult.Guessed, game.submit(listOf(0, 2, 4, 3)))
        assertEquals(2, game.state.tries)
        assertFalse(game.isFinished)
        assertEquals(CodeResult.Solved, game.submit(listOf(0, 3, 2, 1)))
        assertTrue(game.isFinished)
        assertEquals(3, game.state.tries)
        assertEquals(CodeResult.Ignored, game.submit(listOf(5, 4, 3, 2)), "no guesses on a cracked code")
    }

    @Test
    fun thereIsNoLimitToTheTries() {
        val game = CodeGame(CodePuzzle(8, true, listOf(7, 7, 7, 7, 7)))
        repeat(200) { assertEquals(CodeResult.Guessed, game.submit(listOf(0, 1, 2, 3, 4))) }
        assertEquals(200, game.state.tries)
        assertEquals(CodeResult.Solved, game.submit(listOf(7, 7, 7, 7, 7)))
    }

    @Test
    fun theGeneratorMakesBoardsOfTheLevelAndIsRepeatable() {
        for (seed in 0 until 300) {
            val easy = CodeGenerator.generate(seed, Difficulty.Easy)
            assertEquals(4, easy.length)
            assertEquals(5, easy.colors)
            assertFalse(easy.repeats)
            val hard = CodeGenerator.generate(seed, Difficulty.Hard)
            assertEquals(5, hard.length)
            assertEquals(8, hard.colors)
            assertTrue(hard.repeats)
            assertEquals(hard, CodeGenerator.generate(seed, Difficulty.Hard))
        }
        val repeated = (0 until 300).count { CodeGenerator.generate(it, Difficulty.Hard).secret.toSet().size < 5 }
        assertTrue(repeated > 50, "the hard boards do repeat colours: $repeated")
    }

    @Test
    fun theSolverKeepsOnlyTheCodesThatFitTheClues() {
        val game = CodeGame(puzzle)
        game.submit(listOf(0, 2, 4, 3))
        game.submit(listOf(5, 4, 3, 2))
        val fits = CodeSolver.candidates(puzzle, game.state.guesses)
        assertTrue(puzzle.secret in fits, "the secret always fits")
        assertTrue(fits.size < 360, "two guesses cut the 360 codes down: ${fits.size}")
        assertTrue(fits.all { code -> game.state.guesses.all { CodePuzzle.feedback(code, it.colors) == it.feedback } })
    }

    @Test
    fun followingTheSolversGuessCracksEveryBoardInAFewTries() {
        for (seed in 0 until 40) {
            for (difficulty in Difficulty.entries) {
                val p = CodeGenerator.generate(seed, difficulty)
                val game = CodeGame(p)
                var tries = 0
                while (!game.isFinished) {
                    val next = CodeSolver.nextGuess(p, game.state.guesses)
                    assertNotNull(next, "$difficulty seed $seed")
                    game.submit(next)
                    tries++
                    assertTrue(tries <= 15, "$difficulty seed $seed took $tries tries")
                }
            }
        }
    }

    @Test
    fun theGuideSuggestsACodeThatFits() {
        val game = CodeGame(puzzle)
        game.submit(listOf(0, 2, 4, 3))
        val step = CodeGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.CodeNext, step.reason)
        assertEquals(puzzle.length, step.numbers.size)
        assertTrue(puzzle.accepts(step.numbers))
        assertEquals(puzzle.feedback(listOf(0, 2, 4, 3)), CodePuzzle.feedback(step.numbers, listOf(0, 2, 4, 3)), "it fits the clue so far")
        game.submit(puzzle.secret)
        assertNull(CodeGuide.step(puzzle, game.state), "nothing to say once cracked")
    }

    // --- the round ---

    private fun round(clock: () -> Long = { 0L }) = CodeRound(0, "r1", puzzle, Language.EN, clock)

    @Test
    fun theRoundSavesAndRestoresItsGuessesAndTime() {
        var now = 0L
        val round = round { now }
        round.onShown()
        round.submit(listOf(5, 4, 3, 2))
        round.submit(listOf(0, 2, 4, 3))
        now = 4_000
        val stored = round.toStored()
        assertEquals("6u0321", stored.answer)
        assertEquals(listOf("n2", "5432", "0243"), stored.guesses)
        assertEquals(GameKind.CodeBreaker, stored.game)

        val back = CodeRound.restore(0, stored, { now })!!
        val state = back.snapshot().state
        assertEquals(2, state.tries)
        assertEquals(listOf(listOf(5, 4, 3, 2), listOf(0, 2, 4, 3)), state.guesses.map { it.colors })
        assertFalse(back.isUntouched)
    }

    @Test
    fun aVeryLongGameKeepsItsLastGuessesAndAllItsTries() {
        val p = CodePuzzle(8, true, listOf(7, 7, 7, 7, 7))
        val round = CodeRound(0, "r2", p, Language.EN)
        repeat(100) { round.submit(listOf(0, 1, 2, 3, it % 8)) }
        val stored = round.toStored()
        assertTrue(stored.guesses.joinToString(",").length <= 256, "fits the saved text")
        val back = CodeRound.restore(0, stored, { 0L })!!
        assertEquals(100, back.snapshot().state.tries)
        assertEquals(CodeRound.KEPT, back.snapshot().state.guesses.size)
    }

    @Test
    fun aFinishedRoundIsAWinWithItsTries() {
        val round = round()
        round.submit(listOf(5, 4, 3, 2))
        assertEquals(CodeResult.Solved, round.submit(puzzle.secret))
        val finished = round.toFinished(1L)
        assertEquals(listOf("2"), finished.guesses)
        assertTrue(round.isFinished)
    }

    @Test
    fun aSavedGameThatCannotBeReadStartsNew() {
        val stored = round().toStored()
        assertNull(CodeRound.restore(0, stored.copy(answer = "nonsense"), { 0L }))
        assertNull(CodeRound.restore(0, stored.copy(guesses = listOf("n1", "00xx")), { 0L }))
        assertNull(CodeRound.restore(0, stored.copy(guesses = listOf("n1", "0321")), { 0L }), "a saved guess that is the code")
    }

    @Test
    fun theTimeIsTheTimeThePageWasOnShow() {
        var now = 1_000L
        val round = round { now }
        round.onShown()
        now = 31_000L
        round.submit(puzzle.secret)
        assertEquals(30_000L, round.snapshot().durationMs, "a code cracked at once still took the time spent looking")
        now = 99_000L
        assertEquals(30_000L, round.snapshot().durationMs, "the time stops when it is cracked")
    }
}
