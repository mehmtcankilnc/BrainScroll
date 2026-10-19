package com.mehmtcan.brainscroll.game.ballslide

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

class BallSlideRoundTest {

    private var clock = 1_000L
    private val board = BallSlideGenerator.generate(4, Difficulty.Easy)
    private val puzzle = board.puzzle
    private val way = assertNotNull(BallSlideSolver.shortest(puzzle, puzzle.start, 1L shl puzzle.start))

    private fun round() = BallSlideRound(0, "b1", puzzle, board.par, Language.EN, { clock })

    /** A direction in which the ball can not go, from the start. */
    private fun blockedDir() = (0 until 4).first { puzzle.slide(puzzle.start, it).isEmpty() }

    private fun BallSlideRound.solve(stepMillis: Long = 1_000) {
        for (dir in way) {
            clock += stepMillis
            slide(dir)
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstSlide() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.BallSlide, r.kind)
        r.slide(way.first())
        assertFalse(r.isUntouched)
    }

    @Test
    fun aSlideIntoAWallChangesNothingNotEvenTheClock() {
        val r = round()
        r.slide(way.first())
        clock += 4_000
        val blocked = (0 until 4).first { r.snapshot().state.let { s -> puzzleSlideEmpty(s.ball, it) } }
        assertEquals(BallSlideResult.Ignored, r.slide(blocked))
        clock += 3_000
        r.undo()
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(2, r.toStored().guesses.size)
    }

    private fun puzzleSlideEmpty(ball: Int, dir: Int) = puzzle.slide(ball, dir).isEmpty()

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        clock = 10_000
        r.slide(way.first())
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.undo()
        clock += 10 * 60_000
        r.slide(way.first())
        assertEquals(2_500 + BallSlideRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsBoardAndActions() {
        val r = round()
        r.slide(way.first())
        r.undo()
        r.slide(way.first())
        r.restart()
        r.slide(way.first())
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.BallSlide, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertEquals(listOf("m${way.first()}", "u", "m${way.first()}", "r", "m${way.first()}"), stored.guesses)

        val back = assertNotNull(BallSlideRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(BallSlideRound.restore(0, stored.copy(answer = "!"), { clock }))
        assertNull(BallSlideRound.restore(0, stored.copy(guesses = listOf("x3")), { clock }))
        assertNull(BallSlideRound.restore(0, stored.copy(guesses = listOf("m")), { clock }))
        assertNull(BallSlideRound.restore(0, stored.copy(guesses = listOf("mx")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfSlides() {
        val r = round()
        clock = 5_000
        r.solve()
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.BallSlide, finished.game)
        assertEquals(listOf(way.size.toString()), finished.guesses)
        assertEquals(way.size, finished.moveCount)
        assertTrue(finished.answer.length <= 32)
    }

    @Test
    fun theGuidePointsAtTheFirstSlideOfAShortestWayAndStopsWhenDone() {
        val r = round()
        val step = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.BallSlideMove, step.reason)
        assertEquals(listOf(way.first()), step.numbers)
        assertEquals(puzzle.slide(puzzle.start, way.first()), step.cells, "the cells the slide goes over")
        r.solve()
        assertNull(r.guideTask())
    }
}

/** Ball slide as one of the games of the endless feed. */
class BallSlideFeedTest {

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
        games: List<GameKind> = listOf(GameKind.BallSlide),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): BallSlideSnapshot = assertIs(snapshot().pages[index])

    /** The slides of a shortest way, worked out from the walls and the ball on the page. */
    private fun EndlessFeed.way(index: Int): List<Int> {
        val s = board(index).state
        val puzzle = BallSlidePuzzle(s.size, s.walls, s.ball)
        return assertNotNull(BallSlideSolver.shortest(puzzle, s.ball, 1L shl s.ball))
    }

    @Test
    fun clearingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        for (dir in f.way(0)) f.slideBall(0, dir)
        assertEquals(1, f.snapshot().solvedCount)
        val result = store.results.single()
        assertEquals(GameKind.BallSlide, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertFalse(store.progress.containsKey(result.id))
        f.slideBall(0, 0)
        f.undoBall(0)
        f.restartBall(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val dirs = f.way(0)
        f.slideBall(0, dirs[0])
        clock += 4_000
        f.slideBall(0, dirs[1])
        val saved = store.progress.values.single()
        assertEquals(GameKind.BallSlide, saved.game)
        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun undoAndRestartDoNothingBeforeTheFirstSlide() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.undoBall(0))
        assertFalse(f.restartBall(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.slideBall(0, f.way(0).first())
        assertTrue(f.undoBall(0))
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.BallSlide))
        f.ensureSize(2)
        assertEquals(BallSlideResult.Ignored, f.slideBall(0, 1), "page 0 is a word puzzle")
        assertFalse(f.undoBall(0))
        assertFalse(f.restartBall(0))
    }

    @Test
    fun theLevelOfTheFeedMakesTheBoardBiggerOrSmaller() {
        val sizes = Difficulty.entries.map { level ->
            val f = EndlessFeed(lists, Language.EN, random = Random(7), store = FakeStore(), now = { clock }, newId = { "x${ids++}" }, games = listOf(GameKind.BallSlide), difficulties = mapOf(GameKind.BallSlide to level))
            f.ensureSize(1)
            f.board(0).state.size
        }
        assertEquals(listOf(4, 5, 6), sizes)
    }
}
