package com.mehmtcan.brainscroll.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LookTest {
    private val picked = Look(Accent.Blue, amoled = true)

    @Test
    fun cosmeticsAreOnHoldSoEveryoneGetsTheDefaultLook() {
        assertEquals(false, COSMETICS_ENABLED)
        assertEquals(Look(), picked.forPro(true))
        assertEquals(Look(), picked.forPro(false))
    }

    @Test
    fun withoutProTheDefaultLookIsDrawn() {
        assertEquals(Look(), picked.forPro(false, enabled = true))
    }

    @Test
    fun withProThePickIsDrawn() {
        assertEquals(picked, picked.forPro(true, enabled = true))
    }

    @Test
    fun thePickIsNotLostWhenProEndsAndComesBack() {
        // forPro does not change the pick: it only chooses what to draw.
        assertEquals(Accent.Blue, picked.accent)
        assertEquals(picked, picked.forPro(false, enabled = true).let { picked }.forPro(true, enabled = true))
    }

    @Test
    fun anAccentOnlyChangesTheControlsNotTheMeaningColors() {
        val blue = DarkColors.withAccent(Accent.Blue)
        assertNotEquals(DarkColors.accent, blue.accent)
        assertEquals(DarkColors.correctFill, blue.correctFill)
        assertEquals(DarkColors.error, blue.error)
        assertEquals(DarkColors.pendingFill, blue.pendingFill)
    }

    @Test
    fun pureBlackOnlyChangesThePage() {
        val black = DarkColors.amoled()
        assertEquals(Color.Black, black.bgPage)
        assertEquals(DarkColors.copy(bgPage = Color.Black), black)
    }

    @Test
    fun anUnknownSavedAccentReadsAsGreen() {
        assertEquals(Accent.Green, Accent.fromId(null))
        assertEquals(Accent.Green, Accent.fromId("purple"))
        assertEquals(Accent.Orange, Accent.fromId("orange"))
    }
}
