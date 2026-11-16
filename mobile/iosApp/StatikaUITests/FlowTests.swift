import XCTest

/// Celá úloha od uvolnění po výsledek (úloha č. 1, lehká, prostý nosník: A pevná kloubová v 0, B posuvná v 4 m,
/// F1 = 7 kN v 3 m → RAx = 0, RAy = 1,75 kN, RB = 5,25 kN), výklad, oblíbené, nastavení a sdílení.
final class FlowTests: StatikaUITestCase {

    func testSolveWholeProblem() {
        solveProblemOne()
        screenshot("done")
    }

    func testPrimerFirstLesson() {
        tap("Výklad a procvičování")
        wait("Pokračovat →")
        screenshot("primer_home")
        tap("Síla je šipka")
        pause(1)
        screenshot("lesson1_card1")
        // zamčené „Dál“ ukáže, co chybí
        tap("Dál →", contains: true)
        pause(0.5)
        screenshot("lesson1_locked")
        tap("↗")
        tap("←")
        pause(0.5)
        screenshot("lesson1_after_chips")
        print("TREE-BEGIN primer\n\(app.debugDescription)\nTREE-END")
    }

    func testFavoritesSettingsAndShare() {
        tap("Přidat do oblíbených", contains: true)
        pause(1)
        screenshot("favorite_added")
        tap("Oblíbené úlohy", contains: true)
        wait("Úloha č. 1")
        screenshot("favorites_sheet")
        app.swipeDown()
        pause(1)
        tap("Nastavení")
        wait("Sdílet úlohu")
        screenshot("settings")
        tap("Sdílet úlohu")
        pause(2)
        screenshot("share_sheet")
    }
}

extension StatikaUITestCase {
    /// Projde úlohu až na obrazovku „Hotovo“.
    func solveProblemOne(systemKeyboard: Bool = true) {
        wait("Úloha č. 1 · Prostý nosník")
        screenshot("start")
        // 1. uvolnění
        tap("RAx, RAy, dvě složky síly")
        tap("Pokračovat →")
        tap("RB, jedna síla kolmo k podložce")
        tap("Pokračovat →")
        let fields = app.textViews
        XCTAssertTrue(fields.firstMatch.waitForExistence(timeout: 10))
        enter("3", into: fields.element(boundBy: 0))
        screenshot("count_keyboard")
        enter("3", into: fields.element(boundBy: 1))
        dismissKeyboard()
        tap("Zkontrolovat")
        tap("Pokračovat →")
        // 2. zatížení (jen svislé síly – nic k nahrazení)
        wait("Náhrada zatížení")
        tap("Pokračovat →")
        // 3. rovnice z palety; mezi tím systémová klávesnice (⌨) – výkres se schová, pole zůstane vidět
        wait("Rovnice rovnováhy")
        screenshot("equations")
        if systemKeyboard {
            key("⌨")
            pause(1.5)
            screenshot("equations_system_keyboard")
            key("⌨")
            pause(1)
        }
        for k in ["RAx", "=", "0"] { key(k) }
        key("Ověřit")
        for k in ["RAy", "+", "RB", "−", "F1  7 kN", "=", "0"] { key(k) }
        key("Ověřit")
        for k in ["RB", "·", "4", "−", "F1  7 kN", "·", "3", "=", "0"] { key(k) }
        key("Ověřit")
        screenshot("equations_done")
        tap("Pokračovat k výpočtu →")
        // 4. výpočet
        wait("Výpočet reakcí")
        let values = app.textViews
        XCTAssertTrue(values.firstMatch.waitForExistence(timeout: 10))
        // první pole: klávesnice se otevře a pole zůstane nad ní (naležato zbude pod lištou jen pruh)
        for _ in 0..<6 where !values.element(boundBy: 0).isHittable { scrollUp(near: values.element(boundBy: 0)) }
        values.element(boundBy: 0).tap()
        pause(1.5)
        screenshot("solve_focus")
        assertAboveKeyboard(values.element(boundBy: 0))
        for (i, v) in ["0", "1,75", "5,25"].enumerated() {
            enter(v, into: values.element(boundBy: i))
        }
        screenshot("solve_typed")
        // „±“ přepne znaménko (klávesnice iOS minus nemá) a zpět – u pole R_B (5,25), které je teď vidět.
        // Compose hlásí přístupnosti jen viditelné prvky, takže „první ±“ a „první pole“ nemusí patřit k sobě:
        // pole se hledá podle hodnoty a ± podle výšky na obrazovce.
        dismissKeyboard()
        toggleSign(ofFieldWith: "5,25")
        XCTAssertTrue(field(with: "5,25").value as? String == "−5,25", "± did not add minus: \(field(with: "5,25").value ?? "nil")")
        toggleSign(ofFieldWith: "5,25")
        XCTAssertEqual(field(with: "5,25").value as? String, "5,25")
        dismissKeyboard()
        tap("Zkontrolovat")
        tap("Pokračovat →")
        wait("Nová úloha")
        pause(1)
    }

}
