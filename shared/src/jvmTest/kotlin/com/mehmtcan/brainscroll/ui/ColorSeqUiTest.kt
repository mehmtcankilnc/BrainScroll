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
import com.mehmtcan.brainscroll.ui.colorseq.ColorSeqEnteredKey
import com.mehmtcan.brainscroll.ui.colorseq.ColorSeqLitKey
import com.mehmtcan.brainscroll.ui.colorseq.ColorSeqPhaseKey
import com.mehmtcan.brainscroll.ui.colorseq.ColorSeqRoundsKey
import com.mehmtcan.brainscroll.ui.colorseq.ColorSeqSequenceKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The color sequence page in the feed, driven through the real screen. The lights are random, so the tests read them the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class ColorSeqUiTest {

    private val games = listOf(GameKind.Word, GameKind.ColorSeq)

    /** Opens the page; the run is not started (the start button is over the buttons). */
    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("colorBoard").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show.
    private fun ComposeUiTest.board() = onAllNodesWithTag("colorBoard").fetchSemanticsNodes().first().config

    private fun ComposeUiTest.phase() = board()[ColorSeqPhaseKey]

    private fun ComposeUiTest.sequence() = board()[ColorSeqSequenceKey]

    private fun ComposeUiTest.pressStart() {
        onAllNodesWithTag("colorStart")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    /** Lets the clock run until the board waits for the touches. */
    private fun ComposeUiTest.untilGuess() {
        repeat(120) {
            if (phase() == "Guess") return
            mainClock.advanceTimeBy(100)
            waitForIdle()
        }
        assertEquals("Guess", phase())
    }

    private fun ComposeUiTest.tapButton(button: Int) {
        onAllNodesWithTag("colorButton$button")[0].performClick()
        mainClock.advanceTimeBy(50)
        waitForIdle()
    }

    private fun ComposeUiTest.repeatRound() = sequence().toList().forEach { tapButton(it) }

    private fun ComposeUiTest.lost() = hasAnyText("Yanlış düğme", "That was not the next one")

    private fun ComposeUiTest.hasStartButton() = onAllNodesWithTag("colorStart").fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theBoardWaitsForTheStartButtonAndPlaysNothingUntilItIsPressed() = runComposeUiTest {
        openBoard()
        mainClock.advanceTimeBy(5_000)
        waitForIdle()
        snap("420_colorseq_ready")
        assertTrue(hasStartButton(), "the start button is over the buttons")
        assertEquals("Ready", phase(), "nothing is played on its own, however long the page is on show")
        assertEquals(-1, board()[ColorSeqLitKey])
        onAllNodesWithTag("colorRestart")[0].assertIsNotEnabled()
        tapButton(sequence()[0])
        assertEquals(0, board()[ColorSeqRoundsKey], "the buttons cannot be touched before start")
        assertTrue(!lost())
    }

    @Test
    fun startPlaysTheLightsInOrderAndThenTheBoardWaits() = runComposeUiTest {
        openBoard()
        pressStart()
        assertTrue(!hasStartButton(), "the start button goes away")
        assertEquals("Show", phase())
        snap("421_colorseq_show")
        tapButton(sequence()[0])
        assertEquals("Show", phase(), "a touch while the lights play does nothing")
        val seen = ArrayList<Int>()
        repeat(80) {
            val lit = board()[ColorSeqLitKey]
            if (lit >= 0 && (seen.isEmpty() || seen.last() != lit)) seen += lit
            if (phase() == "Guess") return@repeat
            mainClock.advanceTimeBy(50)
            waitForIdle()
        }
        assertEquals(sequence(), seen, "the lights of round one are the sequence of the round")
        untilGuess()
        snap("422_colorseq_guess")
        assertTrue(hasAnyText("Renk Dizisi", "Color Sequence"))
        assertEquals(1, sequence().size)
        assertEquals(0, board()[ColorSeqRoundsKey])
    }

    @Test
    fun repeatingARoundPlaysTheNextOneByItselfWithOneLightMoreAndNoStartButton() = runComposeUiTest {
        openBoard()
        pressStart()
        untilGuess()
        repeatRound()
        assertEquals(1, board()[ColorSeqRoundsKey])
        assertEquals(2, sequence().size, "one light more")
        assertEquals("Between", phase())
        assertTrue(!hasStartButton(), "no start button between the rounds")
        mainClock.advanceTimeBy(800)
        waitForIdle()
        assertEquals("Show", phase())
        untilGuess()
        // Half of the round: the touches so far are counted.
        tapButton(sequence()[0])
        assertEquals(1, board()[ColorSeqEnteredKey])
        tapButton(sequence()[1])
        assertEquals(2, board()[ColorSeqRoundsKey])
    }

    @Test
    fun leavingInTheMiddleAndComingBackDoesNotPlayTheLightsAgain() = runComposeUiTest {
        openBoard()
        pressStart()
        untilGuess()
        repeatRound()
        mainClock.advanceTimeBy(800)
        untilGuess()
        // Away to the page above and back.
        onRoot().performTouchInput { swipeDown(startY = height * 0.15f, endY = height * 0.7f) }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        assertTrue(onAllNodesWithTag("colorBoard").fetchSemanticsNodes().isNotEmpty())
        assertTrue(phase() != "Show", "the lights are not played again: ${phase()}")
        assertTrue(!hasStartButton(), "the run goes on, it is not at the start")
        snap("423_colorseq_back")
        onAllNodesWithTag("colorRestart")[0].performClick()
        mainClock.advanceTimeBy(200)
        waitForIdle()
        assertTrue(hasStartButton(), "started over: the start button is back")
        assertEquals("Ready", phase())
        assertEquals(0, board()[ColorSeqRoundsKey])
    }

    @Test
    fun aWrongButtonEndsTheRunAndTheResultShowsTheRounds() = runComposeUiTest {
        openBoard()
        pressStart()
        untilGuess()
        repeatRound()
        mainClock.advanceTimeBy(800)
        untilGuess()
        val due = sequence()[0]
        tapButton((0 until 4).first { it != due })
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("424_colorseq_lost")
        assertTrue(lost())
        assertTrue(hasAnyText("1 tur geçildi", "1 rounds cleared"))
    }

    @Test
    fun clearingEveryRoundWins() = runComposeUiTest {
        openBoard()
        pressStart()
        // The default (medium) level has nine rounds (one to nine lights).
        repeat(9) {
            mainClock.advanceTimeBy(800)
            untilGuess()
            repeatRound()
        }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("425_colorseq_won")
        assertTrue(hasAnyText("Kusursuz hafıza!", "Perfect memory!"))
    }

    @Test
    fun theHintPlaysTheLightsOfTheRoundAgain() = runComposeUiTest {
        openBoard()
        pressStart()
        untilGuess()
        repeatRound()
        mainClock.advanceTimeBy(800)
        untilGuess()
        val round = sequence().toList()
        onAllNodesWithTag("guideButton")[0].performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        mainClock.advanceTimeBy(700)
        waitForIdle()
        assertEquals("Show", phase(), "the lights play again")
        val seen = ArrayList<Int>()
        repeat(120) {
            val lit = board()[ColorSeqLitKey]
            if (lit >= 0 && (seen.isEmpty() || seen.last() != lit)) seen += lit
            mainClock.advanceTimeBy(50)
            waitForIdle()
        }
        assertEquals(round, seen, "the same lights as the round")
        assertEquals("Guess", phase())
        snap("426_colorseq_hint")
    }
}
