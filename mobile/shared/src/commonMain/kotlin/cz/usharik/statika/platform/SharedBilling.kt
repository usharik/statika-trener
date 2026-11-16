package cz.usharik.statika.platform

import kotlinx.coroutines.flow.MutableStateFlow

/** Produkty v obchodech (id v App Store musí být jedinečné v celém účtu, proto se liší). */
object Products {
    const val REMOVE_ADS_PLAY = "remove_ads"
    const val REMOVE_ADS_APP_STORE = "remove_ads_statika"
}

/**
 * Jen volání SDK obchodu (Play Billing / StoreKit 2) pro jednorázový produkt „bez reklamy“.
 * Když produkt ještě není načtený (start bez sítě), [purchase] ho nejdřív načte a pak nákup otevře.
 */
interface BillingSdk {
    fun start(listener: BillingListener)
    fun purchase()

    /** „Obnovit nákupy“ na žádost uživatele (iOS: AppStore.sync – smí se zeptat na Apple ID). */
    fun restore()

    /** Tiché ověření vlastnictví bez dotazu na účet (návrat do aplikace). */
    fun refresh()
}

interface BillingListener {
    /** Lokalizovaná cena („59 Kč“), null dokud produkt není známý. */
    fun onPrice(price: String?)

    /**
     * Ověřený stav vlastnictví (nákup, obnovení, vrácení peněz). [purchased] = právě dokončený nákup
     * (i nákup dokončený mimo aplikaci, např. „Ask to Buy“ nebo čekající platba).
     */
    fun onAdFree(adFree: Boolean, purchased: Boolean)

    /** Nákup skončil bez zakoupení (zrušení, chyba, čekající platba) – další úspěch už není „tento“ nákup. */
    fun onPurchaseFinished(success: Boolean)
}

/**
 * Nákup „bez reklamy“ se stejnými pravidly na obou platformách:
 * - poslední známý stav je uložený, takže aplikace startuje bez reklamy i offline a bez bliknutí;
 * - obchod je zdroj pravdy (vrácení peněz → reklama zpět);
 * - podruhé se nekupuje; analytika „remove_ads_started“ / „remove_ads_purchased“ jen jednou za nákup;
 *   zrušený nákup se nepočítá (obnovení po něm není „remove_ads_purchased“);
 * - při návratu do aplikace se vlastnictví tiše ověří ([onForeground]).
 */
class SharedBilling(private val sdk: BillingSdk, private val store: KeyValueStore, private val analytics: Analytics) : Billing {
    override val isAdFree = MutableStateFlow(store.getInt(K_AD_FREE, 0) == 1)
    override val removeAdsPrice = MutableStateFlow<String?>(null)
    private var purchasing = false

    init {
        sdk.start(object : BillingListener {
            override fun onPrice(price: String?) {
                removeAdsPrice.value = price?.takeIf { it.isNotBlank() }
            }

            override fun onAdFree(adFree: Boolean, purchased: Boolean) = update(adFree, purchased)

            override fun onPurchaseFinished(success: Boolean) {
                if (!success) purchasing = false
            }
        })
    }

    private fun update(adFree: Boolean, purchased: Boolean) {
        if (adFree && !isAdFree.value && (purchased || purchasing)) analytics.event("remove_ads_purchased")
        if (adFree) purchasing = false
        isAdFree.value = adFree
        store.putInt(K_AD_FREE, if (adFree) 1 else 0)
    }

    override fun purchaseRemoveAds() {
        if (isAdFree.value) return
        if (!purchasing) analytics.event("remove_ads_started")
        purchasing = true
        sdk.purchase()
    }

    override fun restorePurchases() = sdk.restore()

    override fun onForeground() = sdk.refresh()

    companion object {
        const val K_AD_FREE = "billing.adFree"
    }
}
