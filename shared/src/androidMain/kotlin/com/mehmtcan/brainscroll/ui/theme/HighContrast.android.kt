package com.mehmtcan.brainscroll.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Android: Settings > Accessibility > High contrast text. The setting is stored under a key that is not part of
 * the public SDK, so a phone that does not know it simply reports "off". Read again when the app returns to the front.
 */
@Composable
actual fun rememberHighContrast(): Boolean {
    val context = LocalContext.current
    var on by remember { mutableStateOf(highContrastOn(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) on = highContrastOn(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return on
}

private fun highContrastOn(context: Context): Boolean =
    Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
