package com.mehmtcan.brainscroll.game.untangle

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.guide.UntangleGuide
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

/** An untangle round in the feed: its clock, what is saved, what is restored and what the guide says. */
class UntangleRoundTest {

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
        EndlessFeed(lists, Language.EN, random = Random(5), store = store, restored = restored, games = listOf(GameKind.Untangle), now = now)

    private fun page(f: EndlessFeed, index: Int = 0) = f.snapshot().pages[index] as UntangleSnapshot

    /** A drag of dot [node] to ([x], [y]): picked up, moved on the way and let go of. */
    private fun EndlessFeed.drag(node: Int, x: Int, y: Int): UntangleResult {
        moveNode(0, node, (x + page(this).state.positions[2 * node]) / 2, (y + page(this).state.positions[2 * node + 1]) / 2)
        moveNode(0, node, x, y)
        return dropNode(0)
    }

    private fun solutionOf(f: EndlessFeed): List<Int> {
        val s = page(f).state
        return assertNotNull(UntangleEmbedder.embed(s.nodeCount, s.edges, 3))
    }

    @Test
    fun aDragIsSavedWithTheDotsWhereTheyAre() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val start = page(f).state.positions
        assertEquals(UntangleResult.Moved, f.drag(0, 500, 500))
        assertEquals(1, page(f).state.moves)
        assertTrue(page(f).state.positions != start)
        assertTrue(store.progress.values.single().guesses.contains("n1"), "the move is saved")
        assertTrue(f.restartUntangle(0))
        assertEquals(start, page(f).state.positions)
        assertFalse(f.restartUntangle(0), "already at the start")
    }

    @Test
    fun aWonBoardGoesToTheHistoryWithItsMovesAndIsNotKept() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val drawing = solutionOf(f)
        var result = UntangleResult.Ignored
        for (node in 0 until page(f).state.nodeCount) {
            result = f.drag(node, drawing[2 * node], drawing[2 * node + 1])
            if (result == UntangleResult.Solved) break
        }
        assertEquals(UntangleResult.Solved, result)
        val won = store.finished.single()
        assertEquals(GameKind.Untangle, won.game)
        assertTrue(won.moveCount >= 1)
        assertTrue(store.progress.isEmpty())
        assertEquals(1, f.solvedCount)
    }

    @Test
    fun theTimeIsTheTimeThePageWasOnShowSoThinkingBeforeTheFirstDragCounts() {
        val store = FakeStore()
        var clock = 10_000L
        val f = feed(store, now = { clock })
        f.ensureSize(1)
        clock += 5_000
        assertEquals(0L, page(f).durationMs, "a page that is not on show has no time yet")
        f.pageShown(0)
        clock += 60_000
        assertEquals(60_000L, page(f).durationMs, "the clock runs from the moment the page came to the screen, with no dot touched")
        val drawing = solutionOf(f)
        for (node in 0 until page(f).state.nodeCount) {
            if (f.drag(node, drawing[2 * node], drawing[2 * node + 1]) == UntangleResult.Solved) break
        }
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
    fun aGameInProgressComesBackTheWayItWas() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.drag(0, 500, 500)
        f.markGuided(0)
        val back = feed(FakeStore(), restored = store.progress.values.sortedBy { it.position })
        val restored = page(back)
        assertEquals(page(f).state.positions, restored.state.positions)
        assertEquals(1, restored.state.moves)
        assertEquals(page(f).id, restored.id)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val puzzle = UntangleGenerator.generate(1, Difficulty.Easy).puzzle
        val stored = UntangleRound(0, "x", puzzle, Language.EN).toStored()
        assertNotNull(UntangleRound.restore(0, stored, { 0L }))
        assertNull(UntangleRound.restore(0, stored.copy(answer = "!!"), { 0L }))
        assertNull(UntangleRound.restore(0, stored.copy(guesses = listOf("n1", "p123")), { 0L }), "wrong number of dots")
        assertNull(UntangleRound.restore(0, stored.copy(guesses = listOf("n1", "p" + "zzz".repeat(puzzle.nodeCount * 2))), { 0L }), "not numbers")
        assertNull(UntangleRound.restore(0, stored.copy(guesses = listOf("q")), { 0L }))
    }

    @Test
    fun followingTheGuideUntanglesTheBoard() {
        for (level in Difficulty.entries) for (seed in 0 until 25) {
            val puzzle = UntangleGenerator.generate(seed, level).puzzle
            val game = UntangleGame(puzzle)
            var steps = 0
            while (game.state.status == UntangleStatus.Playing) {
                val step = assertNotNull(UntangleGuide.step(puzzle, game.state), "$level seed $seed step $steps has a guide")
                assertEquals(GuideReason.UntangleMove, step.reason)
                game.move(step.cells.single(), step.numbers[0], step.numbers[1])
                game.drop()
                assertTrue(++steps <= puzzle.nodeCount * 3, "$level seed $seed: the guide has not untangled the board in $steps steps")
            }
            assertNull(UntangleGuide.step(puzzle, game.state), "nothing to say once it is won")
        }
    }
}
