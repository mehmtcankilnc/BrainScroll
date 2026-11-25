package com.mehmtcan.brainscroll.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AppleSignInButton(text: String, onClick: () -> Unit, modifier: Modifier) = DrawnAppleSignInButton(text, onClick, modifier)
