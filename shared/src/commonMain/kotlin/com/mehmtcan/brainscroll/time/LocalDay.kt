package com.mehmtcan.brainscroll.time

/** Offset of the device's time zone from UTC at [epochMillis], in milliseconds. */
expect fun localUtcOffsetMillis(epochMillis: Long): Long

/**
 * The day on the player's own clock, for things that reset at the player's midnight (the free guide steps). The daily puzzle
 * does not use this: its day is [IstanbulDay].
 */
fun localDayIndex(epochMillis: Long): Long = (epochMillis + localUtcOffsetMillis(epochMillis)).floorDiv(IstanbulDay.DAY_MILLIS)
