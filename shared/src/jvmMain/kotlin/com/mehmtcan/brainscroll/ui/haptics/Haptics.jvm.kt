package com.mehmtcan.brainscroll.ui.haptics

import androidx.compose.runtime.Composable

/** A computer has nothing to buzz. */
@Composable
actual fun rememberPlatformHaptics(): Haptics = NoHaptics
