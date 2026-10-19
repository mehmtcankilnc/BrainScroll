package com.mehmtcan.brainscroll.game

import com.mehmtcan.brainscroll.game.hanoi.HanoiSnapshot
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FeedStore
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The level of the boards in the endless feed: chosen per game, stamped on what is saved, kept on restore. */
class DifficultyFeedTest {

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
    private var ids = 0

    private fun feed(
        store: FeedStore = FakeStore(),
        restored: List<StoredRound> = emptyList(),
        games: List<GameKind> = listOf(GameKind.Hanoi),
        difficulties: Map<GameKind, Difficulty> = emptyMap(),
    ) = EndlessFeed(lists, Language.EN, random = Random(3), store = store, restored = restored, newId = { "id${ids++}" }, games = games, difficulties = difficulties)

    private fun EndlessFeed.hanoi(index: Int): HanoiSnapshot = assertIs(snapshot().pages[index])

    @Test
    fun everyPageIsMediumUntilALevelIsChosen() {
        val f = feed()
        f.ensureSize(3)
        assertEquals(List(3) { Difficulty.Medium }, f.snapshot().difficulties)
        assertEquals(Difficulty.Medium, f.difficultyOf(GameKind.Hanoi))
    }

    @Test
    fun theChosenLevelIsTheLevelOfNewPages() {
        val f = feed(games = listOf(GameKind.Hanoi, GameKind.Queens), difficulties = mapOf(GameKind.Hanoi to Difficulty.Hard))
        f.ensureSize(4)
        assertEquals(
            listOf(Difficulty.Hard, Difficulty.Medium, Difficulty.Hard, Difficulty.Medium),
            f.snapshot().difficulties,
            "a game that has no chosen level is medium",
        )
    }

    @Test
    fun aStartedRoundIsSavedAndFinishedWithItsLevel() {
        val store = FakeStore()
        val f = feed(store, difficulties = mapOf(GameKind.Hanoi to Difficulty.Easy))
        f.ensureSize(2)
        val moves = com.mehmtcan.brainscroll.game.hanoi.HanoiSolver.shortest(f.hanoi(0).state.pegs.toIntArray(), f.hanoi(0).state.goal)
        f.moveDisk(0, moves[0].from, moves[0].to)
        assertEquals(Difficulty.Easy, store.progress.values.single().difficulty)
        for ((i, move) in moves.withIndex()) if (i > 0) f.moveDisk(0, move.from, move.to)
        assertEquals(Difficulty.Easy, store.results.single().difficulty)
    }

    @Test
    fun aRestoredRoundKeepsTheLevelItWasSavedWith() {
        val store = FakeStore()
        val first = feed(store, difficulties = mapOf(GameKind.Hanoi to Difficulty.Hard))
        first.ensureSize(1)
        val state = first.hanoi(0).state
        val move = com.mehmtcan.brainscroll.game.hanoi.HanoiSolver.shortest(state.pegs.toIntArray(), state.goal)[0]
        first.moveDisk(0, move.from, move.to)

        // The player has chosen another level since; the saved page is still a hard one.
        val back = feed(store, restored = store.progress.values.toList(), difficulties = mapOf(GameKind.Hanoi to Difficulty.Easy))
        assertEquals(Difficulty.Hard, back.snapshot().difficulties.first())
        back.ensureSize(2)
        assertEquals(Difficulty.Easy, back.snapshot().difficulties[1])
        back.moveDisk(0, move.from, move.to)
        assertEquals(Difficulty.Hard, store.progress.getValue(back.snapshot().pages[0].id).difficulty, "saving again keeps the level")
    }

    @Test
    fun changingTheLevelRedrawsOnlyThePagesNotTouchedYet() {
        val f = feed()
        f.ensureSize(3)
        val state = f.hanoi(0).state
        val move = com.mehmtcan.brainscroll.game.hanoi.HanoiSolver.shortest(state.pegs.toIntArray(), state.goal)[0]
        f.moveDisk(0, move.from, move.to) // page 0 is started
        val before = f.snapshot().pages.map { it.id }

        f.setDifficulty(GameKind.Hanoi, Difficulty.Hard, fromIndex = 0)

        val after = f.snapshot()
        assertEquals(before[0], after.pages[0].id, "the started page stays")
        assertNotEquals(before[1], after.pages[1].id)
        assertNotEquals(before[2], after.pages[2].id)
        assertEquals(listOf(Difficulty.Medium, Difficulty.Hard, Difficulty.Hard), after.difficulties)
        assertEquals(Difficulty.Hard, f.difficultyOf(GameKind.Hanoi))
    }

    @Test
    fun pagesBeforeTheGivenIndexAreLeftAlone() {
        val f = feed()
        f.ensureSize(3)
        val before = f.snapshot().pages.map { it.id }
        f.setDifficulty(GameKind.Hanoi, Difficulty.Easy, fromIndex = 2)
        assertEquals(before.take(2), f.snapshot().pages.map { it.id }.take(2))
        assertEquals(listOf(Difficulty.Medium, Difficulty.Medium, Difficulty.Easy), f.snapshot().difficulties)
    }

    @Test
    fun choosingTheSameLevelChangesNothing() {
        val f = feed(difficulties = mapOf(GameKind.Hanoi to Difficulty.Hard))
        f.ensureSize(2)
        val before = f.snapshot().pages.map { it.id }
        f.setDifficulty(GameKind.Hanoi, Difficulty.Hard, fromIndex = 0)
        assertEquals(before, f.snapshot().pages.map { it.id })
    }

    @Test
    fun aLevelOnlyChangesThePagesOfItsOwnGame() {
        val f = feed(games = listOf(GameKind.Hanoi, GameKind.Queens))
        f.ensureSize(4)
        val before = f.snapshot().pages
        f.setDifficulty(GameKind.Queens, Difficulty.Hard, fromIndex = 0)
        val after = f.snapshot().pages
        for (i in before.indices) {
            if (before[i].kind == GameKind.Hanoi) assertEquals(before[i].id, after[i].id) else assertNotEquals(before[i].id, after[i].id)
        }
        assertTrue(f.snapshot().difficulties.zip(after).all { (level, page) -> level == if (page.kind == GameKind.Queens) Difficulty.Hard else Difficulty.Medium })
    }

    @Test
    fun aGuidedPageIsNotRedrawnEvenIfNotTouched() {
        val f = feed()
        f.ensureSize(2)
        f.markGuided(1)
        val id = f.snapshot().pages[1].id
        f.setDifficulty(GameKind.Hanoi, Difficulty.Hard, fromIndex = 0)
        assertEquals(id, f.snapshot().pages[1].id)
    }
}
