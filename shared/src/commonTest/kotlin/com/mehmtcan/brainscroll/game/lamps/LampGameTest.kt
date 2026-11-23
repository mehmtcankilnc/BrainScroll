package com.mehmtcan.brainscroll.game.lamps

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.guide.LampGuide
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LampGameTest {
    // A generated board, so the tests do not depend on one made up by hand: its one solution is known.
    private val puzzle = LampGenerator.generate(1, Difficulty.Easy)
    private val n = puzzle.size
    private val answer = LampSolver.uniqueSolution(puzzle)!!
    private val lampCells = answer.indices.filter { answer[it] }

    /** A white cell that the answer does not put a lamp on. */
    private val spare = puzzle.whiteCells.first { !answer[it] }

    private fun lamp(game: LampGame, cell: Int) = game.tap(cell)

    @Test
    fun aTapGoesFromEmptyToLampToDotToEmpty() {
        val game = LampGame(puzzle)
        val cell = spare
        assertEquals(LampResult.Lamp, game.tap(cell))
        assertEquals(LampMark.Lamp, game.state.marks[cell])
        assertEquals(LampResult.Dot, game.tap(cell))
        assertEquals(LampMark.Dot, game.state.marks[cell])
        assertEquals(LampResult.Cleared, game.tap(cell))
        assertEquals(LampMark.Empty, game.state.marks[cell])
        assertEquals(3, game.state.moves)
    }

    @Test
    fun aWallAndACellOffTheBoardAreIgnored() {
        val game = LampGame(puzzle)
        val wall = (0 until n * n).first { puzzle.isWall(it) }
        assertEquals(LampResult.Ignored, game.tap(wall))
        assertEquals(LampResult.Ignored, game.tap(-1))
        assertEquals(LampResult.Ignored, game.tap(n * n))
        assertEquals(0, game.state.moves)
    }

    @Test
    fun aLampLightsCellsAndTwoLampsThatSeeEachOtherAreMarked() {
        val game = LampGame(puzzle)
        val first = puzzle.whiteCells.first { puzzle.seenFrom(it).isNotEmpty() }
        game.tap(first)
        val seen = puzzle.seenFrom(first).first()
        assertTrue(game.state.lit[first] && game.state.lit[seen])
        assertTrue(game.state.clashing.isEmpty())
        game.tap(seen)
        assertEquals(setOf(first, seen), game.state.clashing)
    }

    @Test
    fun puttingTheAnswersLampsWinsAndTheGameIsOverThen() {
        val game = LampGame(puzzle)
        lampCells.dropLast(1).forEach { assertEquals(LampResult.Lamp, game.tap(it)) }
        assertFalse(game.isFinished)
        assertEquals(LampResult.Solved, game.tap(lampCells.last()))
        assertTrue(game.isFinished)
        assertEquals(LampStatus.Won, game.state.status)
        assertEquals(LampResult.Ignored, game.tap(spare))
        assertFalse(game.restart())
        assertEquals(puzzle.whiteCells.size, game.state.litCount)
    }

    @Test
    fun dotsDoNotChangeTheAnswer() {
        val game = LampGame(puzzle)
        game.tap(spare)
        game.tap(spare)
        assertEquals(LampMark.Dot, game.state.marks[spare])
        lampCells.forEach { game.tap(it) }
        assertTrue(game.isFinished, "a dot on a cell that stays dark of lamps is fine")
    }

    @Test
    fun turningAWrongLampIntoADotIsTheTapThatWins() {
        // The player has the answer's lamps and one lamp too many (put first, so no tap before it wins): the board is not solved.
        val game = LampGame(puzzle)
        game.tap(spare)
        lampCells.forEach { game.tap(it) }
        assertFalse(game.isFinished, "an extra lamp spoils the board")
        assertEquals(LampResult.Solved, game.tap(spare), "the dot makes the board right")
        assertTrue(game.isFinished)
    }

    @Test
    fun restartTakesEveryMarkOffButNotTheMoves() {
        val game = LampGame(puzzle)
        game.tap(spare)
        assertTrue(game.restart())
        assertTrue(game.state.marks.all { it == LampMark.Empty })
        assertEquals(1, game.state.moves)
        assertFalse(game.restart(), "nothing to take off")
    }

    @Test
    fun aSavedGameThatCannotBeIsRefused() {
        val game = LampGame(puzzle)
        val empty = List(n * n) { LampMark.Empty }
        fun marked(vararg cells: Int) = empty.mapIndexed { i, m -> if (i in cells) LampMark.Lamp else m }
        val wall = (0 until n * n).first { puzzle.isWall(it) }
        assertFalse(game.restore(empty.drop(1), 0), "the wrong number of cells")
        assertFalse(game.restore(marked(wall), 1), "a mark on a wall")
        assertFalse(game.restore(marked(spare), 0), "fewer moves than marks")
        assertFalse(game.restore(marked(*lampCells.toIntArray()), 99), "a solved board would have been won")
        assertTrue(game.state.marks.all { it == LampMark.Empty })
        assertTrue(game.restore(marked(spare), 3))
        assertEquals(3, game.state.moves)
    }

    // --- the guide ---

    @Test
    fun theGuideSaysWhereToPutALamp() {
        val game = LampGame(puzzle)
        val step = LampGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.LampPlace, step.reason)
        assertTrue(answer[step.cells.single()])
    }

    @Test
    fun theGuideSaysToPutRightALampThatIsNotPartOfTheAnswer() {
        val game = LampGame(puzzle)
        game.tap(spare)
        val step = LampGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.LampClear, step.reason)
        assertEquals(listOf(spare), step.cells)
    }

    @Test
    fun theGuideSaysToPutRightADotOnACellThatHoldsALamp() {
        val game = LampGame(puzzle)
        game.tap(lampCells[0])
        game.tap(lampCells[0])
        val step = LampGuide.step(puzzle, game.state)!!
        assertEquals(GuideReason.LampClear, step.reason)
        assertEquals(listOf(lampCells[0]), step.cells)
    }

    @Test
    fun followingTheGuideSolvesEveryGeneratedBoard() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 10) {
                val p = LampGenerator.generate(seed, difficulty)
                val solution = LampSolver.uniqueSolution(p)!!
                val game = LampGame(p)
                var steps = 0
                while (!game.isFinished) {
                    val step = assertNotNull(LampGuide.step(p, game.state), "$difficulty seed $seed")
                    assertEquals(GuideReason.LampPlace, step.reason, "a game with only right lamps never needs one put right")
                    val cell = step.cells.single()
                    assertTrue(solution[cell], "$difficulty seed $seed")
                    val result = game.tap(cell)
                    assertTrue(result == LampResult.Lamp || result == LampResult.Solved, "$difficulty seed $seed: $result")
                    steps++
                    assertTrue(steps <= solution.count { it })
                }
                assertNull(LampGuide.step(p, game.state))
            }
        }
    }

    // --- the round ---

    private fun round(clock: () -> Long = { 0L }) = LampRound(0, "r1", puzzle, Language.EN, clock)

    @Test
    fun theRoundSavesAndRestoresItsMarksAndTime() {
        var now = 0L
        val round = round { now }
        round.onShown()
        round.tap(lampCells[0])
        round.tap(spare)
        round.tap(spare)
        now = 4_000
        val stored = round.toStored()
        assertEquals(GameKind.Lamps, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertTrue(stored.guesses.joinToString("").length <= 256)
        assertEquals("n3", stored.guesses[0])

        val back = LampRound.restore(0, stored, { now })!!
        val state = back.snapshot().state
        assertEquals(LampMark.Lamp, state.marks[lampCells[0]])
        assertEquals(LampMark.Dot, state.marks[spare])
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
        lampCells.forEach { round.tap(it) }
        now = 20_000
        assertTrue(round.isFinished)
        assertEquals(2_000L, round.snapshot().durationMs, "the clock stopped at the last lamp")
        assertEquals(listOf(lampCells.size.toString()), round.toFinished(99L).guesses)
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
        assertNull(LampRound.restore(0, stored.copy(answer = "!!!"), { 0L }))
        assertNull(LampRound.restore(0, stored.copy(guesses = listOf("x")), { 0L }))
        assertNull(LampRound.restore(0, stored.copy(guesses = listOf("n1", "m9")), { 0L }))
        assertNull(LampRound.restore(0, stored.copy(guesses = listOf("n1", "m11")), { 0L }), "a mark for every cell is needed")
        assertNotNull(LampRound.restore(0, stored, { 0L }))
    }
}
