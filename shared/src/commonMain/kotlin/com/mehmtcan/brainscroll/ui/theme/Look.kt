package com.mehmtcan.brainscroll.ui.theme

/** The look the player picked (accent color, pure black). Kept whether or not Pro is on: it only applies while it is. */
data class Look(val accent: Accent = Accent.Green, val amoled: Boolean = false) {
    /** What is drawn: the pick for Pro, the default look otherwise. The pick is not lost, it comes back with Pro. */
    fun forPro(pro: Boolean): Look = if (pro) this else Look()
}
