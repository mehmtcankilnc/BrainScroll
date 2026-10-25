package com.mehmtcan.brainscroll.ui.pro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProControllerTest {
    private val billing = FakeStoreBilling()
    private val cache = MemoryProCache()

    private fun TestScope.controller(store: StoreBilling? = billing) =
        ProController(store, cache, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

    @Test
    fun startsFromTheCachedAnswer() = runTest {
        cache.value = true
        assertTrue(controller().isPro.value, "Pro is known at once, before the store answers")
    }

    @Test
    fun withoutAStoreThereIsNothingToBuyAndTheAppIsFree() = runTest {
        cache.value = true
        val pro = controller(store = null)
        assertFalse(pro.available)
        assertFalse(pro.isPro.value)
        assertEquals(PurchaseResult.Failed("unavailable"), pro.purchase())
    }

    @Test
    fun aFailedRefreshKeepsTheCachedPro() = runTest {
        cache.value = true
        billing.refreshAnswer = null // no network
        val pro = controller()
        assertTrue(pro.isPro.value)
        assertTrue(cache.value, "a bad network never reads as 'not Pro'")
    }

    @Test
    fun aDefiniteNoFromTheStoreTakesProAwayAndClearsTheCache() = runTest {
        cache.value = true
        billing.refreshAnswer = false // refunded
        val pro = controller()
        assertFalse(pro.isPro.value)
        assertFalse(cache.value)
    }

    @Test
    fun aYesFromTheStoreIsCached() = runTest {
        billing.refreshAnswer = true
        val pro = controller()
        assertTrue(pro.isPro.value)
        assertTrue(cache.value)
    }

    @Test
    fun aSuccessfulPurchaseUnlocksPro() = runTest {
        val pro = controller()
        assertEquals(PurchaseResult.Success, pro.purchase())
        assertTrue(pro.isPro.value)
        assertTrue(cache.value)
    }

    @Test
    fun cancelledPendingAndFailedPurchasesUnlockNothing() = runTest {
        val pro = controller()
        for (result in listOf(PurchaseResult.Cancelled, PurchaseResult.Pending, PurchaseResult.Failed("x"))) {
            billing.purchaseResult = result
            assertEquals(result, pro.purchase())
            assertFalse(pro.isPro.value, "$result must not unlock Pro")
            assertFalse(cache.value)
        }
    }

    @Test
    fun aPurchaseThatThrowsIsAFailureNotACrash() = runTest {
        val pro = controller(store = object : StoreBilling by billing {
            override suspend fun purchase(): PurchaseResult = error("store exploded")
        })
        assertTrue(pro.purchase() is PurchaseResult.Failed)
        assertFalse(pro.isPro.value)
    }

    @Test
    fun aPendingPurchaseUnlocksWhenTheStoreApprovesItLater() = runTest {
        billing.purchaseResult = PurchaseResult.Pending
        val pro = controller()
        pro.purchase()
        assertFalse(pro.isPro.value)
        billing.updates.emit(true) // the parent approved
        assertTrue(pro.isPro.value)
        assertTrue(cache.value)
    }

    @Test
    fun restoreUnlocksOnlyWhenSomethingWasRestored() = runTest {
        val pro = controller()
        billing.restoreResult = RestoreResult.NothingToRestore
        assertEquals(RestoreResult.NothingToRestore, pro.restore())
        assertFalse(pro.isPro.value)
        billing.restoreResult = RestoreResult.Failed
        assertEquals(RestoreResult.Failed, pro.restore())
        assertFalse(pro.isPro.value)
        billing.restoreResult = RestoreResult.Restored
        assertEquals(RestoreResult.Restored, pro.restore())
        assertTrue(pro.isPro.value)
    }

    @Test
    fun theProductComesFromTheStoreAndAFailureIsJustNull() = runTest {
        val pro = controller()
        assertEquals(ProProduct("$4.99"), pro.product())
        billing.price = null
        assertNull(pro.product())
    }

    @Test
    fun theAccountIsPassedToTheStoreAndAFailureThereIsHarmless() = runTest {
        val pro = controller()
        pro.identify("user-1")
        assertEquals(listOf("user-1"), billing.identified)
        billing.identifyFails = true
        pro.identify("user-2") // does not throw
        assertEquals(listOf("user-1"), billing.identified)
    }
}
