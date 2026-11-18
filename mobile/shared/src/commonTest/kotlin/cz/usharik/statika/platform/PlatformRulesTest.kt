package cz.usharik.statika.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Záznam analytiky pro testy. */
private class RecordingAnalytics : Analytics {
    val events = mutableListOf<String>()
    val consents = mutableListOf<AnalyticsConsent>()
    override fun event(name: String, params: Map<String, Any>) {
        events += name
    }
    override fun setConsent(consent: AnalyticsConsent) {
        consents += consent
    }
}

/** TCF řetězec s odsouhlasenými účely (číslováno od 1). */
private fun purposes(vararg on: Int) = (1..11).joinToString("") { if (it in on) "1" else "0" }

private val EEA_ALL = ConsentSnapshot(canRequestAds = true, privacyOptionsRequired = true, consentNotRequired = false, gdprApplies = 1, purposeConsents = purposes(*(1..11).toList().toIntArray()))
/** EU, „Nesouhlasím“: UMP přesto dovolí žádat (neosobní) reklamu */
private val EEA_NONE = ConsentSnapshot(canRequestAds = true, privacyOptionsRequired = true, consentNotRequired = false, gdprApplies = 1, purposeConsents = purposes())
private val EEA_UNKNOWN = ConsentSnapshot(canRequestAds = false, privacyOptionsRequired = false, consentNotRequired = false, gdprApplies = null, purposeConsents = null)
private val OUTSIDE = ConsentSnapshot(canRequestAds = true, privacyOptionsRequired = false, consentNotRequired = true, gdprApplies = 0, purposeConsents = null)

/** Falešné SDK reklamy: odpovědi řídí test. */
private class FakeAdsSdk : AdsSdk {
    var screen = true
    var state = EEA_UNKNOWN
    var updates = 0
    var onUpdate: ((Boolean) -> Unit)? = null
    var forms = 0
    var onForm: ((Boolean) -> Unit)? = null
    var onPrivacy: (() -> Unit)? = null
    var starts = 0
    var onReady: (() -> Unit)? = null
    var loads = 0
    var onLoaded: ((Boolean) -> Unit)? = null
    var canShow = true
    var shows = 0
    var onClosed: ((Boolean) -> Unit)? = null

    override fun hasScreen() = screen
    override fun consent() = state
    override fun updateConsentInfo(onDone: (ok: Boolean) -> Unit) {
        updates++
        onUpdate = onDone
    }
    override fun showConsentFormIfRequired(onDone: (ok: Boolean) -> Unit) {
        forms++
        onForm = onDone
    }
    override fun showPrivacyOptions(onDone: () -> Unit) {
        onPrivacy = onDone
    }
    override fun start(onReady: () -> Unit) {
        starts++
        this.onReady = onReady
    }
    override fun loadInterstitial(onLoaded: (Boolean) -> Unit) {
        loads++
        this.onLoaded = onLoaded
    }
    override fun showInterstitial(onClosed: (Boolean) -> Unit): Boolean {
        if (!canShow) return false
        shows++
        this.onClosed = onClosed
        return true
    }
    @Composable
    override fun Banner(widthDp: Int, modifier: Modifier) = Unit

    /** Celý průchod souhlasem: síť OK, formulář (uživatel zvolil [result]). */
    fun answer(result: ConsentSnapshot) {
        onUpdate!!(true)
        state = result
        onForm!!(true)
    }
}

private class FakeBillingSdk : BillingSdk {
    var listener: BillingListener? = null
    var purchases = 0
    var restores = 0
    var refreshes = 0
    override fun start(listener: BillingListener) {
        this.listener = listener
    }
    override fun purchase() {
        purchases++
    }
    override fun restore() {
        restores++
    }
    override fun refresh() {
        refreshes++
    }
    var exportPurchases = 0
    val consumed = mutableListOf<String>()
    override fun purchaseExport() {
        exportPurchases++
    }
    override fun consumeExport(token: String) {
        consumed += token
    }
}

