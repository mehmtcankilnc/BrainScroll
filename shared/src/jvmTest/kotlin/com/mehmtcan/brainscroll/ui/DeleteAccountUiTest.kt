package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.FakeAccountService
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Deleting the account from the profile: a second step, the server first, then the device. */
@OptIn(ExperimentalTestApi::class)
class DeleteAccountUiTest {

    private fun signedInCloud() = TestCloud(
        account = FakeAccountService(AccountState.SignedIn("user-1", AccountState.SignedIn.Kind.Google, "a@b.c")),
    )

    /** Solves today's daily puzzle so the device has a result, then opens the profile. */
    private fun ComposeUiTest.playAndOpenProfile(cloud: TestCloud) {
        start(cloud)
        openTab("Günlük", "Daily")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") }
        clickAny("Başla", "Start")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("ENTER", "GİR") }
        cloud.daily.clock += 83_000
        typeWord(if (hasAnyText("Günün bulmacası")) "KİTAP" else "CRANE")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()
        openTab("Profil", "Profile")
        waitForIdle()
    }

    private fun ComposeUiTest.scrollProfileTo(vararg texts: String) {
        onNode(hasScrollAction()).performScrollToNode(texts.map { hasText(it, substring = true) }.reduce { a, b -> a or b })
        waitForIdle()
    }

    private fun ComposeUiTest.hasBestTime() = hasAnyText("En iyi süre: 01:23", "Best time: 01:23")

    @Test
    fun theFirstTapOnlyAsksAndCancelChangesNothing() = runComposeUiTest {
        val cloud = signedInCloud()
        playAndOpenProfile(cloud)
        clickAny("Hesabı sil", "Delete account")
        waitForIdle()
        snap("50_delete_confirm")
        assertTrue(hasAnyText("Hesabın silinsin mi?", "Delete your account?"))
        assertEquals(0, cloud.account.deleteRequests, "asking is not deleting")

        clickAny("Vazgeç", "Cancel")
        waitForIdle()
        assertFalse(hasAnyText("Hesabın silinsin mi?", "Delete your account?"))
        assertEquals(0, cloud.account.deleteRequests)
        scrollProfileTo("En iyi süre: 01:23", "Best time: 01:23")
        assertTrue(hasBestTime(), "the progress is still there")
    }

    @Test
    fun confirmingDeletesTheAccountAndEverythingOnTheDevice() = runComposeUiTest {
        val cloud = signedInCloud()
        playAndOpenProfile(cloud)
        scrollProfileTo("En iyi süre: 01:23", "Best time: 01:23")
        assertTrue(hasBestTime())
        scrollProfileTo("Hesabı sil", "Delete account")
        clickAny("Hesabı sil", "Delete account")
        waitForIdle()
        clickAny("Kalıcı olarak sil", "Delete permanently")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Hesap silindi", "Account deleted") }
        waitForIdle()
        snap("51_account_deleted")

        assertEquals(1, cloud.account.deleteRequests)
        assertEquals(2, cloud.account.started, "a new anonymous account is started afterwards (the first start was at launch)")
        scrollProfileTo("Günlük bulmaca", "Daily puzzle")
        assertFalse(hasBestTime(), "the device forgot the result")
    }

    @Test
    fun ifTheServerCannotBeReachedNothingIsDeleted() = runComposeUiTest {
        val cloud = signedInCloud()
        playAndOpenProfile(cloud)
        cloud.account.deleteSucceeds = false
        scrollProfileTo("Hesabı sil", "Delete account")
        clickAny("Hesabı sil", "Delete account")
        waitForIdle()
        clickAny("Kalıcı olarak sil", "Delete permanently")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Hesap silinemedi", "Couldn't delete the account") }
        snap("52_delete_failed")

        assertEquals(1, cloud.account.deleteRequests)
        scrollProfileTo("En iyi süre: 01:23", "Best time: 01:23")
        assertTrue(hasBestTime(), "the progress is untouched")
    }

    @Test
    fun anAnonymousAccountDoesNotOfferDeletion() = runComposeUiTest {
        start(TestCloud())
        openTab("Profil", "Profile")
        waitForIdle()
        assertFalse(hasAnyText("Hesabı sil", "Delete account"))
    }
}
