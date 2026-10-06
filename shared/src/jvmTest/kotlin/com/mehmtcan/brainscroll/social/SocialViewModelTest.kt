package com.mehmtcan.brainscroll.social

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.prepareDatabase
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.social.Access
import com.mehmtcan.brainscroll.ui.social.Section
import com.mehmtcan.brainscroll.ui.social.SocialEvent
import com.mehmtcan.brainscroll.ui.social.SocialViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModelTest {

    private val google = AccountState.SignedIn("user-1", AccountState.SignedIn.Kind.Google, "a@b.c")
    private val anonymous = AccountState.SignedIn("anon-1", AccountState.SignedIn.Kind.Anonymous, null)

    private val api = FakeSocialApi()
    private val account = MutableStateFlow<AccountState>(google)
    private val invites = MutableSharedFlow<String>(replay = 1)
    private val events = mutableListOf<SocialEvent>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun repository(): GameRepository {
        val file = File.createTempFile("social-test", ".db").also { it.deleteOnExit() }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { prepareDatabase(it) }
        return GameRepository(BrainScrollDatabase(driver))
    }

    /** Builds the view model, records its events, and lets the first load finish. */
    private fun TestScope.viewModel(repository: GameRepository = repository()): SocialViewModel {
        val vm = SocialViewModel(api, repository, account, Language.EN, invites)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { events += it } }
        advanceUntilIdle()
        return vm
    }

    private fun speed(vararg rows: DailyRow) = rows.toList()

    @Test
    fun anAnonymousPlayerCanLookAtTablesButIsNotAMember() = runTest {
        account.value = anonymous
        api.speedToday = speed(DailyRow(1, "alice", 2, 30_000, false))
        val vm = viewModel()
        val ui = vm.ui.value
        assertEquals(Access.Anonymous, ui.access)
        assertEquals("alice", ui.speed?.rows?.single()?.username)
        assertTrue(api.calls.none { it == "profile" }, "no profile for an anonymous account")
    }

    @Test
    fun aMemberWithoutAUsernameIsAskedToChooseOne() = runTest {
        api.member = true
        val vm = viewModel()
        assertEquals(Access.NeedsUsername, vm.ui.value.access)
    }

    @Test
    fun choosingAUsernameMakesThePlayerReadyAndShowsTheInviteCode() = runTest {
        api.member = true
        val vm = viewModel()
        vm.saveUsername("Alice_99")
        advanceUntilIdle()
        assertEquals(Access.Ready, vm.ui.value.access)
        assertEquals("Alice_99", vm.ui.value.username)
        assertEquals("ABCD2345", vm.ui.value.inviteCode)
        assertTrue(events.any { it.kind == SocialEvent.Kind.UsernameSaved })
    }

    @Test
    fun badUsernamesAreExplainedAndNotSent() = runTest {
        api.member = true
        val vm = viewModel()
        vm.saveUsername("no")
        vm.saveUsername("has space")
        advanceUntilIdle()
        assertEquals(2, events.count { it.kind == SocialEvent.Kind.UsernameInvalid })
        assertTrue(api.calls.none { it.startsWith("setUsername") }, "the phone checks the rules first")

        vm.saveUsername("fuck")
        vm.saveUsername("taken")
        advanceUntilIdle()
        assertTrue(events.any { it.kind == SocialEvent.Kind.UsernameNotAllowed })
        assertTrue(events.any { it.kind == SocialEvent.Kind.UsernameTaken })
        assertEquals(Access.NeedsUsername, vm.ui.value.access)
    }

    @Test
    fun theSelectionsAskTheServerForTheRightTable() = runTest {
        api.member = true
        api.username = "alice"
        val vm = viewModel()
        vm.selectLanguage(Language.TR)
        advanceUntilIdle()
        vm.selectScope(DailyScope.AllTime)
        advanceUntilIdle()
        vm.selectFriendsOnly(true)
        advanceUntilIdle()
        assertTrue("dailyBoard:TR:AllTime:true" in api.calls, api.calls.toString())
        vm.selectSection(Section.Streak)
        vm.selectKind(StreakKind.Longest)
        advanceUntilIdle()
        assertTrue("streakBoard:Longest:true" in api.calls, api.calls.toString())
    }

    @Test
    fun anonymousPlayersCannotSwitchToFriendsOnly() = runTest {
        account.value = anonymous
        val vm = viewModel()
        vm.selectFriendsOnly(true)
        advanceUntilIdle()
        assertTrue(!vm.ui.value.friendsOnly)
        assertTrue(api.calls.none { it.endsWith(":true") })
    }

    @Test
    fun whenOfflineTheLastViewIsShownFromTheCache() = runTest {
        api.speedToday = speed(DailyRow(1, "alice", 2, 30_000, false))
        val repository = repository()
        viewModel(repository) // loads and caches
        api.failure = SocialException.Offline()
        val second = viewModel(repository)
        val ui = second.ui.value
        assertEquals("alice", ui.speed?.rows?.single()?.username)
        assertTrue(ui.stale)
        assertTrue(!ui.failed)
    }

    @Test
    fun offlineWithNothingCachedIsAFailureThatCanBeRetried() = runTest {
        api.failure = SocialException.Offline()
        val vm = viewModel()
        assertTrue(vm.ui.value.failed)
        assertNull(vm.ui.value.speed)
        api.failure = null
        api.speedToday = speed(DailyRow(1, "alice", 2, 30_000, false))
        vm.refresh()
        advanceUntilIdle()
        assertTrue(!vm.ui.value.failed)
        assertEquals(1, vm.ui.value.speed?.rows?.size)
    }

    @Test
    fun aCachedViewBelongsToOneAccount() = runTest {
        api.speedToday = speed(DailyRow(1, "alice", 2, 30_000, true))
        val repository = repository()
        viewModel(repository)
        // Another account on the same phone must not see the first account's cached table (it marks "me").
        account.value = AccountState.SignedIn("user-2", AccountState.SignedIn.Kind.Google, null)
        api.failure = SocialException.Offline()
        val other = viewModel(repository)
        assertNull(other.ui.value.speed)
    }

    @Test
    fun addingAFriendByCodeWorksAndUnknownOrOwnCodesAreExplained() = runTest {
        api.member = true
        api.username = "alice"
        val vm = viewModel()
        vm.selectSection(Section.Friends)
        advanceUntilIdle()
        vm.addFriendByCode(" zzzz9999 ")
        advanceUntilIdle()
        assertEquals(listOf("mert"), vm.ui.value.friends?.friends)
        assertTrue(events.any { it.kind == SocialEvent.Kind.FriendAdded && it.name == "mert" })

        vm.addFriendByCode("NOPE0000")
        vm.addFriendByCode("ABCD2345")
        advanceUntilIdle()
        assertTrue(events.any { it.kind == SocialEvent.Kind.CodeNotFound })
        assertTrue(events.any { it.kind == SocialEvent.Kind.IsSelf })
    }

    @Test
    fun requestsCanBeSentAcceptedDeclinedAndCancelled() = runTest {
        api.member = true
        api.username = "alice"
        api.friendsState = FriendsState(incoming = listOf("carol", "dora"))
        val vm = viewModel()
        vm.selectSection(Section.Friends)
        advanceUntilIdle()

        vm.respond("carol", true)
        vm.respond("dora", false)
        advanceUntilIdle()
        assertEquals(FriendsState(friends = listOf("carol")), vm.ui.value.friends)

        vm.sendRequest("mert")
        advanceUntilIdle()
        assertEquals(listOf("mert"), vm.ui.value.friends?.outgoing)
        assertTrue(events.any { it.kind == SocialEvent.Kind.RequestSent })
        vm.cancelRequest("mert")
        vm.removeFriend("carol")
        advanceUntilIdle()
        assertEquals(FriendsState(), vm.ui.value.friends)
        vm.sendRequest("nobody")
        advanceUntilIdle()
        assertTrue(events.any { it.kind == SocialEvent.Kind.PlayerNotFound })
    }

    @Test
    fun friendActionsDoNothingForAnonymousPlayers() = runTest {
        account.value = anonymous
        val vm = viewModel()
        vm.addFriendByCode("ZZZZ9999")
        vm.sendRequest("mert")
        advanceUntilIdle()
        assertTrue(api.calls.none { it.startsWith("addFriend") || it.startsWith("sendRequest") })
    }

    @Test
    fun anInviteLinkAddsTheFriendOnceThePlayerIsReady() = runTest {
        api.member = true
        api.username = "alice"
        viewModel()
        invites.emit("ZZZZ9999")
        advanceUntilIdle()
        assertTrue("addFriendByCode:ZZZZ9999" in api.calls)
        assertTrue(events.any { it.kind == SocialEvent.Kind.FriendAdded && it.name == "mert" })
    }

    @Test
    fun anInviteWaitsForTheSignInAndTheUsername() = runTest {
        account.value = anonymous
        val vm = viewModel()
        invites.emit("ZZZZ9999")
        advanceUntilIdle()
        assertTrue(events.any { it.kind == SocialEvent.Kind.InviteNeedsSignIn })
        assertTrue(api.calls.none { it.startsWith("addFriend") })

        // Signs in with Google, still without a username.
        api.member = true
        account.value = google
        advanceUntilIdle()
        assertTrue(api.calls.none { it.startsWith("addFriend") })
        vm.saveUsername("alice")
        advanceUntilIdle()
        assertTrue("addFriendByCode:ZZZZ9999" in api.calls, "the kept invite is used after the username")
        assertNotNull(vm.ui.value.username)
    }

    @Test
    fun anInviteThatCouldNotBeUsedOfflineIsKept() = runTest {
        api.member = true
        api.username = "alice"
        val vm = viewModel()
        api.failure = SocialException.Offline()
        invites.emit("ZZZZ9999")
        advanceUntilIdle()
        assertTrue(events.any { it.kind == SocialEvent.Kind.Offline })
        api.failure = null
        vm.saveUsername("alice2") // any later trigger that checks for a pending invite
        advanceUntilIdle()
        assertTrue(api.calls.count { it == "addFriendByCode:ZZZZ9999" } >= 2, api.calls.toString())
    }

    @Test
    fun signingOutClearsWhatWasShown() = runTest {
        api.speedToday = speed(DailyRow(1, "alice", 2, 30_000, false))
        val vm = viewModel()
        assertNotNull(vm.ui.value.speed)
        account.value = AccountState.NoSession
        advanceUntilIdle()
        assertEquals(Access.Offline, vm.ui.value.access)
        assertNull(vm.ui.value.speed)
    }
}
