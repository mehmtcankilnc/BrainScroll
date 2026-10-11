package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Renders the real feed headlessly at phone size, plays it, and saves screenshots to build/screenshots. */
@OptIn(ExperimentalTestApi::class)
class FeedScreenTest {

    @Test
    fun playsARoundWithFeedbackAndLeavesItUnfinished() = runComposeUiTest {
        start()
        switchToEnglish()
        snap("1_start_en")

        // Too short: shows a message and shakes.
        "CRA".forEach { key(it) }
        enter()
        mainClock.advanceTimeBy(300)
        snap("2_too_short")
        mainClock.advanceTimeBy(2000)

        // A real word: the row flips and the keyboard shows the letter states.
        "NE".forEach { key(it) }
        enter()
        mainClock.advanceTimeBy(2000)
        snap("3_after_first_guess")

        // Swipe to the next puzzle without finishing: the feed scrolls freely, nothing is blocked or counted.
        swipeFeedUp()
        mainClock.advanceTimeBy(1500)
        waitForIdle()
        snap("4_left_unfinished")
        assertFalse(hasAnyText("Skip", "Atla"), "there is no skip any more")
        assertFalse(hasAnyText("Finish this puzzle first", "Önce bu bulmacayı bitir"))

    }

    @Test
    fun nextRowDoesNotShakeAfterAnEarlierRejectedGuess() = runComposeUiTest {
        start()
        switchToEnglish()
        val rest = visibleInputRow()

        // A rejected guess earlier in the round (errorTick becomes 1) ...
        "CRA".forEach { key(it) }
        enter()
        mainClock.advanceTimeBy(1000)
        // ... then a valid guess. The new input row must stay still instead of replaying the shake.
        "NE".forEach { key(it) }
        // Freeze the clock so we look at the first frames of any animation instead of waiting for it to end.
        mainClock.autoAdvance = false
        enter()
        mainClock.advanceTimeBy(70)
        val after = visibleInputRow()
        // Same column: only the row (top) changes after a guess. A shake would move it sideways.
        assertEquals(rest.first, after.first, "the new input row shook")
    }

    @Test
    fun showsTheResultCardAfterSixWrongGuesses() = runComposeUiTest {
        start()
        switchToEnglish()
        // Six different real words. The chance that one of them is the answer is tiny, so the round is lost.
        for (word in listOf("CRANE", "SLATE", "GHOST", "POUND", "BRICK", "FLUID")) {
            word.forEach { key(it) }
            enter()
            mainClock.advanceTimeBy(1500)
        }
        waitForIdle()
        snap("7_lost_result_card")
        // The keyboard is replaced by the result card with the swipe hint.
        assertTrue(
            onAllNodesWithText("Sıradaki bulmaca için yukarı kaydır", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                onAllNodesWithText("Swipe up for the next puzzle", substring = true).fetchSemanticsNodes().isNotEmpty(),
            "result card with swipe hint should be shown",
        )
    }
}
