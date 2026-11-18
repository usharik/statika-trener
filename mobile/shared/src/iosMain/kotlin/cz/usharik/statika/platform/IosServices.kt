@file:OptIn(ExperimentalForeignApi::class)

package cz.usharik.statika.platform

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL
import platform.Foundation.NSUserDefaults
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginPDFContextToData
import platform.UIKit.UIGraphicsBeginPDFPage
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIImage
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

    override fun shareText(text: String) = share(listOf(text))

    /** Compose na iOS kreslí přes Skiu bez PDF výstupu – stránky jdou do PDF jako obrázky 3× (cca 216 dpi). */
    override fun sharePdf(doc: PdfDoc) {
        val bounds = CGRectMake(0.0, 0.0, PdfDoc.WIDTH.toDouble(), PdfDoc.HEIGHT.toDouble())
        val data = NSMutableData()
        UIGraphicsBeginPDFContextToData(data, bounds, null)
        for (i in doc.pages.indices) {
            UIGraphicsBeginPDFPage()
            val png = Image.makeFromBitmap(doc.rasterize(i, 3f).asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes ?: continue
            val bytes = png.usePinned { NSData.dataWithBytes(it.addressOf(0), png.size.toULong()) }
            UIImage.imageWithData(bytes)?.drawInRect(bounds)
        }
        UIGraphicsEndPDFContext()
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + doc.fileName)
        if (!data.writeToURL(url, atomically = true)) return
        share(listOf(url))
    }

    private fun share(items: List<Any>) {
        val vc = topViewController() ?: return
        val share = UIActivityViewController(items, null)
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
