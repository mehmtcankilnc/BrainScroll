package com.mehmtcan.brainscroll.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * "Continue with Apple". On iOS this is the button the system provides (Apple-approved look, logo and title in the language of the
 * phone, read out by VoiceOver); elsewhere a drawn one. Put it first, at least as big as the other sign-in buttons.
 * [text] is used where the button is drawn by the app; the system's own title is used on iOS.
 */
@Composable
expect fun AppleSignInButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier)
