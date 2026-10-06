package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.MergeResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Signing in from an anonymous account to an account that already exists: a ticket is asked for before leaving,
 * and handed over once the other account is signed in (supabase/migrations/...account_merge.sql).
 */
@OptIn(ExperimentalTestApi::class)
class AccountMergeUiTest {

    private fun google(userId: String, email: String = "a@b.c") = AccountState.SignedIn(userId, AccountState.SignedIn.Kind.Google, email)

    /** The profile of an anonymous account, then "Sign in with Google" is tapped. */
    private fun ComposeUiTest.tapSignIn(cloud: TestCloud) {
        start(cloud)
        openTab("Profil", "Profile")
        waitForIdle()
        clickAny("Google ile giriş yap", "Sign in with Google")
        waitUntil(timeoutMillis = 5_000) { cloud.account.googleRequests == 1 }
    }

    private fun ComposeUiTest.waitForCalls(cloud: TestCloud, expected: Int) =
        waitUntil(timeoutMillis = 5_000) { cloud.merge.calls.size >= expected }

    @Test
    fun theTicketIsAskedForBeforeSigningInAndHandedOverToTheOtherAccount() = runComposeUiTest {
        val cloud = TestCloud()
        tapSignIn(cloud)
        assertEquals(listOf("start"), cloud.merge.calls, "asked for while still anonymous")

        // The sign-in ended in an account that already existed: another user id.
        cloud.account.state.value = google("existing-user")
        waitForCalls(cloud, 2)
        assertEquals(listOf("start", "complete:ticket-1"), cloud.merge.calls)

        // The ticket is used up: another change of the account hands nothing over again.
        cloud.account.state.value = google("existing-user", email = "other@b.c")
        waitForIdle()
        assertEquals(2, cloud.merge.calls.size)
    }

    @Test
    fun whenTheIdentityWasLinkedToTheSameAccountNothingIsMoved() = runComposeUiTest {
        val cloud = TestCloud()
        tapSignIn(cloud)
        // Linking keeps the account: the same user id, now with a Google identity.
        cloud.account.state.value = google("test-user")
        waitForIdle()
        assertEquals(listOf("start"), cloud.merge.calls, "no hand-over: it is one account")

        // And the ticket was forgotten: a later, different sign-in does not use it.
        cloud.account.state.value = google("someone-else")
        waitForIdle()
        assertEquals(listOf("start"), cloud.merge.calls)
    }

    @Test
    fun aTicketThatCouldNotBeUsedYetIsKeptAndTriedAgain() = runComposeUiTest {
        val cloud = TestCloud()
        cloud.merge.result = MergeResult.Retry
        tapSignIn(cloud)
        cloud.account.state.value = google("existing-user")
        waitForCalls(cloud, 2)

        cloud.merge.result = MergeResult.Done
        cloud.account.state.value = google("existing-user", email = "again@b.c")
        waitForCalls(cloud, 3)
        assertEquals(listOf("start", "complete:ticket-1", "complete:ticket-1"), cloud.merge.calls)

        cloud.account.state.value = google("existing-user", email = "third@b.c")
        waitForIdle()
        assertEquals(3, cloud.merge.calls.size, "done: not tried a third time")
    }

    @Test
    fun aTicketThePlayerCanNeverUseIsForgottenAtOnce() = runComposeUiTest {
        val cloud = TestCloud()
        cloud.merge.result = MergeResult.Invalid
        tapSignIn(cloud)
        cloud.account.state.value = google("existing-user")
        waitForCalls(cloud, 2)
        cloud.account.state.value = google("existing-user", email = "again@b.c")
        waitForIdle()
        assertEquals(2, cloud.merge.calls.size, "an invalid ticket is not sent again")
    }

    @Test
    fun withoutAConnectionTheSignInGoesOnWithoutATicket() = runComposeUiTest {
        val cloud = TestCloud()
        cloud.merge.ticket = null
        tapSignIn(cloud)
        assertEquals(1, cloud.account.googleRequests, "the sign-in was still started")
        cloud.account.state.value = google("existing-user")
        waitForIdle()
        assertEquals(listOf("start"), cloud.merge.calls, "nothing to hand over")
    }

    @Test
    fun anAccountThatWasNeverAnonymousAsksForNoTicket() = runComposeUiTest {
        val cloud = TestCloud(account = com.mehmtcan.brainscroll.account.FakeAccountService(google("already-google")))
        start(cloud)
        openTab("Profil", "Profile")
        waitForIdle()
        assertTrue(cloud.merge.calls.isEmpty())
        assertTrue(cloud.account.googleRequests == 0)
    }
}
