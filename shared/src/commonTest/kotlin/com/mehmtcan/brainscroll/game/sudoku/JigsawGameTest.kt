package com.mehmtcan.brainscroll.game.sudoku

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.guide.SudokuGuide
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

/** The jigsaw sudoku: the same game as the classic one, played on regions of other shapes. */
class JigsawGameTest {

    private var clock = 1_000L
    private val seed = 5

    private fun game() = SudokuGame(seed, JigsawGenerator.DEFAULT_CLUES, SudokuVariant.Jigsaw)

    private fun round() = SudokuRound(0, "j1", seed, Language.EN, { clock }, JigsawGenerator.DEFAULT_CLUES, SudokuVariant.Jigsaw)

    private fun SudokuGame.empty() = state.values.indices.filter { state.values[it] == 0 }

    @Test
    fun theStateCarriesTheRegionsAndTheyAreNotTheBoxes() {
        val state = game().state
        assertEquals(81, state.regions.size)
        assertFalse(SudokuLayout.of(state.regions).isStandard)
        assertTrue(SudokuGame(seed).state.let { SudokuLayout.of(it.regions).isStandard }, "the classic board keeps its boxes")
    }

    @Test
    fun aRightDigitSettlesAndAWrongOneIsAMistakeJustLikeTheClassicBoard() {
        val g = game()
        val cell = g.empty().first()
        val right = g.solution[cell]
        val wrong = (1..9).first { it != right }
        assertEquals(SudokuResult.Mistake, g.place(cell, wrong))
        assertEquals(1, g.state.mistakes)
        assertEquals(setOf(cell), g.state.wrong)
        assertEquals(SudokuResult.Changed, g.erase(cell))
        assertEquals(SudokuResult.Correct, g.place(cell, right))
        assertEquals(SudokuResult.Ignored, g.place(cell, wrong), "a right digit is final")
    }

    @Test
    fun aRightDigitClearsItsNoteFromTheCellsOfItsRowColumnAndRegionOnly() {
        val g = game()
        val layout = SudokuLayout.of(g.state.regions)
        val cell = g.empty().first { c -> layout.peers[c].any { g.state.values[it] == 0 } }
        val digit = g.solution[cell]
        val peer = layout.peers[cell].first { g.state.values[it] == 0 }
        val stranger = g.empty().first { it != cell && it !in layout.peers[cell] }
        g.toggleNote(peer, digit)
        g.toggleNote(stranger, digit)
        g.place(cell, digit)
        assertEquals(0, g.state.notes[peer] and (1 shl (digit - 1)), "the note of a cell that shares a unit is cleared")
        assertTrue(g.state.notes[stranger] and (1 shl (digit - 1)) != 0, "any other cell keeps it")
    }

    @Test
    fun fillingEveryCellRightWinsTheGame() {
        val g = game()
        for (cell in g.empty()) g.place(cell, g.solution[cell])
        assertEquals(SudokuStatus.Won, g.state.status)
    }

    @Test
    fun theRoundIsAJigsawRoundAndSavesAndRestoresAsOne() {
        val r = round()
        assertEquals(GameKind.Jigsaw, r.kind)
        assertEquals(GameKind.Jigsaw, r.snapshot().kind)
        val cell = r.snapshot().state.let { s -> s.values.indices.first { s.values[it] == 0 } }
        r.toggleNote(cell, 4)
        clock += 2_000
        r.place(cell, 1)
        val stored = r.toStored()
        assertEquals(GameKind.Jigsaw, stored.game)
        assertTrue(stored.answer.length <= 32)

        val back = assertNotNull(SudokuRound.restore(3, stored, { clock }))
        assertEquals(GameKind.Jigsaw, back.kind)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
    }

    @Test
    fun aSavedClassicRoundStaysClassicAndAnUnknownGameIsDropped() {
        val classic = SudokuRound(0, "s1", 4, Language.EN, { clock })
        assertEquals(GameKind.Sudoku, classic.kind)
        val back = assertNotNull(SudokuRound.restore(0, classic.toStored(), { clock }))
        assertEquals(GameKind.Sudoku, back.kind)
        assertTrue(SudokuLayout.of(back.snapshot().state.regions).isStandard)
        assertNull(SudokuRound.restore(0, classic.toStored().copy(game = GameKind.Hanoi), { clock }))
    }

    @Test
    fun aFinishedJigsawRoundIsAWinOfTheJigsawGame() {
        val r = round()
        val state = r.snapshot().state
        val solution = SudokuGenerator.solveBySingles(state.givens.toIntArray(), SudokuLayout.of(state.regions))!!
        for (cell in state.values.indices) if (state.values[cell] == 0) r.place(cell, solution[cell])
        assertTrue(r.isFinished)
        val finished = r.toFinished(99)
        assertEquals(GameKind.Jigsaw, finished.game)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(state.values.count { it == 0 }, finished.moveCount)
        assertTrue(finished.answer.length <= 32)
    }

    @Test
    fun theGuideSpeaksOfRegionsOnAJigsawBoardAndOfBoxesOnTheClassicOne() {
        val jigsaw = assertNotNull(assertNotNull(round().guideTask())())
        assertTrue(jigsaw.reason in listOf(GuideReason.JigsawOnlyDigit, GuideReason.JigsawOnlyPlace), "${jigsaw.reason}")
        val classic = assertNotNull(SudokuGuide.step(SudokuGame(4).state, SudokuGame(4).solution))
        assertTrue(classic.reason in listOf(GuideReason.SudokuOnlyDigit, GuideReason.SudokuOnlyPlace), "${classic.reason}")
    }

    @Test
    fun theGuidePointsAtACellThatReallyHasOneCandidateOrOnePlace() {
        val g = game()
        val step = assertNotNull(SudokuGuide.step(g.state, g.solution))
        val cell = step.cells.single()
        assertEquals(g.solution[cell], step.numbers.single(), "the digit it names is the right one")
    }
}
