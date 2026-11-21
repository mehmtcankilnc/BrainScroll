package com.mehmtcan.brainscroll.game.rectangles

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.guide.RectGuide
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RectGameTest {
    // A generated board, so the tests do not depend on one made up by hand: its one solution is known.
    private val puzzle = RectGenerator.generate(1, Difficulty.Easy)
    private val n = puzzle.size
    private val solution = RectSolver.uniqueSolution(puzzle)!!

    /** A rectangle that the rules allow (it holds one number and is as big as it says) but that is not part of the answer, if the board has one. */
    private fun allowedButWrong(): Rect? =
        puzzle.clueCells.flatMap { RectSolver.candidates(puzzle, it) }.firstOrNull { it !in solution }

    @Test
    fun aRectangleHoldingOneNumberAsBigAsItIsIsLaid() {
        val game = RectGame(puzzle)
        assertEquals(RectResult.Placed, game.place(solution[0]))
        assertEquals(listOf(solution[0]), game.state.rects)
        assertEquals(solution[0].area, game.state.covered)
        assertFalse(game.isFinished)
    }

    @Test
    fun aRectangleThatIsNotAllowedIsRejectedAndChangesNothing() {
        val game = RectGame(puzzle)
        assertEquals(RectResult.Rejected, game.place(Rect(0, 0, n, n)), "the whole board holds every number")
        val clue = puzzle.clueCells.first { puzzle.numbers[it] > 1 }
        assertEquals(RectResult.Rejected, game.place(Rect(clue % n, clue / n, 1, 1)), "one cell for a number above 1")
        assertEquals(RectResult.Placed, game.place(solution[0]))
        assertEquals(RectResult.Rejected, game.place(solution[0]), "on top of another rectangle")
        assertEquals(RectResult.Ignored, game.place(Rect(n - 1, 0, 2, 1)), "off the board")
        assertEquals(RectResult.Ignored, game.place(Rect(0, 0, 0, 1)), "nothing")
        assertEquals(listOf(solution[0]), game.state.rects)
        assertEquals(1, game.state.moves)
    }

    @Test
    fun aRectangleThatIsAllowedButWrongStillLiesThereUntilTakenOff() {
        val wrong = allowedButWrong() ?: return
        val game = RectGame(puzzle)
        assertEquals(RectResult.Placed, game.place(wrong))
        assertEquals(listOf(wrong), game.state.rects)
    }

    @Test
    fun coveringTheBoardWinsAndTheGameIsOverThen() {
        val game = RectGame(puzzle)
        solution.dropLast(1).forEach { assertEquals(RectResult.Placed, game.place(it)) }
        assertEquals(RectResult.Solved, game.place(solution.last()))
        assertTrue(game.isFinished)
        assertEquals(RectStatus.Won, game.state.status)
        assertEquals(RectResult.Ignored, game.place(solution[0]))
        assertFalse(game.remove(solution[0].x, solution[0].y))
        assertFalse(game.restart())
    }

    @Test
    fun aRectangleIsTakenOffByAnyOfItsCellsAndRestartTakesAllOff() {
        val game = RectGame(puzzle)
        val a = solution[0]
        val b = solution[1]
        game.place(a)
        game.place(b)
        val free = (0 until n * n).first { c -> game.state.rectAt(c % n, c / n) == null }
        assertFalse(game.remove(free % n, free / n), "nothing there")
        assertTrue(game.remove(a.x + a.w - 1, a.y + a.h - 1))
        assertEquals(listOf(b), game.state.rects)
        assertEquals(2, game.state.moves, "taking one off does not give the move back")
        assertTrue(game.restart())
        assertEquals(emptyList(), game.state.rects)
        assertFalse(game.restart(), "nothing to take off")
    }

    @Test
    fun aSavedGameThatCannotBeIsRefused() {
        val game = RectGame(puzzle)
        assertFalse(game.restore(listOf(Rect(0, 0, n, n)), 1), "a rectangle that is not allowed")
        assertFalse(game.restore(listOf(solution[0], solution[0]), 2), "two on top of each other")
        assertFalse(game.restore(solution, 99), "a covered board would have been won")
        assertFalse(game.restore(listOf(solution[0]), 0), "fewer moves than rectangles")
        assertEquals(emptyList(), game.state.rects)
        assertTrue(game.restore(listOf(solution[0]), 3))
        assertEquals(3, game.state.moves)
    }

    // --- the guide ---

    @Test
    fun theGuideSaysWhichRectangleToDraw() {
        val game = RectGame(puzzle)
        val first = RectGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.RectPlace, first.reason)
        val rect = Rect(first.cells[0], first.cells[1], first.cells[2], first.cells[3])
        assertTrue(rect in solution)
        assertEquals(listOf(rect.area), first.numbers)
    }

    @Test
    fun theGuideSaysToTakeOffARectangleThatIsNotPartOfTheAnswer() {
        val wrong = allowedButWrong() ?: return
        val game = RectGame(puzzle)
        game.place(wrong)
        val step = RectGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.RectRemove, step.reason)
        assertEquals(listOf(wrong.x, wrong.y, wrong.w, wrong.h), step.cells)
    }

    @Test
    fun followingTheGuideSolvesEveryGeneratedBoard() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 15) {
                val p = RectGenerator.generate(seed, difficulty)
                val game = RectGame(p)
                var steps = 0
                while (!game.isFinished) {
                    val step = assertNotNull(RectGuide.step(p, game.state), "$difficulty seed $seed")
                    assertEquals(GuideReason.RectPlace, step.reason, "a game that has only right rectangles never needs one taken off")
                    val result = game.place(Rect(step.cells[0], step.cells[1], step.cells[2], step.cells[3]))
                    assertTrue(result == RectResult.Placed || result == RectResult.Solved, "$difficulty seed $seed: $result")
                    steps++
                    assertTrue(steps <= p.clueCells.size)
                }
                assertNull(RectGuide.step(p, game.state))
            }
        }
    }

    // --- the round ---

    private fun round(clock: () -> Long = { 0L }) = RectRound(0, "r1", puzzle, Language.EN, clock)

    private fun text(r: Rect) = "${r.x}${r.y}${r.w}${r.h}"

    @Test
    fun theRoundSavesAndRestoresItsRectanglesAndTime() {
        var now = 0L
        val round = round { now }
        round.onShown()
        round.place(solution[0])
        round.place(solution[1])
        round.remove(solution[1].x, solution[1].y)
        now = 4_000
        val stored = round.toStored()
        assertEquals(listOf("n2", text(solution[0])), stored.guesses)
        assertEquals(GameKind.Rectangles, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)

        val back = RectRound.restore(0, stored, { now })!!
        val state = back.snapshot().state
        assertEquals(listOf(solution[0]), state.rects)
        assertEquals(2, state.moves)
        assertFalse(back.isUntouched)
    }

    @Test
    fun aFinishedRoundIsAWin() {
        val round = round()
        solution.dropLast(1).forEach { round.place(it) }
        assertEquals(RectResult.Solved, round.place(solution.last()))
        assertTrue(round.isFinished)
        assertEquals(listOf(solution.size.toString()), round.toFinished(1L).guesses)
    }

    @Test
    fun aSavedGameThatCannotBeReadStartsNew() {
        val stored = round().toStored()
        assertNull(RectRound.restore(0, stored.copy(answer = "!!"), { 0L }))
        assertNull(RectRound.restore(0, stored.copy(guesses = listOf("n1", "00")), { 0L }))
        assertNull(RectRound.restore(0, stored.copy(guesses = listOf("n1", "00x2")), { 0L }))
        assertNull(RectRound.restore(0, stored.copy(guesses = listOf("n1", "00$n$n")), { 0L }), "a rectangle that is not allowed")
    }

    @Test
    fun theTimeIsTheTimeThePageWasOnShow() {
        var now = 1_000L
        val round = round { now }
        round.onShown()
        now = 31_000L
        solution.forEach { round.place(it) }
        assertEquals(30_000L, round.snapshot().durationMs, "a board solved at once still took the time spent looking")
        now = 99_000L
        assertEquals(30_000L, round.snapshot().durationMs, "the time stops when it is covered")
    }

    @Test
    fun aRoundThatWasNeverTouchedIsUntouched() {
        val round = round()
        assertTrue(round.isUntouched)
        round.place(Rect(0, 0, n, n))
        assertTrue(round.isUntouched, "a rectangle that was refused is no start")
        round.place(solution[0])
        assertFalse(round.isUntouched)
    }
}
