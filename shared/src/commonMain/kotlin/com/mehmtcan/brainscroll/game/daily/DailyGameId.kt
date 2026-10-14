package com.mehmtcan.brainscroll.game.daily

import com.mehmtcan.brainscroll.game.GameKind

/** Ids of the daily puzzles of the games other than the word puzzle start with this; the feed leaves them alone. */
const val DAILY_GAME_ID_PREFIX = "daily-"

/** The id of the daily puzzle of [game] on [dayIndex]: also the id of its saved progress and of its result. */
fun dailyGameId(dayIndex: Long, game: GameKind): String = "$DAILY_GAME_ID_PREFIX$dayIndex-${game.id}"
