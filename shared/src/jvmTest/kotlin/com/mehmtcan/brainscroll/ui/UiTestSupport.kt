package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.App
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.FakeAccountMergeApi
import com.mehmtcan.brainscroll.account.FakeAccountService
import com.mehmtcan.brainscroll.cloud.CloudServices
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.daily.FakeDailyApi
import com.mehmtcan.brainscroll.cloud.CloudServicesFactory
import com.mehmtcan.brainscroll.social.FakeSocialApi
import com.mehmtcan.brainscroll.sync.FakeCloud
import com.mehmtcan.brainscroll.telemetry.FakeTelemetryApi
import com.mehmtcan.brainscroll.ui.haptics.Haptics
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import java.io.File
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/** Helpers shared by the headless UI tests. They drive the real app at phone size. */

private fun shot(name: String, image: ImageBitmap) {
    val dir = File("build/screenshots").also { it.mkdirs() }
    val bytes = Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes
    File(dir, "$name.png").writeBytes(bytes)
}

/** Saves a screenshot to `shared/build/screenshots/<name>.png`. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.snap(name: String) = shot(name, onRoot().captureToImage())

/** The cloud and the account of a test run. The tests keep a reference to look at what the app did with them. */
internal class TestCloud(
    val account: FakeAccountService = FakeAccountService(),
    val api: FakeCloud = FakeCloud(),
    val daily: FakeDailyApi = FakeDailyApi(),
    val social: FakeSocialApi = FakeSocialApi(),
    val telemetry: FakeTelemetryApi = FakeTelemetryApi(),
    val merge: FakeAccountMergeApi = FakeAccountMergeApi(),
) {
    val factory: CloudServicesFactory = { CloudServices(account, api, daily, social, telemetry, merge) }

    init {
        // The fake cloud stores data under the account of the "session": make it the same one as the fake account.
        (account.state.value as? AccountState.SignedIn)?.let { api.currentUser = it.userId }
    }
}

