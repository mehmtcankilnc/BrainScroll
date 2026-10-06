package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The memory match page in the feed, driven through the real screen. The board is random, so the tests read the cards
 * the way a player does: turn one over, look at it (the screen reader description names the shape).
 */
@OptIn(ExperimentalTestApi::class)
class MemoryUiTest {

    private val both = listOf(GameKind.Word, GameKind.Memory)

    /** Starts the app and swipes from the first page (a word puzzle) to the second one (memory). */
    private fun ComposeUiTest.openMemory(cloud: TestCloud = TestCloud(), haptics: RecordingHaptics? = null) {
        start(cloud, haptics = haptics, games = both)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { cardCount() == 12 }
    }

    private fun ComposeUiTest.cardCount() = onAllNodesWithTag("memoryCard").fetchSemanticsNodes().size

    private fun ComposeUiTest.description(position: Int): String =
        onAllNodesWithTag("memoryCard")[position].fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()

    private fun ComposeUiTest.isDown(position: Int) = description(position).let { it.startsWith("Kapalı kart") || it.startsWith("Face-down") }

    private fun ComposeUiTest.isMatched(position: Int) = description(position).let { it.startsWith("Eşleşti") || it.startsWith("Matched") }

    /** The name of the shape of a card that is face up ("Yıldız kartı, 3. konum" gives "Yıldız"). */
    private fun ComposeUiTest.shapeOf(position: Int): String =
        description(position).substringBefore(',').removePrefix("Eşleşti: ").removePrefix("Matched: ").removeSuffix(" kartı").removeSuffix(" card")

    private fun ComposeUiTest.tap(position: Int) {
        onAllNodesWithTag("memoryCard")[position].performClick()
        waitForIdle()
    }

    /** Lets the short pause for two wrong cards pass. */
    private fun ComposeUiTest.letMismatchTurnBack() {
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
    }

    /**
     * Turns over the neighbours (0 and 1, then 2 and 3, ...) until two of them are a mistake, and returns them. A pair
     * that happens to match on the way is simply matched. (All six neighbour pairs matching has a chance of about
     * one in ten thousand.)
     */
    private fun ComposeUiTest.makeAMistake(): Pair<Int, Int> {
        for (k in 0 until 6) {
            tap(2 * k)
            tap(2 * k + 1)
            if (!isMatched(2 * k)) return (2 * k) to (2 * k + 1)
        }
        error("every neighbour pair matched, which is possible but very unlikely: run the test again")
    }

    /**
     * Plays the board to the end without a mistake, by looking: flip a card, remember its shape, and either find its
     * twin among the cards seen or turn over a new one.
     */
    private fun ComposeUiTest.solveByLooking() {
        val seen = mutableMapOf<Int, String>()
        val matched = mutableSetOf<Int>()
        var guard = 0
        while (matched.size < 12 && guard++ < 60) {
            val first = (0 until 12).first { it !in matched && it !in seen }
            tap(first)
            val shape = shapeOf(first)
            val twin = seen.entries.firstOrNull { it.key !in matched && it.value == shape }?.key
            if (twin != null) {
                tap(twin)
                matched += first; matched += twin
                continue
            }
            seen[first] = shape
            // Not seen before: turn over another new card to look for the twin.
            val second = (0 until 12).first { it !in matched && it !in seen }
            tap(second)
            val secondShape = shapeOf(second)
            if (secondShape == shape) {
                matched += first; matched += second
                seen.remove(first)
            } else {
                seen[second] = secondShape
                // A mismatch: the two cards are face up until the next tap hides them.
                val twinOfSecond = seen.entries.firstOrNull { it.key != second && it.key !in matched && it.value == secondShape }?.key
                if (twinOfSecond != null) {
                    tap(second); tap(twinOfSecond)
                    matched += second; matched += twinOfSecond
                    seen.remove(second); seen.remove(twinOfSecond)
                }
            }
        }
    }

    @Test
    fun theSecondPageIsAMemoryBoardOfTwelveFaceDownCards() = runComposeUiTest {
        openMemory()
        snap("80_memory_start")
        assertEquals(12, cardCount())
        assertTrue((0 until 12).all { isDown(it) }, "every card starts face down")
        assertTrue(hasAnyText("Hafıza eşleştirme", "Memory match"))
        assertTrue(hasAnyText("Hamle 0", "Moves 0"))
        assertTrue(hasAnyText("Hata 0/6", "Mistakes 0/6"))
    }

