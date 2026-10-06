package com.mehmtcan.brainscroll.account

/** A stand-in for the two merge functions on the server. Tests choose the answers and read the calls. */
class FakeAccountMergeApi : AccountMergeApi {
    /** What start() hands out. Null imitates a server that could not be reached. */
    var ticket: String? = "ticket-1"

    /** What complete() answers. */
    var result = MergeResult.Done

    val calls = mutableListOf<String>()

    override suspend fun start(): String? {
        calls += "start"
        return ticket
    }

    override suspend fun complete(ticket: String): MergeResult {
        calls += "complete:$ticket"
        return result
    }
}
