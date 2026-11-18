import Foundation
import Observation

@MainActor
@Observable
final class GenieModel {
    enum Status: Equatable {
        case loading, getStarted, noMatches, content, networkError, storageError
    }

    private let repository: CatalogRepository
    private let ratings: RatingsStore
    @ObservationIgnored private var job: Task<Void, Never>?
    @ObservationIgnored private var revision: Int?
    private var recommendations: [Recommendation] = []
    private var selectedIndex = 0

    private(set) var status = Status.loading

    var pick: Recommendation? {
        recommendations.indices.contains(selectedIndex) ? recommendations[selectedIndex] : nil
    }
    var position: Int { selectedIndex + 1 }
    var count: Int { recommendations.count }
    var hasNext: Bool { position < count }

    init(repository: CatalogRepository, ratings: RatingsStore) {
        self.repository = repository
        self.ratings = ratings
    }

    func sync(force: Bool = false) {
        let revision = ratings.revision
        guard force || self.revision != revision else { return }
        self.revision = revision
        job?.cancel()
        let preferences: [TastePreference]
        do {
            preferences = try ratings.tastePreferences()
        } catch {
            status = .storageError
            return
        }
        guard preferences.contains(where: { $0.choice != .unseen }) else {
            recommendations = []
            selectedIndex = 0
            status = .getStarted
            return
        }

        let previousID = pick?.movie.id
        status = .loading
        job = Task { [weak self, repository] in
            do {
                let catalog = try await repository.load()
                try Task.checkCancellation()
                let work = Task.detached(priority: .userInitiated) {
                    try RecommendationEngine.rank(catalog.movies, using: preferences)
                }
                let matches = try await withTaskCancellationHandler {
                    try await work.value
                } onCancel: {
                    work.cancel()
                }
                guard let self, !Task.isCancelled else { return }
                recommendations = matches
                selectedIndex = matches.firstIndex { $0.movie.id == previousID } ?? 0
                status = matches.isEmpty ? .noMatches : .content
            } catch {
                guard !Task.isCancelled else { return }
                self?.status = .networkError
            }
        }
    }

    func next() {
        guard status == .content, hasNext else { return }
        selectedIndex += 1
    }

    func restart() {
        guard status == .content else { return }
        selectedIndex = 0
    }
}
