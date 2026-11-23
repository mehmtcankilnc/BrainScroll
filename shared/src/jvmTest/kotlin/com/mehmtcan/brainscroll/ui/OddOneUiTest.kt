package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.oddone.OddRule
import com.mehmtcan.brainscroll.ui.home.GameKeywords
import com.mehmtcan.brainscroll.ui.howto.OddOneScenes
import com.mehmtcan.brainscroll.ui.oddone.OddBreaksKey
import com.mehmtcan.brainscroll.ui.oddone.OddNumberKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The odd one out page in the feed, driven through the real screen. The questions are random, so the tests read the numbers the way a screen reader does. */
@OptIn(ExperimentalTestApi::class)
class OddOneUiTest {

    private val games = listOf(GameKind.Word, GameKind.OddOne)

    private fun ComposeUiTest.openBoard() {
        start(games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("oddNumber").fetchSemanticsNodes().isNotEmpty() }
    }

    // The page next to the one on show may be there too: the first five numbers are the ones on show.
    private fun ComposeUiTest.numbers() = onAllNodesWithTag("oddNumber").fetchSemanticsNodes().take(5).map { it.config[OddNumberKey] }

    private fun ComposeUiTest.oddIndex() = onAllNodesWithTag("oddNumber").fetchSemanticsNodes().take(5).indexOfFirst { it.config[OddBreaksKey] }

    private fun ComposeUiTest.pick(index: Int) {
        onAllNodesWithTag("oddNumber")[index].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
    }

    private fun ComposeUiTest.won() = hasAnyText("Hepsini buldun!", "Found every odd one!")
    private fun ComposeUiTest.lost() = hasAnyText("Yanlış sayı", "Wrong number")

    @Test
    fun theBoardShowsFiveNumbersAndIsNotOverAtTheStart() = runComposeUiTest {
        openBoard()
        snap("440_oddone_start")
        assertTrue(hasAnyText("Farklı Olanı Bul", "Odd One Out"))
        assertEquals(5, numbers().toSet().size)
        assertTrue(hasAnyText("Tur 1 /", "Round 1 of"))
        assertFalse(won() || lost())
    }

    @Test
    fun aRightPickShowsTheRuleThenTheNextQuestion() = runComposeUiTest {
        openBoard()
        val before = numbers()
        pick(oddIndex())
        snap("441_oddone_rule")
        assertTrue(hasAnyText("Kural:", "Rule:"), "the rule is shown")
        assertEquals(before, numbers(), "the same numbers stay while the rule is shown")
        mainClock.advanceTimeBy(1_800)
        waitForIdle()
        assertTrue(hasAnyText("Tur 2 /", "Round 2 of"), "the next round")
        assertTrue(numbers() != before)
    }

    @Test
    fun aWrongPickEndsTheRun() = runComposeUiTest {
        openBoard()
        pick((0 until 5).first { it != oddIndex() })
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        snap("442_oddone_lost")
        assertTrue(lost())
    }

    @Test
    fun rightPicksToTheEndWin() = runComposeUiTest {
        openBoard()
        var guard = 0
        while (!won() && guard++ < 20) {
            pick(oddIndex())
            mainClock.advanceTimeBy(1_800)
            waitForIdle()
        }
        snap("443_oddone_won")
        assertTrue(won())
    }

    @Test
    fun theScenesOfTheCardShowRealQuestions() {
        for ((name, scene) in OddOneScenes) {
            val numbers = scene.cells.map { it.text.toInt() }
            // One rule that four of the numbers follow, and no other rule that singles out another number.
            val rules = OddRule.entries.filter { rule -> numbers.count { !rule.holds(it) } == 1 }
            assertTrue(rules.isNotEmpty(), "$name: a rule fits four of $numbers")
            val odds = rules.map { rule -> numbers.single { !rule.holds(it) } }.toSet()
            assertEquals(1, odds.size, "$name: $numbers has one odd one, not $odds")
        }
    }

    @Test
    fun theGameCanBeFoundByItsSearchWords() {
        assertTrue("farklı olanı bul" in GameKeywords.getValue(GameKind.OddOne))
    }
}
