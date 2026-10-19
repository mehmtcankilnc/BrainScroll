package com.mehmtcan.brainscroll.game.wordle

import com.mehmtcan.brainscroll.game.Game

enum class WordleStatus { Playing, Won, Lost }

/** One accepted guess and its per-letter feedback. */
data class GuessRow(val word: String, val results: List<LetterResult>)

/** Immutable snapshot the UI renders. [answer] is only revealed once the game is over. */
data class WordleState(
    val rows: List<GuessRow>,
    val status: WordleStatus,
    val maxAttempts: Int,
    val answer: String?,
)

/** What the player can do. The UI sends a full word when the player presses Enter. */
sealed interface WordleAction {
    data class Submit(val guess: String) : WordleAction
}

/** Why a guess was rejected. Rejected guesses do not use up an attempt. */
enum class GuessError {
    GameOver,
    WrongLength,
    InvalidLetters,
    NotInDictionary,

    /** Hard mode: the guess leaves out a hint the earlier guesses gave (a green letter moved, or a yellow letter missing). */
    HardMode,
}

class WordleGame(
    private val wordList: WordList,
    answer: String,
    private val maxAttempts: Int = 6,
    /** Hard mode: every hint found so far has to be used in the next guess, as in the original game's hard mode. */
    private val hardMode: Boolean = false,
) : Game<WordleState, WordleAction> {

    private val language = wordList.language
    private val answer = language.uppercase(answer.trim())

    /** The secret word. Internal: only the feed stores it, the UI gets it from [WordleState] after the game ends. */
    internal val secretAnswer: String get() = answer
    private val rows = mutableListOf<GuessRow>()
    private var status = WordleStatus.Playing

    /** Set by [perform] when the last action was rejected, null when it was accepted. */
    var lastError: GuessError? = null
        private set

    init {
        require(this.answer.length == wordList.wordLength && language.isValidWord(this.answer)) {
            "answer must be ${wordList.wordLength} letters of ${language.name}"
        }
    }

    override val isFinished: Boolean get() = status != WordleStatus.Playing

    override val state: WordleState
        get() = WordleState(
            rows = rows.toList(),
            status = status,
            maxAttempts = maxAttempts,
            answer = if (isFinished) answer else null,
        )

    override fun perform(action: WordleAction): WordleState {
        when (action) {
            is WordleAction.Submit -> submit(action.guess)
        }
        return state
    }

    /** True if [guess] keeps every green letter where it was and holds every yellow letter as often as the earlier guesses showed it. */
    private fun usesTheHints(guess: String): Boolean {
        for (row in rows) {
            for (i in row.word.indices) if (row.results[i] == LetterResult.Correct && guess[i] != row.word[i]) return false
            for (letter in row.word.toSet()) {
                val shown = row.word.indices.count { row.word[it] == letter && row.results[it] != LetterResult.Absent }
                if (guess.count { it == letter } < shown) return false
            }
        }
        return true
    }

    private fun submit(rawGuess: String) {
        val guess = language.uppercase(rawGuess.trim())
        lastError = when {
            isFinished -> GuessError.GameOver
            guess.length != wordList.wordLength -> GuessError.WrongLength
            !language.isValidWord(guess) -> GuessError.InvalidLetters
            guess !in wordList.valid -> GuessError.NotInDictionary
            hardMode && !usesTheHints(guess) -> GuessError.HardMode
            else -> null
        }
        if (lastError != null) return

        rows += GuessRow(guess, evaluateGuess(guess, answer))
        status = when {
            guess == answer -> WordleStatus.Won
            rows.size >= maxAttempts -> WordleStatus.Lost
            else -> WordleStatus.Playing
        }
    }
}
