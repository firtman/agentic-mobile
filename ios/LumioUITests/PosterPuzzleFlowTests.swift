import XCTest

final class PosterPuzzleFlowTests: XCTestCase {
    @MainActor
    func testLiveBoardDragsTapsAndRetainsDifficulty() {
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        app.buttons["destination-arcade"].tap()
        let entry = app.buttons["arcade-puzzle"]
        XCTAssertTrue(entry.waitForExistence(timeout: 8))
        entry.tap()
        let first = app.buttons["puzzle-tile-0"]
        let title = app.staticTexts["puzzle-movie"]
        XCTAssertTrue(waitForArtwork(in: app, tile: first))
        let original = title.label
        let second = app.buttons["puzzle-tile-1"]
        let firstPiece = first.label.components(separatedBy: ",").first
        let secondPiece = second.label.components(separatedBy: ",").first
        let moves = app.staticTexts["puzzle-moves"]

        first.press(forDuration: 0.06, thenDragTo: second)
        XCTAssertEqual(moves.label, "Moves: 1")
        XCTAssertEqual(first.label.components(separatedBy: ",").first, secondPiece)
        XCTAssertEqual(second.label.components(separatedBy: ",").first, firstPiece)
        first.tap()
        second.tap()
        XCTAssertEqual(moves.label, "Moves: 2")
        XCTAssertEqual(first.label.components(separatedBy: ",").first, firstPiece)

        app.navigationBars["Poster Puzzle"].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(entry.waitForExistence(timeout: 8))
        entry.tap()
        XCTAssertTrue(first.waitForExistence(timeout: 8))
        XCTAssertEqual(moves.label, "Moves: 2")

        app.buttons["puzzle-size-4"].tap()
        XCTAssertTrue(app.buttons["puzzle-tile-15"].exists)
        XCTAssertEqual(moves.label, "Moves: 0")
        app.buttons["puzzle-size-6"].tap()
        XCTAssertTrue(app.buttons["puzzle-tile-35"].exists)
        XCTAssertEqual(title.label, original)
        XCTAssertTrue(app.buttons["puzzle-size-6"].isSelected)

        let scroll = app.scrollViews["puzzle-scroll"]
        let reference = app.buttons["puzzle-reference"]
        if !reference.isHittable {
            let origin = scroll.coordinate(withNormalizedOffset: CGVector(dx: 0.035, dy: 0.85))
            origin.press(forDuration: 0.05, thenDragTo: origin.withOffset(CGVector(dx: 0, dy: -300)))
        }
        reference.tap()
        XCTAssertTrue(app.images["puzzle-reference-image"].waitForExistence(timeout: 6))
        XCTAssertFalse(first.exists)
        reference.tap()

        app.buttons["puzzle-next"].tap()
        XCTAssertTrue(waitForArtwork(in: app, tile: first))
        XCTAssertNotEqual(title.label, original)
        XCTAssertTrue(app.buttons["puzzle-tile-35"].exists)
        XCTAssertEqual(moves.label, "Moves: 0")
    }

    private func waitForArtwork(in app: XCUIApplication, tile: XCUIElement) -> Bool {
        let error = app.staticTexts["puzzle-artwork-error"]
        let next = app.buttons["puzzle-next"]
        for _ in 0..<5 {
            let readyOrFailed = XCTNSPredicateExpectation(
                predicate: NSPredicate { _, _ in tile.exists || error.exists }, object: nil
            )
            _ = XCTWaiter.wait(for: [readyOrFailed], timeout: 12)
            if tile.exists { return true }
            guard next.exists else { return false }
            next.tap()
        }
        return false
    }
}