/**
 * Starts the app in a 390x740 dp window and waits until the word lists have loaded and the keyboard is drawn.
 * Nothing here talks to the real network: the account and the cloud are fakes.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.start(
    cloud: TestCloud = TestCloud(),
    haptics: Haptics? = null,
    reduceMotion: Boolean? = null,
    highContrast: Boolean? = null,
    /** The system font size setting: 2.0 is the largest steps phones offer. */
    fontScale: Float = 1f,
    /** Only word puzzles unless a test asks for more: most tests were written for a feed of word puzzles. */
    games: List<GameKind> = listOf(GameKind.Word),
    /** The app opens on the home tab. Most tests are about the feed, so they go there; the home tests stay. */
    openFeed: Boolean = true,
    /** The game of the daily puzzle. The word puzzle unless a test is about another game: most tests were written for it. */
    dailyGame: (Long) -> GameKind = { GameKind.Word },
) {
    // On a phone the Activity owns the ViewModel and clears it when the screen ends. Here nobody does that, so
    // the test does: without it the ViewModel of one test (and its background sync) lives on into the next one.
    val owner = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }
    setContent {
        DisposableEffect(Unit) { onDispose { owner.viewModelStore.clear() } }
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides owner,
            LocalDensity provides Density(LocalDensity.current.density, fontScale),
        ) {
            Box(Modifier.requiredSize(390.dp, 740.dp)) {
                App(cloudServices = cloud.factory, haptics = haptics, reduceMotion = reduceMotion, highContrast = highContrast, games = games, dailyGame = dailyGame)
            }
        }
    }
    waitForIdle()
    // The app opens on the home tab; most tests are about the feed, so go there first.
    if (openFeed) {
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { hasKeyboard() }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.hasKeyboard() =
    onAllNodesWithText("ENTER").fetchSemanticsNodes().isNotEmpty() ||
        onAllNodesWithText("GİR").fetchSemanticsNodes().isNotEmpty()

/**
 * Position (left, top in px) of the input row that is on screen. The next page is composed one page
 * ahead (below the screen), so several rows carry the tag: the visible one is the one nearest the top.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.visibleInputRow(): Pair<Float, Float> =
    onAllNodesWithTag("inputRow").fetchSemanticsNodes()
        .map { it.boundsInRoot }
        .filter { it.width > 0f && it.top >= 0f } // pages that are not laid out report empty bounds
        .minBy { it.top }
        .let { it.left to it.top }

/** Drags up over the grid area. (The default swipe starts at the bottom edge, which is the tab bar.) */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.swipeFeedUp() {
    onRoot().performTouchInput { swipeUp(startY = height * 0.55f, endY = height * 0.10f) }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.key(letter: Char) {
    onAllNodes(hasText(letter.toString()) and hasClickAction())[0].performClick()
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.enter() {
    val label = if (onAllNodesWithText("ENTER").fetchSemanticsNodes().isNotEmpty()) "ENTER" else "GİR"
    // The next page is already composed too (one page ahead), so there are two keyboards: use the first.
    onAllNodesWithText(label)[0].performClick()
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.typeWord(word: String) {
    word.forEach { key(it) }
    enter()
}

/**
 * Makes the word puzzles of the feed English only: back to the home tab, into the feed editor, English on and Turkish
 * off, and the feed is started again.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.switchToEnglish() {
    goHome()
    clickAny("Akışın", "Your feed")
    waitForIdle()
    onNodeWithTag("languageEN").performClick()
    waitForIdle()
    val turkishOn = onNodeWithTag("languageTR").fetchSemanticsNode().config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Selected) == true
    if (turkishOn) onNodeWithTag("languageTR").performClick()
    waitForIdle()
    onNodeWithTag("startFeed").performClick()
    waitForIdle()
}

/** Opens a bottom tab. The label follows the device language, so the Turkish and English names are both given. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.openTab(turkish: String, english: String) {
    // Daily and ranks are pages that open from the home tab, not tabs of their own.
    when (turkish) {
        "Günlük" -> {
            goHome()
            onNodeWithTag("dailyCard").performClick()
            return
        }
        "Sıralama" -> {
            goHome()
            clickAny("Tümü", "See all")
            return
        }
    }
    // The feed and the pages have no tab bar: first go back to where it is.
    if (onAllNodesWithText(turkish).fetchSemanticsNodes().isEmpty() && onAllNodesWithText(english).fetchSemanticsNodes().isEmpty()) goHome()
    val label = if (onAllNodesWithText(turkish).fetchSemanticsNodes().isNotEmpty()) turkish else english
    onNodeWithText(label).performClick()
}

/** Opens the profile tab and then one of its pages by the name of its row (Turkish and English). */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.openProfilePage(turkish: String, english: String) {
    openTab("Profil", "Profile")
    waitForIdle()
    clickAny(turkish, english)
    waitForIdle()
}

/** Back out of the feed or a page to a tab, then to the home tab. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.goHome() {
    for (description in listOf("Geri", "Back", "Ana Sayfa", "Home")) {
        if (onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()) {
            onAllNodesWithContentDescription(description)[0].performClick()
            waitForIdle()
            break
        }
    }
    for (label in listOf("Ana Sayfa", "Home")) {
        if (onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()) {
            onAllNodesWithText(label)[0].performClick()
            waitForIdle()
            break
        }
    }
}

/** True if any of the texts is on screen. Texts follow the device language, so callers give Turkish and English. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.hasAnyText(vararg texts: String) =
    texts.any { onAllNodesWithText(it, substring = true).fetchSemanticsNodes().isNotEmpty() }

/** Clicks the first of the texts that is on screen (exact match). */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.clickAny(vararg texts: String) {
    val text = texts.first { onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty() }
    onNodeWithText(text).performClick()
}

/** Remembers every buzz, so a test can say what the phone would have felt. */
internal class RecordingHaptics : Haptics {
    val kinds = mutableListOf<HapticKind>()
    override fun perform(kind: HapticKind) { kinds += kind }
}
