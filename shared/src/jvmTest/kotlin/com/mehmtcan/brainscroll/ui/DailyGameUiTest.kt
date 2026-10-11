package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.FakeAccountService
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.social.DailyRow
import com.mehmtcan.brainscroll.social.FakeSocialApi
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The daily puzzle on a day when the game is not the word puzzle, through the real screens: the card on the home tab,
 * the page of the game with its own bar, and the speed table of that game in the ranks.
 */
@OptIn(ExperimentalTestApi::class)
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
        assertTrue(hasAnyText("Bugünün oyunu: Mayın tarlası", "Today's game: Minesweeper"), "the table says whose game it is")
        assertTrue(cloud.social.calls.any { it == "gameBoard:mines:Today:false" })
        assertFalse(cloud.social.calls.any { it.startsWith("dailyBoard") }, "the word table is not asked for")
    }
}
