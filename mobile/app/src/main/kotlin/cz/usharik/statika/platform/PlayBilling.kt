package cz.usharik.statika.platform

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Google Play Billing pro jednorázový produkt „bez reklamy“ – jen volání SDK. Uložený stav, analytika
 * a pravidla nákupu jsou ve společném [SharedBilling].
 * - Při každém připojení a návratu do aplikace ([refresh]) se vlastněné nákupy ověří znovu
 *   (vrácení peněz → reklama zpět, čekající platba dokončená mimo aplikaci → bez reklamy).
 * - Nákupy se potvrzují (jinak je Play po třech dnech vrátí).
 */
class PlayBillingSdk(context: Context) : BillingSdk {
    private var listener: BillingListener? = null

    @Volatile
    private var details: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override fun start(listener: BillingListener) {
        this.listener = listener
        connect()
    }

    /** Připojení k Play; [then] se spustí po úspěšném připojení (např. nákup po prvním neúspěšném startu bez sítě). */
    private fun connect(then: () -> Unit = {}) {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    queryDetails(then)
                    queryOwned()
                }
            }
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    override fun purchase() {
        if (details == null) {
            // první spuštění bez sítě: připojit, načíst produkt a teprve pak otevřít nákup
            if (client.isReady) queryDetails { launchPurchase() } else connect { launchPurchase() }
            return
        }
        launchPurchase()
    }

    private fun launchPurchase() {
        val d = details
        val activity = CurrentActivity.activity
        if (d == null || activity == null) return listener?.onPurchaseFinished(false) ?: Unit
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        val r = client.launchBillingFlow(activity, params)
        if (r.responseCode != BillingResponseCode.OK && r.responseCode != BillingResponseCode.ITEM_ALREADY_OWNED) listener?.onPurchaseFinished(false)
    }

    override fun restore() {
        if (client.isReady) queryOwned() else connect()
    }

    // Play neptá na účet – tiché ověření je totéž co obnovení
    override fun refresh() = restore()

    private fun onMain(block: () -> Unit) = android.os.Handler(android.os.Looper.getMainLooper()).post(block)

    private fun queryDetails(then: () -> Unit = {}) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT).setProductType(ProductType.INAPP).build()))
            .build()
        client.queryProductDetailsAsync(params) { result, res ->
            if (result.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            val d = res.productDetailsList.firstOrNull { it.productId == PRODUCT }
            details = d
            onMain {
                listener?.onPrice(d?.oneTimePurchaseOfferDetails?.formattedPrice)
                if (d != null) then()
            }
        }
    }

    private fun queryOwned() {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = purchases.filter { PRODUCT in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            // nepotvrzený nákup = právě dokončený (např. čekající platba zaplacená, když aplikace neběžela)
            val fresh = owned.any { !it.isAcknowledged }
            owned.forEach(::acknowledge)
            onMain { listener?.onAdFree(owned.isNotEmpty(), purchased = fresh) }
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                val mine = purchases.orEmpty().filter { PRODUCT in it.products }
                val bought = mine.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                bought.forEach(::acknowledge)
                onMain {
                    if (bought.isNotEmpty()) listener?.onAdFree(true, purchased = true)
                    // čekající platba: dokončí se později (refresh při návratu ji najde)
                    else listener?.onPurchaseFinished(false)
                }
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> queryOwned()
            else -> onMain { listener?.onPurchaseFinished(false) } // zrušeno nebo chyba
        }
    }

    private fun acknowledge(p: Purchase) {
        if (p.isAcknowledged) return
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { }
    }

    private companion object {
        /** jednorázový produkt v Play Console */
        const val PRODUCT = Products.REMOVE_ADS_PLAY
    }
}

/** Stav „bez reklamy“ ze starších verzí (SharedPreferences „billing“) převezme společné úložiště. */
fun migrateLegacyAdFree(context: Context, store: KeyValueStore) {
    val old = context.getSharedPreferences("billing", Context.MODE_PRIVATE)
    if (old.getBoolean("ad_free", false) && store.getInt(SharedBilling.K_AD_FREE, 0) == 0) store.putInt(SharedBilling.K_AD_FREE, 1)
    if (old.contains("ad_free")) old.edit().remove("ad_free").apply()
}
