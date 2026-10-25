package com.mehmtcan.brainscroll.ui.pro

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitGetProducts
import com.revenuecat.purchases.kmp.ktx.awaitLogIn
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * RevenueCat behind [StoreBilling]. The Android and iOS files are the same code (the SDK is one API); they are two files
 * because the desktop build has no RevenueCat and so no source set is shared by exactly these two.
 */
internal class RevenueCatBilling(apiKey: String) : StoreBilling {
    private val updates = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)
    private val purchases: Purchases

    init {
        Purchases.configure(PurchasesConfiguration(apiKey))
        purchases = Purchases.sharedInstance
        // Fires for a purchase approved later (Ask to Buy), a refund, and anything the store changed on its own.
        purchases.delegate = object : PurchasesDelegate {
            override fun onPurchasePromoProduct(
                product: StoreProduct,
                startPurchase: (
                    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                    onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
                ) -> Unit,
            ) = Unit

            override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
                updates.tryEmit(customerInfo.isPro())
            }
        }
    }

    override val entitlement: Flow<Boolean> = updates

    private fun CustomerInfo.isPro() = entitlements[ProConfig.ENTITLEMENT]?.isActive == true

    override suspend fun refresh() {
        updates.emit(purchases.awaitCustomerInfo().isPro())
    }

    override suspend fun product(): ProProduct? =
        purchases.awaitGetProducts(listOf(ProConfig.PRODUCT_ID)).firstOrNull()?.let { ProProduct(it.price.formatted) }

    override suspend fun purchase(): PurchaseResult {
        val product = purchases.awaitGetProducts(listOf(ProConfig.PRODUCT_ID)).firstOrNull()
            ?: return PurchaseResult.Failed("product_not_found")
        return try {
            val done = purchases.awaitPurchase(product)
            if (done.customerInfo.isPro()) PurchaseResult.Success else PurchaseResult.Failed("entitlement_inactive")
        } catch (e: PurchasesTransactionException) {
            when {
                e.userCancelled -> PurchaseResult.Cancelled
                e.code == PurchasesErrorCode.PaymentPendingError -> PurchaseResult.Pending
                else -> PurchaseResult.Failed(e.code.name)
            }
        }
    }

    override suspend fun restore(): RestoreResult = try {
        val info = purchases.awaitRestore()
        updates.emit(info.isPro())
        if (info.isPro()) RestoreResult.Restored else RestoreResult.NothingToRestore
    } catch (e: PurchasesException) {
        RestoreResult.Failed
    }

    override suspend fun identify(userId: String) {
        if (purchases.appUserID != userId) updates.emit(purchases.awaitLogIn(userId).customerInfo.isPro())
    }
}

// One store for the whole process: the activity can be rebuilt (rotation), the SDK must be configured once.
private var instance: RevenueCatBilling? = null

actual fun createStoreBilling(): StoreBilling? =
    ProConfig.IOS_API_KEY.takeIf { it.isNotBlank() }?.let { key -> instance ?: RevenueCatBilling(key).also { instance = it } }
