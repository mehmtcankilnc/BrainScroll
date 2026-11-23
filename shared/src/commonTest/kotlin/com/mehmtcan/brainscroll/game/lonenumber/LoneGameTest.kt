package com.mehmtcan.brainscroll.game.lonenumber

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.guide.LoneGuide
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoneGameTest {
    // A generated board, so the tests do not depend on one made up by hand: its one solution is known.
    private val puzzle = LoneGenerator.generate(1, Difficulty.Easy)
    private val n = puzzle.size
    private val solution = LoneSolver.uniqueSolution(puzzle)!!.sorted()

    /** A cell that the answer does not shade and that touches no cell of the answer, so shading it is allowed but wrong. */
    private fun allowedButWrong(): Int? = (0 until n * n).firstOrNull { cell ->
        cell !in solution && puzzle.neighbours(cell).none { it in solution }
    }

    @Test
    fun aTapGoesFromEmptyToShadedToCircledToEmpty() {
        val game = LoneGame(puzzle)
        val cell = solution[0]
        assertEquals(LoneResult.Shaded, game.tap(cell))
        assertEquals(LoneMark.Shaded, game.state.marks[cell])
        assertEquals(LoneResult.Circled, game.tap(cell))
        assertEquals(LoneMark.Circled, game.state.marks[cell])
        assertEquals(LoneResult.Cleared, game.tap(cell))
        assertEquals(LoneMark.Empty, game.state.marks[cell])
        assertEquals(3, game.state.moves)
    }

    @Test
    fun aCellNextToAShadedOneCannotBeShaded() {
        val game = LoneGame(puzzle)
        val cell = solution[0]
        game.tap(cell)
        val next = puzzle.neighbours(cell).first()
        assertEquals(LoneResult.Rejected, game.tap(next))
        assertEquals(LoneMark.Empty, game.state.marks[next])
        assertEquals(1, game.state.moves, "a refused tap is not a move")
        // Once the first one is circled, its neighbour may be shaded.
        game.tap(cell)
        assertTrue(game.tap(next) != LoneResult.Rejected)
    }

    @Test
    fun aTapOffTheBoardIsIgnored() {
        val game = LoneGame(puzzle)
        assertEquals(LoneResult.Ignored, game.tap(-1))
        assertEquals(LoneResult.Ignored, game.tap(n * n))
    }

    @Test
    fun shadingTheAnswerWinsAndTheGameIsOverThen() {
        val game = LoneGame(puzzle)
        solution.dropLast(1).forEach { assertEquals(LoneResult.Shaded, game.tap(it)) }
        assertFalse(game.isFinished)
        assertEquals(LoneResult.Solved, game.tap(solution.last()))
        assertTrue(game.isFinished)
        assertEquals(LoneStatus.Won, game.state.status)
        assertEquals(LoneResult.Ignored, game.tap(0))
        assertFalse(game.restart())
        assertEquals(solution.toSet(), game.state.shaded)
    }

    @Test
    fun turningAWrongShadedCellIntoACircleIsTheTapThatWins() {
        // A cell that is not part of the answer and does not touch one that is: it can be shaded, and then the board is wrong.
        val extra = (0 until n * n).firstOrNull { it !in solution && puzzle.neighbours(it).none { nb -> nb in solution } } ?: return
        val game = LoneGame(puzzle)
        game.tap(extra)
        solution.forEach { game.tap(it) }
        assertFalse(game.isFinished, "an extra shaded cell spoils the board")
        assertEquals(LoneResult.Solved, game.tap(extra), "the circle makes the board right")
        assertTrue(game.isFinished)
    }

    @Test
    fun circlesDoNotChangeTheAnswer() {
        val game = LoneGame(puzzle)
        val free = (0 until n * n).first { it !in solution }
        game.tap(free)
        game.tap(free)
        assertEquals(LoneMark.Circled, game.state.marks[free])
        solution.forEach { game.tap(it) }
        assertTrue(game.isFinished, "a circle on a cell that stays is fine")
    }

    @Test
    fun theRepeatedNumbersAreTheOnesStillTwiceAmongTheCellsLeft() {
        val game = LoneGame(puzzle)
        val before = game.state.repeated
        assertTrue(before.isNotEmpty())
        solution.forEach { game.tap(it) }
        assertTrue(game.state.repeated.isEmpty())
    }

    @Test
    fun restartTakesEveryMarkOffButNotTheMoves() {
        val game = LoneGame(puzzle)
        game.tap(solution[0])
        assertTrue(game.restart())
        assertTrue(game.state.marks.all { it == LoneMark.Empty })
        assertEquals(1, game.state.moves)
        assertFalse(game.restart(), "nothing to take off")
    }

    @Test
    fun aSavedGameThatCannotBeIsRefused() {
        val game = LoneGame(puzzle)
        val empty = List(n * n) { LoneMark.Empty }
        fun marked(vararg cells: Int) = empty.mapIndexed { i, m -> if (i in cells) LoneMark.Shaded else m }
        assertFalse(game.restore(empty.drop(1), 0), "the wrong number of cells")
        val a = 0
        val b = puzzle.neighbours(a).first()
        assertFalse(game.restore(marked(a, b), 2), "two shaded cells that touch")
        assertFalse(game.restore(marked(solution[0]), 0), "fewer moves than marks")
        assertFalse(game.restore(marked(*solution.toIntArray()), 99), "a solved board would have been won")
        assertTrue(game.state.marks.all { it == LoneMark.Empty })
        assertTrue(game.restore(marked(solution[0]), 3))
        assertEquals(3, game.state.moves)
        assertEquals(setOf(solution[0]), game.state.shaded)
    }

    // --- the guide ---

    @Test
    fun theGuideSaysWhichCellToShade() {
        val game = LoneGame(puzzle)
        val step = LoneGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.LoneShade, step.reason)
        assertTrue(step.cells.single() in solution)
    }

    @Test
    fun theGuideSaysToPutRightAShadedCellThatIsNotPartOfTheAnswer() {
        val wrong = allowedButWrong() ?: return
        val game = LoneGame(puzzle)
        game.tap(wrong)
        val step = LoneGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.LoneClear, step.reason)
        assertEquals(listOf(wrong), step.cells)
    }

    @Test
    fun theGuideSaysToPutRightACircleOnACellTheAnswerShades() {
        val game = LoneGame(puzzle)
        game.tap(solution[0])
        game.tap(solution[0])
        val step = LoneGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.LoneClear, step.reason)
        assertEquals(listOf(solution[0]), step.cells)
    }

    @Test
    fun followingTheGuideSolvesEveryGeneratedBoard() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 15) {
                val p = LoneGenerator.generate(seed, difficulty)
                val answer = LoneSolver.uniqueSolution(p)!!
                val game = LoneGame(p)
                var steps = 0
                while (!game.isFinished) {
                    val step = assertNotNull(LoneGuide.step(p, game.state), "$difficulty seed $seed")
                    assertEquals(GuideReason.LoneShade, step.reason, "a game with only right marks never needs one put right")
                    val result = game.tap(step.cells.single())
                    assertTrue(result == LoneResult.Shaded || result == LoneResult.Solved, "$difficulty seed $seed: $result")
                    steps++
                    assertTrue(steps <= answer.size)
                }
                assertNull(LoneGuide.step(p, game.state))
            }
        }
    }

    // --- the round ---

    private fun round(clock: () -> Long = { 0L }) = LoneRound(0, "r1", puzzle, Language.EN, clock)

    @Test
    fun theRoundSavesAndRestoresItsMarksAndTime() {
        var now = 0L
        val round = round { now }
        round.onShown()
        round.tap(solution[0])
        round.tap(solution[1])
        round.tap(solution[1])
        now = 4_000
        val stored = round.toStored()
        assertEquals(GameKind.LoneNumber, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertTrue(stored.guesses.joinToString("").length <= 256)
        assertEquals("n3", stored.guesses[0])

        val back = LoneRound.restore(0, stored, { now })!!
        val state = back.snapshot().state
        assertEquals(LoneMark.Shaded, state.marks[solution[0]])
        assertEquals(LoneMark.Circled, state.marks[solution[1]])
        assertEquals(3, state.moves)
        assertEquals(4_000L, back.snapshot().durationMs)
        assertFalse(back.isUntouched)
    }

    @Test
    fun theTimeRunsOnlyWhileThePageIsOnShowAndStopsWhenTheBoardIsSolved() {
        var now = 0L
        val round = round { now }
        now = 1_000
        round.onShown()
        now = 3_000
        round.onHidden()
        now = 10_000
        assertEquals(2_000L, round.snapshot().durationMs)
        round.onShown()
        solution.forEach { round.tap(it) }
        now = 20_000
        assertTrue(round.isFinished)
        assertEquals(2_000L, round.snapshot().durationMs, "the clock stopped at the last cell")
        val finished = round.toFinished(99L)
        assertEquals(listOf(solution.size.toString()), finished.guesses)
    }

    @Test
    fun aRefusedTapDoesNotMakeTheRoundTouched() {
        val round = round()
        assertTrue(round.isUntouched)
        round.tap(-1)
        assertTrue(round.isUntouched)
        round.tap(solution[0])
        assertFalse(round.isUntouched)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsRefused() {
        val stored = round().toStored()
        assertNull(LoneRound.restore(0, stored.copy(answer = "!!!"), { 0L }))
        assertNull(LoneRound.restore(0, stored.copy(guesses = listOf("x")), { 0L }))
        assertNull(LoneRound.restore(0, stored.copy(guesses = listOf("n1", "m9")), { 0L }))
        assertNull(LoneRound.restore(0, stored.copy(guesses = listOf("n1", "m11")), { 0L }), "a mark for every cell is needed")
        assertNotNull(LoneRound.restore(0, stored, { 0L }))
    }
}
