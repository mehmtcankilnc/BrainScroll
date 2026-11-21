package com.mehmtcan.brainscroll.game.codebreaker

/** What one peg of a guess scored, the way the letters of the word puzzle do: right colour in the right place, in the code but elsewhere, or not (left over) in the code. */
enum class CodeMark { Absent, Present, Exact }

/** What a guess scored, a mark for every peg in the order of the guess. */
data class CodeFeedback(val marks: List<CodeMark>) {
    /** How many pegs are the right colour in the right place. */
    val exact: Int get() = marks.count { it == CodeMark.Exact }

    /** How many pegs are a colour that is in the code but not in that place. */
    val misplaced: Int get() = marks.count { it == CodeMark.Present }
}

/**
 * A secret code of colours (the numbers 0 until [colors]), [length] places long. With [repeats] false every colour is in it at most
 * once. The board is saved as text (see [encode]): the number of colours, a `u` (no repeats) or an `r`, and the code as digits.
 */
class CodePuzzle(val colors: Int, val repeats: Boolean, val secret: List<Int>) {
    init {
        require(colors in MIN_COLORS..MAX_COLORS) { "colors $colors" }
        require(secret.size in MIN_LENGTH..MAX_LENGTH) { "length ${secret.size}" }
        require(secret.all { it in 0 until colors }) { "a colour is not one of $colors" }
        require(repeats || secret.toSet().size == secret.size) { "a colour twice without repeats" }
        require(repeats || colors >= secret.size) { "not enough colours" }
    }

    val length: Int get() = secret.size

    /** Whether [guess] is something a player can enter on this board: the right length and colours (no repeats if the board has none). */
    fun accepts(guess: List<Int>): Boolean =
        guess.size == length && guess.all { it in 0 until colors } && (repeats || guess.toSet().size == guess.size)

    fun feedback(guess: List<Int>): CodeFeedback = feedback(secret, guess)

    fun encode(): String = "$colors${if (repeats) 'r' else 'u'}" + secret.joinToString("")

    override fun equals(other: Any?) = other is CodePuzzle && colors == other.colors && repeats == other.repeats && secret == other.secret

    override fun hashCode() = 31 * (31 * colors + repeats.hashCode()) + secret.hashCode()

    companion object {
        const val MIN_COLORS = 2
        const val MAX_COLORS = 8
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 6

        /** Reads what [encode] wrote. Null if it is not a board (so a saved game of another kind starts a new one). */
        fun decode(text: String): CodePuzzle? {
            if (text.length < 2 + MIN_LENGTH) return null
            val colors = text[0].digitToIntOrNull() ?: return null
            val repeats = when (text[1]) {
                'r' -> true
                'u' -> false
                else -> return null
            }
            val secret = text.substring(2).map { it.digitToIntOrNull() ?: return null }
            return runCatching { CodePuzzle(colors, repeats, secret) }.getOrNull()
        }

        /**
         * The score of [guess] against [secret], peg by peg, the way the word puzzle scores a word: the pegs in the right place first,
         * then a peg is "present" while the code still has a colour of its own left over for it (a colour is counted once for every
         * time the code holds it, however often the guess has it).
         */
        fun feedback(secret: List<Int>, guess: List<Int>): CodeFeedback {
            val marks = Array(secret.size) { CodeMark.Absent }
            val left = IntArray(MAX_COLORS)
            for (i in secret.indices) {
                if (secret[i] == guess[i]) marks[i] = CodeMark.Exact else left[secret[i]]++
            }
            for (i in secret.indices) {
                if (marks[i] != CodeMark.Exact && left[guess[i]] > 0) {
                    marks[i] = CodeMark.Present
                    left[guess[i]]--
                }
            }
            return CodeFeedback(marks.toList())
        }

        /** Every code of a board, in order (the small boards only: at most 8 to the 6th). */
        fun allCodes(colors: Int, length: Int, repeats: Boolean): List<List<Int>> {
            val out = ArrayList<List<Int>>()
            fun fill(prefix: IntArray, at: Int) {
                if (at == length) {
                    out += prefix.toList()
                    return
                }
                for (c in 0 until colors) {
                    if (!repeats && (0 until at).any { prefix[it] == c }) continue
                    prefix[at] = c
                    fill(prefix, at + 1)
                }
            }
            fill(IntArray(length), 0)
            return out
        }
    }
}
