package com.mehmtcan.brainscroll.game.guide

import com.mehmtcan.brainscroll.game.lights.LightsGame
import com.mehmtcan.brainscroll.game.lights.LightsStatus
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.game.memory.MemoryGame
import com.mehmtcan.brainscroll.game.memory.MemoryStatus
import com.mehmtcan.brainscroll.game.mines.MinesGame
import com.mehmtcan.brainscroll.game.mines.MinesResult
import com.mehmtcan.brainscroll.game.mines.MinesStatus
import com.mehmtcan.brainscroll.game.pipes.PipesGame
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator
import com.mehmtcan.brainscroll.game.pipes.PipesStatus
import com.mehmtcan.brainscroll.game.slide.SlideGame
import com.mehmtcan.brainscroll.game.slide.SlideStatus
import com.mehmtcan.brainscroll.game.sudoku.SudokuGame
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuStatus
import com.mehmtcan.brainscroll.game.target.Op
import com.mehmtcan.brainscroll.game.target.TargetGame
import com.mehmtcan.brainscroll.game.target.TargetGenerator
import com.mehmtcan.brainscroll.game.target.TargetStatus
import com.mehmtcan.brainscroll.game.tubes.TubesGame
import com.mehmtcan.brainscroll.game.tubes.TubesStatus
import com.mehmtcan.brainscroll.game.wordle.LetterResult
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Every guide has to lead somewhere: playing only what it says finishes the game, whatever the board. */
class GuidesTest {

    @Test
    fun minesGuideStartsInTheMiddleThenOnlyOpensSafeCellsAndFlagsMines() {
        for (seed in 1..20) {
            val game = MinesGame(seed = seed)
            var steps = 0
            while (game.state.status == MinesStatus.Playing && steps++ < 120) {
                val step = assertNotNull(MinesGuide.step(game.state), "seed $seed: a step while playing")
                val cell = step.cells.single()
                when (step.reason) {
                    GuideReason.MinesStart, GuideReason.MinesSafe -> assertTrue(game.open(cell) != MinesResult.Exploded, "seed $seed: the guide never sends you on a mine")
                    GuideReason.MinesMine -> assertTrue(game.toggleFlag(cell))
                    else -> error("seed $seed: ${step.reason}")
                }
            }
            assertEquals(MinesStatus.Won, game.state.status, "seed $seed")
        }
    }

    @Test
    fun sudokuGuideFillsTheBoardWithTheRightDigits() {
        for (seed in 1..10) {
            val game = SudokuGame(seed)
            val solution = SudokuGenerator.generate(seed).solution.toList()
            var steps = 0
            while (game.state.status == SudokuStatus.Playing && steps++ < 100) {
                val step = assertNotNull(SudokuGuide.step(game.state, solution))
                val cell = step.cells.single()
                assertTrue(step.reason == GuideReason.SudokuOnlyDigit || step.reason == GuideReason.SudokuOnlyPlace, "singles are enough: ${step.reason}")
                assertEquals(solution[cell], step.numbers.single(), "seed $seed: the digit is the right one")
                game.place(cell, step.numbers.single())
            }
            assertEquals(SudokuStatus.Won, game.state.status, "seed $seed")
            assertEquals(0, game.state.mistakes)
        }
    }

    @Test
    fun sudokuGuideAsksToTakeAWrongDigitOutFirst() {
        val game = SudokuGame(3)
        val solution = SudokuGenerator.generate(3).solution.toList()
        val cell = game.state.values.indexOf(0)
        game.place(cell, if (solution[cell] == 1) 2 else 1)
        val step = assertNotNull(SudokuGuide.step(game.state, solution))
        assertEquals(GuideReason.SudokuErase, step.reason)
        assertEquals(listOf(cell), step.cells)
    }

    @Test
    fun pipesGuideSolvesTheBoardInTheFewestTaps() {
        for (seed in 1..10) {
            val puzzle = PipesGenerator.generate(seed)
            val game = PipesGame(puzzle.start, puzzle.par)
            var taps = 0
            while (game.state.status == PipesStatus.Playing && taps < 200) {
                val step = assertNotNull(PipesGuide.step(game.state))
                repeat(step.numbers.single()) { game.tap(step.cells.single()); taps++ }
            }
            assertEquals(PipesStatus.Won, game.state.status)
            assertEquals(puzzle.par, taps, "seed $seed: the guide goes the shortest way")
        }
    }

