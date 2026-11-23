package com.mehmtcan.brainscroll.game.greaterless

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GlGuide
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GlGameTest {
    // A generated board, so the tests do not depend on one made up by hand: its one solution is known.
    private val puzzle = GlGenerator.generate(1, Difficulty.Easy)
    private val n = puzzle.size
    private val answer = GlSolver.uniqueSolution(puzzle)!!
    private val open = (0 until n * n).filter { !puzzle.isGiven(it) }

    @Test
    fun aNumberIsPutInACellAndTakenOffAgain() {
        val game = GlGame(puzzle)
        val cell = open.first()
        for (v in 1..n) {
            assertEquals(GlResult.Placed, game.set(cell, v))
            assertEquals(v, game.state.cells[cell])
        }
        assertEquals(GlResult.Cleared, game.set(cell, 0))
        assertEquals(0, game.state.cells[cell])
        assertEquals(n + 1, game.state.moves)
    }

    @Test
    fun puttingTheNumberThatIsThereOrOneThatDoesNotExistChangesNothing() {
        val game = GlGame(puzzle)
        val cell = open.first()
        assertEquals(GlResult.Ignored, game.set(cell, 0), "the cell is empty already")
        game.set(cell, 2)
        assertEquals(GlResult.Ignored, game.set(cell, 2))
        assertEquals(GlResult.Ignored, game.set(cell, n + 1))
        assertEquals(GlResult.Ignored, game.set(cell, -1))
        assertEquals(2, game.state.cells[cell])
        assertEquals(1, game.state.moves)
    }

    @Test
    fun aGivenCellAndACellOffTheBoardAreIgnored() {
        val game = GlGame(puzzle)
        val given = (0 until n * n).first { puzzle.isGiven(it) }
        assertEquals(GlResult.Ignored, game.set(given, 1))
        assertEquals(GlResult.Ignored, game.set(-1, 1))
        assertEquals(GlResult.Ignored, game.set(n * n, 1))
        assertEquals(0, game.state.moves)
    }

    @Test
    fun puttingTheAnswerWinsAndTheGameIsOverThen() {
        val game = GlGame(puzzle)
        open.dropLast(1).forEach { game.set(it, answer[it]) }
        assertFalse(game.isFinished)
        val last = open.last()
        assertEquals(GlResult.Solved, game.set(last, answer[last]))
        assertTrue(game.isFinished)
        assertEquals(GlStatus.Won, game.state.status)
        assertEquals(GlResult.Ignored, game.set(open.first(), 1))
        assertFalse(game.restart())
        assertTrue(game.state.conflicts.isEmpty())
    }

    @Test
    fun aRepeatedNumberAndAWrongSignAreMarked() {
        val game = GlGame(puzzle)
        // The same number in two cells of a row.
        val row = (0 until n).map { it }.filter { !puzzle.isGiven(it) }
        if (row.size >= 2) {
            game.set(row[0], 1)
            game.set(row[1], 1)
            assertTrue(game.state.conflicts.containsAll(setOf(row[0], row[1])))
        }
        // A sign that is not true marks both cells.
        val (a, b, sign) = GlRules.links(puzzle).first { (a, b, _) -> !puzzle.isGiven(a) && !puzzle.isGiven(b) }
        val fresh = GlGame(puzzle)
        if (sign == GlPuzzle.LESS) {
            fresh.set(a, n)
            fresh.set(b, 1)
        } else {
            fresh.set(a, 1)
            fresh.set(b, n)
        }
        assertTrue(a in fresh.state.conflicts && b in fresh.state.conflicts)
    }

    @Test
    fun takingAWrongNumberOffOrChangingItIsTheTapThatCanWin() {
        // The answer is in, but one cell holds a wrong number: the move that puts the right one in its place is the last move.
        val game = GlGame(puzzle)
        val wrongCell = open.first()
        val wrong = (1..n).first { it != answer[wrongCell] }
        game.set(wrongCell, wrong)
        open.drop(1).forEach { game.set(it, answer[it]) }
        assertFalse(game.isFinished, "a wrong number spoils the board")
        assertEquals(GlResult.Solved, game.set(wrongCell, answer[wrongCell]))
        assertEquals(answer[wrongCell], game.state.cells[wrongCell])
        assertEquals(GlStatus.Won, game.state.status)
    }

    @Test
    fun restartEmptiesEveryNumberThePlayerPutButNotTheMoves() {
        val game = GlGame(puzzle)
        game.set(open.first(), 1)
        assertTrue(game.restart())
        assertTrue(open.all { game.state.cells[it] == 0 })
        assertTrue((0 until n * n).filter { puzzle.isGiven(it) }.all { game.state.cells[it] == puzzle.given[it] })
        assertEquals(1, game.state.moves)
        assertFalse(game.restart(), "nothing to take off")
    }

    @Test
    fun aSavedGameThatCannotBeIsRefused() {
        val game = GlGame(puzzle)
        val start = puzzle.given
        assertFalse(game.restore(start.drop(1), 0), "the wrong number of cells")
        assertFalse(game.restore(start.mapIndexed { i, v -> if (i == open.first()) n + 1 else v }, 5), "a number above the size")
        val given = (0 until n * n).first { puzzle.isGiven(it) }
        assertFalse(game.restore(start.mapIndexed { i, v -> if (i == given) 0 else v }, 0), "a given cell changed")
        assertFalse(game.restore(start.mapIndexed { i, v -> if (i == open.first()) 1 else v }, 0), "fewer moves than numbers put")
        assertFalse(game.restore(answer.toList(), 99), "a solved board would have been won")
        assertEquals(start, game.state.cells)
        assertTrue(game.restore(start.mapIndexed { i, v -> if (i == open.first()) 1 else v }, 3))
        assertEquals(3, game.state.moves)
    }

    // --- the guide ---

    @Test
    fun theGuideSaysWhichNumberGoesWhere() {
        val game = GlGame(puzzle)
        val step = GlGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.GlFill, step.reason)
        val cell = step.cells.single()
        assertEquals(answer[cell], step.numbers.single())
        assertEquals(0, game.state.cells[cell])
    }

    @Test
    fun theGuideSaysToPutRightANumberThatIsNotTheAnswers() {
        val game = GlGame(puzzle)
        val cell = open.first()
        game.set(cell, (1..n).first { it != answer[cell] })
        val step = GlGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.GlClear, step.reason)
        assertEquals(listOf(cell), step.cells)
    }

    @Test
    fun followingTheGuideSolvesEveryGeneratedBoard() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 10) {
                val p = GlGenerator.generate(seed, difficulty)
                val solution = GlSolver.uniqueSolution(p)!!
                val game = GlGame(p)
                var steps = 0
                while (!game.isFinished) {
                    val step = assertNotNull(GlGuide.step(p, game.state), "$difficulty seed $seed")
                    assertEquals(GuideReason.GlFill, step.reason, "a game with only right numbers never needs one put right")
                    val cell = step.cells.single()
                    assertEquals(solution[cell], step.numbers.single(), "$difficulty seed $seed")
                    game.set(cell, solution[cell])
                    steps++
                    assertTrue(steps <= p.size * p.size - p.givenCount, "$difficulty seed $seed")
                }
                assertNull(GlGuide.step(p, game.state))
            }
        }
    }

    // --- the round ---

    private fun round(clock: () -> Long = { 0L }) = GlRound(0, "r1", puzzle, Language.EN, clock)

    @Test
    fun theRoundSavesAndRestoresItsNumbersAndTime() {
        var now = 0L
        val round = round { now }
        round.onShown()
        round.put(open[0], 1)
        round.put(open[1], 1)
        round.put(open[1], 2)
        now = 4_000
        val stored = round.toStored()
        assertEquals(GameKind.GreaterLess, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertTrue(stored.guesses.joinToString("").length <= 256)
        assertEquals("n3", stored.guesses[0])

        val back = GlRound.restore(0, stored, { now })!!
        val state = back.snapshot().state
        assertEquals(1, state.cells[open[0]])
        assertEquals(2, state.cells[open[1]])
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
        var moves = 0
        for (cell in open) {
            round.put(cell, answer[cell])
            moves++
        }
        now = 20_000
        assertTrue(round.isFinished)
        assertEquals(2_000L, round.snapshot().durationMs, "the clock stopped at the last number")
        assertEquals(listOf(round.snapshot().state.moves.toString()), round.toFinished(99L).guesses)
        assertEquals(moves, round.snapshot().state.moves)
    }

    @Test
    fun aTapThatDoesNothingDoesNotMakeTheRoundTouched() {
        val round = round()
        assertTrue(round.isUntouched)
        round.put(-1, 1)
        assertTrue(round.isUntouched)
        round.put(open.first(), 1)
        assertFalse(round.isUntouched)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsRefused() {
        val stored = round().toStored()
        assertNull(GlRound.restore(0, stored.copy(answer = "!!!"), { 0L }))
        assertNull(GlRound.restore(0, stored.copy(guesses = listOf("x")), { 0L }))
        assertNull(GlRound.restore(0, stored.copy(guesses = listOf("n1", "m9")), { 0L }))
        assertNull(GlRound.restore(0, stored.copy(guesses = listOf("n1", "mz" + "0".repeat(n * n - 1))), { 0L }))
        assertNotNull(GlRound.restore(0, stored, { 0L }))
    }
}
