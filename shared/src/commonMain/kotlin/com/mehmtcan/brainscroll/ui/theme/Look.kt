package com.mehmtcan.brainscroll.ui.theme

/**
 * Cosmetics are on hold (docs/pro-plan.md section 8.5): free and Pro players get the same default look and the
 * picker is hidden. Turn this on to bring the accent colors and pure black back.
 */
const val COSMETICS_ENABLED = false

/** The look the player picked (accent color, pure black). Kept whether or not Pro is on: it only applies while it is. */
data class Look(val accent: Accent = Accent.Green, val amoled: Boolean = false) {
    /** What is drawn: the pick for Pro, the default look otherwise. The pick is not lost, it comes back with Pro. */
    fun forPro(pro: Boolean, enabled: Boolean = COSMETICS_ENABLED): Look = if (enabled && pro) this else Look()
}