    @Test
    fun lightsGuidePutsEveryLightOutInNoMoreTapsThanTheBoardWasMadeWith() {
        for (seed in 1..20) {
            val game = LightsGame(seed)
            var taps = 0
            while (game.state.status == LightsStatus.Playing && taps < 30) {
                game.tap(assertNotNull(LightsGuide.step(game.state)).cells.single())
                taps++
            }
            assertEquals(LightsStatus.Won, game.state.status, "seed $seed")
            assertTrue(taps <= game.taps, "seed $seed: $taps taps for a board made with ${game.taps}")
        }
    }

    @Test
    fun lightsGuideFindsAWayFromAnyBoardTheGameCanBeInAfterWrongTaps() {
        val game = LightsGame(5)
        listOf(0, 7, 12, 24, 3).forEach { game.tap(it) }
        var taps = 0
        while (game.state.status == LightsStatus.Playing && taps < 40) {
            game.tap(assertNotNull(LightsGuide.step(game.state)).cells.single())
            taps++
        }
        assertEquals(LightsStatus.Won, game.state.status)
    }

    @Test
    fun slideGuideSlidesTheTilesHomeOneStepAtATime() {
        for (seed in 1..5) {
            val game = SlideGame(seed)
            var steps = 0
            while (game.state.status == SlideStatus.Playing && steps++ < 120) {
                game.tap(assertNotNull(SlideGuide.step(game.state), "seed $seed step $steps").cells.single())
            }
            assertEquals(SlideStatus.Won, game.state.status, "seed $seed")
        }
    }

    @Test
    fun tubesGuidePoursTheTubesSorted() {
        for (seed in 1..8) {
            val game = TubesGame(TubesGame.newLayout(Random(seed)))
            var pours = 0
            while (game.state.status == TubesStatus.Playing && pours++ < 80) {
                val step = assertNotNull(TubesGuide.step(game.state))
                assertEquals(GuideReason.TubesPour, step.reason)
                game.pour(step.cells[0], step.cells[1])
            }
            assertEquals(TubesStatus.Won, game.state.status, "seed $seed")
        }
    }

    @Test
    fun tubesGuideSaysWhenTheTubesAreStuck() {
        // Two colors in three tubes of two, with no empty tube: the top colors do not match, so nothing can be poured.
        val stuck = TubesGame(listOf(listOf(0, 1), listOf(1, 0)), capacity = 2)
        assertEquals(GuideReason.TubesStuck, TubesGuide.step(stuck.state)?.reason)
    }

    @Test
    fun targetGuideCombinesNumbersUntilTheTargetIsMade() {
        for (seed in 1..20) {
            val puzzle = TargetGenerator.generate(seed)
            val game = TargetGame(puzzle.numbers, puzzle.target, puzzle.par)
            var moves = 0
            while (game.state.status == TargetStatus.Playing && moves++ < 10) {
                val step = assertNotNull(TargetGuide.step(game.state))
                val op = Op.fromSymbol(step.text.single())!!
                game.combine(step.cells[0], op, step.cells[1])
            }
            assertEquals(TargetStatus.Won, game.state.status, "seed $seed")
            assertEquals(puzzle.par, game.state.moves, "seed $seed: the fewest operations")
        }
    }

    @Test
    fun memoryGuidePointsAtAPairThenAtThePartnerOfTheOpenCard() {
        val layout = listOf(CardSymbol.Star, CardSymbol.Moon, CardSymbol.Star, CardSymbol.Moon)
        val game = MemoryGame(layout)
        val pair = assertNotNull(MemoryGuide.step(game.state))
        assertEquals(GuideReason.MemoryPair, pair.reason)
        assertEquals(layout[pair.cells[0]], layout[pair.cells[1]])

        game.flip(1)
        val partner = assertNotNull(MemoryGuide.step(game.state))
        assertEquals(GuideReason.MemoryPartner, partner.reason)
        assertEquals(listOf(3), partner.cells)

        game.flip(3)
        game.flip(0)
        game.flip(2)
        assertEquals(MemoryStatus.Won, game.state.status)
        assertNull(MemoryGuide.step(game.state))
    }

    @Test
    fun wordGuideGivesALetterThePlayerDoesNotHaveYet() {
        val rows = listOf(listOf(LetterResult.Correct, LetterResult.Absent, LetterResult.Present, LetterResult.Absent, LetterResult.Absent))
        val step = assertNotNull(WordGuide.step("CRANE", rows, WordleStatus.Playing))
        assertEquals(listOf(2), step.numbers, "the first place that is not green is the second")
        assertEquals("R", step.text)
        assertNull(WordGuide.step("CRANE", rows, WordleStatus.Won))
    }
}
