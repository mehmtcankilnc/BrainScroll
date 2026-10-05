package com.mehmtcan.brainscroll.daily

import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.LetterResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyStateTest {

    // Shaped exactly like what the database functions return (see daily_state_json in the migration).
    private val playing = """
        {"day_index": 20733, "language": "EN", "status": "PLAYING", "max_attempts": 6,
         "guesses": [{"word": "SLATE", "feedback": "AACAC"}, {"word": "BABBY", "feedback": "PPCAC"}],
         "started_at_ms": 1791300000000, "finished_at_ms": null, "duration_ms": null,
         "answer": null, "result_id": null, "server_now_ms": 1791300060000}
    """.trimIndent()

    @Test
    fun aRunningAttemptIsReadWithItsColors() {
        val state = parseDailyState(playing)
        assertEquals(DailyStatus.Playing, state.status)
        assertEquals(Language.EN, state.language)
        assertEquals(20733L, state.dayIndex)
        assertEquals(listOf("SLATE", "BABBY"), state.guesses.map { it.word })
        assertEquals(
            listOf(LetterResult.Present, LetterResult.Present, LetterResult.Correct, LetterResult.Absent, LetterResult.Correct),
            state.guesses[1].results,
        )
        assertFalse(state.isFinished)
        assertNull(state.answer) // the answer is not known while playing
        assertNull(state.durationMs)
    }

    @Test
    fun aFinishedAttemptCarriesTheAnswerTheTimeAndTheResultId() {
        val state = parseDailyState(
            """{"day_index": 5, "language": "TR", "status": "WON", "max_attempts": 6,
               "guesses": [{"word": "KİTAP", "feedback": "CCCCC"}],
               "started_at_ms": 1000, "finished_at_ms": 93500, "duration_ms": 92500,
               "answer": "KİTAP", "result_id": "abc-123", "server_now_ms": 94000}""",
        )
        assertEquals(DailyStatus.Won, state.status)
        assertEquals(Language.TR, state.language)
        assertTrue(state.isFinished)
        assertEquals("KİTAP", state.answer)
        assertEquals(92_500L, state.durationMs)
        assertEquals(93_500L, state.finishedAtMs)
        assertEquals("abc-123", state.resultId)
    }

    @Test
    fun anAttemptThatWasNotStartedHasNothingInIt() {
        val state = parseDailyState(
            """{"day_index": 5, "language": "EN", "status": "NOT_STARTED", "max_attempts": 6, "guesses": [],
               "started_at_ms": null, "finished_at_ms": null, "duration_ms": null, "answer": null,
               "result_id": null, "server_now_ms": 100}""",
        )
        assertEquals(DailyStatus.NotStarted, state.status)
        assertTrue(state.guesses.isEmpty())
        assertNull(state.startedAtMs)
    }

    @Test
    fun aLostAttemptIsFinished() {
        val state = parseDailyState(playing.replace("PLAYING", "LOST").replace("\"answer\": null", "\"answer\": \"CRANE\""))
        assertEquals(DailyStatus.Lost, state.status)
        assertTrue(state.isFinished)
    }

    @Test
    fun fieldsAddedByALaterServerVersionAreIgnored() {
        val state = parseDailyState(playing.replace("\"max_attempts\": 6,", "\"max_attempts\": 6, \"streak\": 4,"))
        assertEquals(DailyStatus.Playing, state.status)
    }

    @Test
    fun anUnknownStatusOrFeedbackLetterIsAnErrorNotASilentGuess() {
        assertFailsWith<IllegalStateException> { parseDailyState(playing.replace("PLAYING", "PAUSED")) }
        assertFailsWith<IllegalStateException> { parseDailyState(playing.replace("AACAC", "AAXAC")) }
    }

    @Test
    fun durationsAreShownAsMinutesAndSeconds() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("00:59", formatDuration(59_999))
        assertEquals("01:23", formatDuration(83_000))
        assertEquals("1:02:05", formatDuration(3_725_000))
        assertEquals("00:00", formatDuration(-5)) // never negative
    }

    @Test
    fun theCountdownAlwaysShowsHours() {
        assertEquals("00:00:00", formatCountdown(0))
        assertEquals("05:07:09", formatCountdown((5 * 3600 + 7 * 60 + 9) * 1000L))
        assertEquals("23:59:59", formatCountdown(86_399_999))
    }
}
