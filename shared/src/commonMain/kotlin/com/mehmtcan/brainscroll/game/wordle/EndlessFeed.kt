package com.mehmtcan.brainscroll.game.wordle

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.memory.FlipResult
import com.mehmtcan.brainscroll.game.lights.LightsResult
import com.mehmtcan.brainscroll.game.lights.LightsRound
import com.mehmtcan.brainscroll.game.mines.MinesResult
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator
import com.mehmtcan.brainscroll.game.pipes.PipesResult
import com.mehmtcan.brainscroll.game.pipes.PipesRound
import com.mehmtcan.brainscroll.game.mines.MinesRound
import com.mehmtcan.brainscroll.game.memory.MemoryGame
import com.mehmtcan.brainscroll.game.memory.MemoryRound
import com.mehmtcan.brainscroll.game.tubes.PourResult
import com.mehmtcan.brainscroll.game.tubes.TubesGame
import com.mehmtcan.brainscroll.game.slide.SlideResult
import com.mehmtcan.brainscroll.game.slide.SlideRound
import com.mehmtcan.brainscroll.game.sudoku.SudokuResult
import com.mehmtcan.brainscroll.game.target.Op
import com.mehmtcan.brainscroll.game.target.TargetGenerator
import com.mehmtcan.brainscroll.game.target.TargetResult
import com.mehmtcan.brainscroll.game.target.TargetRound
import com.mehmtcan.brainscroll.game.sudoku.SudokuRound
import com.mehmtcan.brainscroll.game.tubes.TubesRound
import kotlin.random.Random

/** Everything the feed screen draws. */
data class FeedSnapshot(
    /** One entry per page of the feed, in order, whatever the game of that page. */
    val pages: List<PageSnapshot>,
    val language: Language,
    /** Consecutive solved puzzles, all games together. A loss resets it, skipping does not. */
    val answerStreak: Int,
    val skipsLeft: Int,
    /** False once the feed has been started: the language is fixed for the rest of the session. */
    val canChangeLanguage: Boolean,
) {
    /**
     * The word puzzles among the pages, in order. This is the same as [pages] in a feed that only has word puzzles,
     * which is what most of the word puzzle tests use.
     */
    val rounds: List<RoundSnapshot> get() = pages.filterIsInstance<RoundSnapshot>()
}

/**
 * The endless feed, generated locally (docs/decisions.md). Its pages are different games, in the order of [games]
 * (word puzzle, memory match, word puzzle, ...).
 *
 * Rules:
 * - Answer streak: +1 for a win, back to 0 for a loss, in any game. Skipping does not break it.
 * - One skip per feed ("akış" = session: a cold start, or the first action after [SESSION_IDLE_MILLIS] of
 *   inactivity, gives a fresh skip, docs/decisions.md). Leaving an unfinished puzzle forward uses it.
 *   A puzzle that was skipped can be resumed later; finishing it counts like any other.
 * - Word answers do not repeat until the whole pool of that language has been used.
 * - The word language can only be chosen before the first letter is typed or the first skip. It does not touch
 *   memory pages.
 */
