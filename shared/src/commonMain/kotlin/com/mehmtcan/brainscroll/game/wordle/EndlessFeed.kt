package com.mehmtcan.brainscroll.game.wordle

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.memory.FlipResult
import com.mehmtcan.brainscroll.game.hanoi.HanoiGenerator
import com.mehmtcan.brainscroll.game.hanoi.HanoiResult
import com.mehmtcan.brainscroll.game.hanoi.HanoiRound
import com.mehmtcan.brainscroll.game.queens.QueensGenerator
import com.mehmtcan.brainscroll.game.queens.QueensResult
import com.mehmtcan.brainscroll.game.queens.QueensRound
import com.mehmtcan.brainscroll.game.blockout.BlockOutGenerator
import com.mehmtcan.brainscroll.game.blockout.BlockOutResult
import com.mehmtcan.brainscroll.game.blockout.BlockOutRound
import com.mehmtcan.brainscroll.game.nonogram.NonogramGenerator
import com.mehmtcan.brainscroll.game.nonogram.NonogramResult
import com.mehmtcan.brainscroll.game.nonogram.NonogramRound
import com.mehmtcan.brainscroll.game.arrows.ArrowsGenerator
import com.mehmtcan.brainscroll.game.axissum.AxisSumGenerator
import com.mehmtcan.brainscroll.game.axissum.AxisSumResult
import com.mehmtcan.brainscroll.game.axissum.AxisSumRound
import com.mehmtcan.brainscroll.game.ballslide.BallSlideGenerator
import com.mehmtcan.brainscroll.game.ballslide.BallSlideResult
import com.mehmtcan.brainscroll.game.ballslide.BallSlideRound
import com.mehmtcan.brainscroll.game.colorflow.FlowGenerator
import com.mehmtcan.brainscroll.game.colorflow.FlowResult
import com.mehmtcan.brainscroll.game.colorflow.FlowRound
import com.mehmtcan.brainscroll.game.fruitmath.FruitGenerator
import com.mehmtcan.brainscroll.game.fruitmath.FruitResult
import com.mehmtcan.brainscroll.game.fruitmath.FruitRound
import com.mehmtcan.brainscroll.game.codebreaker.CodeGenerator
import com.mehmtcan.brainscroll.game.codebreaker.CodeResult
import com.mehmtcan.brainscroll.game.codebreaker.CodeRound
import com.mehmtcan.brainscroll.game.rectangles.Rect
import com.mehmtcan.brainscroll.game.flashgrid.FlashGenerator
import com.mehmtcan.brainscroll.game.flashgrid.FlashResult
import com.mehmtcan.brainscroll.game.flashgrid.FlashRound
import com.mehmtcan.brainscroll.game.rectangles.RectGenerator
import com.mehmtcan.brainscroll.game.rectangles.RectResult
import com.mehmtcan.brainscroll.game.rectangles.RectRound
import com.mehmtcan.brainscroll.game.lasers.LaserGenerator
import com.mehmtcan.brainscroll.game.untangle.UntangleGenerator
import com.mehmtcan.brainscroll.game.untangle.UntangleResult
import com.mehmtcan.brainscroll.game.untangle.UntangleRound
import com.mehmtcan.brainscroll.game.lasers.LaserResult
import com.mehmtcan.brainscroll.game.lasers.LaserRound
import com.mehmtcan.brainscroll.game.matchsticks.MatchGenerator
import com.mehmtcan.brainscroll.game.matchsticks.MatchMove
import com.mehmtcan.brainscroll.game.matchsticks.MatchResult
import com.mehmtcan.brainscroll.game.matchsticks.MatchRound
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsGenerator
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsResult
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsRound
import com.mehmtcan.brainscroll.game.arrows.ArrowsResult
import com.mehmtcan.brainscroll.game.arrows.ArrowsRound
import com.mehmtcan.brainscroll.game.tents.TentsGenerator
import com.mehmtcan.brainscroll.game.tents.TentsResult
import com.mehmtcan.brainscroll.game.tents.TentsRound
import com.mehmtcan.brainscroll.game.lights.LightsResult
import com.mehmtcan.brainscroll.game.lights.LightsRound
import com.mehmtcan.brainscroll.game.line.LineGenerator
import com.mehmtcan.brainscroll.game.line.LinePuzzle
import com.mehmtcan.brainscroll.game.line.LineResult
import com.mehmtcan.brainscroll.game.line.LineRound
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
import com.mehmtcan.brainscroll.game.sudoku.SudokuVariant
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
    /** The level of each page, in the order of [pages]. */
    val difficulties: List<Difficulty> = emptyList(),
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
    /** The level of each game's new boards; a game that is not in the map is medium. */
    difficulties: Map<GameKind, Difficulty> = emptyMap(),
    /** Makes the board of a new one-line page. The daily puzzle gives the board of its day; the default is a random 5x5 one. */
    private val lineBoard: (Random) -> LinePuzzle = { LineGenerator.generate(it.nextLong(), size = 5, checkpoints = 4) },
) {
    /** Every game this feed can show, in order. [games] is the part of it the player switched on. */
    private val allGames: List<GameKind> = games
    private var games: List<GameKind> = games

    private var levels: Map<GameKind, Difficulty> = difficulties

    /** The level each round was made for, by round id. A round restored from storage keeps the level it was saved with. */
    private val roundLevels = mutableMapOf<String, Difficulty>()

    private val rounds = mutableListOf<FeedRound>()
    private val counted = mutableSetOf<Int>()

    /** Rounds that were played again from the history: their result is kept, but a win does not raise [solvedCount]. */
    private val replays = mutableSetOf<String>()
    private val bags = mutableMapOf<Language, ArrayDeque<String>>()

    /** The page that is on the screen, or null while the feed is not (see [pageShown]). */
    private var shownPage: Int? = null

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
                GameKind.Sudoku, GameKind.Jigsaw -> SudokuRound.restore(rounds.size, stored, now)
                GameKind.Lights -> LightsRound.restore(rounds.size, stored, now)
                GameKind.Slide -> SlideRound.restore(rounds.size, stored, now)
                GameKind.Target -> TargetRound.restore(rounds.size, stored, now)
                GameKind.Pipes -> PipesRound.restore(rounds.size, stored, now)
                GameKind.Hanoi -> HanoiRound.restore(rounds.size, stored, now)
                GameKind.Queens -> QueensRound.restore(rounds.size, stored, now)
                GameKind.Tents -> TentsRound.restore(rounds.size, stored, now)
                GameKind.BlockOut -> BlockOutRound.restore(rounds.size, stored, now)
                GameKind.Arrows -> ArrowsRound.restore(rounds.size, stored, now)
                GameKind.Nonogram -> NonogramRound.restore(rounds.size, stored, now)
                GameKind.AxisSum -> AxisSumRound.restore(rounds.size, stored, now)
                GameKind.BallSlide -> BallSlideRound.restore(rounds.size, stored, now)
                GameKind.ColorFlow -> FlowRound.restore(rounds.size, stored, now)
                GameKind.TopicGroups -> TopicGroupsRound.restore(rounds.size, stored, now)
                GameKind.Matchsticks -> MatchRound.restore(rounds.size, stored, now)
                GameKind.Lasers -> LaserRound.restore(rounds.size, stored, now)
                GameKind.Untangle -> UntangleRound.restore(rounds.size, stored, now)
                GameKind.FruitMath -> FruitRound.restore(rounds.size, stored, now)
                GameKind.CodeBreaker -> CodeRound.restore(rounds.size, stored, now)
                GameKind.Rectangles -> RectRound.restore(rounds.size, stored, now)
                GameKind.FlashGrid -> FlashRound.restore(rounds.size, stored, now)
                GameKind.Line -> LineRound.restore(rounds.size, stored, now)
            }
            if (round != null) {
                rounds += round
                roundLevels[round.id] = stored.difficulty
            }
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

    /** Marks the round [id] as played again from the history (a favorite): its win leaves the solved count as it is. */
    fun markAsReplay(id: String) {
        replays += id
    }

    /** The level of the new boards of [kind]. */
    fun difficultyOf(kind: GameKind): Difficulty = levels[kind] ?: Difficulty.DEFAULT

    /**
     * Changes the level of the boards of [kind] that come from now on. Pages of that game from [fromIndex] on that were not
     * touched and were made for another level are drawn again; started and finished pages stay as they are.
     */
    fun setDifficulty(kind: GameKind, difficulty: Difficulty, fromIndex: Int) {
        if (difficultyOf(kind) == difficulty) return
        levels = levels + (kind to difficulty)
        for (i in fromIndex.coerceAtLeast(0) until rounds.size) {
            val round = rounds[i]
            if (round.kind == kind && round.isUntouched && !round.assisted && levelOf(round) != difficulty) rounds[i] = newRound(i, kind)
        }
    }

    private fun levelOf(round: FeedRound): Difficulty = roundLevels[round.id] ?: Difficulty.DEFAULT

    private fun stored(round: FeedRound): StoredRound = round.toStored().copy(difficulty = levelOf(round))

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
            if (round is TopicGroupsRound && round.isUntouched && !round.assisted && round.language !in next) rounds[i] = newRound(i, GameKind.TopicGroups)
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

    // --- Hanoi actions (ignored on a page of another game) ---

    /** Moves the top disk of tower [from] onto tower [to] on the Hanoi board of page [index]. */
    fun moveDisk(index: Int, from: Int, to: Int): HanoiResult {
        val round = rounds[index] as? HanoiRound ?: return HanoiResult.Ignored
        val result = round.move(from, to)
        if (result != HanoiResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the disks of page [index] back as they were at the start. Returns false when nothing had changed. */
    fun restartHanoi(index: Int): Boolean {
        val round = rounds[index] as? HanoiRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Queens actions (ignored on a page of another game) ---

    /** Taps [cell] on the queens board of page [index]: puts a queen there or takes the player's queen away. */
    fun toggleQueen(index: Int, cell: Int): QueensResult {
        val round = rounds[index] as? QueensRound ?: return QueensResult.Ignored
        val result = round.toggle(cell)
        if (result != QueensResult.Ignored) afterMove(index, round)
        return result
    }

    /** Takes away the queens the player put on page [index]. Returns false when there were none. */
    fun restartQueens(index: Int): Boolean {
        val round = rounds[index] as? QueensRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Tents actions (ignored on a page of another game) ---

    /** Taps [cell] on the tents board of page [index]: empty, tent, grass, empty. */
    fun tapTent(index: Int, cell: Int): TentsResult {
        val round = rounds[index] as? TentsRound ?: return TentsResult.Ignored
        val result = round.tap(cell)
        if (result != TentsResult.Ignored) afterMove(index, round)
        return result
    }

    /** Clears the marks on page [index]. Returns false when there were none. */
    fun restartTents(index: Int): Boolean {
        val round = rounds[index] as? TentsRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Arrow escape actions (ignored on a page of another game) ---

    /** Taps [cell] on the arrow escape board of page [index]: the arrow leaves, or it is a mistake. */
    fun tapArrow(index: Int, cell: Int): ArrowsResult {
        val round = rounds[index] as? ArrowsRound ?: return ArrowsResult.Ignored
        val result = round.tap(cell)
        if (result != ArrowsResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the arrows of page [index] back. Returns false when none had left. */
    fun restartArrows(index: Int): Boolean {
        val round = rounds[index] as? ArrowsRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Axis sum actions (ignored on a page of another game) ---

    /** Taps [cell] on the axis sum board of page [index]: the number is erased, or brought back. */
    fun tapAxisSum(index: Int, cell: Int): AxisSumResult {
        val round = rounds[index] as? AxisSumRound ?: return AxisSumResult.Ignored
        val result = round.tap(cell)
        if (result != AxisSumResult.Ignored) afterMove(index, round)
        return result
    }

    /** Brings the numbers of page [index] back. Returns false when none was erased. */
    fun restartAxisSum(index: Int): Boolean {
        val round = rounds[index] as? AxisSumRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Ball slide actions (ignored on a page of another game) ---

    /** Slides the ball of page [index] in direction [dir] (see BallDir). */
    fun slideBall(index: Int, dir: Int): BallSlideResult {
        val round = rounds[index] as? BallSlideRound ?: return BallSlideResult.Ignored
        val result = round.slide(dir)
        if (result != BallSlideResult.Ignored) afterMove(index, round)
        return result
    }

    /** Takes the last slide of page [index] back. Returns false when there is none. */
    fun undoBall(index: Int): Boolean {
        val round = rounds[index] as? BallSlideRound ?: return false
        return round.undo().also { if (it) persist(round) }
    }

    /** Puts the ball of page [index] on its start again. Returns false when nothing was done yet. */
    fun restartBall(index: Int): Boolean {
        val round = rounds[index] as? BallSlideRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Color flow actions (ignored on a page of another game) ---

    /** A stroke begins on [cell] of the color flow board of page [index]. Returns false if it cannot begin there. */
    fun startFlow(index: Int, cell: Int): Boolean = (rounds[index] as? FlowRound)?.start(cell) ?: false

    /** The finger of the stroke on page [index] moves on to [cell]. Returns true if the board changed. */
    fun extendFlow(index: Int, cell: Int): Boolean = (rounds[index] as? FlowRound)?.extend(cell) ?: false

    /** The finger of the stroke on page [index] is lifted: the stroke counts, and the board is saved or finished. */
    fun endFlow(index: Int): FlowResult {
        val round = rounds[index] as? FlowRound ?: return FlowResult.Ignored
        val result = round.end()
        if (result != FlowResult.Ignored) afterMove(index, round)
        return result
    }

    /** Clears the paths of page [index]. Returns false when there were none. */
    fun restartFlow(index: Int): Boolean {
        val round = rounds[index] as? FlowRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Fruit equations actions (ignored on a page of another game) ---

    /** Gives [answer] for the last line of the fruit equations board of page [index]. */
    fun submitFruit(index: Int, answer: Int): FruitResult {
        val round = rounds[index] as? FruitRound ?: return FruitResult.Ignored
        val result = round.submit(answer)
        if (result != FruitResult.Ignored) afterMove(index, round)
        return result
    }

    // --- Flash grid actions (ignored on a page of another game) ---

    /** Start is pressed on the flash grid of page [index]: the first round is lit. Returns false if it was started already. */
    fun startFlash(index: Int): Boolean {
        val round = rounds[index] as? FlashRound ?: return false
        return round.start().also { if (it) persist(round) }
    }

    /** The flash grid of page [index] is started over with other tiles. Returns false if there was nothing to start over. */
    fun restartFlash(index: Int): Boolean {
        val round = rounds[index] as? FlashRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    /** Touches tile [tile] of the flash grid on page [index]. */
    fun tapFlashTile(index: Int, tile: Int): FlashResult {
        val round = rounds[index] as? FlashRound ?: return FlashResult.Ignored
        val result = round.tap(tile)
        if (result != FlashResult.Ignored) afterMove(index, round)
        return result
    }

    // --- Rectangles actions (ignored on a page of another game) ---

    /** Lays [rect] on the rectangles board of page [index]. */
    fun placeRect(index: Int, rect: Rect): RectResult {
        val round = rounds[index] as? RectRound ?: return RectResult.Ignored
        val result = round.place(rect)
        if (result == RectResult.Placed || result == RectResult.Solved) afterMove(index, round)
        return result
    }

    /** Takes off the rectangle over the cell ([column], [row]) of page [index]. Returns true if there was one. */
    fun removeRect(index: Int, column: Int, row: Int): Boolean {
        val round = rounds[index] as? RectRound ?: return false
        return round.remove(column, row).also { if (it) persist(round) }
    }

    /** Takes every rectangle off page [index]. Returns false when there were none. */
    fun restartRects(index: Int): Boolean {
        val round = rounds[index] as? RectRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Code breaker actions (ignored on a page of another game) ---

    /** Gives [guess] on the code breaker board of page [index]. */
    fun submitCode(index: Int, guess: List<Int>): CodeResult {
        val round = rounds[index] as? CodeRound ?: return CodeResult.Ignored
        val result = round.submit(guess)
        if (result != CodeResult.Ignored) afterMove(index, round)
        return result
    }

    // --- Untangle actions (ignored on a page of another game) ---

    /** Dot [node] of the tangle of page [index] is at ([x], [y]) while the finger is down. Returns true if the dot moved. */
    fun moveNode(index: Int, node: Int, x: Int, y: Int): Boolean = (rounds[index] as? UntangleRound)?.move(node, x, y) ?: false

    /** The finger of the drag on page [index] is lifted: the drag counts, and the board is saved or finished. */
    fun dropNode(index: Int): UntangleResult {
        val round = rounds[index] as? UntangleRound ?: return UntangleResult.Ignored
        val result = round.drop()
        if (result != UntangleResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the dots of page [index] back where the board was given. Returns false when they already lie so. */
    fun restartUntangle(index: Int): Boolean {
        val round = rounds[index] as? UntangleRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Laser mirror actions (ignored on a page of another game) ---

    /** Turns the mirror on [cell] of the laser board of page [index]. */
    fun turnMirror(index: Int, cell: Int): LaserResult {
        val round = rounds[index] as? LaserRound ?: return LaserResult.Ignored
        val result = round.turn(cell)
        if (result != LaserResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts every mirror of page [index] back as the board was given. Returns false when they already stand so. */
    fun restartLasers(index: Int): Boolean {
        val round = rounds[index] as? LaserRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Matchstick actions (ignored on a page of another game) ---

    /** Takes a stick and lays it on another place on the matchstick sum of page [index]. */
    fun moveStick(index: Int, move: MatchMove): MatchResult {
        val round = rounds[index] as? MatchRound ?: return MatchResult.Ignored
        val result = round.move(move)
        if (result != MatchResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the stick of page [index] back after a wrong move. Returns false when no stick was off its place. */
    fun undoStick(index: Int): Boolean {
        val round = rounds[index] as? MatchRound ?: return false
        return round.undo().also { if (it) persist(round) }
    }

    // --- Word groups actions (ignored on a page of another game) ---

    /** Taps [word] of the word groups board of page [index]: the word is picked, put back, or the four picked are checked. */
    fun tapTopicWord(index: Int, word: Int): TopicGroupsResult {
        val round = rounds[index] as? TopicGroupsRound ?: return TopicGroupsResult.Ignored
        val result = round.tap(word)
        if (result != TopicGroupsResult.Ignored) afterMove(index, round)
        return result
    }

    /** Mixes up the words of the word groups board of page [index] that are not found yet. Returns false when there is nothing to mix. */
    fun shuffleTopicWords(index: Int): Boolean {
        val round = rounds[index] as? TopicGroupsRound ?: return false
        return round.shuffle().also { if (it) persist(round) }
    }

    /** Puts the picked words of page [index] back. Returns false when none was picked. */
    fun clearTopicSelection(index: Int): Boolean {
        val round = rounds[index] as? TopicGroupsRound ?: return false
        return round.clearSelection().also { if (it) persist(round) }
    }

    // --- Mini nonogram actions (ignored on a page of another game) ---

    /** Sets the cells from [from] to [to] (on one row or column) of the nonogram board of page [index] to [value]. */
    fun paintNonogram(index: Int, from: Int, to: Int, value: Int): NonogramResult {
        val round = rounds[index] as? NonogramRound ?: return NonogramResult.Ignored
        val result = round.paint(from, to, value)
        if (result != NonogramResult.Ignored) afterMove(index, round)
        return result
    }

    /** Clears the board of page [index]. Returns false when it was clear. */
    fun restartNonogram(index: Int): Boolean {
        val round = rounds[index] as? NonogramRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- Block out actions (ignored on a page of another game) ---

    /** Slides [piece] of the block out board of page [index] to the place [to]. */
    fun slideBlock(index: Int, piece: Int, to: Int): BlockOutResult {
        val round = rounds[index] as? BlockOutRound ?: return BlockOutResult.Ignored
        val result = round.move(piece, to)
        if (result != BlockOutResult.Ignored) afterMove(index, round)
        return result
    }

    /** Puts the pieces of page [index] back as they were at the start. Returns false when nothing had moved. */
    fun restartBlockOut(index: Int): Boolean {
        val round = rounds[index] as? BlockOutRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    // --- One-line actions (ignored on a page of another game) ---

    /** A finger moving over [cell] on the one-line board of page [index]. */
    fun dragLine(index: Int, cell: Int): LineResult {
        val round = rounds[index] as? LineRound ?: return LineResult.Ignored
        val result = round.drag(cell)
        if (result != LineResult.Ignored) afterMove(index, round)
        return result
    }

    /** A tap on [cell] on the one-line board of page [index]. */
    fun tapLine(index: Int, cell: Int): LineResult {
        val round = rounds[index] as? LineRound ?: return LineResult.Ignored
        val result = round.tap(cell)
        if (result != LineResult.Ignored) afterMove(index, round)
        return result
    }

    /** Takes the last cell off the line of page [index]. Returns false when there was nothing to take off. */
    fun undoLine(index: Int): Boolean {
        val round = rounds[index] as? LineRound ?: return false
        return round.undo().also { if (it) persist(round) }
    }

    /** Clears the line of page [index]. Returns false when it had not begun. */
    fun restartLine(index: Int): Boolean {
        val round = rounds[index] as? LineRound ?: return false
        return round.restart().also { if (it) persist(round) }
    }

    /** The next cells of the line of page [index], or null when there is no hint to give (it costs a star when it is given). */
    fun hintLine(index: Int): List<Int>? {
        val round = rounds[index] as? LineRound ?: return null
        return round.showHint().also { if (it != null) persist(round) }
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
            round.onHidden() // the clock stops when the board is over
            val result = round.toFinished(finishedAt = now()).copy(difficulty = levelOf(round))
            // Only a win without the guide counts; a loss or a guided puzzle leaves the number as it is.
            if (!result.assisted && result.outcome == Outcome.WON && round.id !in replays) solvedCount++
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
        store.saveProgress(stored(round))
    }

    /** The page [index] is on the screen now: the page that was on show before it is left, and the clock of a timed game starts. */
    fun pageShown(index: Int) {
        if (shownPage != index) shownPage?.let { leave(it) }
        shownPage = index
        rounds.getOrNull(index)?.onShown()
    }

    /** The feed is not on the screen any more (another tab, the app in the background): the page that was on show is left. */
    fun pagesHidden() {
        shownPage?.let { leave(it) }
        shownPage = null
    }

    /** The clock of the round on page [index] stops, and what it has counted is saved. */
    private fun leave(index: Int) {
        val round = rounds.getOrNull(index) ?: return
        round.onHidden()
        // A page that was not touched is not saved (a puzzle that was only looked at is not worth a place in the store).
        if (!round.isUntouched) persist(round)
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
        difficulties = rounds.map { levelOf(it) },
    )

    /** A new page of [kind], made for the level chosen for that game. */
    private fun newRound(index: Int, kind: GameKind): FeedRound = buildRound(index, kind).also { roundLevels[it.id] = difficultyOf(kind) }

    private fun buildRound(index: Int, kind: GameKind): FeedRound = when (kind) {
        GameKind.Word -> {
            val list = wordLists.getValue(wordLanguages.random(random))
            GameLevels.word(difficultyOf(kind)).let { WordleRound(index, WordleGame(list, nextAnswer(list), it.attempts, it.hardMode), list, newId()) }
        }
        GameKind.Memory -> MemoryRound(index, newId(), MemoryGame.newLayout(random, difficultyOf(kind)), language, now)
        GameKind.Tubes -> GameLevels.tubes(difficultyOf(kind)).let { TubesRound(index, newId(), TubesGame.newLayout(random, it.colors, empty = it.empty), language, now) }
        GameKind.Mines -> GameLevels.mines(difficultyOf(kind)).let { MinesRound(index, newId(), random.nextInt(), language, now, it.rows, it.cols, it.mines) }
        GameKind.Sudoku -> SudokuRound(index, newId(), random.nextInt(), language, now, GameLevels.sudokuClues(difficultyOf(kind)))
        GameKind.Jigsaw -> SudokuRound(index, newId(), random.nextInt(), language, now, GameLevels.jigsawClues(difficultyOf(kind)), SudokuVariant.Jigsaw)
        GameKind.Lights -> GameLevels.lights(difficultyOf(kind)).let { LightsRound(index, newId(), random.nextInt(), language, now, it.size, it.taps) }
        GameKind.Slide -> GameLevels.slide(difficultyOf(kind)).let { SlideRound(index, newId(), random.nextInt(), language, now, it.cols, it.rows, it.scramble) }
        GameKind.Target -> TargetGenerator.generate(random.nextInt(), difficultyOf(kind)).let { TargetRound(index, newId(), it.numbers, it.target, language, now, it.par) }
        GameKind.Pipes -> GameLevels.pipes(difficultyOf(kind)).let { PipesGenerator.generate(random.nextInt(), it.size, it.minPar) }.let { PipesRound(index, newId(), it.start.toList(), language, now, it.par) }
        GameKind.Hanoi -> HanoiGenerator.generate(random.nextInt(), difficultyOf(kind)).let { HanoiRound(index, newId(), it.start.toList(), it.goal, language, now, it.par) }
        GameKind.Queens -> GameLevels.queens(difficultyOf(kind)).let { QueensGenerator.generate(random.nextInt(), it.size, it.locked) }.let { QueensRound(index, newId(), it.size, it.locked, language, now) }
        GameKind.Tents -> TentsRound(index, newId(), TentsGenerator.generate(random.nextInt(), difficultyOf(kind)), language, now)
        GameKind.BlockOut -> GameLevels.blockOut(difficultyOf(kind)).let { BlockOutGenerator.generate(random.nextInt(), it.size, it.minPar) }.let { BlockOutRound(index, newId(), it.puzzle, language, now, it.par) }
        GameKind.Arrows -> ArrowsRound(index, newId(), ArrowsGenerator.generate(random.nextInt(), difficultyOf(kind)), language, now)
        GameKind.Nonogram -> NonogramRound(index, newId(), NonogramGenerator.generate(random.nextInt(), difficultyOf(kind)), language, now)
        GameKind.AxisSum -> AxisSumRound(index, newId(), AxisSumGenerator.generate(random.nextInt(), difficultyOf(kind)), language, now)
        GameKind.ColorFlow -> FlowGenerator.generate(random.nextInt(), difficultyOf(kind)).let { FlowRound(index, newId(), it.puzzle, language, now) }
        GameKind.TopicGroups -> TopicGroupsRound(index, newId(), TopicGroupsGenerator.generate(random.nextInt(), wordLanguages.random(random), difficultyOf(kind)), now)
        GameKind.Matchsticks -> MatchGenerator.generate(random.nextInt(), difficultyOf(kind)).let { MatchRound(index, newId(), it.puzzle, language, now) }
        GameKind.Lasers -> LaserGenerator.generate(random.nextInt(), difficultyOf(kind)).let { LaserRound(index, newId(), it.puzzle, language, now) }
        GameKind.Untangle -> UntangleGenerator.generate(random.nextInt(), difficultyOf(kind)).let { UntangleRound(index, newId(), it.puzzle, language, now) }
        GameKind.FruitMath -> FruitGenerator.generate(random.nextInt(), difficultyOf(kind)).let { FruitRound(index, newId(), it, language, now) }
        GameKind.CodeBreaker -> CodeGenerator.generate(random.nextInt(), difficultyOf(kind)).let { CodeRound(index, newId(), it, language, now) }
        GameKind.FlashGrid -> FlashGenerator.generate(random.nextInt(), difficultyOf(kind)).let { FlashRound(index, newId(), it, language, now) }
        GameKind.Rectangles -> RectGenerator.generate(random.nextInt(), difficultyOf(kind)).let { RectRound(index, newId(), it, language, now) }
        GameKind.BallSlide -> BallSlideGenerator.generate(random.nextInt(), difficultyOf(kind)).let { BallSlideRound(index, newId(), it.puzzle, it.par, language, now) }
        GameKind.Line -> LineRound(index, newId(), lineBoard(random), language, now)
    }

    /** Keeps the store in step with a round: untouched rounds are not worth saving, started ones are. */
    private fun persist(round: FeedRound) {
        when {
            round.isFinished -> Unit // `finish` already moved it to the history
            round.isUntouched -> store.deleteProgress(round.id)
            else -> store.saveProgress(stored(round))
        }
    }

    /** Draws from a shuffled bag so answers only repeat after the whole pool was used. */
    private fun nextAnswer(list: WordList): String {
        val bag = bags.getOrPut(list.language) { ArrayDeque() }
        if (bag.isEmpty()) bag.addAll(list.answers.shuffled(random))
        return bag.removeFirst()
    }
}

