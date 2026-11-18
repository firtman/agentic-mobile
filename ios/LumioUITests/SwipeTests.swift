import XCTest

final class SwipeTests: XCTestCase {
    @MainActor
    func testButtonsGesturesUndoAndDetails() {
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launchEnvironment["LUMIO_EPHEMERAL_RATINGS"] = "1"
        app.launch()

        app.buttons["destination-swipe"].tap()
        let title = app.staticTexts["swipe-title"]
        let card = app.descendants(matching: .any)["swipe-card"].firstMatch
        XCTAssertTrue(card.waitForExistence(timeout: 12))
        XCTAssertFalse(app.buttons["swipe-undo"].isEnabled)

        let first = title.label
        app.buttons["swipe-like"].tap()
        XCTAssertTrue(waitForChange(of: title, from: first))
        let second = title.label

        card.swipeLeft(velocity: .slow)
        XCTAssertTrue(waitForChange(of: title, from: second))
        let third = title.label

        let start = card.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.3))
        start.press(forDuration: 0.05, thenDragTo: start.withOffset(CGVector(dx: 0, dy: 160)))
        XCTAssertTrue(waitForChange(of: title, from: third))

        // Global Undo restores the most recent choice and returns that film to the queue.
        app.buttons["swipe-undo"].tap()
        XCTAssertTrue(app.staticTexts["swipe-undone"].waitForExistence(timeout: 6))
        XCTAssertTrue(waitForLabel(of: title, equal: third))

        app.buttons["swipe-details"].tap()
        let detailTitle = app.staticTexts["movie-detail-title"]
        XCTAssertTrue(detailTitle.waitForExistence(timeout: 12))
        XCTAssertEqual(detailTitle.label, third)
        XCTAssertFalse(app.buttons["movie-rate-unseen"].isSelected)

        // A choice in details removes the film from the Swipe queue.
        app.buttons["movie-rate-unseen"].tap()
        app.navigationBars["Movie details"].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(waitForChange(of: title, from: third))
    }

    private func waitForChange(of element: XCUIElement, from label: String) -> Bool {
        let predicate = NSPredicate(format: "exists == true AND label != %@", label)
        return XCTWaiter().wait(for: [expectation(for: predicate, evaluatedWith: element)], timeout: 12) == .completed
    }

    private func waitForLabel(of element: XCUIElement, equal label: String) -> Bool {
        let predicate = NSPredicate(format: "exists == true AND label == %@", label)
        return XCTWaiter().wait(for: [expectation(for: predicate, evaluatedWith: element)], timeout: 12) == .completed
    }
}
