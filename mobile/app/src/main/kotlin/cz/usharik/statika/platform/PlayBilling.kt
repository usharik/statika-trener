package cz.usharik.statika.platform

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Google Play Billing – jen volání SDK. Uložený stav, analytika a pravidla nákupu jsou ve společném [SharedBilling].
 * - „bez reklamy“ (jednorázový): při každém připojení a návratu do aplikace ([refresh]) se vlastnictví ověří znovu
 *   (vrácení peněz → reklama zpět, čekající platba dokončená mimo aplikaci → bez reklamy); nákup se potvrzuje
 *   (jinak ho Play po třech dnech vrátí).
 * - PDF s řešením (spotřební): zaplacený nákup se ohlásí [BillingListener.onExportPaid] a spotřebuje se až
 *   v [consumeExport]; nespotřebované platby se při připojení a návratu do aplikace najdou znovu.
 */
class PlayBillingSdk(context: Context) : BillingSdk {
    private var listener: BillingListener? = null

    @Volatile
    private var details: Map<String, ProductDetails> = emptyMap()

    /** produkt, jehož nákup je právě otevřený (zrušení z Play neříká, o který šlo) */
    @Volatile
    private var inFlight: String? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override fun start(listener: BillingListener) {
        this.listener = listener
        connect()
    }

    /** Připojení k Play; [then] se spustí po připojení a načtení produktů (i neúspěšném), např. nákup po startu bez sítě. */
    private fun connect(then: () -> Unit = {}) {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    queryDetails(then)
                    queryOwned()
                } else onMain(then) // bez Play: čekající nákup se ukončí jako neúspěšný
            }
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    override fun purchase() = purchase(AD_FREE)

    override fun purchaseExport() = purchase(PDF)

    private fun purchase(product: String) {
        if (details[product] == null) {
            // první spuštění bez sítě: připojit, načíst produkty a teprve pak otevřít nákup
            if (client.isReady) queryDetails { launchPurchase(product) } else connect { launchPurchase(product) }
            return
        }
        launchPurchase(product)
    }

    private fun finished(product: String?) {
        if (product == PDF) listener?.onExportPurchaseFinished(false) else listener?.onPurchaseFinished(false)
    }

    private fun launchPurchase(product: String) {
        val d = details[product]
        val activity = CurrentActivity.activity
        if (d == null || activity == null) return finished(product)
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        inFlight = product
        val r = client.launchBillingFlow(activity, params)
        if (r.responseCode == BillingResponseCode.ITEM_ALREADY_OWNED) {
            // u PDF: předchozí platba ještě není spotřebovaná – najde se a započte
            queryOwned()
            if (product == PDF) finished(product)
        } else if (r.responseCode != BillingResponseCode.OK) finished(product)
    }

    override fun consumeExport(token: String) {
        val params = ConsumeParams.newBuilder().setPurchaseToken(token).build()
        val run = { client.consumeAsync(params) { _, _ -> } }
        if (client.isReady) run() else connect { run() }
    }

    override fun restore() {
        if (client.isReady) queryOwned() else connect()
    }

    // Play neptá na účet – tiché ověření je totéž co obnovení
    override fun refresh() = restore()

    private fun onMain(block: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(block)
    }

    private fun queryDetails(then: () -> Unit = {}) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(AD_FREE, PDF).map { QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(ProductType.INAPP).build() })
            .build()
        client.queryProductDetailsAsync(params) { result, res ->
            if (result.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync onMain(then)
            val found = res.productDetailsList.associateBy { it.productId }
            details = found
            onMain {
                listener?.onPrice(found[AD_FREE]?.oneTimePurchaseOfferDetails?.formattedPrice)
                listener?.onExportPrice(found[PDF]?.oneTimePurchaseOfferDetails?.formattedPrice)
                // nákup: produkt chybí → launchPurchase ho ukončí jako neúspěšný
                then()
            }
        }
    }

    private fun queryOwned() {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) return@queryPurchasesAsync
            val paid = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            val owned = paid.filter { AD_FREE in it.products }
            // nepotvrzený nákup = právě dokončený (např. čekající platba zaplacená, když aplikace neběžela)
            val fresh = owned.any { !it.isAcknowledged }
            owned.forEach(::acknowledge)
            val exports = paid.filter { PDF in it.products }
            onMain {
                listener?.onAdFree(owned.isNotEmpty(), purchased = fresh)
                exports.forEach { listener?.onExportPaid(it.purchaseToken) }
            }
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val product = inFlight
        inFlight = null
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                val bought = purchases.orEmpty().filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                val adFree = bought.filter { AD_FREE in it.products }
                val exports = bought.filter { PDF in it.products }
                adFree.forEach(::acknowledge)
                onMain {
                    if (adFree.isNotEmpty()) listener?.onAdFree(true, purchased = true)
                    exports.forEach { listener?.onExportPaid(it.purchaseToken) }
                    // čekající platba: dokončí se později (refresh při návratu ji najde)
                    if (adFree.isEmpty() && exports.isEmpty()) finished(product)
                }
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryOwned()
                if (product == PDF) onMain { finished(product) }
            }
            else -> onMain { finished(product) } // zrušeno nebo chyba
        }
    }

    private fun acknowledge(p: Purchase) {
        if (p.isAcknowledged) return
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { }
    }

    private companion object {
        /** jednorázový produkt v Play Console */
        const val AD_FREE = Products.REMOVE_ADS_PLAY

        /** spotřební produkt v Play Console – jedno stažení PDF s řešením */
        const val PDF = Products.PDF_SOLUTION_PLAY
    }
}

/** Stav „bez reklamy“ ze starších verzí (SharedPreferences „billing“) převezme společné úložiště. */
fun migrateLegacyAdFree(context: Context, store: KeyValueStore) {
    val old = context.getSharedPreferences("billing", Context.MODE_PRIVATE)
    if (old.getBoolean("ad_free", false) && store.getInt(SharedBilling.K_AD_FREE, 0) == 0) store.putInt(SharedBilling.K_AD_FREE, 1)
    if (old.contains("ad_free")) old.edit().remove("ad_free").apply()
}
