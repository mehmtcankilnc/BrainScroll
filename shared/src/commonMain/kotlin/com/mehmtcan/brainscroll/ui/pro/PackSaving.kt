package com.mehmtcan.brainscroll.ui.pro

/**
 * Why the big pack is the better deal, told with the player's own prices: what the big pack would cost at the price per hint of
 * the small one ([fullPriceText], written like the store writes prices), and how much cheaper it really is ([percent]).
 */
data class PackSaving(val fullPriceText: String, val percent: Int)

/** Null when there is nothing honest to say: no prices from the store, no real saving (under 5 percent), or an odd price format. */
fun packSaving(small: HintPack, big: HintPack): PackSaving? {
    if (small.hints <= 0 || big.hints <= small.hints || small.priceMicros <= 0 || big.priceMicros <= 0) return null
    val fullMicros = small.priceMicros * big.hints / small.hints
    if (big.priceMicros >= fullMicros) return null
    val percent = (((fullMicros - big.priceMicros) * 100 + fullMicros / 2) / fullMicros).toInt()
    if (percent < 5) return null
    val text = formatLike(small.priceText, fullMicros) ?: return null
    return PackSaving(text, percent)
}

private val NUMBER = Regex("[0-9][0-9.,   ]*[0-9]|[0-9]")

/**
 * Writes [micros] (millionths of the currency) the way [template] is written: the same currency sign in the same place, the same
 * decimal and thousands separators, the same number of decimals. Null when [template] has no number in it.
 */
internal fun formatLike(template: String, micros: Long): String? {
    val match = NUMBER.find(template) ?: return null
    val number = match.value
    val lastSeparator = number.indexOfLast { it == '.' || it == ',' }
    val decimals = if (lastSeparator >= 0 && number.length - lastSeparator - 1 in 1..2) number.length - lastSeparator - 1 else 0
    val decimalSeparator = if (decimals > 0) number[lastSeparator] else null
    val groupSeparator = number.firstOrNull { !it.isDigit() && it != decimalSeparator }
        ?: if (decimalSeparator == ',') '.' else ','
    var scale = 1L
    repeat(decimals) { scale *= 10 }
    val value = (micros * scale + 500_000) / 1_000_000
    val whole = (value / scale).toString()
    val grouped = buildString {
        whole.forEachIndexed { i, c ->
            if (i > 0 && (whole.length - i) % 3 == 0) append(groupSeparator)
            append(c)
        }
    }
    val text = if (decimals > 0) grouped + decimalSeparator + (value % scale).toString().padStart(decimals, '0') else grouped
    return template.replaceRange(match.range, text)
}
