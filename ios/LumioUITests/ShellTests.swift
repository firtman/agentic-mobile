import XCTest

final class ShellTests: XCTestCase {
    @MainActor
    func testDestinationsAndSettingsReturn() {
        let app = XCUIApplication()
        app.launchArguments = [
            "-AppleLanguages", "(de)", "-AppleLocale", "de_DE",
            "-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityM"
        ]
        app.launch()

        for (destination, title) in [("swipe", "Wischen"), ("genie", "Genie"), ("arcade", "Arcade")] {
            let button = app.buttons["destination-\(destination)"]
            XCTAssertTrue(button.waitForExistence(timeout: 5))
            button.tap()
            XCTAssertEqual(app.staticTexts["screen-title"].label, title)
            app.buttons["settings"].tap()
            XCTAssertTrue(app.buttons["settings-close"].waitForExistence(timeout: 5))
            app.buttons["settings-close"].tap()
            XCTAssertEqual(app.staticTexts["screen-title"].label, title)
        }
    }

    @MainActor
    func testLiveCatalogConnection() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.descendants(matching: .any)["catalog-connected"].firstMatch.waitForExistence(timeout: 10))
    }

    @MainActor
    func testUnavailableCatalogOffersRetry() {
        let app = XCUIApplication()
        app.launchEnvironment["LUMIO_API_BASE_URL"] = "http://127.0.0.1:1"
        app.launch()
        let retry = app.buttons["catalog-retry"]
        XCTAssertTrue(retry.waitForExistence(timeout: 10))
        retry.tap()
        XCTAssertTrue(retry.waitForExistence(timeout: 10))
    }
}
