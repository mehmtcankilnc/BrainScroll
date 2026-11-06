package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The "How to play?" button on every game page and the panel it opens. */
@OptIn(ExperimentalTestApi::class)
class HowToPlayUiTest {

    /** The card of steps is shown (every game has one now). */
    private fun ComposeUiTest.sheetShown() = onAllNodesWithTag("howToCard").fetchSemanticsNodes().isNotEmpty()

    /** The button of the page that is on screen (the pages next to it are composed too, but have no size on screen). */
    private fun ComposeUiTest.pressHowTo() {
        val buttons = onAllNodesWithTag("howToButton")
        val visible = buttons.fetchSemanticsNodes().indexOfFirst { it.boundsInRoot.width > 0f }
        check(visible >= 0) { "no button on screen" }
        buttons[visible].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.openOnSecondPage(game: GameKind) {
        start(games = listOf(GameKind.Word, game))
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
    }

    private fun ComposeUiTest.close() {
        tapThroughHowTo()
        mainClock.advanceTimeBy(500)
        waitForIdle()
    }

    @Test
    fun theWordPuzzleHasTheButtonAndItsRules() = runComposeUiTest {
        start()
        snap("110_howto_button_word")
        assertTrue(onAllNodesWithTag("howToButton").fetchSemanticsNodes().isNotEmpty(), "the rules button is in the bar")
        pressHowTo()
        snap("111_howto_word")
        assertTrue(sheetShown())
        close()
        assertFalse(sheetShown())
    }

    @Test
    fun memoryMatchHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Memory)
        pressHowTo()
        snap("112_howto_memory")
        close()
        assertFalse(sheetShown())
    }

    @Test
    fun colorTubesHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Tubes)
        pressHowTo()
        snap("113_howto_tubes")
        // The game has the card of steps now, not the sheet of text.
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isNotEmpty())
        tapThroughHowTo()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun minesweeperHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Mines)
        snap("114_howto_button_mines")
        pressHowTo()
        snap("115_howto_mines")
        // The game has the card of steps now, not the sheet of text.
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isNotEmpty())
        tapThroughHowTo()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun sudokuHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Sudoku)
        pressHowTo()
        snap("125_howto_sudoku")
        // Sudoku has the card of steps now, not the sheet of text.
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isNotEmpty())
        assertTrue(hasAnyText("NASIL OYNANIR", "HOW TO PLAY"))
        tapThroughHowTo()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun lightsOutHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Lights)
        pressHowTo()
        snap("133_howto_lights")
        // The game has the card of steps now, not the sheet of text.
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isNotEmpty())
        tapThroughHowTo()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun slidingPuzzleHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Slide)
        pressHowTo()
        snap("143_howto_slide")
        // The game has the card of steps now, not the sheet of text.
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isNotEmpty())
        tapThroughHowTo()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun targetNumberHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Target)
        pressHowTo()
        snap("154_howto_target")
        // The game has the card of steps now, not the sheet of text.
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isNotEmpty())
        tapThroughHowTo()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun pipesHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Pipes)
        pressHowTo()
        snap("163_howto_pipes")
        // The game has the card of steps now, not the sheet of text.
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isNotEmpty())
        tapThroughHowTo()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun tappingOutsideThePanelClosesItAndTheGameBelowIsNotTouched() = runComposeUiTest {
        openOnSecondPage(GameKind.Tubes)
        pressHowTo()
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isNotEmpty())
        // The scrim covers the page: a tap on the tubes behind it only closes the panel, it does not select a tube.
        onRoot().performTouchInput { click(androidx.compose.ui.geometry.Offset(4f, 4f)) }
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("howToCard").fetchSemanticsNodes().isEmpty())
        assertTrue(onAllNodesWithTag("tube").fetchSemanticsNodes().none { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()?.let {
                it.contains("seçili") || it.contains("selected")
            } == true
        })
    }

    @Test
    fun thePanelCoversTheBarAtTheTopToo() = runComposeUiTest {
        start()
        pressHowTo()
        snap("116_howto_covers_bar")
        assertTrue(sheetShown())
        // The scrim is over the bar as well: a tap on the home arrow only closes the panel and stays in the feed.
        val home = if (onAllNodesWithContentDescription("Ana Sayfa", ignoreCase = true).fetchSemanticsNodes().isNotEmpty()) "Ana Sayfa" else "Home"
        onAllNodesWithContentDescription(home, ignoreCase = true).onFirst().performClick()
        waitForIdle()
        assertFalse(sheetShown())
        assertTrue(hasAnyText("Kelime bulmacası", "Word puzzle"), "still in the feed")
    }
}
