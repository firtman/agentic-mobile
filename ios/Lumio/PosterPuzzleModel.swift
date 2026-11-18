import CoreGraphics
import Foundation
import Observation

@MainActor
@Observable
final class PosterPuzzleModel {
    enum Status: Equatable {
        case idle, loadingCatalog, catalogError, noPosters, loadingArtwork, artworkError, ready
    }

    private struct Candidate {
        let movie: Movie
        let url: URL
    }

    private let repository: CatalogRepository
    private let playCue: (GameCue) -> Void
    private let loadImage: (URL) async throws -> CGImage
    @ObservationIgnored private var request: Task<Void, Never>?
    @ObservationIgnored private var candidates: [Candidate] = []
    @ObservationIgnored private var current: Candidate?

    private(set) var status = Status.idle
    private(set) var movie: Movie?
    private(set) var artwork: CGImage?
    private(set) var difficulty = PuzzleDifficulty.three
    private(set) var board = PuzzleBoard(difficulty: .three)
    private(set) var showingReference = false

    var canMove: Bool { status == .ready && !showingReference && !board.complete }
    var hasPuzzle: Bool { movie != nil }

    init(repository: CatalogRepository, playCue: @escaping (GameCue) -> Void = { _ in },
         loadImage: ((URL) async throws -> CGImage)? = nil) {
        self.repository = repository
        self.playCue = playCue
        self.loadImage = loadImage ?? { try await PosterImages.shared.image(from: $0) }
    }

    func startIfNeeded() {
        if status == .idle { loadCatalog() }
    }

    func retry() {
        switch status {
        case .catalogError:
            loadCatalog()
        case .artworkError:
            if let current { loadArtwork(current) }
        default:
            break
        }
    }

    func next() {
        guard !candidates.isEmpty else { return }
        let alternatives = candidates.filter { $0.movie.id != current?.movie.id }
        guard let candidate = (alternatives.isEmpty ? candidates : alternatives).randomElement() else { return }
        current = candidate
        movie = candidate.movie
        resetBoard()
        loadArtwork(candidate)
    }

    func changeDifficulty(_ difficulty: PuzzleDifficulty) {
        guard self.difficulty != difficulty else { return }
        self.difficulty = difficulty
        resetBoard()
    }

    func choose(_ position: Int) {
        guard canMove else { return }
        let previousMoves = board.moves
        board.choose(position)
        playMoveCue(after: previousMoves)
    }

    func swap(_ source: Int, with destination: Int) {
        guard canMove else { return }
        let previousMoves = board.moves
        board.swap(source, with: destination)
        playMoveCue(after: previousMoves)
    }

    private func playMoveCue(after previousMoves: Int) {
        if board.moves > previousMoves { playCue(board.complete ? .completion : .swap) }
    }

    func toggleReference() {
        guard status == .ready else { return }
        board.clearSelection()
        showingReference.toggle()
    }

    private func resetBoard() {
        board = PuzzleBoard(difficulty: difficulty)
        showingReference = false
    }

    private func loadCatalog() {
        request?.cancel()
        status = .loadingCatalog
        request = Task { [weak self, repository] in
            do {
                let catalog = try await repository.load()
                guard let self, !Task.isCancelled else { return }
                var seen = Set<String>()
                candidates = catalog.movies.compactMap { movie in
                    guard !movie.id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                          !movie.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                          let value = movie.posterUrl,
                          let url = URL(string: value, encodingInvalidCharacters: false),
                          let host = url.host, !host.isEmpty,
                          let scheme = url.scheme?.lowercased(), ["http", "https"].contains(scheme),
                          seen.insert(movie.id).inserted else { return nil }
                    return Candidate(movie: movie, url: url)
                }
                if candidates.isEmpty {
                    status = .noPosters
                } else {
                    next()
                }
            } catch {
                guard !Task.isCancelled else { return }
                self?.status = .catalogError
            }
        }
    }

    private func loadArtwork(_ candidate: Candidate) {
        request?.cancel()
        artwork = nil
        status = .loadingArtwork
        request = Task { [weak self, loadImage] in
            do {
                let supplied = try await loadImage(candidate.url)
                let width = min(supplied.width, max(1, supplied.height * 2 / 3))
                let height = min(supplied.height, width * 3 / 2)
                let area = CGRect(x: (supplied.width - width) / 2, y: (supplied.height - height) / 2,
                                  width: width, height: height)
                guard let crop = supplied.cropping(to: area) else { throw URLError(.cannotDecodeContentData) }
                guard let self, !Task.isCancelled else { return }
                artwork = crop
                status = .ready
            } catch {
                guard !Task.isCancelled else { return }
                self?.status = .artworkError
            }
        }
    }
}
