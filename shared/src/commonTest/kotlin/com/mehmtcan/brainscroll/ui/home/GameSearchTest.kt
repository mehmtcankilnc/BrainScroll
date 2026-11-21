package com.mehmtcan.brainscroll.ui.home

import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameSearchTest {
    private fun found(query: String, names: Map<GameKind, String> = emptyMap()) =
        (GameKind.entries - GameKind.Line).filter { gameMatchesQuery(it, names[it] ?: it.name, query) }.toSet()

    @Test
    fun everyGameHasKeywords() {
        for (game in GameKind.entries) assertTrue(GameKeywords[game].orEmpty().isNotBlank(), "$game has no keywords")
    }

    @Test
    fun anEmptyQueryFindsEveryGame() {
        assertEquals((GameKind.entries - GameKind.Line).toSet(), found(""))
        assertEquals((GameKind.entries - GameKind.Line).toSet(), found("   "))
    }

    @Test
    fun colourFindsTheGamesAboutColoursEvenWhenTheNameHasNone() {
        val colours = found("renk")
        for (game in listOf(GameKind.Tubes, GameKind.ColorFlow, GameKind.CodeBreaker)) assertTrue(game in colours, "$game")
        // Games that have no colour in them are not found, even when something on the board is painted.
        for (game in listOf(GameKind.Queens, GameKind.Nonogram, GameKind.BallSlide)) assertFalse(game in colours, "$game")
        assertFalse(GameKind.Sudoku in colours)
        assertFalse(GameKind.Hanoi in colours)
        // The English word finds the same ones.
        for (game in listOf(GameKind.Tubes, GameKind.ColorFlow, GameKind.CodeBreaker)) assertTrue(game in found("colour"), "$game")
        assertTrue(GameKind.Tubes in found("color"))
    }

    @Test
    fun numberFindsEveryGameThatIsDoneWithNumbersIncludingTheEquations() {
        val numbers = found("sayı")
        for (game in listOf(GameKind.Sudoku, GameKind.Slide, GameKind.Target, GameKind.Jigsaw, GameKind.AxisSum, GameKind.Rectangles, GameKind.Matchsticks, GameKind.FruitMath)) {
            assertTrue(game in numbers, "$game")
        }
        assertFalse(GameKind.Hanoi in numbers)
        assertTrue(GameKind.Matchsticks in found("matematik"))
        assertTrue(GameKind.FruitMath in found("matematik"))
    }

    @Test
    fun theNameFindsItsGameWhereverTheLettersAre() {
        val names = mapOf(GameKind.Tubes to "Renk tüpleri", GameKind.Hanoi to "Hanoi Kulesi")
        assertTrue(GameKind.Hanoi in found("kule", names))
        assertTrue(GameKind.Hanoi in found("ule", names), "a part of the name")
        assertTrue(GameKind.Tubes in found("tup", names), "plain letters find the Turkish ones")
    }

    @Test
    fun turkishLettersAndCapitalsDoNotGetInTheWay() {
        assertTrue(GameKind.Lights in found("ISIK"))
        assertTrue(GameKind.Lights in found("ışık"))
        assertTrue(GameKind.Lights in found("isik"))
        assertTrue(GameKind.Untangle in found("DÜĞÜM"))
        assertTrue(GameKind.Untangle in found("dugum"))
        assertEquals("cigsi", foldForSearch("ÇİĞŞI"))
    }

    @Test
    fun everyWordOfTheQueryHasToFit() {
        assertTrue(GameKind.Matchsticks in found("kibrit denklem"))
        assertTrue(GameKind.FruitMath in found("meyve denklem"))
        assertFalse(GameKind.Matchsticks in found("kibrit meyve"))
        assertTrue(found("zzzz").isEmpty())
    }

    @Test
    fun aKeywordIsFoundFromItsStartNotFromItsMiddle() {
        assertTrue(GameKind.Mines in found("mayı"))
        assertFalse(GameKind.Mines in found("ayın"), "the middle of a keyword is not a match")
    }
}
