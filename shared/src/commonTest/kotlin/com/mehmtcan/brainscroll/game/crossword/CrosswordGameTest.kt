package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrosswordGameTest {

    private val puzzle = CrosswordGenerator.generate(5, Language.EN)

    private fun game() = CrosswordGame(puzzle)

    private fun CrosswordGame.letterCells() = state.isClue.indices.filter { !state.isClue[it] }

    @Test
    fun theBoardStartsEmptyWithItsClues() {
        val state = game().state
        assertEquals(CrosswordStatus.Playing, state.status)
        assertEquals(puzzle.slots.size, state.clues.size)
        assertTrue(state.letters.all { it == ' ' })
        assertEquals(0, state.mistakes)
        assertEquals(puzzle.clues.toSet(), state.clues.map { it.text }.toSet())
    }

    @Test
    fun aRightLetterIsFinalAndAWrongOneIsAMistakeThatCanBeTakenBack() {
        val g = game()
        val cell = g.letterCells().first()
        val right = g.solution[cell]
        val wrong = ('A'..'Z').first { it != right }
        assertEquals(CrosswordResult.Mistake, g.place(cell, wrong))
        assertEquals(1, g.state.mistakes)
        assertEquals(setOf(cell), g.state.wrong)
        assertEquals(CrosswordResult.Ignored, g.place(cell, wrong), "the same wrong letter again is nothing")
        assertEquals(CrosswordResult.Erased, g.erase(cell))
        assertEquals(' ', g.state.letters[cell])
        assertEquals(CrosswordResult.Correct, g.place(cell, right))
        assertTrue(cell in g.state.correct)
        assertEquals(CrosswordResult.Ignored, g.place(cell, wrong), "a right letter is final")
        assertEquals(CrosswordResult.Ignored, g.erase(cell))
        assertEquals(2, g.state.moves)
    }

    @Test
    fun aClueSquareAFarSquareAndANonLetterAreIgnored() {
        val g = game()
        assertEquals(CrosswordResult.Ignored, g.place(0, 'A'), "square 0 is a clue")
        assertEquals(CrosswordResult.Ignored, g.place(999, 'A'))
        assertEquals(CrosswordResult.Ignored, g.place(g.letterCells().first(), '7'))
        assertEquals(CrosswordResult.Ignored, g.place(g.letterCells().first(), 'Ğ'), "a letter of another language")
        assertEquals(CrosswordResult.Ignored, g.erase(g.letterCells().first()), "nothing there to take back")
        assertEquals(0, g.state.moves)
    }

    @Test
    fun fillingEveryLetterRightWinsTheGameAndNothingMoreIsAccepted() {
        val g = game()
        for (cell in g.letterCells()) g.place(cell, g.solution[cell])
        assertTrue(g.isFinished)
        assertEquals(CrosswordStatus.Won, g.state.status)
        assertEquals(g.letterCells().size, g.state.moves)
        assertEquals(CrosswordResult.Ignored, g.place(g.letterCells().first(), 'Z'))
    }

    @Test
    fun theTurkishBoardTakesTurkishLettersOnly() {
        val tr = CrosswordGame(CrosswordGenerator.generate(2, Language.TR))
        val cell = tr.letterCells().first()
        assertEquals(CrosswordResult.Ignored, tr.place(cell, 'Q'), "Q is not in the Turkish alphabet")
        val wrong = "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ".first { it != tr.solution[cell] }
        assertEquals(CrosswordResult.Mistake, tr.place(cell, wrong))
    }

    @Test
    fun theLettersAtACrossingBelongToBothAnswers() {
        val g = game()
        val crossing = (0 until CrosswordLayout.CELLS).first { cell -> puzzle.slots.count { cell in it.cells } == 2 }
        val both = puzzle.slots.filter { crossing in it.cells }
        for (slot in both) assertEquals(g.solution[crossing], puzzle.words[puzzle.slots.indexOf(slot)][slot.cells.indexOf(crossing)])
        assertFalse(g.state.clues.filter { crossing in it.cells }.size != 2)
    }
}
