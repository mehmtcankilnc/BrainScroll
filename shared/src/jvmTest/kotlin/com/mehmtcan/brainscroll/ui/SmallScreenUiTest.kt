package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
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
    @Test fun hanoi() = shot(GameKind.Hanoi, "hanoi")
    @Test fun queens() = shot(GameKind.Queens, "queens")
    @Test fun tents() = shot(GameKind.Tents, "tents")
    @Test fun jigsaw() = shot(GameKind.Jigsaw, "jigsaw")
    @Test fun blockout() = shot(GameKind.BlockOut, "blockout")
    @Test fun crossword() = shot(GameKind.Crossword, "crossword")

    /** The towers and the restart button are both on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun hanoiFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Hanoi), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("300_small_hanoi_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("hanoiTower", "hanoiRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }

    /** The queens board and the restart button are both on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun queensFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Queens), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("301_small_queens_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("queensCell", "queensRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }

    /** The block out board and the restart button are both on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun blockOutFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.BlockOut), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("303_small_blockout_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("blockoutPiece", "blockoutCell", "blockoutRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }

    /** The crossword board, its clue and the keyboard are both on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun crosswordFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Crossword), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("304_small_crossword_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("crosswordSquare", "crosswordBanner")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }

    /** The tents board and the restart button are both on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun tentsFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Tents), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("302_small_tents_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("tentsCell", "tentsRestart", "tentsRowClue", "tentsColumnClue")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }
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
        // The texts follow the language of the machine (Turkish here, English on the CI runner).
        val gotIt = listOf("Anladım", "Got it").first { onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }
        val button = onAllNodesWithText(gotIt)[0].fetchSemanticsNode().boundsInRoot
        assertTrue(button.bottom <= sheet.bottom + 1f && button.top >= 0f, "the button of the sheet is in view")
    }
}
