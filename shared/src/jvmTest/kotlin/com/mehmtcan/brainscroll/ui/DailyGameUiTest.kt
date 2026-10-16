package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.FakeAccountService
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.crossword.CrosswordLayout
import com.mehmtcan.brainscroll.game.crossword.CrosswordPuzzle
import com.mehmtcan.brainscroll.game.crossword.CrosswordSchedule
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.crossword.CrosswordClueTexts
import com.mehmtcan.brainscroll.time.IstanbulDay
import kotlin.time.Clock
import com.mehmtcan.brainscroll.social.DailyRow
import com.mehmtcan.brainscroll.social.FakeSocialApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The daily puzzle on a day when the game is not the word puzzle, through the real screens: the card on the home tab,
 * the page of the game with its own bar, and the speed table of that game in the ranks.
 */
@OptIn(ExperimentalTestApi::class, kotlin.time.ExperimentalTime::class)
class DailyGameUiTest {

    private val minesDay: (Long) -> GameKind = { GameKind.Mines }

    @Test
    fun theHomeCardNamesTheGameOfTheDay() = runComposeUiTest {
        start(openFeed = false, dailyGame = minesDay)
        snap("200_daily_card_mines")
        assertTrue(hasAnyText("Mayın tarlası", "Minesweeper"), "the card shows today's game")
        assertTrue(hasAnyText("Bugün herkes aynı tahtayı çözüyor", "Everyone solves the same board today"))
        assertFalse(hasAnyText("TR English", "Türkçe English"), "no language to pick for the daily puzzle")
    }

    @Test
    fun theCardOpensTheGameWithItsOwnBar() = runComposeUiTest {
        start(openFeed = false, dailyGame = minesDay)
        onNodeWithTag("dailyCard").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("mineCell").fetchSemanticsNodes().size == 64 }
        snap("201_daily_game_mines")

        assertTrue(onAllNodesWithTag("dailyGameTitle").fetchSemanticsNodes().isNotEmpty(), "the bar names the game")
        assertTrue(onAllNodesWithTag("howToButton").fetchSemanticsNodes().isNotEmpty(), "and has the rules button")
        assertTrue(onAllNodesWithTag("dayStreak").fetchSemanticsNodes().isNotEmpty(), "and the day streak")

