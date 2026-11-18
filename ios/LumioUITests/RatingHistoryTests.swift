import XCTest

final class RatingHistoryTests: XCTestCase {
    @MainActor
    func testRateEditUndoAndReviewDetails() {
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launchEnvironment["LUMIO_EPHEMERAL_RATINGS"] = "1"
        app.launch()

        app.buttons["destination-swipe"].tap()
        let title = app.staticTexts["swipe-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 12))
        let movie = title.label
        app.buttons["swipe-like"].tap()
        app.buttons["open-rating-history"].tap()
        let details = app.buttons["history-details"].firstMatch
        XCTAssertTrue(details.waitForExistence(timeout: 12))
        XCTAssertTrue(details.label.contains(movie))

        app.buttons["history-edit"].firstMatch.tap()
        app.buttons["history-option-dislike"].tap()
        app.buttons["history-save"].tap()
        XCTAssertTrue(waitForLabel(of: details, containing: "Dislike"))
        app.buttons["history-undo"].firstMatch.tap()
        XCTAssertTrue(waitForLabel(of: details, containing: "Like"))

        details.tap()
        let detailTitle = app.staticTexts["movie-detail-title"]
        XCTAssertTrue(detailTitle.waitForExistence(timeout: 12))
        XCTAssertEqual(detailTitle.label, movie)
        XCTAssertTrue(app.buttons["movie-rate-like"].isSelected)
        app.navigationBars["Movie details"].buttons.element(boundBy: 0).tap()
        app.buttons["history-undo"].firstMatch.tap()
        XCTAssertTrue(app.staticTexts["history-empty"].waitForExistence(timeout: 6))
    }

    private func waitForLabel(of element: XCUIElement, containing value: String) -> Bool {
        let predicate = NSPredicate(format: "exists == true AND label CONTAINS %@", value)
        return XCTWaiter().wait(for: [expectation(for: predicate, evaluatedWith: element)], timeout: 8) == .completed
    }

    @MainActor
    func testHistoryIsReachableFromGenieWhenCatalogIsUnavailable() {
        let app = XCUIApplication()
        app.launchEnvironment["LUMIO_EPHEMERAL_RATINGS"] = "1"
        app.launchEnvironment["LUMIO_API_BASE_URL"] = "http://127.0.0.1:1"
        app.launch()

        app.buttons["destination-genie"].tap()
        app.buttons["open-rating-history"].tap()
        XCTAssertTrue(app.staticTexts["history-empty"].waitForExistence(timeout: 6))
    }
}
