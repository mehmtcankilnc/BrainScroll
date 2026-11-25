package com.mehmtcan.brainscroll.account

/**
 * What Apple's native sign-in sheet hands back. [rawNonce] is the random value we made up; Apple received only
 * its SHA-256 hash and put that in the token, and Supabase checks the token against the raw value.
 * [authorizationCode] is the one-time code Apple also hands over: the server turns it into a token it can later revoke when the
 * account is deleted (App Store guideline 5.1.1(v)). Null where Apple gave none.
 */
class AppleCredential(val idToken: String, val rawNonce: String, val authorizationCode: String? = null)

/** The native "Sign in with Apple" sheet. Exists on iOS only (docs/decisions.md). */
interface AppleSignIn {
    /**
     * Shows the sheet. Returns null if the player closed it without signing in (not an error).
     * Throws if Apple reports a real failure.
     */
    suspend fun requestCredential(): AppleCredential?
}

/** The Apple sign-in of this platform, or null where it is not offered (Android, desktop). */
expect fun platformAppleSignIn(): AppleSignIn?
