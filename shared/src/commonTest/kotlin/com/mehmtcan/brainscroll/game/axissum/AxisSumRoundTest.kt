package com.mehmtcan.brainscroll.game.axissum

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FeedStore
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AxisSumRoundTest {

    private var clock = 1_000L
    private val puzzle = AxisSumGenerator.generate(4, Difficulty.Medium)
    private val solution = assertNotNull(AxisSumSolver.unique(puzzle))
    private val toErase = solution.indices.filter { !solution[it] }

    private fun round() = AxisSumRound(0, "a1", puzzle, Language.EN, { clock }).also { it.onShown() }

    private fun AxisSumRound.solve(stepMillis: Long = 1_000) {
        for (cell in toErase) {
            clock += stepMillis
            tap(cell)
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstTap() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.AxisSum, r.kind)
        r.tap(toErase.first())
        assertFalse(r.isUntouched)
    }

    @Test
    fun theTimeIsTheTimeThePageWasOnShowAndThinkingIsNotCapped() {
        clock = 10_000
        val r = round()
        r.tap(0)
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.tap(0)
        clock += 10 * 60_000
        r.tap(0)
        assertEquals(2_500 + 10 * 60_000L, r.snapshot().durationMs)
    }

    @Test
    fun aTapOnNothingChangesNothingNotEvenTheClock() {
        val r = round()
        r.tap(0)
        clock += 4_000
        assertEquals(AxisSumResult.Ignored, r.tap(-1))
        clock += 3_000
        r.tap(0)
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(2, r.toStored().guesses.size)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsBoardAndActions() {
        val r = round()
        val other = solution.indices.first { solution[it] }
        r.tap(toErase[0])
        r.tap(other)
        r.restart()
        r.tap(toErase[1])
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.AxisSum, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertEquals(listOf("t${toErase[0]}", "t$other", "r", "t${toErase[1]}"), stored.guesses)

        val back = assertNotNull(AxisSumRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(AxisSumRound.restore(0, stored.copy(answer = "!"), { clock }))
        assertNull(AxisSumRound.restore(0, stored.copy(guesses = listOf("x3")), { clock }))
        assertNull(AxisSumRound.restore(0, stored.copy(guesses = listOf("t")), { clock }))
        assertNull(AxisSumRound.restore(0, stored.copy(guesses = listOf("tx")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfTaps() {
        clock = 5_000
        val r = round()
        r.solve()
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.AxisSum, finished.game)
        assertEquals(listOf(toErase.size.toString()), finished.guesses)
        assertEquals(toErase.size, finished.moveCount)
        assertTrue(finished.answer.length <= 32)
    }

    @Test
    fun theGuideBringsBackAWrongErasureFirstThenErasesTheNextNumber() {
        val r = round()
        val wrong = solution.indexOfFirst { it }
        r.tap(wrong)
        val back = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.AxisSumKeep, back.reason)
        assertEquals(listOf(wrong), back.cells)
        assertEquals(listOf(wrong / puzzle.size + 1, wrong % puzzle.size + 1), back.numbers)
        r.tap(wrong)
        val erase = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.AxisSumErase, erase.reason)
        assertTrue(erase.cells.single() in toErase)
        r.solve()
        assertNull(r.guideTask())
    }
}

/** Axis sum as one of the games of the endless feed. */
class AxisSumFeedTest {

    private class FakeStore : FeedStore {
        val progress = linkedMapOf<String, StoredRound>()
        val results = mutableListOf<FinishedRound>()
        override fun saveProgress(round: StoredRound) { progress[round.id] = round }
        override fun deleteProgress(id: String) { progress.remove(id) }
        override fun finish(result: FinishedRound) {
            progress.remove(result.id)
            results += result
        }
    }

    private val words = listOf("crane", "slate", "ghost")
    private val lists = mapOf(
        Language.EN to WordList(Language.EN, 5, valid = words, answers = words),
        Language.TR to WordList(Language.TR, 5, valid = listOf("kitap"), answers = listOf("kitap")),
    )
    private var clock = 1_000L
    private var ids = 0

    private fun feed(
        store: FeedStore = FakeStore(),
        restored: List<StoredRound> = emptyList(),
        games: List<GameKind> = listOf(GameKind.AxisSum),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): AxisSumSnapshot = assertIs(snapshot().pages[index])

    /** The cells to erase, found from what the page shows (the sums and the numbers). */
    private fun EndlessFeed.toErase(index: Int): List<Int> {
        val s = board(index).state
        val way = assertNotNull(AxisSumSolver.solutions(s.size, s.digits, s.rowTargets, s.columnTargets, limit = 1).firstOrNull())
        return way.indices.filter { !way[it] }
    }

    @Test
    fun clearingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        for (cell in f.toErase(0)) f.tapAxisSum(0, cell)
        assertEquals(1, f.snapshot().solvedCount)
        val result = store.results.single()
        assertEquals(GameKind.AxisSum, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertFalse(store.progress.containsKey(result.id))
        f.tapAxisSum(0, 0)
        f.restartAxisSum(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val cells = f.toErase(0)
        f.tapAxisSum(0, cells[0])
        clock += 4_000
        f.tapAxisSum(0, cells[1])
        val saved = store.progress.values.single()
        assertEquals(GameKind.AxisSum, saved.game)
        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun restartingABoardNothingIsErasedOnDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartAxisSum(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.tapAxisSum(0, 0)
        assertTrue(f.restartAxisSum(0))
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.AxisSum))
        f.ensureSize(2)
        assertEquals(AxisSumResult.Ignored, f.tapAxisSum(0, 3), "page 0 is a word puzzle")
        assertFalse(f.restartAxisSum(0))
    }

    @Test
    fun theLevelOfTheFeedMakesTheBoardBiggerOrSmaller() {
        val sizes = Difficulty.entries.map { level ->
            val f = EndlessFeed(lists, Language.EN, random = Random(7), store = FakeStore(), now = { clock }, newId = { "x${ids++}" }, games = listOf(GameKind.AxisSum), difficulties = mapOf(GameKind.AxisSum to level))
            f.ensureSize(1)
            f.board(0).state.size
        }
        assertEquals(listOf(4, 5, 6), sizes)
    }
}
