package com.mehmtcan.brainscroll.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The gray of small supporting text (captions, game names, a countdown) where [BrainScrollColors.textSecondary] is too faint to read
 * comfortably: a third of the way from the secondary text color to the main one. It follows the theme (and the high-contrast look).
 */
val BrainScrollColors.textSoft: Color get() = lerp(textSecondary, textPrimary, 0.35f)
