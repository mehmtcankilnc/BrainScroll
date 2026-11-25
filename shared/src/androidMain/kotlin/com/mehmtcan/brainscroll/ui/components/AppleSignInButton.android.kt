package com.mehmtcan.brainscroll.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Sign in with Apple is offered on iOS only (canSignInWithApple is false here), so nothing is drawn. */
@Composable
actual fun AppleSignInButton(text: String, onClick: () -> Unit, modifier: Modifier) = Unit