class ConsentTest {
    @Test
    fun tcfUcely() {
        assertTrue(tcfPurpose("1000000100", 1))
        assertTrue(tcfPurpose("1000000100", 8))
        assertFalse(tcfPurpose("1000000100", 2))
        assertFalse(tcfPurpose("10", 9))
        assertFalse(tcfPurpose(null, 1))
        assertFalse(tcfPurpose("1", 0))
    }

    @Test
    fun rozhodnutiOAnalytice() {
        assertEquals(AnalyticsConsent.GRANTED, analyticsConsent(OUTSIDE))
        assertEquals(AnalyticsConsent(analytics = true, adStorage = true, adUserData = true), analyticsConsent(EEA_ALL))
        // „Nesouhlasím“: canRequestAds je true, ale analytika ne
        assertEquals(AnalyticsConsent.DENIED, analyticsConsent(EEA_NONE))
        // účel 1 bez měřicích účelů 8/9 nestačí; s 9 ano
        assertFalse(analyticsConsent(EEA_NONE.copy(purposeConsents = purposes(1))).analytics)
        assertTrue(analyticsConsent(EEA_NONE.copy(purposeConsents = purposes(1, 9))).analytics)
        assertFalse(analyticsConsent(EEA_NONE.copy(purposeConsents = purposes(8, 9))).analytics)
        assertTrue(analyticsConsent(EEA_NONE.copy(purposeConsents = purposes(1, 8))).adStorage)
        assertFalse(analyticsConsent(EEA_NONE.copy(purposeConsents = purposes(1, 8))).adUserData)
        // TCF nic neuložila: jen kde UMP souhlas nevyžaduje
        assertEquals(AnalyticsConsent.DENIED, analyticsConsent(EEA_UNKNOWN))
        assertEquals(AnalyticsConsent.GRANTED, analyticsConsent(EEA_UNKNOWN.copy(consentNotRequired = true)))
    }

    @Test
    fun gdprAppliesZUloziste() {
        assertEquals(1, parseGdprApplies(1))
        assertEquals(0, parseGdprApplies(0L))
        assertEquals(1, parseGdprApplies("1"))
        assertEquals(1, parseGdprApplies(true))
        assertNull(parseGdprApplies("x"))
        assertNull(parseGdprApplies(2))
        assertNull(parseGdprApplies(null))
    }
}

class SharedAdsTest {
    private val analytics = RecordingAnalytics()
    private val billing = FakeBilling()
    private val sdk = FakeAdsSdk()
    private val ads = SharedAds(sdk, billing, analytics)

    private fun consentAndReady(result: ConsentSnapshot = EEA_ALL) {
        ads.onForeground()
        sdk.answer(result)
        sdk.onReady!!()
    }

    @Test
    fun nicSeNenaciteBezSouhlasu() {
        ads.onForeground()
        sdk.onUpdate!!(true)
        sdk.state = EEA_UNKNOWN.copy(privacyOptionsRequired = true)
        sdk.onForm!!(true)
        assertEquals(0, sdk.starts)
        assertFalse(ads.isReady.value)
        assertEquals(listOf(AnalyticsConsent.DENIED), analytics.consents)
        assertTrue(ads.privacyOptionsRequired.value)
        var done: Boolean? = null
        ads.showInterstitial { done = it }
        assertEquals(false, done)
        assertEquals(0, sdk.loads)
    }

    @Test
    fun nesouhlasimReklamaNeosobniAleAnalytikaVypnuta() {
        consentAndReady(EEA_NONE)
        assertEquals(1, sdk.starts)
        assertTrue(ads.isReady.value)
        assertEquals(listOf(AnalyticsConsent.DENIED), analytics.consents)
    }

    @Test
    fun souhlasSpustiSdkJenJednouAAnalytikuZapne() {
        ads.onForeground()
        ads.onForeground() // už se zjišťuje
        assertEquals(1, sdk.updates)
        sdk.answer(EEA_ALL)
        assertEquals(1, sdk.starts)
        assertEquals(listOf(AnalyticsConsent.GRANTED), analytics.consents)
        sdk.onReady!!()
        assertTrue(ads.isReady.value)
        assertEquals(1, sdk.loads)
        // vyřízeno – další návrat do aplikace se už neptá
        ads.onForeground()
        assertEquals(1, sdk.updates)
    }

