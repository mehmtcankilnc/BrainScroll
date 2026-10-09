package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
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

    private fun ComposeUiTest.sheetShown() = onAllNodesWithTag("howToSheet").fetchSemanticsNodes().isNotEmpty()

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
        clickAny("Anladım", "Got it")
        waitForIdle()
    }

    @Test
    fun theWordPuzzleHasTheButtonAndItsRules() = runComposeUiTest {
        start()
        snap("110_howto_button_word")
        assertTrue(hasAnyText("Nasıl Oynanır?", "How to play?"))
        pressHowTo()
        snap("111_howto_word")
        assertTrue(sheetShown())
        assertTrue(hasAnyText("Nasıl oynanır: Kelime bulmacası", "How to play: Word puzzle"))
        close()
        assertFalse(sheetShown())
    }

    @Test
    fun memoryMatchHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Memory)
        pressHowTo()
        snap("112_howto_memory")
        assertTrue(hasAnyText("Nasıl oynanır: Hafıza eşleştirme", "How to play: Memory match"))
        close()
        assertFalse(sheetShown())
    }

    @Test
    fun colorTubesHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Tubes)
        pressHowTo()
        snap("113_howto_tubes")
        assertTrue(hasAnyText("Nasıl oynanır: Renk tüpleri", "How to play: Color tubes"))
        close()
        assertFalse(sheetShown())
    }

    @Test
    fun minesweeperHasItsRules() = runComposeUiTest {
        openOnSecondPage(GameKind.Mines)
        snap("114_howto_button_mines")
        pressHowTo()
        snap("115_howto_mines")
        assertTrue(hasAnyText("Nasıl oynanır: Mayın tarlası", "How to play: Minesweeper"))
        close()
        assertFalse(sheetShown())
    }

    @Test
    fun tappingOutsideThePanelClosesItAndTheGameBelowIsNotTouched() = runComposeUiTest {
        openOnSecondPage(GameKind.Tubes)
        pressHowTo()
        assertTrue(sheetShown())
        // The scrim covers the page: a tap on the tubes behind it only closes the panel, it does not select a tube.
        onAllNodesWithTag("tube")[0].performClick()
        waitForIdle()
        assertFalse(sheetShown())
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
        onAllNodesWithContentDescription("Ana Sayfa", ignoreCase = true).onFirst().performClick()
        waitForIdle()
        assertFalse(sheetShown())
        assertTrue(hasAnyText("Kelime bulmacası", "Word puzzle"), "still in the feed")
    }
}
