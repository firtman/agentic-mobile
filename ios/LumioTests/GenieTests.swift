import SwiftData
import XCTest
@testable import Lumio

@MainActor
final class GenieTests: XCTestCase {
    func testRankingUsesSignedWeightsAndOnlyActualPositiveReasons() throws {
        let movies = try [
            movie("liked", genres: ["Drama"]),
            movie("unseen", genres: ["Drama"]),
            movie("mixed", director: "A Director", cast: ["A Star", "a star", "Other Actor"], genres: ["Drama", "Horror"]),
            movie("director", director: "A Director", genres: ["Horror"]),
            movie("genre", genres: ["drama", "Drama"]),
            movie("zero", genres: ["Comedy"]),
            movie("negative", genres: ["Horror"])
        ]
        let preferences = [
            TastePreference(movieID: "liked", choice: .like, genres: ["Drama", " drama ", ""],
                            director: "a director", cast: ["A Star", "A Star", "Other Actor"]),
            TastePreference(movieID: "disliked", choice: .dislike, genres: ["Horror"], director: nil, cast: []),
            TastePreference(movieID: "unseen", choice: .unseen, genres: ["Comedy"], director: nil, cast: [])
        ]
        let results = try RecommendationEngine.rank(movies, using: preferences)
        XCTAssertEqual(results.map(\.movie.id), ["mixed", "director", "genre"])
        XCTAssertEqual(results.map(\.score), [7, 2, 1])
        XCTAssertEqual(results[0].matches.map(\.name), ["A Director", "A Star", "Other Actor"])
        XCTAssertEqual(results[1].matches.map(\.name), ["A Director"])
    }

    func testTiesUsePopularityTitleAndIDAndCapTheList() throws {
        let first = try movie("a", title: "Zulu", popularity: 100, genres: ["Drama"])
        let byTitle = try movie("z", title: "Alpha", popularity: 10, genres: ["Drama"])
        let sameTitle1 = try movie("b", title: "Beta", popularity: 10, genres: ["Drama"])
        let sameTitle2 = try movie("c", title: "beta", popularity: 10, genres: ["Drama"])
        let remaining = try (0..<40).map { try movie("remaining-\($0)", genres: ["Drama"]) }
        let preference = TastePreference(movieID: "liked", choice: .like, genres: ["Drama"], director: nil, cast: [])
        let recommendations = try RecommendationEngine.rank(remaining + [sameTitle2, first, sameTitle1, byTitle, first], using: [preference])
        XCTAssertEqual(recommendations.count, 30)
        XCTAssertEqual(recommendations.prefix(4).map(\.movie.id), ["a", "z", "b", "c"])
    }

    func testCatalogCoalescesAndRetriesAfterFailure() async throws {
        GenieCatalogStub.reset(failFirst: true)
        let repository = makeRepository()
        do {
            _ = try await repository.load()
            XCTFail("Expected the first call to fail")
        } catch let error as CatalogHTTPError {
            XCTAssertEqual(error.statusCode, 503)
        }
        async let first = repository.load()
        async let second = repository.load()
        let (left, right) = try await (first, second)
        XCTAssertEqual(left.movies.map(\.id), right.movies.map(\.id))
        _ = try await repository.load()
        XCTAssertEqual(GenieCatalogStub.requests, 2)
    }

    func testUnseenSkipsCatalogAndSelectionSurvivesRecomputation() async throws {
        GenieCatalogStub.reset()
        let ratings = RatingsStore(configuration: ModelConfiguration(isStoredInMemoryOnly: true))
        let model = GenieModel(repository: makeRepository(), ratings: ratings)
        _ = try ratings.rate(movie("unseen", genres: ["Drama"]), as: .unseen)
        model.sync()
        XCTAssertEqual(model.status, .getStarted)
        XCTAssertEqual(GenieCatalogStub.requests, 0)

        _ = try ratings.rate(movie("liked", genres: ["Drama"]), as: .like)
        model.sync()
        try await waitForContent(model)
        XCTAssertEqual(model.pick?.movie.id, "a")
        XCTAssertEqual(model.count, 2)
        model.next()
        XCTAssertEqual(model.pick?.movie.id, "b")
        XCTAssertFalse(model.hasNext)
        model.next()
        XCTAssertEqual(model.position, 2)

        _ = try ratings.rate(movie("other", genres: ["Drama"]), as: .like)
        model.sync()
        try await waitForContent(model)
        XCTAssertEqual(model.pick?.movie.id, "b")
        model.restart()
        XCTAssertEqual(model.position, 1)
        model.next()

        _ = try ratings.rate(movie("b", genres: ["Drama"]), as: .unseen)
        model.sync()
        try await waitForContent(model)
        XCTAssertEqual(model.pick?.movie.id, "a")
        XCTAssertEqual(model.count, 1)
        XCTAssertEqual(GenieCatalogStub.requests, 1)
    }

    private func waitForContent(_ model: GenieModel) async throws {
        let clock = ContinuousClock()
        let deadline = clock.now + .seconds(4)
        while model.status != .content {
            guard clock.now < deadline else { throw URLError(.timedOut) }
            try await Task.sleep(for: .milliseconds(20))
        }
    }

    private func makeRepository() -> CatalogRepository {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [GenieCatalogStub.self]
        return CatalogRepository(client: CatalogClient(
            baseURL: URL(string: "https://catalog.example")!,
            session: URLSession(configuration: configuration)
        ))
    }

    private func movie(
        _ id: String, title: String? = nil, popularity: Double = 0,
        director: String = "", cast: [String] = [], genres: [String] = []
    ) throws -> Movie {
        let data = try JSONSerialization.data(withJSONObject: [
            "id": id, "title": title ?? id, "popularity": popularity,
            "director": director, "cast": cast, "genres": genres
        ])
        return try JSONDecoder().decode(Movie.self, from: data)
    }
}

private final class GenieCatalogStub: URLProtocol {
    private static let lock = NSLock()
    private static var count = 0
    private static var failFirst = false

    static var requests: Int { lock.withLock { count } }

    static func reset(failFirst: Bool = false) {
        lock.withLock {
            count = 0
            self.failFirst = failFirst
        }
    }

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let url = request.url else { return }
        let fail = Self.lock.withLock {
            Self.count += 1
            return Self.count == 1 && Self.failFirst
        }
        let body = fail ? "{}" : """
            {"movies":[{"id":"a","title":"A","genres":["Drama"]},{"id":"b","title":"B","genres":["Drama"]}],"trivia":[]}
            """
        let response = HTTPURLResponse(url: url, statusCode: fail ? 503 : 200, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(body.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}
