package com.mehmtcan.brainscroll.account

/**
 * What Google's native sign-in hands back. [rawNonce] is the random value we made up; Google received only its SHA-256 hash and put
 * that in the token, and Supabase checks the token against the raw value.
 */
class GoogleCredential(val idToken: String, val rawNonce: String)

/**
 * The account chooser of the phone itself (Android's Credential Manager): one small sheet over the app, no browser, so the player
 * never sees the address of the server. Only where it is set up; elsewhere sign-in goes through the browser.
 */
interface GoogleSignIn {
    /** Shows the sheet. Returns null if the player closed it (not an error). Throws if Google reports a real failure. */
    suspend fun requestCredential(): GoogleCredential?
}

/** The native Google sign-in of this platform, or null where it is not available or not set up yet. */
expect fun platformGoogleSignIn(): GoogleSignIn?
