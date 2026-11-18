package cz.usharik.statika.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Malá trvalá nastavení (SharedPreferences na Androidu, NSUserDefaults na iOS).
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun getInt(key: String, default: Int): Int
    fun putString(key: String, value: String?)
    fun putInt(key: String, value: Int)
}

/** Úložiště jen v paměti – testy a náhledy. */
class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, Any>()
    override fun getString(key: String) = values[key] as? String
    override fun getInt(key: String, default: Int) = values[key] as? Int ?: default
    override fun putString(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
    override fun putInt(key: String, value: Int) {
        values[key] = value
    }
}

/** Analytika (Firebase). UI ani stav kroků Firebase přímo neznají. */
interface Analytics {
    fun event(name: String, params: Map<String, Any> = emptyMap())

    /**
     * Rozhodnutí o souhlasu ([analyticsConsent] z UMP + TCF). Do prvního rozhodnutí se nic neukládá
     * ani neposílá; zavolá se znovu při každé změně (i odvolání v „možnostech soukromí“).
     */
    fun setConsent(consent: AnalyticsConsent) {}
}

object NoAnalytics : Analytics {
    override fun event(name: String, params: Map<String, Any>) = Unit
}

/** Reklama (AdMob). */
interface Ads {
    /** Zobrazí mezistránkovou reklamu, je-li připravená; [onDone] se zavolá vždy právě jednou (shown = byla vidět). */
    fun showInterstitial(onDone: (shown: Boolean) -> Unit)

    /** Pruh reklamy dole na obrazovce; nic, kde platforma reklamu nemá. */
    @Composable
    fun Banner(modifier: Modifier)

    /** Uživatel z EU může souhlas s reklamou kdykoli změnit (požadavek UMP / GDPR). */
    val privacyOptionsRequired: StateFlow<Boolean>
    fun showPrivacyOptions()

    /** Aplikace je na popředí (start, návrat): souhlas, který dosud selhal nebo se nezobrazil, se zkusí znovu. */
    fun onForeground() {}
}

object NoAds : Ads {
    override fun showInterstitial(onDone: (shown: Boolean) -> Unit) = onDone(false)

    @Composable
    override fun Banner(modifier: Modifier) = Unit
    override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false)
    override fun showPrivacyOptions() = Unit
}

/** Jednorázový nákup „bez reklamy“. */
interface Billing {
    val isAdFree: StateFlow<Boolean>

    /** Cena z obchodu (např. „59 Kč“), null dokud není známa nebo když nákupy nejsou k dispozici. */
    val removeAdsPrice: StateFlow<String?>
    fun purchaseRemoveAds()
    fun restorePurchases()

    /** Aplikace je na popředí: tiše ověřit vlastnictví (čekající platba dokončená, vrácení peněz). */
    fun onForeground() {}

    /** Cena jednoho stažení PDF s řešením, null dokud není známa. */
    val exportPrice: StateFlow<String?>

    /** Zaplacená a dosud nevyužitá stažení PDF s řešením. */
    val exportCredits: StateFlow<Int>

    /** Probíhá nákup stažení (od otevření platby do zaplacení / zrušení). */
    val exportBusy: StateFlow<Boolean>

    /** Zaplatit jedno stažení PDF s řešením; po zaplacení přibude [exportCredits]. */
    fun purchaseExport()

    /** Odečte jedno zaplacené stažení; false, když žádné není. */
    fun takeExportCredit(): Boolean
}

class FakeBilling(adFree: Boolean = false) : Billing {
    override val isAdFree = MutableStateFlow(adFree)
    override val removeAdsPrice: StateFlow<String?> = MutableStateFlow(null)
    override fun purchaseRemoveAds() {
        isAdFree.value = true
    }
    override fun restorePurchases() = Unit

    override val exportPrice: StateFlow<String?> = MutableStateFlow(null)
    override val exportCredits = MutableStateFlow(0)
    override val exportBusy: StateFlow<Boolean> = MutableStateFlow(false)

    /** bez obchodu (testy, náhledy) se „zaplatí“ hned */
    override fun purchaseExport() {
        exportCredits.value += 1
    }

    override fun takeExportCredit(): Boolean {
        if (exportCredits.value <= 0) return false
        exportCredits.value -= 1
        return true
    }
}

/**
 * Dokument PDF ze stránek A4 ([WIDTH] × [HEIGHT] bodů, 1 bod = 1/72″), které kreslí společný kód;
 * [fileName] je název souboru včetně přípony .pdf. Stránka se kreslí v jemnějších jednotkách
 * ([SCALE] na bod, hustota DrawScope = [SCALE]) – při malých velikostech písma by Android zaokrouhloval
 * šířky znaků a text by byl nerovnoměrně proložený. Kreslit stránku přes [drawPage].
 */
class PdfDoc(val fileName: String, val title: String, val pages: List<DrawScope.() -> Unit>) {
    companion object {
        const val WIDTH = 595f
        const val HEIGHT = 842f
        const val SCALE = 4f
    }
}

/** Co se liší mezi Androidem a iOS pod společným UI. */
interface PlatformServices {
    val versionName: String
    fun shareText(text: String)
    fun openUrl(url: String)

    /** Uloží PDF do dočasného souboru a nabídne ho systémovým sdílením (uložit, tisk, poslat). */
    fun sharePdf(doc: PdfDoc) {}
}

object NoPlatform : PlatformServices {
    override val versionName = "dev"
    override fun shareText(text: String) = Unit
    override fun openUrl(url: String) = Unit
}

/** Všechny služby aplikace – jedna instance po celou dobu běhu. */
class AppGraph(
    val store: KeyValueStore,
    val analytics: Analytics = NoAnalytics,
    val ads: Ads = NoAds,
    val billing: Billing = FakeBilling(),
    val platform: PlatformServices = NoPlatform,
) {
    /** Úlohy otevřené odkazem (statika://12345, …#12345) – Android i iOS sem jen předají adresu. */
    val links = LinkRequests()

    /** Platforma volá při každém návratu aplikace na popředí (Android onResume, iOS didBecomeActive). */
    fun onForeground() {
        ads.onForeground()
        billing.onForeground()
    }
}

val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph není k dispozici") }

const val WEB_URL = "https://usharik.github.io/statika-trener/"
const val PRIVACY_URL = "https://usharik.github.io/statika-trener/privacy.html"
