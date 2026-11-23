package com.mehmtcan.brainscroll.game.zerosones

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.BinGuide
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BinGameTest {
    // A generated board, so the tests do not depend on one made up by hand: its one solution is known.
    private val puzzle = BinGenerator.generate(1, Difficulty.Easy)
    private val n = puzzle.size
    private val solution = BinSolver.uniqueSolution(puzzle)!!

    /** The cells the player has to fill, in order. */
    private val open = (0 until n * n).filter { !puzzle.isGiven(it) }

    /** Taps the cell until it holds [digit] (0 is one tap, 1 is two). */
    private fun put(game: BinGame, cell: Int, digit: Int): BinResult {
        var result = game.tap(cell)
        if (digit == 1) result = game.tap(cell)
        return result
    }

    @Test
    fun aTapGoesFromEmptyToZeroToOneToEmpty() {
        val game = BinGame(puzzle)
        val cell = open[0]
        assertEquals(BinResult.Zero, game.tap(cell))
        assertEquals(0, game.state.cells[cell])
        assertEquals(BinResult.One, game.tap(cell))
        assertEquals(1, game.state.cells[cell])
        assertEquals(BinResult.Cleared, game.tap(cell))
        assertEquals(-1, game.state.cells[cell])
        assertEquals(3, game.state.moves)
    }

    @Test
    fun aGivenCellAndACellOffTheBoardAreIgnored() {
        val game = BinGame(puzzle)
        val given = (0 until n * n).first { puzzle.isGiven(it) }
        assertEquals(BinResult.Ignored, game.tap(given))
        assertEquals(BinResult.Ignored, game.tap(-1))
        assertEquals(BinResult.Ignored, game.tap(n * n))
        assertEquals(0, game.state.moves)
        assertEquals(puzzle.given[given], game.state.cells[given])
    }

    @Test
    fun fillingTheAnswerWinsAndTheGameIsOverThen() {
        val game = BinGame(puzzle)
        open.dropLast(1).forEach { assertTrue(put(game, it, solution[it]) != BinResult.Solved) }
        assertFalse(game.isFinished)
        assertEquals(BinResult.Solved, put(game, open.last(), solution[open.last()]))
        assertTrue(game.isFinished)
        assertEquals(BinStatus.Won, game.state.status)
        assertEquals(BinResult.Ignored, game.tap(open[0]))
        assertFalse(game.restart())
        assertEquals(solution.toList(), game.state.cells)
    }

    @Test
    fun aBoardThatIsFullButWrongIsNotWon() {
        val game = BinGame(puzzle)
        // Every cell gets the other digit than the answer's: the board is full, and it cannot follow the rules.
        open.forEach { put(game, it, 1 - solution[it]) }
        assertEquals(n * n, game.state.filled)
        assertFalse(game.isFinished)
        assertTrue(game.state.conflicts.isNotEmpty())
    }

    @Test
    fun aCellThatBreaksARuleIsListedAsBroken() {
        val game = BinGame(puzzle)
        assertTrue(game.state.conflicts.isEmpty(), "a board starts without a broken rule")
        // Three digits side by side: find three open cells next to each other in a row, if the board has them.
        val row = (0 until n).firstOrNull { r -> (0..n - 3).any { c -> (0..2).all { !puzzle.isGiven(r * n + c + it) } } } ?: return
        val c = (0..n - 3).first { c -> (0..2).all { !puzzle.isGiven(row * n + c + it) } }
        for (k in 0..2) game.tap(row * n + c + k)
        assertEquals(setOf(row * n + c, row * n + c + 1, row * n + c + 2), game.state.conflicts.intersect(setOf(row * n + c, row * n + c + 1, row * n + c + 2)))
    }

    @Test
    fun restartEmptiesEveryDigitButNotTheGivenOnesOrTheMoves() {
        val game = BinGame(puzzle)
        game.tap(open[0])
        game.tap(open[1])
        assertTrue(game.restart())
        assertEquals(puzzle.given, game.state.cells)
        assertEquals(2, game.state.moves)
        assertFalse(game.restart(), "nothing to take off")
    }

    @Test
    fun aSavedGameThatCannotBeIsRefused() {
        val game = BinGame(puzzle)
        val empty = puzzle.given
        fun withDigit(cell: Int, digit: Int) = empty.mapIndexed { i, v -> if (i == cell) digit else v }
        assertFalse(game.restore(empty.drop(1), 0), "the wrong number of cells")
        val given = (0 until n * n).first { puzzle.isGiven(it) }
        assertFalse(game.restore(withDigit(given, 1 - puzzle.given[given]), 1), "a given cell that changed")
        assertFalse(game.restore(withDigit(open[0], 0), 0), "fewer moves than digits")
        assertFalse(game.restore(withDigit(open[0], 5), 5), "a digit that is not 0 or 1")
        assertFalse(game.restore(solution.toList(), 99), "a solved board would have been won")
        assertEquals(puzzle.given, game.state.cells)
        assertTrue(game.restore(withDigit(open[0], 0), 3))
        assertEquals(3, game.state.moves)
        assertEquals(0, game.state.cells[open[0]])
    }

    // --- the guide ---

    @Test
    fun theGuideSaysWhichDigitToPutWhere() {
        val game = BinGame(puzzle)
        val step = BinGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.BinFill, step.reason)
        val cell = step.cells.single()
        assertTrue(!puzzle.isGiven(cell))
        assertEquals(solution[cell], step.numbers[0])
    }

    @Test
    fun theGuideSaysToPutRightADigitThatIsNotTheAnswers() {
        val game = BinGame(puzzle)
        val cell = open[0]
        put(game, cell, 1 - solution[cell])
        val step = BinGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.BinClear, step.reason)
        assertEquals(listOf(cell), step.cells)
    }

    @Test
    fun followingTheGuideSolvesEveryGeneratedBoard() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 10) {
                val p = BinGenerator.generate(seed, difficulty)
                val answer = BinSolver.uniqueSolution(p)!!
                val game = BinGame(p)
                var steps = 0
                while (!game.isFinished) {
                    val step = assertNotNull(BinGuide.step(p, game.state), "$difficulty seed $seed")
                    assertEquals(GuideReason.BinFill, step.reason, "a game with only right digits never needs one put right")
                    val cell = step.cells.single()
                    assertEquals(answer[cell], step.numbers[0], "$difficulty seed $seed")
                    put(game, cell, step.numbers[0])
                    steps++
                    assertTrue(steps <= p.size * p.size - p.givenCount)
                }
                assertNull(BinGuide.step(p, game.state))
            }
        }
    }

    // --- the round ---

    private fun round(clock: () -> Long = { 0L }) = BinRound(0, "r1", puzzle, Language.EN, clock)

    @Test
    fun theRoundSavesAndRestoresItsDigitsAndTime() {
        var now = 0L
        val round = round { now }
        round.onShown()
        round.tap(open[0])
        round.tap(open[1])
        round.tap(open[1])
        now = 4_000
        val stored = round.toStored()
        assertEquals(GameKind.ZerosOnes, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertTrue(stored.guesses.joinToString("").length <= 256)
        assertEquals("n3", stored.guesses[0])

        val back = BinRound.restore(0, stored, { now })!!
        val cells = back.snapshot().state.cells
        assertEquals(0, cells[open[0]])
        assertEquals(1, cells[open[1]])
        assertEquals(3, back.snapshot().state.moves)
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
        open.forEach { cell ->
            round.tap(cell)
            if (solution[cell] == 1) round.tap(cell)
        }
        now = 20_000
        assertTrue(round.isFinished)
        assertEquals(2_000L, round.snapshot().durationMs, "the clock stopped at the last cell")
        val finished = round.toFinished(99L)
        assertEquals(1, finished.guesses.size)
    }

    @Test
    fun aTapThatDoesNothingDoesNotMakeTheRoundTouched() {
        val round = round()
        assertTrue(round.isUntouched)
        round.tap(-1)
        assertTrue(round.isUntouched)
        round.tap(open[0])
        assertFalse(round.isUntouched)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsRefused() {
        val stored = round().toStored()
        assertNull(BinRound.restore(0, stored.copy(answer = "!!!"), { 0L }))
        assertNull(BinRound.restore(0, stored.copy(guesses = listOf("x")), { 0L }))
        assertNull(BinRound.restore(0, stored.copy(guesses = listOf("n1", "m9")), { 0L }))
        assertNull(BinRound.restore(0, stored.copy(guesses = listOf("n1", "m11")), { 0L }), "a digit for every cell is needed")
        assertNotNull(BinRound.restore(0, stored, { 0L }))
    }
}
