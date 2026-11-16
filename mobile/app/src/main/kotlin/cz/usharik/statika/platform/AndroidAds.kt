package cz.usharik.statika.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import cz.usharik.statika.BuildConfig

/**
 * AdMob + UMP na Androidu – jen volání SDK. Postup souhlasu, rozhodnutí z TCF, „bez reklamy“
 * a mezistránkovou reklamu drží společný [SharedAds].
 */
class AdMobSdk(private val context: Context) : AdsSdk {
    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private var interstitial: InterstitialAd? = null

    /** UMP ukládá klíče IAB TCF do výchozích SharedPreferences aplikace. */
    private val tcf get() = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    override fun hasScreen() = CurrentActivity.activity != null

    override fun consent() = ConsentSnapshot(
        canRequestAds = consent.canRequestAds(),
        privacyOptionsRequired = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
        consentNotRequired = consent.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED,
        gdprApplies = parseGdprApplies(tcf.all[TcfKeys.GDPR_APPLIES]),
        purposeConsents = tcf.all[TcfKeys.PURPOSE_CONSENTS] as? String,
    )

    override fun updateConsentInfo(onDone: (ok: Boolean) -> Unit) {
        val activity = CurrentActivity.activity ?: return onDone(false)
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), { onDone(true) }, { onDone(false) })
    }

    override fun showConsentFormIfRequired(onDone: (ok: Boolean) -> Unit) {
        val activity = CurrentActivity.activity ?: return onDone(false)
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error -> onDone(error == null) }
    }

    override fun showPrivacyOptions(onDone: () -> Unit) {
        val a = CurrentActivity.activity ?: return
        UserMessagingPlatform.showPrivacyOptionsForm(a) { onDone() }
    }

    override fun start(onReady: () -> Unit) {
        // obsah reklam jen „s dohledem rodičů“ – uživatelé jsou studenti
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                // vlastní zařízení vývojáře: jen testovací reklamy (zásady AdMob zakazují klikat na vlastní reklamy)
                .setTestDeviceIds(BuildConfig.ADMOB_TEST_DEVICES.split(',').map(String::trim).filter(String::isNotEmpty))
                .build(),
        )
        MobileAds.initialize(context) { onReady() }
    }

    override fun loadInterstitial(onLoaded: (loaded: Boolean) -> Unit) {
        InterstitialAd.load(context, BuildConfig.INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                onLoaded(true)
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitial = null
                onLoaded(false)
            }
        })
    }

    override fun showInterstitial(onClosed: (shown: Boolean) -> Unit): Boolean {
        val ad = interstitial ?: return false
        val activity = CurrentActivity.activity ?: return false
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = onClosed(true)
            override fun onAdFailedToShowFullScreenContent(e: AdError) = onClosed(false)
        }
        ad.show(activity)
        return true
    }

    @Composable
    override fun Banner(widthDp: Int, modifier: Modifier) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(ctx, widthDp))
                    adUnitId = BuildConfig.BANNER_ID
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
