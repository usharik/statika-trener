package cz.usharik.statika.platform

import platform.UIKit.UIView
import platform.UIKit.UIViewController

/*
 * Firebase, Google Mobile Ads (+ UMP) a StoreKit 2 nemají cestu z Kotlinu – implementuje je Swift aplikace
 * (iosApp/Statika, soubory .swift) a předá je do [IosAppGraph]. Rozhraní jsou schválně „swiftová“: řetězce, Double,
 * closures a objekty-posluchači, žádné Kotlin výjimky přes hranici.
 */

/** Firebase Analytics (+ Crashlytics). Sběr je vypnutý, dokud [setCollectionEnabled] nedostane true. */
interface IosTelemetry {
    /** Hodnoty jsou už upravené společným [AnalyticsParams]: Long, Double nebo String. */
    fun logEvent(name: String, params: Map<String, Any>)

    /** Rozhodnutí o souhlasu ([analyticsConsent]): první true teprve nakonfiguruje Firebase, false sběr vypne. */
    fun setCollectionEnabled(enabled: Boolean)
}

/**
 * AdMob + UMP – jen volání SDK, každé odpovídá na hlavním vlákně. Postup souhlasu a pravidla drží
 * společný [SharedAds]; klíče TCF čte Kotlin přímo z NSUserDefaults.
 */
interface IosAdsBridge {
    /** Google testovací jednotky v Debug, jednotky aplikace v Release (Info.plist). */
    val bannerId: String
    val interstitialId: String

    /** UMP: smí se žádat reklama */
    val canRequestAds: Boolean

    /** UMP: aplikace má nabízet „možnosti soukromí“ */
    val privacyFormRequired: Boolean

    /** UMP: souhlas se v této oblasti nevyžaduje (NOT_REQUIRED) */
    val consentNotRequired: Boolean

    /** requestConsentInfoUpdate; [onDone] ok = false při chybě. */
    fun updateConsentInfo(onDone: (ok: Boolean) -> Unit)

    /** loadAndPresentIfRequired přes [viewController]; [onDone] ok = false, když formulář selhal. */
    fun showConsentFormIfRequired(viewController: UIViewController, onDone: (ok: Boolean) -> Unit)

    /** Formulář „možnosti soukromí“; [onDone] po zavření. */
    fun showPrivacyOptions(viewController: UIViewController, onDone: () -> Unit)

    /** Obsah reklam PG a MobileAds.start; [onReady] po spuštění. */
    fun start(onReady: () -> Unit)

    /** Výška adaptivního banneru (v bodech) pro danou šířku. */
    fun bannerHeight(width: Double): Double

    /** Banner dané šířky, který se hned načítá; [viewController] ukazuje celoobrazovkový obsah reklamy. */
    fun createBanner(adUnitId: String, width: Double, viewController: UIViewController): UIView

    /** Přednačte jednu mezistránkovou reklamu; [onLoaded] = je připravená. */
    fun loadInterstitial(adUnitId: String, onLoaded: (loaded: Boolean) -> Unit)

    /** Ukáže přednačtenou reklamu; false (a nic se neukáže), když žádná není nebo se právě jedna ukazuje. */
    fun showInterstitial(viewController: UIViewController, onClosed: (shown: Boolean) -> Unit): Boolean
}

// StoreKit 2 implementuje Swift přímo jako společné [BillingSdk] (StoreKitBridge.swift).
