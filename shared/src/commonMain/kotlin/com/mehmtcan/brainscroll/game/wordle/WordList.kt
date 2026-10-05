package com.mehmtcan.brainscroll.game.wordle

/**
 * The words one language needs: [valid] decides which guesses are accepted,
 * [answers] is the pool the endless feed draws its secret words from.
 *
 * All words are stored uppercased with the language's own rules (see [Language.uppercase]).
 */
class WordList(
    val language: Language,
    val wordLength: Int,
    valid: Collection<String>,
    answers: Collection<String>,
) {
    val valid: Set<String> = normalize(valid)
    val answers: List<String> = normalize(answers).toList()

    init {
        require(this.answers.isNotEmpty()) { "answer pool must not be empty" }
        // An answer the player cannot type would make the game unwinnable.
        val missing = this.answers.filterNot { it in this.valid }
        require(missing.isEmpty()) { "answers missing from the valid set: ${missing.take(5)}" }
    }

    operator fun contains(word: String): Boolean = language.uppercase(word) in valid

    private fun normalize(words: Collection<String>): Set<String> =
        words.asSequence()
            .map { language.uppercase(it.trim()) }
            .filter { it.length == wordLength && language.isValidWord(it) }
            .toCollection(LinkedHashSet())

    companion object {
        /** Parses a plain text resource: one word per line, blank lines and `#` comments ignored. */
        fun parseLines(text: String): List<String> =
            text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
    }
}
