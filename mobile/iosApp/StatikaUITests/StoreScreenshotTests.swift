import XCTest

/// Snímky pro App Store (iosApp/store/capture_screenshots.sh): přílohy „store-NN-název“ v jazyce STORE_LANGUAGE
/// (cs / ru; skript ho předá jako TEST_RUNNER_STORE_LANGUAGE) a „iap-settings“ pro kontrolu nákupu.
/// Jazyk a úlohu nastavují argumenty spuštění `-statika.<klíč> <hodnota>`: doména argumentů NSUserDefaults má
/// přednost před tím, co -uiTesting zapíše do AppDelegate (čeština, úloha č. 1), takže aplikace nic dalšího nepotřebuje.
final class StoreScreenshotTests: StatikaUITestCase {
    private var lang: String { ProcessInfo.processInfo.environment["STORE_LANGUAGE"] ?? "cs" }
    private var extraArguments: [String] = []

    override var launchArguments: [String] {
        ["-uiTesting", "-statika.lang", lang, "-AppleLanguages", "(\(lang))", "-AppleLocale", lang == "ru" ? "ru_RU" : "cs_CZ"]
            + extraArguments
    }

    /// Text rozhraní v jazyce snímků.
    private func t(_ cs: String, _ ru: String) -> String { lang == "ru" ? ru : cs }

    private func storeShot(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    /// Napíše text do pole; na menším displeji (delší ruské popisky) pole někdy po prvním klepnutí ještě nemá
    /// fokus klávesnice – klepne znovu (pole pod klávesnicí nejdřív odscrolluje).
    private func type(_ value: String, into field: XCUIElement) {
        for _ in 0..<4 {
            if !field.isHittable { app.swipeUp(); pause(0.6) }
            field.tap()
            pause(0.6)
            if (field.value(forKey: "hasKeyboardFocus") as? Bool) == true { break }
        }
        field.typeText(value)
    }

    /// Skryje klávesnici klepnutím na titulek v jazyce snímků (dismissKeyboard() zná jen český).
    private func hideKeyboard() {
        guard app.keyboards.firstMatch.exists else { return }
        el(t("Statika · nosníky", "Статика · балки")).tap()
        pause(0.8)
        XCTAssertFalse(app.keyboards.firstMatch.exists, "keyboard still visible after tapping the title")
    }

    /// Bohatší úloha než č. 1: Gerberův nosník (kloub), těžká.
    func test01Problem() {
        extraArguments = ["-statika.level", "3", "-statika.types", "gerber", "-statika.seed", "4"]
        app.terminate()
        app.launchArguments = launchArguments
        app.launch()
        wait(t("Úloha č.", "Задача №"), contains: true)
        pause(2)
        storeShot("store-01-problem")
    }

    /// Úloha č. 1 (lehká, prostý nosník) až po výsledek: rovnice z palety a obrazovka „Hotovo“.
    func test02SolveFlow() {
        wait(t("Úloha č. 1", "Задача № 1"), contains: true)
        let next = t("Pokračovat →", "Дальше →")
        let check = t("Zkontrolovat", "Проверить")
        tap(t("RAx, RAy, dvě složky síly", "RAx, RAy, две составляющие силы"))
        tap(next)
        tap(t("RB, jedna síla kolmo k podložce", "RB, одна сила перпендикулярно опорной поверхности"))
        tap(next)
        let fields = app.textViews
        XCTAssertTrue(fields.firstMatch.waitForExistence(timeout: 10))
        type("3", into: fields.element(boundBy: 0))
        type("3", into: fields.element(boundBy: 1))
        tap(check)
        tap(next)
        wait(t("Náhrada zatížení", "Замена нагрузок"))
        tap(next)
        wait(t("Rovnice rovnováhy", "Уравнения равновесия"))
        let verify = t("Ověřit", "Проверить")
        for k in ["RAx", "=", "0"] { key(k) }
        key(verify)
        for k in ["RAy", "+", "RB", "−", "F1  7 kN", "=", "0"] { key(k) }
        key(verify)
        for k in ["RB", "·", "4", "−", "F1  7 kN", "·", "3", "=", "0"] { key(k) }
        key(verify)
        pause(1)
        storeShot("store-02-equations")
        tap(t("Pokračovat k výpočtu →", "Перейти к расчёту →"))
        wait(t("Výpočet reakcí", "Расчёт реакций"))
        let values = app.textViews
        XCTAssertTrue(values.firstMatch.waitForExistence(timeout: 10))
        for (i, v) in ["0", "1,75", "5,25"].enumerated() {
            type(v, into: values.element(boundBy: i))
        }
        // číselná klávesnice zakrývá „Zkontrolovat“ a Compose hlásí jen viditelné prvky (jako FlowTests)
        hideKeyboard()
        tap(check)
        tap(next)
        wait(t("Nová úloha", "Новая задача"))
        // výsledek nahoře: výkres se skutečnými směry reakcí
        app.swipeDown()
        pause(1.5)
        storeShot("store-03-done")
    }

    /// Výklad „Základy statiky“: seznam lekcí a karta lekce o momentu.
    func test03Primer() {
        tap(t("Výklad a procvičování", "Объяснения и задачи"))
        wait(t("Pokračovat →", "Продолжить →"))
        pause(1)
        storeShot("store-04-primer")
        tap(t("Moment síly", "Момент силы"))
        pause(1.5)
        storeShot("store-05-lesson")
    }

    /// Nastavení s nákupem „bez reklamy“ (snímek pro kontrolu nákupu v App Store Connect).
    func test04PurchaseInSettings() {
        tap(t("Nastavení", "Настройки"))
        // okno nastavení se vysouvá zdola; tlačítko nákupu je na menším iPhonu až pod okrajem a Compose hlásí
        // přístupnosti jen viditelné prvky – nejdřív posouvat, pak hledat
        wait(t("Nastavení", "Настройки"))
        pause(1)
        let buy = el(t("Odstranit reklamu", "Убрать рекламу"), contains: true)
        for _ in 0..<6 where !(buy.exists && buy.isHittable) {
            app.swipeUp()
            pause(1)
        }
        XCTAssertTrue(buy.waitForExistence(timeout: 5) && buy.isHittable, "remove-ads button not visible in the settings sheet")
        pause(1.5)
        storeShot("iap-settings")
    }

    /// Kurz „Základy trigonometrie“ (seznam lekcí) a jedna série procvičování s úlohou.
    func test05TrigAndPractice() {
        tap(t("Výklad a procvičování", "Объяснения и задачи"))
        tap(t("Základy trigonometrie", "Основы тригонометрии"))
        wait(t("Procvičování", "Практика"))
        pause(1)
        storeShot("store-06-trig")
        tap(t("Procvičování", "Практика"))
        tap(t("Strany a podíly", "Стороны и отношения"), contains: true)
        pause(1.5)
        storeShot("store-07-practice")
    }

    /// Okno „Úloha jako PDF“ s placeným „Zadání s řešením“ (snímek pro kontrolu nákupu pdf_solution_statika).
    func test06PdfDialog() {
        wait(t("Úloha č.", "Задача №"), contains: true)
        tap(t("Uložit jako PDF", "Сохранить в PDF"))
        wait(t("Úloha jako PDF", "Задача в PDF"))
        pause(1)
        storeShot("iap-pdf")
    }
}
