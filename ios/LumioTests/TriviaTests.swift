import XCTest
@testable import Lumio

@MainActor
final class TriviaTests: XCTestCase {
    func testValidatesDeduplicatesAndRejectsMalformedQuestions() {
        let source = questions + [
            questions[0],
            question("duplicate-choices", options: ["A", " a ", "C", "D"]),
            question("blank-choice", options: ["A", "B", " ", "D"]),
            question("bad-index", answer: 9),
            question(" "),
            question("blank-question", prompt: " ")
        ]
        XCTAssertEqual(TriviaRound.validQuestions(source).map(\.id), questions.map(\.id))
    }

    func testPerfectRoundEarns750AndAutoAdvancesAfterFourSeconds() {
        var round = TriviaRound(questions: questions)
        for (index, points) in [100, 125, 150, 175, 200].enumerated() {
            XCTAssertEqual(round.index, index)
            XCTAssertEqual(round.remaining, .seconds(20))
            round.answer(round.question.answerIndex)
            XCTAssertEqual(round.earned, points)
            round.advanceTime(.milliseconds(3999))
            XCTAssertEqual(round.phase, .feedback)
            round.advanceTime(.milliseconds(1))
        }
        XCTAssertEqual(round.phase, .results)
        XCTAssertEqual(round.score, 750)
        XCTAssertEqual(round.bestStreak, 5)
        XCTAssertEqual(round.correctCount, 5)
    }

    func testLifelineTimeOutAndHoldDoNotCarryDisallowedStateIntoNextQuestion() {
        var round = TriviaRound(questions: questions)
        round.useFiftyFifty()
        XCTAssertEqual(round.eliminated.count, 2)
        XCTAssertFalse(round.eliminated.contains(round.question.answerIndex))
        let eliminated = round.eliminated
        round.useFiftyFifty()
        XCTAssertEqual(round.eliminated, eliminated)
        round.answer(eliminated.first)
        XCTAssertEqual(round.phase, .question)
        round.answer(round.question.answerIndex)
        round.next()
        XCTAssertTrue(round.eliminated.isEmpty)
        XCTAssertTrue(round.fiftyUsed)

        round.advanceTime(.seconds(20))
        XCTAssertEqual(round.phase, .feedback)
        XCTAssertNil(round.selected)
        XCTAssertEqual(round.streak, 0)
        XCTAssertEqual(round.score, 100)
        round.hold()
        round.advanceTime(.seconds(60))
        XCTAssertEqual(round.phase, .feedback)
        XCTAssertEqual(round.index, 1)
        round.next()

        round.answer(1)
        XCTAssertEqual(round.earned, 0)
        XCTAssertEqual(round.streak, 0)
        round.next()
        round.answer(round.question.answerIndex)
        XCTAssertEqual(round.earned, 100)
    }

    func testModelRejectsLateTapsPausesAcrossNavigationAndReplays() async throws {
        var now: Duration = .zero
        let session = URLSessionConfiguration.ephemeral
        session.protocolClasses = [TriviaCatalogStub.self]
        let client = CatalogClient(baseURL: URL(string: "https://catalog.example")!, session: URLSession(configuration: session))
        let model = TriviaModel(repository: CatalogRepository(client: client), now: { now })
        model.setVisible(true)
        model.start()
        try await waitForRound(model)
        now = .seconds(20)
        model.answer(0, question: 0)
        XCTAssertNil(model.round?.selected)
        XCTAssertEqual(model.round?.phase, .feedback)
        XCTAssertEqual(model.round?.score, 0)

        now += .seconds(1)
        model.setVisible(false)
        now += .seconds(120)
        model.refreshClock()
        XCTAssertEqual(model.round?.remaining, .seconds(3))
        XCTAssertEqual(model.round?.paused, true)
        model.setVisible(true)
        XCTAssertEqual(model.round?.paused, true)
        model.resume()
        now += .seconds(3)
        model.refreshClock()
        XCTAssertEqual(model.round?.index, 1)
        XCTAssertEqual(model.round?.phase, .question)
        model.answer(0, question: 0)
        XCTAssertEqual(model.round?.phase, .question)

        model.useFiftyFifty(question: 1)
        model.start()
        try await waitForRound(model)
        XCTAssertEqual(model.round?.index, 0)
        XCTAssertEqual(model.round?.fiftyUsed, false)
    }

    private func waitForRound(_ model: TriviaModel) async throws {
        let deadline = ContinuousClock.now.advanced(by: .seconds(3))
        while model.status == .loading {
            guard ContinuousClock.now < deadline else { throw URLError(.timedOut) }
            try await Task.sleep(for: .milliseconds(20))
        }
        XCTAssertEqual(model.status, .playing)
    }

    private var questions: [TriviaQuestion] { (0..<5).map { question(String($0)) } }

    private func question(
        _ id: String, prompt: String = "Fixture?", options: [String] = ["A", "B", "C", "D"], answer: Int = 0
    ) -> TriviaQuestion {
        TriviaQuestion(id: id, movieId: "fixture", kind: "fixture", question: prompt, options: options, answerIndex: answer)
    }
}

private final class TriviaCatalogStub: URLProtocol {
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let url = request.url else { return }
        let questions = (0..<5).map {
            #"{"id":"\#($0)","movieId":"fixture","kind":"fixture","question":"Fixture?","options":["A","B","C","D"],"answerIndex":0}"#
        }.joined(separator: ",")
        let body = #"{"movies":[],"trivia":[\#(questions)]}"#
        let response = HTTPURLResponse(url: url, statusCode: 200, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(body.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}
