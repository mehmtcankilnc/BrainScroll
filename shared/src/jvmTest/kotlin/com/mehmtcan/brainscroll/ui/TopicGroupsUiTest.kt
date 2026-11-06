package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsData
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.topicgroups.TopicGroupsWord
import com.mehmtcan.brainscroll.ui.topicgroups.fitFontPx
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The word groups page in the feed, driven through the real screen. The board is random, but words are numbered by group (the
 * group of word `w` is `w / 4`), and every tile carries its number for a test, so the tests know the answer.
 */
@OptIn(ExperimentalTestApi::class)
class TopicGroupsUiTest {

    private val games = listOf(GameKind.Word, GameKind.TopicGroups)

    /** The smallest text size that is still easy to read on a tile. */
    private val MIN_PX = 10f

    private fun ComposeUiTest.openTopics(haptics: RecordingHaptics? = null, reduceMotion: Boolean? = null) {
        start(haptics = haptics, reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("topicTile").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.nodesOf(tag: String): List<SemanticsNodeInteraction> =
        onAllNodesWithTag(tag).let { all -> (0 until all.fetchSemanticsNodes().size).map { all[it] } }

    private fun SemanticsNodeInteraction.number() = fetchSemanticsNode().config.getOrNull(TopicGroupsWord)!!

    private fun SemanticsNodeInteraction.text() = fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()

    private fun SemanticsNodeInteraction.picked() = fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected) == true

    private fun ComposeUiTest.tile(word: Int) = nodesOf("topicTile").first { it.number() == word }

    private fun ComposeUiTest.tilesLeft() = onAllNodesWithTag("topicTile").fetchSemanticsNodes().size

    private fun ComposeUiTest.bannerCount() = onAllNodesWithTag("topicBanner").fetchSemanticsNodes().size

    private fun ComposeUiTest.tap(vararg words: Int) {
        for (w in words) {
            tile(w).performClick()
            mainClock.advanceTimeBy(60)
            waitForIdle()
        }
    }

    private fun ComposeUiTest.settle() {
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
    }

    private fun group(g: Int) = (g * 4 until g * 4 + 4).toList().toIntArray()

    private fun ComposeUiTest.won() = hasAnyText("Tüm gruplar bulundu!", "All groups found!")

    @Test
    fun theBoardShowsSixteenWordsTheFoundCounterTheMistakesAndTheTitle() = runComposeUiTest {
        openTopics()
        snap("330_topicgroups_start")
        assertEquals(16, tilesLeft())
        assertEquals((0 until 16).toList(), nodesOf("topicTile").map { it.number() }.sorted())
        assertTrue(hasAnyText("Tema Grupları", "Topic Groups"))
        assertTrue(hasAnyText("Bulunan 0/4", "Found 0/4"))
        assertTrue(hasAnyText("Hata 0", "Mistakes 0"))
        assertEquals(0, bannerCount())
        assertTrue(onAllNodesWithTag("languageFlag").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun tappingAWordPicksItAndTappingItAgainPutsItBack() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTopics(haptics)
        haptics.kinds.clear()
        tap(5)
        assertTrue(tile(5).picked())
        assertTrue(tile(5).text().contains("seçili") || tile(5).text().contains("picked"))
        snap("331_topicgroups_picked")
        tap(5)
        assertTrue(!tile(5).picked())
        assertEquals(listOf(HapticKind.Light, HapticKind.Light), haptics.kinds)
    }

    @Test
    fun aRightGroupIsFoundWithItsTopicAndTheOtherWordsMoveDown() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTopics(haptics)
        haptics.kinds.clear()
        tap(*group(2))
        settle()
        snap("332_topicgroups_found")
        assertEquals(12, tilesLeft())
        assertEquals(4, onAllNodesWithTag("topicFoundTile").fetchSemanticsNodes().size)
        assertEquals(1, bannerCount())
        assertTrue(hasAnyText("Bulunan 1/4", "Found 1/4"))
        assertTrue(hasAnyText("Hata 0", "Mistakes 0"))
        assertEquals(HapticKind.Medium, haptics.kinds.last())
        // The found words are in the first row, above every other word.
        val firstRow = nodesOf("topicFoundTile").maxOf { it.fetchSemanticsNode().boundsInRoot.top }
        val rest = nodesOf("topicTile").minOf { it.fetchSemanticsNode().boundsInRoot.top }
        assertTrue(firstRow < rest, "found row at $firstRow, the others from $rest")
    }

    @Test
    fun aWrongPickIsAMistakeWithABuzzAndStaysPicked() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTopics(haptics)
        haptics.kinds.clear()
        tap(0, 1, 2, 4)
        settle()
        snap("333_topicgroups_wrong")
        assertTrue(hasAnyText("Hata 1", "Mistakes 1"))
        assertEquals(16, tilesLeft())
        assertEquals(HapticKind.Warning, haptics.kinds.last())
        assertTrue(listOf(0, 1, 2, 4).all { tile(it).picked() })
        // The same four a second time buzz but are no new mistake.
        tap(4, 4)
        assertTrue(hasAnyText("Hata 1", "Mistakes 1"))
        assertEquals(HapticKind.Warning, haptics.kinds.last())
    }

