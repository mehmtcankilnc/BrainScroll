package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.codebreaker.CodeInputKey
import com.mehmtcan.brainscroll.ui.codebreaker.CodeSecretKey
import com.mehmtcan.brainscroll.ui.codebreaker.CodeTriesKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The code breaker page in the feed, driven through the real screen. The code is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class CodeBreakerUiTest {

    private val games = listOf(GameKind.Word, GameKind.CodeBreaker)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("codeBoard").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show.
    private fun ComposeUiTest.board() = onAllNodesWithTag("codeBoard").fetchSemanticsNodes().first().config

    private fun ComposeUiTest.press(tag: String) {
        onAllNodesWithTag(tag)[0].performClick()
        mainClock.advanceTimeBy(50)
        waitForIdle()
    }

    private fun ComposeUiTest.pick(colors: List<Int>) = colors.forEach { press("codeColor$it") }

    private fun ComposeUiTest.check() {
        press("codeCheck")
        mainClock.advanceTimeBy(700)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Kod kırıldı!", "Code cracked!")

    /** A code that is not the secret and that the board accepts. */
    private fun ComposeUiTest.wrongGuess(): List<Int> {
        val secret = board()[CodeSecretKey]
        // The colours in order, moved along until it differs from the secret (a board with no repeats takes any order).
        val all = (0 until 5).toList()
        return (0..4).map { shift -> all.map { (it + shift) % 5 }.take(secret.size) }.first { it != secret && it.toSet().size == secret.size }
    }

    @Test
    fun theBoardShowsTheColoursAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("390_codebreaker_start")
        assertTrue(hasAnyText("Kod Kırıcı", "Code Breaker"))
        assertEquals(0, board()[CodeTriesKey])
        assertEquals(emptyList(), board()[CodeInputKey])
        assertTrue(!won())
        for (tag in listOf("codeColor0", "codeColor4", "codeBackspace", "codeCheck", "codeSlot0", "codeSlot3")) assertTrue(onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty(), tag)
        onAllNodesWithTag("codeCheck")[0].assertIsNotEnabled()
    }

    @Test
    fun tappingColoursFillsThePlacesAndASlotOrDeleteTakesOneBack() = runComposeUiTest {
        openBoard()
        pick(listOf(2, 0))
        assertEquals(listOf(2, 0), board()[CodeInputKey])
        press("codeBackspace")
        assertEquals(listOf(2), board()[CodeInputKey])
        pick(listOf(1, 3))
        assertEquals(listOf(2, 1, 3), board()[CodeInputKey])
        press("codeSlot0")
        assertEquals(listOf(1, 3), board()[CodeInputKey], "a tap on a filled place takes its colour out")
    }

    @Test
    fun aColourAlreadyInTheRowCannotBePickedTwiceOnABoardWithNoRepeats() = runComposeUiTest {
        openBoard()
        pick(listOf(1))
        onAllNodesWithTag("codeColor1")[0].assertIsNotEnabled()
        onAllNodesWithTag("codeColor2")[0].assertIsEnabled()
    }

    @Test
    fun checkNeedsAFullRowAndAWrongGuessIsCountedAndCleared() = runComposeUiTest {
        openBoard()
        val guess = wrongGuess()
        pick(guess.dropLast(1))
        onAllNodesWithTag("codeCheck")[0].assertIsNotEnabled()
        pick(guess.takeLast(1))
        onAllNodesWithTag("codeCheck")[0].assertIsEnabled()
        check()
        assertEquals(1, board()[CodeTriesKey])
        assertEquals(emptyList(), board()[CodeInputKey], "the row is empty again for the next try")
        assertTrue(!won())
    }

    @Test
    fun theRightCodeWinsAndShowsTheTries() = runComposeUiTest {
        openBoard()
        val guess = wrongGuess()
        pick(guess)
        check()
        pick(board()[CodeSecretKey])
        check()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("391_codebreaker_won")
        assertTrue(won())
        assertEquals(2, board()[CodeTriesKey])
        assertTrue(hasAnyText("2 denemede", "2 tries"))
    }

    @Test
    fun thereIsNoLimitToTheTriesAndTheNewestRowStaysInView() = runComposeUiTest {
        openBoard()
        val guess = wrongGuess()
        repeat(14) {
            pick(guess)
            check()
        }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("392_codebreaker_many")
        assertEquals(14, board()[CodeTriesKey])
        assertTrue(!won())
        val list = onAllNodesWithTag("codeBoard").fetchSemanticsNodes().first().boundsInRoot
        val slot = onAllNodesWithTag("codeSlot0").fetchSemanticsNodes().first().boundsInRoot
        assertTrue(slot.top >= list.top - 1f && slot.bottom <= list.bottom + 1f, "the row being filled is in view: $slot against $list")
        // And the code can still be cracked.
        pick(board()[CodeSecretKey])
        check()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(won())
        assertEquals(15, board()[CodeTriesKey])
    }

    @Test
    fun theBoardAndTheResultFitAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.CodeBreaker), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("393_codebreaker_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        fun inside(tag: String) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        for (tag in listOf("codeBoard", "codeColor0", "codeColor4", "codeBackspace", "codeCheck")) inside(tag)
        pick(board()[CodeSecretKey])
        check()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("394_codebreaker_small_won")
        assertTrue(won())
        inside("codeResultCard")
        val listBottom = onAllNodesWithTag("codeBoard").fetchSemanticsNodes().first().boundsInRoot.bottom
        val cardTop = onAllNodesWithTag("codeResultCard").fetchSemanticsNodes().first().boundsInRoot.top
        assertTrue(listBottom <= cardTop + 1f, "the tries and the result card do not overlap: $listBottom, $cardTop")
    }
}
