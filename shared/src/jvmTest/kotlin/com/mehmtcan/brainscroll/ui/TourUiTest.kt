package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What a first start of the app shows: the mascot's opening animation, then a short tour of the home page. */
@OptIn(ExperimentalTestApi::class)
class TourUiTest {

    private fun androidx.compose.ui.test.ComposeUiTest.has(tag: String) = onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun aFirstStartShowsTheOpeningThenATwoStopTour() = runComposeUiTest {
        start(openFeed = false, intro = true)
        assertTrue(has("splash"), "the opening animation covers the app first")
        waitUntil(timeoutMillis = 10_000) { !has("splash") }
        waitUntil(timeoutMillis = 10_000) { has("tour") }
        assertTrue(hasAnyText("Günün bulmacası", "Today's puzzle"))
        snap("14_tour_daily")

        onNodeWithTag("tourNext").performClick()
        waitForIdle()
        assertTrue(hasAnyText("Senin akışın", "Your feed"))
        snap("14_tour_feed")
        // The last stop has no skip: it is the end.
        assertEquals(false, has("tourSkip"))
        onNodeWithTag("tourNext").performClick()
        waitUntil(timeoutMillis = 5_000) { !has("tour") }
    }

    @Test
    fun theTourCanBeSkippedAtTheFirstStop() = runComposeUiTest {
        start(openFeed = false, intro = true)
        waitUntil(timeoutMillis = 10_000) { has("tour") }
        onNodeWithTag("tourSkip").performClick()
        waitUntil(timeoutMillis = 5_000) { !has("tour") }
        // The home page is usable under it.
        assertTrue(has("dailyCard"))
    }

    @Test
    fun withoutTheIntroNothingCoversTheApp() = runComposeUiTest {
        start(openFeed = false)
        waitForIdle()
        assertEquals(false, has("splash"))
        assertEquals(false, has("tour"))
    }
}
