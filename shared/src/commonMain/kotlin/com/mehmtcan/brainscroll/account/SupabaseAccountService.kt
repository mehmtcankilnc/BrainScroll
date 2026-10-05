package com.mehmtcan.brainscroll.account

import com.mehmtcan.brainscroll.cloud.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * [AccountService] on top of Supabase Auth.
 *
 * Signing in with Google from an anonymous account tries to LINK the identity first, so the account (and its cloud
 * data) stays the same one. If that Google account already belongs to somebody else (for example it was used on
 * another phone), linking is refused and the app signs in to that existing account instead. Nothing is lost
 * either way: the sync engine offers this device's history to whichever account ends up signed in.
 */
class SupabaseAccountService(
    private val client: SupabaseClient,
    private val apple: AppleSignIn?,
    private val scope: CoroutineScope,
    private val retryDelayMillis: Long = 30_000,
) : AccountService {

    private val auth = client.auth

    override val state: StateFlow<AccountState> =
        auth.sessionStatus.map(::toAccountState).stateIn(scope, SharingStarted.Eagerly, AccountState.Loading)

    private val _events = MutableSharedFlow<AccountEvent>(extraBufferCapacity = 4)
    override val events: SharedFlow<AccountEvent> = _events

    override val canSignInWithApple: Boolean get() = apple != null

    override suspend fun start() {
        auth.awaitInitialization()
        // Only when there is truly no session: while a refresh is still being retried the library handles it.
        while (auth.sessionStatus.value is SessionStatus.NotAuthenticated) {
            try {
                auth.signInAnonymously()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                delay(retryDelayMillis) // offline: the app works locally, try again in a while
            }
        }
    }

    override suspend fun signInWithGoogle() {
        guardedSignIn {
            if (isAnonymous(auth.currentUserOrNull())) {
                auth.linkIdentity(Google) // opens the browser; the result comes back through handleCallbackUrl
            } else {
                auth.signInWith(Google)
            }
        }
    }

    override suspend fun signInWithApple() {
        val native = apple ?: return
        val credential = try {
            native.requestCredential() ?: return // the player closed the sheet: nothing to report
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _events.tryEmit(AccountEvent.SignInFailed)
            return
        }
        guardedSignIn {
            if (isAnonymous(auth.currentUserOrNull())) {
                try {
                    auth.linkIdentityWithIdToken(Apple, credential.idToken) { nonce = credential.rawNonce }
                } catch (e: AuthRestException) {
                    if (e.errorCode != AuthErrorCode.IdentityAlreadyExists) throw e
                    signInWithAppleToken(credential) // this Apple account already exists: use it
                }
            } else {
                signInWithAppleToken(credential)
            }
            _events.tryEmit(AccountEvent.SignedIn)
        }
    }

    private suspend fun signInWithAppleToken(credential: AppleCredential) {
        auth.signInWith(IDToken) {
            idToken = credential.idToken
            provider = Apple
            nonce = credential.rawNonce
        }
    }

    override suspend fun signOut() {
        guardedSignIn {
            auth.signOut()
            // A fresh anonymous account, so the app is never without a backup target. Offline this retries
            // for as long as it takes, so it runs on its own and does not hold up the caller.
            scope.launch { start() }
        }
    }

    override suspend fun handleCallbackUrl(url: String) {
        val callback = parseCallbackUrl(url, SupabaseConfig.REDIRECT_SCHEME, SupabaseConfig.REDIRECT_HOST) ?: return
        when {
            callback.errorCode == CallbackUrl.IDENTITY_ALREADY_EXISTS ->
                // The Google account is somebody else's already: sign in to it instead of linking.
                guardedSignIn { auth.signInWith(Google) }
            callback.isError -> _events.tryEmit(AccountEvent.SignInFailed)
            callback.code != null -> guardedSignIn {
                auth.exchangeCodeForSession(callback.code)
                _events.tryEmit(AccountEvent.SignedIn)
            }
        }
    }

    private suspend inline fun guardedSignIn(block: () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _events.tryEmit(AccountEvent.SignInFailed)
        }
    }
}

private fun toAccountState(status: SessionStatus): AccountState = when (status) {
    is SessionStatus.Initializing -> AccountState.Loading
    is SessionStatus.NotAuthenticated -> AccountState.NoSession
    // The library keeps the stored session and retries the refresh, so the account is still the same one.
    is SessionStatus.RefreshFailure -> AccountState.Loading
    is SessionStatus.Authenticated -> {
        val user = status.session.user
        AccountState.SignedIn(
            userId = status.session.user?.id ?: "",
            kind = accountKind(user?.identities?.map { it.provider }.orEmpty()),
            email = user?.email,
        )
    }
}

/** Anonymous accounts have no real identity; a linked account lists its providers. */
fun accountKind(providers: List<String>): AccountState.SignedIn.Kind = when {
    "google" in providers -> AccountState.SignedIn.Kind.Google
    "apple" in providers -> AccountState.SignedIn.Kind.Apple
    else -> AccountState.SignedIn.Kind.Anonymous
}

private fun isAnonymous(user: UserInfo?): Boolean =
    user != null && accountKind(user.identities?.map { it.provider }.orEmpty()) == AccountState.SignedIn.Kind.Anonymous
