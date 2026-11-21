package com.mehmtcan.brainscroll.game.fruitmath

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.FruitGuide
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FeedStore
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A fruit equations round in the feed: its clock, what is saved, what is restored and what the guide says. */
class FruitRoundTest {

    private class FakeStore : FeedStore {
        val progress = linkedMapOf<String, StoredRound>()
        val finished = ArrayList<FinishedRound>()
        override fun saveProgress(round: StoredRound) { progress[round.id] = round }
        override fun deleteProgress(id: String) { progress.remove(id) }
        override fun finish(result: FinishedRound) { progress.remove(result.id); finished += result }
    }

    private val words = listOf("crane", "slate", "ghost")
    private val lists = mapOf(Language.EN to WordList(Language.EN, 5, valid = words, answers = words))

    private fun feed(store: FakeStore, now: () -> Long = { 0L }, restored: List<StoredRound> = emptyList()) =
        EndlessFeed(lists, Language.EN, random = Random(5), store = store, restored = restored, games = listOf(GameKind.FruitMath), now = now)

    private fun page(f: EndlessFeed, index: Int = 0) = f.snapshot().pages[index] as FruitSnapshot

    private fun wrongAnswer(p: FruitSnapshot) = p.state.puzzle.answer + 1

    @Test
    fun aWrongAnswerIsCountedSavedAndComesBack() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val wrong = wrongAnswer(page(f))
        assertEquals(FruitResult.Wrong, f.submitFruit(0, wrong))
        assertEquals(FruitResult.Wrong, f.submitFruit(0, wrong))
        assertEquals("m2", store.progress.values.single().guesses.single())
        val back = feed(FakeStore(), restored = store.progress.values.sortedBy { it.position })
        assertEquals(2, page(back).state.mistakes)
        assertEquals(page(f).id, page(back).id)
    }

    @Test
    fun theRightAnswerGoesToTheHistoryWithItsMistakesAndIsNotKept() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val p = page(f)
        f.submitFruit(0, wrongAnswer(p))
        assertEquals(FruitResult.Solved, f.submitFruit(0, p.state.puzzle.answer))
        val result = store.finished.single()
        assertEquals(GameKind.FruitMath, result.game)
        assertEquals(listOf("1"), result.guesses)
        assertEquals(1, result.moveCount)
        assertTrue(store.progress.isEmpty())
        assertEquals(1, f.solvedCount)
    }

    @Test
    fun theTimeIsTheTimeThePageWasOnShowSoThinkingBeforeTheOnlyAnswerCounts() {
        val store = FakeStore()
        var clock = 10_000L
        val f = feed(store, now = { clock })
        f.ensureSize(1)
        clock += 5_000
        assertEquals(0L, page(f).durationMs, "a page that is not on show has no time yet")
        f.pageShown(0)
        clock += 60_000
        assertEquals(60_000L, page(f).durationMs, "the clock runs from the moment the page came to the screen, with no answer given")
        f.submitFruit(0, page(f).state.puzzle.answer)
        assertEquals(60_000L, store.finished.single().durationMs, "right at the first try after a minute: a minute, not no time")
        clock += 30_000
        assertEquals(60_000L, store.finished.single().durationMs)
    }

    @Test
    fun theClockStopsWhileThePageOrTheFeedIsLeftAndGoesOnWhenItIsBack() {
        val store = FakeStore()
        var clock = 0L
        val f = feed(store, now = { clock })
        f.ensureSize(2)
        f.pageShown(0)
        clock += 4_000
        f.pageShown(1)
        clock += 100_000
        assertEquals(4_000L, page(f, 0).durationMs, "page 0 was left")
        f.pagesHidden()
        clock += 50_000
        assertEquals(100_000L, page(f, 1).durationMs, "the feed was left (another tab, the app in the background)")
        f.pageShown(0)
        clock += 1_000
        assertEquals(5_000L, page(f, 0).durationMs, "it goes on from where it stopped")
    }

    @Test
    fun aPageOnShowIsTheSamePageWhateverTheClockSays() {
        var clock = 0L
        val f = feed(FakeStore(), now = { clock })
        f.ensureSize(1)
        f.pageShown(0)
        val before = page(f)
        clock += 5_000
        assertEquals(before.withoutClock(), page(f).withoutClock())
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val puzzle = FruitGenerator.generate(1, Difficulty.Easy)
        val stored = FruitRound(0, "x", puzzle, Language.EN).toStored()
        assertNotNull(FruitRound.restore(0, stored, { 0L }))
        assertNull(FruitRound.restore(0, stored.copy(answer = "!!"), { 0L }))
        assertNull(FruitRound.restore(0, stored.copy(guesses = listOf("m-1")), { 0L }), "a negative number of mistakes")
        assertNull(FruitRound.restore(0, stored.copy(guesses = listOf("mx")), { 0L }), "not a number")
        assertNull(FruitRound.restore(0, stored.copy(guesses = listOf("q")), { 0L }))
    }

    @Test
    fun theGuideGivesTheValuesOfTheFruits() {
        for (level in Difficulty.entries) for (seed in 0 until 30) {
            val puzzle = FruitGenerator.generate(seed, level)
            val game = FruitGame(puzzle)
            val step = assertNotNull(FruitGuide.step(puzzle, game.state))
            assertEquals(GuideReason.FruitValues, step.reason)
            assertEquals(puzzle.fruits + puzzle.values, step.numbers)
            game.submit(puzzle.answer)
            assertNull(FruitGuide.step(puzzle, game.state), "nothing to say once it is won")
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstAnswer() {
        val round = FruitRound(0, "x", FruitGenerator.generate(2, Difficulty.Easy), Language.EN)
        assertTrue(round.isUntouched)
        assertFalse(round.isFinished)
        round.submit(-5)
        assertTrue(round.isUntouched, "a number that cannot be an answer changes nothing")
        round.submit(round.snapshot().state.puzzle.answer + 1)
        assertFalse(round.isUntouched)
    }
}
