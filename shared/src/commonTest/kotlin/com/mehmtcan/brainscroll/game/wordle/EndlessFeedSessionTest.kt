package com.mehmtcan.brainscroll.game.wordle

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A session ends after 30 minutes without any action: the skip comes back (docs/decisions.md). */
class EndlessFeedSessionTest {

    private val words = listOf("crane", "slate", "ghost", "pound", "apple", "brick")
    private val en = WordList(Language.EN, 5, valid = words, answers = words)
    private val tr = WordList(Language.TR, 5, valid = listOf("çiçek", "kırık", "kitap"), answers = listOf("çiçek", "kırık", "kitap"))
    private var clock = 1_000_000L
    private val minute = 60_000L

    private fun feed(skips: Int = 1) =
        EndlessFeed(mapOf(Language.EN to en, Language.TR to tr), Language.EN, Random(1), skips, now = { clock })

    @Test
    fun theSkipComesBackAfterThirtyMinutesWithoutAnyAction() {
        val f = feed()
        f.ensureSize(3)
        assertTrue(f.leave(0)) // uses the one skip
        assertEquals(0, f.snapshot().skipsLeft)
        assertFalse(f.leave(1), "no skip left in the same session")

        clock += 30 * minute
        assertTrue(f.startNewSessionIfIdle(), "a new session started")
        assertEquals(1, f.snapshot().skipsLeft)
        assertTrue(f.leave(1), "and the skip works again")
    }

    @Test
    fun lessThanThirtyMinutesIsStillTheSameSession() {
        val f = feed()
        f.ensureSize(3)
        f.leave(0)
        clock += 29 * minute
        assertFalse(f.startNewSessionIfIdle())
        assertEquals(0, f.snapshot().skipsLeft)
    }

    @Test
    fun anyActionKeepsTheSessionAlive() {
        val f = feed()
        f.ensureSize(3)
        f.leave(0)
        // Typing every 20 minutes for two hours: never 30 minutes without an action.
        repeat(6) {
            clock += 20 * minute
            f.type(1, 'A')
            f.backspace(1)
        }
        assertEquals(0, f.snapshot().skipsLeft)
        assertFalse(f.leave(1))
    }

    @Test
    fun theFirstActionAfterALongPauseStartsANewSessionByItself() {
        val f = feed()
        f.ensureSize(3)
        f.leave(0)
        clock += 45 * minute
        // Nobody asked: the leave itself notices the pause.
        assertTrue(f.leave(1))
        assertEquals(0, f.snapshot().skipsLeft, "and the fresh skip was used by that leave")
    }

    @Test
    fun aNewSessionKeepsTheStreakAndTheSkippedRounds() {
        val f = feed()
        f.ensureSize(3)
        f.setAnswerStreak(4)
        f.leave(0)
        clock += 40 * minute
        f.startNewSessionIfIdle()
        assertEquals(4, f.snapshot().answerStreak)
        assertTrue(f.snapshot().rounds[0].skipped, "a skipped puzzle stays skipped")
    }

    @Test
    fun noRedrawIsNeededWhenThereWasNothingToRestore() {
        val f = feed()
        f.ensureSize(2)
        clock += 90 * minute
        assertFalse(f.startNewSessionIfIdle(), "the skip was never used, so nothing changed on screen")
        assertEquals(1, f.snapshot().skipsLeft)
    }
}