    @Test
    fun ulozenySouhlasPlatiHnedBezCekaniNaSit() {
        sdk.state = EEA_ALL
        ads.onForeground()
        assertEquals(1, sdk.starts)
        assertEquals(listOf(AnalyticsConsent.GRANTED), analytics.consents)
    }

    @Test
    fun bezSiteSeZkusiZnovuPriNavratu() {
        ads.onForeground()
        sdk.onUpdate!!(false)
        assertEquals(0, sdk.forms)
        assertEquals(listOf(AnalyticsConsent.DENIED), analytics.consents)
        ads.onForeground()
        assertEquals(2, sdk.updates)
        sdk.answer(OUTSIDE)
        assertEquals(listOf(AnalyticsConsent.DENIED, AnalyticsConsent.GRANTED), analytics.consents)
    }

    @Test
    fun formularSelhalZkusiSeZnovu() {
        ads.onForeground()
        sdk.onUpdate!!(true)
        sdk.onForm!!(false)
        ads.onForeground()
        assertEquals(2, sdk.updates)
    }

    @Test
    fun bezObrazovkySeNeptaAZkusiPozdeji() {
        sdk.screen = false
        ads.onForeground()
        assertEquals(0, sdk.updates)
        sdk.screen = true
        ads.onForeground()
        assertEquals(1, sdk.updates)
    }

    @Test
    fun odvolaniSouhlasuVypneAnalytikuIReklamu() {
        consentAndReady(EEA_ALL)
        ads.showPrivacyOptions()
        sdk.state = EEA_NONE.copy(canRequestAds = false)
        sdk.onPrivacy!!()
        assertEquals(listOf(AnalyticsConsent.GRANTED, AnalyticsConsent.DENIED), analytics.consents)
        assertFalse(ads.isReady.value)
        assertTrue(ads.privacyOptionsRequired.value)
        // znovu souhlas: SDK už běží, start se neopakuje
        ads.showPrivacyOptions()
        sdk.state = EEA_ALL
        sdk.onPrivacy!!()
        assertTrue(ads.isReady.value)
        assertEquals(1, sdk.starts)
    }

    @Test
    fun mezistrankovaReklamaOnDonePraveJednou() {
        consentAndReady()
        sdk.onLoaded!!(true)
        val results = mutableListOf<Boolean>()
        ads.showInterstitial { results += it }
        sdk.onClosed!!(true)
        sdk.onClosed!!(false) // SDK ohlásí zavření i chybu – onDone jen jednou
        assertEquals(listOf(true), results)
        assertEquals(2, sdk.loads) // po zavření se přednačte další
    }

    @Test
    fun dvojiKlepnutiBehemReklamy() {
        consentAndReady()
        sdk.onLoaded!!(true)
        val first = mutableListOf<Boolean>()
        val second = mutableListOf<Boolean>()
        ads.showInterstitial { first += it }
        ads.showInterstitial { second += it }
        assertEquals(1, sdk.shows) // druhé klepnutí k SDK nedojde
        assertEquals(listOf(false), second)
        assertEquals(1, sdk.loads) // během zobrazení se nic nenačítá
        sdk.onClosed!!(true)
        assertEquals(listOf(true), first)
        assertEquals(2, sdk.loads)
    }

    @Test
    fun kdyzSeReklamaNedaUkazatOnDoneFalse() {
        consentAndReady()
        sdk.onLoaded!!(true)
        sdk.canShow = false
        val results = mutableListOf<Boolean>()
        ads.showInterstitial { results += it }
        assertEquals(listOf(false), results)
    }

    @Test
    fun neprednactenaReklamaSeNeukazuje() {
        consentAndReady()
        sdk.onLoaded!!(false)
        var done: Boolean? = null
        ads.showInterstitial { done = it }
        assertEquals(false, done)
        assertEquals(2, sdk.loads) // zkusí načíst znovu
    }

