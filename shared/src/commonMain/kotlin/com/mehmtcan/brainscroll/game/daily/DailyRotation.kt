package com.mehmtcan.brainscroll.game.daily

import com.mehmtcan.brainscroll.game.GameKind
import kotlin.random.Random

/**
 * Which game is the daily puzzle on a given day. Worked out from the day number alone, so every phone agrees without
 * asking the server and it works offline.
 *
 * The days are cut into rounds of one day per game. In each round the games are in a shuffled order that depends only
 * on the number of the round, so no game comes twice before every game has come once. The shuffles are fixed, not random
 * per phone. To keep the same game from ending one round and starting the next, a round that would begin with the game
 * the last round ended with swaps its first two games.
 */
object DailyRotation {
    /** The games that can be the daily puzzle: all of them. */
    val games: List<GameKind> = GameKind.entries

    fun gameFor(dayIndex: Long, candidates: List<GameKind> = games): GameKind {
        val size = candidates.size
        val round = dayIndex.floorDiv(size.toLong())
        return order(round, candidates)[dayIndex.mod(size.toLong()).toInt()]
    }

    /** The games of round [round] in the order they come. */
    internal fun order(round: Long, candidates: List<GameKind> = games): List<GameKind> {
        val mixed = shuffled(round, candidates).toMutableList()
        // The last game of the round before is its raw shuffle's last game: swapping the first two never changes it.
        val previousLast = shuffled(round - 1, candidates).last()
        if (candidates.size > 2 && mixed.first() == previousLast) {
            val first = mixed[0]
            mixed[0] = mixed[1]
            mixed[1] = first
        }
        return mixed
    }

    private fun shuffled(round: Long, candidates: List<GameKind>): List<GameKind> =
        candidates.shuffled(Random(round * 7_919L + 13L))
}

/** Ids of the daily puzzles of the games other than the word puzzle start with this; the feed leaves them alone. */
const val DAILY_GAME_ID_PREFIX = "daily-"

/** The id of the daily puzzle of [game] on [dayIndex]: also the id of its saved progress and of its result. */
fun dailyGameId(dayIndex: Long, game: GameKind): String = "$DAILY_GAME_ID_PREFIX$dayIndex-${game.id}"
