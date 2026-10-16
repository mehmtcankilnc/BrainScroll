package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrosswordGeneratorTest {

    private val size = CrosswordLayout.SIZE

    @Test
    fun everyReadyBoardOfBothLanguagesFollowsTheRules() {
        for (language in Language.entries) {
            val boards = CrosswordBoards.of(language)
            assertTrue(boards.size >= 100, "$language has ${boards.size} boards")
            boards.forEachIndexed { number, text ->
                val puzzle = CrosswordPuzzle.ofBoard(language, number)
                assertEquals(text, puzzle.squares)
                assertEquals(CrosswordLayout.CELLS, puzzle.squares.length)
                // Every answer is a word of the bank with its clue, and none is twice.
                for (i in puzzle.slots.indices) {
                    assertEquals(puzzle.slots[i].cells.size, puzzle.words[i].length)
                    assertEquals(CrosswordBank.clueOf(language, puzzle.words[i]), puzzle.clues[i])
                }
                assertEquals(puzzle.words.size, puzzle.words.toSet().size, "$language $number: an answer is twice")
                // Every run of letters, across or down, is an answer: nothing in a row or a column is a word that is not one.
                val runs = CrosswordLayout.runs(puzzle.isClue)
                assertEquals(runs.size, puzzle.slots.size, "$language $number")
                // Every letter square is in an answer, and every clue square holds one or two clues.
                val covered = BooleanArray(CrosswordLayout.CELLS)
                for (slot in puzzle.slots) for (cell in slot.cells) covered[cell] = true
                assertTrue(puzzle.isClue.indices.all { puzzle.isClue[it] || covered[it] }, "$language $number: a letter in no answer")
                val perSquare = puzzle.slots.groupBy { it.clueCell }
                assertTrue(puzzle.isClue.indices.all { !puzzle.isClue[it] || perSquare[it]?.size in 1..2 }, "$language $number: a clue square without a clue")
                // The clue square is next to the first letter of its answer.
                for (slot in puzzle.slots) {
                    val first = slot.cells.first()
                    val straight = if (slot.across) first - 1 else first - size
                    val turning = if (slot.across) first - size else first - 1
                    assertEquals(if (slot.bent) turning else straight, slot.clueCell)
                    assertTrue(puzzle.isClue[slot.clueCell])
                }
                assertTrue(puzzle.squares.all { it == CrosswordPuzzle.CLUE || it in language.alphabet })
                val clues = puzzle.isClue.count { it }
                assertTrue(clues in 55..100, "$language $number has $clues clue squares")
            }
        }
    }

    @Test
    fun theBoardsDiffer() {
        for (language in Language.entries) {
            val boards = CrosswordBoards.of(language)
            assertEquals(boards.size, boards.toSet().size, "$language has a board twice")
        }
    }

    @Test
    fun theSameSeedAndLanguageMakeTheSameBoardAndOthersDiffer() {
        assertEquals(CrosswordGenerator.generate(7, Language.TR).squares, CrosswordGenerator.generate(7, Language.TR).squares)
        for (language in Language.entries) {
            val boards = (0 until 30).map { CrosswordGenerator.generate(it, language).squares }.toSet()
            assertEquals(30, boards.size, "$language")
        }
        // Any seed gives a board, a negative one too.
        assertNotNull(CrosswordGenerator.generate(-5, Language.EN))
        assertNotNull(CrosswordGenerator.generate(Int.MIN_VALUE, Language.TR))
    }

    @Test
    fun aBoardIsWrittenAsItsNumberAndReadBack() {
        for (language in Language.entries) for (seed in 0 until 20) {
            val puzzle = CrosswordGenerator.generate(seed, language)
            val text = puzzle.encode()
            assertTrue(text.length <= 32)
            val back = assertNotNull(CrosswordPuzzle.decode(language, text))
            assertEquals(puzzle.squares, back.squares)
            assertEquals(puzzle.words, back.words)
            assertEquals(puzzle.clues, back.clues)
        }
    }

    @Test
    fun aBoardWrittenOutInFullIsReadToo() {
        val board = CrosswordBoards.of(Language.EN).first()
        assertNotNull(CrosswordPuzzle.decode(Language.EN, board))
    }

    @Test
    fun aBoardThatBreaksTheRulesIsNotRead() {
        val good = CrosswordBoards.of(Language.EN).first()
        assertNull(CrosswordPuzzle.decode(Language.EN, good.drop(1)), "too short")
        assertNull(CrosswordPuzzle.decode(Language.EN, good.replace('#', 'Ğ')), "letters of another language")
        val firstLetter = good.indexOfFirst { it != '#' }
        assertNull(CrosswordPuzzle.decode(Language.EN, good.substring(0, firstLetter) + "Q" + good.substring(firstLetter + 1)), "a word the bank does not know")
        assertNull(CrosswordPuzzle.decode(Language.EN, "b99999"), "no such board")
        assertNull(CrosswordPuzzle.decode(Language.EN, "bx"))
        assertNull(CrosswordPuzzle.decode(Language.EN, "b-1"))
    }
}
