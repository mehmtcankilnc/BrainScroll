package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The delete key of the word puzzle: one tap deletes one letter, holding it keeps deleting. */
@OptIn(ExperimentalTestApi::class)
class KeyboardHoldUiTest {

    private fun androidx.compose.ui.test.ComposeUiTest.tiles(letter: String) = onAllNodesWithText(letter).fetchSemanticsNodes().size

    @Test
    fun aTapDeletesOneLetterAndHoldingDeletesTheRest() = runComposeUiTest {
        start()
        val keyboards = tiles("A") // the keys of the keyboard(s), before anything is typed
        val cKeys = tiles("C")
        key('C'); key('R'); key('A')
        assertEquals(keyboards + 1, tiles("A"), "the letter A is on its tile")

        onAllNodesWithTag("keyBackspace")[0].performClick()
        waitForIdle()
        assertEquals(keyboards, tiles("A"), "one tap took the last letter")

        key('A'); key('N')
        assertTrue(tiles("N") > 0)
        mainClock.autoAdvance = false
        onAllNodesWithTag("keyBackspace")[0].performTouchInput { down(center) }
        mainClock.advanceTimeBy(400 + 70 * 6)
        onAllNodesWithTag("keyBackspace")[0].performTouchInput { up() }
        mainClock.autoAdvance = true
        waitForIdle()
        assertEquals(keyboards, tiles("A"), "holding deleted the letters one after the other")
        assertEquals(cKeys, tiles("C"), "the first letter went too: the row is empty")
    }
}
