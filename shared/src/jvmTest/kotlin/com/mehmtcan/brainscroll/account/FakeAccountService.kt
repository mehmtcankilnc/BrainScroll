package com.mehmtcan.brainscroll.account

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** An account service that never touches the network, for UI tests. The tests set [state] and read the counters. */
class FakeAccountService(
    initial: AccountState = AccountState.SignedIn("test-user", AccountState.SignedIn.Kind.Anonymous, null),
    override val canSignInWithApple: Boolean = false,
) : AccountService {
    override val state = MutableStateFlow(initial)
    override val events = MutableSharedFlow<AccountEvent>(extraBufferCapacity = 4)

    var started = 0
    var googleRequests = 0
    var appleRequests = 0
    var signOuts = 0
    val callbackUrls = mutableListOf<String>()

    override suspend fun start() { started++ }
    override suspend fun signInWithGoogle() { googleRequests++ }
    override suspend fun signInWithApple() { appleRequests++ }
    override suspend fun signOut() { signOuts++ }
    override suspend fun handleCallbackUrl(url: String) { callbackUrls += url }
}
