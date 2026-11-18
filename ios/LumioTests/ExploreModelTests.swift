import XCTest
@testable import Lumio

@MainActor
final class ExploreModelTests: XCTestCase {
    func testSavedSortAndReset() {
        let catalog = makeClient()
        let restored = ExploreModel(catalog: catalog, savedSort: ExploreSort.titleDescending.rawValue)
        XCTAssertEqual(restored.filters.sort, .titleDescending)
        restored.reset()
        XCTAssertEqual(restored.filters, ExploreFilters())
        XCTAssertEqual(ExploreModel(catalog: catalog, savedSort: "invalid").filters.sort, .yearDescending)
    }

    func testFailedNextPageKeepsCardsAndRetryAppendsUniqueMovies() async throws {
        ExploreStub.resetAttempts()
        let model = ExploreModel(catalog: makeClient())
        model.start()
        try await waitFor { model.status == .content }
        XCTAssertEqual(model.movies.map(\.id), ["first"])
        XCTAssertTrue(model.hasMore)

        model.loadMore()
        try await waitFor { model.pageFailed }
        XCTAssertEqual(model.movies.map(\.id), ["first"])

        model.loadMore()
        try await waitFor { model.movies.count == 2 }
        XCTAssertEqual(model.movies.map(\.id), ["first", "second"])
        XCTAssertFalse(model.hasMore)
        XCTAssertFalse(model.pageFailed)
    }

    private func waitFor(_ condition: () -> Bool) async throws {
        let clock = ContinuousClock()
        let deadline = clock.now + .seconds(3)
        while !condition() {
            guard clock.now < deadline else {
                XCTFail("Explore state did not settle")
                throw URLError(.timedOut)
            }
            try await Task.sleep(for: .milliseconds(20))
        }
    }

    private func makeClient() -> CatalogClient {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [ExploreStub.self]
        return CatalogClient(
            baseURL: URL(string: "https://catalog.example")!,
            session: URLSession(configuration: configuration)
        )
    }
}

private final class ExploreStub: URLProtocol {
    private static let lock = NSLock()
    private static var pageAttempts = 0

    static func resetAttempts() {
        lock.withLock { pageAttempts = 0 }
    }

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let url = request.url,
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else { return }
        let query = components.queryItems ?? []
        let (status, body): (Int, String)
        if components.path == "/genres" {
            (status, body) = (200, #"{"genres":["Drama"],"languages":["en"]}"#)
        } else if query.contains(URLQueryItem(name: "offset", value: "1")) {
            let attempt = Self.lock.withLock {
                Self.pageAttempts += 1
                return Self.pageAttempts
            }
            (status, body) = attempt == 1 ? (503, "{}") : (200,
                #"{"movies":[{"id":"first","title":"Duplicate"},{"id":"second","title":"Other"}],"total":3,"limit":24,"offset":1}"#)
        } else {
            (status, body) = (200,
                #"{"movies":[{"id":"first","title":"Fixture"}],"total":3,"limit":24,"offset":0}"#)
        }
        let response = HTTPURLResponse(url: url, statusCode: status, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(body.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}
