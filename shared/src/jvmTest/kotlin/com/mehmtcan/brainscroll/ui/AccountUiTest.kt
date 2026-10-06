package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        openTab("Profil", "Profile")
        waitForIdle()
    }

    @Test
    fun anAnonymousPlayerSeesTheGoogleButtonAndItReachesTheService() = runComposeUiTest {
        val cloud = TestCloud()
        start(cloud)
        openProfile()
        snap("10_account_anonymous")

        assertTrue(hasAnyText("Anonim hesap", "Anonymous account"))
        clickAny("Google ile giriş yap", "Sign in with Google")
        // The click starts a coroutine, so wait for it instead of assuming it already ran.
        waitUntil(timeoutMillis = 5_000) { cloud.account.googleRequests == 1 }
        assertEquals(1, cloud.account.googleRequests)
        assertEquals(false, hasAnyText("Apple ile giriş yap", "Sign in with Apple")) // Android and desktop: no Apple
    }

    @Test
    fun theAppleButtonIsOfferedWhereAppleSignInExists() = runComposeUiTest {
        val cloud = TestCloud(account = FakeAccountService(canSignInWithApple = true))
        start(cloud)
        openProfile()

        clickAny("Apple ile giriş yap", "Sign in with Apple")
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
        assertEquals(false, hasAnyText("Google ile giriş yap", "Sign in with Google").and(!hasAnyText("yapıldı", "Signed in")))
        clickAny("Çıkış yap", "Sign out")
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
        openProfile()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Her şey yedeklendi", "Everything is backed up") }
    }
}
