import XCTest

/// Reklama s testovacími jednotkami Googlu (Debug): formulář souhlasu (UMP), pak banner na obrazovce „Hotovo“.
/// Potřebuje síť; ci.sh ho v CI přeskakuje (SKIP_ADS).
final class AdsTests: StatikaUITestCase {
    override var launchArguments: [String] { ["-uiTesting", "-uiTestingAds"] }

    func testConsentThenBannerOnDoneScreen() {
        // formulář UMP se v simulátoru (EU nastavení testovací aplikace) ukáže při prvním spuštění
        let consent = app.buttons["Consent"].firstMatch
        if consent.waitForExistence(timeout: 20) {
            screenshot("consent_form")
            consent.tap()
        }
        solveProblemOne()
        // testovací banner: webový obsah reklamy s nápisem „Test Ad“
        let ad = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS[c] 'Test Ad'")).firstMatch
        XCTAssertTrue(ad.waitForExistence(timeout: 30), "test banner not shown")
        app.swipeUp()
        pause(2)
        screenshot("done_with_banner")
    }
}
