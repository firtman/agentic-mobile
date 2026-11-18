import Foundation
import Observation

enum ExploreSort: String, CaseIterable, Identifiable {
    case yearDescending, yearAscending, titleAscending, titleDescending, ratingDescending, popularityDescending

    var id: Self { self }

    var field: String {
        switch self {
        case .yearDescending, .yearAscending: "year"
        case .titleAscending, .titleDescending: "title"
        case .ratingDescending: "rating"
        case .popularityDescending: "popularity"
        }
    }

    var order: String {
        switch self {
        case .yearAscending, .titleAscending: "asc"
        default: "desc"
        }
    }
}

struct ExploreFilters: Equatable {
    var query = ""
    var genre: String?
    var sort: ExploreSort = .yearDescending

    func parameters(offset: Int) -> [URLQueryItem] {
        var parameters = [
            URLQueryItem(name: "sort", value: sort.field),
            URLQueryItem(name: "order", value: sort.order),
            URLQueryItem(name: "limit", value: "24"),
            URLQueryItem(name: "offset", value: String(offset))
        ]
        let search = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if !search.isEmpty { parameters.append(URLQueryItem(name: "q", value: search)) }
        if let genre { parameters.append(URLQueryItem(name: "genre", value: genre)) }
        return parameters
    }
}

@MainActor
@Observable
final class ExploreModel {
    enum Status { case loading, empty, content, failed }
    enum GenreStatus { case loading, ready, failed }

    private let catalog: CatalogClient
    private var request: Task<Void, Never>?
    private var genreRequest: Task<Void, Never>?
    private var generation = 0
    private var nextOffset: Int?

    private(set) var filters: ExploreFilters
    private(set) var status = Status.loading
    private(set) var genres: [String] = []
    private(set) var genreStatus = GenreStatus.loading
    private(set) var movies: [Movie] = []
    private(set) var total = 0
    private(set) var loadingMore = false
    private(set) var pageFailed = false

    var hasMore: Bool { nextOffset != nil }

    init(catalog: CatalogClient, savedSort: String? = nil) {
        self.catalog = catalog
        filters = ExploreFilters(sort: savedSort.flatMap(ExploreSort.init(rawValue:)) ?? .yearDescending)
    }

    func start() {
        if request == nil { refresh() }
        if genreRequest == nil { loadGenres() }
    }

    func search(_ query: String) {
        guard filters.query != query else { return }
        filters.query = query
        reload(debounced: true)
    }

    func chooseGenre(_ genre: String?) {
        guard filters.genre != genre else { return }
        filters.genre = genre
        refresh()
    }

    func chooseSort(_ sort: ExploreSort) {
        guard filters.sort != sort else { return }
        filters.sort = sort
        refresh()
    }

    func reset() {
        filters = ExploreFilters()
        refresh()
    }

    func refresh() {
        reload(debounced: false)
        if genreStatus == .failed { loadGenres() }
    }

    func loadGenres() {
        genreRequest?.cancel()
        genreStatus = .loading
        genreRequest = Task { [weak self, catalog] in
            do {
                let response = try await catalog.genres()
                guard let self, !Task.isCancelled else { return }
                var seen = Set<String>()
                genres = response.genres.filter {
                    !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && seen.insert($0).inserted
                }
                genreStatus = .ready
            } catch {
                guard !Task.isCancelled else { return }
                self?.genreStatus = .failed
            }
        }
    }

    private func reload(debounced: Bool) {
        generation += 1
        request?.cancel()
        movies = []
        nextOffset = nil
        total = 0
        loadingMore = false
        pageFailed = false
        status = .loading
        fetch(offset: 0, debounced: debounced)
    }

    func loadMore() {
        guard let nextOffset, !loadingMore else { return }
        loadingMore = true
        pageFailed = false
        fetch(offset: nextOffset)
    }

    private func fetch(offset: Int, debounced: Bool = false) {
        let generation = generation
        let parameters = filters.parameters(offset: offset)
        request = Task { [weak self, catalog] in
            do {
                if debounced { try await Task.sleep(for: .milliseconds(300)) }
                let page = try await catalog.movies(query: parameters)
                guard let self, self.generation == generation, !Task.isCancelled else { return }
                var seen = Set(movies.map(\.id))
                movies.append(contentsOf: page.movies.filter { seen.insert($0.id).inserted })
                total = page.total
                let next = offset + page.movies.count
                nextOffset = !page.movies.isEmpty && next < page.total ? next : nil
                status = movies.isEmpty ? .empty : .content
                loadingMore = false
            } catch {
                guard let self, self.generation == generation, !Task.isCancelled else { return }
                loadingMore = false
                if offset == 0 {
                    status = .failed
                } else {
                    pageFailed = true
                }
            }
        }
    }
}
