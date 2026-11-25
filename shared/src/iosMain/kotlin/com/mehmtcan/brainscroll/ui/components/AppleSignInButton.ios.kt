@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.AuthenticationServices.ASAuthorizationAppleIDButton
import platform.AuthenticationServices.ASAuthorizationAppleIDButtonStyle
import platform.AuthenticationServices.ASAuthorizationAppleIDButtonTypeContinue
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventTouchUpInside
import platform.darwin.NSObject

/**
 * Apple's own button (Apple Human Interface Guidelines: the system-provided button keeps the approved look, the logo and the title in
 * the phone's language). White on our dark screens, with the corner radius of our other buttons.
 */
@Composable
actual fun AppleSignInButton(text: String, onClick: () -> Unit, modifier: Modifier) {
    val latest by rememberUpdatedState(onClick)
    // The button holds its target weakly, so the target must be kept here.
    val target = remember { AppleButtonTarget { latest() } }
    UIKitView(
        factory = {
            ASAuthorizationAppleIDButton.buttonWithType(ASAuthorizationAppleIDButtonTypeContinue, ASAuthorizationAppleIDButtonStyle.ASAuthorizationAppleIDButtonStyleWhite).also { button ->
                button.cornerRadius = 12.0
                button.addTarget(target, NSSelectorFromString("tapped"), UIControlEventTouchUpInside)
            }
        },
        modifier = modifier.fillMaxWidth().height(52.dp),
    )
}

private class AppleButtonTarget(private val onTap: () -> Unit) : NSObject() {
    @ObjCAction
    fun tapped() = onTap()
}
