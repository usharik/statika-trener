package cz.usharik.statika.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Jen volání SDK reklamy (AdMob + UMP) na dané platformě – žádná pravidla. Postup souhlasu
 * (uložený souhlas hned → aktualizace ze sítě → formulář → rozhodnutí z TCF), spuštění SDK,
 * „bez reklamy“ a mezistránkovou reklamu drží [SharedAds]. Vše se volá a odpovídá na hlavním vlákně.
 */
interface AdsSdk {
    /** Je teď obrazovka (Activity / view controller), přes kterou jde ukázat formulář nebo reklamu? */
    fun hasScreen(): Boolean

    /** Aktuální stav UMP + uložené klíče TCF. */
    fun consent(): ConsentSnapshot

    /** UMP requestConsentInfoUpdate; [onDone] ok = false při chybě (bez sítě). */
    fun updateConsentInfo(onDone: (ok: Boolean) -> Unit)

    /** UMP loadAndShowConsentFormIfRequired; [onDone] ok = false, když se formulář nepodařilo načíst/ukázat. */
    fun showConsentFormIfRequired(onDone: (ok: Boolean) -> Unit)

    /** Formulář „možnosti soukromí“ (změna nebo odvolání souhlasu); [onDone] po zavření. */
    fun showPrivacyOptions(onDone: () -> Unit)

    /** Nastaví obsah reklam na PG a spustí SDK; [onReady] po spuštění. [SharedAds] ho volá nejvýš jednou. */
    fun start(onReady: () -> Unit)

    /** Přednačte jednu mezistránkovou reklamu; [onLoaded] = je připravená. */
    fun loadInterstitial(onLoaded: (loaded: Boolean) -> Unit)

    /** Ukáže přednačtenou reklamu; false (a [onClosed] se nezavolá), když to teď nejde. */
    fun showInterstitial(onClosed: (shown: Boolean) -> Unit): Boolean

    /** Adaptivní banner dané šířky (dp), vystředěný v [modifier]. */
    @Composable
    fun Banner(widthDp: Int, modifier: Modifier)
}

/**
 * Reklama se stejnými pravidly na Androidu i iOS:
 * - souhlas: uložený souhlas platí hned, pak aktualizace ze sítě a případně formulář; když něco selže
 *   (bez sítě, ještě není obrazovka), zkusí se to při dalším [onForeground];
 * - analytika se řídí tím, co uživatel v TCF opravdu dovolil ([analyticsConsent]), ne „canRequestAds“;
 * - reklama se žádá jen s canRequestAds (i po „Nesouhlasím“ – pak neosobní, tak to UMP určuje);
 * - po nákupu „bez reklamy“ se nic nenačítá ani neukazuje;
 * - mezistránková reklama: [showInterstitial] zavolá onDone vždy právě jednou; během zobrazení se další
 *   volání hned vrátí s false (dvojí klepnutí);
 * - bez SDK ([sdk] = null, např. UI testy nebo sestavení bez id) aplikace běží bez reklamy.
 */
class SharedAds(private val sdk: AdsSdk?, private val billing: Billing, private val analytics: Analytics) : Ads {
    private val ready = MutableStateFlow(false)
    private val _privacy = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacy
    val isReady: StateFlow<Boolean> get() = ready

    /** souhlas se právě zjišťuje */
    private var inFlight = false
    /** souhlas je pro tento běh vyřízený (aktualizace ze sítě i formulář proběhly) */
    private var settled = false
    private var started = false
    private var sdkReady = false
    private var canRequest = false
    private var lastConsent: AnalyticsConsent? = null
    private var loaded = false
    private var loading = false
    private var showing = false

    /** Start i každý návrat do aplikace: zjistit souhlas, pokud dosud nevyšel. */
    override fun onForeground() = gatherConsent()

    fun gatherConsent() {
        val s = sdk ?: return
        if (settled || inFlight || !s.hasScreen()) return
        inFlight = true
        // souhlas z minula platí hned – reklama i analytika nemusí čekat na síť
        val cached = s.consent()
        if (cached.canRequestAds) apply(cached)
        s.updateConsentInfo { ok ->
            if (!ok) {
                // bez sítě: platí uložený stav, příště se zkusí znovu
                apply(s.consent())
                inFlight = false
                return@updateConsentInfo
            }
            s.showConsentFormIfRequired { shown ->
                apply(s.consent())
                inFlight = false
                settled = shown
            }
        }
    }

    private fun apply(c: ConsentSnapshot) {
        _privacy.value = c.privacyOptionsRequired
        val a = analyticsConsent(c)
        if (a != lastConsent) {
            lastConsent = a
            analytics.setConsent(a)
        }
        canRequest = c.canRequestAds
        val s = sdk ?: return
        if (canRequest && !started) {
            started = true
            s.start {
                sdkReady = true
                updateReady()
            }
        }
        updateReady()
    }

    private fun updateReady() {
        ready.value = sdkReady && canRequest
        if (ready.value) loadInterstitial()
    }

    override fun showPrivacyOptions() {
        val s = sdk ?: return
        s.showPrivacyOptions { apply(s.consent()) }
    }

    private fun loadInterstitial() {
        val s = sdk ?: return
        if (loading || loaded || showing || billing.isAdFree.value || !ready.value) return
        loading = true
        s.loadInterstitial { ok ->
            loaded = ok
            loading = false
        }
    }

    override fun showInterstitial(onDone: (shown: Boolean) -> Unit) {
        val s = sdk
        if (s == null || showing || billing.isAdFree.value || !ready.value || !loaded) {
            if (!showing) loadInterstitial()
            onDone(false)
            return
        }
        // reklama se spotřebuje hned – druhé klepnutí už k SDK nedojde
        loaded = false
        showing = true
        var finished = false
        fun finish(shown: Boolean) {
            if (finished) return
            finished = true
            showing = false
            loadInterstitial()
            onDone(shown)
        }
        if (!s.showInterstitial(::finish)) finish(false)
    }

    @Composable
    override fun Banner(modifier: Modifier) {
        val s = sdk ?: return
        val isReady by ready.collectAsState()
        val adFree by billing.isAdFree.collectAsState()
        if (!isReady || adFree) return
        // šířka podle místa, které banner skutečně má (karta, ne celý displej); AdMob víc než 728 neumí
        BoxWithConstraints(modifier.fillMaxWidth()) {
            val width = maxWidth.value.toInt().coerceAtMost(MAX_BANNER_WIDTH)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                key(width) { s.Banner(width, Modifier) }
            }
        }
    }

    companion object {
        const val MAX_BANNER_WIDTH = 728
    }
}