    @Test
    fun poNakupuBezReklamy() {
        consentAndReady()
        sdk.onLoaded!!(true)
        billing.purchaseRemoveAds()
        var done: Boolean? = null
        ads.showInterstitial { done = it }
        assertEquals(false, done)
        assertNull(sdk.onClosed)
    }

    @Test
    fun bezSdkAplikaceBeziBezReklamy() {
        val a = SharedAds(null, billing, analytics)
        a.onForeground()
        var done: Boolean? = null
        a.showInterstitial { done = it }
        assertEquals(false, done)
        assertFalse(a.isReady.value)
    }
}

class SharedBillingTest {
    private val analytics = RecordingAnalytics()
    private val store = InMemoryKeyValueStore()
    private val sdk = FakeBillingSdk()

    @Test
    fun pdfSeZaplatiZapocteJednouASpotrebuje() {
        val b = SharedBilling(sdk, store, analytics)
        assertFalse(b.takeExportCredit())
        b.purchaseExport()
        assertTrue(b.exportBusy.value)
        assertEquals(1, sdk.exportPurchases)
        sdk.listener!!.onExportPaid("t1")
        sdk.listener!!.onExportPaid("t1") // stejná platba nahlášená znovu (refresh před spotřebováním)
        assertFalse(b.exportBusy.value)
        assertEquals(1, b.exportCredits.value)
        assertEquals(listOf("t1", "t1"), sdk.consumed)
        assertEquals(listOf("pdf_purchase_started", "pdf_purchased"), analytics.events)
        assertTrue(b.takeExportCredit())
        assertFalse(b.takeExportCredit()) // každé stažení zvlášť
    }

    @Test
    fun zaplaceneStazeniPrezijeRestartAPlatbaSeNezapoctePodruhe() {
        val b = SharedBilling(sdk, store, analytics)
        sdk.listener!!.onExportPaid("t1")
        val sdk2 = FakeBillingSdk()
        val b2 = SharedBilling(sdk2, store, analytics)
        assertEquals(1, b2.exportCredits.value)
        sdk2.listener!!.onExportPaid("t1") // nespotřebovaná platba po restartu
        assertEquals(1, b2.exportCredits.value)
        assertEquals(listOf("t1"), sdk2.consumed)
        assertTrue(b.exportCredits.value == 1)
    }

    @Test
    fun zaplaceneStazeniJeVOddelenemUlozisti() {
        val purchases = InMemoryKeyValueStore()
        val b = SharedBilling(sdk, store, analytics, purchases)
        sdk.listener!!.onExportPaid("t1")
        assertEquals(1, purchases.getInt(SharedBilling.K_EXPORT_CREDITS, 0))
        assertEquals(0, store.getInt(SharedBilling.K_EXPORT_CREDITS, 0)) // hlavní (zálohované) úložiště nic
        assertTrue(b.takeExportCredit())
    }

    @Test
    fun zrusenaPlatbaZaPdfNicNepripise() {
        val b = SharedBilling(sdk, store, analytics)
        b.purchaseExport()
        sdk.listener!!.onExportPurchaseFinished(false)
        assertFalse(b.exportBusy.value)
        assertEquals(0, b.exportCredits.value)
        assertFalse(b.isAdFree.value)
    }

    @Test
    fun ulozenyStavPlatiHnedPoStartu() {
        store.putInt(SharedBilling.K_AD_FREE, 1)
        val b = SharedBilling(sdk, store, analytics)
        assertTrue(b.isAdFree.value)
        b.purchaseRemoveAds()
        assertEquals(0, sdk.purchases) // podruhé se nekupuje
    }

    @Test
    fun nakupUlozStavAAnalytikaJednou() {
        val b = SharedBilling(sdk, store, analytics)
        assertFalse(b.isAdFree.value)
        b.purchaseRemoveAds()
        b.purchaseRemoveAds() // dvojí klepnutí
        assertEquals(2, sdk.purchases)
        sdk.listener!!.onAdFree(adFree = true, purchased = true)
        sdk.listener!!.onAdFree(adFree = true, purchased = true)
        assertTrue(b.isAdFree.value)
        assertEquals(1, store.getInt(SharedBilling.K_AD_FREE, 0))
        assertEquals(listOf("remove_ads_started", "remove_ads_purchased"), analytics.events)
        assertTrue(SharedBilling(FakeBillingSdk(), store, analytics).isAdFree.value)
    }

