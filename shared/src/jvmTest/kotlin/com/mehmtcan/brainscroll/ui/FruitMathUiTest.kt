package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.fruitmath.FruitAnswerKey
import com.mehmtcan.brainscroll.ui.fruitmath.FruitInputKey
import com.mehmtcan.brainscroll.ui.fruitmath.FruitMistakesKey
import com.mehmtcan.brainscroll.ui.fruitmath.FruitShakeKey
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.getOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The fruit equations page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class FruitMathUiTest {

    private val games = listOf(GameKind.Word, GameKind.FruitMath)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("fruitBoard").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first board is the one on show.
    private fun ComposeUiTest.board() = onAllNodesWithTag("fruitBoard").fetchSemanticsNodes().first().config

    private fun ComposeUiTest.press(tag: String) {
        onAllNodesWithTag(tag)[0].performClick()
        mainClock.advanceTimeBy(50)
        waitForIdle()
    }

    /** Types [number] on the keypad (no enter). */
    private fun ComposeUiTest.type(number: Int) {
        number.toString().forEach { press("fruitKey$it") }
    }

    private fun ComposeUiTest.submit(number: Int) {
        type(number)
        press("fruitEnter")
        mainClock.advanceTimeBy(700)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Doğru cevap!", "Right answer!")

    @Test
    fun theBoardShowsAKeypadAndIsNotWonAtTheStart() = runComposeUiTest {
        openBoard()
        snap("380_fruitmath_start")
        assertTrue(hasAnyText("Meyve Denklemi", "Fruit Equations"))
        assertEquals("", board()[FruitInputKey])
        assertEquals(0, board()[FruitMistakesKey])
        assertTrue(!won())
        for (tag in listOf("fruitKey0", "fruitKey9", "fruitBackspace", "fruitEnter")) assertTrue(onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty(), tag)
    }

    @Test
    fun theKeysTypeIntoThePlaceOfTheQuestionMarkAndDeleteTakesADigitBack() = runComposeUiTest {
        openBoard()
        press("fruitKey4")
        press("fruitKey2")
        assertEquals("42", board()[FruitInputKey])
        press("fruitBackspace")
        assertEquals("4", board()[FruitInputKey])
        // No more than three digits.
        press("fruitKey1"); press("fruitKey1"); press("fruitKey1")
        assertEquals("411", board()[FruitInputKey])
    }

    @Test
    fun aWrongAnswerIsCountedAndClearedAndTheRightOneWins() = runComposeUiTest {
        openBoard()
        val answer = board()[FruitAnswerKey]
        submit(answer + 1)
        assertEquals(1, board()[FruitMistakesKey])
        assertEquals("", board()[FruitInputKey], "a wrong answer is cleared")
        assertTrue(!won())
        submit(answer)
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("381_fruitmath_solved")
        assertTrue(won())
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun enterDoesNothingWhileNothingIsTyped() = runComposeUiTest {
        openBoard()
        press("fruitEnter")
        assertEquals(0, board()[FruitMistakesKey])
    }

    @Test
    fun thinkingBeforeTheOnlyAnswerCountsInTheTime() = runComposeUiTest {
        openBoard()
        Thread.sleep(1_300)
        submit(board()[FruitAnswerKey])
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(won())
        assertTrue(hasAnyText("00:01", "00:02"), "the time includes the time spent looking")
    }

    @Test
    fun theBoardTheKeypadAndTheResultFitAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.FruitMath), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("382_fruitmath_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        fun inside(tag: String) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        for (tag in listOf("fruitBoard", "fruitKey1", "fruitKey0", "fruitBackspace", "fruitEnter")) inside(tag)
        // The result takes the place of the keypad and has to fit it.
        submit(board()[FruitAnswerKey])
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("383_fruitmath_small_solved")
        assertTrue(won())
        inside("fruitBoard")
        inside("fruitResultCard")
        val boardBottom = onAllNodesWithTag("fruitBoard").fetchSemanticsNodes().first().boundsInRoot.bottom
        val cardTop = onAllNodesWithTag("fruitResultCard").fetchSemanticsNodes().first().boundsInRoot.top
        assertTrue(boardBottom <= cardTop + 1f, "the board (to $boardBottom) is not over the result (from $cardTop)")
    }

    @Test
    fun aHardBoardDrawsPowersSquaresAndQuotientsAndTheThumbnailAndTheCardDrawToo() = runComposeUiTest {
        // A hard board that has a square and a quotient, if one of the first seeds has them.
        val puzzle = (0 until 300).map { com.mehmtcan.brainscroll.game.fruitmath.FruitGenerator.generate(it, com.mehmtcan.brainscroll.game.Difficulty.Hard) }
            .first { p ->
                p.kinds.any { it == com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.PowerOf || it == com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.PowerBase } &&
                    p.kinds.any { it == com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.SquarePlus || it == com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.SquareOnly } &&
                    p.kinds.any { it == com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.Quotient }
            }
        setContent {
            com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme {
                androidx.compose.foundation.layout.Column(
                    androidx.compose.ui.Modifier.size(360.dp, 520.dp).background(com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme.colors.bgSurface).padding(14.dp),
                ) {
                    val colors = com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme.colors
                    val lines = puzzle.lines + puzzle.last
                    val sizes = com.mehmtcan.brainscroll.ui.fruitmath.fruitSizes(lines, 60.dp, 330.dp)
                    for (line in puzzle.lines) com.mehmtcan.brainscroll.ui.fruitmath.FruitRow(line, line.total.toString(), colors.textPrimary, sizes, 60.dp)
                    com.mehmtcan.brainscroll.ui.fruitmath.FruitRow(puzzle.last, "?", colors.streakAnswer, sizes, 60.dp)
                    com.mehmtcan.brainscroll.ui.home.GameThumbnail(GameKind.FruitMath, com.mehmtcan.brainscroll.game.wordle.Language.EN, androidx.compose.ui.Modifier.size(110.dp))
                }
            }
        }
        waitForIdle()
        snap("384_fruitmath_hard_rows")
    }

    private fun ComposeUiTest.shakeOffset() = board()[FruitShakeKey]

    @Test
    fun aWrongAnswerShakesTheBoardAndItComesBackToRest() = runComposeUiTest {
        start(games = games, reduceMotion = false)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("fruitBoard").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0f, shakeOffset(), "at rest")
        type(board()[FruitAnswerKey] + 1)
        // The clock is moved by hand: with its own running a wait for idle would play the whole shake at once.
        mainClock.autoAdvance = false
        onAllNodesWithTag("fruitEnter")[0].performClick()
        val seen = ArrayList<Float>()
        repeat(30) {
            mainClock.advanceTimeBy(16)
            seen += shakeOffset()
        }
        assertTrue(seen.any { kotlin.math.abs(it) > 1f }, "the board is off to one side while it shakes: $seen")
        assertTrue(seen.any { it < -1f } && seen.any { it > 1f }, "it goes to both sides: $seen")
        mainClock.advanceTimeBy(700)
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(0f, shakeOffset(), 0.01f, "the board is back where it was")
        assertEquals(1, board()[FruitMistakesKey])
    }

    @Test
    fun withReducedMotionTheBoardDoesNotShake() = runComposeUiTest {
        start(games = games, reduceMotion = true)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("fruitBoard").fetchSemanticsNodes().isNotEmpty() }
        type(board()[FruitAnswerKey] + 1)
        mainClock.autoAdvance = false
        onAllNodesWithTag("fruitEnter")[0].performClick()
        repeat(10) {
            mainClock.advanceTimeBy(16)
            assertEquals(0f, shakeOffset(), 0.01f, "no shake")
        }
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(1, board()[FruitMistakesKey], "the wrong answer still counts")
    }

    @Test
    fun everyLineIsReadAloudWithItsTotalAndTheLastWithTheQuestion() = runComposeUiTest {
        openBoard()
        val lines = onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher("a line") { node ->
                node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)
                    ?.any { it.contains(" equals ") || it.contains(" eşittir ") } == true
            },
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue(lines.size >= 4, "a board has at least three lines and the question: ${lines.size}")
        val spoken = lines.last().config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].first()
        assertTrue(spoken.endsWith("?"), "the last line asks: $spoken")
    }
}
