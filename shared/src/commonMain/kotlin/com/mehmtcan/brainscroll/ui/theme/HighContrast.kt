package com.mehmtcan.brainscroll.ui.theme

import androidx.compose.runtime.Composable

/**
 * True when the player turned on the system's contrast setting (Android: High contrast text, iOS: Increase
 * Contrast). The theme then brightens secondary text and thickens outlines (docs/design.md section 9).
 */
@Composable
expect fun rememberHighContrast(): Boolean
