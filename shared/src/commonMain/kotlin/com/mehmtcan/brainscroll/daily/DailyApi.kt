package com.mehmtcan.brainscroll.daily

import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.LetterResult

enum class DailyStatus { NotStarted, Playing, Won, Lost }

/** One guess as the server judged it: the word and the color of each letter. */
data class DailyGuess(val word: String, val results: List<LetterResult>)

/**
 * One player's attempt at today's puzzle, as the server reports it. The app never knows the answer while the
 * attempt is running: [answer] is null until the attempt is over.
 */
data class DailyState(
    val dayIndex: Long,
    val language: Language,
    val status: DailyStatus,
    val maxAttempts: Int,
    val guesses: List<DailyGuess>,
    val startedAtMs: Long?,
    val finishedAtMs: Long?,
    /** How long the attempt took, measured by the server's clock. Only known when it is over. */
    val durationMs: Long?,
    val answer: String?,
    /** The id of the saved result (the server writes it when the attempt ends). */
    val resultId: String?,
    /** The server's clock when this was sent. The countdown to the next puzzle is based on it, not on the phone's. */
    val serverNowMs: Long,
) {
    val isFinished: Boolean get() = status == DailyStatus.Won || status == DailyStatus.Lost
}

/** What can go wrong when talking to the daily puzzle. The daily puzzle needs a connection (docs/decisions.md). */
sealed class DailyException(message: String) : Exception(message) {
    /** No network or no answer in time. */
    class Offline : DailyException("offline")

    /** The server answered with an error, or there is no session yet. */
    class Unavailable : DailyException("unavailable")

    class AlreadyFinished : DailyException("already finished")
    class NotStarted : DailyException("not started")
    class InvalidGuess : DailyException("invalid guess")
}

/**
 * The daily puzzle on the server. Three operations, nothing else: the answer, the coloring and the clock all
 * live on the server. Implementations throw [DailyException].
 */
interface DailyApi {
    /** Today's attempt without starting it (shows "Start", "Continue" or the result). Does not start the clock. */
    suspend fun state(language: Language): DailyState

    /** Starts today's puzzle and its clock. Asking again changes nothing, it returns the current state. */
    suspend fun start(language: Language): DailyState

    /** Sends one guess and gets it back colored. */
    suspend fun guess(language: Language, word: String): DailyState
}
