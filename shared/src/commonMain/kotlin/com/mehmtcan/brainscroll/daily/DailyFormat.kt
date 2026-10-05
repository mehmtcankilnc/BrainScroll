package com.mehmtcan.brainscroll.daily

/** `83_000` -> `01:23`, `3_725_000` -> `1:02:05`. Whole seconds, rounded down. */
fun formatDuration(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$mm:$ss" else "$mm:$ss"
}

/** The countdown to the next puzzle always shows hours, like a clock: `05:07:09`. */
fun formatCountdown(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1000
    val hh = (totalSeconds / 3600).toString().padStart(2, '0')
    val mm = ((totalSeconds % 3600) / 60).toString().padStart(2, '0')
    val ss = (totalSeconds % 60).toString().padStart(2, '0')
    return "$hh:$mm:$ss"
}
