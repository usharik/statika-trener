import GoogleMobileAds
import UIKit
import UserMessagingPlatform
import Shared

/// Google Mobile Ads + User Messaging Platform (souhlas v EU) za Kotlin rozhraním `IosAdsBridge` – jen volání SDK.
/// Postup souhlasu (uložený → síť → formulář), rozhodnutí o analytice z TCF, start SDK nejvýš jednou,
/// „bez reklamy“ a pravidla mezistránkové reklamy drží společný `SharedAds` (shared/src/commonMain).
/// Každý callback se volá na hlavním vlákně (Kotlin stav v SharedAds není vícevláknový).
/// Bez App Tracking Transparency: aplikace je pro studenty, reklama není personalizovaná podle IDFA.
final class AdMobBridge: NSObject, IosAdsBridge {
    let bannerId: String
    let interstitialId: String

    private var interstitial: InterstitialAd?
    private var onClosed: ((KotlinBoolean) -> Void)?
    private var presented = false

    /// Most, když Info.plist nese platná id AdMob; jinak nil a aplikace běží bez reklamy.
    static func ifAvailable() -> AdMobBridge? {
        let info = Bundle.main.infoDictionary ?? [:]
        guard let appId = info["GADApplicationIdentifier"] as? String, appId.hasPrefix("ca-app-pub-"),
              let banner = info["StatikaBannerId"] as? String, banner.hasPrefix("ca-app-pub-"),
              let inter = info["StatikaInterstitialId"] as? String, inter.hasPrefix("ca-app-pub-") else {
            NSLog("Statika: no AdMob ids in Info.plist, ads are off")
            return nil
        }
        return AdMobBridge(bannerId: banner, interstitialId: inter)
    }

    private init(bannerId: String, interstitialId: String) {
        self.bannerId = bannerId
        self.interstitialId = interstitialId
    }

    private static func onMain(_ block: @escaping () -> Void) {
        if Thread.isMainThread { block() } else { DispatchQueue.main.async(execute: block) }
    }

    // MARK: souhlas

    var canRequestAds: Bool { ConsentInformation.shared.canRequestAds }
    var privacyFormRequired: Bool { ConsentInformation.shared.privacyOptionsRequirementStatus == .required }
    var consentNotRequired: Bool { ConsentInformation.shared.consentStatus == .notRequired }

    func updateConsentInfo(onDone: @escaping (KotlinBoolean) -> Void) {
        let parameters = RequestParameters()
        parameters.isTaggedForUnderAgeOfConsent = false
        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { error in
            if let error { NSLog("Statika: consent info update failed: \(error.localizedDescription)") }
            AdMobBridge.onMain { onDone(KotlinBoolean(bool: error == nil)) }
        }
    }

    func showConsentFormIfRequired(viewController: UIViewController, onDone: @escaping (KotlinBoolean) -> Void) {
        ConsentForm.loadAndPresentIfRequired(from: viewController) { error in
            if let error { NSLog("Statika: consent form failed: \(error.localizedDescription)") }
            AdMobBridge.onMain { onDone(KotlinBoolean(bool: error == nil)) }
        }
    }

    func showPrivacyOptions(viewController: UIViewController, onDone: @escaping () -> Void) {
        ConsentForm.presentPrivacyOptionsForm(from: viewController) { error in
            if let error { NSLog("Statika: privacy options failed: \(error.localizedDescription)") }
            AdMobBridge.onMain { onDone() }
        }
    }

    // MARK: start SDK (SharedAds volá nejvýš jednou)

    func start(onReady: @escaping () -> Void) {
        // obsah reklam jen „pro všechny s dohledem rodičů“ – uživatelé jsou studenti (jako na Androidu)
        MobileAds.shared.requestConfiguration.maxAdContentRating = .parentalGuidance
        MobileAds.shared.start { _ in AdMobBridge.onMain(onReady) }
    }

    // MARK: banner

    func bannerHeight(width: Double) -> Double {
        Double(largeAnchoredAdaptiveBanner(width: CGFloat(width)).size.height)
    }

    func createBanner(adUnitId: String, width: Double, viewController: UIViewController) -> UIView {
        let banner = BannerView(adSize: largeAnchoredAdaptiveBanner(width: CGFloat(width)))
        banner.adUnitID = adUnitId
        banner.rootViewController = viewController
        banner.load(Request())
        return banner
    }

    // MARK: mezistránková reklama

    func loadInterstitial(adUnitId: String, onLoaded: @escaping (KotlinBoolean) -> Void) {
        InterstitialAd.load(with: adUnitId, request: Request()) { [weak self] ad, error in
            if let error { NSLog("Statika: interstitial failed to load: \(error.localizedDescription)") }
            AdMobBridge.onMain {
                self?.interstitial = ad
                ad?.fullScreenContentDelegate = self
                onLoaded(KotlinBoolean(bool: ad != nil))
            }
        }
    }

    func showInterstitial(viewController: UIViewController, onClosed: @escaping (KotlinBoolean) -> Void) -> Bool {
        // právě se jedna ukazuje – nepřepsat její callback (ten by se pak nezavolal)
        guard self.onClosed == nil, let ad = interstitial else { return false }
        interstitial = nil
        self.onClosed = onClosed
        presented = false
        ad.present(from: viewController)
        return true
    }

    private func finish(shown: Bool) {
        AdMobBridge.onMain {
            let callback = self.onClosed
            self.onClosed = nil
            callback?(KotlinBoolean(bool: shown))
        }
    }
}

extension AdMobBridge: FullScreenContentDelegate {
    func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) { AdMobBridge.onMain { self.presented = true } }
    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) { AdMobBridge.onMain { self.finish(shown: self.presented) } }
    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) { finish(shown: false) }
}
