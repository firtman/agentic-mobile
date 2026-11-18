import CoreGraphics
import XCTest
@testable import Lumio

@MainActor
final class PosterPuzzleTests: XCTestCase {
    func testBoardSwapsExactlyTwoAndRejectsInvalidOrCompletedMoves() {
        for size in PuzzleDifficulty.allCases {
            let shuffled = PuzzleBoard(difficulty: size)
            XCTAssertEqual(shuffled.tiles.count, size.tileCount)
            XCTAssertEqual(Set(shuffled.tiles), Set(0..<size.tileCount))
            XCTAssertFalse(shuffled.complete)
        }

        var board = PuzzleBoard(difficulty: .three, tiles: [1, 0, 2, 3, 4, 5, 6, 7, 8])
        board.swap(0, with: 0)
        board.swap(0, with: -1)
        XCTAssertEqual(board.moves, 0)
        board.choose(0)
        board.choose(0)
        XCTAssertNil(board.selected)
        board.choose(0)
        XCTAssertEqual(board.selected, 0)
        board.choose(1)
        XCTAssertEqual(board.tiles, Array(0..<9))
        XCTAssertEqual(board.moves, 1)
        XCTAssertTrue(board.complete)
        board.swap(0, with: 1)
        XCTAssertEqual(board.moves, 1)
    }

    func testFailedArtworkBlocksPlayAndRetryAndNextRespectSamePosterRules() async throws {
        let repository = makeRepository()
        let image = try makeImage()
        var requests: [URL] = []
        var cues: [GameCue] = []
        let model = PosterPuzzleModel(repository: repository, playCue: { cues.append($0) }) { url in
            requests.append(url)
            if requests.count == 1 { throw URLError(.cannotDecodeContentData) }
            return image
        }
        model.startIfNeeded()
        try await waitFor(.artworkError, in: model)
        let first = try XCTUnwrap(model.movie?.id)
        XCTAssertTrue(["first", "second"].contains(first))
        XCTAssertNil(model.artwork)
        XCTAssertFalse(model.canMove)
        model.swap(0, with: 1)
        model.toggleReference()
        XCTAssertEqual(model.board.moves, 0)
        XCTAssertFalse(model.showingReference)

        model.retry()
        try await waitFor(.ready, in: model)
        XCTAssertEqual(requests[0], requests[1])
        XCTAssertEqual(model.artwork?.width, 6)
        XCTAssertEqual(model.artwork?.height, 9)
        model.swap(0, with: 1)
        XCTAssertEqual(model.board.moves, 1)
        model.toggleReference()
        model.choose(1)
        XCTAssertNil(model.board.selected)
        model.changeDifficulty(.six)
        XCTAssertEqual(model.movie?.id, first)
        XCTAssertEqual(model.board.tiles.count, 36)
        XCTAssertEqual(model.board.moves, 0)
        XCTAssertFalse(model.showingReference)
        XCTAssertEqual(requests.count, 2)

        XCTAssertEqual(cues.count, 1)
        model.choose(0)
        model.choose(0)
        model.swap(0, with: 0)
        XCTAssertEqual(cues.count, 1)
        for index in model.board.tiles.indices where model.board.tiles[index] != index {
            let source = try XCTUnwrap(model.board.tiles.firstIndex(of: index))
            model.choose(index)
            model.choose(source)
        }
        XCTAssertTrue(model.board.complete)
        XCTAssertEqual(cues.last, .completion)
        XCTAssertEqual(cues.count, 1 + model.board.moves)
        model.next()
        try await waitFor(.ready, in: model)
        XCTAssertNotEqual(model.movie?.id, first)
        XCTAssertEqual(model.difficulty, .six)
        XCTAssertEqual(model.board.moves, 0)
    }

    private func waitFor(_ status: PosterPuzzleModel.Status, in model: PosterPuzzleModel) async throws {
        let deadline = ContinuousClock.now.advanced(by: .seconds(3))
        while model.status != status {
            guard ContinuousClock.now < deadline else { throw URLError(.timedOut) }
            try await Task.sleep(for: .milliseconds(20))
        }
    }

    private func makeRepository() -> CatalogRepository {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [PuzzleCatalogStub.self]
        return CatalogRepository(client: CatalogClient(
            baseURL: URL(string: "https://catalog.example")!, session: URLSession(configuration: configuration)
        ))
    }

    private func makeImage() throws -> CGImage {
        let context = try XCTUnwrap(CGContext(
            data: nil, width: 8, height: 9, bitsPerComponent: 8, bytesPerRow: 0,
            space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
        ))
        return try XCTUnwrap(context.makeImage())
    }
}

private final class PuzzleCatalogStub: URLProtocol {
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let url = request.url else { return }
        let body = """
            {"movies":[
              {"id":"blank","title":"No poster"},
              {"id":"non-web","title":"Local poster","posterUrl":"file:///picture.png"},
              {"id":"bad-url","title":"Invalid poster","posterUrl":"https://invalid test/image.png"},
              {"id":"first","title":"First","posterUrl":"https://catalog.example/first.png"},
              {"id":"second","title":"Second","posterUrl":"https://catalog.example/second.png"}
            ],"trivia":[]}
            """
        let response = HTTPURLResponse(url: url, statusCode: 200, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(body.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}
