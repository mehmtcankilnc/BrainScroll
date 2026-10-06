package com.mehmtcan.brainscroll.account

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Who the app is signed in as. The app always works offline; this only matters for the cloud backup. */
sealed interface AccountState {
    /** The stored session is being loaded. */
    data object Loading : AccountState

    /** No session (for example the very first start without a network). Local play is not affected. */
    data object NoSession : AccountState

    data class SignedIn(val userId: String, val kind: Kind, val email: String?) : AccountState {
        enum class Kind { Anonymous, Google, Apple }
    }
}

/** One-off things the UI should tell the player about. */
sealed interface AccountEvent {
    data object SignedIn : AccountEvent
    /** [detail] is a short technical hint (an error code), shown in small print so a failure can be diagnosed. */
    data class SignInFailed(val detail: String? = null) : AccountEvent

    /** The account and everything in the cloud is gone. */
    data object AccountDeleted : AccountEvent

    /** The deletion did not happen (no connection, or the server refused). Nothing was changed. */
    data object DeleteFailed : AccountEvent
}

interface AccountService {
    val state: StateFlow<AccountState>
    val events: SharedFlow<AccountEvent>

    /** True on iOS only: Apple sign-in is native there and not offered elsewhere (docs/decisions.md). */
    val canSignInWithApple: Boolean

    /**
     * Makes sure there is a session. Without one an anonymous account is created, so every install has a cloud
     * backup from the first minute. If that fails (offline) it is retried until it works, in the background.
     */
    suspend fun start()

    /** Signs in with Google in the browser. The outcome arrives through [state] and [events]. */
    suspend fun signInWithGoogle()

    /** Signs in with Apple (native sheet, iOS only). */
    suspend fun signInWithApple()

    /** Back to a fresh anonymous account. Everything stays on the device, it is offered to that account again. */
    suspend fun signOut()

    /**
     * Deletes the account and everything stored for it in the cloud, then signs out of it. Returns false (and nothing
     * is changed) if the server could not be reached. The caller clears the data on the device and then calls
     * [start] for a new anonymous account.
     */
    suspend fun deleteAccount(): Boolean

    /** The app was opened through its login link after the browser finished. */
    suspend fun handleCallbackUrl(url: String)
}
