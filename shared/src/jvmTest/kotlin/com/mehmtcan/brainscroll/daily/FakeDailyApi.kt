package com.mehmtcan.brainscroll.daily

import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.evaluateGuess
import kotlinx.coroutines.CompletableDeferred

/**
 * A stand-in for the server's three daily functions, following the same rules (supabase/migrations/...daily_puzzle):
 * the fake knows the answer, the app only ever sees colors; the clock starts at start(); an attempt ends on the
 * right word or the sixth guess; starting again changes nothing.
 */
class FakeDailyApi(
    private val answers: Map<Language, String> = mapOf(Language.EN to "CRANE", Language.TR to "KİTAP"),
    var dayIndex: Long = 20_733,
    /** The "server clock". Tests move it to make attempts take time. */
    var clock: Long = 1_000_000L,
) : DailyApi {

    private class Attempt(val startedAt: Long) {
        val guesses = mutableListOf<DailyGuess>()
        var finishedAt: Long? = null
        var status = DailyStatus.Playing
        var resultId: String? = null
    }

    private val attempts = mutableMapOf<Language, Attempt>()
    private var resultCounter = 0

    /** Thrown by every call while set, to simulate being offline. */
    var failure: DailyException? = null

    /** While set, guess() waits for it: lets a test look at the screen while a request is in flight. */
    var gate: CompletableDeferred<Unit>? = null

    val calls = mutableListOf<String>()

    /** Pretends the player already played on another phone. */
    fun prefill(language: Language, words: List<String>, startedAt: Long = clock) {
        val attempt = Attempt(startedAt)
        attempts[language] = attempt
        val answer = answers.getValue(language)
        words.forEach { word ->
            attempt.guesses += DailyGuess(word, evaluateGuess(word, answer))
            finishIfDone(language, attempt, word, answer)
        }
    }

    override suspend fun state(language: Language): DailyState {
        calls += "state"
        failure?.let { throw it }
        return view(language)
    }

    override suspend fun start(language: Language): DailyState {
        calls += "start"
        failure?.let { throw it }
        attempts.getOrPut(language) { Attempt(startedAt = clock) } // asking again changes nothing
        return view(language)
    }

    override suspend fun guess(language: Language, word: String): DailyState {
        calls += "guess:$word"
        gate?.await()
        failure?.let { throw it }
        val attempt = attempts[language] ?: throw DailyException.NotStarted()
        if (attempt.finishedAt != null) throw DailyException.AlreadyFinished()
        val answer = answers.getValue(language)
        val upper = language.uppercase(word)
        if (upper.length != 5 || !language.isValidWord(upper)) throw DailyException.InvalidGuess()
        attempt.guesses += DailyGuess(upper, evaluateGuess(upper, answer))
        finishIfDone(language, attempt, upper, answer)
        return view(language)
    }

    private fun finishIfDone(language: Language, attempt: Attempt, word: String, answer: String) {
        val won = word == answer
        if (won || attempt.guesses.size >= 6) {
            attempt.finishedAt = clock
            attempt.status = if (won) DailyStatus.Won else DailyStatus.Lost
            attempt.resultId = "fake-result-${++resultCounter}-${language.name}"
        }
    }

    private fun view(language: Language): DailyState {
        val attempt = attempts[language]
        val answer = answers.getValue(language)
        return DailyState(
            dayIndex = dayIndex,
            language = language,
            status = attempt?.status ?: DailyStatus.NotStarted,
            maxAttempts = 6,
            guesses = attempt?.guesses?.toList().orEmpty(),
            startedAtMs = attempt?.startedAt,
            finishedAtMs = attempt?.finishedAt,
            durationMs = attempt?.finishedAt?.let { it - attempt.startedAt },
            answer = if (attempt?.finishedAt != null) answer else null, // hidden until it is over
            resultId = attempt?.resultId,
            serverNowMs = clock,
        )
    }
}
