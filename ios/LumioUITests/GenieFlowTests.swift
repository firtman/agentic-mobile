import XCTest

final class GenieFlowTests: XCTestCase {
    @MainActor
    func testLiveRecommendationAdvancesAndSurvivesReturningFromDetails() {
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launchEnvironment["LUMIO_EPHEMERAL_RATINGS"] = "1"
        app.launch()

        app.buttons["destination-genie"].tap()
        XCTAssertTrue(app.staticTexts["Give Genie a starting point"].waitForExistence(timeout: 12))
        app.buttons["Start swiping"].tap()
        XCTAssertTrue(app.staticTexts["swipe-title"].waitForExistence(timeout: 12))
        let rated = app.staticTexts["swipe-title"].label
        app.buttons["swipe-like"].tap()
        app.buttons["destination-genie"].tap()

        let pick = app.staticTexts["genie-title"]
        XCTAssertTrue(pick.waitForExistence(timeout: 25))
        XCTAssertNotEqual(pick.label, rated)
        let first = pick.label
        XCTAssertTrue(app.staticTexts.matching(identifier: "genie-reason").firstMatch.exists)
        app.buttons["genie-next"].tap()
        XCTAssertNotEqual(pick.label, first)
        let next = pick.label
        XCTAssertTrue(app.staticTexts["genie-position"].label.hasPrefix("2 of "))

        app.buttons["genie-details"].tap()
        let detail = app.staticTexts["movie-detail-title"]
        XCTAssertTrue(detail.waitForExistence(timeout: 12))
        XCTAssertEqual(detail.label, next)
        app.navigationBars["Movie details"].buttons.element(boundBy: 0).tap()
        XCTAssertEqual(pick.label, next)
    }
}
