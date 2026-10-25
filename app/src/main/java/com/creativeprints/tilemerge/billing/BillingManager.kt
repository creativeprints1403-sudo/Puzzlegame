package com.creativeprints.tilemerge.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Handles the one-time "Premium" purchase (no ads + unlimited undo).
 * The product must be created in Play Console with the ID [PRODUCT_ID].
 */
class BillingManager(
    context: Context,
    private val onPremiumChanged: (Boolean) -> Unit,
) : PurchasesUpdatedListener {

    private val main = Handler(Looper.getMainLooper())
    private var details: ProductDetails? = null

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    /** Localized price, e.g. "$2.99", once Play has returned the product. */
    var price by mutableStateOf<String?>(null)
        private set

    fun connect() {
        if (client.isReady) {
            refreshPurchases()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    refreshPurchases()
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        client.queryProductDetailsAsync(params) { result, productDetails ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val found = productDetails.productDetailsList.firstOrNull()
            main.post {
                details = found
                price = found?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    /** Re-checks what the user owns, e.g. after a reinstall or a refund. */
    fun refreshPurchases() {
        if (!client.isReady) return
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val owned = handle(purchases)
                main.post { onPremiumChanged(owned) }
            }
        }
    }

    /** Opens Google Play's purchase sheet. Returns false if the store isn't ready yet. */
    fun launchPurchase(activity: Activity): Boolean {
        val product = details
        if (!client.isReady || product == null) {
            connect()
            return false
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build())
            )
            .build()
        client.launchBillingFlow(activity, params)
        return true
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            if (handle(purchases)) main.post { onPremiumChanged(true) }
        } else if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            refreshPurchases()
        }
    }

    /** Acknowledges new purchases (required within 3 days or Google refunds them). */
    private fun handle(purchases: List<Purchase>): Boolean {
        var owned = false
        for (purchase in purchases) {
            if (PRODUCT_ID !in purchase.products) continue
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            owned = true
            if (!purchase.isAcknowledged) {
                val ack = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                client.acknowledgePurchase(ack) { }
            }
        }
        return owned
    }

    fun end() = client.endConnection()

    companion object {
        const val PRODUCT_ID = "premium"
    }
}
