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

    private fun feed(random: Random = Random(1)) =
        EndlessFeed(mapOf(Language.EN to en, Language.TR to tr), Language.EN, random)

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
    fun aPuzzleDoneWithTheGuideIsNeutralForTheStreak() {
        val f = feed()
        f.ensureSize(4)
        f.markGuided(0)
        f.win(0)
        assertEquals(0, f.answerStreak, "a guided win does not add")
        f.win(1)
        assertEquals(1, f.answerStreak)
        f.markGuided(2)
        f.lose(2)
        assertEquals(1, f.answerStreak, "a guided loss does not break it either")
    }

    @Test
    fun theGuideHasAStepForAWordPuzzleThatIsNotOver() {
        val f = feed()
        f.ensureSize(1)
        val step = f.guideTask(0)!!.invoke()
        assertEquals(com.mehmtcan.brainscroll.game.guide.GuideReason.WordLetter, step?.reason)
        f.win(0)
        assertEquals(null, f.guideTask(0), "a puzzle that is over has no guide")
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
    fun partialProgressIsKeptInTheSnapshot() {
        val f = feed()
        f.ensureSize(2)
        f.type(0, 'c')
        f.type(0, 'r')
        assertEquals("CR", f.snapshot().rounds[0].input)
    }

    @Test
    fun theWordLanguageCanBeChangedForTheUntouchedPages() {
        val f = feed()
        f.ensureSize(3)
        f.setWordLanguage(Language.TR, fromIndex = 0)
        assertEquals(Language.TR, f.language)
        assertTrue(f.snapshot().rounds.all { it.language == Language.TR })
    }

    @Test
    fun newRoundsUseTheCurrentLanguage() {
        val f = feed()
        f.setWordLanguage(Language.TR, fromIndex = 0)
        f.ensureSize(2)
        assertTrue(f.snapshot().rounds.all { it.language == Language.TR })
    }

    @Test
    fun aStartedPuzzleKeepsItsLanguage() {
        val f = feed()
        f.ensureSize(3)
        f.type(0, 'c')
        f.setWordLanguage(Language.TR, fromIndex = 0)
        assertEquals(Language.EN, f.snapshot().rounds[0].language)
        assertEquals(Language.TR, f.snapshot().rounds[1].language)
    }

    @Test
    fun withTwoLanguagesPuzzlesComeInBothOfThem() {
        val f = EndlessFeed(mapOf(Language.EN to en, Language.TR to tr), Language.EN, Random(1), startLanguages = listOf(Language.EN, Language.TR))
        f.ensureSize(30)
        assertEquals(setOf(Language.EN, Language.TR), f.snapshot().rounds.map { it.language }.toSet())
    }

    @Test
    fun turningALanguageOffRedrawsTheUntouchedPagesInIt() {
        val f = EndlessFeed(mapOf(Language.EN to en, Language.TR to tr), Language.EN, Random(1), startLanguages = listOf(Language.EN, Language.TR))
        f.ensureSize(30)
        f.setWordLanguages(setOf(Language.TR), fromIndex = 0)
        assertTrue(f.snapshot().rounds.all { it.language == Language.TR })
    }

    @Test
    fun theLastLanguageCannotBeTurnedOff() {
        val f = feed()
        f.setWordLanguages(emptySet(), fromIndex = 0)
        assertEquals(listOf(Language.EN), f.wordLanguages)
    }
}