    @Test
    fun swappingOneWordOfAWrongPickFindsTheGroup() = runComposeUiTest {
        openTopics()
        tap(0, 1, 2, 4)
        tap(4, 3)
        settle()
        assertEquals(12, tilesLeft())
        assertTrue(hasAnyText("Hata 1", "Mistakes 1"))
    }

    @Test
    fun clearPutsEveryPickedWordBackAndIsDimWhenNothingIsPicked() = runComposeUiTest {
        openTopics()
        onNodeWithTag("topicClear").assertIsNotEnabled()
        tap(1, 6)
        assertTrue(tile(1).picked() && tile(6).picked())
        onNodeWithTag("topicClear").performClick()
        waitForIdle()
        assertTrue(!tile(1).picked() && !tile(6).picked())
        onNodeWithTag("topicClear").assertIsNotEnabled()
    }

    private fun ComposeUiTest.order() = nodesOf("topicTile").sortedWith(compareBy({ it.fetchSemanticsNode().boundsInRoot.top }, { it.fetchSemanticsNode().boundsInRoot.left })).map { it.number() }

    @Test
    fun shuffleMixesTheWordsAndKeepsThePick() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTopics(haptics)
        tap(7)
        val before = order()
        haptics.kinds.clear()
        onNodeWithTag("topicShuffle").performClick()
        settle()
        val after = order()
        assertNotEquals(before, after)
        assertEquals(before.sorted(), after.sorted())
        assertTrue(tile(7).picked())
        assertEquals(listOf(HapticKind.Light), haptics.kinds)
    }

    @Test
    fun findingEveryGroupWinsAndShowsTheTimeAndMistakes() = runComposeUiTest {
        openTopics()
        tap(0, 1, 2, 4)
        tap(4, 3)
        tap(*group(1))
        tap(*group(2))
        settle()
        snap("334_topicgroups_solved")
        assertTrue(won())
        assertEquals(4, bannerCount())
        assertEquals(0, tilesLeft())
        assertTrue(hasAnyText("1 hata", "1 mistakes"))
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun theLastGroupComesByItselfAfterTheThirdIsFound() = runComposeUiTest {
        openTopics()
        tap(*group(0))
        tap(*group(1))
        mainClock.autoAdvance = false
        tile(8).performClick()
        tile(9).performClick()
        tile(10).performClick()
        tile(11).performClick()
        mainClock.advanceTimeBy(100)
        // The fourth group is not on show yet: the one before it is still arriving.
        assertTrue(!won())
        mainClock.advanceTimeBy(2_000)
        assertEquals(4, bannerCount())
        assertTrue(won())
    }

    @Test
    fun theWinBuzzesOnlyWhenTheLastGroupIsOnShow() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openTopics(haptics)
        tap(*group(0))
        tap(*group(1))
        tap(8, 9, 10)
        haptics.kinds.clear()
        mainClock.autoAdvance = false
        tile(11).performClick()
        mainClock.advanceTimeBy(400)
        assertTrue(HapticKind.Success !in haptics.kinds, "no success buzz while the last group is still coming: ${haptics.kinds}")
        assertTrue(!won())
        mainClock.advanceTimeBy(1_500)
        assertTrue(HapticKind.Success in haptics.kinds, "the success buzz comes once it is shown: ${haptics.kinds}")
        assertTrue(won())
    }

    @Test
    fun withReducedMotionTheWinShowsQuickly() = runComposeUiTest {
        openTopics(reduceMotion = true)
        tap(*group(0))
        tap(*group(1))
        tap(8, 9, 10)
        mainClock.autoAdvance = false
        tile(11).performClick()
        mainClock.advanceTimeBy(300)
        assertTrue(won())
        assertEquals(4, bannerCount())
    }

    /**
     * A found group is first drawn where its words were and then slides up: the first frame after the tap must not show the words
     * at the top already (the end of the move), and the topic must not be there before the words have arrived.
     */
    @Test
    fun aFoundGroupStartsWhereItsWordsWereAndTheTopicWaitsForThem() = runComposeUiTest {
        openTopics()
        // The group whose words lie lowest on the board has the longest way to go.
        val lowest = (0 until 4).maxBy { g -> group(g).sumOf { w -> tile(w).fetchSemanticsNode().boundsInRoot.top.toDouble() } }
        val words = group(lowest)
        val startTops = words.associateWith { tile(it).fetchSemanticsNode().boundsInRoot.top }
        for (w in words.dropLast(1)) tap(w)
        mainClock.autoAdvance = false
        tile(words.last()).performClick()
        mainClock.advanceTimeBy(16)
        val farthest = words.maxBy { startTops.getValue(it) }
        val now = nodesOf("topicFoundTile").first { it.number() == farthest }.fetchSemanticsNode().boundsInRoot.top
        val start = startTops.getValue(farthest)
        assertEquals(0, bannerCount(), "no topic yet")
        mainClock.advanceTimeBy(200)
        assertEquals(0, bannerCount(), "no topic before the words have arrived")
        mainClock.advanceTimeBy(700)
        assertEquals(1, bannerCount())
        val arrived = nodesOf("topicFoundTile").first { it.number() == farthest }.fetchSemanticsNode().boundsInRoot.top
        val topRow = nodesOf("topicFoundTile").minOf { it.fetchSemanticsNode().boundsInRoot.top }
        assertTrue(start - arrived > 50f, "the word had a way to go: from $start to $arrived")
        assertTrue(kotlin.math.abs(arrived - topRow) < 2f, "the word reached the top row: $arrived against $topRow")
        assertTrue(now > arrived + (start - arrived) * 0.5f, "after one frame the word is still near where it was: start $start, now $now, top row $arrived")
    }

    @Test
    fun aScreenReaderHearsEachWordAndTheTopicOfAFoundGroupOnce() = runComposeUiTest {
        openTopics()
        val words = nodesOf("topicTile").map { it.text() }
        assertTrue(words.all { it.isNotBlank() })
        tap(*group(1))
        settle()
        val banner = nodesOf("topicBanner").single().text()
        assertTrue(banner.contains(":"), banner)
        // The tiles of the found group are silent: the banner says everything.
        assertTrue(nodesOf("topicFoundTile").all { it.text().isEmpty() })
    }

    @Test
    fun theLanguageFlagMakesTheBoardAgainInTheOtherLanguage() = runComposeUiTest {
        openTopics()
        fun languageOfBoard(): Language {
            val words = nodesOf("topicTile").map { it.text().substringBefore(",").trim() }
            return Language.entries.first { l -> words.all { w -> TopicGroupsData.topics(l).any { w in it.words } } }
        }
        val before = languageOfBoard()
        val other = Language.entries.first { it != before }
        // The flag of the page on screen (the page above has one too).
        nodesOf("languageFlag").maxBy { it.fetchSemanticsNode().boundsInRoot.top }.performClick()
        settle()
        snap("335_topicgroups_language")
        assertEquals(other, languageOfBoard())
        assertEquals(16, tilesLeft())
    }

    @Test
    fun theGuidePointsAtTheFourWordsOfAGroup() = runComposeUiTest {
        openTopics()
        onAllNodesWithTag("guideButton")[0].performClick()
        waitUntil(timeoutMillis = 60_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        mainClock.advanceTimeBy(500)
        snap("336_topicgroups_guide")
        val marks = onAllNodesWithTag("guideMark").fetchSemanticsNodes()
        assertEquals(4, marks.size)
    }

    /** The pictures of the How to Play card at chosen times of their loops, in both languages, to look at. */
    @Test
    fun theHowToPicturesAtChosenTimes() = runComposeUiTest {
        val language = androidx.compose.runtime.mutableStateOf(Language.EN)
        val step = androidx.compose.runtime.mutableIntStateOf(0)
        val time = androidx.compose.runtime.mutableFloatStateOf(0f)
        setContent {
            com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme {
                androidx.compose.runtime.CompositionLocalProvider(com.mehmtcan.brainscroll.ui.howto.LocalHowToWordLanguage provides language.value) {
                    androidx.compose.foundation.layout.Box(
                        androidx.compose.ui.Modifier.size(320.dp).background(com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme.colors.bgPage),
                    ) { com.mehmtcan.brainscroll.ui.howto.TopicGroupsDemo(step.intValue, time.floatValue) }
                }
            }
        }
        val frames = listOf(0 to listOf(0f, 0.2f, 0.45f, 0.8f), 1 to listOf(0.05f, 0.3f, 0.5f, 0.7f), 2 to listOf(0.05f, 0.3f, 0.45f, 0.8f))
        for (l in Language.entries) for ((s, times) in frames) {
            language.value = l
            step.intValue = s
            for (t in times) {
                time.floatValue = t
                waitForIdle()
                snap("demo_topicgroups_${l.name.lowercase()}_${s}_${(t * 100).toInt()}")
            }
        }
    }

    /** Every word fits its tile and every topic line fits its bar on the narrowest phone, at a size that can still be read. */
    @Test
    fun everyWordAndTopicFitsItsPlaceAtAReadableSize() = runComposeUiTest {
        lateinit var measurer: androidx.compose.ui.text.TextMeasurer
        val density = androidx.compose.ui.unit.Density(1f, 1f)
        setContent {
            com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme {
                measurer = androidx.compose.ui.text.rememberTextMeasurer()
                val type = com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme.typography
                // The narrowest phone the app supports, 360 dp wide: a board of 328 dp has four tiles of 76 dp (66 dp inside), a bar of 328 dp.
                val tileInner = 76f - 10f
                val barInner = 328f - 16f
                val tileHeight = 76f * 0.92f
                val bold = type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                val medium = type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                val problems = mutableListOf<String>()
                for (language in Language.entries) for (topic in TopicGroupsData.topics(language)) {
                    for (word in topic.words) {
                        val px = measurer.fitFontPx(word, bold, density, tileInner, tileHeight * 0.27f)
                        val width = measurer.measure(word, bold.copy(fontSize = androidx.compose.ui.unit.TextUnit(px, androidx.compose.ui.unit.TextUnitType.Sp)), maxLines = 1, softWrap = false).size.width
                        if (width > tileInner + 0.5f) problems += "$language $word is $width wide in a tile of $tileInner"
                        if (px < MIN_PX) problems += "$language $word is only ${px}px"
                    }
                    // The longest four words of the topic make the longest line of a bar.
                    val line = topic.words.sortedByDescending { it.length }.take(4).joinToString(", ")
                    val px = measurer.fitFontPx(line, medium, density, barInner, tileHeight * 0.2f)
                    if (px < MIN_PX) problems += "$language ${topic.title}: the line of words is only ${px}px"
                    val title = language.uppercase(topic.title)
                    val titlePx = measurer.fitFontPx(title, bold, density, barInner, tileHeight * 0.27f)
                    if (titlePx < MIN_PX) problems += "$language $title is only ${titlePx}px"
                }
                check(problems.isEmpty()) { problems.joinToString(" | ") }
            }
        }
        waitForIdle()
    }

    /** The topic of a Turkish group is written with Turkish capitals: İ and I are not the same letter. */
    @Test
    fun turkishTopicsShowTurkishCapitals() {
        assertEquals("GİYSİLER", Language.TR.uppercase("Giysiler"))
        assertEquals("İÇECEKLER", Language.TR.uppercase("İçecekler"))
        assertEquals("TÜRKİYE'NİN ŞEHİRLERİ", Language.TR.uppercase("Türkiye'nin şehirleri"))
        assertEquals("KIRTASİYE", Language.TR.uppercase("Kırtasiye"))
    }

    @Test
    fun theThumbnailOfTheGame() = runComposeUiTest {
        setContent {
            com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.size(220.dp).background(com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme.colors.bgSurface)) {
                    com.mehmtcan.brainscroll.ui.home.GameThumbnail(GameKind.TopicGroups, Language.TR, androidx.compose.ui.Modifier.size(200.dp))
                }
            }
        }
        waitForIdle()
        snap("337_topicgroups_thumbnail")
    }

    /** The How to Play card in the feed, step by step, in both languages. */
    @Test
    fun theHowToCardInBothLanguages() = runComposeUiTest {
        openTopics()
        for (round in 0 until 2) {
            val name = if (round == 0) "tr" else "en"
            if (round == 1) {
                nodesOf("languageFlag").maxBy { it.fetchSemanticsNode().boundsInRoot.top }.performClick()
                settle()
            }
            nodesOf("howToButton").maxBy { it.fetchSemanticsNode().boundsInRoot.top.coerceAtLeast(-1f) }.performClick()
            waitForIdle()
            for (step in 0 until 3) {
                mainClock.advanceTimeBy(1_000)
                waitForIdle()
                snap("338_topicgroups_card_${name}_${step}a")
                mainClock.advanceTimeBy(1_500)
                waitForIdle()
                snap("338_topicgroups_card_${name}_${step}b")
                if (step < 2) {
                    // The main button says Next here.
                    onNodeWithTag("howToOk").performClick()
                    waitForIdle()
                }
            }
            // On the last step the same button says OK and closes the card.
            onNodeWithTag("howToOk").performClick()
            waitForIdle()
        }
    }
}
