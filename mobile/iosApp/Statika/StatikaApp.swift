import SwiftUI
import UIKit
import Shared

@main
struct StatikaApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some SwiftUI.Scene {
        WindowGroup {
            // Compose kreslí od okraje k okraji a bezpečné oblasti řeší přes window insets.
            ContentView(app: appDelegate.app)
                .ignoresSafeArea()
                // statika://12345 (nebo odkaz s #12345) otevře úlohu podle čísla
                .onOpenURL { url in appDelegate.app.openUrl(url: url.absoluteString) }
        }
    }
}

/// Drží služby aplikace (nastavení, Firebase, AdMob, StoreKit) po celou dobu běhu.
final class AppDelegate: NSObject, UIApplicationDelegate {
    let app: IosAppGraph

    override init() {
        let args = ProcessInfo.processInfo.arguments
        // UI testy: čistý stav, čeština, lehké úlohy s prostým nosníkem, úloha č. 1; bez reklamy (bez formuláře UMP),
        // pokud test výslovně nechce reklamu (-uiTestingAds).
        if args.contains("-uiTesting") {
            let d = UserDefaults.standard
            d.dictionaryRepresentation().keys.filter { $0.hasPrefix("statika.") }.forEach(d.removeObject(forKey:))
            d.set("cs", forKey: "statika.lang")
            d.set("simple", forKey: "statika.types")
            d.set(1, forKey: "statika.level")
            d.set(1, forKey: "statika.seed")
            // první spuštění (prohlídka aplikace, nabídka výkladu) jen v testu, který o ně výslovně stojí
            if !args.contains("-uiTestingFirstRun") {
                d.set(1, forKey: "statika.learn.firstRun")
                d.set(1, forKey: "statika.tour.done")
            }
        }
        let ads = args.contains("-uiTesting") && !args.contains("-uiTestingAds") ? nil : AdMobBridge.ifAvailable()
        app = IosAppGraph(telemetry: FirebaseTelemetry.ifAvailable(), ads: ads, billing: StoreKitBridge())
        super.init()
    }
}
