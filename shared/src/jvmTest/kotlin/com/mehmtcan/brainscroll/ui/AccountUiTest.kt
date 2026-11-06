package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.account.AccountEvent
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.DeepLinkInbox
import com.mehmtcan.brainscroll.account.FakeAccountService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The account section of the profile tab and what the app does with the account service. */
@OptIn(ExperimentalTestApi::class)
class AccountUiTest {

    private fun ComposeUiTest.openProfile() {
        openProfilePage("Ayarlar", "Settings")
    }

    @Test
    fun anAnonymousPlayerSeesTheGoogleButtonAndItReachesTheService() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        openProfile()
        snap("10_account_anonymous")

        assertTrue(hasAnyText("Misafir hesap", "Guest account"))
        clickAny("Google ile devam et", "Continue with Google")
        // The click starts a coroutine, so wait for it instead of assuming it already ran.
        waitUntil(timeoutMillis = 5_000) { cloud.account.googleRequests == 1 }
        assertEquals(1, cloud.account.googleRequests)
        assertEquals(false, hasAnyText("Apple ile devam et", "Continue with Apple")) // Android and desktop: no Apple
    }

    @Test
    fun withOnlyGoogleTheProfileSignInGoesStraightToGoogle() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        openTab("Profil", "Profile")
        waitForIdle()
        onNodeWithTag("profileSignIn").performClick()
        waitUntil(timeoutMillis = 5_000) { cloud.account.googleRequests == 1 }
        // No sheet and no trip to the settings: there is nothing to choose between.
        assertEquals(0, onAllNodesWithTag("signInSheet").fetchSemanticsNodes().size)
        assertEquals(1, cloud.account.googleRequests)
    }

    @Test
    fun theProfileSheetOffersAppleFirstAndGoogleUnderIt() = runComposeUiTest {
        val cloud = TestCloud(account = FakeAccountService(canSignInWithApple = true))
        start(cloud)
        openTab("Profil", "Profile")
        waitForIdle()
        onNodeWithTag("profileSignIn").performClick()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("signInSheet").fetchSemanticsNodes().isNotEmpty() }
        snap("13_profile_sign_in_sheet_apple")
        val apple = onNodeWithTag("signInApple").fetchSemanticsNode().boundsInRoot
        val google = onNodeWithTag("signInGoogle").fetchSemanticsNode().boundsInRoot
        assertTrue(apple.bottom <= google.top, "Apple is first, Google under it")
        assertEquals(apple.height, google.height, "the two buttons are the same size")
        onNodeWithTag("signInApple").performClick()
        waitUntil(timeoutMillis = 5_000) { cloud.account.appleRequests == 1 }
    }

    @Test
    fun theAppleButtonIsOfferedWhereAppleSignInExists() = runComposeUiTest {
        val cloud = TestCloud(account = FakeAccountService(canSignInWithApple = true))
        start(cloud)
        openProfile()

        clickAny("Apple ile devam et", "Continue with Apple")
        waitUntil(timeoutMillis = 5_000) { cloud.account.appleRequests == 1 }
        assertEquals(1, cloud.account.appleRequests)
    }

    @Test
    fun aLinkedAccountShowsItsEmailAndCanSignOut() = runComposeUiTest {
        val account = FakeAccountService(AccountState.SignedIn("u1", AccountState.SignedIn.Kind.Google, "player@example.com"))
        val cloud = TestCloud(account = account)
        start(cloud)
        openProfile()
        snap("11_account_google")

        assertTrue(hasAnyText("Google ile giriş yapıldı", "Signed in with Google"))
        assertTrue(hasAnyText("player@example.com"))
        assertEquals(false, hasAnyText("Google ile devam et", "Continue with Google").and(!hasAnyText("yapıldı", "Signed in")))
        clickAny("Çıkış yap", "Sign out")
        // Signing out asks first.
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("confirmYes").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, account.signOuts, "asking is not signing out")
        onNodeWithTag("confirmYes").performClick()
        waitUntil(timeoutMillis = 5_000) { account.signOuts == 1 }
        assertEquals(1, account.signOuts)
    }

    @Test
    fun withoutASessionTheAppSaysItWorksOffline() = runComposeUiTest {
        val cloud = TestCloud(account = FakeAccountService(AccountState.NoSession))
        start(cloud)
        openProfile()
        assertTrue(hasAnyText("Çevrimdışı", "Offline"))
        // Playing is not blocked: the feed tab still has its keyboard.
        openTab("Akış", "Feed")
        waitForIdle()
        assertTrue(hasAnyText("ENTER", "GİR"))
    }

    @Test
    fun aFailedSignInShowsAMessage() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        openProfile()

        cloud.account.events.tryEmit(AccountEvent.SignInFailed("apple error 1000"))
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Giriş yapılamadı", "sign in, try again") }
        // The small print tells what went wrong, so a failure can be diagnosed from a screenshot.
        assertTrue(hasAnyText("apple error 1000"))
        snap("12_sign_in_failed")

        // The x closes it.
        onNodeWithTag("noticeClose").performClick()
        waitForIdle()
        assertTrue(onAllNodesWithTag("noticeClose").fetchSemanticsNodes().isEmpty(), "the message is gone")
    }

    @Test
    fun aMessageCanBeSwipedAway() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        openProfile()
        cloud.account.events.tryEmit(AccountEvent.SignInFailed("apple error 1000"))
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("noticeClose").fetchSemanticsNodes().isNotEmpty() }
        // The texts follow the language of the machine (Turkish here, English on the CI runner).
        val failed = listOf("Giriş yapılamadı", "Couldn't sign in").first { onAllNodesWithText(it, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText(failed, substring = true, ignoreCase = true).performTouchInput { swipeRight() }
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("noticeClose").fetchSemanticsNodes().isEmpty() }
    }

    @Test
    fun theLoginLinkFromTheBrowserReachesTheAccountService() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        DeepLinkInbox.deliver("com.mehmtcan.brainscroll://login-callback?code=abc")
        waitUntil(timeoutMillis = 5_000) { cloud.account.callbackUrls.isNotEmpty() }
        assertEquals(listOf("com.mehmtcan.brainscroll://login-callback?code=abc"), cloud.account.callbackUrls)
    }

    @Test
    fun theAccountIsStartedWhenTheAppStarts() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        waitUntil(timeoutMillis = 5_000) { cloud.account.started > 0 }
    }

    @Test
    fun aFinishedPuzzleIsBackedUpToTheCloud() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        switchToEnglish()
        // Six different real words end the round, won or lost.
        for (word in listOf("CRANE", "SLATE", "GHOST", "POUND", "BRICK", "FLUID")) {
            typeWord(word)
            mainClock.advanceTimeBy(1500)
        }
        waitForIdle()

        // The sync waits a moment after the change and then uploads in the background. The wait runs on the
        // test clock, so the clock is moved forward instead of waiting in real time.
        mainClock.advanceTimeBy(3_000)
        waitUntil(timeoutMillis = 10_000) { cloud.api.resultsOf("test-user").isNotEmpty() }
        assertEquals(1, cloud.api.resultsOf("test-user").size)
    }
}
