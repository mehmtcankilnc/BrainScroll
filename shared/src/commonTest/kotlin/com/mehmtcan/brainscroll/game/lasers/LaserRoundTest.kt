package com.mehmtcan.brainscroll.game.lasers

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.guide.LaserGuide
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

/** A laser mirrors round in the feed: its clock, what is saved, what is restored and what the guide says. */
class LaserRoundTest {

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
        EndlessFeed(lists, Language.EN, random = Random(5), store = store, restored = restored, games = listOf(GameKind.Lasers), now = now)

    private fun page(f: EndlessFeed, index: Int = 0) = f.snapshot().pages[index] as LaserSnapshot

    /** The mirrors to turn, in cell numbers, to win the board of [page] in the fewest turns. */
    private fun winning(page: LaserSnapshot): List<Int> {
        val s = page.state
        val cells = s.mirrors.indices.filter { s.mirrors[it] != 0 }
        val turns = assertNotNull(LaserRules.fewestTurns(s.size, s.row, s.mirrors, s.targets))
        return cells.zip(turns).filter { it.second }.map { it.first }
    }

    @Test
    fun aTurnIsSavedAndARestartIsToo() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val start = page(f).state.mirrors
        val cell = start.indexOfFirst { it != 0 }
        assertEquals(LaserResult.Turned, f.turnMirror(0, cell))
        assertEquals(1, page(f).state.turns)
        assertTrue(store.progress.values.single().guesses.contains("n1"), "the turn is saved")
        assertTrue(f.restartLasers(0))
        assertEquals(start, page(f).state.mirrors)
        assertFalse(f.restartLasers(0), "already at the start")
    }

    @Test
    fun aWonBoardGoesToTheHistoryWithItsTurnsAndIsNotKept() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val cells = winning(page(f))
        var last = LaserResult.Ignored
        cells.forEach { last = f.turnMirror(0, it) }
        assertEquals(LaserResult.Solved, last)
        val result = store.finished.single()
        assertEquals(GameKind.Lasers, result.game)
        assertEquals(cells.size, result.moveCount)
        assertTrue(store.progress.isEmpty())
        assertEquals(1, f.solvedCount)
    }

    @Test
    fun theTimeIsTheTimeThePageWasOnShowSoThinkingBeforeTheFirstTapCounts() {
        val store = FakeStore()
        var clock = 10_000L
        val f = feed(store, now = { clock })
        f.ensureSize(1)
        clock += 5_000
        assertEquals(0L, page(f).durationMs, "a page that is not on show has no time yet")
        f.pageShown(0)
        clock += 60_000
        assertEquals(60_000L, page(f).durationMs, "the clock runs from the moment the page came to the screen, with no tap made")
        winning(page(f)).forEach { f.turnMirror(0, it) }
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
        val cell = page(f).state.mirrors.indexOfFirst { it != 0 }
        f.turnMirror(0, cell)
        f.markGuided(0)
        val back = feed(FakeStore(), restored = store.progress.values.sortedBy { it.position })
        val restored = page(back)
        assertEquals(page(f).state.mirrors, restored.state.mirrors)
        assertEquals(1, restored.state.turns)
        assertEquals(page(f).id, restored.id)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val puzzle = LaserGenerator.generate(1, Difficulty.Easy).puzzle
        val stored = LaserRound(0, "x", puzzle, Language.EN).toStored()
        assertNotNull(LaserRound.restore(0, stored, { 0L }))
        assertNull(LaserRound.restore(0, stored.copy(answer = "!!"), { 0L }))
        assertNull(LaserRound.restore(0, stored.copy(guesses = listOf("n1", "m1")), { 0L }), "wrong number of mirrors")
        assertNull(LaserRound.restore(0, stored.copy(guesses = listOf("n1", "m" + "9".repeat(puzzle.mirrorCells.size))), { 0L }), "not a mirror")
        assertNull(LaserRound.restore(0, stored.copy(guesses = listOf("q")), { 0L }))
    }

    @Test
    fun theGuidePointsAtAMirrorOfTheFewestTurnsAndFollowingItWins() {
        for (level in Difficulty.entries) for (seed in 0 until 20) {
            val puzzle = LaserGenerator.generate(seed, level).puzzle
            val game = LaserGame(puzzle)
            var steps = 0
            while (game.state.status == LaserStatus.Playing) {
                val step = assertNotNull(LaserGuide.step(puzzle, game.state), "$level seed $seed")
                assertEquals(GuideReason.LaserTurn, step.reason)
                assertTrue(puzzle.mirrorCells.contains(step.cells.single()))
                game.turn(step.cells.single())
                assertTrue(++steps <= LaserGenerator.generate(seed, level).par, "$level seed $seed follows the fewest turns")
            }
            assertNull(LaserGuide.step(puzzle, game.state), "nothing to say once it is won")
        }
    }
}