        // The board plays like in the feed.
        onAllNodesWithTag("mineCell")[27].performClick()
        waitForIdle()
        assertTrue(hasAnyText("Mayın") || hasAnyText("Mines"))
    }

    @Test
    fun backGoesHomeAndTheProgressStays() = runComposeUiTest {
        start(openFeed = false, dailyGame = minesDay)
        onNodeWithTag("dailyCard").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("mineCell").fetchSemanticsNodes().size == 64 }
        onAllNodesWithTag("mineCell")[27].performClick()
        waitForIdle()
        goHome()
        assertTrue(hasAnyText("Bugünün bulmacası", "Today's puzzle"))

        onNodeWithTag("dailyCard").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("mineCell").fetchSemanticsNodes().size == 64 }
        // The opened area is still there (open cells have a number or are empty, not closed).
        assertTrue(onAllNodesWithTag("mineCell").fetchSemanticsNodes().size == 64)
    }

    @Test
    fun theSpeedTableOfTodayIsTheTableOfTodaysGame() = runComposeUiTest {
        val google = AccountState.SignedIn("user-1", AccountState.SignedIn.Kind.Google, "a@b.c")
        val cloud = TestCloud(
            account = FakeAccountService(google),
            social = FakeSocialApi(member = true, username = "carol").apply {
                gameToday = listOf(DailyRow(1, "alice", 31, 42_000), DailyRow(2, "carol", 40, 55_000, isMe = true))
            },
        )
        start(cloud, openFeed = false, dailyGame = minesDay)
        openTab("Sıralama", "Ranks")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("alice") }
        snap("202_ranks_game_table")

        assertTrue(hasAnyText("31 hamle", "31 moves"), "moves, not guesses")
        assertTrue(hasAnyText("00:42"))
        assertTrue(hasAnyText("Bugün · Mayın tarlası", "Today · Minesweeper"), "the filter line says whose game it is")
        assertTrue(cloud.social.calls.any { it == "gameBoard:mines:Today:false" })
        assertFalse(cloud.social.calls.any { it.startsWith("dailyBoard") }, "the word table is not asked for")
    }

    private val crosswordDay: (Long) -> GameKind = { GameKind.Crossword }

    private fun ComposeUiTest.squareCount() = onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes().size

    /** Today's crossword, found the way a player would: the clues on the screen say which language's board it is. */
    private fun ComposeUiTest.todaysCrossword(): CrosswordPuzzle {
        val day = IstanbulDay.dayIndex(Clock.System.now().toEpochMilliseconds())
        val shown = onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes().flatMap { it.config.getOrNull(CrosswordClueTexts).orEmpty() }.map { it.drop(2) }.toSet()
        return Language.entries.map { CrosswordSchedule.puzzleFor(day, it) }.first { board -> board.clues.all { it in shown } }
    }

    private fun ComposeUiTest.pressKey(letter: Char) {
        val screen = onRoot().fetchSemanticsNode().boundsInRoot
        val nodes = onAllNodes(hasText(letter.toString()) and hasClickAction()).fetchSemanticsNodes()
        val at = nodes.indices.filter { nodes[it].boundsInRoot.let { b -> b.top >= 0f && b.bottom <= screen.bottom && b.width > 0f } }.maxBy { nodes[it].boundsInRoot.top }
        onAllNodes(hasText(letter.toString()) and hasClickAction())[at].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.solveCrossword(board: CrosswordPuzzle) {
        for (cell in board.squares.indices) {
            if (board.isClue[cell]) continue
            onAllNodesWithTag("crosswordSquare")[cell].performSemanticsAction(SemanticsActions.OnClick)
            mainClock.advanceTimeBy(1_000)
            waitForIdle()
            pressKey(board.squares[cell])
        }
        mainClock.advanceTimeBy(2_500)
        waitForIdle()
    }

    @Test
    fun theHomeCardOffersTheCrosswordAndItsBoardIsPlayedOnItsPageWithStars() = runComposeUiTest {
        start(openFeed = false, dailyGame = crosswordDay)
        snap("210_daily_card_crossword")
        assertTrue(hasAnyText("Çapraz Bulmaca", "Crossword"), "the card shows today's game")
        onNodeWithTag("dailyCard").performClick()
        waitUntil(timeoutMillis = 10_000) { squareCount() >= CrosswordLayout.CELLS }
        snap("211_daily_crossword_start")
        assertTrue(onAllNodesWithTag("dailyGameTitle").fetchSemanticsNodes().isNotEmpty())
        assertTrue(onAllNodesWithTag("starsLive").fetchSemanticsNodes().isNotEmpty(), "the daily board shows its stars")

        solveCrossword(todaysCrossword())
        snap("212_daily_crossword_solved")
        assertTrue(hasAnyText("Bulmaca çözüldü!", "Crossword solved!"))
        assertTrue(onAllNodesWithTag("crosswordShare").fetchSemanticsNodes().isNotEmpty(), "the solved board offers to share the result")
        assertTrue(onAllNodesWithTag("stars").fetchSemanticsNodes().isNotEmpty(), "and shows the stars it earned")
    }

    @Test
    fun afterSolvingTheCardShowsTheStarsAndTheCountdownAndTheBoardOpensSolved() = runComposeUiTest {
        start(openFeed = false, dailyGame = crosswordDay)
        onNodeWithTag("dailyCard").performClick()
        waitUntil(timeoutMillis = 10_000) { squareCount() >= CrosswordLayout.CELLS }
        solveCrossword(todaysCrossword())
        goHome()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("crosswordDone", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        snap("213_daily_card_crossword_done")
        assertTrue(hasAnyText("Sıradaki bulmaca", "Next puzzle in"), "the time to the next puzzle is on the card")

        onNodeWithTag("dailyCard").performClick()
        waitUntil(timeoutMillis = 5_000) { squareCount() >= CrosswordLayout.CELLS }
        assertTrue(hasAnyText("Bulmaca çözüldü!", "Crossword solved!"))
        assertTrue(onAllNodesWithTag("keyBackspace").fetchSemanticsNodes().isEmpty(), "a solved board is not played again")
    }
}
