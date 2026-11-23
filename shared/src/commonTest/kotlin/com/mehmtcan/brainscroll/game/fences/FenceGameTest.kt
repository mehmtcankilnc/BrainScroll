package com.mehmtcan.brainscroll.game.fences

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.FenceGuide
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FenceGameTest {
    // A generated board, so the tests do not depend on one made up by hand: its one loop is known.
    private val puzzle = FenceGenerator.generate(1, Difficulty.Easy)
    private val answer = FenceSolver.uniqueSolution(puzzle)!!
    private val lineEdges = answer.indices.filter { answer[it] }

    /** An edge the answer leaves empty. */
    private val spare = answer.indices.first { !answer[it] }

    @Test
    fun aTapGoesFromEmptyToLineToCrossToEmpty() {
        val game = FenceGame(puzzle)
        assertEquals(FenceResult.Line, game.tap(spare))
        assertEquals(FenceSolver.LINE, game.state.marks[spare])
        assertEquals(FenceResult.Cross, game.tap(spare))
        assertEquals(FenceSolver.CROSS, game.state.marks[spare])
        assertEquals(FenceResult.Cleared, game.tap(spare))
        assertEquals(FenceSolver.UNKNOWN, game.state.marks[spare])
        assertEquals(3, game.state.moves)
    }

    @Test
    fun anEdgeOffTheBoardIsIgnored() {
        val game = FenceGame(puzzle)
        assertEquals(FenceResult.Ignored, game.tap(-1))
        assertEquals(FenceResult.Ignored, game.tap(puzzle.geometry.edges))
        assertEquals(0, game.state.moves)
    }

    @Test
    fun drawingTheLoopWinsAndTheGameIsOverThen() {
        val game = FenceGame(puzzle)
        lineEdges.dropLast(1).forEach { assertEquals(FenceResult.Line, game.tap(it)) }
        assertFalse(game.isFinished)
        assertEquals(FenceResult.Solved, game.tap(lineEdges.last()))
        assertTrue(game.isFinished)
        assertEquals(FenceStatus.Won, game.state.status)
        assertEquals(FenceResult.Ignored, game.tap(spare))
        assertFalse(game.restart())
        assertEquals(lineEdges.size, game.state.lineCount)
    }

    @Test
    fun crossesDoNotChangeTheAnswer() {
        val game = FenceGame(puzzle)
        game.tap(spare)
        game.tap(spare)
        assertEquals(FenceSolver.CROSS, game.state.marks[spare])
        lineEdges.forEach { game.tap(it) }
        assertTrue(game.isFinished, "a cross on an edge that stays empty is fine")
    }

    @Test
    fun turningAWrongLineIntoACrossIsTheTapThatWins() {
        // The player has the loop and one line too many (drawn first, so no tap before it wins): the board is not solved.
        val game = FenceGame(puzzle)
        game.tap(spare)
        lineEdges.forEach { game.tap(it) }
        assertFalse(game.isFinished, "an extra line spoils the board")
        assertEquals(FenceResult.Solved, game.tap(spare), "the cross makes the board right")
        assertTrue(game.isFinished)
    }

    @Test
    fun theBoardShowsBranchesAndSquaresWithTooManyLines() {
        val game = FenceGame(puzzle)
        val g = puzzle.geometry
        // Three lines at one dot.
        val dot = g.dotSide + 1
        g.dotEdges[dot].forEach { game.tap(it) }
        assertEquals(setOf(dot), game.state.branching)
        // A numbered square with all four sides drawn has too many lines (unless its number is 4, which cannot be).
        val fresh = FenceGame(puzzle)
        val cell = puzzle.clues.indexOfFirst { it in 0..3 }
        g.cellEdges[cell].forEach { fresh.tap(it) }
        assertTrue(cell in fresh.state.overfull)
    }

    @Test
    fun restartTakesEveryMarkOffButNotTheMoves() {
        val game = FenceGame(puzzle)
        game.tap(spare)
        assertTrue(game.restart())
        assertTrue(game.state.marks.all { it == FenceSolver.UNKNOWN })
        assertEquals(1, game.state.moves)
        assertFalse(game.restart(), "nothing to take off")
    }

    @Test
    fun aSavedGameThatCannotBeIsRefused() {
        val game = FenceGame(puzzle)
        val empty = List(puzzle.geometry.edges) { 0 }
        fun marked(vararg edges: Int) = empty.mapIndexed { i, m -> if (i in edges) FenceSolver.LINE else m }
        assertFalse(game.restore(empty.drop(1), 0), "the wrong number of edges")
        assertFalse(game.restore(empty.mapIndexed { i, m -> if (i == 0) 3 else m }, 5), "a mark that is none of the three")
        assertFalse(game.restore(marked(spare), 0), "fewer moves than marks")
        assertFalse(game.restore(marked(*lineEdges.toIntArray()), 99), "a solved board would have been won")
        assertTrue(game.state.marks.all { it == FenceSolver.UNKNOWN })
        assertTrue(game.restore(marked(spare), 3))
        assertEquals(3, game.state.moves)
    }

    // --- the guide ---

    @Test
    fun theGuideSaysWhereToDrawALine() {
        val game = FenceGame(puzzle)
        val step = FenceGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.FenceLine, step.reason)
        assertTrue(answer[step.cells.single()])
    }

    @Test
    fun theGuideSaysToPutRightAMarkThatIsNotTheAnswers() {
        val game = FenceGame(puzzle)
        game.tap(spare)
        val step = FenceGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.FenceClear, step.reason)
        assertEquals(listOf(spare), step.cells)
        // A cross on a line of the answer is wrong too.
        val other = FenceGame(puzzle)
        other.tap(lineEdges[0])
        other.tap(lineEdges[0])
        assertEquals(GuideReason.FenceClear, FenceGuide.step(puzzle, other.state)!!.reason)
    }

    @Test
    fun theGuideGoesOnFromTheLinesAlreadyDrawn() {
        // With part of the loop drawn, the next line touches what is drawn (a dot of a drawn line), whenever the answer has such a line.
        val g = puzzle.geometry
        val game = FenceGame(puzzle)
        val start = lineEdges.first()
        game.tap(start)
        val reached = g.edgeDots[start].toSet()
        val step = FenceGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.FenceLine, step.reason)
        assertTrue(g.edgeDots[step.cells.single()].any { it in reached }, "the line goes on from the one drawn")
    }

    @Test
    fun followingTheGuideSolvesEveryGeneratedBoard() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 6) {
                val p = FenceGenerator.generate(seed, difficulty)
                val solution = FenceSolver.solveByLogic(p, true)!!
                val game = FenceGame(p)
                var steps = 0
                while (!game.isFinished) {
                    val step = assertNotNull(FenceGuide.step(p, game.state), "$difficulty seed $seed")
                    assertEquals(GuideReason.FenceLine, step.reason, "the guide only points at lines to draw")
                    val edge = step.cells.single()
                    assertEquals(FenceSolver.LINE, solution[edge], "$difficulty seed $seed")
                    game.tap(edge)
                    steps++
                    assertTrue(steps <= solution.count { it == FenceSolver.LINE }, "$difficulty seed $seed")
                }
                assertNull(FenceGuide.step(p, game.state))
            }
        }
    }

    // --- the round ---

    private fun round(clock: () -> Long = { 0L }) = FenceRound(0, "r1", puzzle, Language.EN, clock)

    @Test
    fun theRoundSavesAndRestoresItsMarksAndTime() {
        var now = 0L
        val round = round { now }
        round.onShown()
        round.tap(lineEdges[0])
        round.tap(spare)
        round.tap(spare)
        now = 4_000
        val stored = round.toStored()
        assertEquals(GameKind.Fences, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertTrue(stored.guesses.joinToString("").length <= 256)
        assertEquals("n3", stored.guesses[0])

        val back = FenceRound.restore(0, stored, { now })!!
        val state = back.snapshot().state
        assertEquals(FenceSolver.LINE, state.marks[lineEdges[0]])
        assertEquals(FenceSolver.CROSS, state.marks[spare])
        assertEquals(3, state.moves)
        assertEquals(4_000L, back.snapshot().durationMs)
        assertFalse(back.isUntouched)
    }

    @Test
    fun aBigBoardIsSavedInTheLimitsOfTheTable() {
        val p = FenceGenerator.generate(3, Difficulty.Hard)
        val round = FenceRound(0, "r2", p, Language.EN, { 0L })
        for (e in 0 until p.geometry.edges step 3) round.tap(e)
        val stored = round.toStored()
        assertTrue(stored.answer.length <= 32, "${stored.answer.length} characters")
        assertTrue(stored.guesses.joinToString("").length <= 256, "${stored.guesses.joinToString("").length} characters")
        assertNotNull(FenceRound.restore(0, stored, { 0L }))
    }

    @Test
    fun theTimeRunsOnlyWhileThePageIsOnShowAndStopsWhenTheLoopIsClosed() {
        var now = 0L
        val round = round { now }
        now = 1_000
        round.onShown()
        now = 3_000
        round.onHidden()
        now = 10_000
        assertEquals(2_000L, round.snapshot().durationMs)
        round.onShown()
        lineEdges.forEach { round.tap(it) }
        now = 20_000
        assertTrue(round.isFinished)
        assertEquals(2_000L, round.snapshot().durationMs, "the clock stopped at the last line")
        assertEquals(listOf(lineEdges.size.toString()), round.toFinished(99L).guesses)
    }

    @Test
    fun aTapThatDoesNothingDoesNotMakeTheRoundTouched() {
        val round = round()
        assertTrue(round.isUntouched)
        round.tap(-1)
        assertTrue(round.isUntouched)
        round.tap(spare)
        assertFalse(round.isUntouched)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsRefused() {
        val stored = round().toStored()
        assertNull(FenceRound.restore(0, stored.copy(answer = "!!!"), { 0L }))
        assertNull(FenceRound.restore(0, stored.copy(guesses = listOf("x")), { 0L }))
        assertNull(FenceRound.restore(0, stored.copy(guesses = listOf("n1", "m9")), { 0L }))
        assertNull(FenceRound.restore(0, stored.copy(guesses = listOf("n1", "m3" + "0".repeat(puzzle.geometry.edges - 1))), { 0L }))
        assertNotNull(FenceRound.restore(0, stored, { 0L }))
    }
}
