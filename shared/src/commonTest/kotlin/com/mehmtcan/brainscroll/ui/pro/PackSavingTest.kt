package com.mehmtcan.brainscroll.ui.pro

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PackSavingTest {
    private fun small(text: String, micros: Long) = HintPack("small", 20, text, micros)
    private fun big(text: String, micros: Long) = HintPack("big", 75, text, micros)

    @Test
    fun turkishPricesAreWrittenTheTurkishWay() {
        val saving = packSaving(small("39,99₺", 39_990_000), big("99,99₺", 99_990_000))
        assertEquals(PackSaving("149,96₺", 33), saving)
    }

    @Test
    fun dollarPricesKeepTheSignAndThePoint() {
        val saving = packSaving(small("$0.99", 990_000), big("$2.99", 2_990_000))
        assertEquals(PackSaving("$3.71", 19), saving)
    }

    @Test
    fun theSignMayStandAfterTheNumberWithASpace() {
        val saving = packSaving(small("0,99 €", 990_000), big("2,99 €", 2_990_000))
        assertEquals("3,71 €", saving?.fullPriceText)
    }

    @Test
    fun aCurrencyWithoutDecimalsIsRoundedAndThousandsAreGrouped() {
        assertEquals("¥563", formatLike("¥150", 562_500_000))
        assertEquals("¥1,125", formatLike("¥300", 1_125_000_000))
        assertEquals("1.234,50 €", formatLike("0,99 €", 1_234_500_000))
        assertEquals("$1,234.50", formatLike("$0.99", 1_234_500_000))
    }

    @Test
    fun nothingIsSaidWithoutRealPricesOrARealSaving() {
        assertNull(packSaving(small("preview", 0), big("preview", 0)), "no prices from the store")
        assertNull(packSaving(small("$0.99", 990_000), big("$3.71", 3_710_000)), "the big pack is not cheaper per hint")
        assertNull(packSaving(small("$0.99", 990_000), big("$3.60", 3_600_000)), "under five percent is not worth saying")
        assertNull(packSaving(small("no number", 990_000), big("$2.99", 2_990_000)), "a price that cannot be read")
    }
}
