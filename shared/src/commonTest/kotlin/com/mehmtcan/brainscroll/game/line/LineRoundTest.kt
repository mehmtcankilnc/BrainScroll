package com.mehmtcan.brainscroll.game.line

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LineRoundTest {

    private var clock = 1_000L

    private val puzzle = LineFixtures.board

    private fun round() = LineRound(0, "l1", puzzle, Language.EN) { clock }

    private fun LineRound.solve(secondsPerCell: Long = 1) {
        for (cell in puzzle.solution) {
            clock += secondsPerCell * 1_000
            drag(cell)
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstChangeAndHasNoGuide() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Line, r.kind)
        assertFalse(r.assisted)
        assertNull(r.guideTask(), "the hint is the game's own, not the guide")
        r.drag(puzzle.solution[0])
        assertFalse(r.isUntouched)
    }

    @Test
    fun anActionThatChangesNothingIsNotRecorded() {
        val r = round()
        r.onShown()
        r.drag(puzzle.solution[0])
        clock += 4_000
        assertEquals(LineResult.Ignored, r.drag(99))
        assertEquals(LineResult.Ignored, r.drag(puzzle.solution[0]), "the end of the line itself")
        assertFalse(r.undo(), "number 1 itself is never taken off")
        clock += 3_000
        r.drag(puzzle.solution[1])
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(listOf("d${puzzle.solution[0]}", "d${puzzle.solution[1]}"), r.toStored().guesses)
    }

    @Test
    fun theTimeStartsWhenThePageComesToTheScreenNotWithTheFirstTouch() {
        val r = round()
        r.onShown()
        clock += 30_000
        r.drag(puzzle.solution[0])
        assertEquals(30_000, r.snapshot().durationMs, "looking at the board counts")
    }

    @Test
    fun theClockStopsWhenThePageIsLeftAndGoesOnWhenItComesBack() {
        val r = round()
        r.onShown()
        clock += 5_000
        r.onHidden()
        clock += 10 * 60_000
        assertEquals(5_000, r.snapshot().durationMs, "a stopped clock does not move")
        r.onShown()
        clock += 2_000
        assertEquals(7_000, r.snapshot().durationMs)
    }

    @Test
    fun theClockStopsWhenTheBoardIsDone() {
        val r = round()
        r.onShown()
        r.solve()
        clock += 10 * 60_000
        assertEquals(puzzle.cells * 1_000L, r.snapshot().durationMs)
        assertEquals(puzzle.cells * 1_000L, r.toFinished(1).durationMs)
    }

    @Test
    fun aBoardThatWasOnlyLookedAtIsSavedWithItsTimeSoLeavingDoesNotStartItAgain() {
        val r = round()
        assertTrue(r.isUntouched)
        r.onShown()
        clock += 10_000
        r.onHidden()
        assertFalse(r.isUntouched, "the time of looking has to be kept")
        val stored = r.toStored()
        assertEquals("10000", stored.input)
        val back = assertNotNull(LineRound.restore(0, stored) { clock })
        assertEquals(10_000, back.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsBoardAndActions() {
        val r = round()
        val line = puzzle.solution
        r.drag(line[0]); r.drag(line[1]); r.drag(line[2]); r.drag(line[3])
        r.undo()
        r.tap(line[1])
        r.drag(line[2])
        r.showHint()
        r.restart()
        r.drag(line[0])
        clock += 3_000
        r.drag(line[1])
        val stored = r.toStored()
        assertEquals(GameKind.Line, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertEquals(true, stored.assisted, "a hint was used")
        assertTrue(stored.guesses.containsAll(listOf("u", "r", "h", "t${line[1]}")))

        val back = assertNotNull(LineRound.restore(2, stored) { clock })
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
        assertEquals(2, back.snapshot().state.undos, "the undo and the restart count, the retraction does not")
        assertEquals(1, back.snapshot().state.hintsUsed)
    }

    @Test
    fun theBoardIsSavedWithoutItsSolutionAndFoundAgain() {
        val text = puzzle.encode()
        assertTrue(text.length < 60, text)
        val back = assertNotNull(LinePuzzle.decode(text))
        assertContentEquals(puzzle.checkpoints, back.checkpoints)
        assertContentEquals(puzzle.solution, back.solution, "one line, so the same one")
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().also { it.drag(puzzle.solution[0]) }.toStored()
        fun gone(answer: String = stored.answer, guesses: List<String> = stored.guesses) =
            assertNull(LineRound.restore(0, stored.copy(answer = answer, guesses = guesses)) { clock }, "$answer $guesses")
        gone(answer = "")
        gone(answer = "x:1,2")
        gone(answer = "5:")
        gone(answer = "5:3")
        gone(answer = "5:0,0")
        gone(answer = "5:0,99")
        gone(answer = "2:0,3")
        gone(answer = "3:0,4,1", guesses = emptyList()) // no line passes these numbers
        gone(guesses = listOf("x5"))
        gone(guesses = listOf("d"))
        gone(guesses = listOf("t-"))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseGuessesAreTheMovesTheUndosAndTheHints() {
        val r = round()
        clock = 5_000
        r.onShown()
        r.solve()
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode, "the daily screen saves it as a daily result")
        assertEquals(GameKind.Line, finished.game)
        assertEquals(listOf(puzzle.cells.toString(), "0", "0"), finished.guesses)
        assertEquals(puzzle.cells, finished.moveCount)
        assertEquals("${puzzle.size}x${puzzle.size}", finished.answer)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters")
        assertEquals(puzzle.cells * 1_000L, finished.durationMs)
        assertFalse(finished.assisted)
    }

    @Test
    fun aHintMakesTheRoundAssistedSoItLeavesTheStatistics() {
        val r = round()
        r.drag(puzzle.solution[0])
        val hint = assertNotNull(r.showHint())
        assertContentEquals(puzzle.solution.drop(1).take(3), hint)
        assertContentEquals(hint, r.snapshot().hint)
        assertTrue(r.assisted)
        r.drag(puzzle.solution[1])
        assertTrue(r.snapshot().hint.isEmpty(), "the hint goes away with the next change")
        for (cell in puzzle.solution.drop(2)) r.drag(cell)
        val finished = r.toFinished(1)
        assertEquals(listOf(puzzle.cells.toString(), "0", "1"), finished.guesses)
        assertTrue(finished.assisted)
    }

    @Test
    fun theMovesOfTheResultCountEverySquareThatWasPutOn() {
        val r = round()
        val line = puzzle.solution
        r.drag(line[0]); r.drag(line[1]); r.drag(line[2])
        repeat(2) { r.undo() }
        assertEquals(LineResult.Ignored, r.tap(line[0]), "the line is only number 1 now")
        for (cell in line.drop(1)) r.drag(cell)
        assertTrue(r.isFinished)
        val finished = r.toFinished(1)
        assertEquals("27", finished.guesses[0], "3 squares, then the 24 that are left")
        assertEquals(27, finished.moveCount)
        assertEquals(2, r.snapshot().state.undos)
    }
}
