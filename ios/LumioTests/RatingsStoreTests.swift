import SwiftData
import XCTest
@testable import Lumio

@MainActor
final class RatingsStoreTests: XCTestCase {
    func testChangesAndUndoSurviveReopeningStore() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let configuration = ModelConfiguration(url: directory.appendingPathComponent("ratings.store"), cloudKitDatabase: .none)
        let first = try movie("first")
        let second = try movie("second")
        let (initial, changed, other) = try persistChanges(configuration, first: first, second: second)

        let reopened = RatingsStore(configuration: configuration)
        XCTAssertEqual(try reopened.choice(for: first.id), .dislike)
        XCTAssertEqual(try reopened.choice(for: second.id), .unseen)
        XCTAssertFalse(try reopened.undoLatest(expectedID: changed))
        XCTAssertTrue(try reopened.undoLatest(expectedID: other))
        XCTAssertNil(try reopened.choice(for: second.id))
        XCTAssertTrue(try reopened.undoLatest(expectedID: changed))
        XCTAssertEqual(try reopened.choice(for: first.id), .like)
        XCTAssertTrue(try reopened.undoLatest(expectedID: initial))
        XCTAssertNil(try reopened.choice(for: first.id))
    }

    func testHistoryEditAndPerFilmUndoPreserveGlobalOrderAndRestoreTimestamp() throws {
        var now = Date(timeIntervalSince1970: 100)
        let store = RatingsStore(configuration: ModelConfiguration(isStoredInMemoryOnly: true), now: { now })
        let first = try movie("first")
        let second = try movie("second")
        let firstToken = try XCTUnwrap(store.rate(first, as: .like))
        now += 10
        let secondToken = try XCTUnwrap(store.rate(second, as: .unseen))

        let before = try store.history()
        XCTAssertEqual(before.map(\.id), ["second", "first"])
        let item = try XCTUnwrap(before.last)
        now += 10
        XCTAssertTrue(try store.changeChoice(for: item, to: .dislike))
        XCTAssertFalse(try store.changeChoice(for: item, to: .unseen))
        XCTAssertFalse(try store.undoMovie(first.id, expectedID: firstToken))

        let changed = try XCTUnwrap(store.history().first)
        XCTAssertEqual(changed.id, first.id)
        XCTAssertEqual(changed.choice, .dislike)
        XCTAssertEqual(changed.updatedAt, now)
        XCTAssertTrue(try store.undoMovie(first.id, expectedID: XCTUnwrap(changed.undoID)))
        let restored = try store.history()
        XCTAssertEqual(restored.map(\.id), ["second", "first"])
        XCTAssertEqual(restored.last?.choice, .like)
        XCTAssertEqual(restored.last?.updatedAt, item.updatedAt)

        XCTAssertTrue(try store.undoMovie(first.id, expectedID: firstToken))
        XCTAssertEqual(try store.latestUndoID(), secondToken)
        XCTAssertEqual(try store.ratedIDs(), [second.id])
        XCTAssertTrue(try store.undoLatest(expectedID: secondToken))
        XCTAssertTrue(try store.history().isEmpty)
    }

    func testLedgerRetainsOnlyMostRecentTwoHundredChanges() throws {
        let store = RatingsStore(configuration: ModelConfiguration(isStoredInMemoryOnly: true))
        let item = try movie("film")
        let first = try XCTUnwrap(store.rate(item, as: .like))
        var retained: [UUID] = []
        for index in 1...200 {
            retained.append(try XCTUnwrap(store.rate(item, as: index.isMultiple(of: 2) ? .like : .unseen)))
        }
        for token in retained.reversed() {
            XCTAssertTrue(try store.undoLatest(expectedID: token))
        }
        XCTAssertFalse(try store.undoLatest(expectedID: first))
        XCTAssertEqual(try store.choice(for: item.id), .like)
    }

    private func persistChanges(
        _ configuration: ModelConfiguration,
        first: Movie,
        second: Movie
    ) throws -> (UUID, UUID, UUID) {
        let store = RatingsStore(configuration: configuration)
        let initial = try XCTUnwrap(store.rate(first, as: .like))
        XCTAssertNil(try store.rate(first, as: .like))
        let changed = try XCTUnwrap(store.rate(first, as: .dislike))
        let other = try XCTUnwrap(store.rate(second, as: .unseen))
        return (initial, changed, other)
    }

    private func movie(_ id: String) throws -> Movie {
        try JSONDecoder().decode(Movie.self, from: Data(
            """
            {"id":"\(id)","title":"Fixture","year":2024,"genres":["Drama"],"director":"Director","cast":["Performer"]}
            """.utf8
        ))
    }
}