    @Test
    fun tappingACardTurnsItFaceUpAndNamesItsShape() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openMemory(haptics = haptics)
        haptics.kinds.clear()
        tap(5)
        mainClock.advanceTimeBy(500)
        snap("81_memory_one_card")
        assertFalse(isDown(5))
        assertTrue(description(5).let { it.contains("6. konum") || it.contains("position 6") }, description(5))
        assertEquals(listOf(HapticKind.Light), haptics.kinds, "opening a card is a light tap")
    }

    @Test
    fun twoCardsThatDoNotMatchAreAMistakeAndTurnBackByThemselves() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openMemory(haptics = haptics)
        val (a, b) = makeAMistake()
        mainClock.advanceTimeBy(400)
        snap("82_memory_mismatch")
        assertTrue(hasAnyText("Hata 1/6", "Mistakes 1/6") || hasAnyText("Hata 2/6", "Mistakes 2/6") || hasAnyText("Hata 3/6", "Mistakes 3/6") || hasAnyText("Hata 4/6", "Mistakes 4/6") || hasAnyText("Hata 5/6", "Mistakes 5/6"))
        assertTrue(HapticKind.Warning in haptics.kinds, "a mistake is a warning")
        assertFalse(isDown(a) || isDown(b), "the wrong cards stay face up for a moment")
        letMismatchTurnBack()
        assertTrue(isDown(a) && isDown(b), "and then turn back by themselves")
    }

    @Test
    fun aMatchStaysAndBuzzesMediumAndTheBoardCanBeSolved() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openMemory(haptics = haptics)
        haptics.kinds.clear()
        solveByLooking()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Tüm çiftler bulundu", "All pairs found") }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("83_memory_won")

        assertTrue((0 until 12).all { isMatched(it) }, "every card is matched")
        assertTrue(HapticKind.Medium in haptics.kinds, "a match is a medium tap")
        assertEquals(1, haptics.kinds.count { it == HapticKind.Success }, "and winning is one success")
        assertTrue(Regex("""\d+ (hamle|moves) · \d\d:\d\d""").containsMatchIn(visibleResult()), "the result says the moves and the time")
    }

    private fun ComposeUiTest.visibleResult(): String {
        val nodes = onAllNodes(hasText(" · ", substring = true)).fetchSemanticsNodes()
        return nodes.joinToString { n -> n.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }.orEmpty() }
    }

    @Test
    fun tooManyMistakesLoseAndShowTheWholeBoard() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openMemory(haptics = haptics)
        val (a, b) = makeAMistake()
        // Turn the same two wrong cards over again and again until the board is lost (at most six mistakes).
        var guard = 0
        while (!hasAnyText("Çok fazla hata", "Too many mistakes") && guard++ < 8) {
            tap(a)
            tap(b)
        }
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Çok fazla hata", "Too many mistakes") }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("84_memory_lost")

        assertTrue(hasAnyText("Hata 6/6", "Mistakes 6/6"))
        assertTrue(HapticKind.Warning in haptics.kinds)
        assertFalse(HapticKind.Success in haptics.kinds, "losing is no success")
        assertTrue((0 until 12).none { isDown(it) }, "a lost board shows all its cards")
    }

    @Test
    fun aFinishedBoardIsCountedInTheProfileAndTheTelemetryAndCanBeAFavorite() = runComposeUiTest {
        val cloud = TestCloud()
        openMemory(cloud)
        solveByLooking()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Tüm çiftler bulundu", "All pairs found") }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        onAllNodesWithTag("favoriteHeart")[0].performClick()
        waitForIdle()

        openTab("Profil", "Profile")
        waitForIdle()
        onNode(hasScrollAction()).performScrollToNode(hasText("Hafıza eşleştirme").or(hasText("Memory match")))
        waitForIdle()
        snap("85_profile_memory")
        assertTrue(hasAnyText("Hafıza eşleştirme", "Memory match"), "the memory section")
        assertTrue(hasAnyText("En az hamle", "Fewest moves"))
        onNode(hasScrollAction()).performScrollToNode(hasText("hamle ·", substring = true).or(hasText("moves ·", substring = true)))
        assertTrue(hasAnyText("hamle ·", "moves ·"), "the favorite row names the game and its result")

        waitUntil(timeoutMillis = 15_000) { cloud.telemetry.events.any { it.event == TelemetryEvent.EndlessFinished } }
        val event = cloud.telemetry.events.first { it.event == TelemetryEvent.EndlessFinished }
        assertEquals("memory", event.props["game"])
        assertEquals("WON", event.props["outcome"])
        assertTrue(event.props["moves"]!!.toInt() in 6..11)
    }

    @Test
    fun theWordPageBeforeAndAfterStillWorksInTheMixedFeed() = runComposeUiTest {
        start(TestCloud(), games = both)
        // Page 0 is a word puzzle with a keyboard.
        assertTrue(hasAnyText("ENTER", "GİR"))
        key('C')
        // Leave it unfinished (uses the skip): the memory board is next, then a word puzzle again after it.
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { cardCount() == 12 }
        solveByLooking()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Tüm çiftler bulundu", "All pairs found") }
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        assertTrue(hasAnyText("ENTER", "GİR"), "after the memory board comes a word puzzle again")
    }
}
