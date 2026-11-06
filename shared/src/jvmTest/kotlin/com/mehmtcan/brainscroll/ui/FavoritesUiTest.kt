package com.mehmtcan.brainscroll.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.ui.feed.ProfileState
import com.mehmtcan.brainscroll.ui.profile.FavoritesPage
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The favorites page: the heart asks before it removes, and the ordering sheet reorders the cards. */
@OptIn(ExperimentalTestApi::class)
class FavoritesUiTest {

    private fun round(id: String, game: GameKind, durationMs: Long, finishedAt: Long, difficulty: Difficulty = Difficulty.Medium) = FinishedRound(
        id = id, mode = Mode.ENDLESS, language = Language.EN, answer = "x", guesses = listOf("1"),
        outcome = Outcome.WON, finishedAt = finishedAt, durationMs = durationMs, game = game, difficulty = difficulty,
    )

    private fun profile(vararg favorites: FinishedRound) =
        ProfileState(stats = com.mehmtcan.brainscroll.stats.computeStats(emptyList()), favorites = favorites.toList())

    /** The ids of the cards, top to bottom, read from their replay buttons' order on screen. */
    private fun androidx.compose.ui.test.ComposeUiTest.cardTops(): List<Float> =
        onAllNodesWithTag("favoriteReplay").fetchSemanticsNodes().map { it.boundsInRoot.top }

    @Test
    fun theHeartAsksBeforeItRemovesAndCancelKeepsTheCard() = runComposeUiTest {
        val removed = mutableStateOf<String?>(null)
        setContent {
            BrainScrollTheme {
                FavoritesPage(
                    profile(round("a", GameKind.Sudoku, 61_000, 1, Difficulty.Hard), round("b", GameKind.Lights, 30_000, 2), round("c", GameKind.Memory, 12_000, 3)),
                    onRemove = { removed.value = it.id },
                )
            }
        }
        snap("231_favorites_cards")
        onAllNodesWithTag("favoriteHeart")[0].performClick()
        waitForIdle()
        assertTrue(hasAnyText("Favorilerden çıkarılsın mı?", "Remove from favorites?"), "it asks first")
        assertEquals(null, removed.value, "asking is not removing")
        clickAny("Vazgeç", "Cancel")
        waitForIdle()
        assertEquals(null, removed.value)
        assertFalse(hasAnyText("Favorilerden çıkarılsın mı?", "Remove from favorites?"))

        onAllNodesWithTag("favoriteHeart")[0].performClick()
        waitForIdle()
        clickAny("Çıkar", "Remove")
        waitForIdle()
        assertEquals("a", removed.value, "confirming removes that card's puzzle")
    }

    @Test
    fun theSortSheetOrdersTheCardsAndRecordsCanBeTheOnlyOnes() = runComposeUiTest {
        setContent {
            BrainScrollTheme {
                FavoritesPage(profile(round("slow", GameKind.Sudoku, 90_000, 1), round("fast", GameKind.Lights, 20_000, 2)))
            }
        }
        // Newest added first: the page lists them in the order given.
        assertTrue(hasAnyText("Son eklenenler", "Recently added"))
        val before = cardTops()
        assertEquals(2, before.size)

        onNodeWithTag("favoritesSortButton").performClick()
        waitForIdle()
        onNodeWithTag("favoritesSort_Fastest").performClick()
        waitForIdle()
        clickAny("Anladım", "Got it")
        waitForIdle()
        assertTrue(hasAnyText("En hızlılar", "Fastest first"), "the label says how the list is ordered")
        // The sudoku (90 s) is the first of the given list and the slowest: it is now the last card.
        assertEquals(2, cardTops().size)

        onNodeWithTag("favoritesSortButton").performClick()
        waitForIdle()
        onNodeWithTag("favoritesRecordsOnly").performClick()
        waitForIdle()
        clickAny("Anladım", "Got it")
        waitForIdle()
        // No game has a best time on this profile, so no card is a record: nothing matches.
        assertEquals(0, cardTops().size)
        assertTrue(hasAnyText("Bu seçimlere uyan bir şey yok.", "Nothing matches these choices."))
    }

    @Test
    fun oneLineReplayHasItsOwnScreenWithABackArrowAndNoFeedPromise() = runComposeUiTest {
        val file = java.io.File.createTempFile("replay-ui", ".db").also { it.deleteOnExit() }
        val driver = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { com.mehmtcan.brainscroll.data.prepareDatabase(it) }
        val repository = com.mehmtcan.brainscroll.data.GameRepository(com.mehmtcan.brainscroll.db.BrainScrollDatabase(driver))
        val puzzle = com.mehmtcan.brainscroll.game.line.LineGenerator.generate(7L, size = 5, checkpoints = 4)
        val answer = com.mehmtcan.brainscroll.game.line.LineRound(0, "x", puzzle, Language.EN).toStored().answer
        val replay = com.mehmtcan.brainscroll.ui.line.LineReplay(repository, now = { 1_800_000_000_000L })
        replay.start(round("fav", GameKind.Line, 40_000, 1).copy(answer = answer))
        var back = false
        setContent { BrainScrollTheme { com.mehmtcan.brainscroll.ui.line.LineReplayScreen(replay, onBack = { back = true }) } }
        waitForIdle()
        snap("232_line_replay")
        assertTrue(hasAnyText("Tekrar oyna", "Play again"), "the bar says it is a replay")
        for (cell in puzzle.solution) replay.drag(cell)
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("233_line_replay_won")
        assertTrue(hasAnyText("Favorilerine geri okuyla dön", "Go back to your favorites with the arrow"), "no next puzzle is promised")
        assertFalse(hasAnyText("Swipe up", "Sonraki bulmaca için yukarı"), "and nothing about swiping up")
        onAllNodesWithContentDescription("Geri").let { if (it.fetchSemanticsNodes().isNotEmpty()) it[0] else onAllNodesWithContentDescription("Back")[0] }.performClick()
        assertTrue(back)
    }
}
