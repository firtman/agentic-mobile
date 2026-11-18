import XCTest

final class SettingsTests: XCTestCase {
    @MainActor
    func testOfflineSettingsPersistAndCreditsOpen() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launchEnvironment["LUMIO_API_BASE_URL"] = "http://127.0.0.1:1"
        app.launch()
        app.buttons["settings"].tap()
        let styles = ["system", "light", "dark"].map { app.buttons["appearance-\($0)"] }
        let sound = app.switches["game-sound"]
        let original = try XCTUnwrap(styles.first(where: { $0.isSelected }))
        let initialSound = try XCTUnwrap(sound.value as? String)
        let selected = styles[2].isSelected ? styles[1] : styles[2]
        selected.tap()
        sound.switches.firstMatch.tap()
        app.terminate()
        app.launch()
        app.buttons["settings"].tap()
        XCTAssertTrue(selected.isSelected)
        XCTAssertNotEqual(sound.value as? String, initialSound)
        original.tap()
        sound.switches.firstMatch.tap()
        for (file, title) in [("catalog_sources", "Catalog sources"), ("fraunces_ofl", "Fraunces"),
                              ("dm_sans_ofl", "DM Sans"), ("lucide", "Lucide")] {
            app.buttons["notice-\(file)"].tap()
            let bar = app.navigationBars[title]
            XCTAssertTrue(bar.waitForExistence(timeout: 5))
            XCTAssertFalse(app.staticTexts["This notice is unavailable."].exists)
            bar.buttons.element(boundBy: 0).tap()
        }
        app.buttons["settings-close"].tap()
        XCTAssertTrue(app.buttons["catalog-retry"].waitForExistence(timeout: 5))
    }
}
