package com.mehmtcan.brainscroll.game.wordle

/** Feedback for one letter of a guess. */
enum class LetterResult { Correct, Present, Absent }

/**
 * Compares [guess] with [answer] (same length, already uppercased) and returns one result per letter.
 *
 * Two passes make repeated letters work:
 * 1. Mark exact matches as Correct and remove them from the pool of unmatched answer letters.
 * 2. For the rest, a letter is Present only while the pool still has that letter; each hit uses one up.
 *
 * Example: answer "ABBEY", guess "BABBY" -> Present, Present, Correct, Absent, Correct
 * (the third B is Absent because the answer's two Bs are already used up).
 */
fun evaluateGuess(guess: String, answer: String): List<LetterResult> {
    require(guess.length == answer.length) { "guess and answer must have the same length" }

    val results = MutableList(guess.length) { LetterResult.Absent }
    val unmatched = mutableMapOf<Char, Int>()

    for (i in guess.indices) {
        if (guess[i] == answer[i]) {
            results[i] = LetterResult.Correct
        } else {
            unmatched[answer[i]] = (unmatched[answer[i]] ?: 0) + 1
        }
    }

    for (i in guess.indices) {
        if (results[i] == LetterResult.Correct) continue
        val left = unmatched[guess[i]] ?: 0
        if (left > 0) {
            results[i] = LetterResult.Present
            unmatched[guess[i]] = left - 1
        }
    }

    return results
}
