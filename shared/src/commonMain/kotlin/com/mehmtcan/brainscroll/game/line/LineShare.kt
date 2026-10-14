package com.mehmtcan.brainscroll.game.line

/** The line a player pastes into a chat after solving the daily board: no board, no hint of the answer. */
object LineShare {
    /** `Tek Çizgi #42 ⭐⭐⭐ 00:48 🔥7`; the streak is left out when there is none, and a board with no star shows a dash. */
    fun text(name: String, number: Long, stars: Int, duration: String, streak: Int): String = buildString {
        append(name).append(" #").append(number).append(' ')
        append(if (stars > 0) "\u2B50".repeat(stars.coerceAtMost(LineScoring.MAX_STARS)) else "-")
        append(' ').append(duration)
        if (streak > 0) append(" \uD83D\uDD25").append(streak)
    }
}
