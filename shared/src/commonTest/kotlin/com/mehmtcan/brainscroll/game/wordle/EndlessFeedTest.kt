package com.mehmtcan.brainscroll.game.wordle

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EndlessFeedTest {

    private val words = listOf("crane", "slate", "ghost", "pound", "apple", "brick")

    private val en = WordList(Language.EN, 5, valid = words, answers = words)
    private val tr = WordList(Language.TR, 5, valid = listOf("çiçek", "kırık", "kitap"), answers = listOf("çiçek", "kırık", "kitap"))

    private fun feed(random: Random = Random(1), skips: Int = 1) =
        EndlessFeed(mapOf(Language.EN to en, Language.TR to tr), Language.EN, random, skips)

    /** Types [word] into round [index] and submits it. */
    private fun EndlessFeed.guess(index: Int, word: String) {
        word.forEach { type(index, it) }
        submit(index)
    }

    /** Wins round [index]: the answer is found by trying every word until the round finishes. */
    private fun EndlessFeed.win(index: Int) {
        for (w in words) {
            if (snapshot().rounds[index].isFinished) return
            guess(index, w)
        }
    }

    /** Loses round [index] by submitting six words that are all wrong (answers are never repeated within a round). */
    private fun EndlessFeed.lose(index: Int) {
        val answer = revealAnswer(index)
        val wrong = words.filter { it.uppercase() != answer }
        repeat(6) { guess(index, wrong[it % wrong.size]) }
    }

    /** Finds a round's answer without breaking the feed rules: play a copy built from the same seed. */
    private fun EndlessFeed.revealAnswer(index: Int): String {
        val copy = feed()
        copy.ensureSize(size)
        copy.win(index)
        return copy.snapshot().rounds[index].answer!!
    }

    @Test
    fun ensureSizeCreatesRounds() {
        val f = feed()
        f.ensureSize(3)
        assertEquals(3, f.size)
        f.ensureSize(2) // never shrinks
        assertEquals(3, f.size)
    }

    @Test
    fun answersDoNotRepeatUntilThePoolIsUsed() {
        val f = feed()
        f.ensureSize(words.size)
        f.snapshot().rounds.forEach { f.win(it.index) }
        val answers = f.snapshot().rounds.map { it.answer }
        assertEquals(words.size, answers.toSet().size)
    }

    @Test
    fun winningRaisesTheAnswerStreak() {
        val f = feed()
        f.ensureSize(2)
        f.win(0)
        f.win(1)
        assertEquals(2, f.answerStreak)
    }

    @Test
    fun losingResetsTheAnswerStreak() {
        val f = feed()
        f.ensureSize(3)
        f.win(0)
        f.win(1)
        f.lose(2)
        assertEquals(0, f.answerStreak)
    }

    @Test
    fun aFinishedRoundIsCountedOnlyOnce() {
        val f = feed()
        f.ensureSize(1)
        f.win(0)
        f.submit(0)
        f.submit(0)
        assertEquals(1, f.answerStreak)
    }

    @Test
    fun leavingAnUnfinishedRoundUsesTheOnlySkip() {
        val f = feed()
        f.ensureSize(3)
        assertTrue(f.leave(0))
        assertEquals(0, f.snapshot().skipsLeft)
        assertTrue(f.snapshot().rounds[0].skipped)
        assertFalse(f.canLeave(1))
        assertFalse(f.leave(1))
    }

    @Test
    fun skippingDoesNotBreakTheStreak() {
        val f = feed()
        f.ensureSize(3)
        f.win(0)
        f.leave(1)
        f.win(2)
        assertEquals(2, f.answerStreak)
    }

    @Test
    fun leavingAFinishedRoundIsFreeAndCannotBeBlocked() {
        val f = feed()
        f.ensureSize(2)
        f.win(0)
        assertTrue(f.leave(0))
        assertEquals(1, f.snapshot().skipsLeft)
    }

    @Test
    fun aSkippedRoundCanBeLeftAgainWithoutPayingTwice() {
        val f = feed()
        f.ensureSize(2)
        f.leave(0)
        assertTrue(f.canLeave(0))
        assertTrue(f.leave(0))
        assertEquals(0, f.snapshot().skipsLeft)
    }

    @Test
    fun aSkippedRoundCanBeResumedAndCounts() {
        val f = feed()
        f.ensureSize(2)
        f.leave(0)
        f.win(0)
        assertEquals(1, f.answerStreak)
    }

    @Test
    fun partialProgressIsKeptInTheSnapshot() {
        val f = feed()
        f.ensureSize(2)
        f.type(0, 'c')
        f.type(0, 'r')
        assertEquals("CR", f.snapshot().rounds[0].input)
    }

    @Test
    fun languageCanBeChosenBeforeTheFeedStarts() {
        val f = feed()
        f.ensureSize(3)
        assertTrue(f.snapshot().canChangeLanguage)
        assertTrue(f.setLanguage(Language.TR))
        assertEquals(Language.TR, f.language)
        assertTrue(f.snapshot().rounds.all { it.language == Language.TR })
    }

    @Test
    fun newRoundsUseTheCurrentLanguage() {
        val f = feed()
        f.setLanguage(Language.TR)
        f.ensureSize(2)
        assertTrue(f.snapshot().rounds.all { it.language == Language.TR })
    }

    @Test
    fun typingALetterLocksTheLanguage() {
        val f = feed()
        f.ensureSize(2)
        f.type(0, 'c')
        assertFalse(f.snapshot().canChangeLanguage)
        assertFalse(f.setLanguage(Language.TR))
        assertEquals(Language.EN, f.language)
        assertTrue(f.snapshot().rounds.all { it.language == Language.EN })
    }

    @Test
    fun skippingLocksTheLanguage() {
        val f = feed()
        f.ensureSize(2)
        f.leave(0)
        assertFalse(f.setLanguage(Language.TR))
    }

    @Test
    fun deletingTheTypedLettersDoesNotUnlockTheLanguageOnceAGuessWasMade() {
        val f = feed()
        f.ensureSize(2)
        f.guess(0, "crane")
        assertFalse(f.setLanguage(Language.TR))
    }

    @Test
    fun streakAndSkipsAreNotTouchedByAnAllowedLanguageChange() {
        val f = feed()
        f.ensureSize(2)
        f.setLanguage(Language.TR)
        assertEquals(0, f.answerStreak)
        assertEquals(1, f.skipsLeft)
    }

    @Test
    fun noSkipsMeansLeavingAnUnfinishedRoundIsBlocked() {
        val f = feed(skips = 0)
        f.ensureSize(2)
        assertFalse(f.canLeave(0))
        assertFalse(f.leave(0))
    }
}
