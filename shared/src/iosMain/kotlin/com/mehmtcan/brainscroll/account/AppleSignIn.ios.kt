@file:OptIn(ExperimentalForeignApi::class)

package com.mehmtcan.brainscroll.account

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASPresentationAnchor
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** iOS shows the native "Sign in with Apple" sheet. */
actual fun platformAppleSignIn(): AppleSignIn? = IosAppleSignIn()

private class IosAppleSignIn : AppleSignIn {

    // The controller only keeps a weak reference to its delegate, so we hold on to it until the sheet is done.
    private var running: AppleSheet? = null

    override suspend fun requestCredential(): AppleCredential? = suspendCancellableCoroutine { continuation ->
        val rawNonce = randomNonce()

        val request = ASAuthorizationAppleIDProvider().createRequest().apply {
            requestedScopes = listOf(ASAuthorizationScopeEmail)
            // Apple puts the HASH of the nonce in the token. Supabase gets the raw value and hashes it to compare.
            nonce = sha256Hex(rawNonce)
        }
        val controller = ASAuthorizationController(authorizationRequests = listOf(request))

        val sheet = AppleSheet(rawNonce) { result ->
            running = null
            result.fold(
                onSuccess = { continuation.resume(it) },
                onFailure = { continuation.resumeWithException(it) },
            )
        }
        running = sheet
        controller.delegate = sheet
        controller.presentationContextProvider = sheet
        controller.performRequests()

        continuation.invokeOnCancellation { running = null }
    }
}

private class AppleSheet(
    private val rawNonce: String,
    private val finish: (Result<AppleCredential?>) -> Unit,
) : NSObject(), ASAuthorizationControllerDelegateProtocol, ASAuthorizationControllerPresentationContextProvidingProtocol {

    override fun authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization: ASAuthorization) {
        val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        val token = credential?.identityToken?.let(::utf8String)
        finish(
            if (token != null) Result.success(AppleCredential(token, rawNonce))
            else Result.failure(IllegalStateException("apple: no identity token")),
        )
    }

    override fun authorizationController(controller: ASAuthorizationController, didCompleteWithError: NSError) {
        // Closing the sheet is not a failure; anything else is.
        if (didCompleteWithError.code == ASAuthorizationErrorCanceled) finish(Result.success(null))
        else finish(Result.failure(IllegalStateException("apple error ${didCompleteWithError.code}")))
    }

    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
        val scenes = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>()
        val window = scenes.flatMap { it.windows.filterIsInstance<UIWindow>() }.firstOrNull { it.isKeyWindow() }
        return window ?: UIWindow()
    }
}

private fun utf8String(data: NSData): String? = NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?

/** 32 random bytes from the system's secure generator, as 64 hex characters. */
private fun randomNonce(): String {
    val bytes = ByteArray(32)
    bytes.usePinned { pinned -> SecRandomCopyBytes(kSecRandomDefault, bytes.size.convert(), pinned.addressOf(0)) }
    return bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}

private fun sha256Hex(text: String): String {
    val input = text.encodeToByteArray()
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    input.usePinned { inPinned ->
        digest.usePinned { outPinned ->
            CC_SHA256(inPinned.addressOf(0), input.size.convert(), outPinned.addressOf(0))
        }
    }
    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}
