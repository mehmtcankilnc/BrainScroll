package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.DeepLinkInbox
import com.mehmtcan.brainscroll.account.FakeAccountService
import com.mehmtcan.brainscroll.social.DailyRow
import com.mehmtcan.brainscroll.social.FakeSocialApi
import com.mehmtcan.brainscroll.social.FriendsState
import com.mehmtcan.brainscroll.social.SocialException
import com.mehmtcan.brainscroll.social.StreakRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The ranks tab, driven through the real screen with a fake server. */
@OptIn(ExperimentalTestApi::class)
class SocialUiTest {

    private val google = AccountState.SignedIn("user-1", AccountState.SignedIn.Kind.Google, "a@b.c")

    private fun cloud(member: Boolean, username: String? = null, configure: FakeSocialApi.() -> Unit = {}) = TestCloud(
        account = FakeAccountService(if (member) google else AccountState.SignedIn("anon", AccountState.SignedIn.Kind.Anonymous, null)),
        social = FakeSocialApi(member = member, username = username).apply(configure),
    )

    private fun ComposeUiTest.openRanks(cloud: TestCloud) {
        start(cloud)
        openTab("Sıralama", "Ranks")
        waitForIdle()
    }

    private fun ComposeUiTest.waitForText(vararg texts: String) =
        waitUntil(timeoutMillis = 5_000) { hasAnyText(*texts) }

    private fun ComposeUiTest.scrollTo(vararg texts: String) {
        onNode(hasScrollAction()).performScrollToNode(texts.map { hasText(it, substring = true) }.reduce { a, b -> a or b })
        waitForIdle()
    }

    private val table = listOf(
        DailyRow(1, "alice", 2, 30_000),
        DailyRow(2, "bob", 3, 12_000),
        DailyRow(3, "carol", 3, 20_000, isMe = true),
    )

    @Test
    fun anAnonymousPlayerSeesTheTableAndAnInvitationToSignIn() = runComposeUiTest {
        val cloud = cloud(member = false) { speedToday = table.map { it.copy(isMe = false) } }
        openRanks(cloud)
        waitForText("alice")
        snap("20_ranks_anonymous")

        assertTrue(hasAnyText("alice") && hasAnyText("bob"))
        assertTrue(hasAnyText("2/6"), "the guesses are shown")
        assertTrue(hasAnyText("00:30"), "and the time")
        assertTrue(hasAnyText("Google ile giriş yap", "Sign in with Google"))
        assertFalse(hasAnyText("Herkes", "Everyone"), "no everyone/friends filter for anonymous players")
    }

    @Test
    fun aMemberChoosesAUsernameAndThenSeesTheirPlace() = runComposeUiTest {
        val cloud = cloud(member = true) { speedToday = table }
        openRanks(cloud)
        waitForText("Kullanıcı adı seç", "Choose a username")
        snap("21_ranks_choose_username")

        onAllNodes(hasSetTextAction())[0].performTextInput("carol")
        clickAny("Kaydet", "Save")
        waitUntil(timeoutMillis = 5_000) { cloud.social.username == "carol" }
        waitForIdle()
        snap("22_ranks_ready")

        assertFalse(hasAnyText("Kullanıcı adı seç", "Choose a username"), "the card is gone")
        assertTrue(hasAnyText("carol (sen)", "carol (you)"), "my own row is marked")
    }

    @Test
    fun aTakenUsernameIsExplainedAndTheCardStays() = runComposeUiTest {
        val cloud = cloud(member = true)
        openRanks(cloud)
        waitForText("Kullanıcı adı seç", "Choose a username")
        onAllNodes(hasSetTextAction())[0].performTextInput("taken")
        clickAny("Kaydet", "Save")
        waitForText("Bu ad alınmış", "That name is taken")
        assertTrue(hasAnyText("Kullanıcı adı seç", "Choose a username"))
    }

    @Test
    fun streakTablesAndTheFriendsFilterWork() = runComposeUiTest {
        val cloud = cloud(member = true, username = "carol") {
            streaksCurrent = listOf(StreakRow(1, "dora", 12), StreakRow(2, "carol", 5, isMe = true))
            streaksLongest = listOf(StreakRow(1, "dora", 30))
        }
        openRanks(cloud)
        clickAny("Seri", "Streaks")
        waitForText("dora")
        snap("23_ranks_streaks")
        assertTrue(hasAnyText("12") && hasAnyText("carol (sen)", "carol (you)"))

        clickAny("En uzun", "Longest")
        waitUntil(timeoutMillis = 5_000) { cloud.social.calls.any { it == "streakBoard:Longest:false" } }
        // The section chip and the filter chip both say "Friends"; the filter is the second one.
        val friendsLabel = if (onAllNodesWithText("Arkadaşlar").fetchSemanticsNodes().isNotEmpty()) "Arkadaşlar" else "Friends"
        onAllNodesWithText(friendsLabel)[1].performClick()
        waitUntil(timeoutMillis = 5_000) { cloud.social.calls.any { it == "streakBoard:Longest:true" } }
    }

    @Test
    fun theFriendsPartShowsTheInviteCodeAndAddsFriendsByCode() = runComposeUiTest {
        val cloud = cloud(member = true, username = "carol") {
            friendsState = FriendsState(friends = listOf("bob"), incoming = listOf("dora"), outgoing = listOf("erin"))
        }
        openRanks(cloud)
        clickFirstChip("Arkadaşlar", "Friends")
        waitForText("ABCD2345")
        snap("24_ranks_friends")

        assertTrue(hasAnyText("bob") && hasAnyText("dora"))
        assertTrue(hasAnyText("Kabul et", "Accept"))
        scrollTo("erin") // the list is lazy: only what is on screen exists
        assertTrue(hasAnyText("erin"), "the request I sent")

        scrollTo("Kodla ekle", "Add by code")
        onAllNodes(hasSetTextAction())[0].performTextInput("zzzz9999")
        clickAny("Ekle", "Add")
        waitUntil(timeoutMillis = 5_000) { cloud.social.calls.contains("addFriendByCode:ZZZZ9999") }
        waitForText("mert")
        assertEquals(listOf("bob", "mert"), cloud.social.friendsState.friends)
    }

    @Test
    fun anInviteLinkAddsTheFriendAndOpensTheRanksTab() = runComposeUiTest {
        val cloud = cloud(member = true, username = "carol")
        start(cloud)
        DeepLinkInbox.deliver("com.mehmtcan.brainscroll://invite?code=ZZZZ9999")
        waitUntil(timeoutMillis = 5_000) { cloud.social.calls.contains("addFriendByCode:ZZZZ9999") }
        waitForText("mert artık arkadaşın", "mert is now your friend")
        snap("25_invite_link")
        assertTrue(cloud.account.callbackUrls.isEmpty(), "an invite link is not a login link")
    }

    @Test
    fun offlineShowsAMessageAndARetryButton() = runComposeUiTest {
        val cloud = cloud(member = false) { failure = SocialException.Offline() }
        openRanks(cloud)
        waitForText("Tablo yüklenemedi", "The table can't be loaded")
        snap("26_ranks_offline")
        cloud.social.failure = null
        cloud.social.speedToday = table
        clickAny("Tekrar dene", "Try again")
        waitForText("alice")
    }

    /** The section chip and the friends filter both say "Friends". The section chip is the first match from the top. */
    private fun ComposeUiTest.clickFirstChip(vararg texts: String) {
        val text = texts.first { onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText(text)[0].performClick()
    }
}
