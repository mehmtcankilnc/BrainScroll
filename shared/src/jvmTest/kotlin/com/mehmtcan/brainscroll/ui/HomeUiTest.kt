package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The home tab, the feed editor and the pages around them (docs/decisions.md, "Ana Sayfa yenilemesi"). */
@OptIn(ExperimentalTestApi::class)
class HomeUiTest {

    private val both = listOf(GameKind.Word, GameKind.Memory)

    private fun androidx.compose.ui.test.ComposeUiTest.cards() = onAllNodesWithTag("gameCard").fetchSemanticsNodes().size

    @Test
    fun theAppOpensOnTheHomeTabWithItsSections() = runComposeUiTest {
        start(games = both, openFeed = false)
        snap("20_home")

        assertTrue(hasAnyText("Bugünün bulmacası", "Today's puzzle"))
        assertTrue(hasAnyText("Akışın", "Your feed"))
        assertTrue(hasAnyText("Bugün en hızlılar", "Fastest today"))
        assertTrue(hasAnyText("Akışa başla", "Start your feed"))
        // The game cards are gone: the feed is entered through its tab or the start button.
        assertFalse(hasAnyText("Hemen oyna", "Play now"))
        // The feed card shows the pictures of the games that are on.
        assertEquals(2, onAllNodesWithTag("feedThumbnail").fetchSemanticsNodes().size)
    }

    /** Every game of the feed: the one-line game is the daily puzzle only and has no card there. */
    private val feedGames = GameKind.entries - GameKind.Line

    @Test
    fun withManyGamesTheFeedCardShowsThreeAndCountsTheRest() = runComposeUiTest {
        start(games = feedGames, openFeed = false)
        snap("22_home_all_games")

        assertEquals(3, onAllNodesWithTag("feedThumbnail").fetchSemanticsNodes().size)
        val rest = feedGames.size - 3
        assertTrue(hasAnyText("+$rest oyun", "+$rest games"))
    }

    @Test
    fun editOpensTheFeedEditorWithEveryGameAsACard() = runComposeUiTest {
        start(games = feedGames, openFeed = false)

        clickAny("Akışın", "Your feed")
        waitForIdle()
        snap("23_feed_editor")

        assertTrue(hasAnyText("Akışını oluştur", "Build your feed"))
        assertTrue(hasAnyText("${feedGames.size} oyun seçili", "${feedGames.size} games selected"))
        assertEquals(feedGames.size, cards())
        onAllNodesWithTag("gameCard").onLast().performScrollTo()
        waitForIdle()
        snap("24_feed_editor_end")
        // The pictures are there for every game, and the way back is the back button.
        goHome()
        assertTrue(hasAnyText("Akışın", "Your feed"))
    }

    @Test
    fun aCardSwitchesAGameOffAndOnAndTheCountFollows() = runComposeUiTest {
        start(games = both, openFeed = false)
        clickAny("Akışın", "Your feed")
        waitForIdle()
        assertTrue(hasAnyText("2 oyun seçili", "2 games selected"))

        val memory = if (hasAnyText("Hafıza eşleştirme")) "Hafıza eşleştirme" else "Memory match"
        onAllNodesWithText(memory).onLast().performClick()
        waitForIdle()
        assertTrue(hasAnyText("1 oyun seçili", "1 game selected", "1 games selected"))

        onAllNodesWithText(memory).onLast().performClick()
        waitForIdle()
        assertTrue(hasAnyText("2 oyun seçili", "2 games selected"))
    }

    @Test
    fun theLastGameCannotBeSwitchedOff() = runComposeUiTest {
        start(games = both, openFeed = false)
        clickAny("Akışın", "Your feed")
        waitForIdle()

        // Memory off is fine; then Word is the last one and its card does nothing any more.
        val memory = if (hasAnyText("Hafıza eşleştirme")) "Hafıza eşleştirme" else "Memory match"
        val word = if (hasAnyText("Kelime bulmacası")) "Kelime bulmacası" else "Word puzzle"
        onAllNodesWithText(memory).onLast().performClick()
        waitForIdle()
        onAllNodesWithText(word).onLast().performClick()
        waitForIdle()

        assertTrue(hasAnyText("En az bir oyun açık kalır", "At least one game stays on"))
        assertTrue(hasAnyText("1 oyun seçili", "1 game selected", "1 games selected"))
        snap("21_feed_editor_last_game")
    }

