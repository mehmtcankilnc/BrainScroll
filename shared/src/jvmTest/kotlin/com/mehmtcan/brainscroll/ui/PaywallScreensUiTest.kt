package com.mehmtcan.brainscroll.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import com.mehmtcan.brainscroll.game.guide.QuotaState
import com.mehmtcan.brainscroll.stats.Rescue
import com.mehmtcan.brainscroll.telemetry.NoTelemetry
import com.mehmtcan.brainscroll.ui.pro.HintPack
import com.mehmtcan.brainscroll.ui.pro.HintWallet
import com.mehmtcan.brainscroll.ui.pro.LocalPro
import com.mehmtcan.brainscroll.ui.pro.PaywallSheet
import com.mehmtcan.brainscroll.ui.pro.PaywallSource
import com.mehmtcan.brainscroll.ui.pro.ProProduct
import com.mehmtcan.brainscroll.ui.pro.ProState
import com.mehmtcan.brainscroll.ui.pro.PurchaseResult
import com.mehmtcan.brainscroll.ui.pro.RestoreResult
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertTrue

/** The two purchase pages on their own, at a phone size (390 x 740 dp at 2x): look at `shared/build/screenshots/91*.png`. */
@OptIn(ExperimentalTestApi::class)
class PaywallScreensUiTest {
    private class ShopPro(override val hints: HintWallet = HintWallet.None) : ProState {
        override val available = true
        override val isPro: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun product() = ProProduct("129,99₺")
        override suspend fun purchase(): PurchaseResult = PurchaseResult.Cancelled
        override suspend fun restore(): RestoreResult = RestoreResult.NothingToRestore
        override suspend fun packs() = listOf(HintPack("brainscroll_hints_20", 20, "49,99₺"), HintPack("brainscroll_hints_75", 75, "129,99₺"))
        override suspend fun buyPack(pack: HintPack): PurchaseResult = PurchaseResult.Cancelled
    }

    private fun has(test: androidx.compose.ui.test.ComposeUiTest, tag: String) =
        test.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theStoreWithPacksAndTheStreakOffer() = runSkikoComposeUiTest(size = Size(780f, 1480f), density = Density(2f)) {
        var rescue by mutableStateOf<Rescue?>(null)
        setContent {
            BrainScrollTheme {
                CompositionLocalProvider(LocalPro provides ShopPro()) {
                    PaywallSheet(
                        PaywallSource.StoreIcon, onClose = {}, telemetry = NoTelemetry, modifier = Modifier,
                        rescue = rescue, quota = QuotaState(freeLeft = 2, bonusLeft = 0, unlimited = false),
                    )
                }
            }
        }
        waitUntil(timeoutMillis = 10_000) { has(this, "pack_brainscroll_hints_75") }
        mainClock.advanceTimeBy(600)
        snap("910_store_with_packs")
        assertTrue(has(this, "proTerms"), "the terms link is on the store")

        rescue = Rescue(freeStreak = 0, proStreak = 14)
        waitUntil(timeoutMillis = 10_000) { !has(this, "pack_brainscroll_hints_75") && has(this, "proLater") }
        mainClock.advanceTimeBy(600)
        snap("911_streak_offer")
        assertTrue(has(this, "proTerms"), "the terms link is on the streak offer")
        assertTrue(has(this, "proPrivacy") && has(this, "proRestore"))
    }
}
