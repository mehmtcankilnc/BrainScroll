package com.mehmtcan.brainscroll.game.wordle

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.WordGuide
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot

/** Immutable picture of one round for the UI. The UI never touches the mutable [WordleRound]. */
data class RoundSnapshot(
    /** Stable id of the puzzle; also the id of its saved result (favorites use it). */
    override val id: String,
    override val index: Int,
    val language: Language,
    val wordLength: Int,
    val maxAttempts: Int,
    val rows: List<GuessRow>,
    /** Letters typed for the current (not yet submitted) row. */
    val input: String,
    val status: WordleStatus,
    /** Only known once the round is over. */
    val answer: String?,
    val error: GuessError?,
    /** Increases on every rejected guess, so the UI can react to the same error twice in a row. */
    val errorTick: Int,
    /** Best known result per keyboard letter: Correct beats Present beats Absent. */
    val letterStates: Map<Char, LetterResult>,
    /** A guess that was typed and sent but not answered yet (daily puzzle: the server colors it). Drawn as a row. */
    val pending: String? = null,
) : PageSnapshot {
    override val isFinished: Boolean get() = status != WordleStatus.Playing
    override val isWon: Boolean get() = status == WordleStatus.Won
    override val kind: GameKind get() = GameKind.Word
}

/**
 * One word puzzle in the feed: a [WordleGame] plus the letters the player is currently typing.
 * Mutable on purpose (it is the single owner of its state); [snapshot] hands out immutable copies.
 */
class WordleRound(
    override val index: Int,
    private val game: WordleGame,
    private val wordList: WordList,
    override val id: String = "round-$index",
) : FeedRound {
    val language = wordList.language
    private var input = ""
    private var error: GuessError? = null
    private var errorTick = 0

    override val kind: GameKind get() = GameKind.Word

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val answer = game.secretAnswer
        val state = game.state
        val rows = state.rows.map { row -> row.results }
        return { WordGuide.step(answer, rows, state.status) }
    }



    /** True while nothing has been typed or guessed, so the round can still be swapped for another one. */
    override val isUntouched: Boolean get() = input.isEmpty() && game.state.rows.isEmpty()

    override val isFinished: Boolean get() = game.isFinished

    fun type(letter: Char) {
        if (isFinished || input.length >= wordList.wordLength) return
        val upper = language.uppercase(letter.toString())
        if (upper.length == 1 && upper[0] in language.alphabet) {
            input += upper
            error = null
        }
    }

    fun backspace() {
        if (isFinished || input.isEmpty()) return
        input = input.dropLast(1)
        error = null
    }

    /** Submits the typed word. A rejected word keeps the letters so the player can fix them. */
    fun submit() {
        if (isFinished) return
        game.perform(WordleAction.Submit(input))
        error = game.lastError
        if (error == null) {
            input = ""
        } else {
            errorTick++
        }
    }

    /** What has to be saved to rebuild this round later. */
    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = game.secretAnswer,
        guesses = game.state.rows.map { it.word },
        input = input,
        position = index,
        assisted = assisted,
    )

    override fun toFinished(finishedAt: Long) = toFinished(finishedAt, Mode.ENDLESS)

    fun toFinished(finishedAt: Long, mode: Mode) = FinishedRound(
        id = id,
        mode = mode,
        language = language,
        answer = game.secretAnswer,
        guesses = game.state.rows.map { it.word },
        outcome = if (game.state.status == WordleStatus.Won) Outcome.WON else Outcome.LOST,
        assisted = assisted,
        finishedAt = finishedAt,
    )

    override fun snapshot(): RoundSnapshot {
        val state = game.state
        return RoundSnapshot(
            id = id,
            index = index,
            language = language,
            wordLength = wordList.wordLength,
            maxAttempts = state.maxAttempts,
            rows = state.rows,
            input = input,
            status = state.status,
            answer = state.answer,
            error = error,
            errorTick = errorTick,
            letterStates = keyStatesOf(state.rows),
        )
    }

    companion object {
        /** Rebuilds a round from storage by replaying its guesses and typing its pending letters again. */
        fun restore(index: Int, stored: StoredRound, wordList: WordList): WordleRound {
            val round = WordleRound(index, WordleGame(wordList, stored.answer), wordList, stored.id)
            stored.guesses.forEach { round.game.perform(WordleAction.Submit(it)) }
            stored.input.forEach { round.type(it) }
            round.assisted = stored.assisted
            return round
        }
    }
}

/** Best known result per keyboard letter: Correct beats Present beats Absent. Shared with the daily puzzle. */
fun keyStatesOf(rows: List<GuessRow>): Map<Char, LetterResult> {
    val best = mutableMapOf<Char, LetterResult>()
    for (row in rows) {
        row.word.forEachIndexed { i, letter ->
            val result = row.results[i]
            val current = best[letter]
            // LetterResult is declared Correct, Present, Absent: a smaller ordinal is a better result.
            if (current == null || result.ordinal < current.ordinal) best[letter] = result
        }
    }
    return best
}
