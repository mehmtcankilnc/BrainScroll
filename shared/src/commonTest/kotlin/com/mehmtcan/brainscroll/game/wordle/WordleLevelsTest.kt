package com.mehmtcan.brainscroll.game.wordle

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The levels of the word puzzle: 7, 6 or 5 guesses, and the hard mode at the hard level. */
class WordleLevelsTest {

    private val en = WordList(
        language = Language.EN,
        wordLength = 5,
        valid = listOf("crane", "slate", "ghost", "pound", "apple", "trace", "crate", "caper", "react"),
        answers = listOf("crane"),
    )

    private fun hard(answer: String = "CRANE") = WordleGame(en, answer, maxAttempts = 5, hardMode = true)

    private fun WordleGame.guess(word: String) = perform(WordleAction.Submit(word))

    // --- the hard mode ---------------------------------------------------------------------------------------------

    @Test
    fun withoutHardModeAnyWordGoes() {
        val g = WordleGame(en, "CRANE")
        g.guess("slate")
        g.guess("ghost")
        assertNull(g.lastError)
        assertEquals(2, g.state.rows.size)
    }

    @Test
    fun hardModeRefusesAGuessThatMovesAGreenLetter() {
        val g = hard()
        g.guess("slate") // A in place 3 and E in place 5 are green
        g.guess("ghost")
        assertEquals(GuessError.HardMode, g.lastError)
        assertEquals(1, g.state.rows.size, "a refused guess does not use an attempt")
    }

    @Test
    fun hardModeRefusesAGuessThatLeavesOutAYellowLetter() {
        val g = hard()
        g.guess("trace") // T absent, R present, A present, C present, E green
        g.guess("pound")
        assertEquals(GuessError.HardMode, g.lastError)
        g.guess("caper") // has C, R, A and E, but the E of trace is green in place 5
        assertEquals(GuessError.HardMode, g.lastError, "caper moves the green E")
        g.guess("crate") // C, R, A, T and E in place 5
        assertNull(g.lastError)
    }

    @Test
    fun hardModeAcceptsAGuessThatUsesEveryHint() {
        val g = hard()
        g.guess("trace")
        g.guess("crane")
        assertNull(g.lastError)
        assertEquals(WordleStatus.Won, g.state.status)
    }

    @Test
    fun aRefusedHardModeGuessIsTheOnlyThingThatChangesNothing() {
        val g = hard()
        g.guess("slate")
        val before = g.state
        g.guess("ghost")
        assertEquals(before, g.state)
    }

    @Test
    fun hardModeDoesNotRefuseAnythingBeforeTheFirstGuess() {
        val g = hard()
        g.guess("ghost")
        assertNull(g.lastError)
    }

    // --- the levels ------------------------------------------------------------------------------------------------

    @Test
    fun theLevelsGiveSevenSixAndFiveGuesses() {
        assertEquals(GameLevels.WordShape(7, false), GameLevels.word(Difficulty.Easy))
        assertEquals(GameLevels.WordShape(6, false), GameLevels.word(Difficulty.Medium))
        assertEquals(GameLevels.WordShape(5, true), GameLevels.word(Difficulty.Hard))
    }

    private class Store : FeedStore {
        val progress = linkedMapOf<String, StoredRound>()
        override fun saveProgress(round: StoredRound) { progress[round.id] = round }
        override fun deleteProgress(id: String) { progress.remove(id) }
        override fun finish(result: FinishedRound) { progress.remove(result.id) }
    }

    private val lists = mapOf(
        Language.EN to en,
        Language.TR to WordList(Language.TR, 5, valid = listOf("kitap"), answers = listOf("kitap")),
    )

    private fun feed(level: Difficulty, store: FeedStore = Store(), restored: List<StoredRound> = emptyList(), levelOfFeed: Difficulty = level) =
        EndlessFeed(lists, Language.EN, random = Random(5), store = store, restored = restored, games = listOf(GameKind.Word), difficulties = mapOf(GameKind.Word to levelOfFeed))

    @Test
    fun aPageHasTheGuessesOfItsLevel() {
        for ((level, attempts) in listOf(Difficulty.Easy to 7, Difficulty.Medium to 6, Difficulty.Hard to 5)) {
            val f = feed(level)
            f.ensureSize(2)
            assertEquals(List(2) { attempts }, f.snapshot().rounds.map { it.maxAttempts }, "$level")
        }
    }

    @Test
    fun aRestoredPageKeepsTheGuessesAndTheHardModeOfTheLevelItWasMadeFor() {
        val store = Store()
        val f = feed(Difficulty.Hard, store)
        f.ensureSize(1)
        f.type(0, 'S'); f.type(0, 'L'); f.type(0, 'A'); f.type(0, 'T'); f.type(0, 'E')
        f.submit(0)
        val saved = store.progress.values.single()
        assertEquals(Difficulty.Hard, saved.difficulty)

        // The player has switched to the easy level since; the saved page is still a hard one.
        val back = feed(Difficulty.Easy, restored = listOf(saved), levelOfFeed = Difficulty.Easy)
        assertEquals(5, back.snapshot().rounds.first().maxAttempts)
        for (c in "GHOST") back.type(0, c)
        back.submit(0)
        assertEquals(GuessError.HardMode, back.snapshot().rounds.first().error, "the hard mode still applies")
        assertEquals(7, back.also { it.ensureSize(2) }.snapshot().rounds[1].maxAttempts, "a new page is easy")
    }

    @Test
    fun theHardPageLosesAfterFiveGuesses() {
        val f = feed(Difficulty.Hard)
        f.ensureSize(1)
        // "ghost" shares no letter with the answer, so no hint is given and hard mode has nothing to refuse.
        repeat(5) {
            for (c in "GHOST") f.type(0, c)
            f.submit(0)
        }
        val page = f.snapshot().rounds.first()
        assertEquals(WordleStatus.Lost, page.status)
        assertEquals(5, page.rows.size)
    }
}
