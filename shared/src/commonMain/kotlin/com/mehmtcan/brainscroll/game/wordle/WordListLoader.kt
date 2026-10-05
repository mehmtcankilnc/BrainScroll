package com.mehmtcan.brainscroll.game.wordle

import brainscroll.shared.generated.resources.Res

private const val WORD_LENGTH = 5

/**
 * Reads the bundled word files for [language] from Compose resources (`composeResources/files/words`).
 * `suspend` because resource reads are asynchronous on some platforms; call it from a coroutine.
 */
suspend fun loadWordList(language: Language): WordList {
    val code = when (language) {
        Language.EN -> "en"
        Language.TR -> "tr"
    }
    val valid = WordList.parseLines(Res.readBytes("files/words/${code}_valid.txt").decodeToString())
    val answers = WordList.parseLines(Res.readBytes("files/words/${code}_answers.txt").decodeToString())
    return WordList(language, WORD_LENGTH, valid, answers)
}