    @Test
    fun obnoveniNeniNakup() {
        val b = SharedBilling(sdk, store, analytics)
        sdk.listener!!.onAdFree(adFree = true, purchased = false)
        assertTrue(b.isAdFree.value)
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun nakupDokoncenyMimoAplikaciSeZapocita() {
        SharedBilling(sdk, store, analytics)
        sdk.listener!!.onAdFree(adFree = true, purchased = true)
        assertEquals(listOf("remove_ads_purchased"), analytics.events)
    }

    @Test
    fun vraceniPenezVratiReklamu() {
        store.putInt(SharedBilling.K_AD_FREE, 1)
        val b = SharedBilling(sdk, store, analytics)
        sdk.listener!!.onAdFree(adFree = false, purchased = false)
        assertFalse(b.isAdFree.value)
        assertEquals(0, store.getInt(SharedBilling.K_AD_FREE, 1))
    }

    @Test
    fun zrusenyNakupSePakNepocitaJakoNakup() {
        val b = SharedBilling(sdk, store, analytics)
        b.purchaseRemoveAds()
        sdk.listener!!.onPurchaseFinished(false) // zrušeno
        sdk.listener!!.onAdFree(adFree = true, purchased = false) // později „Obnovit nákupy“
        assertTrue(b.isAdFree.value)
        assertEquals(listOf("remove_ads_started"), analytics.events)
        // nové klepnutí po zrušení je nový nákup
        b.purchaseRemoveAds()
    }

    @Test
    fun opakovanyPokusPoZruseniJeNovyNakup() {
        val b = SharedBilling(sdk, store, analytics)
        b.purchaseRemoveAds()
        sdk.listener!!.onPurchaseFinished(false)
        b.purchaseRemoveAds()
        assertEquals(listOf("remove_ads_started", "remove_ads_started"), analytics.events)
    }

    @Test
    fun navratDoAplikaceOveriNakupTise() {
        val b = SharedBilling(sdk, store, analytics)
        b.onForeground()
        assertEquals(1, sdk.refreshes)
        assertEquals(0, sdk.restores)
    }

    @Test
    fun cena() {
        val b = SharedBilling(sdk, store, analytics)
        sdk.listener!!.onPrice("59 Kč")
        assertEquals("59 Kč", b.removeAdsPrice.value)
        sdk.listener!!.onPrice(" ")
        assertNull(b.removeAdsPrice.value)
        b.restorePurchases()
        assertEquals(1, sdk.restores)
    }
}

class PlatformCommonTest {
    @Test
    fun odkazy() {
        assertEquals(12345, seedFromLink("statika://12345"))
        assertEquals(12345, seedFromLink("statika://open/12345"))
        assertEquals(12345, seedFromLink("statika:12345"))
        assertEquals(42, seedFromLink("STATIKA://42"))
        assertEquals(491631, seedFromLink("https://usharik.github.io/statika-trener/#491631"))
        assertEquals(7, seedFromLink("https://usharik.github.io/statika-trener/?seed=7"))
        assertEquals(7, seedFromLink("https://x/?lang=cs&seed=7#abc"))
        assertEquals(12345, seedFromLink("statika://12345/"))
        assertEquals(12345, seedFromLink("statika://open/12345/?x=1"))
        assertNull(seedFromLink("statika://99999999999999999999"))
        assertNull(seedFromLink("statika://-5"))
        assertNull(seedFromLink("statika://0"))
        assertNull(seedFromLink("statika://1000000"))
        assertNull(seedFromLink("https://usharik.github.io/statika-trener/"))
        assertNull(seedFromLink("https://usharik.github.io/statika-trener/#abc"))
        assertNull(seedFromLink(""))
        assertNull(seedFromLink(null))
    }

