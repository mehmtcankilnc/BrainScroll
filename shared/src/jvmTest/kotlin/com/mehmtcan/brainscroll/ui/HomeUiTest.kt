package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The home tab and the pages around it (docs/decisions.md, Sadeleştirme). */
@OptIn(ExperimentalTestApi::class)
class HomeUiTest {

    private val both = listOf(GameKind.Word, GameKind.Memory)

    @Test
    fun theAppOpensOnTheHomeTabWithItsSections() = runComposeUiTest {
        start(games = both, openFeed = false)
        snap("20_home")

        assertTrue(hasAnyText("Bugünün bulmacası", "Today's puzzle"))
        assertTrue(hasAnyText("Akışını kişiselleştir", "Personalize your feed"))
        assertTrue(hasAnyText("Bugün en hızlılar", "Fastest today"))
        // The game cards are gone: the feed is entered through its tab.
        assertFalse(hasAnyText("Hemen oyna", "Play now"))
    }

    @Test
    fun theLastGameCannotBeSwitchedOff() = runComposeUiTest {
        start(games = both, openFeed = false)

        // Memory off is fine; then Word is the last one and its chip is not clickable any more.
        val memory = if (hasAnyText("Hafıza eşleştirme")) "Hafıza eşleştirme" else "Memory match"
        onAllNodesWithText(memory).onLast().performClick()
        waitForIdle()

        assertTrue(hasAnyText("En az bir oyun açık kalır", "At least one game stays on"))
        snap("21_personalize_inline")
    }

    @Test
    fun theFeedHasNoTabBarAndTheChevronGoesHome() = runComposeUiTest {
        start(games = both)

        assertFalse(hasAnyText("Profil", "Profile"))
        goHome()

        assertTrue(hasAnyText("Bugünün bulmacası", "Today's puzzle"))
        assertTrue(hasAnyText("Profil", "Profile"))
    }

    @Test
    fun theDailyCardOpensAPageWithABackButton() = runComposeUiTest {
        start(games = both, openFeed = false)

        openTab("Günlük", "Daily")
        waitForIdle()
        assertFalse(hasAnyText("Profil", "Profile"))

        goHome()
        assertTrue(hasAnyText("Akışını kişiselleştir", "Personalize your feed"))
    }
}
