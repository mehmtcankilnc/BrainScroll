package com.mehmtcan.brainscroll.ui.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

/** The Taptic Engine: impacts for keys and answers, notifications for warnings and successes. */
@Composable
actual fun rememberPlatformHaptics(): Haptics = remember { IosHaptics() }

@OptIn(ExperimentalForeignApi::class)
private class IosHaptics : Haptics {
    private val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val medium = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val notification = UINotificationFeedbackGenerator()

    override fun perform(kind: HapticKind) {
        when (kind) {
            HapticKind.Light -> light.impactOccurred().also { light.prepare() }
            HapticKind.Medium -> medium.impactOccurred().also { medium.prepare() }
            HapticKind.Warning -> notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
            HapticKind.Success -> notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
        }
    }

    init {
        light.prepare()
        notification.prepare()
    }
}
