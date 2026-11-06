package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.pro.FakeStoreBilling
import com.mehmtcan.brainscroll.ui.pro.PurchaseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pro through the real screens: the free guide steps, the purchase screen, the profile card. */
@OptIn(ExperimentalTestApi::class)
class ProUiTest {

    // The badge sits inside the clickable bulb, whose semantics are merged: look at the unmerged tree.
    private fun ComposeUiTest.has(tag: String) = onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.openLightsFeed(store: FakeStoreBilling?) {
        start(openFeed = false, games = listOf(GameKind.Lights), store = store)
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { has("lightCell") }
    }

    /**
     * One paid guide step: ask, wait for the sentence, close it, then touch the board. The same step asked for on the same board is
     * free (see [theSameStepAskedForAgainCostsNothing]), so each step needs the board to have changed.
     */
    private fun ComposeUiTest.useOneGuideStep() {
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { has("guideBanner") }
        onAllNodesWithTag("guideClose")[0].performClick()
        waitUntil(timeoutMillis = 10_000) { !has("guideBanner") }
        onAllNodesWithTag("lightCell")[0].performClick()
        waitForIdle()
    }

    @Test
    fun theFifthStepIsFreeAndTheSixthOpensThePurchaseScreen() = runComposeUiTest {
        openLightsFeed(FakeStoreBilling())
        repeat(5) { useOneGuideStep() }
        mainClock.advanceTimeBy(500)
        snap("900_guide_out_of_steps")
        assertTrue(has("guideBadge"), "the bulb shows how many free steps are left")
        assertTrue(!has("paywallSheet"), "nothing opened by itself")

        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { has("paywallSheet") }
        assertTrue(!has("guideBanner"), "no step is given once the free ones are gone")
        assertTrue(has("paywallClose"), "the close button is there from the first frame")
        mainClock.advanceTimeBy(500)
        snap("901_paywall")
        onNodeWithTag("paywallClose").performClick()
        waitUntil(timeoutMillis = 10_000) { !has("paywallSheet") }
    }

    @Test
    fun theSameStepAskedForAgainCostsNothing() = runComposeUiTest {
        // The word puzzle is where it was seen: one step is one free step, however often the player opens it again.
        start(openFeed = true, store = FakeStoreBilling())
        onNodeWithTag("guideButton").assertContentDescriptionContains("5", substring = true)
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { has("guideBanner") }
        onAllNodesWithTag("guideClose")[0].performClick()
        waitUntil(timeoutMillis = 10_000) { !has("guideBanner") }
        onNodeWithTag("guideButton").assertContentDescriptionContains("4", substring = true)
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { has("guideBanner") }
        onAllNodesWithTag("guideClose")[0].performClick()
        waitUntil(timeoutMillis = 10_000) { !has("guideBanner") }
        onNodeWithTag("guideButton").assertContentDescriptionContains("4", substring = true)
    }

    @Test
    fun buyingProFromThePurchaseScreenGivesUnlimitedSteps() = runComposeUiTest {
        val store = FakeStoreBilling()
        openLightsFeed(store)
        repeat(5) { useOneGuideStep() }
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { has("paywallSheet") }
        onNodeWithTag("proBuy").performClick()
        waitUntil(timeoutMillis = 10_000) { has("proThanks") }
        assertEquals(1, store.purchases)
        mainClock.advanceTimeBy(500)
        snap("902_paywall_thanks")
        onNodeWithTag("paywallClose").performClick()
        waitUntil(timeoutMillis = 10_000) { !has("paywallSheet") }
        assertTrue(!has("guideBadge"), "Pro has no count")
        // A sixth step now works.
        useOneGuideStep()
    }

    @Test
    fun aCancelledPurchaseKeepsTheScreenOpenAndUnlocksNothing() = runComposeUiTest {
        val store = FakeStoreBilling().apply { purchaseResult = PurchaseResult.Cancelled }
        openLightsFeed(store)
        repeat(5) { useOneGuideStep() }
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { has("paywallSheet") }
        onNodeWithTag("proBuy").performClick()
        waitUntil(timeoutMillis = 10_000) { has("proMessage") }
        assertTrue(!has("proThanks"))
    }

