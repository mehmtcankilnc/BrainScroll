package com.mehmtcan.brainscroll.game.flashgrid

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.FlashGuide
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlashGameTest {
    private val puzzle = FlashPuzzle(4, 3, 6, 12345)

    private fun wrongTile(pattern: List<Int>) = (0 until 16).first { it !in pattern }

    @Test
    fun everyRoundLightsOneTileMoreAndOnlyDependsOnTheSeed() {
        assertEquals(4, puzzle.rounds)
        for (round in 0 until puzzle.rounds) {
            val pattern = puzzle.pattern(round)
            assertEquals(3 + round, pattern.size)
            assertEquals(pattern.toSet().size, pattern.size)
            assertTrue(pattern.all { it in 0 until 16 })
            assertEquals(pattern, FlashPuzzle(4, 3, 6, 12345).pattern(round), "the same run lights the same tiles")
        }
        assertTrue((0 until 20).any { FlashPuzzle(4, 3, 6, it).pattern(0) != puzzle.pattern(0) }, "other seeds light other tiles")
    }

    @Test
    fun theRunIsSavedAndReadBackInAFewCharacters() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 50) {
                val p = FlashGenerator.generate(seed, difficulty)
                val text = p.encode()
                assertTrue(text.length <= 14, text)
                assertEquals(p, FlashPuzzle.decode(text))
            }
        }
        assertNull(FlashPuzzle.decode(""))
        assertNull(FlashPuzzle.decode("zzzz"))
        assertNull(FlashPuzzle.decode("4 9"))
    }

    @Test
    fun aRunThatCannotBeIsRefused() {
        fun refused(block: () -> FlashPuzzle) = runCatching(block).isFailure
        assertTrue(refused { FlashPuzzle(2, 1, 2, 0) })
        assertTrue(refused { FlashPuzzle(3, 5, 4, 0) })
        assertTrue(refused { FlashPuzzle(3, 3, 9, 0) }, "a grid that is all lit")
        assertTrue(refused { FlashPuzzle(4, 3, 6, -1) })
    }

    @Test
    fun theLevelsMakeTheRunsTheyAreMeantFor() {
        val easy = FlashGenerator.generate(1, Difficulty.Easy)
        val hard = FlashGenerator.generate(1, Difficulty.Hard)
        assertEquals(3, easy.size)
        assertEquals(3, easy.startTiles)
        assertEquals(5, hard.size)
        assertTrue(hard.maxTiles > easy.maxTiles)
        assertEquals(hard, FlashGenerator.generate(1, Difficulty.Hard))
    }

    @Test
    fun findingEveryTileOfARoundClearsItAndTheNextHasOneMore() {
        val game = FlashGame(puzzle).also { it.start() }
        val pattern = puzzle.pattern(0)
        assertEquals(FlashResult.Found, game.tap(pattern[0]))
        assertEquals(FlashResult.Ignored, game.tap(pattern[0]), "a tile found twice")
        assertEquals(FlashResult.Found, game.tap(pattern[1]))
        assertEquals(FlashResult.RoundCleared, game.tap(pattern[2]))
        assertEquals(1, game.state.roundsCleared)
        assertEquals(puzzle.pattern(1), game.state.pattern)
        assertEquals(emptySet(), game.state.found)
        assertFalse(game.isFinished)
    }

    @Test
    fun aTileThatWasNotLitEndsTheRun() {
        val game = FlashGame(puzzle).also { it.start() }
        game.tap(puzzle.pattern(0)[0])
        val wrong = wrongTile(puzzle.pattern(0))
        assertEquals(FlashResult.Missed, game.tap(wrong))
        assertTrue(game.isFinished)
        assertEquals(FlashStatus.Lost, game.state.status)
        assertEquals(wrong, game.state.missed)
        assertEquals(FlashResult.Ignored, game.tap(puzzle.pattern(0)[1]))
    }

    @Test
    fun clearingTheLastRoundWins() {
        val game = FlashGame(puzzle).also { it.start() }
        var last = FlashResult.Ignored
        for (round in 0 until puzzle.rounds) for (tile in puzzle.pattern(round)) last = game.tap(tile)
        assertEquals(FlashResult.Perfect, last)
        assertEquals(FlashStatus.Won, game.state.status)
        assertEquals(puzzle.rounds, game.state.roundsCleared)
        assertEquals(FlashResult.Ignored, game.tap(0))
    }

    @Test
    fun aTileOffTheBoardIsIgnored() {
        val game = FlashGame(puzzle).also { it.start() }
        assertEquals(FlashResult.Ignored, game.tap(-1))
        assertEquals(FlashResult.Ignored, game.tap(16))
        assertFalse(game.isFinished)
    }

    @Test
    fun aSavedRunCanOnlyBeOneThatWasNotWon() {
        val game = FlashGame(puzzle)
        assertFalse(game.restore(-1, true))
        assertFalse(game.restore(puzzle.rounds, true))
        assertFalse(game.restore(2, false), "rounds cleared with no start")
        assertTrue(game.restore(2, true))
        assertEquals(2, game.state.roundsCleared)
        assertTrue(game.state.started)
        assertEquals(puzzle.pattern(2), game.state.pattern)
        assertTrue(FlashGame(puzzle).restore(0, false), "a run that was not started")
    }

    @Test
    fun nothingCanBeTouchedBeforeStartIsPressed() {
        val game = FlashGame(puzzle)
        assertFalse(game.state.started)
        assertEquals(FlashResult.Ignored, game.tap(puzzle.pattern(0)[0]))
        assertEquals(FlashResult.Ignored, game.tap(wrongTile(puzzle.pattern(0))), "a wrong tile does not count either")
        assertFalse(game.isFinished)
        assertTrue(game.start())
        assertFalse(game.start(), "started once")
        assertEquals(1, game.state.showToken, "the tiles are lit when start is pressed")
        assertEquals(FlashResult.Found, game.tap(puzzle.pattern(0)[0]))
    }

    @Test
    fun clearingARoundLightsTheNextOneAgainAndTheTilesAreNotShownForAnythingElse() {
        val game = FlashGame(puzzle)
        game.start()
        val token = game.state.showToken
        game.tap(puzzle.pattern(0)[0])
        assertEquals(token, game.state.showToken, "a found tile does not light anything")
        puzzle.pattern(0).drop(1).forEach { game.tap(it) }
        assertEquals(token + 1, game.state.showToken, "a cleared round lights the next one")
    }

    @Test
    fun startingOverGivesNewTilesAndNeedsStartAgain() {
        val game = FlashGame(puzzle)
        assertFalse(game.restart(game.freshPuzzle()), "nothing started yet")
        game.start()
        puzzle.pattern(0).forEach { game.tap(it) }
        assertEquals(1, game.state.roundsCleared)
        val fresh = game.freshPuzzle()
        assertEquals(puzzle.size, fresh.size)
        assertEquals(puzzle.maxTiles, fresh.maxTiles)
        assertTrue(fresh.seed != puzzle.seed)
        assertTrue(game.restart(fresh))
        assertEquals(fresh, game.puzzle)
        assertEquals(0, game.state.roundsCleared)
        assertFalse(game.state.started)
        assertEquals(FlashResult.Ignored, game.tap(fresh.pattern(0)[0]))
        assertTrue(game.start())
    }

    @Test
    fun aRunThatIsOverCannotBeStartedOver() {
        val game = FlashGame(puzzle)
        game.start()
        game.tap(wrongTile(puzzle.pattern(0)))
        assertFalse(game.restart(game.freshPuzzle()))
        assertFalse(game.start())
    }

    // --- the guide ---

    @Test
    fun theGuideLightsTheTilesNotFoundYet() {
        val game = FlashGame(puzzle).also { it.start() }
        val pattern = puzzle.pattern(0)
        game.tap(pattern[0])
        val step = FlashGuide.step(game.state)!!
        assertEquals(GuideReason.FlashPattern, step.reason)
        assertEquals(pattern.drop(1), step.cells)
        game.tap(wrongTile(pattern))
        assertNull(FlashGuide.step(game.state))
    }

    @Test
    fun theGuideHasNothingToShowBeforeTheRunIsStarted() {
        val game = FlashGame(puzzle)
        assertNull(FlashGuide.step(game.state))
        game.start()
        assertNotNull(FlashGuide.step(game.state))
    }

    // --- the round ---

    private fun round(clock: () -> Long = { 0L }) = FlashRound(0, "r1", puzzle, Language.EN, clock).also { it.start() }

    @Test
    fun theRoundSavesAndRestoresItsRoundsAndTime() {
        var now = 0L
        val round = round { now }
        round.onShown()
        puzzle.pattern(0).forEach { round.tap(it) }
        now = 5_000
        val stored = round.toStored()
        assertEquals(listOf("r1", "s1"), stored.guesses)
        assertEquals(puzzle.encode(), stored.answer)
        assertEquals(GameKind.FlashGrid, stored.game)
        val back = FlashRound.restore(0, stored, { now })!!
        assertEquals(1, back.snapshot().state.roundsCleared)
        assertTrue(back.snapshot().state.started)
        assertFalse(back.isUntouched)
    }

    @Test
    fun aRunLostOrWonIsAResultWithItsRounds() {
        val lost = round()
        puzzle.pattern(0).forEach { lost.tap(it) }
        assertEquals(FlashResult.Missed, lost.tap(wrongTile(puzzle.pattern(1))))
        assertTrue(lost.isFinished)
        val lostRow = lost.toFinished(1L)
        assertEquals(Outcome.LOST, lostRow.outcome)
        assertEquals(listOf("1"), lostRow.guesses)

        val won = round()
        for (r in 0 until puzzle.rounds) puzzle.pattern(r).forEach { won.tap(it) }
        val wonRow = won.toFinished(1L)
        assertEquals(Outcome.WON, wonRow.outcome)
        assertEquals(listOf(puzzle.rounds.toString()), wonRow.guesses)
        assertFalse(won.snapshot().state.status == FlashStatus.Lost)
        assertTrue(won.snapshot().isWon)
        assertFalse(lost.snapshot().isWon)
    }

    @Test
    fun aSavedGameThatCannotBeReadStartsNew() {
        val stored = round().toStored()
        assertNull(FlashRound.restore(0, stored.copy(answer = "!!"), { 0L }))
        assertNull(FlashRound.restore(0, stored.copy(guesses = listOf("x1")), { 0L }))
        assertNull(FlashRound.restore(0, stored.copy(guesses = listOf("r${puzzle.rounds}", "s1")), { 0L }), "a won run would not have been saved")
        assertNull(FlashRound.restore(0, stored.copy(guesses = listOf("r2")), { 0L }), "rounds cleared with no start")
    }

    @Test
    fun comingBackToThePageDoesNotLightTheTilesAgain() {
        var now = 0L
        val round = round { now }
        val token = round.snapshot().state.showToken
        round.onShown()
        round.onHidden()
        round.onShown()
        assertTrue(round.snapshot().onShow)
        assertEquals(token, round.snapshot().state.showToken, "only start and a cleared round light the tiles")
        // A restored run is started and has the same tiles hidden.
        val back = FlashRound.restore(0, round.toStored(), { now })!!
        assertTrue(back.snapshot().state.started)
        now = 9_000
        assertEquals(round.snapshot().withoutClock().let { (it as FlashSnapshot).state.pattern }, back.snapshot().state.pattern)
    }

    @Test
    fun startingOverKeepsTheRunAsAnotherOneInTheSavedRound() {
        val round = round()
        val before = round.toStored().answer
        puzzle.pattern(0).forEach { round.tap(it) }
        assertTrue(round.restart())
        val stored = round.toStored()
        assertTrue(stored.answer != before, "the new tiles are the saved ones")
        assertEquals(listOf("r0"), stored.guesses, "not started: no s1")
        val back = FlashRound.restore(0, stored, { 0L })!!
        assertFalse(back.snapshot().state.started)
        assertEquals(0, back.snapshot().state.roundsCleared)
    }

    @Test
    fun aRoundThatWasOnlyStartedIsNotUntouched() {
        val fresh = FlashRound(0, "r9", puzzle, Language.EN)
        assertTrue(fresh.isUntouched)
        assertTrue(fresh.start())
        assertFalse(fresh.isUntouched)
    }

    @Test
    fun aRoundThatWasNeverTouchedIsUntouched() {
        val round = FlashRound(0, "r8", puzzle, Language.EN)
        assertTrue(round.isUntouched)
        round.tap(puzzle.pattern(0)[0])
        assertTrue(round.isUntouched, "a tap before start does nothing")
        round.start()
        round.tap(puzzle.pattern(0)[0])
        assertFalse(round.isUntouched)
    }
}
