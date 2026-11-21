package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeDown
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.flashgrid.FlashFoundKey
import com.mehmtcan.brainscroll.ui.flashgrid.FlashPatternKey
import com.mehmtcan.brainscroll.ui.flashgrid.FlashPhaseKey
import com.mehmtcan.brainscroll.ui.flashgrid.FlashRoundsKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The flash grid page in the feed, driven through the real screen. The tiles are random, so the tests read them the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class FlashGridUiTest {

    private val games = listOf(GameKind.Word, GameKind.FlashGrid)

    /** Opens the page; the run is not started (the start button is over the grid). */
    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("flashBoard").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show.
    private fun ComposeUiTest.board() = onAllNodesWithTag("flashBoard").fetchSemanticsNodes().first().config

    private fun ComposeUiTest.phase() = board()[FlashPhaseKey]

    private fun ComposeUiTest.pressStart() {
        onAllNodesWithTag("flashStart")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    /** Lets the clock run until the grid waits for the touches. */
    private fun ComposeUiTest.untilGuess() {
        repeat(40) {
            if (phase() == "Guess") return
            mainClock.advanceTimeBy(250)
            waitForIdle()
        }
        assertEquals("Guess", phase())
    }

    private fun ComposeUiTest.tapTile(tile: Int) {
        onAllNodesWithTag("flashTile$tile")[0].performClick()
        mainClock.advanceTimeBy(50)
        waitForIdle()
    }

    private fun ComposeUiTest.pattern() = board()[FlashPatternKey]

    private fun ComposeUiTest.lost() = hasAnyText("Bu kare yanmamıştı", "That tile was not lit")

    private fun ComposeUiTest.wrongTile() = (0 until 16).first { it !in pattern() }

    private fun ComposeUiTest.hasStartButton() = onAllNodesWithTag("flashStart").fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theGridWaitsForTheStartButtonAndShowsNothingUntilItIsPressed() = runComposeUiTest {
        openBoard()
        mainClock.advanceTimeBy(5_000)
        waitForIdle()
        snap("410_flashgrid_ready")
        assertTrue(hasStartButton(), "the start button is over the grid")
        assertEquals("Ready", phase(), "nothing is lit on its own, however long the page is on show")
        onAllNodesWithTag("flashRestart")[0].assertIsNotEnabled()
        tapTile(pattern()[0])
        assertEquals(emptyList(), board()[FlashFoundKey], "the tiles cannot be touched before start")
        assertTrue(!lost())
    }

    @Test
    fun startLightsTheTilesAndThenTheyGoDark() = runComposeUiTest {
        openBoard()
        pressStart()
        assertTrue(!hasStartButton(), "the start button goes away")
        assertEquals("Show", phase())
        snap("411_flashgrid_show")
        tapTile(pattern()[0])
        assertEquals(emptyList(), board()[FlashFoundKey], "a touch before the tiles are dark does nothing")
        untilGuess()
        snap("412_flashgrid_guess")
        assertTrue(hasAnyText("Flaş Izgara", "Flash Grid"))
        assertEquals(3, pattern().size)
        assertEquals(0, board()[FlashRoundsKey])
    }

    @Test
    fun findingTheTilesClearsTheRoundAndTheNextOneStartsByItselfWithNoStartButton() = runComposeUiTest {
        openBoard()
        pressStart()
        untilGuess()
        pattern().forEach { tapTile(it) }
        assertEquals(1, board()[FlashRoundsKey])
        assertEquals(4, pattern().size, "one more tile")
        // The green tiles show for a moment, then the next round is lit, without a touch and without a start button.
        assertEquals("Between", phase())
        assertTrue(!hasStartButton(), "no start button between the rounds")
        mainClock.advanceTimeBy(900)
        waitForIdle()
        assertEquals("Show", phase())
        assertTrue(!hasStartButton())
        untilGuess()
        pattern().forEach { tapTile(it) }
        assertEquals(2, board()[FlashRoundsKey])
    }

    @Test
    fun leavingInTheMiddleAndComingBackDoesNotShowTheTilesAgain() = runComposeUiTest {
        openBoard()
        pressStart()
        untilGuess()
        val first = pattern()
        tapTile(first[0])
        // Away to the page above and back.
        onRoot().performTouchInput { swipeDown(startY = height * 0.15f, endY = height * 0.7f) }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("flashBoard").fetchSemanticsNodes().isNotEmpty())
        assertTrue(phase() != "Show", "the tiles are not lit again: ${phase()}")
        assertTrue(!hasStartButton(), "the run goes on, it is not at the start")
        // The restart button is there for a player who wants to see tiles again, and it asks for start.
        snap("413_flashgrid_back")
        onAllNodesWithTag("flashRestart")[0].performClick()
        mainClock.advanceTimeBy(200)
        waitForIdle()
        assertTrue(hasStartButton(), "started over: the start button is back")
        assertEquals("Ready", phase())
        assertEquals(0, board()[FlashRoundsKey])
    }

    @Test
    fun aTileThatWasNotLitEndsTheRunAndTheResultShowsTheRounds() = runComposeUiTest {
        openBoard()
        pressStart()
        untilGuess()
        pattern().forEach { tapTile(it) }
        mainClock.advanceTimeBy(900)
        untilGuess()
        tapTile(wrongTile())
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("414_flashgrid_lost")
        assertTrue(lost())
        assertTrue(hasAnyText("1 tur geçildi", "1 rounds cleared"))
    }

    @Test
    fun clearingEveryRoundWins() = runComposeUiTest {
        openBoard()
        pressStart()
        // The default (medium) level has seven rounds (3 to 9 tiles).
        repeat(7) {
            mainClock.advanceTimeBy(900)
            untilGuess()
            pattern().forEach { tapTile(it) }
        }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("415_flashgrid_won")
        assertTrue(hasAnyText("Mükemmel hafıza!", "Perfect memory!"))
    }

    @Test
    fun theGridAndTheResultFitAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.FlashGrid), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("416_flashgrid_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        fun inside(tag: String) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        inside("flashBoard")
        inside("flashStart")
        inside("flashMessage")
        inside("flashRestart")
        pressStart()
        untilGuess()
        tapTile(wrongTile())
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("417_flashgrid_small_lost")
        assertTrue(lost())
        inside("flashResultCard")
        val boardBottom = onAllNodesWithTag("flashBoard").fetchSemanticsNodes().first().boundsInRoot.bottom
        val cardTop = onAllNodesWithTag("flashResultCard").fetchSemanticsNodes().first().boundsInRoot.top
        assertTrue(boardBottom <= cardTop + 1f, "the grid and the result card do not overlap: $boardBottom, $cardTop")
    }
}
