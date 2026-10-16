package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.wordle.Language

/**
 * Picks the boards of the crossword (docs/yeni-oyunlar-plan.md). A board of 16 by 16 squares is too big to search for on the phone,
 * so the boards are made ahead of time by `tools/crossword/Gen.java` and kept in [CrosswordBoards]; here a seed only chooses one of
 * them. The same seed and language always give the same board.
 */
object CrosswordGenerator {
    fun generate(seed: Int, language: Language): CrosswordPuzzle = CrosswordPuzzle.ofBoard(language, seed)
}
