import XCTest

final class MovieDetailsTests: XCTestCase {
    @MainActor
    func testOpenDetailsRateUndoAndReturnToExplore() {
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launchEnvironment["LUMIO_EPHEMERAL_RATINGS"] = "1"
        app.launch()

        let spotlight = app.buttons["explore-spotlight"]
        XCTAssertTrue(spotlight.waitForExistence(timeout: 12))
        spotlight.tap()
        XCTAssertTrue(app.staticTexts["movie-detail-title"].waitForExistence(timeout: 12))

        let like = app.buttons["movie-rate-like"]
        let dislike = app.buttons["movie-rate-dislike"]
        let undo = app.buttons["movie-rating-undo"]
        XCTAssertTrue(like.waitForExistence(timeout: 6))
        like.tap()
        XCTAssertTrue(undo.waitForExistence(timeout: 6))
        dislike.tap()
        XCTAssertTrue(undo.waitForExistence(timeout: 6))
        undo.tap()
        XCTAssertTrue(app.staticTexts["movie-rating-undone"].waitForExistence(timeout: 6))
        XCTAssertTrue(like.isSelected)
        XCTAssertFalse(dislike.isSelected)

        let back = app.navigationBars["Movie details"].buttons.element(boundBy: 0)
        XCTAssertTrue(back.exists)
        back.tap()
        XCTAssertTrue(spotlight.waitForExistence(timeout: 6))

        let scroll = app.scrollViews["explore-scroll"]
        let card = app.buttons.matching(identifier: "explore-movie").firstMatch
        if !card.isHittable { scroll.swipeUp() }
        XCTAssertTrue(card.isHittable)
        card.tap()
        XCTAssertTrue(app.staticTexts["movie-detail-title"].waitForExistence(timeout: 12))
        XCTAssertTrue(app.buttons["movie-rate-like"].isSelected)
    }
}
