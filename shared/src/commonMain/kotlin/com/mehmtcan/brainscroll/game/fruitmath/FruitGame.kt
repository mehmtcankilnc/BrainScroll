package com.mehmtcan.brainscroll.game.fruitmath

enum class FruitStatus { Playing, Won }

/** What an answer did. */
enum class FruitResult {
    /** Nothing changed (a number that cannot be an answer, a board that is won). */
    Ignored,

    /** The answer is wrong. */
    Wrong,

    /** The answer is right. */
    Solved,
}

/** The picture of the board for the UI. */
data class FruitState(
    val puzzle: FruitPuzzle,
    /** How many wrong answers were given. */
    val mistakes: Int,
    val status: FruitStatus,
)

/**
 * The rules of the fruit equations, free of UI and time. An answer is right (the board is won) or wrong (it is counted, and the player
 * may try again). There is no losing.
 */
class FruitGame(private val puzzle: FruitPuzzle) {
    private var mistakes = 0
    private var won = false

    val isFinished: Boolean get() = won

    val state: FruitState get() = FruitState(puzzle, mistakes, if (won) FruitStatus.Won else FruitStatus.Playing)

    /** Gives [answer] for the last line. */
    fun submit(answer: Int): FruitResult {
        if (won || answer !in 0..FruitPuzzle.MAX_ANSWER) return FruitResult.Ignored
        if (answer == puzzle.answer) {
            won = true
            return FruitResult.Solved
        }
        mistakes++
        return FruitResult.Wrong
    }

    /** Puts back the number of wrong answers of a saved game. Returns false if it cannot be one (below 0): nothing is changed then. */
    fun restore(savedMistakes: Int): Boolean {
        if (savedMistakes < 0) return false
        mistakes = savedMistakes
        return true
    }
}
