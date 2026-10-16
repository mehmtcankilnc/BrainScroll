package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.wordle.Language

/**
 * The words of the crossword puzzle and their clues, written for this game (docs/yeni-oyunlar-plan.md). A clue is short, because
 * it has to fit in a square of the board, and has one right answer. Every answer has two to eight letters.
 *
 * Words are written in capitals of their language (Turkish has the dotted İ and the dotless I). A word has one clue; the boards are
 * made of words of this bank (`tools/crossword/Gen.java`), and the clue is found again from the word, so a word that is in a board
 * has to stay in the bank. The text itself lives in [CrosswordBankData], made from the files of `tools/crossword/bank`.
 */
object CrosswordBank {

    class Entry(val word: String, val clue: String)

    /** All the entries of a language. */
    fun entries(language: Language): List<Entry> = when (language) {
        Language.TR -> TR_ENTRIES
        Language.EN -> EN_ENTRIES
    }

    /** The clue of [word], or null if the bank does not have it. */
    fun clueOf(language: Language, word: String): String? = index(language)[word]

    private fun index(language: Language): Map<String, String> = when (language) {
        Language.TR -> TR_INDEX
        Language.EN -> EN_INDEX
    }

    private fun parse(pieces: List<String>): List<Entry> = pieces.flatMap { it.lines() }.map { it.trim() }.filter { it.isNotEmpty() }.map {
        val (word, clue) = it.split('|', limit = 2)
        Entry(word.trim(), clue.trim())
    }

    private val TR_ENTRIES: List<Entry> by lazy { parse(CrosswordBankData.TR) }
    private val EN_ENTRIES: List<Entry> by lazy { parse(CrosswordBankData.EN) }
    private val TR_INDEX: Map<String, String> by lazy { TR_ENTRIES.associate { it.word to it.clue } }
    private val EN_INDEX: Map<String, String> by lazy { EN_ENTRIES.associate { it.word to it.clue } }
}
