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
    /** The language of the pages of other games and the first of [wordLanguages]. */
    val language: Language,
    /** The languages the word puzzles of this feed come in, one or more. */
    val wordLanguages: List<Language>,
    /** Puzzles solved in this feed without the guide, all games together. Nothing but a new feed (or a new start of the app) resets it: a loss, or leaving a puzzle unfinished, does not. */
    val solvedCount: Int,
    /** A puzzle was started and not finished: there is a feed to go on with. */
    val hasUnfinished: Boolean,
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
 * - Answer streak: +1 for a win, back to 0 for a loss, in any game. Leaving a puzzle unfinished does not break it.
 * - Word answers do not repeat until the whole pool of that language has been used.
 * - The word puzzles come in one or more languages ([setWordLanguages]); with several, each puzzle gets one of them
 *   at random. The other games do not use a language.
 */
class EndlessFeed(
    private val wordLists: Map<Language, WordList>,
    startLanguage: Language,
    private val random: Random = Random.Default,
    private val store: FeedStore = NoFeedStore,
    /** Unfinished puzzles from an earlier run. They become the first pages, in their saved order. */
    restored: List<StoredRound> = emptyList(),
    /** How many puzzles were already solved in this flow when the feed was made (a feed that goes on after the last page was finished). */
    startSolved: Int = 0,
    private val now: () -> Long = { 0L },
    private val newId: () -> String = { Random.nextLong().toULong().toString(16) },
    /** The games of the feed and their order: each page is followed by the next game in this list, round and round. */
    games: List<GameKind> = listOf(GameKind.Word),
    /** The languages of the word puzzles; each new word puzzle is in one of them, chosen at random. */
    startLanguages: List<Language> = listOf(startLanguage),
) {
    /** Every game this feed can show, in order. [games] is the part of it the player switched on. */
    private val allGames: List<GameKind> = games
    private var games: List<GameKind> = games

    private val rounds = mutableListOf<FeedRound>()
    private val counted = mutableSetOf<Int>()
    private val bags = mutableMapOf<Language, ArrayDeque<String>>()

    var wordLanguages: List<Language> = startLanguages.ifEmpty { listOf(startLanguage) }
        private set

    /** The language for everything that is not a word puzzle: the first of [wordLanguages]. */
    val language: Language get() = wordLanguages.first()
    var solvedCount = startSolved
        private set
    val size: Int get() = rounds.size

    init {
        if (GameKind.Word in games) wordLanguages.forEach { require(wordLists.containsKey(it)) { "no word list for $it" } }
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
     * and belong to a game that is now off are replaced; started and finished pages stay as they are.
     * At least one game stays on: an empty [enabled] (or one with no known game) changes nothing.
     */
    fun setEnabledGames(enabled: Set<GameKind>, fromIndex: Int) {
        val next = allGames.filter { it in enabled }
        if (next.isEmpty() || next == games) return
        games = next
        for (i in fromIndex.coerceAtLeast(0) until rounds.size) {
            val round = rounds[i]
            if (round.isUntouched && !round.assisted && round.kind !in games) {
                rounds[i] = newRound(i, kindAfter(rounds.getOrNull(i - 1)?.kind))
            }
        }
    }

    /**
     * Changes the languages of the word puzzles that come from now on. Word pages from [fromIndex] on that were not
     * touched and are in a language that is now off are drawn again; puzzles that were started keep the language they
     * began in. An empty [languages] (or one with no known language) changes nothing.
     */
    fun setWordLanguages(languages: Set<Language>, fromIndex: Int) {
        val next = Language.entries.filter { it in languages && wordLists.containsKey(it) }
        if (next.isEmpty() || next == wordLanguages) return
        wordLanguages = next
        for (i in fromIndex.coerceAtLeast(0) until rounds.size) {
            val round = rounds[i]
            if (round is WordleRound && round.isUntouched && !round.assisted && round.language !in next) rounds[i] = newRound(i, GameKind.Word)
        }
    }

    /** The same for a feed with one word language. */
    fun setWordLanguage(newLanguage: Language, fromIndex: Int) = setWordLanguages(setOf(newLanguage), fromIndex)

    // --- Word puzzle actions (ignored on a page of another game) ---

    fun type(index: Int, letter: Char) {
        val round = rounds[index] as? WordleRound ?: return
        round.type(letter)
        persist(round)
    }

    fun backspace(index: Int) {
        val round = rounds[index] as? WordleRound ?: return
        round.backspace()
        persist(round)
    }

    fun submit(index: Int) {
        val round = rounds[index] as? WordleRound ?: return
        round.submit()
        afterMove(index, round)
    }

    // --- Memory match actions (ignored on a page of another game) ---

    /** Turns over card [card] of the memory board on page [index]. */
    fun flip(index: Int, card: Int): FlipResult {
        val round = rounds[index] as? MemoryRound ?: return FlipResult.Ignored
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
        val result = round.pour(from, to)
        if (result != PourResult.Ignored) afterMove(index, round)
        return result
    }

    /** Takes back the last pour on page [index]. Returns false when there was nothing to take back. */
    fun undo(index: Int): Boolean {
        val round = rounds[index] as? TubesRound ?: return false
        return round.undo().also { if (it) persist(round) }
    }

    /** Puts the tubes of page [index] back as they were at the start. Returns false when nothing had changed. */
    fun restart(index: Int): Boolean {
        val round = rounds[index] as? TubesRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Minesweeper actions (ignored on a page of another game) ---

    /** Opens [cell] on the minesweeper board of page [index]. */
    fun openCell(index: Int, cell: Int): MinesResult {
        val round = rounds[index] as? MinesRound ?: return MinesResult.Ignored
        val result = round.open(cell)
        if (result != MinesResult.Ignored) afterMove(index, round)
        return result
    }

    /** Flags or unflags [cell] on page [index]. Returns false when that was not possible. */
    fun flagCell(index: Int, cell: Int): Boolean {
        val round = rounds[index] as? MinesRound ?: return false
        return round.flag(cell).also { if (it) persist(round) }
    }

    /** Opens the neighbors of the open number [cell] that are not flagged, when its flags match its number. */
    fun chordCell(index: Int, cell: Int): MinesResult {
        val round = rounds[index] as? MinesRound ?: return MinesResult.Ignored
        val result = round.chord(cell)
        if (result != MinesResult.Ignored) afterMove(index, round)
        return result
    }

    // --- Lights Out actions (ignored on a page of another game) ---

    /** Taps [cell] on the Lights Out board of page [index]. */
    fun tapLight(index: Int, cell: Int): LightsResult {
        val round = rounds[index] as? LightsRound ?: return LightsResult.Ignored
        val result = round.tap(cell)
        if (result != LightsResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the lights of page [index] back as they were at the start. Returns false when nothing had changed. */
    fun restartLights(index: Int): Boolean {
        val round = rounds[index] as? LightsRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Sliding puzzle actions (ignored on a page of another game) ---

    /** Taps the tile in [cell] on the sliding puzzle of page [index]. */
    fun slideTile(index: Int, cell: Int): SlideResult {
        val round = rounds[index] as? SlideRound ?: return SlideResult.Ignored
        val result = round.slide(cell)
        if (result != SlideResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the tiles of page [index] back as they were at the start. Returns false when nothing had changed. */
    fun restartSlide(index: Int): Boolean {
        val round = rounds[index] as? SlideRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Target number actions (ignored on a page of another game) ---

    /** Combines the numbers [firstId] and [secondId] with [op] on the target number game of page [index]. */
    fun combineNumbers(index: Int, firstId: Int, op: Op, secondId: Int): TargetResult {
        val round = rounds[index] as? TargetRound ?: return TargetResult.Ignored
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
        return round.undo().also { if (it) persist(round) }
    }

    /** Puts the numbers of page [index] back as they were at the start. Returns false when nothing had been done. */
    fun restartNumbers(index: Int): Boolean {
        val round = rounds[index] as? TargetRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Pipes actions (ignored on a page of another game) ---

    /** Turns the tile in [cell] on the pipes board of page [index]. */
    fun turnPipe(index: Int, cell: Int): PipesResult {
        val round = rounds[index] as? PipesRound ?: return PipesResult.Ignored
        val result = round.tap(cell)
        if (result != PipesResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the tiles of page [index] back as they were at the start. Returns false when nothing had changed. */
    fun restartPipes(index: Int): Boolean {
        val round = rounds[index] as? PipesRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Sudoku actions (ignored on a page of another game) ---

    /** Puts [digit] in [cell] of the sudoku board of page [index]. A wrong digit is a mistake; the third one loses. */
    fun placeDigit(index: Int, cell: Int, digit: Int): SudokuResult {
        val round = rounds[index] as? SudokuRound ?: return SudokuResult.Ignored
        val result = round.place(cell, digit)
        if (result != SudokuResult.Ignored) afterMove(index, round)
        return result
    }

    /** Switches the pencil note [digit] on or off in [cell] of page [index]. */
    fun toggleNote(index: Int, cell: Int, digit: Int): SudokuResult {
        val round = rounds[index] as? SudokuRound ?: return SudokuResult.Ignored
        return round.toggleNote(cell, digit).also { if (it != SudokuResult.Ignored) persist(round) }
    }

    /** Takes back a wrong digit or the notes of [cell] on page [index]. */
    fun eraseCell(index: Int, cell: Int): SudokuResult {
        val round = rounds[index] as? SudokuRound ?: return SudokuResult.Ignored
        return round.erase(cell).also { if (it != SudokuResult.Ignored) persist(round) }
    }

    /** After a move: a round that just ended becomes a result (once), anything else is saved as progress. */
    private fun afterMove(index: Int, round: FeedRound) {
        if (round.isFinished && counted.add(index)) {
            val result = round.toFinished(finishedAt = now())
            // Only a win without the guide counts; a loss or a guided puzzle leaves the number as it is.
            if (!result.assisted && result.outcome == Outcome.WON) solvedCount++
            store.finish(result)
        } else {
            persist(round)
        }
    }

    /** The next step of the guide for page [index] as work to run anywhere, or null if the puzzle is over. */
    fun guideTask(index: Int): com.mehmtcan.brainscroll.game.guide.GuideTask? = rounds.getOrNull(index)?.guideTask()

    /**
     * The guide gave a step for page [index]: from now on the puzzle is neutral (no streak, no statistics) and is saved
     * so, also when it was not started yet.
     */
    fun markGuided(index: Int) {
        val round = rounds.getOrNull(index) ?: return
        if (round.isFinished || round.assisted) return
        round.markAssisted()
        store.saveProgress(round.toStored())
    }

    /** The first page whose puzzle was started and is not finished, or null. The feed "to go on with". */
    fun firstUnfinishedIndex(): Int? = rounds.indexOfFirst { !it.isUntouched && !it.isFinished }.takeIf { it >= 0 }

    /** True once the player did something on a page. */
    val hasStarted: Boolean get() = rounds.any { !it.isUntouched }

    fun snapshot() = FeedSnapshot(
        pages = rounds.map { it.snapshot() },
        language = language,
        wordLanguages = wordLanguages,
        solvedCount = solvedCount,
        hasUnfinished = firstUnfinishedIndex() != null,
    )

    private fun newRound(index: Int, kind: GameKind): FeedRound = when (kind) {
        GameKind.Word -> {
            val list = wordLists.getValue(wordLanguages.random(random))
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
            round.isUntouched -> store.deleteProgress(round.id)
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

