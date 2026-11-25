package com.mehmtcan.brainscroll.ui.pro

import com.mehmtcan.brainscroll.cloud.HintSpend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HintWalletTest {
    private val api = FakeHintBalanceApi(balance = 20)
    private val cache = MemoryHintCache(7)

    @Test
    fun startsFromTheCachedNumberUntilTheServerAnswers() = runTest {
        val wallet = HintWallet(api, cache)
        assertEquals(7, wallet.balance.value)
        wallet.refresh()
        assertEquals(20, wallet.balance.value)
        assertEquals(20, cache.value, "the number is kept for the next start")
    }

    @Test
    fun aFailedReadChangesNothing() = runTest {
        api.offline = true
        val wallet = HintWallet(api, cache)
        wallet.refresh()
        assertEquals(7, wallet.balance.value, "no network never reads as 'no hints'")
        assertEquals(7, cache.value)
    }

    @Test
    fun spendingFollowsTheServer() = runTest {
        val wallet = HintWallet(api, cache)
        assertEquals(HintSpend.Spent(19), wallet.spend())
        assertEquals(19, wallet.balance.value)
        assertEquals(19, cache.value)
    }

    @Test
    fun aHintIsNotGivenWithoutTheServersYes() = runTest {
        api.offline = true
        val wallet = HintWallet(api, cache)
        assertEquals(HintSpend.Offline, wallet.spend())
        assertEquals(7, wallet.balance.value, "offline spends nothing")
    }

    @Test
    fun anEmptyAnswerFromTheServerCorrectsAStaleNumber() = runTest {
        api.balance = 0
        val wallet = HintWallet(api, cache)
        assertEquals(HintSpend.Empty, wallet.spend())
        assertEquals(0, wallet.balance.value)
        assertEquals(0, cache.value)
    }

    @Test
    fun waitsForTheWebhookAfterAPurchase() = runTest {
        val wallet = HintWallet(api, cache)
        wallet.refresh()
        launch {
            delay(5_000) // the webhook comes a few seconds after the store
            api.balance += 20
        }
        assertTrue(wallet.awaitCredit(before = 20))
        assertEquals(40, wallet.balance.value)
    }

    @Test
    fun givesUpAfterTwentySecondsAndSaysTheHintsAreStillOnTheWay() = runTest {
        val wallet = HintWallet(api, cache)
        wallet.refresh()
        assertFalse(wallet.awaitCredit(before = 20))
        assertEquals(20, wallet.balance.value, "nothing is added by the app itself")
    }
}
