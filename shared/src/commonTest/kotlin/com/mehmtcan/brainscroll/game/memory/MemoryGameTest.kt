package com.mehmtcan.brainscroll.game.memory

import com.mehmtcan.brainscroll.game.memory.CardSymbol.Circle
import com.mehmtcan.brainscroll.game.memory.CardSymbol.Heart
import com.mehmtcan.brainscroll.game.memory.CardSymbol.Star
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemoryGameTest {

    /** Positions: 0 and 3 are circles, 1 and 4 stars, 2 and 5 hearts. */
    private val layout = listOf(Circle, Star, Heart, Circle, Star, Heart)

    private fun game(maxMistakes: Int = 3) = MemoryGame(layout, maxMistakes)

    @Test
    fun aNewBoardIsAllFaceDown() {
        val state = game().state
        assertTrue(state.faceUp.isEmpty() && state.matched.isEmpty())
        assertEquals(0, state.moves)
        assertEquals(0, state.mistakes)
        assertEquals(MemoryStatus.Playing, state.status)
    }

    @Test
    fun theFirstCardOfAPairJustOpens() {
        val g = game()
        assertEquals(FlipResult.Opened, g.flip(0))
        assertEquals(setOf(0), g.state.faceUp)
        assertEquals(0, g.state.moves, "a move is two cards")
    }

    @Test
    fun twoEqualCardsStayAndCountAsAMove() {
        val g = game()
        g.flip(0)
        assertEquals(FlipResult.Match, g.flip(3))
        val state = g.state
        assertEquals(setOf(0, 3), state.matched)
        assertTrue(state.faceUp.isEmpty())
        assertEquals(1, state.moves)
        assertEquals(0, state.mistakes)
    }

    @Test
    fun twoDifferentCardsAreAMistakeAndStayFaceUpToBeLookedAt() {
        val g = game()
        g.flip(0)
        assertEquals(FlipResult.Mismatch, g.flip(1))
        val state = g.state
        assertEquals(setOf(0, 1), state.faceUp)
        assertTrue(state.mismatchShown)
        assertEquals(1, state.mistakes)
        assertEquals(1, state.moves)
        assertTrue(state.matched.isEmpty())
    }

    @Test
    fun theNextTapHidesTheMismatchAndOpensTheNewCard() {
        val g = game()
        g.flip(0)
        g.flip(1) // mismatch
        assertEquals(FlipResult.Opened, g.flip(2))
        assertEquals(setOf(2), g.state.faceUp, "the two wrong cards are hidden, the new one is up")
        assertFalse(g.state.mismatchShown)
    }

    @Test
    fun tappingOneOfTheTwoWrongCardsAgainStartsANewMove() {
        val g = game()
        g.flip(0)
        g.flip(1) // mismatch: 0 and 1 are face up
        assertEquals(FlipResult.Opened, g.flip(0), "0 was hidden by this tap, so it is opened again as the first card")
        assertEquals(setOf(0), g.state.faceUp)
        assertEquals(FlipResult.Match, g.flip(3))
    }

    @Test
    fun closeMismatchHidesThePairAndDoesNothingWithoutOne() {
        val g = game()
        g.closeMismatch() // nothing to hide
        g.flip(0)
        g.closeMismatch() // one card up is not a mismatch: it stays
        assertEquals(setOf(0), g.state.faceUp)
        g.flip(1)
        g.closeMismatch()
        assertTrue(g.state.faceUp.isEmpty())
        assertEquals(1, g.state.mistakes, "hiding does not undo the mistake")
    }

    @Test
    fun tappingAnOpenOrMatchedCardOrOutsideTheBoardDoesNothing() {
        val g = game()
        g.flip(0)
        assertEquals(FlipResult.Ignored, g.flip(0), "already face up")
        g.flip(3) // match
        assertEquals(FlipResult.Ignored, g.flip(0), "already matched")
        assertEquals(FlipResult.Ignored, g.flip(-1))
        assertEquals(FlipResult.Ignored, g.flip(6))
        assertEquals(1, g.state.moves)
    }

    @Test
    fun findingEveryPairWins() {
        val g = game()
        g.flip(0); g.flip(3)
        g.flip(1); g.flip(4)
        assertFalse(g.isFinished)
        g.flip(2); g.flip(5)
        assertTrue(g.isFinished)
        assertEquals(MemoryStatus.Won, g.state.status)
        assertEquals(3, g.state.moves)
        assertEquals(0, g.state.mistakes)
    }

    @Test
    fun theLastAllowedMistakeLoses() {
        val g = game(maxMistakes = 3)
        repeat(2) { g.flip(0); g.flip(1) }
        assertEquals(MemoryStatus.Playing, g.state.status)
        assertEquals(2, g.state.mistakes)
        g.flip(0)
        g.flip(1)
        assertEquals(3, g.state.mistakes)
        assertEquals(MemoryStatus.Lost, g.state.status)
        assertTrue(g.isFinished)
    }

    @Test
    fun nothingWorksAfterTheGameIsOver() {
        val g = game(maxMistakes = 1)
        g.flip(0); g.flip(1)
        assertTrue(g.isFinished)
        assertEquals(FlipResult.Ignored, g.flip(2))
        assertEquals(1, g.state.moves)
    }

    @Test
    fun performIsTheSameAsFlipping() {
        val g = game()
        g.perform(Flip(0))
        assertEquals(setOf(0), g.state.faceUp)
    }

    @Test
    fun aBoardNeedsEverySymbolExactlyTwice() {
        assertFailsWith<IllegalArgumentException> { MemoryGame(listOf(Circle, Star, Heart)) }
        assertFailsWith<IllegalArgumentException> { MemoryGame(listOf(Circle, Circle, Circle, Circle)) }
        assertFailsWith<IllegalArgumentException> { MemoryGame(emptyList()) }
    }

    @Test
    fun aNewLayoutHasSixDifferentSymbolsTwiceEach() {
        val layout = MemoryGame.newLayout(Random(5))
        assertEquals(12, layout.size)
        val counts = layout.groupingBy { it }.eachCount()
        assertEquals(6, counts.size)
        assertTrue(counts.values.all { it == 2 })
    }

    @Test
    fun layoutsDifferBetweenBoardsAndRepeatWithTheSameSeed() {
        assertEquals(MemoryGame.newLayout(Random(1)), MemoryGame.newLayout(Random(1)))
        val many = (1..30).map { MemoryGame.newLayout(Random(it)) }.toSet()
        assertTrue(many.size > 20, "boards are shuffled")
        val used = (1..30).flatMap { MemoryGame.newLayout(Random(it)) }.toSet()
        assertEquals(CardSymbol.entries.toSet(), used, "all eight symbols are used over time")
    }

    @Test
    fun aPerfectPlayerNeedsNoMistakes() {
        val layout = MemoryGame.newLayout(Random(3))
        val g = MemoryGame(layout)
        val bySymbol = layout.indices.groupBy { layout[it] }.values
        for (pair in bySymbol) { g.flip(pair[0]); g.flip(pair[1]) }
        assertEquals(MemoryStatus.Won, g.state.status)
        assertEquals(6, g.state.moves)
        assertEquals(0, g.state.mistakes)
    }
}
