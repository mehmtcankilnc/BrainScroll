package com.mehmtcan.brainscroll.game.wordle

/**
 * Letter rules per language.
 *
 * Why not just `String.uppercase()`? Kotlin's `uppercase()` is locale-independent, so it turns
 * both "i" and "ı" into "I". In Turkish that merges two different letters (i/İ and ı/I), which
 * would corrupt guesses. We map case by hand instead, so behavior is identical on every platform.
 */
enum class Language(val alphabet: String, private val upperPairs: Map<Char, Char>) {
    EN(
        alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
        upperPairs = ('a'..'z').associateWith { it.uppercaseChar() },
    ),
    TR(
        alphabet = "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ",
        upperPairs = ('a'..'z').associateWith { it.uppercaseChar() } +
            mapOf('ç' to 'Ç', 'ğ' to 'Ğ', 'ı' to 'I', 'i' to 'İ', 'ö' to 'Ö', 'ş' to 'Ş', 'ü' to 'Ü'),
    );

    /** Uppercases [text] using this language's rules. Characters outside the map stay unchanged. */
    fun uppercase(text: String): String =
        buildString(text.length) { text.forEach { append(upperPairs[it] ?: it) } }

    /** True if every character of [word] (already uppercased) belongs to this language's alphabet. */
    fun isValidWord(word: String): Boolean = word.all { it in alphabet }
}
