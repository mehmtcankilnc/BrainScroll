package com.mehmtcan.brainscroll.ui.pro

import androidx.compose.runtime.staticCompositionLocalOf

/** Opens the purchase screen. [AppShell] provides it; the default does nothing (previews, tests). */
val LocalOpenPaywall = staticCompositionLocalOf<(PaywallSource) -> Unit> { {} }

/** What opened the purchase screen. Only the name goes into the anonymous counts. */
enum class PaywallSource(val wireName: String) {
    Guide("guide"),
    Profile("profile"),
    Streak("streak"),
    Theme("theme"),
    StreakRescue("streak_rescue"),
    StoreIcon("store_icon"),
}
