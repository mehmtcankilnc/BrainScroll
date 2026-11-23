package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.cubefold.CubeCorrectKey
import com.mehmtcan.brainscroll.ui.cubefold.CubeViewKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The cube folding page in the feed, driven through the real screen. The questions are random, so the tests read the cubes the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class CubeFoldUiTest {

    private val games = listOf(GameKind.Word, GameKind.CubeFold)

    /** The default (medium) level shows four cubes. */
    private val options = 4

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("cubeOption").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first cubes are the ones on show.
    private fun ComposeUiTest.cubes() = onAllNodesWithTag("cubeOption").fetchSemanticsNodes().take(options)

    private fun ComposeUiTest.rightIndex() = cubes().indexOfFirst { it.config[CubeCorrectKey] }

    private fun ComposeUiTest.wrongIndex() = cubes().indexOfFirst { !it.config[CubeCorrectKey] }

    private fun ComposeUiTest.pick(index: Int) {
        onAllNodesWithTag("cubeOption")[index].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Keskin göz!", "Sharp eyes!")
    private fun ComposeUiTest.lost() = hasAnyText("Açılım bu küpü vermez", "The net does not fold to that cube")

    @Test
    fun thePageShowsTheNetAndFourCubesAndIsNotOverAtTheStart() = runComposeUiTest {
        openBoard()
        snap("460_cubefold_start")
        assertTrue(hasAnyText("Küp Katlama", "Cube Folding"))
        assertEquals(options, cubes().size)
        assertEquals(1, cubes().count { it.config[CubeCorrectKey] })
        assertTrue(cubes().all { it.config[CubeViewKey].toSet().size == 3 })
        assertEquals(1, onAllNodesWithTag("cubeNet").fetchSemanticsNodes().take(1).size)
        assertTrue(hasAnyText("Soru 1 /", "Question 1 of"))
        assertFalse(won() || lost())
    }

    @Test
    fun aRightPickShowsTheAnswerThenTheNextQuestion() = runComposeUiTest {
        openBoard()
        val before = cubes().map { it.config[CubeViewKey] }
        pick(rightIndex())
        snap("461_cubefold_right")
        assertTrue(hasAnyText("Doğru! Açılım bu küpü oluşturur", "Right! The net folds to this cube"))
        // The cubes cannot be tapped while the answer is shown.
        pick(wrongIndex())
        assertFalse(lost(), "a tap while the answer is on show does nothing")
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        assertTrue(hasAnyText("Soru 2 /", "Question 2 of"))
        assertTrue(cubes().map { it.config[CubeViewKey] } != before, "a new question")
    }

    @Test
    fun aWrongPickEndsTheRunAndSaysWhy() = runComposeUiTest {
        openBoard()
        pick(rightIndex())
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        pick(wrongIndex())
        snap("462_cubefold_wrong")
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        snap("463_cubefold_lost")
        assertTrue(lost())
        assertTrue(hasAnyText("1 soru doğru", "1 questions right"))
        assertTrue(hasAnyText("karşılıklı", "opposite", "ters sırada", "wrong way round"))
    }

    @Test
    fun answeringEveryQuestionWins() = runComposeUiTest {
        openBoard()
        // The default (medium) level has eight questions.
        repeat(8) {
            pick(rightIndex())
            mainClock.advanceTimeBy(2_000)
            waitForIdle()
        }
        snap("464_cubefold_won")
        assertTrue(won())
    }

    @Test
    fun theHintRulesOutAWrongCubeAndNeverPointsAtTheRightOne() = runComposeUiTest {
        start(openFeed = false, games = listOf(GameKind.CubeFold))
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("cubeOption").fetchSemanticsNodes().isNotEmpty() }
        val right = rightIndex()
        onAllNodesWithTag("guideButton")[0].performClick()
        waitUntil(timeoutMillis = 60_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        mainClock.advanceTimeBy(500)
        snap("465_cubefold_hint")
        val marks = onAllNodesWithTag("guideMark").fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(1, marks.size, "one cube is ringed")
        val cube = cubes()[right].boundsInRoot
        assertFalse(marks[0].overlaps(cube), "the ring is not on the right cube")
        assertTrue(onAllNodesWithTag("guideEvidence").fetchSemanticsNodes().size in 2..3, "the squares of the net that show why are ringed softly")
    }
}
