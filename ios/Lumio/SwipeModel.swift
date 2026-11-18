import Foundation
import Observation

/// Physical directions, independent of language direction. Upward and short drags never rate.
func swipeChoice(for translation: CGSize, threshold: CGFloat = 72) -> RatingChoice? {
    let x = translation.width, y = translation.height
    if abs(x) >= threshold, abs(x) > abs(y) { return x > 0 ? .like : .dislike }
    if y >= threshold, y > abs(x) { return .unseen }
    return nil
}

/// Swipe keeps only unread catalog pages in memory. Choices, snapshots and Undo live in `RatingsStore`.
@MainActor
@Observable
final class SwipeModel {
    enum Status: Equatable { case loading, card, exhausted, failed, storageFailed }
    enum Feedback: Equatable { case undone, stale, saveFailed, undoFailed }

    static let pageSize = 24

    @ObservationIgnored private let catalog: CatalogClient
    @ObservationIgnored private let ratings: RatingsStore
    @ObservationIgnored private var ratedIDs: Set<String>?
    @ObservationIgnored private var syncedRevision: Int?
    @ObservationIgnored private var nextOffset = 0
    @ObservationIgnored private var hasMore = true
    @ObservationIgnored private var request: Task<Void, Never>?
    @ObservationIgnored private var generation = 0
    /// Films rated here this session, so a global Undo can return the restored film to the front.
    @ObservationIgnored private var recent: [String: Movie] = [:]

    private var queue: [Movie] = []
    private(set) var status = Status.loading
    private(set) var undoID: UUID?
    private(set) var feedback: Feedback?

    var current: Movie? { status == .card ? queue.first : nil }

    init(catalog: CatalogClient, ratings: RatingsStore) {
        self.catalog = catalog
        self.ratings = ratings
    }

    /// Reads the saved ledger when it changed elsewhere, such as in Movie Details or another destination.
    func sync() {
        guard ratedIDs == nil || syncedRevision != ratings.revision else {
            showNext()
            return
        }
        do {
            let ids = try ratings.ratedIDs()
            undoID = try ratings.latestUndoID()
            let restored = ratedIDs.map { $0.subtracting(ids) } ?? []
            ratedIDs = ids
            syncedRevision = ratings.revision
            if status == .storageFailed { status = .loading }
            if !restored.isEmpty {
                // A removed choice makes an already-consumed film eligible again.
                resetQueue()
                queue = restored.compactMap { recent[$0] }.sorted { $0.title < $1.title }
            }
            showNext()
        } catch {
            cancelRequest()
            ratedIDs = nil
            syncedRevision = nil
            undoID = nil
            status = .storageFailed
        }
    }

    func rate(_ choice: RatingChoice) {
        guard let movie = current else { return }
        feedback = nil
        do {
            _ = try ratings.rate(movie, as: choice)
            recent[movie.id] = movie
            if recent.count > 200 { recent.removeValue(forKey: recent.keys.first!) }
            queue.removeAll { $0.id == movie.id }
            sync()
        } catch {
            feedback = .saveFailed
        }
    }

    func undo() {
        guard let undoID else { return }
        do {
            feedback = try ratings.undoLatest(expectedID: undoID) ? .undone : .stale
        } catch {
            feedback = .undoFailed
        }
        sync()
    }

    func retry() {
        feedback = nil
        switch status {
        case .storageFailed:
            sync()
        case .exhausted:
            resetQueue()
            showNext()
        case .failed:
            cancelRequest()
            loadPage()
        case .loading, .card:
            break
        }
    }

    private func showNext() {
        guard let ids = ratedIDs else { return }
        queue.removeAll { ids.contains($0.id) }
        if !queue.isEmpty {
            status = .card
        } else if !hasMore {
            status = .exhausted
        } else if request == nil, status != .failed {
            loadPage()
        }
    }

    private func loadPage() {
        generation += 1
        let generation = generation
        status = .loading
        request = Task { [weak self] in
            guard let self else { return }
            do {
                // Pages whose films are all saved are skipped without a false empty state.
                while queue.isEmpty, hasMore {
                    let page = try await catalog.movies(query: Self.parameters(offset: nextOffset))
                    guard self.generation == generation, !Task.isCancelled else { return }
                    let excluded = ratedIDs ?? []
                    var seen = Set(queue.map(\.id))
                    queue += page.movies.filter { !excluded.contains($0.id) && seen.insert($0.id).inserted }
                    nextOffset = page.offset + page.movies.count
                    hasMore = !page.movies.isEmpty && nextOffset < page.total
                }
                request = nil
                showNext()
            } catch {
                guard self.generation == generation, !Task.isCancelled else { return }
                request = nil
                status = .failed
            }
        }
    }

    private func resetQueue() {
        cancelRequest()
        queue = []
        nextOffset = 0
        hasMore = true
        status = .loading
    }

    private func cancelRequest() {
        generation += 1
        request?.cancel()
        request = nil
    }

    static func parameters(offset: Int) -> [URLQueryItem] {
        [
            URLQueryItem(name: "sort", value: "popularity"),
            URLQueryItem(name: "order", value: "desc"),
            URLQueryItem(name: "limit", value: String(pageSize)),
            URLQueryItem(name: "offset", value: String(offset))
        ]
    }
}
