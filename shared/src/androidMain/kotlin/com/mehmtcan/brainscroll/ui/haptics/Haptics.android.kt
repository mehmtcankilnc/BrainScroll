package com.mehmtcan.brainscroll.ui.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Uses the view's haptic feedback, so the system setting for touch feedback is respected. */
@Composable
actual fun rememberPlatformHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) {
        Haptics { kind ->
            val constant = when (kind) {
                HapticKind.Light -> HapticFeedbackConstants.KEYBOARD_TAP
                HapticKind.Medium -> HapticFeedbackConstants.CONTEXT_CLICK
                // CONFIRM and REJECT exist from Android 11; older versions get the nearest older constants.
                HapticKind.Warning -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
                HapticKind.Success -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
            }
            view.performHapticFeedback(constant)
        }
    }
}
