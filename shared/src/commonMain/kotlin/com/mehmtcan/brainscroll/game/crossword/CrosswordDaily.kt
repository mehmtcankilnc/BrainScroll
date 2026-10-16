package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language

/** Stars of a solved daily crossword: the fewer the mistakes, the more stars. The time decides between players with the same stars. */
object CrosswordScoring {
    const val MAX_STARS = 3

    /** Up to this many mistakes still earn three stars. */
    const val THREE_STARS_UP_TO = 3

    /** Up to this many mistakes earn two stars; more than that, one. */
    const val TWO_STARS_UP_TO = 10

    fun stars(mistakes: Int): Int = when {
        mistakes <= THREE_STARS_UP_TO -> 3
        mistakes <= TWO_STARS_UP_TO -> 2
        else -> 1
    }

    /**
     * The one number a daily result keeps as its guess and the server gets as its moves: 1 + the mistakes (up to 998). It is
     * never 0 (the server wants at least one move), and the server works the stars out of it the same way (`crossword_stars` in
     * supabase/migrations/20261016090000_crossword_daily.sql).
     */
    fun encode(mistakes: Int): Int = 1 + mistakes.coerceIn(0, 998)

    fun mistakesOf(code: Int): Int = (code - 1).coerceAtLeast(0)

    fun starsOf(code: Int): Int = stars(mistakesOf(code))
}

/** The stars of a finished daily crossword result: its only guess is the score code (see [CrosswordScoring.encode]). */
val FinishedRound.crosswordStars: Int
    get() = CrosswordScoring.starsOf(guesses.firstOrNull()?.toIntOrNull() ?: 1)

/**
 * Which board is the daily one: it comes from the day number (see [com.mehmtcan.brainscroll.time.IstanbulDay]), so everyone with the
 * same language gets the same board without a server, and the boards of the list come in a scattered order without repeating until
 * all of them have been played.
 */
object CrosswordSchedule {
    /**
     * The day that is "#1" in what a player shares. A placeholder until the release day is known: set it to the day number
     * (see [com.mehmtcan.brainscroll.time.IstanbulDay.dayIndex]) of the first day the app offers the daily crossword.
     */
    const val LAUNCH_DAY_INDEX = 20_740L

    /** The number of the board of [dayIndex] in what a player shares; never below 1. */
    fun puzzleNumber(dayIndex: Long): Long = (dayIndex - LAUNCH_DAY_INDEX + 1).coerceAtLeast(1)

    /** The place in the list of [count] boards that [dayIndex] plays: a step through the list that does not skip a board. */
    fun boardNumber(dayIndex: Long, count: Int): Int {
        require(count > 0)
        var step = maxOf(1, count / 3)
        while (gcd(step, count) != 1) step++
        return (((dayIndex % count) + count) % count * step % count).toInt()
    }

    fun puzzleFor(dayIndex: Long, language: Language): CrosswordPuzzle =
        CrosswordPuzzle.ofBoard(language, boardNumber(dayIndex, CrosswordBoards.of(language).size))

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}

/** The line a player pastes into a chat after solving the daily board: no board, no hint of the answer. */
object CrosswordShare {
    /** `Çapraz Bulmaca #42 ⭐⭐⭐ 12:48 🔥7`; the streak is left out when there is none. */
    fun text(name: String, number: Long, stars: Int, duration: String, streak: Int): String = buildString {
        append(name).append(" #").append(number).append(' ')
        append(if (stars > 0) "⭐".repeat(stars.coerceAtMost(CrosswordScoring.MAX_STARS)) else "-")
        append(' ').append(duration)
        if (streak > 0) append(" 🔥").append(streak)
    }
}