    @Test
    fun theStartButtonOpensTheFeed() = runComposeUiTest {
        start(games = both, openFeed = false)

        onNodeWithTag("startFeedHome").performClick()
        waitUntil(timeoutMillis = 10_000) { hasAnyText("ENTER", "GİR") }

        assertTrue(hasAnyText("Profil", "Profile"), "the feed keeps the tab bar")
    }

    @Test
    fun theStartButtonOfTheEditorOpensTheFeedToo() = runComposeUiTest {
        start(games = both, openFeed = false)
        clickAny("Akışın", "Your feed")
        waitForIdle()

        onNodeWithTag("startFeed").performClick()
        waitUntil(timeoutMillis = 10_000) { hasAnyText("ENTER", "GİR") }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.switchOffWordInTheEditor() {
        clickAny("Akışın", "Your feed")
        waitForIdle()
        val word = if (hasAnyText("Kelime bulmacası")) "Kelime bulmacası" else "Word puzzle"
        onAllNodesWithText(word).onLast().performClick()
        waitForIdle()
        assertTrue(hasAnyText("1 oyun seçili", "1 game selected", "1 games selected"))
    }

    private fun androidx.compose.ui.test.ComposeUiTest.memoryCards() = onAllNodesWithTag("memoryCard").fetchSemanticsNodes().size

    /**
     * True when a memory page is the one on show: its title is near the top of the screen. (A page that is composed
     * but not shown reports its place as zero.)
     */
    private fun androidx.compose.ui.test.ComposeUiTest.memoryPageOnShow(): Boolean =
        listOf("Hafıza eşleştirme", "Memory match").any { title ->
            onAllNodesWithText(title).fetchSemanticsNodes().any { it.boundsInRoot.center.y in 1f..200f }
        }

    @Test
    fun startingTheFeedWithTheWordPuzzleSwitchedOffOpensAnotherGame() = runComposeUiTest {
        start(games = both, openFeed = false)
        switchOffWordInTheEditor()

        onNodeWithTag("startFeed").performClick()
        waitUntil(timeoutMillis = 10_000) { memoryCards() > 0 }

        assertTrue(memoryPageOnShow(), "the memory board is the page on show")
    }

    @Test
    fun aWordPuzzleStartedEarlierIsForgottenWhenANewFeedWithoutItsGameIsStarted() = runComposeUiTest {
        start(games = both)
        // Start a word puzzle: one letter makes it a puzzle in progress, which the feed keeps.
        key('A')
        goHome()

        switchOffWordInTheEditor()
        onNodeWithTag("startFeed").performClick()
        waitForIdle()
        onNodeWithTag("confirmYes").performClick() // the half done puzzle is forgotten: asked first
        waitUntil(timeoutMillis = 10_000) { memoryCards() > 0 }

        assertTrue(memoryPageOnShow(), "the new feed has no word puzzle, it opens on a memory board")
    }

    @Test
    fun theEditorHasItsTitleAndTheCountOnTheLineOfTheBackButton() = runComposeUiTest {
        start(games = both, openFeed = false)
        clickAny("Akışın", "Your feed")
        waitForIdle()

        val back = onAllNodesWithContentDescription("Geri").fetchSemanticsNodes().ifEmpty { onAllNodesWithContentDescription("Back").fetchSemanticsNodes() }.single()
        val title = onAllNodesWithText("Akışını oluştur").fetchSemanticsNodes().ifEmpty { onAllNodesWithText("Build your feed").fetchSemanticsNodes() }.single()
        val count = onNodeWithTag("editorSelectedCount").fetchSemanticsNode()
        fun middle(node: androidx.compose.ui.semantics.SemanticsNode) = node.boundsInRoot.center.y
        assertTrue(kotlin.math.abs(middle(back) - middle(title)) < 6f, "title and back button on one line")
        assertTrue(kotlin.math.abs(middle(count) - middle(title)) < 6f, "count and title on one line")
        snap("25_feed_editor_header")
    }

    @Test
    fun theStreaksSitInTheTopRightCorner() = runComposeUiTest {
        start(games = both, openFeed = false)
        val flame = onAllNodesWithContentDescription("Günlük seri", substring = true).fetchSemanticsNodes().ifEmpty {
            onAllNodesWithContentDescription("Daily streak", substring = true).fetchSemanticsNodes()
        }
        assertTrue(flame.isNotEmpty())
        val left = flame.minOf { it.boundsInRoot.left }
        assertTrue(left > 390f / 2 * 1.0f, "the streaks are in the right half of the screen")
        assertTrue(hasAnyText("BrainScroll"), "the name of the app fills the left of the line")
    }

    @Test
    fun theFeedKeepsTheTabBarAndTheChevronGoesHome() = runComposeUiTest {
        start(games = both)

        assertTrue(hasAnyText("Profil", "Profile"))
        goHome()

        assertTrue(hasAnyText("Bugünün bulmacası", "Today's puzzle"))
        assertTrue(hasAnyText("Profil", "Profile"))
    }

    @Test
    fun theDailyCardOpensAPageWithABackButton() = runComposeUiTest {
        start(games = both, openFeed = false)

        openTab("Günlük", "Daily")
        waitForIdle()
        assertFalse(hasAnyText("Profil", "Profile"))

        goHome()
        assertTrue(hasAnyText("Akışın", "Your feed"))
    }

    private fun androidx.compose.ui.test.ComposeUiTest.cLetters() = onAllNodes(hasText("C")).fetchSemanticsNodes().size

    @Test
    fun todaysPuzzleIsAboveTheFeedCard() = runComposeUiTest {
        start(games = both, openFeed = false)
        val daily = onNodeWithTag("dailyCard").fetchSemanticsNode().boundsInRoot.top
        val feed = onNodeWithTag("feedCard").fetchSemanticsNode().boundsInRoot.top
        assertTrue(daily < feed, "today's puzzle comes first, the feed card below it")
    }

    @Test
    fun theFeedCardHasAnEditLinkAndNoLanguageRowAndTheCardOpensTheEditor() = runComposeUiTest {
        start(games = both, openFeed = false)
        assertTrue(onAllNodesWithTag("editFeedLink").fetchSemanticsNodes().isNotEmpty(), "an edit link next to the title")
        assertFalse(hasAnyText("Kelime oyunlarının dili", "Word language"), "no language row")
        assertFalse(hasAnyText("→"), "no arrow on the start button")

        clickAny("Akışın", "Your feed")
        waitForIdle()
        assertTrue(hasAnyText("Akışını oluştur", "Build your feed"), "tapping the card opens the editor")
    }

    @Test
    fun thereIsNothingToContinueUntilAPuzzleIsStarted() = runComposeUiTest {
        start(games = both, openFeed = false)
        assertTrue(onAllNodesWithTag("continueFeed").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun continuingGoesBackToThePuzzleThatWasLeftHalfDone() = runComposeUiTest {
        start(games = both)
        key('C')
        key('R')
        goHome()

        assertTrue(onAllNodesWithTag("continueFeed").fetchSemanticsNodes().isNotEmpty(), "the button is offered")
        val feed = onNodeWithTag("feedCard").fetchSemanticsNode().boundsInRoot
        val button = onNodeWithTag("continueFeed").fetchSemanticsNode().boundsInRoot
        assertTrue(button.top >= feed.top && button.bottom <= feed.bottom, "the button is part of the feed card")

        onNodeWithTag("continueFeed").performClick()
        waitUntil(timeoutMillis = 10_000) { cLetters() > 0 }
        assertEquals(2, cLetters(), "the typed letter is back: the C key and the tile")
    }

    @Test
    fun startingANewFeedForgetsWhatWasLeftHalfDone() = runComposeUiTest {
        start(games = both)
        key('C')
        key('R')
        goHome()

        // A new feed is started from the editor: there is no second start button on the home page.
        assertTrue(onAllNodesWithTag("startFeedHome").fetchSemanticsNodes().isEmpty())
        onNodeWithTag("editFeedLink").performClick()
        waitForIdle()
        onNodeWithTag("startFeed").performClick()
        waitForIdle()
        assertTrue(onAllNodesWithTag("confirmDialog").fetchSemanticsNodes().isNotEmpty(), "a puzzle would be forgotten: ask first")
        assertTrue(hasAnyText("Yeni akış başlatılsın mı?", "Start a new feed?"))
        onNodeWithTag("confirmYes").performClick()
        waitUntil(timeoutMillis = 10_000) { cLetters() > 0 }
        assertEquals(1, cLetters(), "only the key: nothing was typed in the new feed")

        goHome()
        assertTrue(onAllNodesWithTag("continueFeed").fetchSemanticsNodes().isEmpty(), "and there is nothing to continue")
    }

    @Test
    fun theFeedTabGoesOnWithAPuzzleLeftHalfDone() = runComposeUiTest {
        start(games = both)
        key('C')
        goHome()
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { cLetters() > 0 }
        assertEquals(2, cLetters(), "the letter typed before is still there")
    }

    @Test
    fun theFeedTabMakesANewFeedFromTheChoicesWhenNothingIsHalfDone() = runComposeUiTest {
        start(games = both, openFeed = false)
        switchOffWordInTheEditor()
        goHome()

        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { memoryCards() > 0 }
        assertTrue(memoryPageOnShow(), "the feed is made from the games the player kept")
    }

    @Test
    fun cancellingTheQuestionKeepsTheFeedAsItWas() = runComposeUiTest {
        start(games = both)
        key('C')
        goHome()

        onNodeWithTag("editFeedLink").performClick()
        waitForIdle()
        onNodeWithTag("startFeed").performClick()
        waitForIdle()
        onNodeWithTag("confirmNo").performClick()
        waitForIdle()
        assertTrue(onAllNodesWithTag("confirmDialog").fetchSemanticsNodes().isEmpty(), "the question is gone")
        goHome()
        assertTrue(onAllNodesWithTag("continueFeed").fetchSemanticsNodes().isNotEmpty(), "and the half done puzzle is still there to continue")
    }

    @Test
    fun nothingIsAskedWhenThereIsNothingToLose() = runComposeUiTest {
        start(games = both, openFeed = false)
        onNodeWithTag("startFeedHome").performClick()
        waitUntil(timeoutMillis = 10_000) { cLetters() > 0 }
        assertTrue(onAllNodesWithTag("confirmDialog").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun theEditorAsksTooBeforeItStartsANewFeed() = runComposeUiTest {
        start(games = both)
        key('C')
        goHome()
        clickAny("Akışın", "Your feed")
        waitForIdle()
        onNodeWithTag("startFeed").performClick()
        waitForIdle()
        assertTrue(onAllNodesWithTag("confirmDialog").fetchSemanticsNodes().isNotEmpty())
        onNodeWithTag("confirmYes").performClick()
        waitUntil(timeoutMillis = 10_000) { cLetters() > 0 }
        assertEquals(1, cLetters(), "a new feed")
    }

    @Test
    fun theFeedTabKeepsTheFeedWhenThePuzzleItWasLeftOnIsNotFinished() = runComposeUiTest {
        start(games = both)
        // The second page, a memory board nobody touched yet: not finished, so the feed is kept where it was.
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitUntil(timeoutMillis = 10_000) { memoryCards() > 0 }
        assertTrue(memoryPageOnShow())
        goHome()

        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { memoryCards() > 0 }
        assertTrue(memoryPageOnShow(), "back on the memory board, not on a new feed's first page")
    }
}
