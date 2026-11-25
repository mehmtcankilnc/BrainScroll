package com.mehmtcan.brainscroll.account

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mehmtcan.brainscroll.cloud.SupabaseConfig
import java.security.MessageDigest
import java.util.UUID

/** Native Google sign-in once the web client id is set in [SupabaseConfig]; until then the browser is used. */
actual fun platformGoogleSignIn(): GoogleSignIn? =
    SupabaseConfig.GOOGLE_WEB_CLIENT_ID.takeIf { it.isNotBlank() }?.let(::AndroidGoogleSignIn)

private class AndroidGoogleSignIn(private val webClientId: String) : GoogleSignIn {

    override suspend fun requestCredential(): GoogleCredential? {
        val activity = CurrentActivity.get() ?: throw IllegalStateException("google error: no screen")
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray()).joinToString("") { "%02x".format(it) }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).setNonce(hashedNonce).build())
            .build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            return null // the player closed the sheet
        } catch (e: GetCredentialException) {
            throw IllegalStateException("google error: ${e.type.substringAfterLast('.')}")
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("google error: unexpected answer")
        }
        return GoogleCredential(GoogleIdTokenCredential.createFrom(credential.data).idToken, rawNonce)
    }
}
