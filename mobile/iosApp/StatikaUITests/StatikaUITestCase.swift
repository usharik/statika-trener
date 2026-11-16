import XCTest

/// Základ UI testů: spustí aplikaci s čistým stavem (-uiTesting, viz AppDelegate) a ukládá snímky obrazovky.
/// Compose Multiplatform hlásí texty a contentDescription jako accessibility label – testy hledají podle nich.
/// Snímky: `screenshot(_:)` v kontrolních bodech; ci.sh je vyexportuje jako PNG.
class StatikaUITestCase: XCTestCase {
    var app: XCUIApplication!
    private var shotNumber = 0

    var launchArguments: [String] { ["-uiTesting"] }

    override func setUpWithError() throws {
        continueAfterFailure = false
        shotNumber = 0
        // každý test začíná na výšku – simulátor si pamatuje natočení z přerušeného běhu (naležato pak gesta míří jinam)
        XCUIDevice.shared.orientation = .portrait
        app = XCUIApplication()
        app.launchArguments = launchArguments
        app.launch()
    }

    override func tearDownWithError() throws {
        screenshot("end")
        app.terminate()
    }

    func screenshot(_ checkpoint: String) {
        shotNumber += 1
        let className = String(describing: type(of: self))
        let testName = name.components(separatedBy: " ").last?.trimmingCharacters(in: CharacterSet(charactersIn: "]")) ?? name
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = String(format: "%@.%@.%02d-%@", className, testName, shotNumber, checkpoint)
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    /// Prvek s labelem [label] (přesně), nebo obsahujícím [label] při contains = true.
    func el(_ label: String, contains: Bool = false) -> XCUIElement {
        let p = contains ? NSPredicate(format: "label CONTAINS %@", label) : NSPredicate(format: "label == %@", label)
        return app.descendants(matching: .any).matching(p).firstMatch
    }

    @discardableResult
    func wait(_ label: String, contains: Bool = false, timeout: TimeInterval = 20, file: StaticString = #filePath, line: UInt = #line) -> XCUIElement {
        let e = el(label, contains: contains)
        XCTAssertTrue(e.waitForExistence(timeout: timeout), "\"\(label)\" not found", file: file, line: line)
        return e
    }

    /// Klepne na prvek; když je pod okrajem, nejdřív posouvá obsah.
    func tap(_ label: String, contains: Bool = false, file: StaticString = #filePath, line: UInt = #line) {
        let e = el(label, contains: contains)
        _ = e.waitForExistence(timeout: 5)
        for _ in 0..<8 where !comfortablyVisible(e) {
            scrollUp(near: e)
        }
        // Compose ukazuje v přístupnosti jen viditelné prvky – prvek nad horním okrajem najde posun zpět
        for _ in 0..<10 where !(e.exists && e.isHittable) {
            scrollDown(near: e)
        }
        XCTAssertTrue(e.waitForExistence(timeout: 10), "\"\(label)\" not found", file: file, line: line)
        e.tap()
    }

    /// Číselné pole, jehož hodnota obsahuje [text] (i se znaménkem).
    func field(with text: String) -> XCUIElement {
        app.textViews.matching(NSPredicate(format: "value CONTAINS %@", text)).firstMatch
    }

    /// Klepne na „±“ pole s hodnotou [text] (tlačítko na stejné výšce, vpravo od pole a nejblíž jeho pravému okraji).
    func toggleSign(ofFieldWith text: String, file: StaticString = #filePath, line: UInt = #line) {
        let f = field(with: text)
        XCTAssertTrue(f.waitForExistence(timeout: 5), "field \(text) not found", file: file, line: line)
        for _ in 0..<6 where !comfortablyVisible(f) { scrollUp(near: f) }
        let buttons = app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", "Změnit znaménko")).allElementsBoundByIndex
        // stejný řádek a vpravo od pole (na iPadu jsou všechna pole vedle sebe), nejblíž jeho pravému okraji
        let candidates = buttons.filter { $0.isHittable && abs($0.frame.midY - f.frame.midY) < f.frame.height && $0.frame.midX > f.frame.minX }
        let row = candidates.min { abs($0.frame.midX - f.frame.maxX) < abs($1.frame.midX - f.frame.maxX) }
        guard let sign = row else {
            XCTFail("± next to field \(text) not found", file: file, line: line)
            return
        }
        sign.tap()
        pause(0.5)
    }

    var isLandscape: Bool { app.frame.width > app.frame.height }

    /// Prvek je vidět a jeho střed není těsně u spodního okraje (naležato zbývá málo místa a klepnutí na
    /// napůl schovanou kartu by minulo).
    func comfortablyVisible(_ e: XCUIElement) -> Bool {
        guard e.exists && e.isHittable else { return false }
        return e.frame.midY < app.frame.maxY - 40
    }

    /// Pole s fokusem nesmí být pod klávesnicí (na iPadu může být klávesnice plovoucí).
    func assertAboveKeyboard(_ field: XCUIElement, file: StaticString = #filePath, line: UInt = #line) {
        let kb = app.keyboards.firstMatch
        guard kb.exists else { return }
        XCTAssertFalse(kb.frame.intersects(field.frame), "focused field hidden under the keyboard", file: file, line: line)
    }

    /// Posune obsah nahoru. Naležato má každý sloupec vlastní posouvání – táhne se ve sloupci prvku
    /// (na výšku jako dřív uprostřed obrazovky).
    func scrollUp(near e: XCUIElement) {
        guard isLandscape else {
            app.swipeUp()
            pause(0.8)
            return
        }
        let w = app.frame.width
        let x = e.exists && e.frame.width > 0 ? min(max(e.frame.midX / w, 0.05), 0.95) : 0.75
        drag(x: x)
    }

    func scrollDown(near e: XCUIElement) {
        guard isLandscape else {
            app.swipeDown()
            pause(0.8)
            return
        }
        let w = app.frame.width
        let x = e.exists && e.frame.width > 0 ? min(max(e.frame.midX / w, 0.05), 0.95) : 0.75
        drag(x: x, from: 0.35, to: 0.75)
    }

    /// Tah prstem nahoru ve svislém pásu x (0…1 šířky aplikace).
    func drag(x: CGFloat, from: CGFloat = 0.75, to: CGFloat = 0.35) {
        let start = app.coordinate(withNormalizedOffset: CGVector(dx: x, dy: from))
        start.press(forDuration: 0.05, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: x, dy: to)))
        pause(0.8)
    }

    /// Klávesa palety rovnic: stejný label mají i čísla kroků nahoře, paleta je vždy nejníž.
    /// Naležato je paleta v levém sloupci pod výkresem – když není vidět, posune se levý sloupec.
    func key(_ label: String, file: StaticString = #filePath, line: UInt = #line) {
        func visible() -> [XCUIElement] {
            app.buttons.matching(NSPredicate(format: "label == %@", label)).allElementsBoundByIndex.filter { $0.isHittable }
        }
        var all = visible()
        for _ in 0..<4 where isLandscape && all.allSatisfy({ $0.frame.midX > app.frame.width / 2 }) {
            drag(x: 0.25)
            all = visible()
        }
        if isLandscape { all = all.filter { $0.frame.midX < app.frame.width / 2 } }
        guard let k = all.max(by: { $0.frame.minY < $1.frame.minY }) else {
            XCTFail("key \(label) not found", file: file, line: line)
            return
        }
        k.tap()
    }

    /// Napíše text do pole. Pole schované pod klávesnicí (číselná nemá „Další“) – nejdřív klávesnici skryje
    /// a pole posune do pohledu, jako by to udělal žák.
    func enter(_ text: String, into field: XCUIElement) {
        let kb = app.keyboards.firstMatch
        if kb.exists && field.frame.maxY > kb.frame.minY { dismissKeyboard() }
        for _ in 0..<6 where !field.isHittable { scrollUp(near: field) }
        // první klepnutí někdy fokus nedá (pole se ještě posouvá nad klávesnici) – klepnout znovu
        for _ in 0..<3 {
            field.tap()
            pause(0.4)
            if (field.value(forKey: "hasKeyboardFocus") as? Bool) == true { break }
        }
        field.typeText(text)
    }

    /// Skryje klávesnici klepnutím mimo pole (číselná klávesnice iOS nemá „Hotovo“).
    func dismissKeyboard() {
        guard app.keyboards.firstMatch.exists else { return }
        el("Statika · nosníky").tap()
        pause(0.8)
        XCTAssertFalse(app.keyboards.firstMatch.exists, "keyboard still visible after tapping outside")
    }

    func pause(_ seconds: TimeInterval) {
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
    }
}
