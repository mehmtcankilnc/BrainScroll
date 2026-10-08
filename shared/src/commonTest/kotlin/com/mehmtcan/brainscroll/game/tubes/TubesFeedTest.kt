package com.mehmtcan.brainscroll.game.tubes

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FeedStore
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Color tubes as one of the games of the endless feed. */
class TubesFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.tubes(index: Int): TubesSnapshot = assertIs(snapshot().pages[index])

    private fun EndlessFeed.solve(index: Int) {
        solutionOf(tubes(index).state.tubes).forEach { (from, to) -> pour(index, from, to) }
    }

    @Test
    fun theGamesComeOneAfterTheOtherAndTubesAreThird() {
        val f = feed()
        f.ensureSize(4)
        assertEquals(listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Word), f.snapshot().pages.map { it.kind })
    }

    @Test
    fun solvingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store, games = listOf(GameKind.Tubes))
        f.ensureSize(2)
        clock += 2_000
        f.solve(0)
        assertTrue(f.snapshot().pages[0].isFinished)
        assertEquals(1, f.snapshot().answerStreak)
        val result = store.results.single()
        assertEquals(GameKind.Tubes, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertFalse(store.progress.containsKey(result.id), "a finished board is not in progress any more")
        // More pours on a won board do nothing.
        f.pour(0, 0, 1)
        assertEquals(1, store.results.size)
    }

    @Test
    fun everySolvedBoardRaisesTheStreakByOne() {
        val f = feed(games = listOf(GameKind.Tubes))
        f.ensureSize(3)
        f.solve(0)
        f.solve(1)
        assertEquals(2, f.snapshot().answerStreak)
    }

    @Test
    fun aBoardInProgressComesBackWithItsMovesAfterARestart() {
        val store = FakeStore()
        val f = feed(store, games = listOf(GameKind.Tubes))
        f.ensureSize(1)
        val (from, to) = solutionOf(f.tubes(0).state.tubes).first()
        f.pour(0, from, to)
        clock += 4_000
        f.undo(0)
        f.pour(0, from, to)
        val saved = store.progress.values.single()
        assertEquals(GameKind.Tubes, saved.game)

        val back = feed(store, restored = listOf(saved), games = listOf(GameKind.Tubes))
        assertEquals(f.tubes(0).state, back.tubes(0).state)
        assertEquals(f.tubes(0).durationMs, back.tubes(0).durationMs)
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed()
        f.ensureSize(2)
        assertEquals(PourResult.Ignored, f.pour(0, 0, 1), "page 0 is a word puzzle")
        assertFalse(f.undo(1), "page 1 is a memory board")
        assertFalse(f.restart(0))
    }

    @Test
    fun anUntouchedTubesPageIsSwappedWhenTheGameIsSwitchedOff() {
        val f = feed()
        f.ensureSize(4)
        f.setEnabledGames(setOf(GameKind.Word, GameKind.Memory), fromIndex = 0)
        assertTrue(f.snapshot().pages.none { it.kind == GameKind.Tubes })
    }
}
