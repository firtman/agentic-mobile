import XCTest

final class TriviaFlowTests: XCTestCase {
    @MainActor
    func testLiveRoundPausesOnLeavingAndAdvancesAfterAnswerFeedback() throws {
        let app = XCUIApplication()
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        app.buttons["destination-arcade"].tap()
        let entry = app.buttons["arcade-trivia"]
        XCTAssertTrue(entry.waitForExistence(timeout: 8))
        entry.tap()
        let start = app.buttons["trivia-start"]
        XCTAssertTrue(start.waitForExistence(timeout: 8))
        let question = app.staticTexts["trivia-question"]
        XCTAssertFalse(question.exists)
        start.tap()
        XCTAssertTrue(question.waitForExistence(timeout: 25))

        let firstQuestion = question.label
        app.buttons["settings"].tap()
        let styles = ["system", "light", "dark"].map { app.buttons["appearance-\($0)"] }
        let original = try XCTUnwrap(styles.first(where: { $0.isSelected }))
        (styles[2].isSelected ? styles[1] : styles[2]).tap()
        app.buttons["settings-close"].tap()
        XCTAssertFalse(question.exists)
        app.buttons["trivia-resume"].tap()
        XCTAssertEqual(question.label, firstQuestion)
        app.buttons["settings"].tap()
        original.tap()
        app.buttons["settings-close"].tap()
        app.buttons["trivia-resume"].tap()

        app.navigationBars["Trivia"].buttons.element(boundBy: 0).tap()
        XCTAssertTrue(entry.waitForExistence(timeout: 8))
        XCTAssertTrue(entry.label.contains("Return to round"))
        entry.tap()
        let resume = app.buttons["trivia-resume"]
        XCTAssertTrue(resume.waitForExistence(timeout: 8))
        XCTAssertFalse(question.exists)
        resume.tap()

        let lifeline = app.buttons["trivia-lifeline"]
        XCTAssertTrue(lifeline.isEnabled)
        lifeline.tap()
        XCTAssertFalse(lifeline.isEnabled)
        let options = (0..<4).map { app.buttons["trivia-answer-\($0)"] }.filter { $0.exists && $0.isEnabled }
        XCTAssertEqual(options.count, 2)
        options.first?.tap()
        let counter = app.staticTexts["trivia-counter"]
        let next = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "exists == true AND label == %@", "Question 2 of 5"), object: counter
        )
        XCTAssertEqual(XCTWaiter.wait(for: [next], timeout: 12), .completed)
        XCTAssertFalse(lifeline.isEnabled)
    }
}
