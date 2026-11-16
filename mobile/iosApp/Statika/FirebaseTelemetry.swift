import Foundation
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics
import Shared

/// Firebase Analytics + Crashlytics za Kotlin rozhraním `IosTelemetry` – jen když aplikace obsahuje
/// GoogleService-Info.plist. Firebase se nakonfiguruje až po souhlasu (rozhodnutí z TCF ve společném kódu:
/// `analyticsConsent`, ne „canRequestAds“), do té doby se nic neodesílá; odvolání sběr zase vypne;
/// Info.plist (project.yml) drží sběr vypnutý i při konfiguraci. Analytics bez IDFA (FirebaseAnalyticsCore).
final class FirebaseTelemetry: IosTelemetry {
    private var configured = false

    static func ifAvailable() -> FirebaseTelemetry? {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            NSLog("Statika: no GoogleService-Info.plist, Firebase is off")
            return nil
        }
        return FirebaseTelemetry()
    }

    func logEvent(name: String, params: [String: Any]) {
        guard configured else { return }
        Analytics.logEvent(name, parameters: params)
    }

    func setCollectionEnabled(enabled: Bool) {
        if !configured {
            guard enabled else { return }
            FirebaseApp.configure()
            configured = true
        }
        Analytics.setAnalyticsCollectionEnabled(enabled)
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(enabled)
        if !enabled { Crashlytics.crashlytics().deleteUnsentReports() }
    }
}
