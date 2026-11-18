package cz.usharik.statika.platform

import kotlinx.coroutines.flow.MutableStateFlow

/** Produkty v obchodech (id v App Store musí být jedinečné v celém účtu, proto se liší). */
object Products {
    const val REMOVE_ADS_PLAY = "remove_ads"
    const val REMOVE_ADS_APP_STORE = "remove_ads_statika"

    /** spotřební produkt: jedno stažení PDF úlohy s řešením */
    const val PDF_SOLUTION_PLAY = "pdf_solution"
    const val PDF_SOLUTION_APP_STORE = "pdf_solution_statika"
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

    /** Tiché ověření vlastnictví bez dotazu na účet (návrat do aplikace); najde i nespotřebované platby za PDF. */
    fun refresh()

    /** Nákup jednoho stažení PDF s řešením (spotřební produkt). */
    fun purchaseExport()

    /** Spotřebuje zaplacené stažení [token] (Play consume / StoreKit finish) – až po jeho započtení. */
    fun consumeExport(token: String)
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

    /** Lokalizovaná cena jednoho stažení PDF s řešením. */
    fun onExportPrice(price: String?)

    /**
     * Ověřená, dosud nespotřebovaná platba za PDF s řešením (právě zaplacená, čekající platba dokončená později,
     * nebo nalezená po restartu). Může přijít víckrát se stejným [token] – započte se jen jednou.
     */
    fun onExportPaid(token: String)

    /** Nákup PDF skončil bez zaplacení (zrušení, chyba, čekající platba). */
    fun onExportPurchaseFinished(success: Boolean)
}

/**
 * Nákup „bez reklamy“ se stejnými pravidly na obou platformách:
 * - poslední známý stav je uložený, takže aplikace startuje bez reklamy i offline a bez bliknutí;
 * - obchod je zdroj pravdy (vrácení peněz → reklama zpět);
 * - podruhé se nekupuje; analytika „remove_ads_started“ / „remove_ads_purchased“ jen jednou za nákup;
 *   zrušený nákup se nepočítá (obnovení po něm není „remove_ads_purchased“);
 * - při návratu do aplikace se vlastnictví tiše ověří ([onForeground]).
 *
 * PDF s řešením se platí za každé stažení (spotřební produkt): zaplacené stažení se uloží jako kredit,
 * teprve pak se platba v obchodě spotřebuje; kredit se odečte při vytvoření PDF ([takeExportCredit]).
 */
class SharedBilling(
    private val sdk: BillingSdk,
    private val store: KeyValueStore,
    private val analytics: Analytics,
    /** úložiště zaplacených stažení PDF – na Androidu soubor vyloučený ze zálohy (jinak by se obnovením dala platba použít znovu) */
    private val purchases: KeyValueStore = store,
) : Billing {
    override val isAdFree = MutableStateFlow(store.getInt(K_AD_FREE, 0) == 1)
    override val removeAdsPrice = MutableStateFlow<String?>(null)
    private var purchasing = false

    override val exportPrice = MutableStateFlow<String?>(null)
    override val exportCredits = MutableStateFlow(purchases.getInt(K_EXPORT_CREDITS, 0))
    override val exportBusy = MutableStateFlow(false)

    /** už započtené platby (poslední), aby se opakovaně hlášená nespotřebovaná platba nezapočetla dvakrát */
    private val countedTokens = purchases.getString(K_EXPORT_TOKENS)?.split('\n')?.filter { it.isNotEmpty() }.orEmpty().toMutableList()

    init {
        sdk.start(object : BillingListener {
            override fun onPrice(price: String?) {
                removeAdsPrice.value = price?.takeIf { it.isNotBlank() }
            }

            override fun onAdFree(adFree: Boolean, purchased: Boolean) = update(adFree, purchased)

            override fun onPurchaseFinished(success: Boolean) {
                if (!success) purchasing = false
            }

            override fun onExportPrice(price: String?) {
                exportPrice.value = price?.takeIf { it.isNotBlank() }
            }

            override fun onExportPaid(token: String) = exportPaid(token)

            override fun onExportPurchaseFinished(success: Boolean) {
                if (!success) exportBusy.value = false
            }
        })
    }

    private fun exportPaid(token: String) {
        if (token !in countedTokens) {
            countedTokens += token
            while (countedTokens.size > MAX_TOKENS) countedTokens.removeAt(0)
            purchases.putString(K_EXPORT_TOKENS, countedTokens.joinToString("\n"))
            exportCredits.value += 1
            purchases.putInt(K_EXPORT_CREDITS, exportCredits.value)
            analytics.event("pdf_purchased")
        }
        exportBusy.value = false
        // spotřebovat až po uložení kreditu – pád mezi tím platbu neztratí (najde se znovu, token už je započtený)
        sdk.consumeExport(token)
    }

    override fun purchaseExport() {
        if (!exportBusy.value) analytics.event("pdf_purchase_started")
        exportBusy.value = true
        sdk.purchaseExport()
    }

    override fun takeExportCredit(): Boolean {
        if (exportCredits.value <= 0) return false
        exportCredits.value -= 1
        purchases.putInt(K_EXPORT_CREDITS, exportCredits.value)
        return true
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
        const val K_EXPORT_CREDITS = "billing.pdfCredits"
        const val K_EXPORT_TOKENS = "billing.pdfTokens"
        private const val MAX_TOKENS = 50
    }
}
