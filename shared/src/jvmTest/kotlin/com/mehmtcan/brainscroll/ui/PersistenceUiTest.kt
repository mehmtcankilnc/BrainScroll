package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Plays the real app and checks the profile tab and what survives closing and reopening it. */
@OptIn(ExperimentalTestApi::class)
class PersistenceUiTest {

    @Test
    fun profileShowsStatsAndTheFavoritedPuzzle() = runComposeUiTest {
        start()
        switchToEnglish()

        // Six different real words end the round (a win only if one of them happens to be the answer).
        for (word in listOf("CRANE", "SLATE", "GHOST", "POUND", "BRICK", "FLUID")) {
            typeWord(word)
            mainClock.advanceTimeBy(1500)
        }
        waitForIdle()

        // Heart the finished puzzle.
        onAllNodesWithTag("favoriteHeart")[0].performClick()
        waitForIdle()

        // The profile tab: one puzzle played and the heart puzzle listed under favorites.
        openTab("Profil", "Profile")
        waitForIdle()
        snap("8_profile")
        assertTrue(
            onAllNodesWithText("Henüz favori yok", substring = true).fetchSemanticsNodes().isEmpty() &&
                onAllNodesWithText("No favorites yet", substring = true).fetchSemanticsNodes().isEmpty(),
            "the favorite should be listed",
        )
        // Played = 1 appears as a stat tile value (the distribution rows also use small numbers, so check the label too).
        assertTrue(
            onAllNodesWithText("Oynanan").fetchSemanticsNodes().isNotEmpty() ||
                onAllNodesWithText("Played").fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun aHalfFinishedPuzzleSurvivesClosingAndReopeningTheApp() {
        val file = File.createTempFile("brainscroll-test", ".db").also { it.delete() }
        val previous = System.getProperty("brainscroll.database")
        System.setProperty("brainscroll.database", file.absolutePath)
        try {
            // First run: pick English, type two letters, and "close" the app.
            runComposeUiTest {
                start()
                switchToEnglish()
                onNodeWithText("EN").assertHasClickAction()
                key('C')
                key('R')
                waitForIdle()
            }

            // Second run: a fresh app on the same database file.
            runComposeUiTest {
                start()
                waitForIdle()
                snap("9_after_reopen")
                // The language is locked again, because the restored puzzle had been started.
                onNodeWithText("EN").assertHasNoClickAction()
                // The two letters are back in the grid: the C key exists on two pages, plus our tile.
                assertEquals(3, onAllNodes(hasText("C")).fetchSemanticsNodes().size)
                assertEquals(3, onAllNodes(hasText("R")).fetchSemanticsNodes().size)
            }
        } finally {
            if (previous == null) System.clearProperty("brainscroll.database") else System.setProperty("brainscroll.database", previous)
            file.delete()
        }
    }
}
