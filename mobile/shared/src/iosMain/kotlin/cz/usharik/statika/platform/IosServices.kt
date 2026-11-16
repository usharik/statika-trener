@file:OptIn(ExperimentalForeignApi::class)

package cz.usharik.statika.platform

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

/** Nejvrchnější zobrazený view controller (přes něj se ukazují formuláře, reklamy, sdílení). */
internal fun topViewController(): UIViewController? {
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

/** Firebase přes Swift; úpravy událostí, podržení do souhlasu a rozhodnutí z TCF jsou společné. */
internal fun iosAnalytics(telemetry: IosTelemetry?): Analytics = NormalizedAnalytics(object : AnalyticsSink {
    override fun log(name: String, params: Map<String, Any>) {
        telemetry?.logEvent(name, params)
    }

    override fun setConsent(consent: AnalyticsConsent) {
        telemetry?.setCollectionEnabled(consent.analytics)
    }
})

/** AdMob + UMP přes Swift ([IosAdsBridge]) – jen volání SDK; postup a pravidla drží společný [SharedAds]. */
internal class IosAdsSdk(private val bridge: IosAdsBridge) : AdsSdk {
    override fun hasScreen() = topViewController() != null

    override fun consent(): ConsentSnapshot {
        val d = NSUserDefaults.standardUserDefaults
        return ConsentSnapshot(
            canRequestAds = bridge.canRequestAds,
            privacyOptionsRequired = bridge.privacyFormRequired,
            consentNotRequired = bridge.consentNotRequired,
            gdprApplies = parseGdprApplies(d.objectForKey(TcfKeys.GDPR_APPLIES)),
            purposeConsents = d.stringForKey(TcfKeys.PURPOSE_CONSENTS),
        )
    }

    override fun updateConsentInfo(onDone: (ok: Boolean) -> Unit) = bridge.updateConsentInfo(onDone)

    override fun showConsentFormIfRequired(onDone: (ok: Boolean) -> Unit) {
        val vc = topViewController() ?: return onDone(false)
        bridge.showConsentFormIfRequired(vc, onDone)
    }

    override fun showPrivacyOptions(onDone: () -> Unit) {
        val vc = topViewController() ?: return
        bridge.showPrivacyOptions(vc, onDone)
    }

    override fun start(onReady: () -> Unit) = bridge.start(onReady)

    override fun loadInterstitial(onLoaded: (loaded: Boolean) -> Unit) = bridge.loadInterstitial(bridge.interstitialId, onLoaded)

    override fun showInterstitial(onClosed: (shown: Boolean) -> Unit): Boolean {
        val vc = topViewController() ?: return false
        return bridge.showInterstitial(vc, onClosed)
    }

    @Composable
    override fun Banner(widthDp: Int, modifier: Modifier) {
        val width = widthDp.toDouble()
        // view controller, ve kterém Compose banner skutečně je (ne ten, co je zrovna nahoře)
        val host = LocalUIViewController.current
        UIKitView(
            factory = { bridge.createBanner(bridge.bannerId, width, host) },
            modifier = modifier.width(width.dp).height(bridge.bannerHeight(width).dp),
        )
    }
}

/** Sdílení, odkazy a verze. */
class IosPlatformServices : PlatformServices {
    override val versionName: String = versionLabel(IOS_VERSION_NAME, IOS_GIT_HASH)

    override fun shareText(text: String) {
        val vc = topViewController() ?: return
        val share = UIActivityViewController(listOf(text), null)
        // na iPadu se sdílení ukazuje jako popover – bez kotvy by aplikace spadla
        share.popoverPresentationController?.let { pop ->
            pop.sourceView = vc.view
            vc.view.bounds.useContents {
                pop.sourceRect = platform.CoreGraphics.CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0)
            }
            pop.permittedArrowDirections = 0u
        }
        vc.presentViewController(share, animated = true, completion = null)
    }

    override fun openUrl(url: String) {
        val u = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(u, emptyMap<Any?, Any>(), null)
    }
}

/** Všechny služby iOS aplikace – Swift ji vytvoří jednou (AppDelegate) a předá do [MainViewController]. */
class IosAppGraph(telemetry: IosTelemetry?, ads: IosAdsBridge?, billing: BillingSdk) {
    private val store = UserDefaultsKeyValueStore()
    private val analytics = iosAnalytics(telemetry)
    private val sharedBilling = SharedBilling(billing, store, analytics)
    internal val iosAds = SharedAds(ads?.let(::IosAdsSdk), sharedBilling, analytics)
    val graph = AppGraph(store, analytics, iosAds, sharedBilling, IosPlatformServices())

    /** Swift onOpenURL: statika://12345 nebo …/statika-trener/#12345 otevře úlohu (společný [LinkRequests]). */
    fun openUrl(url: String) {
        graph.links.open(url)
    }
}
