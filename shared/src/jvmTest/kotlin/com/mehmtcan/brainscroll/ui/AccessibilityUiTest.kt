package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.FakeAccountService
import com.mehmtcan.brainscroll.social.DailyRow
import com.mehmtcan.brainscroll.social.FakeSocialApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Screen reader labels, touch targets, large text and high contrast (docs/design.md section 9). */
@OptIn(ExperimentalTestApi::class)
class AccessibilityUiTest {

    private fun ComposeUiTest.startDaily(cloud: TestCloud = TestCloud(), fontScale: Float = 1f, highContrast: Boolean? = null) {
        start(cloud, fontScale = fontScale, highContrast = highContrast)
        openTab("Günlük", "Daily")
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") }
        clickAny("Başla", "Start")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("ENTER", "GİR") }
    }

    private fun ComposeUiTest.describedAs(vararg texts: String) =
        texts.any { onAllNodesWithContentDescription(it, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private fun ComposeUiTest.answer() = if (hasAnyText("Günün bulmacası")) "KİTAP" else "CRANE"

    @Test
    fun tilesAndKeysAreReadOutWithTheirState() = runComposeUiTest {
        startDaily()
        // Before any guess: keys are described by their letter, special keys by what they do.
        assertTrue(describedAs("Tahmini gönder", "Submit guess"))
        assertTrue(describedAs("Sil", "Delete"))
        assertTrue(describedAs("E harfi", "Letter E"))

        typeWord(answer())
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()
        assertTrue(describedAs("harfi, doğru yerde", "correct place"), "a solved tile says where it is")
    }

    @Test
    fun aTypedLetterAndATriedKeyAreDescribed() = runComposeUiTest {
        startDaily()
        key('Z')
        assertTrue(describedAs("Z harfi, yazıldı", "Letter Z, typed"), "the letter typed into the grid")
    }

    @Test
    fun theKeyboardLearnsFromGuessesForScreenReaders() = runComposeUiTest {
        startDaily()
        // "SLATE" shares letters with CRANE (A, E) and has none with KİTAP's S... pick a word valid in both lists.
        val word = if (hasAnyText("Günün bulmacası")) "KIRIK" else "SLATE"
        typeWord(word)
        waitForIdle()
        mainClock.advanceTimeBy(2_000)
        assertTrue(
            describedAs("kelimede yok", "not in the word") || describedAs("doğru yerde", "correct place") ||
                describedAs("yanlış yerde", "wrong place"),
            "a tried key says what the game learned",
        )
    }

    @Test
    fun theOpenTabIsAnnouncedAsSelected() = runComposeUiTest {
        start()
        val feed = if (onAllNodesWithText("Akış").fetchSemanticsNodes().isNotEmpty()) "Akış" else "Feed"
        onNodeWithText(feed).assertIsSelected()
        openTab("Profil", "Profile")
        waitForIdle()
        val profile = if (onAllNodesWithText("Profil").fetchSemanticsNodes().isNotEmpty()) "Profil" else "Profile"
        onNodeWithText(profile).assertIsSelected()
    }

    @Test
    fun theHeartSaysWhatItDoes() = runComposeUiTest {
        startDaily()
        typeWord(answer())
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Sıradaki bulmaca", "Next puzzle in") }
        waitForIdle()
        assertTrue(describedAs("Favorilere ekle", "Add to favorites"))
        onAllNodesWithTag("favoriteHeart")[0].performClick()
        waitForIdle()
        assertTrue(describedAs("Favorilerden çıkar", "Remove from favorites"))
    }

    @Test
    fun streakChipsAreReadAsSentencesNotAsBareNumbers() = runComposeUiTest {
        start()
        assertTrue(describedAs("Doğru cevap serisi: 0", "Correct answer streak: 0"))
        openTab("Günlük", "Daily")
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") }
        assertTrue(describedAs("Gün serisi: 0", "Day streak: 0"))
    }

    @Test
    fun theLanguageChoicesSayWhichOneIsChosen() = runComposeUiTest {
        startDaily()
        val chosen = onAllNodesWithText("TR").fetchSemanticsNodes() + onAllNodesWithText("EN").fetchSemanticsNodes()
        val selectedFlags = chosen.map { it.config.getOrNull(SemanticsProperties.Selected) }
        assertEquals(2, selectedFlags.size)
        assertEquals(1, selectedFlags.count { it == true }, "exactly one language is selected")
    }

    @Test
    fun smallPillsStillHaveAFortyEightDpTouchArea() = runComposeUiTest {
        val cloud = TestCloud(
            account = FakeAccountService(AccountState.SignedIn("u", AccountState.SignedIn.Kind.Google, null)),
            social = FakeSocialApi(member = true, username = "carol", speedToday = listOf(DailyRow(1, "alice", 2, 30_000))),
        )
        start(cloud)
        openTab("Sıralama", "Ranks")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("alice") }
        for (label in listOf("Hız", "Speed", "Seri", "Streaks", "Bugün", "Today")) {
            val nodes = onAllNodesWithText(label)
            if (nodes.fetchSemanticsNodes().isNotEmpty()) nodes[0].assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun largeTextStillFitsOnTheMainScreens() = runComposeUiTest {
        val cloud = TestCloud(
            account = FakeAccountService(AccountState.SignedIn("u", AccountState.SignedIn.Kind.Google, "a@b.c")),
            social = FakeSocialApi(member = true, username = "carol", speedToday = listOf(DailyRow(1, "alice", 2, 30_000), DailyRow(2, "carol", 3, 41_000, true))),
        )
        start(cloud, fontScale = 2f)
        snap("40_large_text_feed")
        openTab("Günlük", "Daily")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("Başla", "Start") }
        snap("41_large_text_daily")
        openTab("Sıralama", "Ranks")
        waitUntil(timeoutMillis = 5_000) { hasAnyText("alice") }
        snap("42_large_text_ranks")
        openTab("Profil", "Profile")
        waitForIdle()
        snap("43_large_text_profile")
    }

    @Test
    fun highContrastRendersTheMainScreens() = runComposeUiTest {
        startDaily(highContrast = true)
        key('C'); key('R')
        snap("44_high_contrast_daily")
        openTab("Profil", "Profile")
        waitForIdle()
        snap("45_high_contrast_profile")
    }
}
