package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertTrue

/** Every game on a short, narrow screen with big text: the board must still be there and be usable, nothing squeezed to a sliver. */
@OptIn(ExperimentalTestApi::class)
class SmallScreenUiTest {

    private fun shot(kind: GameKind, name: String) = runComposeUiTest {
        start(games = listOf(kind), openFeed = false, fontScale = 1.3f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("300_small_$name")
    }

    @Test fun memory() = shot(GameKind.Memory, "memory")
    @Test fun tubes() = shot(GameKind.Tubes, "tubes")
    @Test fun mines() = shot(GameKind.Mines, "mines")
    @Test fun sudoku() = shot(GameKind.Sudoku, "sudoku")
    @Test fun lights() = shot(GameKind.Lights, "lights")
    @Test fun slide() = shot(GameKind.Slide, "slide")
    @Test fun target() = shot(GameKind.Target, "target")
    @Test fun pipes() = shot(GameKind.Pipes, "pipes")
    @Test fun word() = shot(GameKind.Word, "word")

    @Test
    fun theRulesSheetFitsAShortScreenWithBigText() = runComposeUiTest {
        start(fontScale = 1.5f, screen = 360 to 600)
        onAllNodesWithTag("howToButton")[0].performClick()
        waitForIdle()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("301_small_howto")
        val sheet = onAllNodesWithTag("howToSheet")[0].fetchSemanticsNode().boundsInRoot
        // The "Got it" button is on screen, whatever the text size.
        val button = onAllNodesWithText("Anladım")[0].fetchSemanticsNode().boundsInRoot
        assertTrue(button.bottom <= sheet.bottom + 1f && button.top >= 0f, "the button of the sheet is in view")
    }
}
