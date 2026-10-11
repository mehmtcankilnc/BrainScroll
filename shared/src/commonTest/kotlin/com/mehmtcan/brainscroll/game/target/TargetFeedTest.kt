package com.mehmtcan.brainscroll.game.target

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

/** The target number game as one of the games of the endless feed. */
class TargetFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Target),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.game(index: Int): TargetSnapshot = assertIs(snapshot().pages[index])

    /** The moves that solve page [index]: its puzzle is the numbers and the target of the page as they are on the table. */
    private fun EndlessFeed.solution(index: Int): List<Move> {
        val state = game(index).state
        return TargetSolver.solve(state.tokens.map { it.value }, state.target)!!
    }

    private fun EndlessFeed.solve(index: Int) {
        for (move in solution(index)) combineNumbers(index, move.first, move.op, move.second)
    }

    @Test
    fun theGamesComeOneAfterTheOtherAndTargetIsEighth() {
        val all = listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines, GameKind.Sudoku, GameKind.Lights, GameKind.Slide, GameKind.Target)
        val f = feed(games = all)
        f.ensureSize(9)
        assertEquals(all + GameKind.Word, f.snapshot().pages.map { it.kind })
    }

    @Test
    fun solvingAGameSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        f.solve(0)
        assertEquals(1, f.snapshot().answerStreak)
        val result = store.results.single()
        assertEquals(GameKind.Target, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(f.game(0).state.par.toString(), result.guesses.single())
        assertFalse(store.progress.containsKey(result.id))
        f.combineNumbers(0, 0, Op.Add, 1)
        f.undoNumbers(0)
        assertEquals(1, store.results.size, "a finished game takes no more actions")
    }

    @Test
    fun aGameInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.combineNumbers(0, 0, Op.Add, 1)
        clock += 4_000
        f.combineNumbers(0, 2, Op.Mul, 3)
        f.undoNumbers(0)
        f.combineNumbers(0, 2, Op.Add, 3)
        val saved = store.progress.values.single()
        assertEquals(GameKind.Target, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.game(0).state, back.game(0).state)
        assertEquals(f.game(0).durationMs, back.game(0).durationMs)
    }

    @Test
    fun undoAndRestartDoNothingOnAGameThatIsAtItsStart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.undoNumbers(0))
        assertFalse(f.restartNumbers(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.combineNumbers(0, 0, Op.Add, 1)
        assertTrue(f.undoNumbers(0))
        assertEquals(1, f.game(0).state.moves)
    }

    @Test
    fun aMoveThatIsNotAllowedIsIgnoredAndTheNumbersCanBePreviewed() {
        val f = feed()
        f.ensureSize(1)
        val tokens = f.game(0).state.tokens
        assertEquals(TargetResult.Ignored, f.combineNumbers(0, 0, Op.Add, 0))
        assertEquals(tokens[0].value + tokens[1].value, f.previewNumbers(0, 0, Op.Add, 1))
        assertEquals(0, f.game(0).state.moves)
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Target))
        f.ensureSize(2)
        assertEquals(TargetResult.Ignored, f.combineNumbers(0, 0, Op.Add, 1), "page 0 is a word puzzle")
        assertFalse(f.undoNumbers(0))
        assertFalse(f.restartNumbers(0))
    }
}
