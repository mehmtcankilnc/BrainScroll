package com.mehmtcan.brainscroll.game.topicgroups

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TopicGroupsRoundTest {

    private var clock = 1_000L

    private val puzzle = TopicGroupsGenerator.generate(4, Language.TR, Difficulty.Medium)

    private fun round() = TopicGroupsRound(0, "g1", puzzle, { clock }).also { it.onShown() }

    private fun group(g: Int) = (g * 4 until g * 4 + 4).toList()

    private fun TopicGroupsRound.pick(words: List<Int>, stepMillis: Long = 1_000) {
        for (w in words) {
            clock += stepMillis
            tap(w)
        }
    }

    private fun TopicGroupsRound.finish(stepMillis: Long = 1_000) {
        for (g in 0 until 3) pick(group(g), stepMillis)
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstTap() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.TopicGroups, r.kind)
        assertEquals(Language.TR, r.language)
        r.tap(0)
        assertTrue(!r.isUntouched)
    }

    @Test
    fun aTapThatDoesNothingChangesNothingNotEvenTheClock() {
        val r = round()
        r.tap(0)
        clock += 4_000
        assertEquals(TopicGroupsResult.Ignored, r.tap(99))
        clock += 3_000
        r.tap(1)
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(2, r.toStored().guesses.size)
    }

    @Test
    fun theTimeIsTheTimeThePageWasOnShowAndThinkingIsNotCapped() {
        val r = round()
        r.tap(0)
        clock += 5_000
        r.tap(1)
        clock += 100_000
        r.tap(2)
        assertEquals(5_000 + 100_000L, r.snapshot().durationMs)
    }

    @Test
    fun theBoardIsSavedAsItsEncodingAndTheActions() {
        val r = round()
        r.pick(listOf(0, 1))
        r.shuffle()
        r.clearSelection()
        val stored = r.toStored()
        assertEquals(puzzle.encode(), stored.answer)
        assertEquals(Language.TR, stored.language)
        assertEquals(GameKind.TopicGroups, stored.game)
        assertEquals(4, stored.guesses.size)
        assertEquals(listOf("t0", "t1"), stored.guesses.take(2))
        assertTrue(stored.guesses[2].startsWith("s"))
        assertEquals("c", stored.guesses[3])
    }

    @Test
    fun aRestoredRoundIsExactlyAsItWasLeft() {
        val r = round()
        r.pick(group(2))
        r.pick(listOf(0, 1, 2, 4))
        r.shuffle()
        r.tap(4)
        val back = assertNotNull(TopicGroupsRound.restore(3, r.toStored(), { clock }))
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(3, back.index)
        assertEquals(r.toStored().guesses, back.toStored().guesses)
        assertEquals(1, back.snapshot().state.mistakes)
    }

    @Test
    fun aRestoredRoundKeepsTheGuideMark() {
        val r = round()
        r.tap(0)
        r.markAssisted()
        val back = assertNotNull(TopicGroupsRound.restore(0, r.toStored(), { clock }))
        assertTrue(back.assisted)
    }

    @Test
    fun savedTextThatCannotBeReadMeansNoRound() {
        val good = round().also { it.tap(0) }.toStored()
        assertNull(TopicGroupsRound.restore(0, good.copy(answer = "nonsense"), { clock }))
        assertNull(TopicGroupsRound.restore(0, good.copy(guesses = listOf("t0", "zz")), { clock }))
        assertNull(TopicGroupsRound.restore(0, good.copy(guesses = listOf("tx")), { clock }))
    }

    @Test
    fun finishingGivesAWinWithTheMistakesAsItsOnlyGuess() {
        val r = round()
        r.pick(listOf(0, 1, 2, 4))
        r.pick(listOf(4))
        r.clearSelection()
        r.finish()
        assertTrue(r.isFinished)
        val done = r.toFinished(finishedAt = 50_000)
        assertEquals(Outcome.WON, done.outcome)
        assertEquals(Mode.ENDLESS, done.mode)
        assertEquals(GameKind.TopicGroups, done.game)
        assertEquals(listOf("1"), done.guesses)
        assertEquals(1, done.moveCount)
        assertEquals(Language.TR, done.language)
        assertTrue(done.answer.length <= 32)
    }

    @Test
    fun theGuidePointsAtTheFourWordsOfAGroupAndNamesIt() {
        val r = round()
        val step = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.TopicGroupsGroup, step.reason)
        assertEquals(4, step.cells.size)
        assertEquals(1, step.cells.map { it / 4 }.distinct().size)
        assertEquals(puzzle.groups[step.cells.first() / 4].title, step.text)
        r.pick(group(0))
        r.pick(group(1))
        r.pick(group(2))
        assertNull(r.guideTask())
    }

    @Test
    fun theGuideGoesForTheGroupMostOfThePickedWordsBelongTo() {
        val r = round()
        r.pick(listOf(9, 10))
        val step = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(group(2), step.cells)
    }
}