    @Test
    fun aPendingPurchaseUnlocksWhenItIsApprovedLater() = runComposeUiTest {
        val store = FakeStoreBilling().apply { purchaseResult = PurchaseResult.Pending }
        openLightsFeed(store)
        repeat(5) { useOneGuideStep() }
        onNodeWithTag("guideButton").performClick()
        waitUntil(timeoutMillis = 10_000) { has("paywallSheet") }
        onNodeWithTag("proBuy").performClick()
        waitUntil(timeoutMillis = 10_000) { has("proMessage") }
        assertTrue(!has("proThanks"), "waiting for approval unlocks nothing")
        store.updates.tryEmit(true)
        waitUntil(timeoutMillis = 10_000) { has("proThanks") }
    }

    @Test
    fun proFromTheCachedAnswerHasNoCountAtAll() = runComposeUiTest {
        openLightsFeed(FakeStoreBilling().apply { refreshAnswer = true })
        waitUntil(timeoutMillis = 10_000) { has("guideButton") && !has("guideBadge") }
    }

    @Test
    fun theProfileHasAProCardOnlyWhereThereIsAStore() = runComposeUiTest {
        start(openFeed = false, store = FakeStoreBilling())
        openTab("Profil", "Profile")
        waitUntil(timeoutMillis = 10_000) { has("proCard") }
        snap("903_profile_pro_card")
    }

    @Test
    fun theLookOfAFreePlayerIsAPreviewThatLooksLockedAndOffersPro() = runComposeUiTest {
        start(openFeed = false, store = FakeStoreBilling())
        // With the Pro card on the page the settings row is below the fold of the small screen: scroll to it.
        openTab("Profil", "Profile")
        waitForIdle()
        onAllNodesWithText(if (hasAnyText("Ayarlar")) "Ayarlar" else "Settings")[0].performScrollTo().performClick()
        waitForIdle()
        waitUntil(timeoutMillis = 10_000) { has("accent_blue") }
        snap("904_look_locked")
        assertTrue(!has("lookUsePro"))
        onNodeWithTag("accent_blue", useUnmergedTree = true).performClick()
        waitUntil(timeoutMillis = 10_000) { has("lookUsePro") }
        snap("905_look_preview")
    }

    @Test
    fun withoutAStoreThereIsNoProCardAndNoLimit() = runComposeUiTest {
        openLightsFeed(store = null)
        repeat(6) { useOneGuideStep() }
        assertTrue(!has("guideBadge"))
        assertTrue(!has("paywallSheet"))
        openTab("Profil", "Profile")
        waitForIdle()
        assertTrue(!has("proCard"))
    }

    /**
     * The picture App Review asks for with the product: the purchase screen at a real iPhone size (430 x 932 dp at 3x is
     * 1290 x 2796 px, a size App Store Connect accepts). A JPEG, because it must have no alpha channel.
     * Saved to `shared/build/screenshots/store_paywall_1290x2796.jpg`.
     */
    @Test
    fun storeReviewScreenshotOfThePurchaseScreen() = runSkikoComposeUiTest(size = Size(1290f, 2796f), density = Density(3f)) {
        start(openFeed = false, store = FakeStoreBilling(), screen = 430 to 932)
        openTab("Profil", "Profile")
        waitUntil(timeoutMillis = 10_000) { has("proCard") }
        // The card is at the bottom of the profile, below the fold: bring it into view first.
        onNodeWithTag("proCard", useUnmergedTree = true).performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { has("paywallSheet") }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        val image = onRoot().captureToImage()
        val bytes = org.jetbrains.skia.Image.makeFromBitmap(image.asSkiaBitmap())
            .encodeToData(org.jetbrains.skia.EncodedImageFormat.JPEG, 95)!!.bytes
        val dir = java.io.File("build/screenshots").also { it.mkdirs() }
        java.io.File(dir, "store_paywall_${image.width}x${image.height}.jpg").writeBytes(bytes)
        assertEquals(1290 to 2796, image.width to image.height)
    }
}
