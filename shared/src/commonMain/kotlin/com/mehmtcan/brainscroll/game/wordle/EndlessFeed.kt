package com.mehmtcan.brainscroll.game.wordle

import kotlin.random.Random

/** Everything the feed screen draws. */
data class FeedSnapshot(
    val rounds: List<RoundSnapshot>,
    val language: Language,
    /** Consecutive solved puzzles. A loss resets it, skipping does not. */
    val answerStreak: Int,
    val skipsLeft: Int,
    /** False once the feed has been started: the language is fixed for the rest of the session. */
    val canChangeLanguage: Boolean,
)

/**
 * The endless feed of word puzzles, generated locally (docs/decisions.md).
 *
 * Rules:
 * - Answer streak: +1 for a win, back to 0 for a loss. Skipping does not break it.
 * - One skip per feed ("akış" = session: a cold start, or the first action after [SESSION_IDLE_MILLIS] of
 *   inactivity, gives a fresh skip, docs/decisions.md). Leaving an unfinished puzzle forward uses it.
 *   A puzzle that was skipped can be resumed later; finishing it counts like any other.
 * - Answers do not repeat until the whole pool of that language has been used.
 * - The language can only be chosen before the first letter is typed or the first skip.
 */
class EndlessFeed(
    private val wordLists: Map<Language, WordList>,
    startLanguage: Language,
    private val random: Random = Random.Default,
    private val maxSkips: Int = 1,
    private val store: FeedStore = NoFeedStore,
    /** Unfinished puzzles from an earlier run. They become the first pages, in their saved order. */
    restored: List<StoredRound> = emptyList(),
    /** The answer streak at start-up, derived from the saved history. */
    startStreak: Int = 0,
    private val now: () -> Long = { 0L },
    private val newId: () -> String = { Random.nextLong().toULong().toString(16) },
) {
    private val rounds = mutableListOf<WordleRound>()
    private val counted = mutableSetOf<Int>()
    private val bags = mutableMapOf<Language, ArrayDeque<String>>()

    var language: Language = startLanguage
        private set
    var answerStreak = startStreak
        private set
    var skipsLeft = maxSkips
        private set

    /** The last time the player did something in the feed. A long pause makes the next action start a new session. */
    private var lastActivityAt = now()

    val size: Int get() = rounds.size

    init {
        require(wordLists.containsKey(startLanguage)) { "no word list for $startLanguage" }
        restored.forEachIndexed { i, stored ->
            val list = wordLists[stored.language] ?: return@forEachIndexed
            rounds += WordleRound.restore(i, stored, list)
        }
        // Rounds restored from storage were re-numbered from 0, so save them again with their new position.
        rounds.forEach { persist(it) }
    }

    /** Makes sure rounds `0 until count` exist. */
    fun ensureSize(count: Int) {
        while (rounds.size < count) rounds += newRound(rounds.size)
    }

    /**
     * Starts a new session if the player has been away for [SESSION_IDLE_MILLIS] or more: the skip is available again.
     * Returns true if it did, so the caller can redraw. The answer streak and the puzzles themselves are not touched.
     */
    fun startNewSessionIfIdle(): Boolean {
        val time = now()
        if (time - lastActivityAt < SESSION_IDLE_MILLIS) return false
        lastActivityAt = time
        val changed = skipsLeft != maxSkips
        skipsLeft = maxSkips
        return changed
    }

    private fun touch() {
        startNewSessionIfIdle()
        lastActivityAt = now()
    }

    fun type(index: Int, letter: Char) {
        touch()
        rounds[index].type(letter)
        persist(rounds[index])
    }

    fun backspace(index: Int) {
        touch()
        rounds[index].backspace()
        persist(rounds[index])
    }

    fun submit(index: Int) {
        touch()
        val round = rounds[index]
        round.submit()
        if (round.isFinished && counted.add(index)) {
            val result = round.toFinished(finishedAt = now())
            answerStreak = if (result.outcome == Outcome.WON) answerStreak + 1 else 0
            store.finish(result)
        } else {
            persist(round)
        }
    }

    /** Whether the player may leave round [index] forward right now. Does not use the skip. */
    fun canLeave(index: Int): Boolean {
        val round = rounds[index]
        return round.isFinished || round.skipped || skipsLeft > 0
    }

    /** Leaves round [index] forward, using the skip if it is needed. Returns false if that is not allowed. */
    fun leave(index: Int): Boolean {
        touch()
        val round = rounds[index]
        if (round.isFinished || round.skipped) return true
        if (skipsLeft == 0) return false
        skipsLeft--
        round.skipped = true
        persist(round)
        return true
    }

    /** True once the player typed a letter, made a guess or skipped. From then on the language is locked. */
    val hasStarted: Boolean get() = rounds.any { !it.isUntouched || it.skipped }

    /**
     * Switches the language of the whole feed. Only possible before the feed has been started;
     * returns false (and changes nothing) afterwards.
     */
    fun setLanguage(newLanguage: Language): Boolean {
        require(wordLists.containsKey(newLanguage)) { "no word list for $newLanguage" }
        if (hasStarted) return false
        language = newLanguage
        for (i in rounds.indices) rounds[i] = newRound(i)
        return true
    }

    /**
     * Sets the answer streak from outside. Used after a cloud sync brought in results from another device:
     * the streak is always derived from the saved history, so it is recomputed there and applied here.
     */
    fun setAnswerStreak(streak: Int) {
        answerStreak = streak
    }

    fun snapshot() = FeedSnapshot(
        rounds = rounds.map { it.snapshot() },
        language = language,
        answerStreak = answerStreak,
        skipsLeft = skipsLeft,
        canChangeLanguage = !hasStarted,
    )

    private fun newRound(index: Int): WordleRound {
        val list = wordLists.getValue(language)
        return WordleRound(index, WordleGame(list, nextAnswer(list)), list, newId())
    }

    /** Keeps the store in step with a round: untouched rounds are not worth saving, started ones are. */
    private fun persist(round: WordleRound) {
        when {
            round.isFinished -> Unit // `finish` already moved it to the history
            round.isUntouched && !round.skipped -> store.deleteProgress(round.id)
            else -> store.saveProgress(round.toStored())
        }
    }

    /** Draws from a shuffled bag so answers only repeat after the whole pool was used. */
    private fun nextAnswer(list: WordList): String {
        val bag = bags.getOrPut(list.language) { ArrayDeque() }
        if (bag.isEmpty()) bag.addAll(list.answers.shuffled(random))
        return bag.removeFirst()
    }
}

/** A session ends after 30 minutes without any action in the feed (docs/decisions.md). */
const val SESSION_IDLE_MILLIS = 30L * 60 * 1000
