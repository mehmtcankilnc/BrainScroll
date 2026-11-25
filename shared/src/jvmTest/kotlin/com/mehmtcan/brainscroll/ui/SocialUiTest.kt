package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.DeepLinkInbox
import com.mehmtcan.brainscroll.account.FakeAccountService
import com.mehmtcan.brainscroll.game.Difficulty
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
        assertTrue(hasAnyText("2 tahmin", "2 guesses"), "the guesses are shown")
        assertTrue(hasAnyText("00:30"), "and the time")
        assertTrue(hasAnyText("Google ile devam et", "Continue with Google"))
        assertFalse(hasAnyText("Herkes", "Everyone"), "no everyone/friends filter for anonymous players")
    }

    @Test
    fun aMemberChoosesAUsernameAndThenSeesTheirPlace() = runComposeUiTest {
        // The random name the phone picks fails to save, so the card that asks for a name is shown.
        val cloud = cloud(member = true) { speedToday = table; failRandomNames = true }
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
        val cloud = cloud(member = true) { failRandomNames = true }
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

        // The choices are on the page itself.
        waitForIdle()
        snap("27_ranks_filters")
        clickAny("En uzun", "Longest")
        waitUntil(timeoutMillis = 5_000) { cloud.social.calls.any { it == "streakBoard:Longest:false" } }
        // The friends have a page of their own now, so the only "Friends" on the ranks page is the choice of whose results.
        val friendsLabel = if (onAllNodesWithText("Arkadaşlar").fetchSemanticsNodes().isNotEmpty()) "Arkadaşlar" else "Friends"
        onAllNodesWithText(friendsLabel)[0].performClick()
        waitUntil(timeoutMillis = 5_000) { cloud.social.calls.any { it == "streakBoard:Longest:true" } }
    }

    @Test
    fun theTableOfAFeedGameShowsWhatItIsRankedByAndOneTableForEveryLevel() = runComposeUiTest {
        val cloud = cloud(member = true, username = "alice") {
            gameAllTime = listOf(DailyRow(1, "bob", 8, 99_000), DailyRow(2, "alice", 5, 50_000, isMe = true))
        }
        openRanks(cloud)
        // A series: the rounds are the big number, the time the small one; the chips of the levels are there.
        // The computer running the tests may speak either language, so look for the name in both.
        val cubeFolding = listOf("Küp Katlama", "Cube Folding").first { onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText(cubeFolding).onFirst().performScrollTo().performClick()
        waitForText("8 tur", "8 rounds")
        snap("470_ranks_series_table")
        assertTrue(hasAnyText("01:39"))
        assertTrue(hasAnyText("Önce en çok geçilen tur", "Most rounds cleared first"), "the caption says what the table is ranked by")
        assertTrue(hasAnyText("Orta", "Medium") && hasAnyText("Zor", "Hard") && hasAnyText("Kolay", "Easy"))
        val hard = listOf("Zor", "Hard").first { onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithText(hard).onFirst().performClick()
        waitUntil(timeoutMillis = 5_000) { cloud.social.lastDifficulty == Difficulty.Hard }

        // A game played for time: the time is the big number, the moves the small one.
        cloud.social.gameAllTime = listOf(DailyRow(1, "carol", 31, 42_000))
        onAllNodesWithText("Sudoku").onFirst().performScrollTo().performClick()
        waitForText("00:42")
        snap("471_ranks_time_table")
        assertTrue(hasAnyText("31 hamle", "31 moves"))
        assertTrue(hasAnyText("Önce en kısa süre", "Shortest time first"))
    }

    @Test
    fun theRanksPageHasAWayToReportAUsername() = runComposeUiTest {
        openRanks(cloud(member = true, username = "carol"))
        assertTrue(hasAnyText("Uygunsuz bir kullanıcı adı", "inappropriate username"), "other players' names can be reported (App Store 1.2)")
    }

    @Test
    fun theRanksPageHasNoFriendsTab() = runComposeUiTest {
        openRanks(cloud(member = true, username = "carol"))
        assertFalse(hasAnyText("Davet kodun", "Your invite code"))
        // The only "Friends" on the ranks page is the choice of whose results (a chip); there is no friends tab.
        assertEquals(1, onAllNodesWithText("Arkadaşlar").fetchSemanticsNodes().size + onAllNodesWithText("Friends").fetchSemanticsNodes().size, "only the filter chip")
    }

    @Test
    fun theFriendsPartShowsTheInviteCodeAndAddsFriendsByCode() = runComposeUiTest {
        val cloud = cloud(member = true, username = "carol") {
            friendsState = FriendsState(friends = listOf("bob"), incoming = listOf("dora"), outgoing = listOf("erin"))
        }
        start(cloud)
        // The friends are a page of the profile, not a part of the ranks.
        openProfilePage("Arkadaşlar", "Friends")
        waitForText("ABCD2345")
        snap("24_ranks_friends")

        assertTrue(hasAnyText("bob") && hasAnyText("dora"))
        assertTrue(hasAnyText("Kabul et", "Accept"))
        scrollTo("erin") // the list is lazy: only what is on screen exists
        assertTrue(hasAnyText("erin"), "the request I sent")

        scrollTo("Davet koduyla", "With invite code")
        onAllNodes(hasSetTextAction())[0].performTextInput("zzzz9999")
        clickAny("Ekle", "Add")
        waitUntil(timeoutMillis = 5_000) { cloud.social.calls.contains("addFriendByCode:ZZZZ9999") }
        waitForText("mert")
        assertEquals(listOf("bob", "mert"), cloud.social.friendsState.friends)
    }

    @Test
    fun withNobodyThereIsNoCountAndAPictureTellsWhatToDo() = runComposeUiTest {
        val cloud = cloud(member = true, username = "carol") { friendsState = FriendsState() }
        start(cloud)
        openProfilePage("Arkadaşlar", "Friends")
        waitForText("ABCD2345")
        scrollTo("Birlikte oynamak daha keyifli", "Playing together is more fun")
        snap("240_friends_empty")
        assertTrue(hasAnyText("Birlikte oynamak daha keyifli", "Playing together is more fun"))
        assertTrue(hasAnyText("Davet kodunu paylaş veya bir arkadaşını ekle.", "Share your invite code or add a friend."))
        assertEquals(0, onAllNodesWithTag("friendsCount").fetchSemanticsNodes().size, "no \"0\" beside the title")
    }

    @Test
    fun withFriendsTheirNumberIsShownAndTheCopyButtonsTurnIntoTicksForAWhile() = runComposeUiTest {
        val cloud = cloud(member = true, username = "carol") { friendsState = FriendsState(friends = listOf("bob", "dora")) }
        start(cloud)
        openProfilePage("Arkadaşlar", "Friends")
        waitForText("ABCD2345")
        snap("241_friends_invite_card")
        scrollTo("Arkadaşların", "Your friends")
        assertEquals("2", onNodeWithTag("friendsCount").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString())

        onNodeWithTag("copyInvite").performClick()
        waitForIdle()
        assertTrue(hasAnyText("Kopyalandı!", "Copied!"), "the message button says it copied")
        mainClock.advanceTimeBy(3_500)
        waitForIdle()
        assertTrue(hasAnyText("Davet mesajını kopyala", "Copy invite message"), "and goes back after a few seconds")
    }

    @Test
    fun anInviteLinkAddsTheFriendAndOpensTheFriendsPage() = runComposeUiTest {
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
