package com.mehmtcan.brainscroll.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.ui.feed.Hud
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/** The win streak in the bar of the feed: a "+1" rises over the old number, then it turns into the new one. */
@OptIn(ExperimentalTestApi::class)
class StreakAnimationUiTest {

    @Test
    fun aHigherStreakShowsPlusOneOverTheOldNumberThenTheNewNumber() = runComposeUiTest {
        var streak by mutableIntStateOf(3)
        setContent {
            BrainScrollTheme {
                Hud(title = "Test", badge = null, solvedCount = streak, onHome = {}, onHowToPlay = {}, onStreak = {})
            }
        }
        mainClock.autoAdvance = false
        assertEquals(1, onAllNodesWithText("3").fetchSemanticsNodes().size)

        streak = 4
        mainClock.advanceTimeBy(200)
        assertEquals(1, onAllNodesWithText("+1").fetchSemanticsNodes().size, "the +1 is on its way up")
        assertEquals(1, onAllNodesWithText("3").fetchSemanticsNodes().size, "the old number is still there")

        mainClock.advanceTimeBy(1_500)
        assertEquals(0, onAllNodesWithText("+1").fetchSemanticsNodes().size, "the +1 is gone")
        assertEquals(1, onAllNodesWithText("4").fetchSemanticsNodes().size, "the number is the new one")
    }

    @Test
    fun aStreakThatFallsOrStartsHighChangesAtOnce() = runComposeUiTest {
        var streak by mutableIntStateOf(5)
        setContent {
            BrainScrollTheme {
                Hud(title = "Test", badge = null, solvedCount = streak, onHome = {}, onHowToPlay = {}, onStreak = {})
            }
        }
        mainClock.autoAdvance = false
        streak = 0
        mainClock.advanceTimeBy(50)
        assertEquals(1, onAllNodesWithText("0").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithText("+1").fetchSemanticsNodes().size)
    }
}
