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
    func testLiveExploreLoadsSpotlight() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.descendants(matching: .any)["explore-spotlight"].firstMatch.waitForExistence(timeout: 10))
    }

    @MainActor
    func testSearchEmptyStateAndReset() {
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        let search = app.textFields["explore-search"]
        XCTAssertTrue(search.waitForExistence(timeout: 10))
        search.tap()
        search.typeText("zzzzlumiozzzz\n")
        XCTAssertTrue(app.descendants(matching: .any)["explore-empty"].firstMatch.waitForExistence(timeout: 10))
        app.buttons["explore-reset"].tap()
        XCTAssertTrue(app.descendants(matching: .any)["explore-spotlight"].firstMatch.waitForExistence(timeout: 10))
        let sort = app.buttons["explore-sort"]
        app.swipeUp()
        XCTAssertTrue(sort.label.contains("Newest first"))
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
