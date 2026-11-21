package com.mehmtcan.brainscroll.game.codebreaker

enum class CodeStatus { Playing, Won }

/** What a guess did. */
enum class CodeResult {
    /** Nothing changed (not a code of this board, a board that is won). */
    Ignored,

    /** A guess was counted and the code is still hidden. */
    Guessed,

    /** The guess is the code. */
    Solved,
}

/** One guess with what it scored. */
data class CodeGuess(val colors: List<Int>, val feedback: CodeFeedback)

/** The picture of the board for the UI. */
data class CodeState(
    val puzzle: CodePuzzle,
    /** The guesses so far, the oldest first (a game that was saved with a very long list keeps only its last ones: see [CodeGame.restore]). */
    val guesses: List<CodeGuess>,
    /** How many guesses were given in all, the ones the list no longer holds included. */
    val tries: Int,
    val status: CodeStatus,
)

/**
 * The rules of the code breaker, free of UI and time. There is no limit to the tries and no losing: the game ends when the code is
 * guessed, and its result is the number of guesses.
 */
class CodeGame(private val puzzle: CodePuzzle) {
    private val guesses = ArrayList<CodeGuess>()

    /** Guesses given that [guesses] no longer holds (see [restore]). */
    private var earlier = 0
    private var won = false

    val isFinished: Boolean get() = won

    val state: CodeState get() = CodeState(puzzle, guesses.toList(), earlier + guesses.size, if (won) CodeStatus.Won else CodeStatus.Playing)

    fun submit(guess: List<Int>): CodeResult {
        if (won || !puzzle.accepts(guess)) return CodeResult.Ignored
        val feedback = puzzle.feedback(guess)
        guesses += CodeGuess(guess, feedback)
        if (feedback.exact == puzzle.length) {
            won = true
            return CodeResult.Solved
        }
        return CodeResult.Guessed
    }

    /**
     * Puts back what a saved game holds: the last [saved] guesses and [total] tries in all. Returns false if that cannot be a game of this
     * board (a guess that is not a code, one that is the code, fewer tries than guesses): nothing is changed then.
     */
    fun restore(saved: List<List<Int>>, total: Int): Boolean {
        if (total < saved.size) return false
        if (saved.any { !puzzle.accepts(it) || it == puzzle.secret }) return false
        guesses.clear()
        guesses += saved.map { CodeGuess(it, puzzle.feedback(it)) }
        earlier = total - saved.size
        return true
    }
}
