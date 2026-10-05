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
enum class GuessError { GameOver, WrongLength, InvalidLetters, NotInDictionary }

class WordleGame(
    private val wordList: WordList,
    answer: String,
    private val maxAttempts: Int = 6,
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

    private fun submit(rawGuess: String) {
        val guess = language.uppercase(rawGuess.trim())
        lastError = when {
            isFinished -> GuessError.GameOver
            guess.length != wordList.wordLength -> GuessError.WrongLength
            !language.isValidWord(guess) -> GuessError.InvalidLetters
            guess !in wordList.valid -> GuessError.NotInDictionary
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
