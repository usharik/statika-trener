import XCTest

/// Naležato: na iPadu (a širokém telefonu) dva sloupce – výkres vlevo, kroky vpravo; výklad a paleta rovnic.
final class LayoutTests: StatikaUITestCase {
    override func tearDownWithError() throws {
        try super.tearDownWithError()
        XCUIDevice.shared.orientation = .portrait
    }

    func testLandscape() {
        XCUIDevice.shared.orientation = .landscapeLeft
        wait("Úloha č. 1 · Prostý nosník")
        pause(1.5)
        screenshot("landscape_start")
        // telefon naležato: kroky vpravo se posouvají samostatně – zbytek scénáře stačí na iPadu
        guard UIDevice.current.userInterfaceIdiom == .pad else { return }
        tap("RAx, RAy, dvě složky síly")
        tap("Pokračovat →")
        tap("RB, jedna síla kolmo k podložce")
        tap("Pokračovat →")
        let fields = app.textViews
        XCTAssertTrue(fields.firstMatch.waitForExistence(timeout: 10))
        fields.element(boundBy: 0).tap(); fields.element(boundBy: 0).typeText("3")
        fields.element(boundBy: 1).tap(); fields.element(boundBy: 1).typeText("3")
        tap("Zkontrolovat")
        tap("Pokračovat →")
        tap("Pokračovat →")
        wait("Rovnice rovnováhy")
        for k in ["RAx", "=", "0"] { key(k) }
        pause(1)
        screenshot("landscape_equations")
        tap("Výklad a procvičování")
        tap("Síla je šipka")
        pause(1)
        screenshot("landscape_primer")
    }

    /// Celá úloha naležato (na telefonu i iPadu): dva sloupce, každý se posouvá zvlášť, paleta vlevo pod výkresem.
    func testLandscapeSolveWholeProblem() {
        XCUIDevice.shared.orientation = .landscapeLeft
        pause(1.5)
        solveProblemOne(systemKeyboard: false)
        screenshot("landscape_done")
        wait("Hotovo!")
    }

    /// Téma aplikace jiné než systémové: stavový řádek se musí přizpůsobit aplikaci (okno přebírá její vzhled).
    func testForcedThemes() {
        tap("Nastavení")
        tap("Světlé téma")
        app.swipeDown()
        pause(1.5)
        screenshot("forced_light")
        tap("Nastavení")
        tap("Tmavé téma")
        app.swipeDown()
        pause(1.5)
        screenshot("forced_dark")
        tap("Nastavení")
        tap("Podle systému")
        app.swipeDown()
        pause(1.5)
        screenshot("system_theme")
    }
}
