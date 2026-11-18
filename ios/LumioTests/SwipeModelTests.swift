import SwiftData
import XCTest
@testable import Lumio

@MainActor
final class SwipeModelTests: XCTestCase {
    override func setUp() {
        super.setUp()
        SwipeStub.reset()
    }

    func testGestureDirectionsAndThreshold() {
        XCTAssertEqual(swipeChoice(for: CGSize(width: 90, height: 10)), .like)
        XCTAssertEqual(swipeChoice(for: CGSize(width: -90, height: 30)), .dislike)
        XCTAssertEqual(swipeChoice(for: CGSize(width: 20, height: 100)), .unseen)
        XCTAssertNil(swipeChoice(for: CGSize(width: 10, height: -120)))
        XCTAssertNil(swipeChoice(for: CGSize(width: 60, height: 20)))
    }

    func testSkipsFullyRatedPagesIncludingUnseen() async throws {
        let store = makeStore()
        _ = try store.rate(movie("a"), as: .like)
        _ = try store.rate(movie("b"), as: .unseen)
        let model = SwipeModel(catalog: makeClient(), ratings: store)
        model.sync()
        try await waitFor { model.status == .card }
        XCTAssertEqual(model.current?.id, "c")
        XCTAssertEqual(SwipeStub.offsets, [0, 2])
        XCTAssertEqual(SwipeStub.sorts, ["popularity desc"])
    }

    func testRatingAdvancesAndGlobalUndoReturnsFilm() async throws {
        let store = makeStore()
        let model = SwipeModel(catalog: makeClient(), ratings: store)
        model.sync()
        try await waitFor { model.current?.id == "a" }
        XCTAssertNil(model.undoID)

        model.rate(.like)
        try await waitFor { model.current?.id == "b" }
        model.rate(.unseen)
        try await waitFor { model.current?.id == "c" }
        XCTAssertEqual(try store.choice(for: "b"), .unseen)

        model.undo()
        XCTAssertEqual(model.feedback, .undone)
        try await waitFor { model.current?.id == "b" }
        XCTAssertNil(try store.choice(for: "b"))
        XCTAssertEqual(try store.choice(for: "a"), .like)
        XCTAssertNotNil(model.undoID)
    }

    func testDetailsChangesUpdateQueue() async throws {
        let store = makeStore()
        let model = SwipeModel(catalog: makeClient(), ratings: store)
        model.sync()
        try await waitFor { model.current?.id == "a" }

        // A choice made elsewhere, such as Movie Details, removes the visible card.
        let token = try XCTUnwrap(store.rate(movie("a"), as: .dislike))
        model.sync()
        try await waitFor { model.current?.id == "b" }

        // Undoing it elsewhere makes the film eligible again.
        XCTAssertTrue(try store.undoLatest(expectedID: token))
        model.sync()
        try await waitFor { model.current?.id == "a" }
    }

    func testExhaustionAndRefresh() async throws {
        let store = makeStore()
        for id in ["a", "b", "c", "d"] { _ = try store.rate(movie(id), as: .dislike) }
        let model = SwipeModel(catalog: makeClient(), ratings: store)
        model.sync()
        try await waitFor { model.status == .exhausted }
        XCTAssertNil(model.current)

        model.undo()
        try await waitFor { model.current?.id == "d" }
    }

    func testNetworkFailureRetriesSamePage() async throws {
        SwipeStub.failFirstSecondPage = true
        let store = makeStore()
        _ = try store.rate(movie("a"), as: .like)
        _ = try store.rate(movie("b"), as: .like)
        let model = SwipeModel(catalog: makeClient(), ratings: store)
        model.sync()
        try await waitFor { model.status == .failed }
        model.retry()
        try await waitFor { model.current?.id == "c" }
        XCTAssertEqual(SwipeStub.offsets, [0, 2, 2])
    }

    private func makeStore() -> RatingsStore {
        RatingsStore(configuration: ModelConfiguration(isStoredInMemoryOnly: true))
    }

    private func movie(_ id: String) throws -> Movie {
        try JSONDecoder().decode(Movie.self, from: Data(#"{"id":"\#(id)","title":"Film \#(id)"}"#.utf8))
    }

    private func waitFor(_ condition: () -> Bool) async throws {
        let clock = ContinuousClock()
        let deadline = clock.now + .seconds(3)
        while !condition() {
            guard clock.now < deadline else {
                XCTFail("Swipe state did not settle")
                throw URLError(.timedOut)
            }
            try await Task.sleep(for: .milliseconds(20))
        }
    }

    private func makeClient() -> CatalogClient {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [SwipeStub.self]
        return CatalogClient(
            baseURL: URL(string: "https://catalog.example")!,
            session: URLSession(configuration: configuration)
        )
    }
}

private final class SwipeStub: URLProtocol {
    private static let lock = NSLock()
    nonisolated(unsafe) private static var recordedOffsets: [Int] = []
    nonisolated(unsafe) private static var recordedSorts: Set<String> = []
    nonisolated(unsafe) static var failFirstSecondPage = false

    static var offsets: [Int] { lock.withLock { recordedOffsets } }
    static var sorts: [String] { lock.withLock { recordedSorts.sorted() } }

    static func reset() {
        lock.withLock {
            recordedOffsets = []
            recordedSorts = []
            failFirstSecondPage = false
        }
    }

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let url = request.url,
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else { return }
        let query = Dictionary(uniqueKeysWithValues: (components.queryItems ?? []).map { ($0.name, $0.value ?? "") })
        let offset = Int(query["offset"] ?? "0") ?? 0
        let fail = Self.lock.withLock {
            Self.recordedOffsets.append(offset)
            Self.recordedSorts.insert("\(query["sort"] ?? "") \(query["order"] ?? "")")
            if offset == 2, Self.failFirstSecondPage {
                Self.failFirstSecondPage = false
                return true
            }
            return false
        }
        let ids = offset == 0 ? ["a", "b"] : offset == 2 ? ["c", "d"] : []
        let movies = ids.map { #"{"id":"\#($0)","title":"Film \#($0)"}"# }.joined(separator: ",")
        let body = #"{"movies":[\#(movies)],"total":4,"limit":2,"offset":\#(offset)}"#
        let response = HTTPURLResponse(url: url, statusCode: fail ? 503 : 200, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data((fail ? "{}" : body).utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}
