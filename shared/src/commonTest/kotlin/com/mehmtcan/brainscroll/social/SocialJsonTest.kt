package com.mehmtcan.brainscroll.social

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The JSON the database functions return (see the jsonb_build_object calls in the migration) and the invite link. */
class SocialJsonTest {

    @Test
    fun readsADailyBoardWithMyRowBelowTheTop() {
        val json = """{"rows":[{"rank":1,"username":"alice","guesses":2,"duration_ms":30000,"is_me":false}],
            "me":{"rank":57,"username":"carol","guesses":4,"duration_ms":91000,"is_me":true}}"""
        val board = socialJson.decodeFromString<Board<DailyRow>>(json)
        assertEquals(DailyRow(1, "alice", 2, 30_000, false), board.rows.single())
        assertEquals(57, board.me?.rank)
        assertTrue(board.me!!.isMe)
    }

    @Test
    fun readsAnEmptyBoardWithNoRowOfMine() {
        val board = socialJson.decodeFromString<Board<StreakRow>>("""{"rows":[],"me":null}""")
        assertTrue(board.rows.isEmpty())
        assertNull(board.me)
    }

    @Test
    fun readsAProfileBeforeAndAfterChoosingAName() {
        val fresh = socialJson.decodeFromString<MyProfile>("""{"is_member":true,"username":null,"invite_code":null}""")
        assertEquals(MyProfile(true, null, null), fresh)
        val named = socialJson.decodeFromString<MyProfile>("""{"is_member":true,"username":"Alice_99","invite_code":"ABCD2345"}""")
        assertEquals("ABCD2345", named.inviteCode)
    }

    @Test
    fun readsFriendsAndRequests() {
        val state = socialJson.decodeFromString<FriendsState>("""{"friends":["bob"],"incoming":["carol"],"outgoing":[]}""")
        assertEquals(FriendsState(listOf("bob"), listOf("carol"), emptyList()), state)
    }

    @Test
    fun readsTheStatusOfARequest() {
        assertEquals(RequestStatus.Friends, socialJson.decodeFromString<FriendResult>("""{"username":"bob","status":"FRIENDS"}""").status)
        assertEquals(RequestStatus.Requested, socialJson.decodeFromString<FriendResult>("""{"username":"bob","status":"REQUESTED"}""").status)
    }

    @Test
    fun aCachedBoardReadsBackTheSame() {
        val board = Board(listOf(DailyRow(1, "alice", 2, 30_000, true)), me = null)
        assertEquals(board, socialJson.decodeFromString<Board<DailyRow>>(socialJson.encodeToString(board)))
    }

    @Test
    fun usernameRulesMatchTheServer() {
        assertTrue(isValidUsername("Alice_99"))
        assertTrue(isValidUsername("abc"))
        assertTrue(isValidUsername("a".repeat(16)))
        listOf("ab", "a".repeat(17), "has space", "tür_kçe", "a-b", "").forEach { assertTrue(!isValidUsername(it), it) }
    }

    @Test
    fun inviteLinksRoundTripAndOtherLinksAreNotInvites() {
        assertEquals("ABCD2345", parseInviteCode(inviteLink("ABCD2345")))
        assertEquals("ABCD2345", parseInviteCode("com.mehmtcan.brainscroll://invite?code=abcd2345"))
        assertEquals("ABCD2345", parseInviteCode("com.mehmtcan.brainscroll://invite/?x=1&code=ABCD2345#frag"))
        assertNull(parseInviteCode("com.mehmtcan.brainscroll://invite"))
        assertNull(parseInviteCode("com.mehmtcan.brainscroll://invite?code="))
        assertNull(parseInviteCode("com.mehmtcan.brainscroll://login-callback?code=abc"))
        assertNull(parseInviteCode("com.mehmtcan.brainscroll://inviteEVIL?code=ABCD2345"))
        assertNull(parseInviteCode("https://example.com/invite?code=ABCD2345"))
    }
}