class EndlessFeed(
    private val wordLists: Map<Language, WordList>,
    startLanguage: Language,
    private val random: Random = Random.Default,
    private val maxSkips: Int = 999, // TEST: normal value is 1
    private val store: FeedStore = NoFeedStore,
    /** Unfinished puzzles from an earlier run. They become the first pages, in their saved order. */
    restored: List<StoredRound> = emptyList(),
    /** The answer streak at start-up, derived from the saved history. */
    startStreak: Int = 0,
    private val now: () -> Long = { 0L },
    private val newId: () -> String = { Random.nextLong().toULong().toString(16) },
    /** The games of the feed and their order: each page is followed by the next game in this list, round and round. */
    games: List<GameKind> = listOf(GameKind.Word),
) {
    /** Every game this feed can show, in order. [games] is the part of it the player switched on. */
    private val allGames: List<GameKind> = games
    private var games: List<GameKind> = games

    private val rounds = mutableListOf<FeedRound>()
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
        require(games.isNotEmpty()) { "a feed needs at least one game" }
        for (stored in restored) {
            val round: FeedRound? = when (stored.game) {
                GameKind.Word -> wordLists[stored.language]?.let { WordleRound.restore(rounds.size, stored, it) }
                GameKind.Memory -> MemoryRound.restore(rounds.size, stored, now)
                GameKind.Tubes -> TubesRound.restore(rounds.size, stored, now)
                GameKind.Mines -> MinesRound.restore(rounds.size, stored, now)
                GameKind.Sudoku -> SudokuRound.restore(rounds.size, stored, now)
                GameKind.Lights -> LightsRound.restore(rounds.size, stored, now)
                GameKind.Slide -> SlideRound.restore(rounds.size, stored, now)
                GameKind.Target -> TargetRound.restore(rounds.size, stored, now)
                GameKind.Pipes -> PipesRound.restore(rounds.size, stored, now)
            }
            if (round != null) rounds += round
        }
        // Rounds restored from storage were re-numbered from 0, so save them again with their new position.
        rounds.forEach { persist(it) }
    }

    /** Makes sure rounds `0 until count` exist. */
    fun ensureSize(count: Int) {
        while (rounds.size < count) rounds += newRound(rounds.size, nextKind())
    }

    /**
     * The game of the next page: the one after the game of the last page in [games], round and round. Going by the
     * last page (not by the page number) keeps the order right after a restart, when only the started puzzles are
     * brought back and they are numbered again from zero.
     */
    private fun nextKind(): GameKind = kindAfter(rounds.lastOrNull()?.kind)

    private fun kindAfter(previous: GameKind?): GameKind {
        if (previous == null) return games.first()
        val at = games.indexOf(previous)
        return if (at < 0) games.first() else games[(at + 1) % games.size]
    }

    /** The games that are switched on, in feed order. */
    val enabledGames: List<GameKind> get() = games

    /**
     * Switches games on or off for the pages that are still to come. Pages from [fromIndex] on that were not touched
     * and belong to a game that is now off are replaced; started, finished and skipped pages stay as they are.
     * At least one game stays on: an empty [enabled] (or one with no known game) changes nothing.
     */
    fun setEnabledGames(enabled: Set<GameKind>, fromIndex: Int) {
        val next = allGames.filter { it in enabled }
        if (next.isEmpty() || next == games) return
        games = next
        for (i in fromIndex.coerceAtLeast(0) until rounds.size) {
            val round = rounds[i]
            if (round.isUntouched && !round.skipped && round.kind !in games) {
                rounds[i] = newRound(i, kindAfter(rounds.getOrNull(i - 1)?.kind))
            }
        }
    }

    /**
     * Changes the language of the word puzzles that come from now on, also after the feed has been started (unlike
     * [setLanguage]). Word pages from [fromIndex] on that were not touched are drawn again in the new language;
     * puzzles that were started keep the language they began in.
     */
    fun setWordLanguage(newLanguage: Language, fromIndex: Int) {
        require(wordLists.containsKey(newLanguage)) { "no word list for $newLanguage" }
        if (newLanguage == language) return
        language = newLanguage
        for (i in fromIndex.coerceAtLeast(0) until rounds.size) {
            val round = rounds[i]
            if (round.kind == GameKind.Word && round.isUntouched && !round.skipped) rounds[i] = newRound(i, GameKind.Word)
        }
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

    // --- Word puzzle actions (ignored on a page of another game) ---

    fun type(index: Int, letter: Char) {
        val round = rounds[index] as? WordleRound ?: return
        touch()
        round.type(letter)
        persist(round)
    }

    fun backspace(index: Int) {
        val round = rounds[index] as? WordleRound ?: return
        touch()
        round.backspace()
        persist(round)
    }

    fun submit(index: Int) {
        val round = rounds[index] as? WordleRound ?: return
        touch()
        round.submit()
        afterMove(index, round)
    }

    // --- Memory match actions (ignored on a page of another game) ---

    /** Turns over card [card] of the memory board on page [index]. */
    fun flip(index: Int, card: Int): FlipResult {
        val round = rounds[index] as? MemoryRound ?: return FlipResult.Ignored
        touch()
        val result = round.flip(card)
        if (result != FlipResult.Ignored) afterMove(index, round)
        return result
    }

    /** Hides the mismatched pair of the memory board on page [index] (the screen calls this after its short pause). */
    fun closeMismatch(index: Int) {
        (rounds[index] as? MemoryRound)?.closeMismatch()
    }

    // --- Color tubes actions (ignored on a page of another game) ---

    /** Pours tube [from] into tube [to] on the tubes board of page [index]. */
    fun pour(index: Int, from: Int, to: Int): PourResult {
        val round = rounds[index] as? TubesRound ?: return PourResult.Ignored
        touch()
        val result = round.pour(from, to)
        if (result != PourResult.Ignored) afterMove(index, round)
        return result
    }

    /** Takes back the last pour on page [index]. Returns false when there was nothing to take back. */
    fun undo(index: Int): Boolean {
        val round = rounds[index] as? TubesRound ?: return false
        touch()
        return round.undo().also { if (it) persist(round) }
    }

    /** Puts the tubes of page [index] back as they were at the start. Returns false when nothing had changed. */
    fun restart(index: Int): Boolean {
        val round = rounds[index] as? TubesRound ?: return false
        touch()
        return round.restart().also { if (it) persist(round) }
    }

    // --- Minesweeper actions (ignored on a page of another game) ---

    /** Opens [cell] on the minesweeper board of page [index]. */
    fun openCell(index: Int, cell: Int): MinesResult {
        val round = rounds[index] as? MinesRound ?: return MinesResult.Ignored
        touch()
        val result = round.open(cell)
        if (result != MinesResult.Ignored) afterMove(index, round)
        return result
    }

    /** Flags or unflags [cell] on page [index]. Returns false when that was not possible. */
    fun flagCell(index: Int, cell: Int): Boolean {
        val round = rounds[index] as? MinesRound ?: return false
        touch()
        return round.flag(cell).also { if (it) persist(round) }
    }

    /** Opens the neighbors of the open number [cell] that are not flagged, when its flags match its number. */
    fun chordCell(index: Int, cell: Int): MinesResult {
        val round = rounds[index] as? MinesRound ?: return MinesResult.Ignored
        touch()
        val result = round.chord(cell)
        if (result != MinesResult.Ignored) afterMove(index, round)
        return result
    }

    // --- Lights Out actions (ignored on a page of another game) ---

    /** Taps [cell] on the Lights Out board of page [index]. */
    fun tapLight(index: Int, cell: Int): LightsResult {
        val round = rounds[index] as? LightsRound ?: return LightsResult.Ignored
        touch()
        val result = round.tap(cell)
        if (result != LightsResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the lights of page [index] back as they were at the start. Returns false when nothing had changed. */
    fun restartLights(index: Int): Boolean {
        val round = rounds[index] as? LightsRound ?: return false
        touch()
        return round.restart().also { if (it) persist(round) }
    }

    // --- Sliding puzzle actions (ignored on a page of another game) ---

    /** Taps the tile in [cell] on the sliding puzzle of page [index]. */
    fun slideTile(index: Int, cell: Int): SlideResult {
        val round = rounds[index] as? SlideRound ?: return SlideResult.Ignored
        touch()
        val result = round.slide(cell)
        if (result != SlideResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the tiles of page [index] back as they were at the start. Returns false when nothing had changed. */
    fun restartSlide(index: Int): Boolean {
        val round = rounds[index] as? SlideRound ?: return false
        touch()
        return round.restart().also { if (it) persist(round) }
    }

    // --- Target number actions (ignored on a page of another game) ---

    /** Combines the numbers [firstId] and [secondId] with [op] on the target number game of page [index]. */
    fun combineNumbers(index: Int, firstId: Int, op: Op, secondId: Int): TargetResult {
        val round = rounds[index] as? TargetRound ?: return TargetResult.Ignored
        touch()
        val result = round.combine(firstId, op, secondId)
        if (result != TargetResult.Ignored) afterMove(index, round)
        return result
    }

    /** What [firstId] [op] [secondId] would give on page [index], or null if it is not a move. Changes nothing. */
    fun previewNumbers(index: Int, firstId: Int, op: Op, secondId: Int): Int? =
        (rounds[index] as? TargetRound)?.preview(firstId, op, secondId)

    /** Takes back the last operation on page [index]. Returns false when there was nothing to take back. */
    fun undoNumbers(index: Int): Boolean {
        val round = rounds[index] as? TargetRound ?: return false
        touch()
        return round.undo().also { if (it) persist(round) }
    }

    /** Puts the numbers of page [index] back as they were at the start. Returns false when nothing had been done. */
    fun restartNumbers(index: Int): Boolean {
        val round = rounds[index] as? TargetRound ?: return false
        touch()
        return round.restart().also { if (it) persist(round) }
    }

    // --- Pipes actions (ignored on a page of another game) ---

    /** Turns the tile in [cell] on the pipes board of page [index]. */
    fun turnPipe(index: Int, cell: Int): PipesResult {
        val round = rounds[index] as? PipesRound ?: return PipesResult.Ignored
        touch()
        val result = round.tap(cell)
        if (result != PipesResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the tiles of page [index] back as they were at the start. Returns false when nothing had changed. */
    fun restartPipes(index: Int): Boolean {
        val round = rounds[index] as? PipesRound ?: return false
        touch()
        return round.restart().also { if (it) persist(round) }
    }

    // --- Sudoku actions (ignored on a page of another game) ---

    /** Puts [digit] in [cell] of the sudoku board of page [index]. A wrong digit is a mistake; the third one loses. */
    fun placeDigit(index: Int, cell: Int, digit: Int): SudokuResult {
        val round = rounds[index] as? SudokuRound ?: return SudokuResult.Ignored
        touch()
        val result = round.place(cell, digit)
        if (result != SudokuResult.Ignored) afterMove(index, round)
        return result
    }

    /** Switches the pencil note [digit] on or off in [cell] of page [index]. */
    fun toggleNote(index: Int, cell: Int, digit: Int): SudokuResult {
        val round = rounds[index] as? SudokuRound ?: return SudokuResult.Ignored
        touch()
        return round.toggleNote(cell, digit).also { if (it != SudokuResult.Ignored) persist(round) }
    }

    /** Takes back a wrong digit or the notes of [cell] on page [index]. */
    fun eraseCell(index: Int, cell: Int): SudokuResult {
        val round = rounds[index] as? SudokuRound ?: return SudokuResult.Ignored
        touch()
        return round.erase(cell).also { if (it != SudokuResult.Ignored) persist(round) }
    }

    /** After a move: a round that just ended becomes a result (once), anything else is saved as progress. */
    private fun afterMove(index: Int, round: FeedRound) {
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
        round.markSkipped()
        persist(round)
        return true
    }

    /** True once the player did something on a page or skipped one. From then on the language is locked. */
    val hasStarted: Boolean get() = rounds.any { !it.isUntouched || it.skipped }

    /**
     * Switches the language of the word puzzles in the feed. Only possible before the feed has been started;
     * returns false (and changes nothing) afterwards. Pages of other games are not touched.
     */
    fun setLanguage(newLanguage: Language): Boolean {
        require(wordLists.containsKey(newLanguage)) { "no word list for $newLanguage" }
        if (hasStarted) return false
        language = newLanguage
        for (i in rounds.indices) {
            if (rounds[i].kind == GameKind.Word) rounds[i] = newRound(i, GameKind.Word)
        }
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
        pages = rounds.map { it.snapshot() },
        language = language,
        answerStreak = answerStreak,
        skipsLeft = skipsLeft,
        canChangeLanguage = !hasStarted,
    )

    private fun newRound(index: Int, kind: GameKind): FeedRound = when (kind) {
        GameKind.Word -> {
            val list = wordLists.getValue(language)
            WordleRound(index, WordleGame(list, nextAnswer(list)), list, newId())
        }
        GameKind.Memory -> MemoryRound(index, newId(), MemoryGame.newLayout(random), language, now)
        GameKind.Tubes -> TubesRound(index, newId(), TubesGame.newLayout(random), language, now)
        GameKind.Mines -> MinesRound(index, newId(), random.nextInt(), language, now)
        GameKind.Sudoku -> SudokuRound(index, newId(), random.nextInt(), language, now)
        GameKind.Lights -> LightsRound(index, newId(), random.nextInt(), language, now)
        GameKind.Slide -> SlideRound(index, newId(), random.nextInt(), language, now)
        GameKind.Target -> TargetGenerator.generate(random.nextInt()).let { TargetRound(index, newId(), it.numbers, it.target, language, now, it.par) }
        GameKind.Pipes -> PipesGenerator.generate(random.nextInt()).let { PipesRound(index, newId(), it.start.toList(), language, now, it.par) }
    }

    /** Keeps the store in step with a round: untouched rounds are not worth saving, started ones are. */
    private fun persist(round: FeedRound) {
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
