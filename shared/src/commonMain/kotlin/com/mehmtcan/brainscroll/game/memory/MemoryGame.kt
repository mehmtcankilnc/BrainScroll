package com.mehmtcan.brainscroll.game.memory

import com.mehmtcan.brainscroll.game.Game
import kotlin.random.Random

/**
 * The eight shapes a card can show. Each also has its own color in the UI, but the shape alone tells them apart
 * (docs/design.md: content is told apart by shape, not by color).
 */
enum class CardSymbol { Circle, Triangle, Star, Heart, Diamond, Square, Plus, Moon }

enum class MemoryStatus { Playing, Won, Lost }

/** What one tap on a card did. */
enum class FlipResult {
    /** Nothing happened: the game is over, or the card is already face up or matched. */
    Ignored,

    /** The first card of a pair is face up now. */
    Opened,

    /** The second card was the same: the pair stays. */
    Match,

    /** The second card was different: a mistake. The two stay face up until the next tap hides them. */
    Mismatch,
}

/** The picture of a board for the UI: which cards are face up or matched, and the counters. */
data class MemoryState(
    /** The symbol of each card, by position. Two of each, so `size == 2 * pairs`. */
    val layout: List<CardSymbol>,
    /** Cards that are face up and not matched yet: none, one, or two (a mismatch waiting to be hidden). */
    val faceUp: Set<Int>,
    val matched: Set<Int>,
    /** Pairs turned over so far (every second card flipped is one move). */
    val moves: Int,
    val mistakes: Int,
    val maxMistakes: Int,
    val status: MemoryStatus,
    /** True while the two face-up cards are a mismatch that has not been hidden yet. */
    val mismatchShown: Boolean,
)

/** A player's action: turn over the card at this position. */
data class Flip(val card: Int)

/**
 * The rules of memory match, free of UI and time.
 *
 * Turn over two cards per move. Equal cards stay. Different cards are a mistake and stay face up until the next
 * tap (or until [closeMismatch] when the UI's short pause is over), so the player can look at them. The sixth mistake
 * loses the game; finding every pair wins it (docs/decisions.md).
 */
class MemoryGame(
    private val layout: List<CardSymbol>,
    private val maxMistakes: Int = DEFAULT_MAX_MISTAKES,
) : Game<MemoryState, Flip> {

    private val faceUp = mutableListOf<Int>()
    private val matched = mutableSetOf<Int>()
    private var moves = 0
    private var mistakes = 0
    private var status = MemoryStatus.Playing
    private var mismatchShown = false

    init {
        require(layout.size % 2 == 0 && layout.isNotEmpty()) { "a board needs pairs" }
        require(layout.groupingBy { it }.eachCount().values.all { it == 2 }) { "every symbol must appear exactly twice" }
    }

    override val state: MemoryState
        get() = MemoryState(
            layout = layout,
            faceUp = faceUp.toSet(),
            matched = matched.toSet(),
            moves = moves,
            mistakes = mistakes,
            maxMistakes = maxMistakes,
            status = status,
            mismatchShown = mismatchShown,
        )

    override val isFinished: Boolean get() = status != MemoryStatus.Playing

    override fun perform(action: Flip): MemoryState {
        flip(action.card)
        return state
    }

    /** Turns over the card at [card]. */
    fun flip(card: Int): FlipResult {
        if (isFinished || card !in layout.indices || card in matched) return FlipResult.Ignored
        // The next tap hides the mismatched pair and starts a new move. This comes before the "already face up"
        // check: tapping one of the two wrong cards again is a fresh tap on a face-down card.
        if (mismatchShown) closeMismatch()
        if (card in faceUp) return FlipResult.Ignored

        if (faceUp.isEmpty()) {
            faceUp += card
            return FlipResult.Opened
        }

        val first = faceUp.single()
        moves++
        if (layout[first] == layout[card]) {
            matched += first
            matched += card
            faceUp.clear()
            if (matched.size == layout.size) status = MemoryStatus.Won
            return FlipResult.Match
        }

        faceUp += card
        mistakes++
        mismatchShown = true
        if (mistakes >= maxMistakes) status = MemoryStatus.Lost
        return FlipResult.Mismatch
    }

    /** Hides the two cards of a mismatch. Does nothing when there is none. */
    fun closeMismatch() {
        if (!mismatchShown) return
        faceUp.clear()
        mismatchShown = false
    }

    companion object {
        const val PAIRS = 6
        const val DEFAULT_MAX_MISTAKES = 6

        /** A shuffled board of [pairs] different symbols, two cards each. */
        fun newLayout(random: Random, pairs: Int = PAIRS): List<CardSymbol> {
            require(pairs in 1..CardSymbol.entries.size) { "at most ${CardSymbol.entries.size} different symbols" }
            val symbols = CardSymbol.entries.shuffled(random).take(pairs)
            return (symbols + symbols).shuffled(random)
        }
    }
}
