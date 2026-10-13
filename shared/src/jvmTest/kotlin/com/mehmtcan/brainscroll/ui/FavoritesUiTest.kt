package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The favorites page: a puzzle can be played again from it, and the list can be filtered by game. */
@OptIn(ExperimentalTestApi::class)
class FavoritesUiTest {

    @Test
    fun aFavoriteCanBePlayedAgainAsTheFirstPageOfANewFeed() = runComposeUiTest {
        start(TestCloud(), games = listOf(GameKind.Word))
        switchToEnglish()
        // Six different words end the round, won or lost; its heart is the favorite.
        for (word in listOf("CRANE", "SLATE", "GHOST", "POUND", "BRICK", "FLUID")) {
            typeWord(word)
            mainClock.advanceTimeBy(1500)
        }
        waitForIdle()
        onAllNodesWithTag("favoriteHeart")[0].performClick()
        waitForIdle()

        openProfilePage("Favoriler", "Favorites")
        snap("230_favorites")
        assertEquals(1, onAllNodesWithTag("favoriteReplay").fetchSemanticsNodes().size)
        // With one game in the list there is nothing to filter.
        assertTrue(!hasAnyText("Tümü", "All"))

        onNodeWithTag("favoriteReplay").performClick()
        waitUntil(timeoutMillis = 10_000) { hasAnyText("ENTER", "GİR") }
        assertTrue(hasAnyText("ENTER", "GİR"), "the puzzle is on screen, untouched, with its keyboard")
    }
}
