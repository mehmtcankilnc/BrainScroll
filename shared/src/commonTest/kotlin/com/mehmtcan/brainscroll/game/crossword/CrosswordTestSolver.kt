package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.wordle.Language

/**
 * Finds the letters of a board from what a player sees (the clue squares and their texts), the way a player does: each clue has the
 * words of the bank that carry that text and have that length; where two of them are possible, the crossings decide. For tests.
 */
object CrosswordTestSolver {

    /** The right letter of every square (a space for a clue square), or null if the clues do not lead to one answer. */
    fun solve(language: Language, isClue: List<Boolean>, clues: List<CrosswordClue>): CharArray? {
        val options = clues.map { clue -> CrosswordBank.entries(language).filter { it.clue == clue.text && it.word.length == clue.cells.size }.map { it.word } }
        if (options.any { it.isEmpty() }) return null
        val letters = CharArray(CrosswordLayout.CELLS) { ' ' }
        fun search(i: Int): Boolean {
            if (i == clues.size) return true
            for (word in options[i]) {
                val before = clues[i].cells.map { letters[it] }
                if (clues[i].cells.indices.any { letters[clues[i].cells[it]] != ' ' && letters[clues[i].cells[it]] != word[it] }) continue
                clues[i].cells.forEachIndexed { k, cell -> letters[cell] = word[k] }
                if (search(i + 1)) return true
                clues[i].cells.forEachIndexed { k, cell -> letters[cell] = before[k] }
            }
            return false
        }
        return if (search(0)) letters else null
    }
}
