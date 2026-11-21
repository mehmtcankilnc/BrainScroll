package com.mehmtcan.brainscroll.game.matchsticks

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.guide.MatchGuide
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

/** A matchstick round in the feed: its clock, what is saved, what is restored and what the guide says. */
class MatchRoundTest {

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
        EndlessFeed(lists, Language.EN, random = Random(5), store = store, restored = restored, games = listOf(GameKind.Matchsticks), now = now)

    private fun page(f: EndlessFeed, index: Int = 0) = f.snapshot().pages[index] as MatchSnapshot

    @Test
    fun aWrongMoveIsSavedAndAPutBackStickIsToo() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertTrue(page(f).state.start == page(f).state.cells)
        val start = page(f).state.cells
        val wrong = MatchRules.moves(start).first { !MatchRules.isTrue(MatchRules.apply(start, it)) }
        assertEquals(MatchResult.Moved, f.moveStick(0, wrong))
        assertEquals(1, page(f).state.moves)
        assertTrue(store.progress.values.single().guesses.contains("n1"), "the move is saved")
        assertTrue(f.undoStick(0))
        assertEquals(start, page(f).state.cells)
        assertEquals(1, page(f).state.moves)
        assertFalse(f.undoStick(0), "nothing to put back")
    }

    @Test
    fun aSolvedSumGoesToTheHistoryWithItsMovesAndIsNotKept() {
        val store = FakeStore()
        var clock = 0L
        val f = feed(store, now = { clock })
        f.ensureSize(1)
        val start = page(f).state.cells
        val win = MatchRules.winningMoves(start).single()
        clock = 1_000
        assertEquals(MatchResult.Solved, f.moveStick(0, win))
        val result = store.finished.single()
        assertEquals(GameKind.Matchsticks, result.game)
        assertEquals(listOf("1"), result.guesses)
        assertEquals(1, result.moveCount)
        assertTrue(store.progress.isEmpty())
        assertEquals(1, f.solvedCount)
    }

    @Test
    fun theTimeIsTheTimeThePageWasOnShowSoThinkingBeforeTheOnlyMoveCounts() {
        val store = FakeStore()
        var clock = 10_000L
        val f = feed(store, now = { clock })
        f.ensureSize(1)
        clock += 5_000
        assertEquals(0L, page(f).durationMs, "a page that is not on show has no time yet")
        f.pageShown(0)
        clock += 60_000
        assertEquals(60_000L, page(f).durationMs, "the clock runs from the moment the page came to the screen, with no move made")
        f.moveStick(0, MatchRules.winningMoves(page(f).state.cells).single())
        assertEquals(60_000L, store.finished.single().durationMs, "solved with one move after a minute: a minute, not no time")
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
        assertEquals(100_000L, page(f, 1).durationMs)
        f.pagesHidden()
        clock += 50_000
        assertEquals(100_000L, page(f, 1).durationMs, "the feed was left (another tab, the app in the background)")
        f.pageShown(0)
        clock += 1_000
        assertEquals(5_000L, page(f, 0).durationMs, "it goes on from where it stopped")
    }

    @Test
    fun aPageThatIsOnShowIsTheSamePageWhateverTheClockSaysSoAGuideStepStaysGood() {
        var clock = 0L
        val f = feed(FakeStore(), now = { clock })
        f.ensureSize(1)
        f.pageShown(0)
        val before = page(f)
        clock += 5_000
        val after = page(f)
        assertTrue(before != after, "the time of a page that is on show runs on")
        assertEquals(before.withoutClock(), after.withoutClock(), "the guide compares the board, not the clock")
    }

    @Test
    fun theTimeSoFarIsSavedWhenThePageIsLeft() {
        val store = FakeStore()
        var clock = 0L
        val f = feed(store, now = { clock })
        f.ensureSize(1)
        f.pageShown(0)
        val start = page(f).state.cells
        f.moveStick(0, MatchRules.moves(start).first { !MatchRules.isTrue(MatchRules.apply(start, it)) })
        clock += 7_000
        f.pagesHidden()
        assertEquals("7000", store.progress.values.single().input)
    }

    @Test
    fun aGameInProgressComesBackTheWayItWas() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val start = page(f).state.cells
        val wrong = MatchRules.moves(start).first { !MatchRules.isTrue(MatchRules.apply(start, it)) }
        f.moveStick(0, wrong)
        f.markGuided(0)
        val back = feed(FakeStore(), restored = store.progress.values.sortedBy { it.position })
        val restored = page(back)
        assertEquals(page(f).state.cells, restored.state.cells)
        assertEquals(1, restored.state.moves)
        assertTrue(restored.state.moved)
        assertEquals(page(f).id, restored.id)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val round = MatchRound(0, "x", MatchGenerator.generate(1, Difficulty.Easy).puzzle, Language.EN)
        val stored = round.toStored()
        assertNotNull(MatchRound.restore(0, stored, { 0L }))
        assertNull(MatchRound.restore(0, stored.copy(answer = "!!"), { 0L }))
        assertNull(MatchRound.restore(0, stored.copy(guesses = listOf("n1", "s1.2")), { 0L }), "wrong number of symbols")
        assertNull(MatchRound.restore(0, stored.copy(guesses = listOf("q")), { 0L }))
    }

    @Test
    fun theGuideTakesTheStickToItsPlaceAndAsksToPutAWrongOneBackFirst() {
        val puzzle = MatchGenerator.generate(8, Difficulty.Medium)
        val game = MatchGame(puzzle.puzzle)
        val step = assertNotNull(MatchGuide.step(puzzle.puzzle, game.state))
        assertEquals(GuideReason.MatchstickMove, step.reason)
        val slots = puzzle.puzzle.slots
        assertEquals(puzzle.solution.from, slots[step.cells[0]])
        assertEquals(puzzle.solution.to, slots[step.cells[1]])
        val wrong = MatchRules.moves(puzzle.puzzle.cells).first { !MatchRules.isTrue(MatchRules.apply(puzzle.puzzle.cells, it)) }
        game.move(wrong)
        assertEquals(GuideReason.MatchstickUndo, assertNotNull(MatchGuide.step(puzzle.puzzle, game.state)).reason)
        game.undo()
        game.move(puzzle.solution)
        assertNull(MatchGuide.step(puzzle.puzzle, game.state), "nothing to say once it is solved")
    }
}