    @Test
    fun jazykSystemu() {
        assertEquals("ru", systemLanguageCode("ru-CZ"))
        assertEquals("ru", systemLanguageCode("ru_RU"))
        assertEquals("cs", systemLanguageCode("CS"))
        assertEquals("en", systemLanguageCode("en-Latn-US"))
        assertNull(systemLanguageCode(""))
        assertNull(systemLanguageCode(null))
    }

    @Test
    fun verze() {
        assertEquals("1.7 (36ea322)", versionLabel("1.7", "36ea322"))
        assertEquals("1.7", versionLabel("1.7", "unknown"))
        assertEquals("1.7", versionLabel("1.7", null))
    }

    @Test
    fun parametryAnalytiky() {
        assertEquals("problem_solved", AnalyticsParams.name("problem_solved"))
        assertEquals("e_1abc", AnalyticsParams.name("1abc"))
        assertEquals("a_b", AnalyticsParams.name("a-b"))
        assertEquals(40, AnalyticsParams.name("x".repeat(60)).length)
        val p = AnalyticsParams.params(mapOf("level" to 2, "w" to 1.5f, "ok" to true, "long" to "y".repeat(200), "bad key" to 3L))
        assertEquals(2L, p["level"])
        assertEquals(1.5, p["w"])
        assertEquals("true", p["ok"])
        assertEquals(100, (p["long"] as String).length)
        assertEquals(3L, p["bad_key"])
        assertEquals(25, AnalyticsParams.params((1..40).associate { "k$it" to it }).size)
    }

    private class RecordingSink : AnalyticsSink {
        val logged = mutableListOf<Pair<String, Map<String, Any>>>()
        var decided: AnalyticsConsent? = null
        override fun log(name: String, params: Map<String, Any>) {
            logged += name to params
        }
        override fun setConsent(consent: AnalyticsConsent) {
            decided = consent
        }
    }

    @Test
    fun analytikaPodrziUdalostiDoSouhlasu() {
        val sink = RecordingSink()
        val a = NormalizedAnalytics(sink)
        a.event("problem_start", mapOf("level" to 1))
        assertTrue(sink.logged.isEmpty())
        a.setConsent(AnalyticsConsent.GRANTED)
        assertEquals(listOf("problem_start" to mapOf<String, Any>("level" to 1L)), sink.logged)
        assertEquals(AnalyticsConsent.GRANTED, sink.decided)
        a.event("hint", mapOf("step" to 1))
        assertEquals(2, sink.logged.size)
        // odvolání: další události se zahodí
        a.setConsent(AnalyticsConsent.DENIED)
        a.event("hint")
        assertEquals(2, sink.logged.size)
    }

    @Test
    fun poOdmitnutiSePodrzeneUdalostiZahodi() {
        val sink = RecordingSink()
        val a = NormalizedAnalytics(sink)
        repeat(NormalizedAnalytics.BUFFER + 10) { a.event("e$it") }
        a.setConsent(AnalyticsConsent.DENIED)
        a.setConsent(AnalyticsConsent.GRANTED)
        assertTrue(sink.logged.isEmpty())
    }

    @Test
    fun podrzenychUdalostiJeNejvysLimit() {
        val sink = RecordingSink()
        val a = NormalizedAnalytics(sink)
        repeat(NormalizedAnalytics.BUFFER + 10) { a.event("e$it") }
        a.setConsent(AnalyticsConsent.GRANTED)
        assertEquals(NormalizedAnalytics.BUFFER, sink.logged.size)
        assertEquals("e10", sink.logged.first().first) // nejstarší se zahodí
    }

    @Test
    fun odkazyJakoJednorazoveUdalosti() {
        val l = LinkRequests()
        assertTrue(l.open("statika://5"))
        val first = l.current!!
        assertEquals(5, first.seed)
        l.consume(first)
        assertNull(l.current)
        // stejný odkaz podruhé je nová událost
        assertTrue(l.open("statika://5/"))
        assertEquals(5, l.current!!.seed)
        assertTrue(l.current!!.id != first.id)
        l.consume(first) // starou událost už spotřebovat nejde
        assertEquals(5, l.current!!.seed)
        assertFalse(l.open("https://usharik.github.io/statika-trener/"))
        assertFalse(l.open(0))
    }
}
